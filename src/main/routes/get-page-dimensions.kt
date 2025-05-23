package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*

import java.io.*
import java.util.Base64

import spark.Spark.*

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer
import javax.imageio.ImageIO

fun GetPageDimensions () {
    post("/get-page-dimensions", "application/json", fun(request, response) : Any {
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
        val result = ArrayList<FloatArray>()

        try {
            val doc = Loader.loadPDF(pdf);

            for (page in doc.pages.iterator()) {
                result.add(page.mediaBox.cosArray.toFloatArray())
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
