# nano-pdf
REST microservice intended to provide functionaility similar to pdftk

    start via:
        ./gradlew run
    
    port: 9091

    create docker image in repo dir via:
        ./gradew build #which creates the nessecary tarball
        docker build -t martaflex/nanopdf:0.0.1 .

    unit test wil create ./some.pdf for manual review of the
    modified pdf. this behavior will change obviously
    
    Routes:
    -------
    POST /form-fill
    request body:
        {
            "pdf": "TWFuIGl[....]pcyByZWF=", // base64 encoded pdf file
            "data": {
                // values to pass into the form
                // keys must correspond to the AcroForm field
                // names in the passed pdf file
                "firstname": "Alice",
                "lastname": "Smith"
            },
            "flatten": true // optional parameter to replace the AcroForm
                            // fields with plain text
        }

    response body: modified pdf in binary form

    ( gradle 3.5 might be needed )

