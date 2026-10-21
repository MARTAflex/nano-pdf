# nano-pdf

REST microservice for working with PDF files (filling forms, extracting text, splitting
pages, ...), intended to provide functionality similar to pdftk.

Written in Kotlin, based on [Apache PDFBox](https://pdfbox.apache.org/) and
[Spark](https://sparkjava.com/). The service listens on port **9091**.

## Prerequisites

- Java 8 (the Docker image runs on `eclipse-temurin:8-jre`)
- Gradle and Kotlin are provided by the Gradle wrapper (`./gradlew`), no separate install needed

The easiest way to get the right JDK is [SDKMAN!](https://sdkman.io/), which reads `.sdkmanrc`:

```sh
sdk env install
```

## Development

```sh
./gradlew run      # start the service on http://localhost:9091
./gradlew test     # run the tests
./gradlew build    # compile, test and package
```

The tests start their own server on port 9091, so stop a running instance first.
Test input PDFs live in `resources/test/`.

Quick check that the service is up:

```sh
curl localhost:9091/version
```

### Building without a local JDK

If you don't have Java 8 installed, you can build inside a container:

```sh
docker run --rm -u $(id -u):$(id -g) -e GRADLE_USER_HOME=/src/.gradle \
    -v $PWD:/src -w /src eclipse-temurin:8-jdk ./gradlew build
```

## Docker

### Building an image

The image is built from `deploy/Dockerfile` and expects the distribution tarball from
`./gradlew build` (`build/distributions/nano-pdf-<version>.tar`):

```sh
./gradlew build
docker build -f deploy/Dockerfile -t martaflex/nano-pdf .
```

The version in `deploy/Dockerfile` must match `version` in `build.gradle`; the release
script keeps them in sync.

### Releasing

`deploy/release-docker.sh` bumps the version, builds and tests the project, builds the
image and pushes it to the GitHub Container Registry. Run it from the `deploy/` directory:

```sh
cd deploy
./release-docker.sh [bump_type] [image_number]
```

- `bump_type`: `patch` (default), `minor`, `major`, `prerelease`, `preminor`, `prepatch`,
  `no-bump` (keep the current version) or an explicit version such as `0.2.0`
- `image_number`: suffix of the image tag, defaults to the current timestamp (`YYYYmmddHHMMSS`)

The script:

1. updates the version in `build.gradle`, `src/main/routes/status.kt` and `deploy/Dockerfile`
2. runs `sdk env install` and `./gradlew clean build`
3. builds the image and tags it as `ghcr.io/martaflex/nano-pdf:<version>.<image_number>` and `:latest`
4. pushes both tags

It pauses for confirmation before building, after the tests and before pushing.
It needs SDKMAN!, the [`semver`](https://www.npmjs.com/package/semver) CLI
(`npm install -g semver`) and a `docker login ghcr.io` with push access.
The version changes are not committed automatically.

### Running

```sh
docker run -d -p 127.0.0.1:9091:9091 ghcr.io/martaflex/nano-pdf:latest
```

or with `docker compose up -d` using `docker-compose.yaml`.

## API

All routes except `/version` are `POST` routes with a JSON body. The PDF is always passed
base64 encoded in the `pdf` parameter:

```json
{
    "pdf": "JVBERi0xLjQK[...]",
    "data": { }
}
```

Invalid requests (missing parameters, invalid JSON, invalid base64, unreadable PDFs) are
answered with status `400` and a plain text error message.

| Route | Description | Response |
| --- | --- | --- |
| `GET /version` | service version | text |
| [`POST /form-fill`](#post-form-fill) | fill AcroForm fields, optionally flatten | PDF |
| [`POST /check-if-form`](#post-check-if-form) | does the PDF have form fields | `true` / `false` |
| [`POST /get-form-fields`](#post-get-form-fields) | list form fields with position, page and type | JSON |
| [`POST /set-form-fields`](#post-set-form-fields) | replace all form fields with new text fields | PDF |
| [`POST /available-fonts`](#post-available-fonts) | fonts used in the PDF and installed on the server | JSON |
| [`POST /to-text`](#post-to-text) | text per page | JSON |
| [`POST /chunk-to-text`](#post-chunk-to-text) | text per page for a range of pages | JSON |
| [`POST /areas-to-text`](#post-areas-to-text) | text inside rectangles, per page | JSON |
| [`POST /pattern-to-text`](#post-pattern-to-text) | regex matches, per page | JSON |
| [`POST /get-pdf-length`](#post-get-pdf-length) | number of pages | number |
| [`POST /get-page-dimensions`](#post-get-page-dimensions) | page size per page | JSON |
| [`POST /get-first-page-as-image`](#post-get-first-page-as-image) | first page rendered as PNG | JSON |
| [`POST /group-pages`](#post-group-pages) | split the PDF into new PDFs from picked pages | JSON |
| [`POST /draw-rectangles`](#post-draw-rectangles) | draw rectangles on every page | PDF |

### Forms

#### `POST /form-fill`

```json
{
    "pdf": "...",
    "data": {
        "firstname": "Alice",
        "lastname": "Smith"
    },
    "flatten": true
}
```

- keys in `data` are the fully qualified AcroForm field names; unknown keys are ignored
- `flatten` (optional, default `false`) replaces the form fields with plain text

Response: the filled PDF (binary).

If a field's font is missing in the PDF or can't display the given text (e.g. Polish or
Cyrillic characters with Helvetica), the bundled Liberation Sans is used as a fallback.
Characters that Liberation Sans doesn't cover either (e.g. CJK, emoji) result in a `400`.
Returns `400` if the PDF has no form fields.

#### `POST /check-if-form`

```json
{ "pdf": "..." }
```

Response: `true` if the PDF has AcroForm fields, otherwise `false`.

#### `POST /get-form-fields`

```json
{ "pdf": "..." }
```

Response: one entry per field widget. Fields with several widgets get a counter appended
to the key (`name1`, `name2`, ...). Coordinates are in PDF points with the origin in the
bottom left corner of the page, `page` is 0-based.

```json
{
    "firstname": {
        "fullyQualifiedName": "firstname",
        "value": "Alice",
        "x": 150.0,
        "y": 700.0,
        "width": 250.0,
        "height": 22.0,
        "type": "text",
        "page": 0,
        "defaultAppearance": "/Helv 11 Tf 0 g",
        "quadding": "0"
    }
}
```

`type` is one of `text`, `checkbox`, `radio`, `combo`, `list`, `unknown`.
Returns `400` if the PDF has no form fields.

Values the PDF doesn't provide are returned as `null`: `page` if the widget has no page
reference, `x`/`y`/`width`/`height` if it has no rectangle, `fullyQualifiedName` if the
field has no name (its key is then `""`, `"1"`, ...).

#### `POST /set-form-fields`

Removes **all** existing form fields and creates new text fields:

```json
{
    "pdf": "...",
    "data": {
        "firstname": {
            "fullyQualifiedName": "firstname",
            "type": "text",
            "page": 0,
            "x": 150,
            "y": 700,
            "width": 250,
            "height": 22,
            "fontSize": 11,
            "fontWeight": "normal",
            "fontColor": "black",
            "quadding": 0
        }
    }
}
```

- the field format matches the output of `/get-form-fields` plus `fontSize`, `fontWeight` and `fontColor`; all properties are required
- only `type: "text"` is supported, other fields are skipped
- `fontWeight`: `normal` or anything else for bold (Liberation Sans)
- `fontColor`: `black` or `white`
- `quadding`: `0` left, `1` centered, `2` right

Response: the modified PDF (binary).

#### `POST /available-fonts`

```json
{ "pdf": "..." }
```

Response:

```json
{
    "embeddedPageFonts": ["Helvetica"],
    "embeddedAcroFormFonts": ["Helvetica", "ZapfDingbats"],
    "localFonts": ["Liberation Sans", "..."]
}
```

### Text extraction

#### `POST /to-text`

```json
{ "pdf": "..." }
```

Response: JSON array with the text of each page.

#### `POST /chunk-to-text`

```json
{
    "pdf": "...",
    "data": {
        "chunkSize": 10,
        "chunkIndex": 0
    }
}
```

Response: JSON array with the text of the pages `chunkIndex * chunkSize` up to
`(chunkIndex + 1) * chunkSize - 1`.

#### `POST /areas-to-text`

```json
{
    "pdf": "...",
    "data": {
        "address": { "x": 50, "y": 100, "width": 200, "height": 80 }
    }
}
```

Coordinates are integers in PDF points with the origin in the **top left** corner of the page.

Response: JSON array with one object per page, containing the text found in each area:

```json
[ { "address": "Alice Smith\nMain Street 1\n" } ]
```

#### `POST /pattern-to-text`

```json
{
    "pdf": "...",
    "data": {
        "invoiceNumber": "INV-\\d+"
    }
}
```

Patterns are Java regular expressions and are matched case-insensitively (not every
JavaScript regex can be used as is).

Response: JSON array with one object per page, containing all matches per pattern:

```json
[ { "invoiceNumber": ["INV-1234"] } ]
```

### Pages

#### `POST /get-pdf-length`

```json
{ "pdf": "..." }
```

Response: the number of pages.

#### `POST /get-page-dimensions`

```json
{ "pdf": "..." }
```

Response: JSON array with the media box `[lowerLeftX, lowerLeftY, upperRightX, upperRightY]`
of each page, e.g. `[[0.0, 0.0, 595.0, 842.0]]`.

#### `POST /get-first-page-as-image`

```json
{ "pdf": "..." }
```

Response: JSON string with the first page rendered as base64 encoded PNG (72 dpi).

#### `POST /group-pages`

```json
{
    "pdf": "...",
    "data": {
        "1002": [0, 1, 5],
        "test": [2, 3, 4]
    }
}
```

Each key gets a new PDF made from the listed pages (0-based).

Response: JSON object with a base64 encoded PDF per key.

#### `POST /draw-rectangles`

```json
{
    "pdf": "...",
    "data": {
        "address": { "x": 50, "y": 100, "width": 200, "height": 80 }
    }
}
```

Draws green rectangles on every page, e.g. to check the areas for `/areas-to-text`.
Coordinates are in PDF points with the origin in the **top left** corner of the page.

Response: the modified PDF (binary).

## License

Apache License 2.0, see [LICENSE](LICENSE).
