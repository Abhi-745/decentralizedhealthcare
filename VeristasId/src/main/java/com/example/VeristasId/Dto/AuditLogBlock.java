package com.example.VeristasId.Dto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

public class AuditLogBlock {

    private int index;
    private long timestamp;
    private String accessorId; // e.g., the paramedic's token/role
    private String targetAbhaId;
    private String action; // "READ", "WRITE"
    private boolean accessGranted; // Did OPA allow it or block it?

    private String previousHash;
    private String hash; // The cryptographic seal of THIS block

    // Constructor for creating NEW blocks
    public AuditLogBlock(int index, String accessorId, String targetAbhaId, String action, boolean accessGranted, String previousHash) {
        this.index = index;
        this.timestamp = Instant.now().toEpochMilli();
        this.accessorId = accessorId;
        this.targetAbhaId = targetAbhaId;
        this.action = action;
        this.accessGranted = accessGranted;
        this.previousHash = previousHash;
        this.hash = calculateHash();
    }

    // Constructor for RELOADING existing blocks from the database
    public AuditLogBlock(int index, long timestamp, String accessorId, String targetAbhaId, String action, boolean accessGranted, String previousHash, String hash) {
        this.index = index;
        this.timestamp = timestamp;
        this.accessorId = accessorId;
        this.targetAbhaId = targetAbhaId;
        this.action = action;
        this.accessGranted = accessGranted;
        this.previousHash = previousHash;
        this.hash = hash;
    }

    // SHA-256 Cryptographic Hashing Function
    public String calculateHash() {
        String dataToHash = index + Long.toString(timestamp) + accessorId + targetAbhaId + action + accessGranted + previousHash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing algorithm failed", e);
        }
    }

    // --- GETTERS (No Setters! Immutability means data cannot be changed once created) ---
    public int getIndex() { return index; }
    public long getTimestamp() { return timestamp; }
    public String getAccessorId() { return accessorId; }
    public String getTargetAbhaId() { return targetAbhaId; }
    public String getAction() { return action; }
    public boolean isAccessGranted() { return accessGranted; }
    public String getPreviousHash() { return previousHash; }
    public String getHash() { return hash; }
}