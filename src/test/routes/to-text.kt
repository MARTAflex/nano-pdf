package de.martaflex.nanopdf.routes

import java.io.*
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.Arrays

import org.junit.*
import org.junit.Assert.*

import spark.Spark.*
import com.mashape.unirest.http.*

class ToTextTest {
    companion object {
        @BeforeClass @JvmStatic
        fun setup () {
            port(9091)
            ToText();
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
        val response = Unirest.post("http://127.0.0.1:9091/to-text")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun emptyPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/to-text")
            .header("Content-Type", "application/json")
            .body("")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun invalidPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/to-text")
            .header("Content-Type", "application/json")
            .body("""{ "k1": "v1" }""")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun invalidBase64 () {
        val response = Unirest.post("http://127.0.0.1:9091/to-text")
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
        val pdf = File("resources/test/to-text.pdf").readBytes();

        val pdf64 = String(Base64.getEncoder().encode(pdf));

        val response = Unirest.post("http://127.0.0.1:9091/to-text")
            .header("Content-Type", "application/json")
            .body("""
            {
                "pdf": "${ pdf64 }"
            }
            """)
            .asString()

        assertEquals(200, response.getStatus())

        val retrieved = response.getBody()

        val expected = """["Firstname: Periódicos\nLastname: Españoles\n","Firstname: Periódicos\nLastname: Españoles\n"]"""
        assertEquals(expected, retrieved)
    }

}
