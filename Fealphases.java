import java.io.*;
import java.util.*;

public class FEALPhases {

    
    public static long hexToLong(String hex) {
        return Long.parseUnsignedLong(hex, 16);
    }

    
    public static String longToHex(long value) {
        return String.format("%016X", value);
    }

    
    public static long xor64(long a, long b) {
        return a ^ b;
    }

   
    public static int[] splitBlock(long block) {
        int left = (int) (block >>> 32);
        int right = (int) block;
        return new int[]{left, right};
    }

   
    public static long joinBlock(int left, int right) {
        return ((long) left << 32) | (right & 0xFFFFFFFFL);
    }

    
    public static void generatePlaintextPairs(String filename, int numPairs, int deltaL) throws IOException {
        Random rand = new Random();
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

        for (int i = 0; i < numPairs; i++) {
            int L1 = rand.nextInt();
            int R1 = rand.nextInt();
            int L2 = L1 ^ deltaL;
            int R2 = R1; // no change in R

            long P1 = joinBlock(L1, R1);
            long P2 = joinBlock(L2, R2);

            writer.write(longToHex(P1) + " " + longToHex(P2));
            writer.newLine();
        }

        writer.close();
        System.out.println("Plaintext pairs written to " + filename);
    }

    
    public static List<long[]> loadCiphertextPairs(String filename) throws IOException {
        List<long[]> pairs = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length != 2) continue;

            long c1 = hexToLong(parts[0]);
            long c2 = hexToLong(parts[1]);
            pairs.add(new long[]{c1, c2});
        }

        reader.close();
        return pairs;
    }

    
    public static void attackK3(List<long[]> ciphertextPairs) {
        

        System.out.println("Attacking K3...");
        int candidateCount = 0;

        for (long k3 = 0; k3 <= 0xFFFFFFFFL; k3++) {
            boolean valid = true;

            for (long[] pair : ciphertextPairs) {
                long c1 = pair[0];
                long c2 = pair[1];

                int[] c1Parts = splitBlock(c1);
                int[] c2Parts = splitBlock(c2);

                
                if (!validateDifference(c1Parts, c2Parts, (int) k3)) {
                    valid = false;
                    break;
                }
            }

            if (valid) {
                System.out.printf("Possible K3 candidate: %08X%n", k3);
                candidateCount++;
            }

            
            if (candidateCount > 10) break;
        }

        System.out.println("K3 attack complete.");
    }


    public static boolean validateDifference(int[] c1, int[] c2, int k3) {
        
        return Math.abs(c1[0] - c2[0]) < 1000000;
    }

    
    public static void main(String[] args) {
        try {
          
            generatePlaintextPairs("plaintexts.txt", 4, 0x80800000);

            
            List<long[]> ciphertextPairs = loadCiphertextPairs("ciphertexts.txt");

            
            attackK3(ciphertextPairs);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

