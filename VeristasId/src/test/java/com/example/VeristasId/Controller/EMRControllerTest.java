package com.example.VeristasId.Controller;

import com.example.VeristasId.Dto.EMRUpdateRequest;
import com.example.VeristasId.Model.MedicalRecord;
import com.example.VeristasId.Repository.ConsentRepository;
import com.example.VeristasId.Repository.MedicalRecordRepository;
import com.example.VeristasId.Service.BlockchainAuditService;
import com.example.VeristasId.Service.CredentialService;
import com.example.VeristasId.Service.JwtService;
import com.example.VeristasId.Service.OpaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 16 (Part 3) — EMRControllerTest
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: The Most Complex Controller in the System
 * ═══════════════════════════════════════════════════════════
 *
 * EMRController manages Electronic Medical Records (EMR) — the
 * real-time clinical data (diagnosis, vitals, prescriptions) written
 * by hospital staff during an emergency.
 *
 * It has 6 dependencies and implements the most important
 * security decision in the system:
 *
 *   "A staff member can update an EMR if and only if they have
 *    EITHER an active OPA emergency override OR explicit patient consent."
 *
 * This is the OR security gate:
 *   isEmergencyOverride  ← checked by OPA (role + session stage)
 *   hasConsent           ← checked by ConsentRepository
 *
 *   if (!isEmergencyOverride && !hasConsent) → 403 FORBIDDEN
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 1: The Double-Auth Pattern
 * ═══════════════════════════════════════════════════════════
 *
 * Step 1 of every EMR endpoint:
 *   boolean isValidPatient = credentialService.verifyVC(token);
 *   boolean isValidStaff   = jwtService.verifyStaffToken(token);
 *   if (!isValidPatient && !isValidStaff) → 401 UNAUTHORIZED
 *
 * The system accepts EITHER a patient's VC token (for self-access)
 * OR a staff JWT (for staff access). This dual-auth pattern means
 * a single endpoint serves two very different caller types.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 2: Optional.map() and orElseGet()
 * ═══════════════════════════════════════════════════════════
 *
 * The GET endpoint uses Java's Optional API for elegant null handling:
 *
 *   return medicalRecordRepository.findByPatientDid(patientDid)
 *       .map(ResponseEntity::ok)          // if found: wrap in 200
 *       .orElseGet(() -> {                // if NOT found: return blank
 *           MedicalRecord blank = new MedicalRecord();
 *           blank.setDiagnosis("No records found");
 *           return ResponseEntity.ok(blank); // still 200, not 404!
 *       });
 *
 * WHY return 200 with a blank record instead of 404?
 * For the frontend demo: a 404 would cause the UI to show an error.
 * A blank record with "No records found" gives the UI useful data.
 * In production, this decision would depend on the healthcare standard.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 3: Mocking 6 Dependencies
 * ═══════════════════════════════════════════════════════════
 *
 * Instead of @InjectMocks (which requires knowing field names),
 * we use the @BeforeEach constructor approach. This is more explicit
 * and educational: it shows exactly what the constructor expects.
 *
 * This also demonstrates why having 6 constructor parameters is a
 * design smell (a class that "knows too much"), and hints at future
 * refactoring: the credential + consent checks could move to a
 * dedicated AccessDecisionService.
 */
@ExtendWith(MockitoExtension.class)
class EMRControllerTest {

    @Mock CredentialService      credentialService;
    @Mock JwtService             jwtService;
    @Mock OpaService             opaService;
    @Mock ConsentRepository      consentRepository;
    @Mock MedicalRecordRepository medicalRecordRepository;
    @Mock BlockchainAuditService blockchainAuditService;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String PATIENT_DID = "did:veritas:patient-001";
    private static final String CALLER_DID  = "did:veritas:surgeon-001";
    private static final String TOKEN       = "Bearer valid-staff-jwt";

    @BeforeEach
    void setUp() {
        /*
         * Constructor injection — explicit and safe.
         * We know the exact order Spring Boot uses to wire this controller.
         * Any future change to the constructor will break this test
         * immediately (a good thing — tests act as a contract).
         */
        EMRController controller = new EMRController(
                credentialService, jwtService, opaService,
                consentRepository, medicalRecordRepository, blockchainAuditService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private EMRUpdateRequest buildRequest() {
        return new EMRUpdateRequest("ESID-001", "Acute Appendicitis", "BP:120/80 HR:72", "Ceftriaxone 1g IV");
    }

    private void stubValidStaffToken() {
        when(jwtService.verifyStaffToken(TOKEN)).thenReturn(true);
        when(credentialService.verifyVC(TOKEN)).thenReturn(false); // staff JWT, not a VC
        when(credentialService.extractClaims(TOKEN))
                .thenReturn(Map.of("sub", CALLER_DID, "role", "surgeon"));
        doNothing().when(blockchainAuditService)
                   .recordAccessAttempt(anyString(), anyString(), anyString(), anyBoolean());
    }

    // ─── PUT /api/emr/{patientDid} — updateEMR ───────────────────────────────

    @Test
    void updateEMR_invalidCredential_returns401() throws Exception {
        /*
         * Both verifyVC AND verifyStaffToken return false.
         * The controller returns 401 immediately.
         *
         * This simulates an expired token, a tampered token, or
         * a token signed with the wrong secret key.
         */
        when(credentialService.verifyVC(TOKEN)).thenReturn(false);
        when(jwtService.verifyStaffToken(TOKEN)).thenReturn(false);

        mockMvc.perform(put("/api/emr/" + PATIENT_DID)
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(buildRequest())))
               .andExpect(status().isUnauthorized());

        // OPA and consent must never be checked on an invalid identity
        verify(opaService,          never()).checkAccess(any(), any(), any());
        verify(consentRepository,   never()).findByPatientDidAndDelegateDidAndActiveTrue(any(), any());
    }

    @Test
    void updateEMR_validStaff_opaGranted_savesRecordAndReturns200() throws Exception {
        /*
         * Emergency override path:
         * - Valid staff JWT ✓
         * - OPA grants access (active emergency session + correct stage) ✓
         * - Consent is NOT needed (OPA override is sufficient)
         *
         * The controller must save the record and audit the success.
         */
        stubValidStaffToken();
        when(opaService.checkAccess(anyString(), eq("ESID-001"), eq("update"))).thenReturn(true);
        when(medicalRecordRepository.findByPatientDid(PATIENT_DID)).thenReturn(Optional.empty());
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/api/emr/" + PATIENT_DID)
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(buildRequest())))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("EMR updated successfully")));

        verify(medicalRecordRepository).save(any(MedicalRecord.class));
    }

    @Test
    void updateEMR_opasDenied_butConsentExists_stillSaves200() throws Exception {
        /*
         * The OR security gate — the most important test in this class.
         *
         * Scenario: The emergency session has ENDED (OPA denies override),
         * but the patient previously gave this surgeon explicit consent.
         * The controller MUST still grant write access because OR is sufficient.
         *
         * OPA: DENIED
         * Consent: PRESENT
         * Result: → 200 OK (consent is the "second key" in the OR lock)
         */
        stubValidStaffToken();
        when(opaService.checkAccess(anyString(), anyString(), anyString())).thenReturn(false);
        // Simulate an existing active consent record
        when(consentRepository.findByPatientDidAndDelegateDidAndActiveTrue(PATIENT_DID, CALLER_DID))
                .thenReturn(Optional.of(new com.example.VeristasId.Model.Consent()));
        when(medicalRecordRepository.findByPatientDid(PATIENT_DID)).thenReturn(Optional.empty());
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/api/emr/" + PATIENT_DID)
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(buildRequest())))
               .andExpect(status().isOk());
    }

    @Test
    void updateEMR_noOpaAndNoConsent_returns403AndAudits() throws Exception {
        /*
         * Both gates are closed: OPA denies AND no consent exists.
         * The controller returns 403 and MUST audit the failed attempt.
         *
         * This is the critical dual-failure path — a staff member trying
         * to access records after their emergency session has ended AND
         * without patient consent. This is a potential HIPAA violation.
         */
        stubValidStaffToken();
        when(opaService.checkAccess(anyString(), anyString(), anyString())).thenReturn(false);
        when(consentRepository.findByPatientDidAndDelegateDidAndActiveTrue(any(), any()))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/emr/" + PATIENT_DID)
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(buildRequest())))
               .andExpect(status().isForbidden());

        // The denied attempt MUST be in the audit trail
        verify(blockchainAuditService)
                .recordAccessAttempt(eq(CALLER_DID), eq(PATIENT_DID), eq("EMR_UPDATE_DENIED"), eq(false));
        // The DB must NOT be written to
        verify(medicalRecordRepository, never()).save(any());
    }

    // ─── GET /api/emr/{patientDid} — getEMR ──────────────────────────────────

    @Test
    void getEMR_selfAccess_returnsOwnRecord() throws Exception {
        /*
         * A patient reading their own record — the "isSelf" path.
         *
         * callerDid == patientDid → no consent needed.
         * The real MedicalRecord is returned as JSON.
         */
        when(credentialService.verifyVC(TOKEN)).thenReturn(true);
        when(jwtService.verifyStaffToken(TOKEN)).thenReturn(false);
        // extractClaims returns the patient's own DID as "sub"
        when(credentialService.extractClaims(TOKEN))
                .thenReturn(Map.of("sub", PATIENT_DID)); // sub == patientDid → isSelf = true

        MedicalRecord record = new MedicalRecord();
        record.setPatientDid(PATIENT_DID);
        record.setDiagnosis("Type 2 Diabetes");
        when(medicalRecordRepository.findByPatientDid(PATIENT_DID))
                .thenReturn(Optional.of(record));

        mockMvc.perform(get("/api/emr/" + PATIENT_DID)
                .header("Authorization", TOKEN))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.diagnosis", is("Type 2 Diabetes")));
    }

    @Test
    void getEMR_newPatient_returnsBlankRecord_not404() throws Exception {
        /*
         * Edge case: no EMR found in the database for this DID.
         *
         * Instead of returning a 404 (which would crash the frontend UI),
         * the controller returns a 200 with a blank MedicalRecord containing
         * "No records found (New Patient)" as the diagnosis.
         *
         * This is the orElseGet() path in Optional usage:
         *   .orElseGet(() -> ResponseEntity.ok(blankRecord))
         *
         * From a REST design perspective this is debatable, but for a
         * demo/portfolio system it keeps the frontend clean.
         */
        when(credentialService.verifyVC(TOKEN)).thenReturn(true);
        when(jwtService.verifyStaffToken(TOKEN)).thenReturn(false);
        when(credentialService.extractClaims(TOKEN))
                .thenReturn(Map.of("sub", PATIENT_DID));
        when(medicalRecordRepository.findByPatientDid(PATIENT_DID))
                .thenReturn(Optional.empty()); // no record in DB

        mockMvc.perform(get("/api/emr/" + PATIENT_DID)
                .header("Authorization", TOKEN))
               .andExpect(status().isOk()) // 200, NOT 404
               .andExpect(jsonPath("$.diagnosis", containsString("No records found")));
    }
}
