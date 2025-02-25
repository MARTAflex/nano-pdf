package de.martaflex.nanopdf.routes

import com.fasterxml.jackson.databind.ObjectMapper
import de.martaflex.nanopdf.helpers.*

import java.io.*

import spark.Spark.*

import org.apache.pdfbox.Loader
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocument

fun GroupPages () {
    post("/group-pages", "application/json", fun(request, response) : Any {
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
        try {
            val doc = Loader.loadPDF(pdf);
            val values = json.get("data")
            val result: MutableMap<String, String> = mutableMapOf()

            for (key in values.fieldNames()) {
                val newDocument = PDDocument();
                val outputBuffer = ByteArrayOutputStream()
                //this is 0-based array
                val pages = values.get(key);
                for (pageNumber in pages) {
                    newDocument.importPage(doc.getPage(pageNumber.asInt()))
                }
                newDocument.save(outputBuffer)
                newDocument.close()

                result[key] = toBase64(outputBuffer.toByteArray())
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
