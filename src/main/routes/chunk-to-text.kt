package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*

import java.io.*
import java.util.Base64

import spark.Spark.*
import spark.ResponseTransformer

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.core.JsonParseException

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;

// #2 is another variant of parsing; in this version the order of the resultingText
// is more line based
// #2 import com.itextpdf.text.pdf.parser.PdfTextExtractor;

fun ChunkToText () {
    post("/chunk-to-text", "application/json", fun(request, response) : Any {
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
        val data = json.get("data");
        val chunkSize = data["chunkSize"].asInt();
        val chunkIndex = data["chunkIndex"].asInt();
        val result = ArrayList<String>();

        try {
            val reader = PdfReader(pdf)
            val parser = PdfReaderContentParser(reader);
            val numberOfPages = reader.getNumberOfPages();
            var start = chunkIndex * chunkSize;
            var end = ((chunkIndex * chunkSize) + chunkSize) - 1;
            //for last chunk
            if (end > numberOfPages) {
                //offsetting because of arrayIndex
                end = numberOfPages-1;
            }
            println("numberOfPages" + numberOfPages);
            println("start" + start);
            println("end" + end);
            for (i in start..end) {
                println("i:" + i);
                //offsetting i because start is arrayIndex
                val strategy = parser.processContent(i+1, SimpleTextExtractionStrategy());
                result.add(strategy.getResultantText());
                // #2  result.add(PdfTextExtractor.getTextFromPage(reader, i))
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
