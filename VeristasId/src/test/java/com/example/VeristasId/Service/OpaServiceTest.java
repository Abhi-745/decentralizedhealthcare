package com.example.VeristasId.Service;

import com.example.VeristasId.Dto.OpaResponse;
import com.example.VeristasId.Model.EmergencySessionEntity;
import com.example.VeristasId.Repository.EmergencySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Day 8 — OpaService Tests (Attribute-Based Access Control)
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: What is OPA and why does this project use it?
 * ═══════════════════════════════════════════════════════════
 *
 * Traditional authorization looks like this (inside Java code):
 *
 *   if (role.equals("paramedic") && action.equals("read")) {
 *       return true;
 *   }
 *
 * This works, but every policy change requires:
 *   1. Editing Java code
 *   2. Recompiling the project
 *   3. Redeploying the server
 *   4. A full production rollout
 *
 * OPA (Open Policy Agent) separates the POLICY from the CODE:
 *
 *   Java code:   "Should this token access this record?"  → OPA
 *   OPA engine:  reads the Rego policy file               → true/false
 *   Rego file:   "A paramedic can READ during 'dispatched' stage"
 *
 * Now a hospital's policy team can update the Rego file and
 * reload OPA — NO Java recompilation, NO redeployment.
 *
 * ═══════════════════════════════════════════════════════════
 *  WHAT IS ABAC? (Attribute-Based Access Control)
 * ═══════════════════════════════════════════════════════════
 *
 * RBAC (Role-Based):  "A paramedic can access everything."
 * ABAC (Attribute):   "A paramedic can READ a patient record ONLY
 *                      when the emergency session is in 'dispatched' stage."
 *
 * ABAC combines:
 *   - User attributes:    role from the JWT
 *   - Resource attributes: the patient being accessed
 *   - Environment:        the current emergency session stage
 *
 * ═══════════════════════════════════════════════════════════
 *  THE REGO POLICY (policies/emergency.rego)
 * ═══════════════════════════════════════════════════════════
 *
 *   default allow = false        ← deny everything unless a rule matches
 *
 *   allow if {                   ← Rule 1: Paramedic read during dispatch
 *     token_payload.role == "paramedic"
 *     input.action == "read"
 *     input.session.stage == "dispatched"
 *   }
 *
 *   allow if {                   ← Rule 2: Surgeon update when arrived
 *     token_payload.role == "surgeon"
 *     input.action == "update"
 *     input.session.stage == "arrived"
 *   }
 *
 * ═══════════════════════════════════════════════════════════
 *  HOW OpaService.checkAccess() WORKS
 * ═══════════════════════════════════════════════════════════
 *
 *  1. Load the EmergencySession from PostgreSQL (by esid or patientId)
 *  2. Build an input JSON: { token, action, session: { stage } }
 *  3. POST to OPA server (localhost:8181/v1/data/veritas/emergency/allow)
 *  4. OPA evaluates the Rego rules against the input
 *  5. Returns { "result": true } or { "result": false }
 *  6. On connection failure → return false (fail-secure)
 *
 * ═══════════════════════════════════════════════════════════
 *  HOW WE TEST THIS (Without a real OPA server)
 * ═══════════════════════════════════════════════════════════
 *
 * We mock TWO dependencies:
 *   - RestTemplate   → fake the HTTP call to OPA
 *   - EmergencySessionRepository → fake the DB lookup
 *
 * This means our tests verify that OpaService:
 *   - Builds the correct input payload
 *   - Handles OPA's responses (true/false) correctly
 *   - Handles connection failures gracefully
 *   - Normalizes token/stage casing correctly before sending
 */

@ExtendWith(MockitoExtension.class)
class OpaServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private EmergencySessionRepository sessionRepo;

    @InjectMocks
    private OpaService opaService;

    // Helper: create a mock OpaResponse
    private OpaResponse allow()  { OpaResponse r = new OpaResponse(); r.setResult(true);  return r; }
    private OpaResponse deny()   { OpaResponse r = new OpaResponse(); r.setResult(false); return r; }

    // A realistic dispatched session
    private EmergencySessionEntity dispatchedSession() {
        return new EmergencySessionEntity("ESID-001", "PAT-999", "dispatched", 1720000000000L);
    }

    // An arrived session
    private EmergencySessionEntity arrivedSession() {
        return new EmergencySessionEntity("ESID-002", "PAT-999", "arrived", 1720000001000L);
    }

    @BeforeEach
    void setUp() {
        // Inject the OPA endpoint URL via reflection (replaces @Value injection by Spring)
        ReflectionTestUtils.setField(opaService, "opaEndpoint",
            "http://localhost:8181/v1/data/veritas/emergency/allow");
    }

    // ─── 1. HAPPY PATH — ACCESS GRANTED ─────────────────────────────────────

    @Test
    void checkAccess_paramedicRead_dispatchedSession_returnsTrue() {
        // SCENARIO: A paramedic arrives at the ER. Their JWT says role=paramedic.
        // The emergency session is in "dispatched" stage. They want to READ the patient record.
        // OPA Rule 1 applies → GRANT access.

        String paramedicToken = "Bearer eyJhbGciOiJIUzI1NiJ9.paramedic.sig";
        when(sessionRepo.findById("ESID-001")).thenReturn(Optional.of(dispatchedSession()));
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class))).thenReturn(allow());

        boolean result = opaService.checkAccess(paramedicToken, "ESID-001", "read");

        assertTrue(result, "Paramedic READ in dispatched stage must be ALLOWED by OPA Rule 1");
    }

    @Test
    void checkAccess_surgeonUpdate_arrivedSession_returnsTrue() {
        // SCENARIO: Surgeon arrives at the ER. Session stage is "arrived".
        // They want to UPDATE the EMR. OPA Rule 2 applies → GRANT.

        String surgeonToken = "Bearer eyJhbGciOiJIUzI1NiJ9.surgeon.sig";
        when(sessionRepo.findById("ESID-002")).thenReturn(Optional.of(arrivedSession()));
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class))).thenReturn(allow());

        boolean result = opaService.checkAccess(surgeonToken, "ESID-002", "update");

        assertTrue(result, "Surgeon UPDATE in arrived stage must be ALLOWED by OPA Rule 2");
    }

    // ─── 2. HAPPY PATH — ACCESS DENIED ──────────────────────────────────────

    @Test
    void checkAccess_paramedicUpdate_returnsOpaFalse() {
        // SCENARIO: A paramedic tries to UPDATE (not read) a record.
        // OPA Rule 1 only allows "read". Rule 2 requires "surgeon".
        // No rule matches → DENY.

        when(sessionRepo.findById("ESID-001")).thenReturn(Optional.of(dispatchedSession()));
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class))).thenReturn(deny());

        boolean result = opaService.checkAccess("Bearer token", "ESID-001", "update");

        assertFalse(result, "Paramedic UPDATE must be DENIED — no matching Rego rule");
    }

    @Test
    void checkAccess_surgeonRead_arrivedSession_returnsOpaFalse() {
        // SCENARIO: A surgeon tries to READ (not update) during arrived stage.
        // OPA Rule 2 only grants surgeons "update". No rule for surgeon+read.
        // → DENY.

        when(sessionRepo.findById("ESID-002")).thenReturn(Optional.of(arrivedSession()));
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class))).thenReturn(deny());

        boolean result = opaService.checkAccess("Bearer token", "ESID-002", "read");

        assertFalse(result, "Surgeon READ must be DENIED — Rego only grants surgeon UPDATE");
    }

    @Test
    void checkAccess_paramedicRead_wrongStage_returnsOpaFalse() {
        // SCENARIO: Paramedic tries to read when session stage is "arrived" (not dispatched).
        // Rule 1 requires "dispatched". → DENY.

        when(sessionRepo.findById("ESID-002")).thenReturn(Optional.of(arrivedSession()));
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class))).thenReturn(deny());

        boolean result = opaService.checkAccess("Bearer token", "ESID-002", "read");

        assertFalse(result, "Paramedic READ in 'arrived' stage must be DENIED — Rule 1 needs 'dispatched'");
    }

    // ─── 3. FAIL-SECURE BEHAVIOUR ────────────────────────────────────────────

    @Test
    void checkAccess_opaServerDown_returnsFalse() {
        // SCENARIO: The OPA server is unreachable (crashed, network issue).
        // CRITICAL: The system must DENY access — never accidentally GRANT it.
        // This is the "fail-secure" / "fail-closed" principle.

        when(sessionRepo.findById(anyString())).thenReturn(Optional.of(dispatchedSession()));
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class)))
            .thenThrow(new RestClientException("Connection refused: OPA server down"));

        boolean result = opaService.checkAccess("Bearer token", "ESID-001", "read");

        assertFalse(result,
            "CRITICAL: When OPA is unreachable, access must be DENIED (fail-secure). " +
            "Granting access when the policy engine is down would be a security hole.");
    }

    @Test
    void checkAccess_opaReturnsNull_returnsFalse() {
        // SCENARIO: OPA server responds with an empty body (null).
        // The service must not crash with NullPointerException.
        // Must treat null response as "deny".

        when(sessionRepo.findById(anyString())).thenReturn(Optional.of(dispatchedSession()));
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class))).thenReturn(null);

        boolean result = opaService.checkAccess("Bearer token", "ESID-001", "read");

        assertFalse(result,
            "Null OPA response must be treated as DENY to prevent accidental access grants");
    }

    // ─── 4. SESSION LOOKUP FALLBACK TESTS ───────────────────────────────────

    @Test
    void checkAccess_lookupByPatientId_whenEsidNotFound() {
        // SCENARIO: The caller passes a patientId (not an esid).
        // OpaService first tries findById(target), which returns empty.
        // Then falls back to findFirstByPatientId(target).

        when(sessionRepo.findById("PAT-999")).thenReturn(Optional.empty()); // esid lookup fails
        when(sessionRepo.findFirstByPatientId("PAT-999")).thenReturn(Optional.of(dispatchedSession()));
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class))).thenReturn(allow());

        boolean result = opaService.checkAccess("Bearer token", "PAT-999", "read");

        assertTrue(result, "OpaService must fall back to patientId lookup when esid lookup returns empty");
        // Verify both lookup paths were attempted
        verify(sessionRepo).findById("PAT-999");
        verify(sessionRepo).findFirstByPatientId("PAT-999");
    }

    @Test
    void checkAccess_noSessionFound_sendsSafeDefaults() {
        // SCENARIO: No session found by either esid OR patientId.
        // OpaService builds the input with stage="" and createdAt=0L.
        // OPA will evaluate with empty stage → no rule matches → deny.

        when(sessionRepo.findById("UNKNOWN")).thenReturn(Optional.empty());
        when(sessionRepo.findFirstByPatientId("UNKNOWN")).thenReturn(Optional.empty());
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class))).thenReturn(deny());

        boolean result = opaService.checkAccess("Bearer token", "UNKNOWN", "read");

        assertFalse(result, "When no session exists, OPA must deny (no stage matches any Rego rule)");
        // Verify that both repository methods were called (not just findById)
        verify(sessionRepo).findById("UNKNOWN");
        verify(sessionRepo).findFirstByPatientId("UNKNOWN");
    }

    // ─── 5. TOKEN NORMALISATION TESTS ────────────────────────────────────────

    @Test
    void checkAccess_tokenWithoutBearerPrefix_addsBearerPrefix() {
        // SCENARIO: The caller passes a raw JWT without "Bearer " prefix.
        // The Rego policy splits on " " to extract the token:
        //   [_, jwt] := split(input.token, " ")
        // If "Bearer " is missing, the split returns only 1 part → Rego crashes.
        // OpaService normalises it by adding "Bearer " if missing.

        when(sessionRepo.findById("ESID-001")).thenReturn(Optional.of(dispatchedSession()));
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class))).thenReturn(allow());

        // Pass raw token WITHOUT "Bearer "
        boolean result = opaService.checkAccess("rawJwtWithoutBearer", "ESID-001", "read");

        assertTrue(result, "OpaService must add 'Bearer ' prefix to raw tokens before sending to OPA");

        // Verify OPA was called (not short-circuited before calling RestTemplate)
        verify(restTemplate).postForObject(anyString(), any(), eq(OpaResponse.class));
    }

    @Test
    void checkAccess_tokenAlreadyHasBearerPrefix_doesNotDoublePrefix() {
        // SCENARIO: Token already has "Bearer " prefix — don't add it again.
        // "Bearer Bearer eyJ..." would break OPA's split logic.

        when(sessionRepo.findById("ESID-001")).thenReturn(Optional.of(dispatchedSession()));
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class))).thenReturn(allow());

        opaService.checkAccess("Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig", "ESID-001", "read");

        // Capture what was sent to OPA and verify it's not double-prefixed
        verify(restTemplate).postForObject(
            anyString(),
            argThat(body -> {
                // The body is Map<String, Object> { "input": { "token": "Bearer eyJ..." } }
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> req = (java.util.Map<String, Object>) body;
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> input = (java.util.Map<String, Object>) req.get("input");
                String token = (String) input.get("token");
                // Must have exactly ONE "Bearer " prefix
                return token.startsWith("Bearer ") && !token.startsWith("Bearer Bearer ");
            }),
            eq(OpaResponse.class)
        );
    }

    // ─── 6. STAGE NORMALISATION TEST ────────────────────────────────────────

    @Test
    void checkAccess_uppercaseStageInDb_sendsLowercaseToOpa() {
        // SCENARIO: PostgreSQL stores stage as "DISPATCHED" (uppercase Enum name).
        // The Rego policy compares: input.session.stage == "dispatched" (lowercase).
        // If OpaService doesn't lowercase it, no rule ever matches → always deny.
        // OpaService must call .toLowerCase() before building the OPA input.

        EmergencySessionEntity uppercaseStage =
            new EmergencySessionEntity("ESID-003", "PAT-888", "DISPATCHED", 1720000000L);

        when(sessionRepo.findById("ESID-003")).thenReturn(Optional.of(uppercaseStage));
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class))).thenReturn(allow());

        opaService.checkAccess("Bearer token", "ESID-003", "read");

        verify(restTemplate).postForObject(
            anyString(),
            argThat(body -> {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> req = (java.util.Map<String, Object>) body;
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> input = (java.util.Map<String, Object>) req.get("input");
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> session = (java.util.Map<String, Object>) input.get("session");
                String stage = (String) session.get("stage");
                return "dispatched".equals(stage); // must be lowercase
            }),
            eq(OpaResponse.class)
        );
    }

    // ─── 7. OPA IS CALLED EXACTLY ONCE PER REQUEST ───────────────────────────

    @Test
    void checkAccess_opaCalledExactlyOnce_perCheckAccessInvocation() {
        when(sessionRepo.findById("ESID-001")).thenReturn(Optional.of(dispatchedSession()));
        when(restTemplate.postForObject(anyString(), any(), eq(OpaResponse.class))).thenReturn(allow());

        opaService.checkAccess("Bearer token", "ESID-001", "read");

        // Verify exactly 1 HTTP call to OPA — no caching, no retries, no double calls
        verify(restTemplate, times(1))
            .postForObject(anyString(), any(), eq(OpaResponse.class));
    }
}
