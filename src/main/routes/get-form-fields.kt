package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.fromBase64
import de.martaflex.nanopdf.helpers.fromJson
import de.martaflex.nanopdf.helpers.toJson
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget
import org.apache.pdfbox.pdmodel.interactive.form.*
import spark.Spark.halt
import spark.Spark.post
import java.io.IOException


data class FieldInfo(
    val name: String,
    val value: String,
    val fieldFlags: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: String,
    val page: Int,
    val fontSize: String? = null,
    val fontWeight: String? = null,
    val alignment: String? = null,
)

fun GetFormFields () {
    post("/get-form-fields", "application/json", fun(request, response) : Any {
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

        // FIXME: im basically 100%sure that this will never be null
        val pdf = fromBase64(json.get("pdf").asText())!!

        try {
            val doc = Loader.loadPDF(pdf);
            val acroForm = doc.documentCatalog.acroForm;
            val result = mutableListOf<FieldInfo>()

            if( acroForm == null || acroForm.fields.isEmpty()) {
                return "{}"
            }
            else {

                for (field in acroForm.fields) {
                    val widget = field.widgets[0]
                    val rect = widget.rectangle
                    val page = doc.pages.indexOf(widget.page)

                    result.add(
                        FieldInfo(
                            name = field.fullyQualifiedName,
                            value = field.valueAsString,
                            fieldFlags = field.fieldFlags,
                            x = rect.lowerLeftX,
                            y = rect.lowerLeftY,
                            width = rect.width,
                            height = rect.height,
                            type = getFieldType(field),
                            page = page,
                            fontSize = getFontSize(widget),
                            fontWeight = getFontWeight(widget),
                            alignment = getAlignment(widget),
                        )
                    )
                }

                response.type("application/json")
                return toJson(result)
            }

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

private fun getFieldType(field: PDField): String {
    return when (field) {
        is PDTextField -> "text"
        is PDCheckBox -> "checkbox"
        is PDRadioButton -> "radio"
        is PDComboBox -> "combo"
        is PDListBox -> "list"
        else -> "unknown"
    }
}

private fun getFontSize(widget: PDAnnotationWidget): String? {
    val appearance = widget.appearance?.getNormalAppearance()
    return appearance?.toString() // This may vary depending on your PDF structure
}

private fun getFontWeight(widget: PDAnnotationWidget): String? {
    // You might need to analyze the font dictionary in the appearance stream
    val appearance = widget.appearance?.getNormalAppearance()
    return appearance?.toString() // Adjust according to your PDF structure
}

private fun getAlignment(widget: PDAnnotationWidget): String? {
    // Alignment is not typically stored in a standard way in PDF forms
    // This may require custom logic based on how you've set up your PDFs
    return "-" // Default or implement logic based on field properties
}