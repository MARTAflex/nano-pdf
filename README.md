# nano-pdf
REST microservice intended to provide functionality similar to pdftk

    start via:
        ./gradlew run

    port: 9091

    create docker image in repo dir via:
        ./gradew build #which creates the necessary tarball
        docker build -t martaflex/nanopdf:0.0.5 .

    unit test will create ./some.pdf for manual review of the
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

    POST /to-text       //parses pdf to text, has two variants, only one is active
        request body:
            {
                "pdf": "TWFuIGl[....]pcyByZWF=", // base64 encoded pdf file
            }

        response body: json array with a parsed String per page

    POST /check-if-form       //checks if the pdf has AcroForm fields
        request body:
            {
                "pdf": "TWFuIGl[....]pcyByZWF=", // base64 encoded pdf file
            }

        response text: true || false

    POST /chunk-to-text       //checks if the pdf has AcroForm fields
        request body:
            {
                "pdf": "TWFuIGl[....]pcyByZWF=", // base64 encoded pdf file,
                "data" {
                    "chunkSize": 10,
                    "chunkIndex": 0

                }
            }

        response body: json array with a parsed String per page in interval according to chunkSize and chunkIndex

    POST /get-pdf-length      //get number of pages
        request body:
            {
                "pdf": "TWFuIGl[....]pcyByZWF=", // base64 encoded pdf file
            }

        response text: length


    POST /group-pages       //merges picked pages
        request body:
            {
                "pdf": "TWFuIGl[....]pcyByZWF=", // base64 encoded pdf file
                "data": {
                    // any groupKey : array of page numbers that have to be merged
                    "1002": [0,1,5],
                    "test": [2,3,4]
                },
            }

        response body: json with base64 encoded pdf per groupkey

    -------
    ( gradle 3.5 might be needed )
