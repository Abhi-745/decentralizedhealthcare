package com.example.VeristasId.Controller;

import com.example.VeristasId.Dto.CredentialRequest;
import com.example.VeristasId.Dto.StatusResponse;
import com.example.VeristasId.Service.CredentialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 13 — CredentialController Tests
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: The VC Issuance Pipeline Endpoints
 * ═══════════════════════════════════════════════════════════
 *
 * CredentialController exposes the four operations that make up
 * the full lifecycle of a Verifiable Credential:
 *
 *   POST /api/vc/issue    → Hospital issues a signed VC to a patient
 *   POST /api/vc/verify   → Anyone checks if a VC is active or revoked
 *   POST /api/vc/revoke   → Hospital marks a VC as revoked in the ledger
 *   GET  /api/vc/inspect  → Decode a VC and see its raw claims
 *
 * ═══════════════════════════════════════════════════════════
 *  THE KEY TESTING CHALLENGE: @Autowired FIELD INJECTION
 * ═══════════════════════════════════════════════════════════
 *
 * Every controller we've tested so far used CONSTRUCTOR injection:
 *   public SomeController(ServiceA a, ServiceB b) { ... }
 *
 * We could call new SomeController(mockA, mockB) directly.
 *
 * CredentialController uses FIELD injection instead:
 *   @Autowired private CredentialService service;
 *
 * With field injection:
 *   - There is no constructor that accepts CredentialService
 *   - new CredentialController() creates an instance with service = null
 *   - We CANNOT pass the mock in through the constructor
 *
 * The solution: ReflectionTestUtils.setField()
 *   CredentialController controller = new CredentialController();
 *   ReflectionTestUtils.setField(controller, "service", mockService);
 *
 * This uses Java reflection to access the private "service" field
 * and inject our mock directly — bypassing Java's access control.
 *
 * ReflectionTestUtils.setField() takes:
 *   1. The target object (our controller instance)
 *   2. The field name as a String — MUST match the exact field name in the class
 *   3. The value to set (our Mockito mock)
 *
 * ═══════════════════════════════════════════════════════════
 *  WHY FIELD INJECTION IS CONSIDERED BAD PRACTICE
 * ═══════════════════════════════════════════════════════════
 *
 * Field injection:
 *   ✗ Hides dependencies (you can't see them without reading the class body)
 *   ✗ Makes testing hard (requires ReflectionTestUtils or @SpringRunner)
 *   ✗ Creates objects in an invalid state (null fields after new())
 *   ✗ Breaks final fields
 *
 * Constructor injection (what we used in Days 10-12):
 *   ✓ Dependencies are explicit and visible
 *   ✓ Objects are always fully initialized after construction
 *   ✓ Easy to test with new MyClass(mockA, mockB)
 *   ✓ Works with final fields → immutability
 *
 * This Day 13 test demonstrates exactly WHY constructor injection is
 * preferred — the extra ReflectionTestUtils boilerplate in setUp()
 * is the direct cost of field injection.
 */
@ExtendWith(MockitoExtension.class)
class CredentialControllerTest {

    @Mock private CredentialService credentialService;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    // Sample data
    private static final String SAMPLE_TOKEN = "eyJhbGciOiJFUzI1NiJ9.SAMPLE_PAYLOAD.SIGNATURE";
    private static final String BEARER_TOKEN  = "Bearer " + SAMPLE_TOKEN;

    @BeforeEach
    void setUp() {
        /*
         * FIELD INJECTION pattern:
         * 1. Create the controller with the default no-arg constructor
         *    (Java provides this automatically when no constructor is defined)
         * 2. Inject the mock into the private @Autowired field via reflection
         * 3. Wrap in standaloneSetup MockMvc as usual
         */
        CredentialController controller = new CredentialController();
        ReflectionTestUtils.setField(controller, "service", credentialService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ─── POST /api/vc/issue ───────────────────────────────────────────────────

    @Test
    void issue_validRequest_returns200WithIssuedToken() throws Exception {
        /*
         * The hospital sends a CredentialRequest (subjectDid + credentialType + claims)
         * and gets back a signed VC token string.
         *
         * The controller is a pure pass-through: it takes the request body,
         * calls service.issueCredential(req), and returns whatever the service returns.
         * The controller adds no logic — the test verifies the plumbing is correct.
         */
        when(credentialService.issueCredential(any(CredentialRequest.class)))
                .thenReturn(SAMPLE_TOKEN);

        CredentialRequest request = new CredentialRequest();
        request.setSubjectDid("did:veritas:patient-001");
        request.setCredentialType("PatientIdentityVC");
        request.setClaims(Map.of("abhaId", "99-9999-9999-9999", "bloodGroup", "O-Negative"));

        mockMvc.perform(post("/api/vc/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(content().string(SAMPLE_TOKEN));

        verify(credentialService).issueCredential(any(CredentialRequest.class));
    }

    // ─── POST /api/vc/verify ──────────────────────────────────────────────────

    @Test
    void verify_activeToken_returnsValidStatus() throws Exception {
        /*
         * @RequestParam String token — the VC token is passed as a URL query parameter:
         *   POST /api/vc/verify?token=eyJ...
         *
         * The service returns a StatusResponse DTO with status="VALID".
         * Jackson serialises it to: {"status":"VALID","message":"..."}
         *
         * jsonPath("$.status") checks the "status" key in the JSON response body.
         */
        when(credentialService.checkRevocationStatus(SAMPLE_TOKEN))
                .thenReturn(new StatusResponse("VALID", "Credential is active and has not been revoked."));

        mockMvc.perform(post("/api/vc/verify")
                .param("token", SAMPLE_TOKEN))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status", is("VALID")));
    }

    @Test
    void verify_revokedToken_returnsRevokedStatus() throws Exception {
        /*
         * A VC that was previously issued but then revoked (e.g., patient
         * reported lost device, hospital revoked on their behalf).
         *
         * The OpaSecurityFilter calls verifyVC() → which internally calls
         * checkRevocationStatus() → "REVOKED" → returns false → 401.
         * This test verifies the status message is correctly returned.
         */
        when(credentialService.checkRevocationStatus(SAMPLE_TOKEN))
                .thenReturn(new StatusResponse("REVOKED", "Credential has been revoked."));

        mockMvc.perform(post("/api/vc/verify")
                .param("token", SAMPLE_TOKEN))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status", is("REVOKED")));
    }

    @Test
    void verify_unknownToken_returnsNotFoundStatus() throws Exception {
        // Token that was never issued — not in the DB ledger
        when(credentialService.checkRevocationStatus("unknownToken"))
                .thenReturn(new StatusResponse("NOT_FOUND", "No credential found with this token."));

        mockMvc.perform(post("/api/vc/verify")
                .param("token", "unknownToken"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status", is("NOT_FOUND")));
    }

    // ─── POST /api/vc/revoke ──────────────────────────────────────────────────

    @Test
    void revoke_validToken_returns200WithConfirmation() throws Exception {
        /*
         * Revocation is a void operation — the service marks the token in the DB.
         * The controller returns "Revoked Successfully" as a plain String body.
         *
         * doNothing().when() — for void methods, use this pattern instead of
         * when(...).thenReturn(). Mock void methods are "no-op" by default
         * (they do nothing), so this is technically optional here — but
         * making it explicit is good documentation.
         */
        doNothing().when(credentialService).revokeCredential(SAMPLE_TOKEN);

        mockMvc.perform(post("/api/vc/revoke")
                .param("token", SAMPLE_TOKEN))
               .andExpect(status().isOk())
               .andExpect(content().string("Revoked Successfully"));

        // Prove revokeCredential was actually called with the correct token
        verify(credentialService).revokeCredential(eq(SAMPLE_TOKEN));
    }

    @Test
    void revoke_callsServiceWithExactToken() throws Exception {
        /*
         * Verifies the controller passes the token through unchanged.
         * If the controller accidentally trimmed, lowercased, or modified
         * the token string before passing it to the service, revocation
         * would silently fail (wrong token → DB record not updated).
         */
        String specificToken = "eyJhbGciOiJFUzI1NiJ9.SPECIFIC_TOKEN.SIG";
        doNothing().when(credentialService).revokeCredential(specificToken);

        mockMvc.perform(post("/api/vc/revoke")
                .param("token", specificToken))
               .andExpect(status().isOk());

        // eq() ensures the EXACT string was passed — not just any string
        verify(credentialService).revokeCredential(eq(specificToken));
    }

    // ─── GET /api/vc/inspect ──────────────────────────────────────────────────

    @Test
    void inspect_validAuthHeader_returns200WithClaims() throws Exception {
        /*
         * GET /api/vc/inspect reads from the Authorization header (not @RequestParam).
         * The VC is decoded and its claims returned as a Map — useful for
         * debugging or displaying patient profile information.
         *
         * The response is a JSON object with arbitrary keys (the VC claims),
         * so we check for a specific known key: "sub" (the patient DID).
         */
        Map<String, Object> fakeClaims = Map.of(
                "sub",        "did:veritas:patient-001",
                "abhaId",     "99-9999-9999-9999",
                "bloodGroup", "O-Negative",
                "iss",        "did:veritas:hospital-mumbai"
        );
        when(credentialService.extractClaims(BEARER_TOKEN)).thenReturn(fakeClaims);

        mockMvc.perform(get("/api/vc/inspect")
                .header("Authorization", BEARER_TOKEN))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.sub", is("did:veritas:patient-001")))
               .andExpect(jsonPath("$.abhaId", is("99-9999-9999-9999")));
    }

    @Test
    void inspect_claimsPassedThroughUnmodified() throws Exception {
        /*
         * The controller must return whatever the service provides — no filtering,
         * no renaming, no field removal. The full claims Map goes directly
         * into the response body via Jackson serialisation.
         *
         * This test verifies the controller doesn't accidentally drop any claims.
         */
        Map<String, Object> claims = Map.of("iss", "did:veritas:hospital", "role", "patient");
        when(credentialService.extractClaims(BEARER_TOKEN)).thenReturn(claims);

        mockMvc.perform(get("/api/vc/inspect")
                .header("Authorization", BEARER_TOKEN))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.iss",  is("did:veritas:hospital")))
               .andExpect(jsonPath("$.role", is("patient")));
    }
}
