import java.io.*;
import java.util.*;

public class FEALPhases1 {

    // Utility Functions

    // Convert a 64-bit hex string to long
    public static long hexToLong(String hex) {
        return Long.parseUnsignedLong(hex, 16);
    }

    // Convert long to 16-character hex
    public static String longToHex(long value) {
        return String.format("%016X", value);
    }

    // Join two 32-bit integers into a 64-bit block
    public static long joinBlock(int L, int R) {
        return ((long) L << 32) | (R & 0xFFFFFFFFL);
    }

    // Split a 64-bit block into two 32-bit integers
    public static int[] splitBlock(long block) {
        int L = (int) (block >>> 32);
        int R = (int) block;
        return new int[]{L, R};
    }

    // XOR two 64-bit blocks
    public static long xor64(long a, long b) {
        return a ^ b;
    }

    // Phase 1: Generate Chosen Plaintext Pairs
    public static void generatePlaintextPairs(String outputFile, int numPairs, int inputDifference) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        Random random = new Random();

        for (int i = 0; i < numPairs; i++) {
            int L1 = random.nextInt();
            int R1 = random.nextInt();
            int L2 = L1 ^ inputDifference;
            int R2 = R1;

            long P1 = joinBlock(L1, R1);
            long P2 = joinBlock(L2, R2);

            writer.write(longToHex(P1) + " " + longToHex(P2));
            writer.newLine();
        }

        writer.close();
        System.out.println("Generated " + numPairs + " plaintext pairs to: " + outputFile);
    }

    // Phase 2: Load Ciphertext Pairs from Oracle
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
        System.out.println("Loaded " + pairs.size() + " ciphertext pairs.");
        return pairs;
    }

    // Phase 3: Attack K3 Using Differential Cryptanalysis 
    public static void attackK3(List<long[]> ciphertextPairs) {
        System.out.println("Beginning attack on K3...");

        int validK3Count = 0;

        // Brute-force all 32-bit values of K3
        for (long k3 = 0; k3 <= 0xFFFFFFFFL; k3++) {
            boolean isValid = true;

            for (long[] pair : ciphertextPairs) {
                int[] c1 = splitBlock(pair[0]);
                int[] c2 = splitBlock(pair[1]);

                
                if (!validateWithF(c1, c2, (int) k3)) {
                    isValid = false;
                    break;
                }
            }

            if (isValid) {
                System.out.printf("Found valid K3 candidate: %08X%n", k3);
                validK3Count++;
            }

            if (validK3Count >= 5) break; // early stop after 5 candidates (adjust as needed)
        }

        System.out.println("K3 Attack complete. Candidates found: " + validK3Count);
    }

    // Dummy validation method
    public static boolean validateWithF(int[] c1, int[] c2, int k3) {
        // NOTE: Replace this logic with real FEAL round inverse using F-function
        int deltaL = c1[0] ^ c2[0];
        return (deltaL & 0xFF000000) == 0x80000000; // crude check, change to real differential logic
    }

    // Main Function
    public static void main(String[] args) {
        try {
            // Phase 1: Generate plaintext pairs with delta = 0x80800000 (left half diff)
            generatePlaintextPairs("plaintexts.txt", 4, 0x80800000);

            // Phase 2: Load ciphertexts from the oracle after encryption
            List<long[]> ciphertextPairs = loadCiphertextPairs("ciphertexts.txt");

            // Phase 3: Run brute-force differential attack on K3
            attackK3(ciphertextPairs);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
