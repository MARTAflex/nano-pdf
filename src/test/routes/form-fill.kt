package de.martaflex.nanopdf.routes

import java.io.*
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.Arrays

import org.junit.*
import org.junit.Assert.*

import spark.Spark.*
import com.mashape.unirest.http.*

class FormFillTest {
    companion object {
        @BeforeClass @JvmStatic
        fun setup () {
            port(9091)
            FormFill();
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
        val response = Unirest.post("http://127.0.0.1:9091/form-fill")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun emptyPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/form-fill")
            .header("Content-Type", "application/json")
            .body("")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun invalidPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/form-fill")
            .header("Content-Type", "application/json")
            .body("""{ "k1": "v1" }""")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun invalidBase64 () {
        val response = Unirest.post("http://127.0.0.1:9091/form-fill")
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
    fun PDFWithoutForm () {
        val pdf = File("resources/test/form-fill-out.pdf").readBytes();
        val pdf64 = String(Base64.getEncoder().encode(pdf));

        val response = Unirest.post("http://127.0.0.1:9091/form-fill")
            .header("Content-Type", "application/json")
            .body("""
            {
                "data": {
                    "firstname": "Periódicos",
                    "lastname": "Españoles"
                },
                "flatten": true,
                "pdf": "${ pdf64 }"
            }
            """)
            .asBinary()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun validPost () {
        val pdf = File("resources/test/form-fill.pdf").readBytes();
        val expected = File("resources/test/form-fill-out.pdf").readBytes();
        val pdf64 = String(Base64.getEncoder().encode(pdf));

        val response = Unirest.post("http://127.0.0.1:9091/form-fill")
            .header("Content-Type", "application/json")
            .body("""
            {
                "data": {
                    "firstname": "Periódicos",
                    "lastname": "Españoles"
                },
                "flatten": true,
                "pdf": "${ pdf64 }"
            }
            """)
            .asBinary()

        assertEquals(200, response.getStatus());

        // readbytes returns empty when called multiple times; is that intended?
        val retrieved = response.getBody().readBytes();


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

        //File("some.pdf").writeBytes(retrieved);
        //File("se.pdf").writeText(se);
        //File("sr.pdf").writeText(sr);

        assertEquals(se, sr);
    }

}
