/*
 * Copyright (c) 2017 National Library of Australia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package au.gov.nla.httrack2warc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY;

public class Main {
    private static final String[] LOG_LEVELS = {"error", "warn", "info", "debug", "trace"};

    private static final String USAGE = "Convert HTTrack web crawls to WARC files\n" +
            "\n" +
            "Usage:\n" +
            "  httrack2warc [OPTIONS...] -o outdir crawldir\n" +
            "\n" +
            "Options:\n" +
            "  --cdx FILENAME               Write a CDX index file for the generated WARCs.\n" +
            "  -C, --compression none|gzip  Type of compression to use (default: gzip).\n" +
            "  -x, --exclude REGEX          Exclude URLs matching a regular expression.\n" +
            "  -h, --help                   Show this screen.\n" +
            "  -n, --name PATTERN           WARC name pattern (default: crawl-%d.warc.gz).\n" +
            "  -o, --outdir DIR             Directory to write output (default: current working directory).\n" +
            "  -q, --quiet                  Decrease logging verbosity.\n" +
            "  --redirect-file PATTERN      Direct synthetic redirects to a separate set of WARC files.\n" +
            "  --redirect-prefix URLPREFIX  Generates synthetic redirects from HTTrack-rewritten URLs to original URLs.\n" +
            "  --rewrite-links              When the unmodified HTML is unavailable attempt to rewrite links to undo HTTrack's URL mangling. (experimental)\n" +
            "  -s, --size BYTES             WARC size target (default: 1GB).\n" +
            "  --strict                     Abort on issues normally considered a warning.\n" +
            "  -Z, --timezone ZONEID        Timezone of HTTrack logs (default: " + ZoneId.systemDefault() + ").\n" +
            "  -I, --warcinfo 'KEY: VALUE'  Add extra lines to warcinfo record.\n" +
            "  -v, --verbose                Increase logging verbosity.\n";

    public static void main(String[] args) throws IOException {
        Path crawldir = null;
        Httrack2Warc httrack2Warc = new Httrack2Warc();
        int verbosity = Arrays.asList(LOG_LEVELS).indexOf("info");
//        System.out.println("Run Arguments: " + Arrays.toString(args) + "\n");

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

                case "--cdx":
                    httrack2Warc.setCdxName(args[++i]);
                    break;

                case "--strict":
                    httrack2Warc.setStrict(true);
                    break;

                case "--rewrite-links":
                    httrack2Warc.setRewriteLinks(true);
                    break;

                case "--redirect-file":
                    httrack2Warc.setRedirectFile(args[++i]);
                    break;

                case "--redirect-prefix":
                    httrack2Warc.setRedirectPrefix(args[++i]);
                    break;

                case "--exclude":
                case "-x":
                    httrack2Warc.addExclusion(Pattern.compile(args[++i]));
                    break;

                case "--quiet":
                case "-q":
                    verbosity--;
                    break;

                case "--verbose":
                case "-v":
                    verbosity++;
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

        if (crawldir == null) {
            System.err.println("httrack2warc: A crawl directory must be specified.");
            System.err.println("Try 'httrack2warc --help' for more information.");
            System.exit(1);
        }

        if (System.getProperty(DEFAULT_LOG_LEVEL_KEY) == null) {
            if (verbosity >= LOG_LEVELS.length) verbosity = LOG_LEVELS.length - 1;
            if (verbosity < 0) verbosity = 0;
            System.setProperty(DEFAULT_LOG_LEVEL_KEY, LOG_LEVELS[verbosity]);
        }

        StringBuilder optionsLine = new StringBuilder();
        for (String arg: args) {
            if (optionsLine.length() != 0) optionsLine.append(' ');
            if (arg.contains(" ")) {
                optionsLine.append('\'').append(arg.replace("'", "'\"'\"'")).append('\'');
            } else {
                optionsLine.append(arg);
            }
        }

//        System.out.println("Options: " + optionsLine + "\n");
//        System.out.println("Crawl Directory: " + crawldir + "\n");

        httrack2Warc.addWarcInfoLine("httrack2warcOptions: " + optionsLine);

        httrack2Warc.convert(crawldir);
    }
}
