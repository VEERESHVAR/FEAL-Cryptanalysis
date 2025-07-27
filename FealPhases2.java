import java.io.*;
import java.util.*;

public class FEALPhases2 {

    // Utility Methods
    public static long hexToLong(String hex) {
        return Long.parseUnsignedLong(hex, 16);
    }

    public static String longToHex(long val) {
        return String.format("%016X", val);
    }

    public static long joinBlock(int L, int R) {
        return ((long) L << 32) | (R & 0xFFFFFFFFL);
    }

    public static int[] splitBlock(long block) {
        int L = (int) (block >>> 32);
        int R = (int) block;
        return new int[]{L, R};
    }

    // Phase 1: Load Ciphertext Pairs
    public static List<long[]> loadCiphertextPairs(String file) throws IOException {
        List<long[]> pairs = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] tokens = line.trim().split("\\s+");
            if (tokens.length == 2) {
                long c1 = hexToLong(tokens[0]);
                long c2 = hexToLong(tokens[1]);
                pairs.add(new long[]{c1, c2});
            }
        }

        reader.close();
        return pairs;
    }

    // Phase 2: “Unpeel” one round using K3
    public static List<int[][]> getRound3Inputs(List<long[]> ciphertextPairs, int k3) {
        List<int[][]> roundInputs = new ArrayList<>();

        for (long[] pair : ciphertextPairs) {
            int[] C1 = splitBlock(pair[0]);
            int[] C2 = splitBlock(pair[1]);

            // Reverse one round of FEAL: compute R2 = L4, L2 = R4 ^ F(L4, K3)
            int R4a = C1[0];
            int R4b = C2[0];

            int L4a = C1[1];
            int L4b = C2[1];

            int F_a = fealF(R4a, k3);
            int F_b = fealF(R4b, k3);

            int L3a = L4a ^ F_a;
            int L3b = L4b ^ F_b;

            int[] stateA = new int[]{L3a, R4a}; // L3, R3
            int[] stateB = new int[]{L3b, R4b};

            roundInputs.add(new int[][]{stateA, stateB});
        }

        return roundInputs;
    }

    // Placeholder FEAL F-function
    public static int fealF(int value, int key) {
        // Placeholder for FEAL F-function from FEAL.java (implement properly!)
        return value ^ key;  // Replace with correct F-function!
    }

    // Phase 3: Attack K2 using round 3 inputs
    public static void attackK2(List<int[][]> roundInputs) {
        System.out.println("Starting brute-force on K2...");
        int candidateCount = 0;

        for (long k2 = 0; k2 <= 0xFFFFFFFFL; k2++) {
            boolean isValid = true;

            for (int[][] pair : roundInputs) {
                int[] a = pair[0]; // {L3, R3}
                int[] b = pair[1];

                int R3a = a[1];
                int R3b = b[1];

                int F_a = fealF(R3a, (int) k2);
                int F_b = fealF(R3b, (int) k2);

                int L4a_calc = a[0] ^ F_a;
                int L4b_calc = b[0] ^ F_b;

                int delta = L4a_calc ^ L4b_calc;

                if ((delta & 0xFF000000) != 0x80000000) { // adjust condition to match expected difference
                    isValid = false;
                    break;
                }
            }

            if (isValid) {
                System.out.printf("Found candidate for K2: %08X\n", k2);
                candidateCount++;
            }

            if (candidateCount >= 5) break;
        }

        System.out.println("K2 attack complete.");
    }

    // Main Function
    public static void main(String[] args) {
        try {
            // Load ciphertext pairs
            List<long[]> ciphertextPairs = loadCiphertextPairs("ciphertexts.txt");

            // Provide K3 candidate (replace with actual candidate you found)
            int K3 = 0xDEADBEEF;  // TODO: Replace with real K3 from Phase 1

            // Derive round 3 inputs using K3
            List<int[][]> roundInputs = getRound3Inputs(ciphertextPairs, K3);

            // Brute-force attack on K2
            attackK2(roundInputs);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
