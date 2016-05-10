package gov.nist.crawleval;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by soboroff on 5/10/16.
 */
class MinHasher {
    public final int next_prime = 2147483587; // http://www.prime-numbers.org/prime-number-2147480000-2147485000.htm
    public final int max_value = next_prime - 1;
    public int[] coeffA;
    public int[] coeffB;
    public int num_hashes;

    public MinHasher(int n) {
        num_hashes = n;
        coeffA = pickRandCoeffs(num_hashes);
        coeffB = pickRandCoeffs(num_hashes);
    }

    public int[] hash(Set<Integer> doc) {
        int[] sigs = new int[num_hashes];
        for (int i = 0; i < num_hashes; i++) {
            int min = next_prime + 1;
            for (int shingle : doc) {
                shingle = shingle % max_value;
                int h = (coeffA[i] * shingle + coeffB[i]) % next_prime;
                if (h < min)
                    min = h;
            }
            sigs[i] = min;
        }
        return sigs;
    }

    public int[] pickRandCoeffs(int k) {
        int[] rands = new int[k];
        HashSet<Integer> seen = new HashSet<Integer>(k);
        Random rng = new Random();
        int i = 0;
        while (k > 0) {
            int randIndex = rng.nextInt(max_value);
            while (seen.contains(randIndex))
                randIndex = rng.nextInt(max_value);
            rands[i] = randIndex;
            seen.add(randIndex);
            k = k - 1;
            i++;
        }
        return rands;
    }
}
