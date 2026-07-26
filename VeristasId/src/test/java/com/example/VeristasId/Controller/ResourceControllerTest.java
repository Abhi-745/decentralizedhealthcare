package com.example.VeristasId.Controller;

import com.example.VeristasId.Dto.StatusResponse;
import com.example.VeristasId.Service.AuditService;
import com.example.VeristasId.Service.CredentialService;
import com.example.VeristasId.Service.OpaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 16 (Part 1) — ResourceControllerTest
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: The Full Zero-Trust Request Pipeline
 * ═══════════════════════════════════════════════════════════
 *
 * ResourceController demonstrates the complete Zero-Trust flow
 * for accessing a protected clinical report:
 *
 *   Step 1. Identity Check   → Is this VC valid and not revoked?
 *   Step 2. Claim Extraction → Who is making this request (their DID)?
 *   Step 3. OPA Decision     → Does their ABAC policy allow access RIGHT NOW?
 *   Step 4. Audit Log        → Record the outcome (whether granted or denied).
 *
 * NOTICE: Step 4 always happens — even if the request is denied.
 * This is the immutable audit trail principle. A denied attempt
 * is just as important to record as a successful access.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT: @InjectMocks with Field Injection
 * ═══════════════════════════════════════════════════════════
 *
 * ResourceController uses @Autowired field injection (not constructor).
 * When you annotate a field with @InjectMocks, Mockito:
 *   1. Creates an instance of ResourceController using the no-arg constructor.
 *   2. Scans the @Mock fields in the test class.
 *   3. Injects those mocks into the matching @Autowired fields by FIELD NAME.
 *
 * This is why the mock field names below MUST match the field names
 * inside ResourceController exactly:
 *   - "opaService"  (matches @Autowired private OpaService opaService)
 *   - "credService" (matches @Autowired private CredentialService credService)
 *   - "auditService"(matches @Autowired private AuditService auditService)
 *
 * If you name the mock "credentialService" (wrong name), @InjectMocks
 * will NOT inject it and the field in the controller will remain null,
 * causing a NullPointerException at runtime.
 */
@ExtendWith(MockitoExtension.class)
class ResourceControllerTest {

    // Field names MUST exactly match the @Autowired field names in ResourceController
    @Mock OpaService       opaService;
    @Mock CredentialService credService;  // ← "credService", not "credentialService"
    @Mock AuditService     auditService;

    @InjectMocks
    ResourceController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private void stubValidToken(String token) {
        when(credService.checkRevocationStatus(token))
                .thenReturn(new StatusResponse("VALID", "Credential is active"));
        when(credService.extractClaims(token))
                .thenReturn(Map.of("sub", "did:veritas:patient-001"));
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Test
    void getReport_validToken_opaGranted_returns200WithConfidentialData() throws Exception {
        /*
         * The happy path: valid VC + OPA says "allow".
         * The response body must contain the ACCESS GRANTED message.
         *
         * Note how checkRevocationStatus returns a StatusResponse object.
         * The controller checks: .getStatus().equals("VALID").
         * Returning any other status string (e.g. "REVOKED") triggers a 401.
         */
        String token = "Bearer valid-vc-token";
        stubValidToken(token);
        when(opaService.checkAccess(anyString(), anyString(), isNull())).thenReturn(true);
        doNothing().when(auditService).logDecision(anyString(), anyString(), eq(true));

        mockMvc.perform(get("/api/resources/secure-report")
                .header("Authorization", token))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("ACCESS GRANTED")));
    }

    @Test
    void getReport_validToken_opaDenied_returns403() throws Exception {
        /*
         * Valid VC, but OPA says "deny" — the patient's clearance level
         * does not meet the resource's required clearance at this time.
         * This is Attribute-Based Access Control (ABAC) in action.
         */
        String token = "Bearer valid-vc-token";
        stubValidToken(token);
        when(opaService.checkAccess(anyString(), anyString(), isNull())).thenReturn(false);
        doNothing().when(auditService).logDecision(anyString(), anyString(), eq(false));

        mockMvc.perform(get("/api/resources/secure-report")
                .header("Authorization", token))
               .andExpect(status().isForbidden())
               .andExpect(content().string(containsString("ACCESS DENIED")));
    }

    @Test
    void getReport_revokedCredential_returns401_beforeOpaCheck() throws Exception {
        /*
         * If the VC has been revoked (e.g. the patient left the country
         * and the hospital revoked their identity), the controller
         * returns 401 immediately — it never even reaches the OPA check.
         *
         * This is the "fail fast" security pattern:
         * reject untrustworthy identities at the door, before wasting
         * computational resources on policy evaluations.
         */
        String token = "Bearer revoked-vc-token";
        when(credService.checkRevocationStatus(token))
                .thenReturn(new StatusResponse("REVOKED", "Credential has been revoked"));

        mockMvc.perform(get("/api/resources/secure-report")
                .header("Authorization", token))
               .andExpect(status().isUnauthorized());

        // OPA must NOT be called if the identity is already revoked
        verify(opaService, never()).checkAccess(any(), any(), any());
    }

    @Test
    void getReport_alwaysLogsDecision_evenWhenDenied() throws Exception {
        /*
         * THE MOST IMPORTANT TEST — proves the immutable audit principle.
         *
         * A hospital's legal compliance (HIPAA, DPDPA) requires that
         * EVERY access attempt is recorded — successful ones and failed ones.
         *
         * If auditService.logDecision() was never called on a denied attempt,
         * a bad actor could probe the system repeatedly without any trace.
         *
         * This test proves logDecision() is called even on the 403 path.
         */
        String token = "Bearer valid-vc-token";
        stubValidToken(token);
        when(opaService.checkAccess(anyString(), anyString(), isNull())).thenReturn(false);
        doNothing().when(auditService).logDecision(anyString(), anyString(), anyBoolean());

        mockMvc.perform(get("/api/resources/secure-report")
                .header("Authorization", token))
               .andExpect(status().isForbidden());

        // Verify the audit was logged with the DENIED outcome
        verify(auditService).logDecision(
                eq("did:veritas:patient-001"),
                eq("Q1_Report"),
                eq(false)
        );
    }
}
