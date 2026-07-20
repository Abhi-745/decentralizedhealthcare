package com.example.VeristasId.Controller;

import com.example.VeristasId.Model.Consent;
import com.example.VeristasId.Repository.ConsentRepository;
import com.example.VeristasId.Service.BlockchainAuditService;
import com.example.VeristasId.Service.CredentialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 12 — ConsentController Tests
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: Patient-Controlled Consent
 * ═══════════════════════════════════════════════════════════
 *
 * In a self-sovereign identity system, the patient owns their data.
 * The ConsentController implements the mechanisms for patients to:
 *
 *   POST /api/consent/grant   → give a specific doctor access to their EMR
 *   POST /api/consent/revoke  → take that access back at any time
 *   GET  /api/consent/mine    → see all active and revoked consents
 *
 * The controller is unique because:
 *
 *   1. Identity comes FROM the VC itself (not from a path variable or body)
 *      - extractClaims(token).get("sub") is the patient's DID
 *      - A patient cannot grant consent on behalf of someone else
 *
 *   2. Every consent is SHA-256 signed
 *      - sha256(patientDid + delegateDid + purpose) is stored as a signature
 *      - This cryptographically binds the consent to its exact terms
 *      - Changing "purpose" after granting is detectable
 *
 *   3. Every grant/revoke is audited on the blockchain
 *      - "CONSENT_GRANTED" and "CONSENT_REVOKED" events join the audit chain
 *
 *   4. Duplicate guard: ONE active consent per (patient, delegate) pair
 *      - Attempting to grant twice returns 400 (not 200 or 409)
 *
 * ═══════════════════════════════════════════════════════════
 *  TEST STRATEGY
 * ═══════════════════════════════════════════════════════════
 *
 * 10 tests across 3 endpoints:
 *   - Invalid VC → 401 for every endpoint
 *   - Duplicate consent → 400
 *   - Revoke non-existent → 400
 *   - Happy paths → 200 + save() called + audit recorded
 *   - active=false set correctly on revoke
 */
@ExtendWith(MockitoExtension.class)
class ConsentControllerTest {

    @Mock private ConsentRepository consentRepository;
    @Mock private CredentialService credentialService;
    @Mock private BlockchainAuditService blockchainAuditService;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    // Reusable request body
    private static final String VALID_TOKEN    = "Bearer patientVC";
    private static final String PATIENT_DID    = "did:veritas:patient-001";
    private static final String DELEGATE_DID   = "did:veritas:doctor-99";
    private static final String PURPOSE        = "EMR_READ";

    @BeforeEach
    void setUp() {
        ConsentController controller =
                new ConsentController(consentRepository, credentialService, blockchainAuditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /** Builds the JSON body: {"delegateDid": "...", "purpose": "..."} */
    private String body() throws Exception {
        return mapper.writeValueAsString(Map.of("delegateDid", DELEGATE_DID, "purpose", PURPOSE));
    }

    // ─── GRANT CONSENT ────────────────────────────────────────────────────────

    @Test
    void grantConsent_validVC_newDelegate_returns200() throws Exception {
        /*
         * Full happy path:
         *  1. VC is valid → credentialService.verifyVC() returns true
         *  2. Patient DID extracted from VC claims ("sub" field)
         *  3. No existing consent found → duplicate check passes
         *  4. Consent saved with SHA-256 signature
         *  5. Audit recorded as CONSENT_GRANTED
         *  6. 200 OK returned
         */
        when(credentialService.verifyVC(VALID_TOKEN)).thenReturn(true);
        when(credentialService.extractClaims(VALID_TOKEN))
                .thenReturn(Map.of("sub", PATIENT_DID));
        when(consentRepository.findByPatientDidAndDelegateDidAndActiveTrue(PATIENT_DID, DELEGATE_DID))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/consent/grant")
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
               .andExpect(status().isOk())
               .andExpect(content().string(org.hamcrest.Matchers.containsString(DELEGATE_DID)));

        verify(consentRepository).save(any(Consent.class));
        verify(blockchainAuditService)
                .recordAccessAttempt(eq(PATIENT_DID), eq(DELEGATE_DID), eq("CONSENT_GRANTED"), eq(true));
    }

    @Test
    void grantConsent_invalidVC_returns401() throws Exception {
        /*
         * Revoked or forged VC → gate closes immediately.
         * Nothing else is called: no claims extraction, no DB, no audit.
         */
        when(credentialService.verifyVC(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/consent/grant")
                .header("Authorization", "Bearer revokedVC")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
               .andExpect(status().isUnauthorized());

        verify(consentRepository, never()).save(any());
        verify(blockchainAuditService, never()).recordAccessAttempt(any(), any(), any(), anyBoolean());
    }

    @Test
    void grantConsent_duplicateConsent_returns400() throws Exception {
        /*
         * Patient tries to grant the same doctor access twice.
         * The controller returns 400 Bad Request — not 409 Conflict.
         *
         * WHY 400 not 409?
         * 409 means "server-state conflict". 400 means "your request doesn't
         * make sense given current state". A duplicate consent is a logical
         * error by the client — they already have active access, so this
         * request is redundant. 400 is the correct choice here.
         *
         * Critical: save() must NOT be called — we don't want two active
         * consent records for the same (patient, delegate) pair.
         */
        when(credentialService.verifyVC(VALID_TOKEN)).thenReturn(true);
        when(credentialService.extractClaims(VALID_TOKEN))
                .thenReturn(Map.of("sub", PATIENT_DID));
        when(consentRepository.findByPatientDidAndDelegateDidAndActiveTrue(PATIENT_DID, DELEGATE_DID))
                .thenReturn(Optional.of(new Consent()));

        mockMvc.perform(post("/api/consent/grant")
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
               .andExpect(status().isBadRequest());

        verify(consentRepository, never()).save(any());
    }

    @Test
    void grantConsent_patientDidComesFromVC_notFromBody() throws Exception {
        /*
         * Security test: the patient's identity comes from the cryptographically
         * signed VC claims (the "sub" field), NOT from the request body.
         *
         * If identity came from the body, a malicious user could grant consent
         * on behalf of any patient by putting any DID in the body.
         *
         * We verify that the DID saved to the consent is the one from the VC,
         * not anything from the body.
         */
        when(credentialService.verifyVC(VALID_TOKEN)).thenReturn(true);
        when(credentialService.extractClaims(VALID_TOKEN))
                .thenReturn(Map.of("sub", PATIENT_DID));
        when(consentRepository.findByPatientDidAndDelegateDidAndActiveTrue(PATIENT_DID, DELEGATE_DID))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/consent/grant")
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
               .andExpect(status().isOk());

        // Verify the saved consent uses the DID from the VC, not any arbitrary string
        verify(consentRepository).save(
                argThat(c -> PATIENT_DID.equals(c.getPatientDid())));
    }

    // ─── REVOKE CONSENT ───────────────────────────────────────────────────────

    @Test
    void revokeConsent_existingConsent_returns200AndSetsActiveFalse() throws Exception {
        /*
         * The revoke flow:
         *  1. VC valid → extract patient DID
         *  2. Find active consent for (patientDid, delegateDid) → found
         *  3. consent.setActive(false) → save()
         *  4. Audit CONSENT_REVOKED
         *  5. Return 200
         *
         * argThat(c -> !c.isActive()) proves the entity was actually
         * marked inactive before being saved — not just saved as-is.
         */
        Consent existing = new Consent();
        existing.setPatientDid(PATIENT_DID);
        existing.setDelegateDid(DELEGATE_DID);
        existing.setActive(true);

        when(credentialService.verifyVC(VALID_TOKEN)).thenReturn(true);
        when(credentialService.extractClaims(VALID_TOKEN))
                .thenReturn(Map.of("sub", PATIENT_DID));
        when(consentRepository.findByPatientDidAndDelegateDidAndActiveTrue(PATIENT_DID, DELEGATE_DID))
                .thenReturn(Optional.of(existing));

        mockMvc.perform(post("/api/consent/revoke")
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
               .andExpect(status().isOk())
               .andExpect(content().string(org.hamcrest.Matchers.containsString("revoked")));

        // Proves active was set to false BEFORE saving
        verify(consentRepository).save(argThat(c -> !c.isActive()));
        verify(blockchainAuditService)
                .recordAccessAttempt(eq(PATIENT_DID), eq(DELEGATE_DID), eq("CONSENT_REVOKED"), eq(true));
    }

    @Test
    void revokeConsent_invalidVC_returns401() throws Exception {
        when(credentialService.verifyVC(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/consent/revoke")
                .header("Authorization", "Bearer badToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
               .andExpect(status().isUnauthorized());

        verify(consentRepository, never()).save(any());
    }

    @Test
    void revokeConsent_noActiveConsent_returns400() throws Exception {
        /*
         * Patient tries to revoke a consent that doesn't exist
         * (either never granted, or already revoked).
         * 400 Bad Request — the request is logically inconsistent.
         */
        when(credentialService.verifyVC(VALID_TOKEN)).thenReturn(true);
        when(credentialService.extractClaims(VALID_TOKEN))
                .thenReturn(Map.of("sub", PATIENT_DID));
        when(consentRepository.findByPatientDidAndDelegateDidAndActiveTrue(PATIENT_DID, DELEGATE_DID))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/consent/revoke")
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
               .andExpect(status().isBadRequest());

        verify(consentRepository, never()).save(any());
    }

    // ─── MY CONSENTS ──────────────────────────────────────────────────────────

    @Test
    void myConsents_validVC_returns200WithList() throws Exception {
        /*
         * The patient requests all their consents (active + revoked).
         * findAllByPatientDid returns the full history — not just active ones.
         *
         * This gives the patient a complete audit of who has (or had)
         * access to their medical data.
         */
        Consent c1 = new Consent();
        c1.setPatientDid(PATIENT_DID);
        c1.setDelegateDid(DELEGATE_DID);
        c1.setActive(true);

        when(credentialService.verifyVC(VALID_TOKEN)).thenReturn(true);
        when(credentialService.extractClaims(VALID_TOKEN))
                .thenReturn(Map.of("sub", PATIENT_DID));
        when(consentRepository.findAllByPatientDid(PATIENT_DID))
                .thenReturn(List.of(c1));

        mockMvc.perform(get("/api/consent/mine")
                .header("Authorization", VALID_TOKEN))
               .andExpect(status().isOk());

        verify(consentRepository).findAllByPatientDid(PATIENT_DID);
    }

    @Test
    void myConsents_invalidVC_returns401() throws Exception {
        when(credentialService.verifyVC(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/consent/mine")
                .header("Authorization", "Bearer expiredToken"))
               .andExpect(status().isUnauthorized());

        verify(consentRepository, never()).findAllByPatientDid(any());
    }
}
