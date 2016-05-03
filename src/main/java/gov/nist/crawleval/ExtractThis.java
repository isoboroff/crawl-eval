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

import java.io.InputStreamReader;
import java.util.HashSet;

public class ExtractThis {

    public static void main(String[] args) throws Exception {
        Analyzer analyzer = new ShingleAnalyzerWrapper(new SimpleAnalyzer(), 9);
        String content = KeepEverythingExtractor.INSTANCE.getText(new InputStreamReader(System.in));
        TokenStream ts = analyzer.tokenStream("extracted_text", content);
        CharTermAttribute cattr = ts.addAttribute(CharTermAttribute.class);
        ts.reset();
        while (ts.incrementToken()) {
            System.out.println(cattr.toString());
        }
        ts.close();

    }
}


