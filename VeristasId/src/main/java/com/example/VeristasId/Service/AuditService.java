package com.example.VeristasId.Service;

import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class AuditService {

    // This acts as the "Genesis Block" link
    private String lastBlockHash = "VERISTAS_GENESIS_0000";

    /**
     * Creates a cryptographically linked audit entry.
     */
    public synchronized void logDecision(String subject, String resource, boolean allowed) {
        String result = allowed ? "PERMIT" : "DENY";
        long timestamp = System.currentTimeMillis();

        // The Data Chain: Who + Action + Result + Time + Previous Hash
        String dataToHash = subject + resource + result + timestamp + lastBlockHash;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            String currentHash = Base64.getEncoder().encodeToString(hash);

            // Print the audit block to the console
            System.out.println("\n--- ⛓️ IMMUTABLE AUDIT LOG ENTRY ---");
            System.out.println("USER DID:  " + subject);
            System.out.println("ACTION:    " + resource);
            System.out.println("DECISION:  " + result);
            System.out.println("PREV HASH: " + lastBlockHash);
            System.out.println("CURR HASH: " + currentHash);
            System.out.println("------------------------------------\n");

            // Update the chain head to the current hash
            this.lastBlockHash = currentHash;

        } catch (Exception e) {
            throw new RuntimeException("Audit Hashing Failed: " + e.getMessage());
        }
    }
}