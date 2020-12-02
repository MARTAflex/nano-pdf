package de.martaflex.nanopdf.routes

import java.io.*
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.Arrays

import org.junit.*
import org.junit.Assert.*

import spark.Spark.*
import com.mashape.unirest.http.*

class ChunkToTextTest {
    companion object {
        @BeforeClass @JvmStatic
        fun setup () {
            port(9091)
            ChunkToText();
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
        val response = Unirest.post("http://127.0.0.1:9091/chunk-to-text")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun emptyPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/chunk-to-text")
            .header("Content-Type", "application/json")
            .body("")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun invalidPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/chunk-to-text")
            .header("Content-Type", "application/json")
            .body("""{ "k1": "v1" }""")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun invalidBase64 () {
        val response = Unirest.post("http://127.0.0.1:9091/chunk-to-text")
            .header("Content-Type", "application/json")
            .body("""
            {
                "pdf": "asdasdadasd",
                "data": {
                    "chunkSize": 0,
                    "chunkIndex": 1
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

        val response = Unirest.post("http://127.0.0.1:9091/chunk-to-text")
            .header("Content-Type", "application/json")
            .body("""
            {
                "pdf": "${ pdf64 }",
                "data": {
                    "chunkSize": 1,
                    "chunkIndex": 1
                }
            }
            """)
            .asString()

        assertEquals(200, response.getStatus())

        val retrieved = response.getBody()

        val expected =
        """["Firstname:\nLastname:\nPeriódicos\nEspañoles"]"""

        assertEquals(expected, retrieved)
    }

}
