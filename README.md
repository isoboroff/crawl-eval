
# crawl-eval

This is a bag of tools I created in organizing an evaluation of crawling. The evaluation needed to extract text from HTML, identify near-duplicates with a fingerprinting scheme, and count how many evaluation participants found each page. This data was used in a capture-recapture analysis using R's Rcapture package, that I won't go into here.

There are two parts to this: a pile of short Python scripts for data cleaning the JSON-formatted data, and a Java program to do document fingerprinting using MinHashing.

The code assumes that the crawl data is in JSON following a schema we used in the evaluation and which I will eventually copy here for completeness, but for the time being, here's the two penny tour. A file is a series of lines, each of which contains one JSON record corresponding to a crawl artifact. The essential fields are:

* `content_type`:  This should be the Content-Type header from the response, and is used to decide how to treat binary vs. text content.  Textual content is deduped using MinHash.  Binary content is deduped by canonicalizing the URL (for historical reasons).
* `raw_content`:  This is the raw content from the server.
* `url`:  This is the URL for the raw_content.
* `team`:  This is a string identifier that tells you this file came from this evaluation participant.

The files can be compressed with gzip.

To generate fingerprints for a bunch of such files, do

```
mvn exec:java -Dexec.mainClass="gov.nist.crawleval.MinHash" -Dexec.args="$(echo *json.gz)" > hashes
```

The resulting `hashes` file has lines with three space-separated fields: fingerprint hash, team, URL.

The Python scripts are a hodgepodge of one-offs for cleaning and work scripts that are needed to do the evaluation. I'm including them for completeness and I don't have a better place to put them.  The most useful (for you) (I'm guessing) are probably:

* `jcat.py`: Prettyprint a JSON crawl file.
* `hash2rcap.py`: tabulate the hashes into the capture-recapture format that R wants.
* `content-types.py`: tabulate the content types in the input crawl file.

There is a script `capture.R` which is not actually executable because R hates me, but contains the code for computing estimated site size estimates using the data from `hash2rcap.py`.

