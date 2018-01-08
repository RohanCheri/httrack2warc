# httrack2warc

Converts HTTrack crawls to WARC files.

Status: Working on many crawls but needs more testing on corner cases. We're not using it in production yet.

This tool works by reading the HTTrack cache directory (hts-cache) and any available log files to reconstruct an
approximation of the original requests and responses. This process is not perfect as not all the necessary information
is always available. Some of the information that is available is only present in debug log messages that were never
intended for machine consumption. Please see the list of known issues and limitations below.

## Usage

Download the [latest release jar](https://github.com/nla/httrack2warc/releases)
and run it under Java 8 or later.

```
Usage:
  java -jar httrack2warc-0.2.0-shaded.jar [OPTIONS...] -o outdir crawldir

Options:
  -h, --help                   Show this screen.
  -o, --outdir DIR             Directory to write output (default: current working directory).
  -s, --size BYTES             WARC size target (default: 1GB).
  -n, --name PATTERN           WARC name pattern (default: crawl-%d.warc.gz).
  -Z, --timezone ZONEID        Timezone of HTTrack logs (default: ?).
  -I, --warcinfo 'KEY: VALUE'  Add extra lines to warcinfo record.
  -C, --compression none|gzip  Type of compression to use (default: gzip).
```

### Example

Conduct a crawl using HTTrack:

    $ httrack -O /tmp/crawl http://www.example.org/
    Mirror launched on Mon, 08 Jan 2018 13:50:40 by HTTrack Website Copier/3.49-2 [XR&CO'2014]
    mirroring http://www.example.org/ with the wizard help..
    Done.www.example.org/ (1270 bytes) - OK
    Thanks for using HTTrack!

Run httrack2warc over the output to produce WARC files:

    $ java -jar httrack2warc-shaded-0.2.0.jar /tmp/crawl
    Httrack2Warc - www.example.org/index.html -> http://www.example.org/

Replay the ingested WARC files using a replay tool like [pywb](https://github.com/ikreymer/pywb):

    $ pip install --user pywb
    $ PATH="$PATH:$HOME/.local/bin"
    $ wb-manager init test
    $ wb-manager add test crawl-*.warc.gz
    [INFO]: Copied crawl-0.warc.gz to collections/test/archive
    $ wayback
    [INFO]: Starting pywb Wayback Web Archive Replay on port 8080
    # Open in browser: http://localhost:8080/test/*/example.org/

## Known issues and limitations

### Redirects

Generation of HTTP redirect (30x) records is not yet implemented.  It should be possible to derive these from the log
files.

### HTTP headers

By default HTTrack does not record HTTP headers. If the --debug-headers option is specified however the file
hts-ioinfo.txt will be produced containing a log of the request and response headers.

When headers are available httrack2warc produces WARC records of type request and response. When headers are unavailable
only WARC resource records are produced.

The `Transfer-Encoding` header is always stripped as the encoded bytes of the message are not recorded by HTTrack.

### IP addresses and DNS records

HTTrack does not record DNS records or the IP addresses of hostnames therefore httrack2warc cannot produce
WARC-IP-Address or DNS records.

### HTTrack version compatiblity

Some testing has been done against crawls generated by the following versions: 3.01, 3.21-4, 3.49-2. Not all combinations
of options have been tested.

## Compilation

Install Java JDK 8 (or later) and [Maven](https://maven.apache.org/).  On Fedora Linux:

    dnf install java-1.8.0-openjdk-devel maven

Then compile using Maven from the top-level of this repository:

     cd httrack2warc
     mvn package

This will produce an executable jar file which you can run like so:

    java -jar target/httrack2warc-*-shaded.jar --help

## License

Copyright (C) 2017 National Library of Australia

Licensed under the [Apache License, Version 2.0](LICENSE).

## Similar Projects

* [HTTrack2Arc](https://github.com/arquivo/httrack2arc)
* [Netarchive Suite migrations](https://sbforge.org/sonar/drilldown/measures/1?metric=lines&rids%5B%5D=16)
* [warc-tools httrack2warc.sh](https://code.google.com/archive/p/warc-tools/source/default/source?page=6)
