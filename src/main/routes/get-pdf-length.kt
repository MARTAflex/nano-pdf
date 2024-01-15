package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*

import java.io.*

import spark.Spark.*
import org.apache.pdfbox.Loader

fun GetPdfLength () {
    post("/get-pdf-length", "application/json", fun(request, response) : Any {
        val body = request.body();

        if (body == "") {
            halt(400, "Empty Request Body");
        }

        // FIXME: im basically 100%sure that this will never be null
        val json = fromJson(body)!!;

        // FIXME: move this shit somewhere else
        if (json.get("pdf") == null) {
            halt(400, "pdf parameter required");
        }

        // FIXME: im basically 100%sure that this will never be null
        val pdf = fromBase64(json.get("pdf").asText())!!

        try {
            val doc = Loader.loadPDF(pdf)
            val numberOfPages = doc.numberOfPages;

            doc.close();

            response.type("application/json")
            return numberOfPages;
        }
        catch ( e : IOException) {
            println(e.message);
            halt(400, e.message)
        }

        halt(500, "this is not spposed to happen");
        // FIXME: wait what??
        return ""
    });
}
