package gov.nist.crawleval;

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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CompareFiles {

    @Option(name="-h", usage="Print out help")
    public boolean show_help = false;

    @Option(name="-n", usage="Number of hash entries (default 256)")
    int num_hashes = 256;

    @Option(name="-t", usage="Jaccard similarity threshold (default 0.9)")
    double jaccard_threshold = 0.9;

    @Argument
    public List<String> files = new ArrayList<String>();

    public static void main(String[] args) throws IOException {
        CompareFiles minhash = new CompareFiles();
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

        for (String filename : files) {
            try {
                String raw = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);

                int[] signatures = docToMinHashes(raw);
                lsh.insert(filename, signatures);

            } catch (IllegalArgumentException parse_err) {
                System.err.println("Error parsing " + filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.err.println("Pass 2: Querying near-duplicates");
        count = 0;
        HashMap<String, String> url2sha = new HashMap<>();

        for (String filename : files) {
            try {
                String raw = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);

                int[] sigs = docToMinHashes(raw);
                Set<String> lsh_dupes = lsh.query(sigs);
                for (String d : lsh_dupes)
                    if (!d.equals(filename))
                        System.out.println(filename + " " + d);
            } catch (IllegalArgumentException parse_err) {
                System.err.println("Error parsing " + filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
