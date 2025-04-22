package de.martaflex.nanopdf.helpers

import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File

enum class FontStyle {
    SANS, SANSBOLD,
    //SERIF, SERIFBOLD,
    //MONO, MONOBOLD,
}

object FontProvider {
    private val fonts = mapOf(
        FontStyle.SANS to "fonts/LiberationSans-Regular.ttf",
        FontStyle.SANSBOLD to "fonts/LiberationSans-Bold.ttf",
    )

    
    fun loadFont(document: PDDocument, style: FontStyle = FontStyle.SANS): PDType0Font {
        val fontStream = javaClass.classLoader.getResourceAsStream(fonts[style])
        return PDType0Font.load(document, fontStream, true)
    }
}

