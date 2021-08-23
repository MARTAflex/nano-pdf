package de.martaflex.nanopdf.routes

import de.martaflex.nanopdf.helpers.*

import java.io.*
import java.util.Base64

import spark.Spark.*
import spark.ResponseTransformer

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.core.JsonParseException

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.qrcode.*;
import com.itextpdf.text.Image;

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
            val reader = PdfReader(pdf);
            val buffer = ByteArrayOutputStream();
            val stamper = PdfStamper(reader, buffer);

            val form = stamper.getAcroFields();

            //check if there are any form fields
            val fields = form.getFields();

            if(fields.isEmpty()) {
                halt(400, "pdf has no form fields");
            }
            else {
                form.setGenerateAppearances(true); // if not set the fields will be empty when flattening
                val values = json.get("data");

                for (key in values.fieldNames()) {
                    if (form.getField(key) != null) {
                        form.setField(key, values.get(key).asText(""));
                    }
                }

                stamper.setFormFlattening(json.get("flatten")?.asBoolean() ?: false);
                
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

                stamper.close();

                response.type("application/pdf");
                return buffer.toByteArray();
            }
        }
        catch ( e : InvalidPdfException) {
            println(e.message);
            halt(400, e.message)
        }

        halt(500, "this is not spposed to happen");
        // FIXME: wait what??
        return ""
    });
}
