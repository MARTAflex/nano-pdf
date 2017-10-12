package de.martaflex.nanopdf.routes

import java.io.*
import java.util.Base64

import spark.Spark.*
import spark.ResponseTransformer

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.core.JsonParseException

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

val toJson = fun (o: Any) : String {
    val mapper = ObjectMapper();
    return mapper.writeValueAsString(o);
}

// FIXME: json response on halt
data class Error(
    val status: Int,
    val message: String,
    val reason: String
);

// FIXME: move helper functions somewhere else
fun fromJson (body : String) : JsonNode? {
    try {
        val mapper = ObjectMapper();
        return mapper.readTree(body);
    }
    catch (e : JsonParseException) {
        halt(400, e.message)
    }
    return null;
}

fun fromBase64 (s : String) : ByteArray? {
    try {
        return Base64.getDecoder().decode(s);
    }
    catch (e : IllegalArgumentException) {
        halt(400, e.message)
    }
    return null;
}

fun toBase64 (bytes : ByteArray) : String {
    return String(Base64.getEncoder().encode(bytes));
}

fun FormFill () {
    post("/form-fill", "application/json", fun(request, response) : Any {
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
            val reader = PdfReader(pdf);
            val buffer = ByteArrayOutputStream();
            val stamper = PdfStamper(reader, buffer);
            
            val form = stamper.getAcroFields();
            form.setGenerateAppearances(true); // if not set the fields will be empty when flattening
            val values = json.get("data");

            for (key in values.fieldNames()) {
                if (form.getField(key) != null) {
                    form.setField(key, values.get(key).asText(""));
                }
            }

            stamper.setFormFlattening(json.get("flatten")?.asBoolean() ?: false);
            stamper.close();
            
            response.type("application/pdf");
            return buffer.toByteArray();
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
