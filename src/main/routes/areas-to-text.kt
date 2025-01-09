package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripperByArea
import spark.Spark.*
import java.awt.Rectangle


fun AreasToText () {
    post("/areas-to-text", "application/json", fun(request, response) : Any {
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
        val pdf = fromBase64(json.get("pdf").asText())!!
        val values = json.get("data");
        val result = ArrayList<MutableMap<String,String>>();

        try {
            val doc = Loader.loadPDF(pdf);
            val stripper = PDFTextStripperByArea();
            stripper.sortByPosition = true;

            //prepare areas
            //val rectangles: MutableMap<String, PDRectangle> = mutableMapOf<String, PDRectangle>()
            for (key in values.fieldNames()) {
                val base = values.get(key)
                val x = base.get("x").asText();
                val y = base.get("y").asText();
                val width = base.get("width").asText();
                val height = base.get("height").asText();

                val rect = Rectangle(x.toInt(), y.toInt(), width.toInt(), height.toInt())
                stripper.addRegion(
                    key,
                    rect
                );
            }

            for (page in doc.pages.iterator()) {
                stripper.extractRegions( page )
                val texts = mutableMapOf<String,String>()
                for (key in values.fieldNames()) {
                    texts[key] = stripper.getTextForRegion(key);
                }
                result.add(texts);
            }

            response.type("application/json")
            return toJson(result)
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
