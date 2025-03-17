package de.martaflex.nanopdf.routes

import spark.Spark.*


fun StatusController () {
    get("/version", fun(request, response) : Any {
        // this gets updated via the deploy script and 
        // should be therefore in sync with build.gradle version

        val version = "0.1.4";
        
        return version;
    })
}
