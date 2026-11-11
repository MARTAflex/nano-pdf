package de.martaflex.nanopdf.routes

import java.awt.image.BufferedImage
import java.io.*
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

import org.junit.*
import org.junit.Assert.*

import spark.Spark.*
import com.mashape.unirest.http.*
import com.fasterxml.jackson.databind.ObjectMapper

class AreasToImagesTest {
    companion object {
        @BeforeClass @JvmStatic
        fun setup () {
            port(9091)
            AreasToImages();
            // wait for spark to be initialized
            awaitInitialization()
        }

        @AfterClass @JvmStatic
        fun teardown () {
            stop();
            // give spark some time to shut down
            // FIXME: there is not method for waiting til spark server is stopped
            TimeUnit.SECONDS.sleep(1);
        }

        // the "firstname" field of form-fill-out.pdf (A4), inside the field border
        const val FIRSTNAME = """{ "page": 0, "x": 114, "y": 63, "width": 119, "height": 25 }"""
    }

    private fun pdf64 (file: String) = String(Base64.getEncoder().encode(File(file).readBytes()))

    private fun post (body: String) : HttpResponse<String> {
        return Unirest.post("http://127.0.0.1:9091/areas-to-images")
            .header("Content-Type", "application/json")
            .body(body)
            .asString()
    }

    private fun request (file: String, data: String, extra: String = "") : HttpResponse<String> {
        return post("""{ ${extra} "pdf": "${ pdf64(file) }", "data": ${data} }""")
    }

    private fun images (response: HttpResponse<String>) : Map<String, BufferedImage> {
        val json = ObjectMapper().readTree(response.getBody())
        val result = LinkedHashMap<String, BufferedImage>()
        for (name in json.fieldNames()) {
            val png = Base64.getDecoder().decode(json.get(name).asText())
            result[name] = ImageIO.read(ByteArrayInputStream(png))
        }
        return result
    }

    private fun darkPixels (image: BufferedImage) : Int {
        var count = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if ((image.getRGB(x, y) and 0xff) < 128) count++
            }
        }
        return count
    }

    @Test
    fun noPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/areas-to-images")
            .header("Content-Type", "application/json")
            .asString()

        assertEquals(400, response.getStatus());
    }

    @Test
    fun emptyPostBody () {
        assertEquals(400, post("").getStatus());
    }

    @Test
    fun invalidPostBody () {
        assertEquals(400, post("""{ "k1": "v1" }""").getStatus());
    }

    @Test
    fun invalidBase64 () {
        val response = post("""{ "pdf": "asdasdadasd", "data": { "a": ${FIRSTNAME} } }""")
        assertEquals(400, response.getStatus());
    }

    @Test
    fun cropsArea () {
        val response = request("resources/test/form-fill-out.pdf", """{ "firstname": ${FIRSTNAME} }""")

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getHeaders().getFirst("Content-Type"));

        val image = images(response)["firstname"]!!
        // default 300 dpi: 119 x 25 pt -> ~496 x ~104 px
        assertEquals(119 * 300 / 72.0, image.width.toDouble(), 1.0);
        assertEquals(25 * 300 / 72.0, image.height.toDouble(), 1.0);
        // the filled in text is in there
        assertTrue(darkPixels(image) > 100);
    }

    @Test
    fun dpiAndPadding () {
        val response = request(
            "resources/test/form-fill-out.pdf",
            """{ "firstname": ${FIRSTNAME} }""",
            """ "dpi": 144, "padding": 10, """
        )

        assertEquals(200, response.getStatus());
        val image = images(response)["firstname"]!!
        assertEquals(119 * 2 + 20, image.width);
        assertEquals(25 * 2 + 20, image.height);
        // the padding is white
        for (x in 0 until image.width) {
            assertEquals(0xffffff, image.getRGB(x, 0) and 0xffffff);
        }
    }

    @Test
    fun emptyAreaIsWhite () {
        // bottom right corner of the page, nothing there
        val response = request(
            "resources/test/form-fill-out.pdf",
            """{ "empty": { "x": 500, "y": 780, "width": 50, "height": 30 } }"""
        )

        assertEquals(200, response.getStatus());
        assertEquals(0, darkPixels(images(response)["empty"]!!));
    }

    @Test
    fun multipleAreasAndPages () {
        val response = request(
            "resources/test/to-text.pdf",
            """{
                "p0": { "page": 0, "x": 0, "y": 0, "width": 595, "height": 842 },
                "p1": { "page": 1, "x": 0, "y": 0, "width": 595, "height": 842 },
                "p0small": { "page": 0, "x": 10, "y": 10, "width": 20, "height": 20 }
            }""",
            """ "dpi": 72, """
        )

        assertEquals(200, response.getStatus());
        val result = images(response)
        // order of the request is kept
        assertEquals(listOf("p0", "p1", "p0small"), result.keys.toList());
        assertEquals(595, result["p0"]!!.width);
        assertEquals(842, result["p1"]!!.height);
        assertEquals(20, result["p0small"]!!.width);
    }

    @Test
    fun areaPartiallyOutsideIsClamped () {
        val response = request(
            "resources/test/form-fill-out.pdf",
            """{ "corner": { "x": 585, "y": 832, "width": 50, "height": 50 } }""",
            """ "dpi": 72, """
        )

        assertEquals(200, response.getStatus());
        val image = images(response)["corner"]!!
        assertEquals(10, image.width);
        assertEquals(10, image.height);
    }

    @Test
    fun color () {
        val response = request(
            "resources/test/form-fill-out.pdf",
            """{ "firstname": ${FIRSTNAME} }""",
            """ "color": true, """
        )

        assertEquals(200, response.getStatus());
        assertFalse(images(response)["firstname"]!!.colorModel.colorSpace.type == java.awt.color.ColorSpace.TYPE_GRAY);
    }

    @Test
    fun areaOutsidePage () {
        val response = request(
            "resources/test/form-fill-out.pdf",
            """{ "nowhere": { "x": 700, "y": 900, "width": 10, "height": 10 } }"""
        )
        assertEquals(400, response.getStatus());
        assertTrue(response.getBody().contains("nowhere"));
    }

    @Test
    fun pageDoesNotExist () {
        val response = request(
            "resources/test/form-fill-out.pdf",
            """{ "a": { "page": 1, "x": 0, "y": 0, "width": 10, "height": 10 } }"""
        )
        assertEquals(400, response.getStatus());
    }

    @Test
    fun invalidAreas () {
        val file = "resources/test/form-fill-out.pdf"
        assertEquals(400, request(file, """{ "a": { "x": 0, "y": 0, "width": 10 } }""").getStatus());
        assertEquals(400, request(file, """{ "a": { "x": "0", "y": 0, "width": 10, "height": 10 } }""").getStatus());
        assertEquals(400, request(file, """{ "a": { "x": 0, "y": 0, "width": 0, "height": 10 } }""").getStatus());
        assertEquals(400, request(file, """{ "a": { "page": 0.5, "x": 0, "y": 0, "width": 10, "height": 10 } }""").getStatus());
        assertEquals(400, request(file, """{ "a": 42 }""").getStatus());
        assertEquals(400, request(file, """[]""").getStatus());
    }

    @Test
    fun invalidOptions () {
        val file = "resources/test/form-fill-out.pdf"
        val data = """{ "firstname": ${FIRSTNAME} }"""
        assertEquals(400, request(file, data, """ "dpi": 601, """).getStatus());
        assertEquals(400, request(file, data, """ "dpi": 10, """).getStatus());
        assertEquals(400, request(file, data, """ "dpi": "300", """).getStatus());
        assertEquals(400, request(file, data, """ "padding": -1, """).getStatus());
        assertEquals(400, request(file, data, """ "padding": 1.5, """).getStatus());
        assertEquals(400, request(file, data, """ "color": "yes", """).getStatus());
    }
}
