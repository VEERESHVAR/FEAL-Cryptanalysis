import java.io.*;
import java.util.*;

public class FEALPhases3 {

    // Utility Functions

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

    // Load Ciphertext Pairs
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

    // Step 1: Get Round 2 State using known K3 and K2
    public static List<int[][]> getRound2Inputs(List<long[]> ciphertextPairs, int k3, int k2) {
        List<int[][]> round2States = new ArrayList<>();

        for (long[] pair : ciphertextPairs) {
            int[] C1 = splitBlock(pair[0]);
            int[] C2 = splitBlock(pair[1]);

            // Reverse round 4 (K3)
            int R4a = C1[0], L4a = C1[1];
            int R4b = C2[0], L4b = C2[1];

            int L3a = L4a ^ fealF(R4a, k3);
            int L3b = L4b ^ fealF(R4b, k3);

            // Reverse round 3 (K2)
            int R3a = R4a;
            int R3b = R4b;

            int L2a = L3a ^ fealF(R3a, k2);
            int L2b = L3b ^ fealF(R3b, k2);

            round2States.add(new int[][]{
                new int[]{L2a, R3a},  // A: L2, R2
                new int[]{L2b, R3b}   // B: L2', R2'
            });
        }

        return round2States;
    }

    // Step 2: Attack K1
    public static void attackK1(List<int[][]> round2States) {
        System.out.println("Brute-forcing K1...");

        int candidateCount = 0;

        for (long k1 = 0; k1 <= 0xFFFFFFFFL; k1++) {
            boolean isValid = true;

            for (int[][] state : round2States) {
                int[] A = state[0];
                int[] B = state[1];

                int R2a = A[1];
                int R2b = B[1];

                int F_a = fealF(R2a, (int) k1);
                int F_b = fealF(R2b, (int) k1);

                int L3a = A[0] ^ F_a;
                int L3b = B[0] ^ F_b;

                int delta = L3a ^ L3b;

                if ((delta & 0xFF000000) != 0x80000000) { // Adjust this check to match chosen ΔP
                    isValid = false;
                    break;
                }
            }

            if (isValid) {
                System.out.printf("Possible K1 candidate: %08X\n", k1);
                candidateCount++;
            }

            if (candidateCount >= 5) break;
        }

        System.out.println("K1 brute-force complete.");
    }

    // Dummy FEAL F-function
    public static int fealF(int val, int key) {
        // Replace with actual F-function or call FEAL.java
        return val ^ key;
    }

    // Main Method
    public static void main(String[] args) {
        try {
            // Load oracle ciphertexts
            List<long[]> ciphertexts = loadCiphertextPairs("ciphertexts.txt");

            // Provide known values from previous attacks
            int K3 = 0xDEADBEEF; // Replace with actual value from FEALPhases1
            int K2 = 0xFEEDFACE; // Replace with actual value from FEALPhases2

            // Reconstruct round 2 inputs
            List<int[][]> round2Inputs = getRound2Inputs(ciphertexts, K3, K2);

            // Brute-force K1
            attackK1(round2Inputs);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
