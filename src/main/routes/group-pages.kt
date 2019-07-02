package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*

import java.io.*
import java.util.Base64

import spark.Spark.*
import spark.ResponseTransformer

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.core.JsonParseException

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfCopy;

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
            val reader = PdfReader(pdf)
            val values = json.get("data")
            var result: MutableMap<String, String> = mutableMapOf()

            for (key in values.fieldNames()) {
                var buffer = ByteArrayOutputStream()
                var document = Document()
                var copy = PdfCopy(document, buffer)
                var pages = values.get(key)

                document.open()
                for (page in pages) {
                    //expecting pagenumbers from array; so we offset here
                    copy.addPage(copy.getImportedPage(reader, page.asInt()+1))
                }
                document.close()

                result[key] = toBase64(buffer.toByteArray())
            }

            reader.close()

            response.type("application/json")
            return toJson(result)
        }
        catch ( e : InvalidPdfException) {
            println(e.message);
            halt(400, e.message)
        }

        halt(500, "this is not spposed to happen");
        // FIXME: wait what??
        return ""
    });
}
