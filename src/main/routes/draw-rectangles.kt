package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*

import java.io.*
import java.util.Base64

import spark.Spark.*
import spark.ResponseTransformer

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.core.JsonParseException

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

fun DrawRectangles () {
    post("/draw-rectangles", "application/json", fun(request, response) : Any {
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

        if (json.get("data") == null) {
            halt(400, "data parameter required");
        }

        if (!(json.get("data").isObject() )) {
            halt(400, "data parameter must be an object");
        }

        // FIXME: im basically 100%sure that this will never be null
        val pdf = fromBase64(json.get("pdf").asText())!!;
        val values = json.get("data");

        try {
            val doc = Loader.loadPDF(pdf);
            var buffer = ByteArrayOutputStream()
            var rectangles: ArrayList<PDRectangle> = ArrayList<PDRectangle>()

            for (key in values.fieldNames()) {
                var rect = values.get(key)
                var x = rect.get("x").asText();
                var y = rect.get("y").asText();
                var width = rect.get("width").asText();
                var height = rect.get("height").asText();
                rectangles.add(
                    PDRectangle( 
                        x.toFloat(),
                        y.toFloat(),
                        width.toFloat(),
                        height.toFloat()
                    )
                )
            }
            

            for (page in doc.getPages().iterator()) {
                drawRectanglesOnPage(doc,page,rectangles);
            }

            doc.save(buffer);

            response.type("application/pdf")
            return buffer.toByteArray()
        }
        catch ( e : Exception) {
            println(e.message);
            halt(400, e.message)
        }

        halt(500, "this is not spposed to happen");
        // FIXME: wait what??
        return ""
    });
}
