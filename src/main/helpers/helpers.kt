package de.martaflex.nanopdf.helpers

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import spark.Spark.halt
import java.awt.Color
import java.util.*

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
fun translateCoordinates(bottomLeftX: Float, bottomLeftY: Float, maxX: Float, maxY: Float): Pair<Float, Float> {
    val topLeftX = bottomLeftX
    val topLeftY = maxY - bottomLeftY
    return Pair(topLeftX, topLeftY)
}

fun createDefaultAppearanceString(fontWeight: String, fontSize: Double, fontColor: String): String {
    val fontName = if (fontWeight == "normal") "/Helv" else "/F2"
    val colorString = when (fontColor.lowercase()) {
        "black" -> "0 0 0 rg"
        "white" -> "1 1 1 rg"
        else -> "0 g"
    }

    return "$fontName $fontSize Tf $colorString"
}
