package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*

import java.io.*
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.Arrays

import org.junit.*
import org.junit.Assert.*

import spark.Spark.*
import com.mashape.unirest.http.*

class GroupPagesTest {
    companion object {
        @BeforeClass @JvmStatic
        fun setup () {
            port(9091)
            GroupPages();
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
    }

    @Test
    fun noPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/group-pages")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun emptyPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/group-pages")
            .header("Content-Type", "application/json")
            .body("")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun invalidPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/group-pages")
            .header("Content-Type", "application/json")
            .body("""{ "k1": "v1" }""")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun invalidBase64 () {
        val response = Unirest.post("http://127.0.0.1:9091/group-pages")
            .header("Content-Type", "application/json")
            .body("""
            {
                "pdf": "asdasdadasd",
                "data": {
                    "k1": "v1"
                }
            }
            """)
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun validPost () {
        val pdf = File("resources/test/group-pages.pdf").readBytes();
        val expected = File("resources/test/group-pages-out.pdf").readBytes();
        val pdf64 = String(Base64.getEncoder().encode(pdf));

        val response = Unirest.post("http://127.0.0.1:9091/group-pages")
            .header("Content-Type", "application/json")
            .body("""
            {
                "data": {
                    "test": [0]
                },
                "pdf": "${ pdf64 }"
            }
            """)
            .asString()

        assertEquals(200, response.getStatus());

        val json = fromJson(response.getBody())!!;

        val retrieved = fromBase64(json.get("test").asText())!!;

        // we need to remove the lines with /Author and /Info
        // since theese contain data that is generated dynamically whenever the pdf
        // is modified (e.g. modification timestamp)

        var se = String(expected);
        var sr = String(retrieved);

        // remove /ModDate from content
        var rx = """\/ModDate\([^\)]+\)""".toRegex();
        se = se.replace(rx, "");
        sr = sr.replace(rx, "");

        // remove id from content
        rx = """\/ID\s+\[[^\]]+\]""".toRegex();
        se = se.replace(rx, "");
        sr = sr.replace(rx, "");

        // remove /Producer which contains open office version
        rx = """\/Producer\(([^\\][^\)])+[^\\]\)""".toRegex();
        se = se.replace(rx, "");
        sr = sr.replace(rx, "");

        // remove /CreationDate from content
        rx = """\/CreationDate\([^\)]+\)""".toRegex();
        se = se.replace(rx, "");
        sr = sr.replace(rx, "");

        //File("some.pdf").writeBytes(retrieved);
        //File("se.pdf").writeText(se);
        //File("sr.pdf").writeText(sr);

        assertEquals(se, sr);
    }

}
