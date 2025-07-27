import java.io.*;
import java.util.*;

public class FEALK4K5 {

    // Replace with actual recovered values
    static int K0 = 0xCAFEBABE;
    static int K1 = 0xDEADC0DE;
    static int K2 = 0xFEEDFACE;
    static int K3 = 0xBAADF00D;

    // Load plaintext–ciphertext pairs
    public static List<long[]> loadPairs(String filename) throws IOException {
        List<long[]> pairs = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            String[] tokens = line.trim().split("\\s+");
            if (tokens.length == 2) {
                long p = Long.parseUnsignedLong(tokens[0], 16);
                long c = Long.parseUnsignedLong(tokens[1], 16);
                pairs.add(new long[]{p, c});
            }
        }
        br.close();
        return pairs;
    }

    // FEAL F function (simplified XOR for placeholder)
    public static int fealF(int x, int k) {
        // Replace with actual FEAL F if available
        return x ^ k;
    }

    // Perform 4 FEAL rounds forward
    public static int[] feal4Rounds(int L, int R) {
        int L1 = R;
        int R1 = L ^ fealF(R, K0);

        int L2 = R1;
        int R2 = L1 ^ fealF(R1, K1);

        int L3 = R2;
        int R3 = L2 ^ fealF(R2, K2);

        int L4 = R3;
        int R4 = L3 ^ fealF(R3, K3);

        return new int[]{L4, R4};  // Before whitening
    }

    // Derive whitening keys K4 and K5
    public static void deriveWhiteningKeys(List<long[]> pairs) {
        Set<Integer> k4Candidates = new HashSet<>();
        Set<Integer> k5Candidates = new HashSet<>();

        for (long[] pair : pairs) {
            int[] pt = splitBlock(pair[0]);
            int[] ct = splitBlock(pair[1]);

            // Apply whitening input
            int L0 = pt[0];
            int R0 = pt[1];

            // Apply 4 rounds
            int[] state = feal4Rounds(L0, R0);

            // Whitening output: C = (L4 ⊕ K4, R4 ⊕ K5)
            int L4 = state[0];
            int R4 = state[1];

            int K4 = L4 ^ ct[0];
            int K5 = R4 ^ ct[1];

            k4Candidates.add(K4);
            k5Candidates.add(K5);
        }

        if (k4Candidates.size() == 1 && k5Candidates.size() == 1) {
            int finalK4 = k4Candidates.iterator().next();
            int finalK5 = k5Candidates.iterator().next();
            System.out.printf(" Recovered K4: %08X\n", finalK4);
            System.out.printf(" Recovered K5: %08X\n", finalK5);
        } else {
            System.out.println(" Inconsistent whitening keys! Check your recovered K0–K3.");
            System.out.println("K4 candidates: " + k4Candidates);
            System.out.println("K5 candidates: " + k5Candidates);
        }
    }

    // Split 64-bit block into 2x 32-bit
    public static int[] splitBlock(long block) {
        return new int[]{
            (int) (block >>> 32),
            (int) (block & 0xFFFFFFFFL)
        };
    }

    // Main driver
    public static void main(String[] args) {
        try {
            List<long[]> pairs = loadPairs("ciphertexts.txt"); // same file with known P/C pairs
            deriveWhiteningKeys(pairs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
