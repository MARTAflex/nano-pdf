package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.fromBase64
import de.martaflex.nanopdf.helpers.fromJson
import de.martaflex.nanopdf.helpers.translateCoordinates
import org.apache.pdfbox.Loader
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDResources
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName
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

        try {
            val doc = Loader.loadPDF(pdf);
            val buffer = ByteArrayOutputStream();

            // Remove existing AcroForm
            doc.documentCatalog.acroForm = null


            // Adobe Acrobat uses Helvetica as a default font and
            // stores that under the name '/Helv' in the resources dictionary
            val font = PDType1Font(FontName.HELVETICA)
            val resources = PDResources()
            resources.put(COSName.HELV, font)

            val acroForm = PDAcroForm(doc)
            doc.documentCatalog.acroForm = acroForm

            acroForm.defaultResources = resources
            // Acrobat sets the font size on the form level to be
            // auto sized as default. This is done by setting the font size to '0'

            acroForm.defaultAppearance = "/Helv 0 Tf 0 g"


            val page = doc.getPage(0)
            val maxX = page.mediaBox.width.toInt();
            val maxY = page.mediaBox.height.toInt();

            val x = "100"
            val y = "100"
            val width = "200"
            val height = "30"

            val adjustedY = y.toInt() + height.toInt();

            val (topLeftX, topLeftY) = translateCoordinates(x.toInt(), adjustedY, maxX, maxY);

            val textbox = PDTextField(acroForm)
            textbox.partialName = "SampleField"
            textbox.defaultAppearance = "/Helv 12 Tf 0 g"

            acroForm.fields.add(textbox)

            val widget: PDAnnotationWidget = textbox.widgets.get(0)
            val rect = PDRectangle(
                topLeftX.toFloat(),
                topLeftY.toFloat(),
                width.toFloat(),
                height.toFloat()
            )
            widget.rectangle = rect
            widget.page = page
            widget.isPrinted = true

            page.annotations.add(widget)

            textbox.q = PDVariableText.QUADDING_LEFT

            doc.save(buffer);
            doc.close();

            response.type("application/pdf");
            return buffer.toByteArray();

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
