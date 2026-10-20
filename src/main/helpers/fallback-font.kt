package de.martaflex.nanopdf.helpers

import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDResources
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.pdmodel.interactive.form.PDVariableText
import java.io.IOException

private val DA_FONT = Regex("""/([^\s/]+)\s+([\d.]+)\s+Tf""")
private val FALLBACK_NAME = COSName.getPDFName("NanoPdfFallback")

// Makes sure pdfbox can generate the appearance for a field value:
// - fonts referenced by a /DA but missing in the AcroForm /DR are mapped to the fallback font
// - if the /DA font can't encode the value, the field is switched to the fallback font
// the font is always embedded completely: pdfbox does not subset fonts that are only used
// in form field appearances (flattened output showed garbage), and later edits in a pdf
// viewer need all glyphs anyway
class FallbackFont(private val doc: PDDocument, private val acroForm: PDAcroForm) {
    private var font: PDType0Font? = null

    private fun resources(): PDResources {
        var resources = acroForm.defaultResources
        if (resources == null) {
            resources = PDResources()
            acroForm.defaultResources = resources
        }
        return resources
    }

    private fun fallback(): PDType0Font {
        if (font == null) {
            val stream = javaClass.classLoader.getResourceAsStream("fonts/LiberationSans-Regular.ttf")
            font = PDType0Font.load(doc, stream, false)
        }
        return font!!
    }

    private fun lookup(name: String): PDFont? {
        return try {
            resources().getFont(COSName.getPDFName(name))
        } catch (e: IOException) {
            null
        }
    }

    private fun canEncode(font: PDFont, text: String): Boolean {
        // line breaks and tabs are handled by the appearance generator, not the font
        return try {
            font.encode(text.replace(Regex("[\\r\\n\\t\\u2028\\u2029]"), " "))
            true
        } catch (e: IllegalArgumentException) {
            false
        } catch (e: IOException) {
            false
        }
    }

    private fun useFallback(da: String): String {
        return DA_FONT.replace(da) { "/${FALLBACK_NAME.name} ${it.groupValues[2]} Tf" }
    }

    fun prepare(field: PDVariableText, text: String) {
        // widgets may carry their own /DA which pdfbox prefers over the field one
        val appearances = mutableListOf(field.defaultAppearance)
        for (widget in field.widgets) {
            appearances.add(widget.cosObject.getString(COSName.DA))
        }

        var needsFallback = false
        for (da in appearances.filterNotNull()) {
            val fontName = DA_FONT.find(da)?.groupValues?.get(1) ?: continue
            val font = lookup(fontName)

            if (font == null) {
                resources().put(COSName.getPDFName(fontName), fallback())
            }
            else if (!canEncode(font, text)) {
                needsFallback = true
            }
        }

        if (needsFallback) {
            resources().put(FALLBACK_NAME, fallback())
            field.defaultAppearance?.let { field.defaultAppearance = useFallback(it) }
            for (widget in field.widgets) {
                val da = widget.cosObject.getString(COSName.DA) ?: continue
                widget.cosObject.setString(COSName.DA, useFallback(da))
            }
        }
    }
}
