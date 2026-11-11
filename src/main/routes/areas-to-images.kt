package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.*
import java.util.Base64

import spark.Spark.*

import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import javax.imageio.ImageIO

const val AREAS_TO_IMAGES_DEFAULT_DPI = 300f
const val AREAS_TO_IMAGES_MIN_DPI = 36f
// an A4 page at 600 dpi is ~5000x7000 px, keep memory per request bounded
const val AREAS_TO_IMAGES_MAX_DPI = 600f
const val AREAS_TO_IMAGES_MAX_PADDING = 200

private data class Area(val name: String, val page: Int, val x: Double, val y: Double, val width: Double, val height: Double)

fun AreasToImages () {
    post("/areas-to-images", "application/json", fun(request, response) : Any {
        val body = request.body();

        if (body == "") {
            halt(400, "Empty Request Body");
        }

        // FIXME: im basically 100%sure that this will never be null
        val json = fromJson(body)!!;

        if (json.get("pdf") == null) {
            halt(400, "pdf parameter required");
        }

        if (json.get("data") == null) {
            halt(400, "data parameter required");
        }

        if (!json.get("data").isObject()) {
            halt(400, "data parameter must be an object");
        }

        val dpiNode = json.get("dpi")
        if (dpiNode != null && !dpiNode.isNumber()) {
            halt(400, "dpi parameter must be a number");
        }
        val dpi = dpiNode?.floatValue() ?: AREAS_TO_IMAGES_DEFAULT_DPI
        if (dpi < AREAS_TO_IMAGES_MIN_DPI || dpi > AREAS_TO_IMAGES_MAX_DPI) {
            halt(400, "dpi parameter must be between ${AREAS_TO_IMAGES_MIN_DPI.toInt()} and ${AREAS_TO_IMAGES_MAX_DPI.toInt()}");
        }

        val paddingNode = json.get("padding")
        if (paddingNode != null && !paddingNode.isIntegralNumber()) {
            halt(400, "padding parameter must be an integer");
        }
        val padding = paddingNode?.intValue() ?: 0
        if (padding < 0 || padding > AREAS_TO_IMAGES_MAX_PADDING) {
            halt(400, "padding parameter must be between 0 and $AREAS_TO_IMAGES_MAX_PADDING");
        }

        val colorNode = json.get("color")
        if (colorNode != null && !colorNode.isBoolean()) {
            halt(400, "color parameter must be a boolean");
        }
        val imageType = if (colorNode?.asBoolean() == true) ImageType.RGB else ImageType.GRAY

        val areas = ArrayList<Area>()
        val data = json.get("data")
        for (name in data.fieldNames()) {
            val a = data.get(name)
            if (!a.isObject()) {
                halt(400, "area '$name' must be an object");
            }
            for (key in listOf("x", "y", "width", "height")) {
                if (a.get(key) == null || !a.get(key).isNumber()) {
                    halt(400, "area '$name': $key must be a number");
                }
            }
            val pageNode = a.get("page")
            if (pageNode != null && !pageNode.isIntegralNumber()) {
                halt(400, "area '$name': page must be an integer");
            }
            val area = Area(
                name,
                pageNode?.intValue() ?: 0,
                a.get("x").doubleValue(),
                a.get("y").doubleValue(),
                a.get("width").doubleValue(),
                a.get("height").doubleValue()
            )
            if (area.width <= 0 || area.height <= 0) {
                halt(400, "area '$name': width and height must be greater than 0");
            }
            areas.add(area)
        }

        // FIXME: im basically 100%sure that this will never be null
        val pdf = fromBase64(json.get("pdf").asText())!!

        try {
            Loader.loadPDF(pdf).use { doc ->
                for (area in areas) {
                    if (area.page < 0 || area.page >= doc.numberOfPages) {
                        halt(400, "area '${area.name}': page ${area.page} does not exist, pdf has ${doc.numberOfPages} page(s)");
                    }
                }

                val renderer = PDFRenderer(doc)
                val scale = dpi / 72.0
                val images = HashMap<String, String>()

                // render every needed page only once
                for ((pageIndex, pageAreas) in areas.groupBy { it.page }) {
                    val pageImage = renderer.renderImageWithDPI(pageIndex, dpi, imageType)

                    for (area in pageAreas) {
                        // clamp to the page, rectangles partially outside are cut off
                        val left = Math.max(0, Math.round(area.x * scale).toInt())
                        val top = Math.max(0, Math.round(area.y * scale).toInt())
                        val right = Math.min(pageImage.width, Math.round((area.x + area.width) * scale).toInt())
                        val bottom = Math.min(pageImage.height, Math.round((area.y + area.height) * scale).toInt())

                        if (right <= left || bottom <= top) {
                            halt(400, "area '${area.name}' is outside of page ${area.page}");
                        }

                        val crop = pageImage.getSubimage(left, top, right - left, bottom - top)
                        images[area.name] = toBase64(toPng(withPadding(crop, padding)))
                    }
                }

                // same order as in the request
                val result = LinkedHashMap<String, String>()
                for (area in areas) {
                    result[area.name] = images[area.name]!!
                }

                response.type("application/json")
                return toJson(result)
            }
        }
        catch ( e : IOException) {
            println(e.message);
            halt(400, e.message)
        }

        halt(500, "this is not spposed to happen");
        return ""
    });
}

// copies the image onto a white canvas with `padding` px on every side
private fun withPadding (image: BufferedImage, padding: Int) : BufferedImage {
    val type = if (image.type == BufferedImage.TYPE_CUSTOM) BufferedImage.TYPE_INT_RGB else image.type
    val out = BufferedImage(image.width + 2 * padding, image.height + 2 * padding, type)
    val g = out.createGraphics()
    try {
        g.color = Color.WHITE
        g.fillRect(0, 0, out.width, out.height)
        g.drawImage(image, padding, padding, null)
    }
    finally {
        g.dispose()
    }
    return out
}

private fun toPng (image: BufferedImage) : ByteArray {
    val out = ByteArrayOutputStream()
    ImageIO.write(image, "png", out)
    return out.toByteArray()
}
