package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.fromBase64
import de.martaflex.nanopdf.helpers.fromJson
import de.martaflex.nanopdf.helpers.toJson
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.interactive.form.*
import spark.Spark.halt
import spark.Spark.post
import java.io.IOException


data class FieldInfo(
    val fullyQualifiedName: String,
    val value: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: String,
    val page: Int,
    val defaultAppearance: String? = null,
    val quadding: String? = null,
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
            val result = mutableMapOf<String, FieldInfo>()

            

            if( acroForm == null || acroForm.fields.isEmpty()) {
                halt(400, "no form fields found");
            }
            else {
                for (field in acroForm.getFieldTree()) {
                    for(widget in field.widgets) {
                        val rect = widget.rectangle
                        val page = doc.pages.indexOf(widget.page)
                        val type = getFieldType(field)
                        val fullyQualifiedName = field.fullyQualifiedName

                        var uniqueKey = fullyQualifiedName
                        var counter = 1

                        while (result.containsKey(uniqueKey)) {
                            uniqueKey = "$fullyQualifiedName$counter"
                            counter++
                        }

                        result[uniqueKey] = FieldInfo(
                            fullyQualifiedName = fullyQualifiedName,
                            value = field.valueAsString,
                            x = rect.lowerLeftX,
                            y = rect.lowerLeftY,
                            width = rect.width,
                            height = rect.height,
                            type = type,
                            page = page,
                            defaultAppearance = getDefaultAppearance(field),
                            quadding = getQuadding(field),
                        )
                    }
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

private fun getDefaultAppearance(field: PDField): String? {
    return if (field is PDTextField) {
        field.defaultAppearance.toString()
    } else {
        "-"
    }
}

private fun getQuadding(field: PDField): String? {
    return if (field is PDTextField) {
        field.q.toString()
    }
    else {
        "-"
    }
}
