package de.martaflex.nanopdf.helpers

import java.io.*
import java.util.Base64
import java.awt.Color

import spark.Spark.*
import spark.ResponseTransformer

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.core.JsonParseException

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

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

fun drawRectanglesOnPage (doc : PDDocument, page : PDPage, rectangles : ArrayList<PDRectangle>) {
    var contentStream = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);

    contentStream.setStrokingColor(Color.GREEN);
    for (rect in rectangles) {
        // left
        contentStream.moveTo(rect.getLowerLeftX(), rect.getLowerLeftY());
        contentStream.lineTo(rect.getLowerLeftX(), rect.getUpperRightY());
        contentStream.stroke();
        // top
        contentStream.moveTo(rect.getLowerLeftX(), rect.getUpperRightY());
        contentStream.lineTo(rect.getUpperRightX(), rect.getUpperRightY());
        contentStream.stroke();
        // right
        contentStream.moveTo(rect.getUpperRightX(), rect.getLowerLeftY());
        contentStream.lineTo(rect.getUpperRightX(), rect.getUpperRightY());
        contentStream.stroke();
        // bottom
        contentStream.moveTo(rect.getLowerLeftX(), rect.getLowerLeftY());
        contentStream.lineTo(rect.getUpperRightX(), rect.getLowerLeftY());
        contentStream.stroke();
    }
    
    contentStream.close()
}

//Rectangle has origin(0,0) in topLeft corner
//pdfbox PDRectangle() has origin(0,0) in bottomLeft corner
fun translateCoordinates(bottomLeftX: Int, bottomLeftY: Int, maxX: Int, maxY: Int): Pair<Int, Int> {
    val topLeftX = bottomLeftX
    val topLeftY = maxY - bottomLeftY
    return Pair(topLeftX, topLeftY)
}
