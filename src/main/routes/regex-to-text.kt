package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.PDFTextStripperByArea
import spark.Spark.*
import java.awt.Rectangle
import java.util.ArrayList
import java.util.regex.MatchResult
import java.util.regex.Pattern

data class RegexData(val key: String, val regex: Pattern)

fun RegexToText() {
    post("/regex-to-text", "application/json", fun(request, response) : Any {
        val body = request.body();

        if (body == "") {
            halt(400, "Empty Request Body");
        }

        // FIXME: im basically 100%sure that this will never be null
        val json = fromJson(body)!!;

        if (json.get("pdf") == null) {
            halt(400, "pdf parameter required");
        }

        if (json.get("data") == null) {
            halt(400, "data parameter required");
        }

        if (!(json.get("data").isObject())) {
            halt(400, "data parameter must be a list");
        }

        // FIXME: im basically 100%sure that this will never be null
        val pdf = fromBase64(json.get("pdf").asText())!!
        val values = json.get("data");

        try {
            val doc = Loader.loadPDF(pdf);
            val stripper = PDFTextStripper();
            stripper.sortByPosition = true;

            //regex patterns
            val regexPatterns: MutableMap<String, Regex> = mutableMapOf<String, Regex>()

            for (key in values.fieldNames()) {
                val regexString = values.get(key).asText()
                regexPatterns[key] = Regex(regexString)
            }

            val results = ArrayList<MutableMap<String, List<kotlin.text.MatchResult>>>()


            // Process each page and extract matches for each regex pattern
            for (i in 1..doc.numberOfPages) {
                stripper.startPage = i
                stripper.endPage = i
                val text = stripper.getText(doc)
                val matches: MutableMap<String, List<kotlin.text.MatchResult>> = mutableMapOf<String, List<kotlin.text.MatchResult>>()

                for (regex in regexPatterns.iterator()) {
                    matches[regex.key] = regex.value.findAll(text).toList()
                }
                results.add(matches)

            }

            response.type("application/json")
            return toJson(results)

        } catch (e: Exception) {
            println(e.message)
            halt(400, e.message)
        }

        halt(500, "this is not supposed to happen")
        // FIXME: wait what??
        return ""
    });
}
