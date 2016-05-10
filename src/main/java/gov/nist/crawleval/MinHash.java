package gov.nist.crawleval;

import ch.sentric.URL;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import de.l3s.boilerpipe.extractors.KeepEverythingExtractor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class MinHash {

    @Option(name="-h", usage="Print out help")
    boolean show_help = false;

    @Option(name="-n", usage="Number of bands per hashtable row (default 16; must divide 256)")
    int num_bands = 16;

    @Argument
    List<String> files = new ArrayList<String>();

    public final int num_hashes = 256;

    public static void main(String[] args) throws IOException {
        MinHash minhash = new MinHash();
        minhash.do_it(args);
    }

    protected Analyzer analyzer = new ShingleAnalyzerWrapper(new SimpleAnalyzer(), 9);
    protected MinHasher hasher = null;
    protected HashFunction murmur = Hashing.murmur3_32();
    protected HashFunction sha256 = Hashing.sha256();

    public int[] docToMinHashes(String raw_html) throws Exception {
        HashSet<Integer> doc = new HashSet<Integer>();
        String content_to_parse = KeepEverythingExtractor.INSTANCE.getText(raw_html);
        TokenStream ts = analyzer.tokenStream("extracted_text", content_to_parse);
        CharTermAttribute cattr = ts.addAttribute(CharTermAttribute.class);
        ts.reset();
        while (ts.incrementToken()) {
            doc.add(murmur.hashString(cattr.toString(), Charsets.UTF_8).asInt());
        }
        ts.close();
        if (hasher == null)
            hasher = new MinHasher(num_hashes);
        return hasher.hash(doc);

    }

    public String get_string(Map map, String key) {
        Object val = map.get(key);
        if (val instanceof String && val != null)
            return (String)val;
        else
            return null;
    }

    public void do_it(String[] args) throws IOException {
        CmdLineParser argparser = new CmdLineParser(this);
        try {
            argparser.parseArgument(args);
            if (files.isEmpty()) {
                throw new CmdLineException(argparser, "No files given");
            }

        } catch (CmdLineException cle) {
            System.err.println(cle.getMessage());
            argparser.printUsage(System.err);
            System.err.println();
        }
        if (show_help) {
            argparser.printUsage(System.out);
            System.out.println();
            System.exit(1);
        }

        System.err.println("Pass 1: Indexing using LSH");
        int count = 0;
        LSH lsh = new LSH(num_hashes, num_bands);

        for (String filename : files) {
            BufferedReader in = null;
            if (filename.endsWith(".gz")) {
                in = new BufferedReader(new InputStreamReader(new GZIPInputStream((new FileInputStream(filename)))));
            } else {
                in = new BufferedReader(new FileReader(filename));
            }
            ObjectMapper mapper = new ObjectMapper();
            String line;
            String url = null;
            String key = null;
            try {
                while ((line = in.readLine()) != null) {
                    Map obj = mapper.readValue(line, new TypeReference<Map<?, ?>>() {});
                    String type = get_string(obj, "content_type");
                    if (type == null || !(type.contains("text") || type.contains("html"))) {
                        System.err.println("Skipping non-text document");
                        continue;
                    }
                    String raw = get_string(obj, "raw_content");
                    if (raw == null) {
                        System.err.println("Skipping document with no raw content");
                        continue;
                    }
                    url = get_string(obj, "url");
                    if (url == null) {
                        System.err.println("Skipping document with no url");
                        continue;
                    }

                    int[] signatures = docToMinHashes(raw);
                    key = get_string(obj, "team") + ":" + url;
                    lsh.insert(key, signatures);

                    count++;
                    if ((count % 100) == 0) {
                        System.err.print(".");
                    }
                }
            } catch (EOFException eof) {
                // continue
            } catch (IllegalArgumentException parse_err) {
                System.err.println("Error parsing " + key + " // " + url);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.err.println();
        }

        System.err.println("Pass 2: Querying near-duplicates");
        count = 0;
        HashMap<String, String> url2sha = new HashMap<>();

        for (String filename : files) {
            BufferedReader in = null;
            if (filename.endsWith(".gz")) {
                in = new BufferedReader(new InputStreamReader(new GZIPInputStream((new FileInputStream(filename)))));
            } else {
                in = new BufferedReader(new FileReader(filename));
            }
            ObjectMapper mapper = new ObjectMapper();
            String line;
            String team = null;
            String url = null;
            try {
                while ((line = in.readLine()) != null) {
                    Map obj = mapper.readValue(line, new TypeReference<Map<?, ?>>() {});
                    String raw = get_string(obj, "raw_content");
                    String type = get_string(obj, "content_type");
                    url = get_string(obj, "url");
                    team = get_string(obj, "team");
                    String teamurl = team + ":" + url;
                    if (url2sha.containsKey(teamurl)) {
                        System.out.println(url2sha.get(teamurl) + " " + team + " " + url);
                        continue;
                    }

                    if (url == null) {
                        System.err.println("Skipping document with no url");
                        continue;
                    }
                    if (type == null || !(type.contains("text") || type.contains("html"))) {
                        System.err.println("Hashing canonicalized URL for non-text document");
                        URL canurl = new URL(url);
                        String sha = sha256.hashString(canurl.getNutchNormalizedUrl(), Charsets.UTF_8).toString();
                        url2sha.put(teamurl, sha);
                        System.out.println(url2sha.get(teamurl) + " " + team + " " + url);
                        continue;
                    }
                    if (raw == null) {
                        System.err.println("Skipping document with no raw content");
                        continue;
                    }

                    // textual content
                    String sha = sha256.hashString(raw, Charsets.UTF_8).toString();
                    url2sha.put(teamurl, sha);
                    System.out.println(sha + " " + team + " " + url);

                    int[] sigs = docToMinHashes(raw);
                    Set<String> lsh_dupes = lsh.query(sigs);
                    for (String d : lsh_dupes)
                        if (!url2sha.containsKey(d))
                            url2sha.put(d, sha);

                    count++;
                    if ((count % 100) == 0) {
                        System.err.print(".");
                    }
                }
            } catch (EOFException eof) {
                // continue
            } catch (IllegalArgumentException parse_err) {
                System.err.println("Error parsing " + team + " // " + url);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.err.println();
        }
    }
}
