package com.example.VeristasId.Controller;

import com.example.VeristasId.Dto.AuditLogBlock;
import com.example.VeristasId.Service.BlockchainAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 15 (Part 1) — AuditControllerTest
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: The Blockchain Audit Ledger API
 * ═══════════════════════════════════════════════════════════
 *
 * AuditController exposes the immutable blockchain audit trail
 * that records every single data access in the system.
 *
 *   GET    /api/audit/ledger  → View entire chain
 *   GET    /api/audit/verify  → Prove the chain hasn't been tampered
 *   DELETE /api/audit/reset   → Wipe the chain (dev/demo only)
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 1: @DeleteMapping and HTTP Verbs
 * ═══════════════════════════════════════════════════════════
 *
 * HTTP has 5 main verbs, each with a specific semantic meaning:
 *
 *   GET    → Read data. MUST be safe and idempotent (no side effects).
 *   POST   → Create a new resource. Not idempotent.
 *   PUT    → Replace a resource entirely. Idempotent.
 *   PATCH  → Partially update a resource. Not always idempotent.
 *   DELETE → Remove a resource. Idempotent.
 *
 * Using /reset as a DELETE is correct REST design because:
 *   1. It destroys data (the audit chain).
 *   2. Calling DELETE multiple times has the same end result:
 *      an empty chain. That is what "idempotent" means.
 *
 * Using GET for a destructive operation would be a serious bug
 * because browsers, CDNs, and API gateways cache and pre-fetch
 * GET requests — your chain could be reset without you intending to!
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 2: Mocking void methods with doNothing()
 * ═══════════════════════════════════════════════════════════
 *
 * Mockito's when(...).thenReturn() syntax only works for methods
 * that RETURN a value. For VOID methods (ones that return nothing),
 * the syntax flips:
 *
 *   doNothing().when(mockService).resetChain();
 *
 * This tells Mockito: "When resetChain() is called on the mock,
 * do nothing (as opposed to calling the real implementation)."
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 3: Testing Map<String, Object> with jsonPath
 * ═══════════════════════════════════════════════════════════
 *
 * The /verify endpoint returns a Map<String, Object> containing
 * mixed types: a boolean, an integer, and a String.
 *
 * jsonPath handles each type correctly:
 *   jsonPath("$.isChainMathematicallyValid", is(true))  → boolean check
 *   jsonPath("$.totalBlocks", is(3))                    → integer check
 *   jsonPath("$.statusMessage", containsString("secure")) → string check
 *
 * You do NOT need to cast the values — jsonPath and Hamcrest
 * handle Java-to-JSON type conversion automatically.
 */
@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock private BlockchainAuditService auditService;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuditController controller = new AuditController(auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ─── Helper: build a real AuditLogBlock ───────────────────────────────────
    private AuditLogBlock buildBlock(int index, String previousHash) {
        return new AuditLogBlock(
                index,
                "EMT-9110",           // accessorId
                "99-9999-9999-9999",  // targetAbhaId
                "READ",               // action
                true,                 // accessGranted
                previousHash          // previousHash (SHA-256 of genesis = "0" * 64)
        );
    }

    // ─── GET /api/audit/ledger ────────────────────────────────────────────────

    @Test
    void getLedger_returnsAllBlocks() throws Exception {
        /*
         * The controller simply delegates to auditService.getFullLedger()
         * and wraps the result in a 200 response.
         *
         * We build real AuditLogBlock objects here (not mocks), because
         * AuditLogBlock has a JSON-serialisable structure that Jackson can
         * convert automatically. Using real objects lets us assert on actual
         * field values like "accessorId" and "action".
         *
         * hasSize(2) → Hamcrest matcher that checks the JSON array length.
         * This is more expressive than checking a specific index.
         */
        AuditLogBlock genesis  = buildBlock(0, "0000000000000000000000000000000000000000000000000000000000000000");
        AuditLogBlock blockOne = buildBlock(1, genesis.getHash());

        when(auditService.getFullLedger()).thenReturn(List.of(genesis, blockOne));

        mockMvc.perform(get("/api/audit/ledger"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[0].accessorId",  is("EMT-9110")))
               .andExpect(jsonPath("$[0].action",      is("READ")))
               .andExpect(jsonPath("$[1].index",       is(1)));
    }

    @Test
    void getLedger_emptyChain_returnsEmptyArray() throws Exception {
        /*
         * Edge case: no audit events have been logged yet.
         * The response must be a 200 with an empty JSON array [], NOT a 404.
         *
         * A 404 would imply the resource doesn't exist.
         * An empty list is a valid, existing resource that just has no items yet.
         */
        when(auditService.getFullLedger()).thenReturn(List.of());

        mockMvc.perform(get("/api/audit/ledger"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(0)));
    }

    // ─── GET /api/audit/verify ────────────────────────────────────────────────

    @Test
    void verifyChain_whenIntact_returnsValidTrueAndSecureMessage() throws Exception {
        /*
         * The verify endpoint makes TWO calls to auditService:
         *   1. isChainValid()       → returns boolean
         *   2. getFullLedger().size() → returns block count
         *
         * Both must be stubbed. The response Map has three keys:
         *   - isChainMathematicallyValid (boolean)
         *   - totalBlocks (int)
         *   - statusMessage (String)
         */
        AuditLogBlock block = buildBlock(0, "0".repeat(64));
        when(auditService.isChainValid()).thenReturn(true);
        when(auditService.getFullLedger()).thenReturn(List.of(block));

        mockMvc.perform(get("/api/audit/verify"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.isChainMathematicallyValid", is(true)))
               .andExpect(jsonPath("$.totalBlocks",                is(1)))
               .andExpect(jsonPath("$.statusMessage",              containsString("secure")));
    }

    @Test
    void verifyChain_whenTampered_returnsFalseAndWarning() throws Exception {
        /*
         * CRITICAL test: simulates what happens if someone manually edits
         * a past audit block in the database.
         *
         * The blockchain validation algorithm re-computes each block's
         * SHA-256 hash from its data and checks it against the stored hash.
         * A tampered block will have a mismatched hash, causing isChainValid()
         * to return false.
         *
         * The statusMessage MUST contain "WARNING" in this case so that the
         * SystemStatusPage UI can show a red alert to the administrator.
         */
        when(auditService.isChainValid()).thenReturn(false);
        when(auditService.getFullLedger()).thenReturn(List.of());

        mockMvc.perform(get("/api/audit/verify"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.isChainMathematicallyValid", is(false)))
               .andExpect(jsonPath("$.statusMessage",              containsString("WARNING")));
    }

    @Test
    void verifyChain_totalBlocksMatchesLedgerSize() throws Exception {
        /*
         * Proves the totalBlocks field accurately reflects the number of
         * blocks currently in the ledger — not a hardcoded value.
         *
         * We build 3 blocks in a chain (each references the previous hash)
         * and assert that totalBlocks = 3 in the response.
         */
        AuditLogBlock b0 = buildBlock(0, "0".repeat(64));
        AuditLogBlock b1 = buildBlock(1, b0.getHash());
        AuditLogBlock b2 = buildBlock(2, b1.getHash());

        when(auditService.isChainValid()).thenReturn(true);
        when(auditService.getFullLedger()).thenReturn(List.of(b0, b1, b2));

        mockMvc.perform(get("/api/audit/verify"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalBlocks", is(3)));
    }

    // ─── DELETE /api/audit/reset ──────────────────────────────────────────────

    @Test
    void resetAuditLedger_callsResetChainAndReturnsSuccessMessage() throws Exception {
        /*
         * doNothing().when(mock).voidMethod() — the correct way to stub void methods.
         *
         * We also verify that resetChain() was called EXACTLY ONCE.
         * If someone accidentally calls it twice (e.g., in a loop), that
         * would silently wipe the ledger twice. The times(1) check prevents this.
         */
        doNothing().when(auditService).resetChain();

        mockMvc.perform(delete("/api/audit/reset"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message", containsString("successfully reset")));

        verify(auditService, times(1)).resetChain();
    }
}
