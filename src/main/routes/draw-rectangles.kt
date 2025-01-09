package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.drawRectanglesOnPage
import de.martaflex.nanopdf.helpers.fromBase64
import de.martaflex.nanopdf.helpers.fromJson
import de.martaflex.nanopdf.helpers.translateCoordinates
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.common.PDRectangle
import spark.Spark.halt
import spark.Spark.post
import java.io.ByteArrayOutputStream

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

            val maxX = doc.getPage(0).mediaBox.width
            val maxY = doc.getPage(0).mediaBox.height

            for (key in values.fieldNames()) {
                val rect = values.get(key)
                val x = rect.get("x").asText();
                val y = rect.get("y").asText();
                val width = rect.get("width").asText();
                val height = rect.get("height").asText();

                val adjustedY = y.toFloat() + height.toFloat();

                val (topLeftX, topLeftY) = translateCoordinates(x.toFloat(), adjustedY, maxX, maxY);

                rectangles.add(
                    PDRectangle(
                        topLeftX,
                        topLeftY,
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
