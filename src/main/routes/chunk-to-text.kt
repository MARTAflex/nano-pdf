package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*

import java.io.*

import spark.Spark.*

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper

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
            val doc = Loader.loadPDF(pdf);
            val stripper = PDFTextStripper();

            stripper.sortByPosition = true;

            val numberOfPages = doc.numberOfPages;
            val start = chunkIndex * chunkSize;
            var end = ((chunkIndex * chunkSize) + chunkSize) - 1;
            //for last chunk
            if (end > numberOfPages) {
                //offsetting because of arrayIndex
                end = numberOfPages-1;
            }
            //println("numberOfPages" + numberOfPages);
            //println("start" + start);
            //println("end" + end);
            for (i in start..end) {
                //println("i:" + i);
                //offsetting i because start is arrayIndex
                stripper.startPage = i+1;
                stripper.endPage = i+1;

                result.add(stripper.getText(doc));
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
