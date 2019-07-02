package de.martaflex.nanopdf.helpers

import java.io.*
import java.util.Base64

import spark.Spark.*
import spark.ResponseTransformer

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.core.JsonParseException

data class Error(
    val status: Int,
    val message: String,
    val reason: String
);

// FIXME: json response on halt
fun toJson (o: Any) : String {
    val mapper = ObjectMapper();
    return mapper.writeValueAsString(o);
}

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
