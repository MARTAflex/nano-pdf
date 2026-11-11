package de.martaflex.nanopdf.routes

import java.io.*
import java.util.Base64
import java.util.concurrent.TimeUnit

import org.junit.*
import org.junit.Assert.*

import spark.Spark.*
import com.mashape.unirest.http.*

// /form-fill with FORM_FILL_RETURN_PDF_WITHOUT_FORM=true
class FormFillReturnPdfWithoutFormTest {
    companion object {
        @BeforeClass @JvmStatic
        fun setup () {
            port(9091)
            FormFill(returnPdfWithoutForm = true);
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
    fun PDFWithoutFormIsReturnedUnchanged () {
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

        assertEquals(200, response.getStatus());
        assertEquals("application/pdf", response.getHeaders().getFirst("Content-Type"));
        assertArrayEquals(pdf, response.getBody().readBytes());
    }

    // the flag must not change anything for pdfs that do have a form
    @Test
    fun PDFWithFormIsStillFilled () {
        val pdf64 = String(Base64.getEncoder().encode(File("resources/test/form-fill.pdf").readBytes()));

        val response = Unirest.post("http://127.0.0.1:9091/form-fill")
            .header("Content-Type", "application/json")
            .body("""
            {
                "data": { "firstname": "Alice" },
                "pdf": "${ pdf64 }"
            }
            """)
            .asBinary()

        assertEquals(200, response.getStatus());
        val doc = org.apache.pdfbox.Loader.loadPDF(response.getBody().readBytes());
        assertEquals("Alice", doc.documentCatalog.acroForm.getField("firstname").valueAsString);
        doc.close();
    }
}
