package com.example.VeristasId.Dto;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Day 17 — AuditLogBlockTest
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: Proving the Blockchain's Mathematical Guarantees
 * ═══════════════════════════════════════════════════════════
 *
 * The AuditLogBlock is the most security-critical data structure
 * in the entire system. It provides three provable guarantees:
 *
 *   1. DETERMINISM:  Same event → same hash (always reproducible).
 *   2. AVALANCHE:    Change 1 character → completely different hash.
 *   3. CHAINING:     block[N+1].previousHash == block[N].hash
 *                    Tamper block 5 → blocks 6, 7, 8... become invalid.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 1: SHA-256 in 30 seconds
 * ═══════════════════════════════════════════════════════════
 *
 *   Input:  any string
 *   Output: 256-bit number → expressed as 64 hex characters
 *
 *   Properties:
 *     ✦ Deterministic  — same input ALWAYS → same output
 *     ✦ One-way        — cannot reverse the hash to get input
 *     ✦ Collision-free — P(two inputs → same hash) ≈ 1 in 10^77
 *     ✦ Avalanche      — 1 bit flip in input → ~50% bits flip in output
 *
 *   Used by: Bitcoin, git commits, HTTPS, password databases.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 2: Tamper-Evidence (not Tamper-Prevention)
 * ═══════════════════════════════════════════════════════════
 *
 * You CAN change a record in the database — blockchain does not
 * prevent writes. What it provides is EVIDENCE of tampering:
 *
 *   block[5].accessGranted = false  (changed by attacker)
 *   → block[5].calculateHash() now returns a different value
 *   → block[6].previousHash no longer matches block[5].hash
 *   → isChainValid() returns false
 *   → System alerts administrators of data corruption
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 3: Design-Enforced Immutability
 * ═══════════════════════════════════════════════════════════
 *
 * AuditLogBlock has NO setHash() or setPreviousHash() methods.
 * This is intentional — once sealed, the block cannot be modified
 * via Java code. Only direct database writes can change it, which
 * would be immediately detected by the chain validation algorithm.
 */
class AuditLogBlockTest {

    private static final String GENESIS_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";

    private AuditLogBlock buildBlock(int index, boolean accessGranted, String previousHash) {
        return new AuditLogBlock(index, "EMT-9110", "99-9999-9999-9999",
                "READ", accessGranted, previousHash);
    }

    // ─── GUARANTEE 1: DETERMINISM ─────────────────────────────────────────────

    @Test
    void calculateHash_isDeterministic_sameInputProducesSameHash() {
        /*
         * SHA-256 is a pure function. The same fields always produce
         * the same hash. We use the 8-argument constructor to fix the
         * timestamp (otherwise two calls at different milliseconds would
         * have different timestamps and therefore different hashes).
         */
        long fixedTs = 1_700_000_000_000L;

        // Build two identical blocks with a fixed timestamp
        AuditLogBlock b1 = new AuditLogBlock(0, fixedTs, "EMT-9110",
                "99-9999-9999-9999", "READ", true, GENESIS_HASH, null);
        AuditLogBlock b2 = new AuditLogBlock(0, fixedTs, "EMT-9110",
                "99-9999-9999-9999", "READ", true, GENESIS_HASH, null);

        assertEquals(b1.calculateHash(), b2.calculateHash(),
                "Same input MUST always produce the same SHA-256 hash (Determinism)");
    }

    // ─── GUARANTEE 2: AVALANCHE EFFECT ───────────────────────────────────────

    @Test
    void calculateHash_changingAccessGranted_producesCompletelyDifferentHash() {
        /*
         * The Avalanche Effect: changing just 1 field (accessGranted)
         * produces a completely different 64-character hash.
         *
         * This is what makes tampering detectable — you cannot "tweak"
         * an audit record without destroying its hash fingerprint.
         */
        long fixedTs = 1_700_000_000_000L;

        AuditLogBlock allowed = new AuditLogBlock(1, fixedTs, "SURG-1000",
                "99-9999-9999-9999", "WRITE", true, GENESIS_HASH, null);
        AuditLogBlock denied = new AuditLogBlock(1, fixedTs, "SURG-1000",
                "99-9999-9999-9999", "WRITE", false, GENESIS_HASH, null);

        assertNotEquals(allowed.calculateHash(), denied.calculateHash(),
                "Changing accessGranted must produce a completely different hash (Avalanche Effect)");
    }

    @Test
    void calculateHash_changingAction_producesCompletelyDifferentHash() {
        /*
         * The avalanche effect applies to EVERY field.
         * "READ" vs "WRITE" must produce completely different hashes,
         * preventing an attacker from downgrading a WRITE audit entry.
         */
        long fixedTs = 1_700_000_001_000L;

        AuditLogBlock readBlock  = new AuditLogBlock(2, fixedTs, "EMT",
                "99-9999", "READ",  true, GENESIS_HASH, null);
        AuditLogBlock writeBlock = new AuditLogBlock(2, fixedTs, "EMT",
                "99-9999", "WRITE", true, GENESIS_HASH, null);

        assertNotEquals(readBlock.calculateHash(), writeBlock.calculateHash());
    }

    // ─── GUARANTEE 3: SHA-256 OUTPUT LENGTH ───────────────────────────────────

    @Test
    void hash_isAlways64HexCharacters_sha256Specification() {
        /*
         * SHA-256 outputs exactly 256 bits = 64 hex characters.
         * This is true regardless of how long or short the input is.
         * (A 1-word input and a 1-million-word input both produce 64 chars.)
         *
         * Why does this matter?
         * The database column for hash is @Column(length = 64).
         * If SHA-256 ever produced a different length, the DB would truncate
         * or reject the value — breaking the entire chain.
         */
        AuditLogBlock genesis   = buildBlock(0, true, GENESIS_HASH);
        AuditLogBlock longBlock = new AuditLogBlock(999,
                "did:veritas:paramedic:EMT-9110-station-12-mumbai",
                "99-9999-9999-9999-very-long-id",
                "EMERGENCY_BREAK_GLASS_READ", true, GENESIS_HASH);

        assertEquals(64, genesis.getHash().length(),
                "SHA-256 must always produce exactly 64 hex characters");
        assertEquals(64, longBlock.getHash().length(),
                "Hash length must be 64 regardless of input length");
    }

    // ─── GUARANTEE 4: CHAIN LINKING ───────────────────────────────────────────

    @Test
    void chainLinking_eachBlock_containsHashOfPreviousBlock() {
        /*
         * This is the CORE property that makes a blockchain a blockchain.
         *
         *   block[0].hash → stored as block[1].previousHash
         *   block[1].hash → stored as block[2].previousHash
         *
         * If block[0] is tampered:
         *   block[0].hash changes
         *   block[1].previousHash no longer matches → chain broken
         *   isChainValid() returns false → tampering detected
         */
        AuditLogBlock block0 = buildBlock(0, true, GENESIS_HASH);
        AuditLogBlock block1 = buildBlock(1, true,  block0.getHash());
        AuditLogBlock block2 = buildBlock(2, false, block1.getHash());

        assertEquals(block0.getHash(), block1.getPreviousHash(),
                "Block 1's previousHash must equal Block 0's hash");
        assertEquals(block1.getHash(), block2.getPreviousHash(),
                "Block 2's previousHash must equal Block 1's hash");
    }

    // ─── GUARANTEE 5: IMMUTABILITY ────────────────────────────────────────────

    @Test
    void hash_hasNoPublicSetterMethod_designEnforcedImmutability() {
        /*
         * Immutability is enforced at the Java class design level.
         * This test uses reflection to scan all declared methods and
         * assert that setHash() and setPreviousHash() do NOT exist.
         *
         * If a developer accidentally adds Lombok @Data (which generates
         * all setters), this test immediately catches the regression.
         */
        boolean hasSetHash         = false;
        boolean hasSetPreviousHash = false;

        for (Method method : AuditLogBlock.class.getDeclaredMethods()) {
            if (method.getName().equals("setHash"))         hasSetHash = true;
            if (method.getName().equals("setPreviousHash")) hasSetPreviousHash = true;
        }

        assertFalse(hasSetHash,
                "AuditLogBlock must NOT have setHash() — hash is immutable once sealed");
        assertFalse(hasSetPreviousHash,
                "AuditLogBlock must NOT have setPreviousHash() — chain links are immutable");
    }

    @Test
    void block_hasAllRequiredGetters_forJsonSerialisation() {
        /*
         * "Contract test" — verifies the public API of the DTO.
         * Jackson needs all these getters to serialise the block
         * to JSON for GET /api/audit/ledger.
         *
         * If any getter is removed during refactoring, the API silently
         * drops that field from the JSON response (a breaking change).
         * This test catches that before it reaches production.
         */
        String[] required = {
                "getIndex", "getTimestamp", "getAccessorId", "getTargetAbhaId",
                "getAction", "isAccessGranted", "getPreviousHash", "getHash"
        };

        for (String getter : required) {
            boolean found = false;
            for (Method m : AuditLogBlock.class.getDeclaredMethods()) {
                if (m.getName().equals(getter)) { found = true; break; }
            }
            assertTrue(found, "Required getter '" + getter + "' is missing from AuditLogBlock");
        }
    }
}
