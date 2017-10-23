package au.gov.nla.httrack2warc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;

public class Main {
    private static final String USAGE = "Convert HTTrack web crawls to WARC files\n" +
            "\n" +
            "Usage:\n" +
            "  httrack2warc [OPTIONS...] -o outdir crawldir\n" +
            "\n" +
            "Options:\n" +
            "  -h, --help                   Show this screen.\n" +
            "  -o, --outdir DIR             Directory to write output (default: current working directory).\n" +
            "  -s, --size BYTES             WARC size target (default: 1GB).\n" +
            "  -n, --name PATTERN           WARC name pattern (default: crawl-%d.warc.gz).\n" +
            "  -Z, --timezone ZONEID        Timezone of HTTrack logs (default: " + ZoneId.systemDefault() + ").\n" +
            "  -I, --warcinfo 'KEY: VALUE'  Add extra lines to warcinfo record.\n" +
            "  -C, --compression none|gzip  Type of compression to use (default: gzip).\n";

    public static void main(String[] args) throws IOException {
        Path crawldir = null;
        Httrack2Warc httrack2Warc = new Httrack2Warc();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    System.out.println(USAGE);
                    return;

                case "-o":
                case "--outdir":
                    httrack2Warc.setOutputDirectory(Paths.get(args[++i]));
                    break;

                case "-s":
                case "--size":
                    httrack2Warc.setWarcSizeTarget(Long.parseLong(args[++i]));
                    break;

                case "-n":
                case "--name":
                    httrack2Warc.setWarcNamePattern(args[++i]);
                    break;

                case "-Z":
                case "--timezone":
                    httrack2Warc.setTimezone(ZoneId.of(args[++i]));
                    break;

                case "-I":
                case "--warcinfo":
                    httrack2Warc.addWarcInfoLine(args[++i]);
                    break;

                case "-C":
                case "--compression":
                    httrack2Warc.setCompression(Compression.valueOf(args[++i].toUpperCase()));
                    break;

                default:
                    if (args[i].startsWith("-")) {
                        System.err.println("httrack2warc: Unrecognised option '" + args[i] + "'");
                        System.err.println("Try 'httrack2warc --help' for more information.");
                        System.exit(1);
                    }

                    if (crawldir != null) {
                        System.err.println("httrack2warc: Only a single source crawl directory may be specified.");
                        System.err.println("Try 'httrack2warc --help' for more information.");
                        System.exit(1);
                    }

                    crawldir = Paths.get(args[i]);
            }
        }

        httrack2Warc.convert(crawldir);
    }
}
