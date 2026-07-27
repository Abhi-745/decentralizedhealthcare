package com.example.VeristasId.Service;

import com.example.VeristasId.Dto.AuditLogBlock;
import com.example.VeristasId.Repository.AuditBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Day 18 — BlockchainAuditServiceTest
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: Testing a Three-Layer Audit System
 * ═══════════════════════════════════════════════════════════
 *
 * BlockchainAuditService maintains three parallel audit layers
 * for every single access event in the system:
 *
 *   Layer 1: In-memory SHA-256 linked chain  (instant, ephemeral)
 *   Layer 2: PostgreSQL persistence           (durable, queryable)
 *   Layer 3: Ethereum smart contract          (on-chain, immutable)
 *
 * Day 18 tests focus on Layer 1 — the in-memory chain logic —
 * because that is pure Java and does not require infrastructure.
 * Layers 2 and 3 are tested via mocks.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 1: @PostConstruct and Unit Testing
 * ═══════════════════════════════════════════════════════════
 *
 * Spring calls @PostConstruct automatically after the bean is
 * created and all dependencies are injected. In unit tests,
 * Spring is NOT running — we create the object manually with
 * "new", so @PostConstruct NEVER fires automatically.
 *
 * The fix: call resetChain() in @BeforeEach.
 *
 *   WHY resetChain() instead of initGenesisBlock()?
 *   resetChain() is guaranteed to create a fresh genesis block
 *   in memory regardless of the database state. It is the
 *   canonical way to initialise a clean chain for tests.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 2: Testing with Internal State via Reflection
 * ═══════════════════════════════════════════════════════════
 *
 * The "blockchain" list is private:
 *   private final List<AuditLogBlock> blockchain = new ArrayList<>();
 *
 * To simulate database tampering in the isChainValid() tests,
 * we need to reach into the service's private field and replace
 * a block with a tampered version. We use:
 *
 *   ReflectionTestUtils.getField(service, "blockchain")
 *
 * This returns the actual live List<> inside the service, which
 * we can then mutate directly. This is the standard Spring
 * approach for white-box testing of internal state.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 3: @MockitoSettings(LENIENT)
 * ═══════════════════════════════════════════════════════════
 *
 * resetChain() calls auditBlockRepository.deleteAll() and
 * auditBlockRepository.save(). We do NOT stub these — Mockito
 * uses defaults (void for deleteAll, null for save).
 *
 * BUT: since we call these in setUp() which runs before every
 * test, and some tests don't interact with the repository at all,
 * Mockito's strict mode would report them as "unnecessary
 * stubbing" and fail the test. LENIENT disables this check.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlockchainAuditServiceTest {

    @Mock AuditBlockRepository     auditBlockRepository;
    @Mock ContractLifecycleService contractService;

    private BlockchainAuditService service;

    @BeforeEach
    void setUp() {
        /*
         * Step 1: Create service manually (@PostConstruct won't fire)
         */
        service = new BlockchainAuditService(auditBlockRepository, contractService);

        /*
         * Step 2: contractService.isActive() defaults to false in Mockito
         * → no Ethereum calls → safe for unit testing without Ganache running.
         *
         * Step 3: resetChain() initialises the in-memory genesis block.
         * This is equivalent to what @PostConstruct does on a fresh DB.
         */
        when(contractService.isActive()).thenReturn(false);
        service.resetChain();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test Group 1: Genesis Block
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void afterReset_chainContainsExactlyOneBlock_theGenesisBlock() {
        /*
         * After resetChain(), the in-memory chain must have exactly 1 block:
         * the Genesis Block. This is the "seed" of the chain —
         * every other block is cryptographically linked back to it.
         */
        List<AuditLogBlock> ledger = service.getFullLedger();

        assertEquals(1, ledger.size(),
                "A fresh chain must contain exactly 1 block (the Genesis Block)");
        assertEquals(0, ledger.get(0).getIndex(),
                "Genesis block must have index 0");
        assertEquals("GENESIS", ledger.get(0).getAction(),
                "Genesis block action must be 'GENESIS'");
        assertEquals("SYSTEM", ledger.get(0).getAccessorId(),
                "Genesis block must be authored by SYSTEM");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test Group 2: recordAccessAttempt — chain growth
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void recordAccessAttempt_addsBlockToChain_incrementsIndex() {
        /*
         * Recording one access event must grow the chain from 1 to 2 blocks.
         * The new block must have index = 1 (sequential, 0-based).
         *
         * This proves the chain counter increments correctly.
         */
        service.recordAccessAttempt("paramedic-EMT-9110", "99-9999-9999-9999", "READ", true);

        List<AuditLogBlock> ledger = service.getFullLedger();
        assertEquals(2, ledger.size(),
                "Chain must have 2 blocks after one recordAccessAttempt() call");
        assertEquals(1, ledger.get(1).getIndex(),
                "Second block must have index 1");
        assertEquals("READ", ledger.get(1).getAction());
        assertTrue(ledger.get(1).isAccessGranted());
    }

    @Test
    void recordAccessAttempt_newBlock_previousHashLinksToGenesisHash() {
        /*
         * THE CHAIN-LINKING TEST — the most important chain property.
         *
         * block[1].previousHash MUST equal block[0].hash (genesis hash).
         *
         * This proves that each new block cryptographically references
         * the previous block, forming the immutable chain.
         */
        service.recordAccessAttempt("surgeon-001", "ABHA-001", "WRITE", true);

        AuditLogBlock genesis = service.getFullLedger().get(0);
        AuditLogBlock block1  = service.getFullLedger().get(1);

        assertEquals(genesis.getHash(), block1.getPreviousHash(),
                "Block 1's previousHash must equal the Genesis block's hash");
    }

    @Test
    void recordAccessAttempt_calledTwice_allThreeBlocksChainCorrectly() {
        /*
         * End-to-end chain: genesis → block1 → block2
         *
         * Verifies that EACH block links to the one before it:
         *   block1.previousHash == genesis.hash
         *   block2.previousHash == block1.hash
         *
         * If either link is broken, the chain is invalid and
         * isChainValid() will catch the tampering.
         */
        service.recordAccessAttempt("EMT-9110", "ABHA-001", "READ",  true);
        service.recordAccessAttempt("SURG-777", "ABHA-001", "WRITE", true);

        List<AuditLogBlock> chain = service.getFullLedger();
        assertEquals(3, chain.size());

        assertEquals(chain.get(0).getHash(), chain.get(1).getPreviousHash(),
                "Block 1 must link to Genesis");
        assertEquals(chain.get(1).getHash(), chain.get(2).getPreviousHash(),
                "Block 2 must link to Block 1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test Group 3: isChainValid()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isChainValid_intactChain_returnsTrue() {
        /*
         * A freshly built chain (genesis only, or with new blocks added
         * through the service) must always pass validation.
         *
         * No tampering has occurred → every hash matches its recomputed value
         * and every previousHash matches the prior block's hash.
         */
        service.recordAccessAttempt("EMT-9110", "ABHA-001", "READ", true);
        service.recordAccessAttempt("SURG-001", "ABHA-001", "WRITE", true);

        assertTrue(service.isChainValid(),
                "An intact chain must be valid");
    }

    @Test
    @SuppressWarnings("unchecked")
    void isChainValid_tamperedBlock_detectsCorruptionAndReturnsFalse() {
        /*
         * TAMPER SIMULATION — this is the core security guarantee.
         *
         * We use ReflectionTestUtils to reach into the private
         * "blockchain" list and replace block[1] with a tampered
         * block that has a WRONG previousHash.
         *
         * WHY is AuditLogBlock immutable (no setHash)?
         * If setHash() existed, an attacker could change the stored hash
         * AFTER modifying the data — making the tampered block
         * appear valid. Immutability prevents this.
         *
         * Instead we replace the entire block with a new one that has
         * incorrect linking data, proving that isChainValid() catches it.
         */
        service.recordAccessAttempt("EMT-9110", "ABHA-001", "READ", true);

        // Get the private blockchain list via reflection
        List<AuditLogBlock> chain =
                (List<AuditLogBlock>) ReflectionTestUtils.getField(service, "blockchain");

        // Replace block[1] with a tampered block whose previousHash is intentionally wrong
        AuditLogBlock tampered = new AuditLogBlock(
                1, "ATTACKER", "ABHA-001", "READ", true,
                "TOTALLY-FORGED-PREVIOUS-HASH-0000000000000000000000000000");
        chain.set(1, tampered);

        // The chain link is broken: chain[0].hash ≠ tampered.previousHash
        assertFalse(service.isChainValid(),
                "isChainValid() must detect a tampered previousHash and return false");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test Group 4: resetChain()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void resetChain_afterMultipleEvents_clearsAllBlocksAndReinitsGenesis() {
        /*
         * After 3 access events (chain size = 4), calling resetChain()
         * must:
         *   1. Clear the in-memory chain
         *   2. Create a NEW genesis block (index 0)
         *
         * Note: This does NOT affect the Ethereum contract — on-chain
         * data is immutable. Only the off-chain layers are reset.
         */
        service.recordAccessAttempt("EMT-001", "ABHA-001", "READ", true);
        service.recordAccessAttempt("EMT-001", "ABHA-001", "READ", true);
        service.recordAccessAttempt("EMT-001", "ABHA-001", "READ", true);
        assertEquals(4, service.getFullLedger().size(), "Setup: chain should have 4 blocks");

        service.resetChain(); // ← wipe and reinit

        assertEquals(1, service.getFullLedger().size(),
                "After reset, chain must contain only the new genesis block");
        assertEquals("GENESIS", service.getFullLedger().get(0).getAction(),
                "After reset, the only block must be a fresh genesis block");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test Group 5: Ethereum Layer — never called when contract is inactive
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void recordAccessAttempt_whenContractInactive_neverCallsEthereum() {
        /*
         * contractService.isActive() returns false (stubbed in setUp).
         * The service must NOT attempt to call the Ethereum contract.
         *
         * This is critical for environments without Ganache/testnet running:
         * the service must degrade gracefully to Layers 1+2 only.
         *
         * verify(..., never()) asserts that getContract() was never called.
         * If it HAD been called, the test would throw NullPointerException
         * (since the mock returns null for getContract()).
         */
        service.recordAccessAttempt("paramedic", "ABHA-001", "READ", true);

        verify(contractService, never()).getContract();
    }
}
