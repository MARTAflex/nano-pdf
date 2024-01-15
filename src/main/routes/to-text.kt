package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*

import java.io.*

import spark.Spark.*

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper

fun ToText () {
    post("/to-text", "application/json", fun(request, response) : Any {
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
        val result = ArrayList<String>()

        try {
            val doc = Loader.loadPDF(pdf);
            val stripper = PDFTextStripper();

            stripper.sortByPosition = true;

            for (i in 1..doc.numberOfPages) {
                stripper.startPage = i;
                stripper.endPage = i;

                result.add(stripper.getText(doc));
            }

            response.type("application/json")
            return toJson(result)
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
