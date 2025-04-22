package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDResources
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont

import spark.Spark.*
import java.awt.GraphicsEnvironment


fun AvailableFonts () {
    post("/available-fonts", "application/json", fun(request, response) : Any {
        val body = request.body()

        if (body == "") {
            halt(400, "Empty Request Body")
        }

        // FIXME: im basically 100%sure that this will never be null
        val json = fromJson(body)!!

        // FIXME: move this shit somewhere else
        if (json.get("pdf") == null) {
            halt(400, "pdf parameter required")
        }

        // FIXME: im basically 100%sure that this will never be null
        val pdf = fromBase64(json.get("pdf").asText())!!
        val result = mutableMapOf<String, MutableSet<String>>()
        
        try {
            val doc = Loader.loadPDF(pdf)
            val embeddedPageFonts = mutableSetOf<String>()
            
            for (page in doc.pages) {
                val resources = page.resources

                for (fontKey in resources.fontNames) {
                    val font = resources.getFont(fontKey)
                    embeddedPageFonts.add(font.name)
                }
            }
            
            result["embeddedPageFonts"] = embeddedPageFonts

            val embeddedAcroFormFonts = mutableSetOf<String>()
            val acroForm = doc.documentCatalog.acroForm
            
            if( acroForm != null) {
                val resources = acroForm.defaultResources
                if( resources != null && resources.fontNames != null) {
                    for (fontKey in resources.fontNames) {
                        val font = resources.getFont(fontKey)
                        embeddedAcroFormFonts.add(font.name)
                    }
                }
            }
            
            result["embeddedAcroFormFonts"] = embeddedAcroFormFonts



            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val localFonts = mutableSetOf<String>()
            
            for (fontKey in ge.availableFontFamilyNames) {
                localFonts.add(fontKey)
            }
            
            result["localFonts"] = localFonts

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
