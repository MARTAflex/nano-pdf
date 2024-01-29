package de.martaflex.nanopdf.routes

import java.io.*
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.Arrays

import org.junit.*
import org.junit.Assert.*

import spark.Spark.*
import com.mashape.unirest.http.*

class GetFirstPageAsImageTest {
    companion object {
        @BeforeClass @JvmStatic
        fun setup () {
            port(9091)
            GetFirstPageAsImage();
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
        val response = Unirest.post("http://127.0.0.1:9091/get-first-page-as-image")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun emptyPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/get-first-page-as-image")
            .header("Content-Type", "application/json")
            .body("")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun invalidPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/get-first-page-as-image")
            .header("Content-Type", "application/json")
            .body("""{ "k1": "v1" }""")
            .asString()

        assertEquals(400, response.getStatus());
        println(response.getBody());
    }

    @Test
    fun invalidBase64 () {
        val response = Unirest.post("http://127.0.0.1:9091/get-first-page-as-image")
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

        val response = Unirest.post("http://127.0.0.1:9091/get-first-page-as-image")
            .header("Content-Type", "application/json")
            .body("""
            {
                "pdf": "${ pdf64 }"
            }
            """)
            .asString()

        assertEquals(200, response.getStatus())

        val retrieved = response.getBody()

        val expected = """"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lA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8A5QMA5QMA5QMA5QMA5QMA5QMA5QMA5QMA5QMA5QMA5QMA5QNA+bwEACgfACgfACgfACgfACgfACgfACgfACgfACgfACgfACgfACgfAMoHAMoHAMoHAMoHAMoHAMoHAMoHAMoHAMoHAMoHAMoHAMoHAMoHgPIBgPIBgPIBgPIBgPIBgPIBgPIBgPIBgPIBgPIBgPIBgPIBgPIBoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AygcAygcAygcAygcAygcAygcAygcAygcAygcAygcAygcAygcAygeA8gGA8gGA8gGA8gGA8gGA8gGA8gGA8gGA8gGA8gGA8gGA8gGA8gGgfACgfACgfACgfACgfACgfACgfACgfACgfACgfACgfACgfACgfAAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwDKBwDKBwDKBwDKBwDKBwDKBwDKBwDKBwDKBwDKBwDKBwDKB4DyeQkAUD4AUD4AUD4AUD4AUD4AUD4AUD4AUD4AUD4AUD4AUD4AUD4AlA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8A5QMA5QMA5QMA5QMA5QMA5QMA5QMA5QMA5QMA5QMA5QMA5QMA5QNA+QBA+QBA+QBA+QBA+QBA+QBA+QBA+QBA+QBA+QBA+QBA+QBA+QBQPgBQPgBQPgBQPgBQPgBQPgBQPgBQPgBQPgBQPgBQPgBQPgBQPgCUDwCUDwCUDwCUDwCUDwCUDwCUDwCUDwCUDwCUDwCUDwCUDwCUDwDlAwDlAwDlAwDlAwDlAwDlAwDlAwDlAwDlAwDlAwDlAwDlAwDlA0D5AED5AED5AED5AED5AED5AED5AED5AED5AED5AED5AED5AED5AFA+AFA+AFA+AFA+AFA+AFA+AFA+AFA+AFA+AFA+AFA+AFA+AFA+AJQPAJQPAJQPAJQPAJQPAJQPAJQPAJQPAJQPAJQPAJQPAJQPAOXzEgCgfACgfACgfACgfACgfACgfACgfACgfACgfACgfACgfACgfAAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwDKBwDKBwDKBwDKBwDKBwDKBwDKBwDKBwDKBwDKBwDKBwDKBwDKB4DyAYDyAYDyAYDyAYDyAYDyAYDyAYDyAYDyAYDyAYDyAYDyAYDyAaB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8ACgfACgfACgfACgfACgfACgfACgfACgfACgfACgfACgfACgfACgfAMoHAMoHAMoHAMoHAMoHAMoHAMoHAMoHAMoHAMoHAMoHAMoHAMoHgPIBgPIBgPIBgPIBgPIBgPIBgPIBgPIBgPIBgPIBgPIBgPIBgPIBoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAoHwAKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AKB8AyuclAED5AED5AED5AED5AED5AED5AED5AED5AED5AED5AED5AED5AFA+AFA+AFA+AFA+AFA+AFA+AFA+AFA+AFA+AFA+AFA+AFA+AFA+AJQPAJQPAJQPAJQPAJQPAJQPAJQPAJQPAJQPAJQPAJQPAJQPAJQPAOUDAOUDAOUDAOUDAOUDAOUDAOUDAOUDAOUDAOUDAOUDAOUDAOUDQPkAQPkAQPkAQPkAQPkAQPkAQPkAQPkAQPkAQPkAQPkAQPkAQPkAUD4AUD4AUD4AUD4AUD4AUD4AUD4AUD4AUD4AUD4AUD4AUD4AUD4AlA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8AlA8A5QOAZ+4/sUhhF8ZlnkkAAAAASUVORK5CYII=""""
        assertEquals(expected, retrieved)
    }

}
