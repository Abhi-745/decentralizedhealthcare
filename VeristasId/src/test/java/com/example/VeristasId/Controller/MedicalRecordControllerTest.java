package com.example.VeristasId.Controller;

import com.example.VeristasId.Model.MedicalRecord;
import com.example.VeristasId.Repository.MedicalRecordRepository;
import com.example.VeristasId.Service.BlockchainAuditService;
import com.example.VeristasId.Service.OpaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 11 — MedicalRecordController Tests
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: The Heart of the Zero-Trust System
 * ═══════════════════════════════════════════════════════════
 *
 * This controller is where every layer of security we have built
 * converges on a single endpoint:
 *
 *   GET /api/medical-records/{abhaId}
 *         │
 *         ├─ Layer 1: Does Authorization header exist + start with "Bearer "?
 *         │           No → 401 Unauthorized (OpaService never called)
 *         │
 *         ├─ Layer 2: Does OPA allow this token for this patient + action?
 *         │           No → 403 Forbidden (DB never queried)
 *         │
 *         ├─ Layer 3: Is the patient in the DB?
 *         │           Yes → 200 with real record data
 *         │           No  → 200 with "Unknown Patient" placeholder
 *         │
 *         └─ Audit: EVERY path (allowed AND denied) writes to the audit chain.
 *
 * The second endpoint:
 *   POST /api/medical-records/provisional
 *     → Creates a temp "John Doe" ID for unidentified emergency patients.
 *     → Public endpoint — no auth required (the patient is unconscious).
 *
 * ═══════════════════════════════════════════════════════════
 *  WHY @MockitoSettings(strictness = LENIENT)?
 * ═══════════════════════════════════════════════════════════
 *
 * The constructor of MedicalRecordController calls:
 *   medicalRecordRepository.findByAbhaId("99-9999-9999-9999")
 *
 * This seeds a demo patient on first run. In tests, this constructor
 * stub is set up in @BeforeEach but NOT "used" by every individual test
 * method (since many tests call different abhaIds).
 *
 * Mockito STRICT mode throws UnnecessaryStubbingException for stubs
 * that exist but aren't "exercised" by a test. LENIENT mode relaxes
 * this rule — the constructor stub is set up once, used by the
 * constructor on every setUp(), and not flagged as unnecessary.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicalRecordControllerTest {

    @Mock private OpaService opaService;
    @Mock private BlockchainAuditService auditService;
    @Mock private MedicalRecordRepository medicalRecordRepository;

    private MockMvc mockMvc;

    // A reusable demo patient record for the "record found" tests
    private MedicalRecord demoPatient;

    @BeforeEach
    void setUp() {
        // ── Constructor stub ─────────────────────────────────────────────────
        // The controller's constructor calls findByAbhaId("99-9999-9999-9999")
        // to check if the demo patient is already seeded.
        // We return a non-empty Optional → the constructor skips seeding → no save() called.
        when(medicalRecordRepository.findByAbhaId("99-9999-9999-9999"))
                .thenReturn(Optional.of(new MedicalRecord()));

        // Build the demo patient used in "record found" tests
        demoPatient = new MedicalRecord();
        demoPatient.setAbhaId("PAT-001");
        demoPatient.setPatientName("Arjun Mehta");
        demoPatient.setBloodGroup("B-Positive");
        demoPatient.setAllergies("None");
        demoPatient.setDiagnosis("Hypertension");

        // Constructor injection — three dependencies
        MedicalRecordController controller =
                new MedicalRecordController(opaService, auditService, medicalRecordRepository);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ─── 1. PROVISIONAL IDENTITY ─────────────────────────────────────────────

    @Test
    void createProvisionalIdentity_returns201WithTempId() throws Exception {
        /*
         * POST /api/medical-records/provisional is a PUBLIC endpoint.
         * No Authorization header needed — the patient is unconscious.
         *
         * The controller generates a random ID with prefix "TEMP-JD-"
         * and saves a "John Doe" placeholder record to the DB.
         *
         * We assert:
         *  - HTTP 201 Created (not 200 OK — this creates a resource)
         *  - Body contains the "TEMP-JD-" prefix
         *  - Body contains "PENDING_BIOMETRIC_MERGE" status
         *  - save() was called exactly once
         */
        mockMvc.perform(post("/api/medical-records/provisional"))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.abhaId", startsWith("TEMP-JD-")))
               .andExpect(jsonPath("$.status", is("PENDING_BIOMETRIC_MERGE")))
               .andExpect(jsonPath("$.patientName", containsString("UNIDENTIFIED")));

        // Use explicit class to disambiguate from Hamcrest's any()
        verify(medicalRecordRepository, times(1))
                .save(org.mockito.ArgumentMatchers.any(MedicalRecord.class));
    }

    // ─── 2. GET RECORD — missing auth header ─────────────────────────────────

    @Test
    void getPatientRecord_noAuthHeader_returns401() throws Exception {
        /*
         * `required = false` on @RequestHeader means Spring won't throw a 400
         * if the header is absent — it passes null to the method.
         * The controller checks authHeader == null → 401.
         *
         * OPA is NEVER consulted when the header is missing.
         * verify(never()) proves the short-circuit.
         */
        mockMvc.perform(get("/api/medical-records/PAT-001"))
               .andExpect(status().isUnauthorized());

        verify(opaService, never()).checkAccess(any(), any(), any());
    }

    @Test
    void getPatientRecord_missingBearerPrefix_returns401() throws Exception {
        /*
         * Header exists but doesn't start with "Bearer " — could be a raw
         * token without the scheme, or a Basic auth header sent by mistake.
         * The controller checks !authHeader.startsWith("Bearer ") → 401.
         */
        mockMvc.perform(get("/api/medical-records/PAT-001")
                .header("Authorization", "InvalidScheme sometoken"))
               .andExpect(status().isUnauthorized());

        verify(opaService, never()).checkAccess(any(), any(), any());
    }

    // ─── 3. GET RECORD — OPA denies ──────────────────────────────────────────

    @Test
    void getPatientRecord_opaDenies_returns403() throws Exception {
        /*
         * Valid Bearer token, but OPA says NO (no active emergency session,
         * wrong role, wrong stage, or consent not granted).
         *
         * Expected:
         *  - HTTP 403 Forbidden
         *  - DB is NEVER queried (opaService gates before DB access)
         *  - Audit records the DENIED attempt (success=false)
         */
        when(opaService.checkAccess(anyString(), eq("PAT-001"), eq("read")))
                .thenReturn(false);

        mockMvc.perform(get("/api/medical-records/PAT-001")
                .header("Authorization", "Bearer someToken"))
               .andExpect(status().isForbidden())
               .andExpect(content().string(containsString("Access Denied")));

        // DB must never be touched when OPA denies
        verify(medicalRecordRepository, never()).findByAbhaId("PAT-001");

        // Denied access MUST be audited
        verify(auditService).recordAccessAttempt(anyString(), eq("PAT-001"), eq("READ"), eq(false));
    }

    // ─── 4. GET RECORD — OPA approves, record exists ─────────────────────────

    @Test
    void getPatientRecord_opaApproves_recordFound_returns200WithPatientData() throws Exception {
        /*
         * The happy path: valid token + OPA approves + record is in the DB.
         *
         * We assert the response JSON contains the actual patient fields.
         * jsonPath("$.patientName") checks the specific JSON key in the Map
         * that the controller builds and serialises.
         */
        when(opaService.checkAccess(anyString(), eq("PAT-001"), eq("read")))
                .thenReturn(true);
        when(medicalRecordRepository.findByAbhaId("PAT-001"))
                .thenReturn(Optional.of(demoPatient));

        mockMvc.perform(get("/api/medical-records/PAT-001")
                .header("Authorization", "Bearer validToken"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.patientName", is("Arjun Mehta")))
               .andExpect(jsonPath("$.bloodGroup", is("B-Positive")))
               .andExpect(jsonPath("$.allergies", is("None")));

        // Approved access MUST be audited
        verify(auditService).recordAccessAttempt(anyString(), eq("PAT-001"), eq("READ"), eq(true));
    }

    // ─── 5. GET RECORD — OPA approves, record NOT in DB ─────────────────────

    @Test
    void getPatientRecord_opaApproves_recordNotFound_returns200WithUnknownPatient() throws Exception {
        /*
         * OPA approves the request (valid emergency session exists for this patient),
         * BUT the patient was never registered in this hospital's DB.
         *
         * This is the "unknown/unregistered patient" scenario — common in
         * emergency situations where the patient comes from another hospital.
         *
         * The controller returns 200 (not 404) with a placeholder record
         * to give the paramedic SOMETHING to work with. The placeholder
         * explicitly says "Unknown Patient (Unregistered)" and "Unknown"
         * for blood group — signalling to use O-Negative as safe default.
         *
         * WHY 200 and not 404?
         * A 404 would tell the paramedic "this ID doesn't exist" which is
         * confusing — the ESID exists (OPA approved it), but the patient
         * record doesn't. 200 with a clear placeholder is more useful.
         */
        when(opaService.checkAccess(anyString(), eq("PAT-UNKNOWN"), eq("read")))
                .thenReturn(true);
        when(medicalRecordRepository.findByAbhaId("PAT-UNKNOWN"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/medical-records/PAT-UNKNOWN")
                .header("Authorization", "Bearer validToken"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.patientName", containsString("Unknown")))
               .andExpect(jsonPath("$.bloodGroup", is("Unknown")));
    }

    // ─── 6. AUDIT TRAIL — both paths recorded ────────────────────────────────

    @Test
    void getPatientRecord_noHeader_auditRecordsDenial() throws Exception {
        /*
         * Even completely unauthenticated requests must be audited.
         * The controller records "Anonymous" as the accessor when there
         * is no token to identify the caller.
         */
        mockMvc.perform(get("/api/medical-records/PAT-001"))
               .andExpect(status().isUnauthorized());

        verify(auditService)
                .recordAccessAttempt(eq("Anonymous"), eq("PAT-001"), eq("READ"), eq(false));
    }

    @Test
    void getPatientRecord_opaApproves_auditRecordsSuccess() throws Exception {
        /*
         * Successful accesses also produce an audit entry with success=true.
         * This gives the hospital a complete picture: who accessed what record
         * and whether they were allowed or blocked.
         */
        when(opaService.checkAccess(anyString(), eq("PAT-001"), eq("read")))
                .thenReturn(true);
        when(medicalRecordRepository.findByAbhaId("PAT-001"))
                .thenReturn(Optional.of(demoPatient));

        mockMvc.perform(get("/api/medical-records/PAT-001")
                .header("Authorization", "Bearer validToken"))
               .andExpect(status().isOk());

        verify(auditService)
                .recordAccessAttempt(anyString(), eq("PAT-001"), eq("READ"), eq(true));
    }
}
