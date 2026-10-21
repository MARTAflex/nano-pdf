package de.martaflex.nanopdf.routes

import java.io.*
import java.util.Base64
import java.util.concurrent.TimeUnit

import org.junit.*
import org.junit.Assert.*

import spark.Spark.*
import com.mashape.unirest.http.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

class GetFormFieldsTest {
    companion object {
        @BeforeClass @JvmStatic
        fun setup () {
            port(9091)
            GetFormFields();
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

    private fun postPdf (path : String) : HttpResponse<String> {
        val pdf = File(path).readBytes();
        val pdf64 = String(Base64.getEncoder().encode(pdf));

        return Unirest.post("http://127.0.0.1:9091/get-form-fields")
            .header("Content-Type", "application/json")
            .body("""
            {
                "pdf": "${ pdf64 }"
            }
            """)
            .asString()
    }

    private fun onlyField (response : HttpResponse<String>) : JsonNode {
        assertEquals(200, response.getStatus());
        println(response.getBody());

        val json = ObjectMapper().readTree(response.getBody());
        assertEquals(1, json.size());
        return json.elements().next();
    }

    @Test
    fun emptyPostBody () {
        val response = Unirest.post("http://127.0.0.1:9091/get-form-fields")
            .header("Content-Type", "application/json")
            .body("")
            .asString()

        assertEquals(400, response.getStatus());
    }

    @Test
    fun noFormFields () {
        assertEquals(400, postPdf("resources/test/to-text.pdf").getStatus());
    }

    @Test
    fun missingPage () {
        val field = onlyField(postPdf("resources/test/get-form-fields-missing-page.pdf"));

        assertTrue(field.get("page").isNull());
        assertEquals("f", field.get("fullyQualifiedName").asText());
        assertEquals(150.0, field.get("x").asDouble(), 0.001);
    }

    @Test
    fun missingName () {
        val field = onlyField(postPdf("resources/test/get-form-fields-missing-name.pdf"));

        assertTrue(field.get("fullyQualifiedName").isNull());
        assertEquals(0, field.get("page").asInt());
    }

    @Test
    fun missingRect () {
        val field = onlyField(postPdf("resources/test/get-form-fields-missing-rect.pdf"));

        assertTrue(field.get("x").isNull());
        assertTrue(field.get("y").isNull());
        assertTrue(field.get("width").isNull());
        assertTrue(field.get("height").isNull());
        assertEquals(0, field.get("page").asInt());
    }
}
