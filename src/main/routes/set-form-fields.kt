package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.createDefaultAppearanceString
import de.martaflex.nanopdf.helpers.fromBase64
import de.martaflex.nanopdf.helpers.fromJson
import de.martaflex.nanopdf.helpers.FontProvider
import de.martaflex.nanopdf.helpers.FontStyle
import org.apache.pdfbox.Loader
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDResources
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField
import org.apache.pdfbox.pdmodel.interactive.form.PDVariableText
import spark.Spark.halt
import spark.Spark.post
import java.io.ByteArrayOutputStream
import java.io.IOException


fun SetFormFields () {
    post("/set-form-fields", "application/json", fun(request, response) : Any {
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

        if (json.get("data") == null) {
            halt(400, "data parameter required")
        }

        if (!(json.get("data").isObject() )) {
            halt(400, "data parameter must be an object")
        }

        // FIXME: im basically 100%sure that this will never be null
        val pdf = fromBase64(json.get("pdf").asText())!!

        try {
            val doc = Loader.loadPDF(pdf)
            val buffer = ByteArrayOutputStream()

            // Remove existing AcroForm
            doc.documentCatalog.acroForm = null
            // Remove annotation boxes of formfields
            for (page in doc.getPages().iterator()) {
                val annotations = page.getAnnotations()
                val newAnnotations = ArrayList<PDAnnotation>()

                for (annotation in annotations) {
                    if (!(annotation is PDAnnotationWidget)) {
                        newAnnotations.add(annotation)
                    }
                }
                page.annotations = newAnnotations
            }

            // FIXME: don't know if that's needed here
            // there seems to be standart available fonts
            // https://stackoverflow.com/questions/30181250/java-pdfbox-setting-custom-font-for-a-few-fields-in-pdf-form

            // Adobe Acrobat uses Helvetica as a default font and
            // stores that under the name '/Helv' in the resources dictionary
            //val helveticaFont = PDType1Font(FontName.HELVETICA)
            //val helveticaBoldFont = PDType1Font(FontName.HELVETICA_BOLD)
            val liberationSans = FontProvider.loadFont(doc, FontStyle.SANS)
            val liberationSansBold = FontProvider.loadFont(doc, FontStyle.SANSBOLD)


            val acroForm = PDAcroForm(doc)
            doc.documentCatalog.acroForm = acroForm

            var resources = acroForm.defaultResources
            if (resources == null) {
                resources = PDResources()
            }
            //resources.put(COSName.HELV, helveticaFont)
            // FIXME: helveticaBoldFont should be there as /HeBo
            //        but since is not in COSName its added plain
            //        gets assigned /F2 font
            //resources.add(helveticaBoldFont)
            val sansName = resources.add(liberationSans) 
            val sansBoldName = resources.add(liberationSansBold) 


            acroForm.defaultResources = resources
            // Acrobat sets the font size on the form level to be
            // auto sized as default. This is done by setting the font size to '0'
            acroForm.defaultAppearance = sansName.name + " 0 Tf 0 g"

            val fields = json.get("data")

            for (fieldName in fields.fieldNames()) {
                val fieldValues = fields.get(fieldName)
                val fullyQualifiedName = fieldValues.get("fullyQualifiedName").asText()
                val x = fieldValues.get("x").asText()
                val y = fieldValues.get("y").asText()
                val width = fieldValues.get("width").asText()
                val height = fieldValues.get("height").asText()
                val type = fieldValues.get("type").asText()
                val pageValue = fieldValues.get("page").asText()
                val fontWeight = fieldValues.get("fontWeight").asText()
                val fontColor = fieldValues.get("fontColor").asText()
                val fontSize = fieldValues.get("fontSize").asText()
                val quadding = fieldValues.get("quadding").asText()


                val page = doc.getPage(pageValue.toInt())
                // val maxX = page.mediaBox.width
                // val maxY = page.mediaBox.height

                // val adjustedY = y.toFloat() + height.toFloat()
                // val (topLeftX, topLeftY) = translateCoordinates(x.toFloat(), adjustedY, maxX, maxY)

                // TODO: implement extra fieldtypes
                if (type != "text") {
                    continue
                }
                val textbox = PDTextField(acroForm)
                textbox.partialName = fullyQualifiedName
                
                val fontName = if (fontWeight == "normal") sansName.name else sansBoldName.name

                textbox.defaultAppearance = createDefaultAppearanceString(
                    fontName,
                    fontSize.toDouble(),
                    fontColor,
                )

                acroForm.fields.add(textbox)

                val widget: PDAnnotationWidget = textbox.widgets.get(0)
                val rect = PDRectangle(
                    x.toFloat(),
                    y.toFloat(),
                    width.toFloat(),
                    height.toFloat()
                )

                widget.rectangle = rect
                widget.page = page
                widget.isPrinted = true

                page.annotations.add(widget)

                // FIXME is that right? or can input int directly
                textbox.q = when (quadding.toInt()) {
                    0 -> PDVariableText.QUADDING_LEFT
                    1 -> PDVariableText.QUADDING_CENTERED
                    2 -> PDVariableText.QUADDING_RIGHT
                    else -> PDVariableText.QUADDING_LEFT
                }

                // if needed prefilled formfield
                // textbox.value = value
            }

            doc.save(buffer)
            doc.close()

            response.type("application/pdf")
            return buffer.toByteArray()

        }
        catch ( e : IOException) {
            println(e.message)
            halt(400, e.message)
        }

        halt(500, "this is not spposed to happen")
        // FIXME: wait what??
        return ""
    });
}
