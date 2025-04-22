package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*

import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.interactive.form.*

import java.io.*

import spark.Spark.*

import org.apache.pdfbox.Loader

fun FormFill () {
    post("/form-fill", "application/json", fun(request, response) : Any {
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

        if (json.get("qrcode") != null && !json.get("qrcode").isObject()) {
            halt(400, "qrcode parameter must be an object");
        }

        if (json.get("qrcode") != null && json.get("qrcode").get("content") == null) {
            halt(400, "qrcode.content parameter required");
        }

        // FIXME: im basically 100%sure that this will never be null
        val pdf = fromBase64(json.get("pdf").asText())!!;

        try {
            val doc = Loader.loadPDF(pdf);
            val buffer = ByteArrayOutputStream();
            val acroForm = doc.documentCatalog.acroForm

            if( acroForm == null || acroForm.fields.isEmpty()) {
                halt(400, "pdf has no form fields");
            }
            else {
                // mapping liberationSans as Helvetica 
                //val liberationSans = FontProvider.loadFont(doc, FontStyle.SANS)
                //acroForm.defaultResources.put(COSName.getPDFName("Helv"), liberationSans)
                //acroForm.defaultResources.add(liberationSans)

                val fieldtree = acroForm.getFieldTree()

                // for (field in fieldtree) {
                //     println("Field Name: ${field.fullyQualifiedName}")
                // }
                //form.setGenerateAppearances(true); // if not set the fields will be empty when flattening
                val values = json.get("data");

                for (field in fieldtree) {
                    val key = field.fullyQualifiedName
                    if (field is PDTextField) {
                        val test = field.defaultAppearance.toString()
                        println(test)
                    }

                    val text = values.get(key)?.asText("");
                    if (text != null) {
                        field.setValue(text);
                    }
                }

                if (json.get("flatten")?.asBoolean() == true) {
                    acroForm.flatten();
                }

                /*
                //QRCode stuff
                if (json.get("qrcode") != null && json.get("qrcode").isObject()) {
                    val lastPage = reader.getNumberOfPages();
                    val over = stamper.getOverContent(lastPage);

                    val qrcodeData = json.get("qrcode");
                    val qrcontent = qrcodeData.get("content").asText("");
                    val qrcontentAsJson = toJson(qrcodeData.get("content"));

                    val qrposition = qrcodeData.get("position")?.asText("") ?: "bottomCenter";

                    val qrParams = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
                    
                    var qrcode = BarcodeQRCode(qrcontent, 1000, 1000, qrParams);
                    //overwrite qrcontent if its a supposed to be an JSON-Object
                    //FIXME what should be default
                    if(qrcodeData.get("contentIsObject")?.asBoolean() ?: false) {
                        qrcode = BarcodeQRCode(qrcontentAsJson, 1000, 1000, qrParams);
                    }
                    val qrimage = qrcode.getImage();

                    if (qrposition == "bottomCenter") {
                        qrimage.scaleAbsolute(70.toFloat(), 70.toFloat());
                        val left = (reader.getPageSize(lastPage).getRight() / 2) - (qrimage.getScaledWidth() / 2);
                        qrimage.setAbsolutePosition(left, reader.getPageSize(lastPage).getBottom());
                        over.addImage(qrimage);
                    }
                    if (qrposition == "signature") {
                        qrimage.scaleAbsolute(80.toFloat(), 80.toFloat());
                        val left = reader.getPageSize(lastPage).getRight() - qrimage.getScaledWidth() - 20;
                        qrimage.setAbsolutePosition(left, 102.toFloat());
                        over.addImage(qrimage);
                    }
                    if (qrposition == "custom") {
                        val size = qrcodeData.get("customSize")?.asInt() ?: 80;
                        qrimage.scaleAbsolute(size.toFloat(), size.toFloat());
                        val left = qrcodeData.get("customLeft")?.asInt() ?: 0;
                        val bottom = qrcodeData.get("customBottom")?.asInt() ?: 0;

                        qrimage.setAbsolutePosition(left.toFloat(), bottom.toFloat());
                        over.addImage(qrimage);
                    }
                }
                */
                doc.save(buffer);
                doc.close();

                response.type("application/pdf");
                return buffer.toByteArray();
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
