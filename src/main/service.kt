package de.martaflex.nanopdf

import spark.Spark.*
import de.martaflex.nanopdf.routes.*

fun Service () {
    port(9091)
    FormFill();
    ToText();
    GroupPages();
    CheckIfForm();
    GetPdfLength();
}
