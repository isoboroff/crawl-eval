package gov.nist.crawleval;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by soboroff on 5/10/16.
 */
class LSH {
    Multimap<String, String>[] maps;
    int num_bands;
    int num_rows;
    int num_hashes;

    public LSH(int n, int b) throws IllegalArgumentException {
        num_hashes = n;
        num_bands = b;
        if ((n % b) != 0)
            throw new IllegalArgumentException("Bands must divide num_hashes (" + num_hashes + ") evenly");
        num_rows = n / b;
        System.err.println("LSH with " + num_hashes + " hash buckets and " + num_bands + " bands");
        System.err.println("Target threshold: " + Math.pow(1.0 / num_hashes, (1.0 / (num_hashes / num_bands))));
        maps = new ArrayListMultimap[num_bands];
        setup_bands();
    }

    public LSH(int n, double t) {
        num_hashes = n;
        num_bands = compute_bands(n, t);
        num_rows = n / num_bands;
        System.err.println("LSH with " + num_hashes + " hash buckets and " + num_bands + " bands");
        System.err.println("Target threshold: " + Math.pow(1.0 / num_hashes, (1.0 / (num_hashes / num_bands))));
        maps = new ArrayListMultimap[num_bands];
        setup_bands();
    }

    protected void setup_bands() {
        for (int i = 0; i < num_bands; i++)
            maps[i] = ArrayListMultimap.create();
    }

    protected int compute_bands(int num_hashes, double jaccard_threshold) {
        int bands = num_hashes;
        // System.err.println("N=" + num_hashes);
        while (bands > 1) {
            if ((num_hashes % bands) == 0) {
                double thresh = Math.pow((double)1.0 / bands, (double)bands/num_hashes);
                // System.err.println("Checking b=" + bands + ", threshold is " + thresh);
                if (thresh > jaccard_threshold)
                    break;
            }
            bands --;
        }

        // System.err.println("Returning b=" + bands);
        return bands;
    }

    public void insert(String key, int[] hashes) {
        for (int b = 0; b < num_bands; b++) {
            StringBuffer sb = new StringBuffer();
            for (int r = 0; r < num_rows; r++) {
                sb.append(Integer.toHexString(hashes[b * num_rows + r]));
            }
            String hh = sb.toString();
            maps[b].put(hh, key);
        }
    }

    public Set<String> query(int[] hashes) {
        HashSet<String> candidates = new HashSet<String>();
        for (int b = 0; b < num_bands; b++) {
            StringBuffer sb = new StringBuffer();
            for (int r = 0; r < num_rows; r++) {
                sb.append(String.format("%8x", hashes[b * num_rows + r]));
            }
            String hh = sb.toString();
            candidates.addAll(maps[b].get(hh));
        }
        return candidates;
    }

    public static void main(String args[]) {
        int num_hashes = Integer.parseInt(args[0]);
        double threshold = Double.parseDouble(args[1]);
        LSH lsh = new LSH(num_hashes, threshold);
    }
}
