package com.example.VeristasId.Controller;

import com.example.VeristasId.Dto.RegistrationRequest;
import com.example.VeristasId.Dto.VerifiableCredential;
import com.example.VeristasId.Service.CredentialIssuanceService;
import com.example.VeristasId.Service.PatientWalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 14 — PatientRegistrationController Tests
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: The Patient Onboarding Pipeline
 * ═══════════════════════════════════════════════════════════
 *
 * PatientRegistrationController handles the first interaction
 * a patient has with the entire system: getting their
 * Verifiable Credential (VC) issued and stored.
 *
 * The flow has two steps that must happen in strict order:
 *   Step 1. CredentialIssuanceService.issueCredential(req)
 *           → Signs a W3C VC with the hospital's private ECDSA key
 *   Step 2. PatientWalletService.storeVerifiableCredential(vc)
 *           → Saves the signed VC into the patient's on-device wallet
 *
 * If Step 1 fails (e.g. duplicate patient), Step 2 must NEVER run.
 * Day 14 tests prove that this contract holds.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 1: ResponseEntity<?> — The Wildcard Response
 * ═══════════════════════════════════════════════════════════
 *
 * Unlike Day 13 endpoints that always return the same type,
 * this controller returns DIFFERENT types depending on the outcome:
 *
 *   Success  → ResponseEntity<VerifiableCredential>  (200 OK)
 *   Conflict → ResponseEntity<Map<String, String>>   (409 Conflict)
 *   Error    → ResponseEntity<Void>                  (500 Error)
 *
 * Java does not allow a single return type to cover all three.
 * The solution is the wildcard: ResponseEntity<?>
 * The '?' means "I don't know the type at compile time — it varies."
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 2: The try-catch-catch Ordering Rule
 * ═══════════════════════════════════════════════════════════
 *
 * The controller has:
 *   try { ... }
 *   catch (IllegalStateException e) { return 409; }   ← SPECIFIC first
 *   catch (Exception e)             { return 500; }   ← GENERIC last
 *
 * Java evaluates catch blocks TOP-TO-BOTTOM. If you put
 * catch(Exception) first, it would catch EVERYTHING — including
 * IllegalStateException — and you would never reach the 409 block.
 *
 * RULE: Always catch SPECIFIC exceptions before GENERIC ones.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 3: InOrder — Testing Call Sequence
 * ═══════════════════════════════════════════════════════════
 *
 * Standard verify() does not care about ORDER.
 * verify(serviceA).doX(); verify(serviceB).doY();
 * This passes even if doY() was called BEFORE doX().
 *
 * InOrder enforces sequence:
 *   InOrder inOrder = inOrder(serviceA, serviceB);
 *   inOrder.verify(serviceA).doX();   // Must happen FIRST
 *   inOrder.verify(serviceB).doY();   // Must happen SECOND
 *
 * Critical for our pipeline: issuance MUST come before wallet store.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 4: thenAnswer — Inspecting Arguments Live
 * ═══════════════════════════════════════════════════════════
 *
 * when(mock.method(any())).thenReturn(value) stubs the return.
 * But sometimes you need to INSPECT what argument was actually
 * passed into the mock during the test.
 *
 * thenAnswer(invocation -> { ... }) gives you a lambda that
 * runs when the mock is called. Inside:
 *   invocation.getArgument(0) → the first argument passed in
 *   invocation.getArgument(1) → the second argument, etc.
 *
 * Used in Day 14 to verify the auto-register demo hardcodes
 * "99-9999-9999-9999" as the ABHA ID.
 */
@ExtendWith(MockitoExtension.class)
class PatientRegistrationControllerTest {

    @Mock private CredentialIssuanceService issuanceService;
    @Mock private PatientWalletService      walletService;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        /*
         * Constructor injection — clean and simple.
         * No ReflectionTestUtils needed (unlike Day 13's field injection).
         * This is exactly why constructor injection is the best practice.
         */
        PatientRegistrationController controller =
                new PatientRegistrationController(issuanceService, walletService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private VerifiableCredential buildFakeVC() {
        VerifiableCredential vc = new VerifiableCredential();
        vc.setIssuer("did:veritas:hospital-mumbai");
        vc.setIssuanceDate("2026-01-01T00:00:00Z");
        vc.setId("urn:uuid:test-vc-day14-001");

        VerifiableCredential.CredentialSubject subject =
                new VerifiableCredential.CredentialSubject(
                        "did:veritas:patient-001",
                        "99-9999-9999-9999");
        vc.setCredentialSubject(subject);

        VerifiableCredential.Proof proof =
                new VerifiableCredential.Proof(
                        "EcdsaSecp256k1Signature2019",
                        "eyJhbGciOiJFUzI1NiJ9.SAMPLE.SIG");
        vc.setProof(proof);
        return vc;
    }

    private RegistrationRequest buildRequest() {
        return new RegistrationRequest("99-9999-9999-9999", "did:veritas:patient-001");
    }

    // ─── POST /api/patients/register — Happy Path ─────────────────────────────

    @Test
    void register_validRequest_returns200WithVCBody() throws Exception {
        /*
         * The golden path: patient doesn't exist, key generation succeeds,
         * VC is issued and stored. Controller returns 200 with the full VC.
         *
         * We check that the issuer field from our fake VC appears in
         * the JSON response — proving the VC object was serialised
         * directly into the response body by Jackson.
         */
        when(issuanceService.issueCredential(any(RegistrationRequest.class)))
                .thenReturn(buildFakeVC());
        doNothing().when(walletService).storeVerifiableCredential(anyString());

        mockMvc.perform(post("/api/patients/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(buildRequest())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.issuer", is("did:veritas:hospital-mumbai")));
    }

    @Test
    void register_storesVCInWalletAfterIssuance() throws Exception {
        /*
         * KEY TEST — proves the two-step pipeline runs in the correct order.
         *
         * InOrder enforces: issuance → wallet store.
         * If the developer accidentally reordered the two lines in the
         * controller, this test would catch the mistake immediately.
         *
         * This is the difference between:
         *   verify(A); verify(B)  — does NOT check order
         *   inOrder.verify(A); inOrder.verify(B)  — DOES check order
         */
        when(issuanceService.issueCredential(any())).thenReturn(buildFakeVC());
        doNothing().when(walletService).storeVerifiableCredential(anyString());

        mockMvc.perform(post("/api/patients/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(buildRequest())))
               .andExpect(status().isOk());

        InOrder inOrder = inOrder(issuanceService, walletService);
        inOrder.verify(issuanceService).issueCredential(any());
        inOrder.verify(walletService).storeVerifiableCredential(anyString());
    }

    // ─── POST /api/patients/register — Error Paths ────────────────────────────

    @Test
    void register_duplicatePatient_returns409Conflict() throws Exception {
        /*
         * IllegalStateException is thrown by CredentialIssuanceService when
         * a patient with the same ABHA ID already has a VC in the database.
         *
         * The controller converts this into a 409 CONFLICT with a human-readable
         * error message: Map.of("error", e.getMessage()).
         *
         * WHY 409 and not 400?
         *   400 Bad Request = the request was malformed / had invalid syntax.
         *   409 Conflict    = the request was valid, but it conflicts with
         *                     existing server state (the patient already exists).
         */
        when(issuanceService.issueCredential(any()))
                .thenThrow(new IllegalStateException(
                        "Patient with ABHA ID 99-9999-9999-9999 is already registered."));

        mockMvc.perform(post("/api/patients/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(buildRequest())))
               .andExpect(status().isConflict())
               .andExpect(jsonPath("$.error", containsString("already registered")));
    }

    @Test
    void register_unknownError_returns500() throws Exception {
        /*
         * Any exception that is NOT an IllegalStateException falls through
         * to the second catch block: catch (Exception e).
         *
         * The controller returns 500 Internal Server Error.
         * Note: it also calls e.printStackTrace() — useful for server logs,
         * but means we don't surface the raw error message to the client
         * (which is correct from a security perspective — never leak
         * internal stack traces to API consumers).
         */
        when(issuanceService.issueCredential(any()))
                .thenThrow(new RuntimeException("BouncyCastle key generation failed"));

        mockMvc.perform(post("/api/patients/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(buildRequest())))
               .andExpect(status().isInternalServerError());
    }

    @Test
    void register_onIssuanceFailure_walletStoreIsNeverCalled() throws Exception {
        /*
         * NEGATIVE TEST — this is just as important as the happy path.
         *
         * If issuance fails (duplicate patient), the VC never exists.
         * Calling walletService.storeVerifiableCredential() with a null or
         * fake VC would corrupt the patient wallet. We must prove this cannot happen.
         *
         * verify(mock, never()).method() — asserts the method was NEVER invoked
         * during the test. This is the Mockito equivalent of asserting a side
         * effect did NOT occur.
         */
        when(issuanceService.issueCredential(any()))
                .thenThrow(new IllegalStateException("Already registered"));

        mockMvc.perform(post("/api/patients/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(buildRequest())))
               .andExpect(status().isConflict());

        verify(walletService, never()).storeVerifiableCredential(anyString());
    }

    // ─── GET /api/patients/auto-register-demo ─────────────────────────────────

    @Test
    void autoRegisterDemo_readsPatientDidFromWallet() throws Exception {
        /*
         * /auto-register-demo is a helper endpoint for dev testing.
         * Instead of requiring a request body, it auto-fetches the
         * patient's DID from the wallet and uses a hardcoded ABHA ID.
         *
         * This test proves the controller calls getPatientDid()
         * to construct the RegistrationRequest — not a hardcoded DID.
         */
        when(walletService.getPatientDid()).thenReturn("did:veritas:demo-patient-xyz");
        when(issuanceService.issueCredential(any())).thenReturn(buildFakeVC());
        doNothing().when(walletService).storeVerifiableCredential(anyString());

        mockMvc.perform(get("/api/patients/auto-register-demo"))
               .andExpect(status().isOk());

        verify(walletService).getPatientDid();
    }

    @Test
    void autoRegisterDemo_usesHardcodedDemoAbhaId() throws Exception {
        /*
         * thenAnswer() — the most powerful stubbing technique in Mockito.
         *
         * Instead of just returning a value, thenAnswer() gives us a
         * lambda that executes when the mock method is called. Inside
         * the lambda, we can inspect the ACTUAL ARGUMENTS passed in.
         *
         * invocation.getArgument(0) → the RegistrationRequest object
         *
         * We use this to assert that the ABHA ID hardcoded in the
         * controller source code ("99-9999-9999-9999") is actually
         * what gets passed into the issuance service.
         *
         * This is better than ArgumentCaptor for simple cases — it
         * lets us assert inline, inside the stub itself.
         */
        when(walletService.getPatientDid()).thenReturn("did:veritas:demo-patient");
        doNothing().when(walletService).storeVerifiableCredential(anyString());

        when(issuanceService.issueCredential(any(RegistrationRequest.class)))
                .thenAnswer(invocation -> {
                    RegistrationRequest captured = invocation.getArgument(0);
                    assertEquals("99-9999-9999-9999", captured.getAbhaId(),
                            "auto-register-demo must use the hardcoded ABHA ID");
                    return buildFakeVC();
                });

        mockMvc.perform(get("/api/patients/auto-register-demo"))
               .andExpect(status().isOk());
    }

    @Test
    void autoRegisterDemo_propagatesDuplicateConflict_returns409() throws Exception {
        /*
         * /auto-register-demo internally calls registerPatient().
         * This means ALL the same error-handling paths apply.
         *
         * If the demo patient is already registered, the 409 response
         * should propagate back through the auto-register endpoint.
         */
        when(walletService.getPatientDid()).thenReturn("did:veritas:demo-patient");
        when(issuanceService.issueCredential(any()))
                .thenThrow(new IllegalStateException("Demo patient already registered."));

        mockMvc.perform(get("/api/patients/auto-register-demo"))
               .andExpect(status().isConflict())
               .andExpect(jsonPath("$.error", containsString("already registered")));
    }
}
