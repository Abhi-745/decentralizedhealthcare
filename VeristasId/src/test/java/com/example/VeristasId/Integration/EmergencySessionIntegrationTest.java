package com.example.VeristasId.Integration;

import com.example.VeristasId.Repository.AuditBlockRepository;
import com.example.VeristasId.Repository.EmergencySessionRepository;
import com.example.VeristasId.Service.ContractLifecycleService;
import com.example.VeristasId.Service.CredentialService;
import com.example.VeristasId.Service.JwtService;
import com.example.VeristasId.Service.OpaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 19 — EmergencySessionIntegrationTest
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: The Testing Pyramid
 * ═══════════════════════════════════════════════════════════
 *
 * We have been writing UNIT tests since Day 5:
 *   - Isolated classes, all dependencies mocked
 *   - Fast (<10 ms per test), no Spring context
 *   - MockMvc built manually with standaloneSetup()
 *
 * Day 19 introduces INTEGRATION tests:
 *   - Full Spring Boot application context loaded
 *   - REAL H2 in-memory database (replacing PostgreSQL for tests)
 *   - REAL EmergencySessionRepository querying REAL tables
 *   - HTTP call → Controller → Service → Repository → DB → response
 *
 * The testing pyramid:
 *
 *     /─────────────\     ← E2E tests (few, slow, fragile)
 *    /───────────────\    ← Integration tests  ← YOU ARE HERE
 *   /─────────────────\   ← Unit tests (many, fast, isolated)
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 1: @SpringBootTest vs @WebMvcTest
 * ═══════════════════════════════════════════════════════════
 *
 * @WebMvcTest(EmergencySessionController.class)
 *   → Loads ONLY the web layer (controller + serialisation)
 *   → All services and repositories must be @MockBean
 *   → Fast (~200ms startup)
 *   → Used in Days 10-16 for controller unit tests
 *
 * @SpringBootTest
 *   → Loads the COMPLETE application context
 *   → ALL beans are real (services, repositories, security config)
 *   → Only replace external dependencies with @MockBean
 *   → Slower (~3s startup) but tests cross-layer interactions
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 2: @MockBean vs @Mock
 * ═══════════════════════════════════════════════════════════
 *
 * @Mock (Mockito)
 *   → Creates a mock outside the Spring context
 *   → Used in pure unit tests with @ExtendWith(MockitoExtension.class)
 *   → Does NOT replace the real bean in the Spring context
 *
 * @MockBean (Spring Boot Test)
 *   → Creates a mock AND registers it in the Spring application context
 *   → Replaces the real bean for the duration of the test class
 *   → Any component that @Autowires the bean gets the mock instead
 *
 * We use @MockBean for:
 *   - JwtService: avoids needing a real secret key in test config
 *   - OpaService: no OPA server running in CI
 *   - ContractLifecycleService: no Ethereum/Ganache in tests
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 3: H2 Auto-Configuration in Tests
 * ═══════════════════════════════════════════════════════════
 *
 * Spring Boot Test detects that there is no production datasource
 * available (no PostgreSQL in CI), and AUTOMATICALLY substitutes
 * an H2 in-memory database. Hibernate creates all tables from the
 * @Entity classes (spring.jpa.hibernate.ddl-auto=create-drop).
 *
 * This means every test gets a fresh, clean database — no manual
 * setup SQL needed. Just add H2 to test dependencies.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 4: @AfterEach cleanup for Integration Tests
 * ═══════════════════════════════════════════════════════════
 *
 * Unlike unit tests (which use in-memory mocks), integration tests
 * write REAL data to the H2 database. Without cleanup, test data
 * from Test A can affect Test B.
 *
 * Solution: @AfterEach deleteAll() resets tables between tests.
 * This is simpler than @Transactional (which has subtleties with
 * MockMvc — HTTP requests run in their own transaction, not the
 * test's transaction, so @Transactional doesn't auto-rollback
 * data created by the controller).
 */
@SpringBootTest
@AutoConfigureMockMvc
class EmergencySessionIntegrationTest {

    @Autowired MockMvc mockMvc;

    // Real repositories — wired to the real H2 database
    @Autowired EmergencySessionRepository sessionRepo;
    @Autowired AuditBlockRepository       auditBlockRepo;

    // External dependencies replaced with mocks in the Spring context
    @MockBean JwtService              jwtService;
    @MockBean CredentialService       credentialService;   // needed by OpaSecurityFilter
    @MockBean OpaService              opaService;
    @MockBean ContractLifecycleService contractService;

    private static final String DISPATCHER_TOKEN = "Bearer mock-dispatcher-jwt";
    private static final String PARAMEDIC_TOKEN  = "Bearer mock-paramedic-jwt";

    @BeforeEach
    void configureSecurityFilter() {
        /*
         * OpaSecurityFilter runs BEFORE every controller and checks:
         *   credentialService.verifyVC(authHeader) || jwtService.verifyStaffToken(authHeader)
         *
         * In integration tests no real VC or JWT is issued, so we stub both
         * services globally:
         *   - credentialService.verifyVC(any)  → false  (no patient VC in tests)
         *   - jwtService.verifyStaffToken(any) → true   (any token passes the filter)
         *
         * The controller then receives the request and applies its own role check
         * via jwtService.extractRole() — which specific tests stub to "dispatcher"
         * or "paramedic" as needed.
         *
         * WHY lenient()?
         * Some tests don't call these stubs (e.g. duplicate-ESID test only cares
         * about the 409 path). lenient() prevents Mockito's strict-stub checker
         * from failing those tests for "unnecessary stubbing".
         */
        lenient().when(credentialService.verifyVC(anyString())).thenReturn(false);
        lenient().when(jwtService.verifyStaffToken(anyString())).thenReturn(true);
    }

    @AfterEach
    void cleanupDatabase() {
        sessionRepo.deleteAll();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void stubDispatcher() {
        when(jwtService.verifyStaffToken(DISPATCHER_TOKEN)).thenReturn(true);
        when(jwtService.extractRole(DISPATCHER_TOKEN)).thenReturn("dispatcher");
    }

    private void stubParamedic() {
        when(jwtService.verifyStaffToken(PARAMEDIC_TOKEN)).thenReturn(true);
        when(jwtService.extractRole(PARAMEDIC_TOKEN)).thenReturn("paramedic");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Happy Path — create session, verify in real H2 database
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createSession_validDispatcher_persistedToH2Database() throws Exception {
        /*
         * THE KEY TEST: Proves that the full stack works end-to-end.
         *
         * HTTP Request
         *   ↓
         * EmergencySessionController.createEmergency()
         *   ↓ calls jwtService.verifyStaffToken() → true (mocked)
         *   ↓ calls jwtService.extractRole() → "dispatcher" (mocked)
         *   ↓
         * sessionRepository.save(newSession)
         *   ↓ writes to REAL H2 in-memory table
         *   ↓
         * HTTP Response 200 OK
         *   ↓
         * sessionRepo.findById("ESID-INT-001")
         *   → returns the REAL saved entity from H2 ✓
         *
         * This cannot be tested by a unit test because it requires
         * the real repository + database interaction.
         */
        stubDispatcher();

        mockMvc.perform(post("/api/emergency/create")
                .header("Authorization", DISPATCHER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"esid\":\"ESID-INT-001\",\"patientId\":\"PAT-INT-001\"}"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("ESID-INT-001")));

        // Verify the entity is ACTUALLY in the H2 database
        assertTrue(sessionRepo.findById("ESID-INT-001").isPresent(),
                "Session must be persisted to the H2 database after a successful create call");
        assertEquals("dispatched", sessionRepo.findById("ESID-INT-001").get().getStage(),
                "Newly created session must start in 'dispatched' stage");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Role enforcement — paramedic cannot create a session
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createSession_paramedicRole_returns403_noDataPersisted() throws Exception {
        /*
         * Role-Based Access Control test:
         * Even with a VALID JWT, a paramedic cannot create an emergency session.
         * Only dispatchers hold the authority to initiate emergencies.
         *
         * This also verifies the "fail-fast" pattern: if the role check fails,
         * the controller returns immediately — no database write occurs.
         */
        stubParamedic();

        mockMvc.perform(post("/api/emergency/create")
                .header("Authorization", PARAMEDIC_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"esid\":\"ESID-INT-002\",\"patientId\":\"PAT-INT-002\"}"))
               .andExpect(status().isForbidden())
               .andExpect(content().string(containsString("dispatcher")));

        // The database must be empty — the 403 must have prevented the save
        assertFalse(sessionRepo.findById("ESID-INT-002").isPresent(),
                "A 403 response must NOT write any data to the database");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Idempotency — duplicate ESID returns 409
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createSession_duplicateEsid_returns409Conflict() throws Exception {
        /*
         * The ESID (Emergency Session ID) must be globally unique.
         * Creating the same ESID twice is a conflict (409).
         *
         * Real-world analogy: Two dispatchers accidentally creating
         * the same emergency session for the same patient.
         */
        stubDispatcher();

        // First create — should succeed
        mockMvc.perform(post("/api/emergency/create")
                .header("Authorization", DISPATCHER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"esid\":\"ESID-DUP-001\",\"patientId\":\"PAT-001\"}"))
               .andExpect(status().isOk());

        // Second create with same ESID — must be rejected
        mockMvc.perform(post("/api/emergency/create")
                .header("Authorization", DISPATCHER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"esid\":\"ESID-DUP-001\",\"patientId\":\"PAT-001\"}"))
               .andExpect(status().isConflict());

        // Only 1 session should exist — not 2
        assertEquals(1, sessionRepo.count(),
                "Duplicate ESIDs must not result in duplicate database rows");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: Stage state machine — create then advance stage
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void updateStage_existingSession_stageUpdatedInRealDatabase() throws Exception {
        /*
         * The Emergency Session State Machine:
         *   dispatched → arrived → treatment → stabilised → completed
         *
         * This test directly verifies that calling /api/emergency/update-stage
         * actually persists the new stage to the H2 database.
         *
         * A unit test could only verify that sessionRepository.save() was
         * called — it cannot verify that the data was ACTUALLY stored
         * because the repository is mocked. This integration test closes
         * that gap by checking the real H2 row after the HTTP call.
         */
        stubDispatcher();

        // Setup: create the session first
        mockMvc.perform(post("/api/emergency/create")
                .header("Authorization", DISPATCHER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"esid\":\"ESID-STAGE-001\",\"patientId\":\"PAT-STAGE-001\"}"))
               .andExpect(status().isOk());

        // Advance stage from "dispatched" to "arrived"
        // Note: update-stage has no role check in the controller, but
        // OpaSecurityFilter still requires ANY valid token in the header.
        mockMvc.perform(post("/api/emergency/update-stage")
                .header("Authorization", DISPATCHER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"esid\":\"ESID-STAGE-001\",\"stage\":\"arrived\"}"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("arrived")));

        // Verify the REAL database has the updated stage
        String actualStage = sessionRepo.findById("ESID-STAGE-001")
                .orElseThrow()
                .getStage();
        assertEquals("arrived", actualStage,
                "Stage update must be persisted to the H2 database");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: GET endpoint — retrieves and serialises from real DB
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getSession_existingSession_returnsJsonWithCorrectFields() throws Exception {
        /*
         * Full-stack read test:
         * Session is written to H2 via the POST endpoint, then
         * retrieved via the GET endpoint and verified as JSON.
         *
         * This proves the Jackson serialisation → @Entity → H2 round-trip
         * works end-to-end with no data loss.
         */
        stubDispatcher();

        mockMvc.perform(post("/api/emergency/create")
                .header("Authorization", DISPATCHER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"esid\":\"ESID-GET-001\",\"patientId\":\"PAT-GET-001\"}"))
               .andExpect(status().isOk());

        // GET also requires the token to pass OpaSecurityFilter
        mockMvc.perform(get("/api/emergency/ESID-GET-001")
                .header("Authorization", DISPATCHER_TOKEN))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.esid").value("ESID-GET-001"))
               .andExpect(jsonPath("$.patientId").value("PAT-GET-001"))
               .andExpect(jsonPath("$.stage").value("dispatched"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6: Not Found — unknown ESID returns 404
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getSession_unknownEsid_returns404() throws Exception {
        /*
         * Negative test: an ESID that does not exist in the database
         * must return 404 Not Found. This prevents the frontend
         * from displaying stale or non-existent session data.
         */
        mockMvc.perform(get("/api/emergency/ESID-DOES-NOT-EXIST")
                .header("Authorization", DISPATCHER_TOKEN))
               .andExpect(status().isNotFound());
    }
}
