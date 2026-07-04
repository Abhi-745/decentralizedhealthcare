package com.example.VeristasId.Dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Day 4 — AuditLogBlock (SHA-256 Hash Chain) Tests
 *
 * These tests verify the mathematical foundation of the audit system.
 * Key concepts:
 *  - SHA-256 is deterministic: same inputs → ALWAYS same hash
 *  - SHA-256 is sensitive: change ONE character → completely different hash
 *  - Immutability: no setters means Java memory can't be modified after creation
 *  - Chain linking: each block's hash is locked to the previous block's hash
 */
class AuditLogBlockTest {

    // ─── 1. HASH DETERMINISM ──────────────────────────────────────────────
    // SHA-256 must produce the SAME output for the SAME input, every time.
    // If this fails, the audit system cannot be trusted.

    @Test
    void calculateHash_isDeterministic_sameInputSameHash() {
        // Create two identical blocks at the same moment
        // NOTE: We use the DB-reload constructor (which takes timestamp as param)
        // so timestamp is fixed — not Instant.now() which would differ between calls
        long fixedTime = 1000000L;

        AuditLogBlock block1 = new AuditLogBlock(
            1, fixedTime, "Paramedic", "99-9999", "READ", true, "abc123", null);
        // Recalculate hash manually using block1's own data:
        String expectedHash = block1.calculateHash();

        AuditLogBlock block2 = new AuditLogBlock(
            1, fixedTime, "Paramedic", "99-9999", "READ", true, "abc123", null);
        String actualHash = block2.calculateHash();

        // SHA-256 of identical data must always be identical
        assertEquals(expectedHash, actualHash,
            "SHA-256 is deterministic — same inputs must produce same hash");
    }

    // ─── 2. HASH SENSITIVITY ─────────────────────────────────────────────
    // Change ANY single field → completely different hash.
    // This is the "avalanche effect" — the core of cryptographic security.

    @Test
    void calculateHash_changeAccessorId_producesCompletelyDifferentHash() {
        long fixedTime = 1000000L;

        AuditLogBlock original = new AuditLogBlock(
            1, fixedTime, "Paramedic", "99-9999", "READ", true, "abc123", null);

        // Same block, but accessorId changed from "Paramedic" to "HACKER"
        AuditLogBlock tampered = new AuditLogBlock(
            1, fixedTime, "HACKER", "99-9999", "READ", true, "abc123", null);

        assertNotEquals(original.calculateHash(), tampered.calculateHash(),
            "Changing accessorId must completely change the hash (avalanche effect)");
    }

    @Test
    void calculateHash_changeAccessGranted_producesCompletelyDifferentHash() {
        long fixedTime = 1000000L;

        // OPA DENIED access
        AuditLogBlock denied = new AuditLogBlock(
            1, fixedTime, "Paramedic", "99-9999", "READ", false, "prev", null);

        // A corrupt admin changes accessGranted=false → true in the DB
        // When recalculated, hash will NOT match stored hash → TAMPER DETECTED
        AuditLogBlock manipulated = new AuditLogBlock(
            1, fixedTime, "Paramedic", "99-9999", "READ", true, "prev", null);

        assertNotEquals(denied.calculateHash(), manipulated.calculateHash(),
            "Flipping accessGranted must change the hash — DB manipulation is detectable");
    }

    @Test
    void calculateHash_changeTargetAbhaId_producesCompletelyDifferentHash() {
        long fixedTime = 1000000L;

        AuditLogBlock real = new AuditLogBlock(
            1, fixedTime, "Surgeon", "99-9999-1111-2222", "WRITE", true, "prev", null);

        // Attacker changes patient ID — hiding that they accessed patient X's record
        AuditLogBlock faked = new AuditLogBlock(
            1, fixedTime, "Surgeon", "00-0000-9999-FAKE", "WRITE", true, "prev", null);

        assertNotEquals(real.calculateHash(), faked.calculateHash(),
            "Changing targetAbhaId must change the hash — record swapping is detectable");
    }

    // ─── 3. HASH FORMAT ──────────────────────────────────────────────────
    // SHA-256 produces exactly 256 bits = 32 bytes = 64 hexadecimal characters.
    // This is a mathematical constant — never 63, never 65.

    @Test
    void hash_isExactly64Characters_sha256OutputSize() {
        AuditLogBlock genesis = new AuditLogBlock(
            0, 0L, "SYSTEM", "NONE", "GENESIS", true, "0", null);
        assertEquals(64, genesis.calculateHash().length(),
            "SHA-256 always produces exactly 64 hex chars (256 bits = 32 bytes = 64 hex)");
    }

    @Test
    void hash_containsOnlyHexCharacters() {
        AuditLogBlock block = new AuditLogBlock(
            0, 0L, "SYSTEM", "NONE", "GENESIS", true, "0", null);
        String hash = block.calculateHash();

        // Regex: only lowercase hex digits, exactly 64 of them
        assertTrue(hash.matches("[0-9a-f]{64}"),
            "SHA-256 output must be lowercase hex only — got: " + hash);
    }

    // ─── 4. THE CHAIN LINKING ────────────────────────────────────────────
    // Each block contains the PREVIOUS block's hash.
    // This creates an unbreakable chain: you cannot delete or modify
    // block N without invalidating block N+1, N+2, etc.

    @Test
    void chain_previousHashLinking_isCorrect() {
        // Build a 3-block chain manually
        AuditLogBlock genesis = new AuditLogBlock(
            0, "SYSTEM", "NONE", "GENESIS", true, "0");
        AuditLogBlock block1 = new AuditLogBlock(
            1, "Paramedic", "PAT-001", "READ", true, genesis.getHash());
        AuditLogBlock block2 = new AuditLogBlock(
            2, "Surgeon", "PAT-001", "WRITE", true, block1.getHash());

        // Each block's previousHash must equal the prior block's actual hash
        assertEquals("0",                  genesis.getPreviousHash(), "Genesis previous = '0'");
        assertEquals(genesis.getHash(),    block1.getPreviousHash(),  "Block1 previous = genesis hash");
        assertEquals(block1.getHash(),     block2.getPreviousHash(),  "Block2 previous = block1 hash");
    }

    @Test
    void chain_tampering_propagatesAndBreaksSubsequentBlocks() {
        // Build a 3-block chain
        AuditLogBlock genesis = new AuditLogBlock(
            0, "SYSTEM", "NONE", "GENESIS", true, "0");
        AuditLogBlock block1 = new AuditLogBlock(
            1, "Paramedic", "PAT-001", "READ", true, genesis.getHash());
        AuditLogBlock block2 = new AuditLogBlock(
            2, "Surgeon", "PAT-001", "WRITE", true, block1.getHash());

        // ⚠️ TAMPER: Attacker replaces block1 with a fake one
        // (e.g., changes accessGranted from true to false to erase access record)
        AuditLogBlock tamperedBlock1 = new AuditLogBlock(
            1, "FAKE_PARAMEDIC", "PAT-001", "READ", false, genesis.getHash());

        // tamperedBlock1 has a different hash than original block1
        assertNotEquals(block1.getHash(), tamperedBlock1.getHash(),
            "Tampered block must have different hash");

        // block2 stored the ORIGINAL block1's hash as its previousHash
        // Now block2.previousHash ≠ tamperedBlock1.hash → CHAIN BROKEN → DETECTED
        assertNotEquals(block2.getPreviousHash(), tamperedBlock1.getHash(),
            "Tampering block1 invalidates block2's previousHash → tamper detected");
    }

    // ─── 5. IMMUTABILITY ─────────────────────────────────────────────────
    // AuditLogBlock has NO setters — this is enforced at the Java language level.
    // Once created, values cannot be changed in memory.

    @Test
    void immutability_noSetterMethods_exist() throws NoSuchMethodException {
        // If any setter exists, this test fails — immutability is broken
        Class<?> clazz = AuditLogBlock.class;

        // Verify setters do NOT exist for critical fields
        assertThrows(NoSuchMethodException.class, () ->
            clazz.getMethod("setHash", String.class),
            "setHash must not exist — hash must be immutable");

        assertThrows(NoSuchMethodException.class, () ->
            clazz.getMethod("setAccessGranted", boolean.class),
            "setAccessGranted must not exist — audit records are write-once");

        assertThrows(NoSuchMethodException.class, () ->
            clazz.getMethod("setAccessorId", String.class),
            "setAccessorId must not exist — who accessed cannot be changed");
    }

    @Test
    void getters_returnCorrectValues_noMutation() {
        AuditLogBlock block = new AuditLogBlock(
            3, "Surgeon", "99-1234", "WRITE", true, "prevhash123");

        // Getters return what was set in constructor — nothing can change them
        assertEquals(3,             block.getIndex());
        assertEquals("Surgeon",     block.getAccessorId());
        assertEquals("99-1234",     block.getTargetAbhaId());
        assertEquals("WRITE",       block.getAction());
        assertTrue(block.isAccessGranted());
        assertEquals("prevhash123", block.getPreviousHash());
        assertNotNull(block.getHash(),
            "Hash must be auto-calculated in constructor");
    }
}
