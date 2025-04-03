package de.martaflex.nanopdf

import de.martaflex.nanopdf.routes.*
import spark.Spark.port

fun Service () {
    port(9091)
    FormFill()
    ToText()
    GroupPages()
    CheckIfForm()
    GetPdfLength()
    ChunkToText()
    DrawRectangles()
    GetPageDimensions()
    GetFirstPageAsImage()
    GetFormFields()
    SetFormFields()
    AreasToText()
    PatternToText()
    AvailableFonts()

    StatusController()
}
