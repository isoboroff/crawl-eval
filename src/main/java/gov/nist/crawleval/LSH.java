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
        System.err.println("Target threshold: " + Math.pow(1.0 / num_hashes, (1.0 / (num_hashes / num_bands))));
        maps = new ArrayListMultimap[num_bands];
        for (int i = 0; i < num_bands; i++)
            maps[i] = ArrayListMultimap.create();
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
}
