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

public class MinHashWaPo {

    @Option(name="-h", usage="Print out help")
    boolean show_help = false;

    @Option(name="-n", usage="Number of hash entries (default 256)")
    int num_hashes = 256;

    @Option(name="-t", usage="Jaccard similarity threshold (default 0.9)")
    double jaccard_threshold = 0.9;

    @Argument
    List<String> files = new ArrayList<String>();

    public static void main(String[] args) throws IOException {
        MinHashWaPo minhash = new MinHashWaPo();
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
        else if (val instanceof List && val != null) {
            List<Object> vallist = (List)val;
            return (String)vallist.get(0);
        }
        else
            return null;
    }

    public String doc2string(Map obj) {
        StringBuffer buf = new StringBuffer();
        String title = get_string(obj, "title");
        String author = get_string(obj, "author");
        buf.append(title).append(" ").append(author);
        for (Map content_obj : (List<Map>)obj.get("contents")) {
            if (content_obj == null)
                continue;
            String type = (String)content_obj.getOrDefault("type", "");
            if (type.contains("text")) {
                buf.append(" ").append(content_obj.getOrDefault("content", ""));
            } else if (type.contains("image")) {
                buf.append(" ").append(content_obj.getOrDefault("fullcaption", ""));
            } else if (type.contains("video")) {
                buf.append(" ").append(content_obj.getOrDefault("blurb", ""));
            }
        }
        return buf.toString();
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
        LSH lsh = new LSH(num_hashes, jaccard_threshold);
        HashMap<String, String> titles = new HashMap<>();

        for (String filename : files) {
            BufferedReader in = null;
            if (filename.endsWith(".gz")) {
                in = new BufferedReader(new InputStreamReader(new GZIPInputStream((new FileInputStream(filename)))));
            } else {
                in = new BufferedReader(new FileReader(filename));
            }
            ObjectMapper mapper = new ObjectMapper();
            String line;
            String key = null;
            String title = null;
            try {
                while ((line = in.readLine()) != null) {
                    Map obj = mapper.readValue(line, new TypeReference<Map<?, ?>>() {});
                    key = get_string(obj, "id");
                    title = get_string(obj, "title") + "  " + get_string(obj, "author");
                    titles.put(key, title);

                    String text = doc2string(obj);
                    int[] signatures = docToMinHashes(text);
                    lsh.insert(key, signatures);

                    count++;
                    if ((count % 100) == 0) {
                        System.err.print(".");
                    }
                }
            } catch (EOFException eof) {
                // continue
            } catch (IllegalArgumentException parse_err) {
                System.err.println("Error parsing " + key + " // " + title);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.err.println();
        }

        System.err.println("Pass 2: Querying near-duplicates");
        count = 0;
        HashMap<String, String> id2cluster = new HashMap<>();

        for (String filename : files) {
            BufferedReader in = null;
            if (filename.endsWith(".gz")) {
                in = new BufferedReader(new InputStreamReader(new GZIPInputStream((new FileInputStream(filename)))));
            } else {
                in = new BufferedReader(new FileReader(filename));
            }
            ObjectMapper mapper = new ObjectMapper();
            String line;
            String id = null;
            String url = null;
            try {
                while ((line = in.readLine()) != null) {
                    Map obj = mapper.readValue(line, new TypeReference<Map<?, ?>>() {});
                    id = get_string(obj, "id");

                    if (id2cluster.containsKey(id)) {
                        System.out.println(id2cluster.get(id) + " " + id + " " + titles.get(id));
                        continue;
                    }

                    id2cluster.put(id, id);
                    System.out.println(id + " " + id + " " + titles.get(id));

                    String raw = doc2string(obj);
                    int[] sigs = docToMinHashes(raw);
                    Set<String> lsh_dupes = lsh.query(sigs);
                    for (String d : lsh_dupes)
                        if (!id2cluster.containsKey(d))
                            id2cluster.put(d, id);

                    count++;
                    if ((count % 100) == 0) {
                        System.err.print(".");
                    }
                }
            } catch (EOFException eof) {
                // continue
            } catch (IllegalArgumentException parse_err) {
                System.err.println("Error parsing // " + url);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.err.println();
        }
    }
}
