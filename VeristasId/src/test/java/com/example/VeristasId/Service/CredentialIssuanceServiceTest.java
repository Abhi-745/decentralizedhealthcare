package com.example.VeristasId.Service;

import com.example.VeristasId.Dto.CredentialRequest;
import com.example.VeristasId.Dto.RegistrationRequest;
import com.example.VeristasId.Dto.VerifiableCredential;
import com.example.VeristasId.Model.VCEntity;
import com.example.VeristasId.Repository.VCRepository;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Security;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Day 7 — CredentialIssuanceService + CredentialService Tests
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: What is a Verifiable Credential (VC)?
 * ═══════════════════════════════════════════════════════════
 *
 * In the physical world, a hospital gives you a laminated ID card.
 * The card has:
 *   - WHO you are (name, blood group, ABHA ID)
 *   - WHO issued it (hospital stamp + signature)
 *   - A PROOF it hasn't been forged (hologram / signature)
 *
 * A W3C Verifiable Credential is the digital equivalent:
 *
 *   {
 *     "@context": ["https://www.w3.org/2018/credentials/v1"],  ← standard format
 *     "type": ["VerifiableCredential", "PatientAbhaCredential"], ← what kind of credential
 *     "issuer": "did:veristas:hospital:Ab3xYz9k...",             ← who signed it
 *     "issuanceDate": "2026-07-09T10:00:00Z",                   ← when
 *     "credentialSubject": {
 *       "id": "did:veristas:patient:Xy1z...",                    ← who this is about
 *       "abhaId": "99-9999-9999-9999"                            ← their claims
 *     },
 *     "proof": {
 *       "type": "JwtProof2020",                                  ← how it's signed
 *       "jwt": "eyJhbGci..."                                     ← the cryptographic seal
 *     }
 *   }
 *
 * The "proof.jwt" is the critical part. It is an ES256-signed JWT
 * (ECDSA with P-256 curve) that mathematically proves the hospital
 * issued this exact credential. Anyone in the world can verify it
 * using the hospital's PUBLIC key — without contacting the hospital.
 *
 * ═══════════════════════════════════════════════════════════
 *  TWO SERVICES, TWO ROLES
 * ═══════════════════════════════════════════════════════════
 *
 * CredentialIssuanceService → The "issuing desk" — builds the VC document
 * CredentialService         → The "cryptography engine" — signs it, saves to DB
 *
 * CredentialIssuanceService DEPENDS ON CredentialService.
 * Today we test both, using Mockito to isolate them.
 *
 * ═══════════════════════════════════════════════════════════
 *  WHAT IS MOCKITO?
 * ═══════════════════════════════════════════════════════════
 *
 * Mockito lets you create "fake" versions of dependencies.
 * Instead of needing a real PostgreSQL database and a real
 * ECDSA private key, we create mock objects that:
 *   1. Behave exactly as we instruct them to
 *   2. Let us verify HOW the service called them
 *
 * This is called "mocking" — isolating the unit under test.
 */

// ─── PART 1: Testing CredentialIssuanceService ────────────────────────────

@ExtendWith(MockitoExtension.class) // activates Mockito annotations
class CredentialIssuanceServiceTest {

    // @Mock creates a fake CredentialService (no real DB, no real keys)
    @Mock
    private CredentialService credentialService;

    // @InjectMocks creates the real CredentialIssuanceService and injects
    // the mock CredentialService into it via the @Autowired field
    @InjectMocks
    private CredentialIssuanceService issuanceService;

    private RegistrationRequest sampleRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Register BouncyCastle so 'BC' is available as a provider
        // (same thing BouncyCastleConfig.java does in production)
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }

        // Build a sample patient registration request
        sampleRequest = new RegistrationRequest(
            "99-9999-9999-9999",               // ABHA ID
            "did:veristas:patient:Ab3xYz9k1m2n" // Patient's DID
        );

        // @InjectMocks creates the service but does NOT call @PostConstruct
        // (that requires Spring). We call it manually to initialize hospitalDid
        // and the hospital key pair — exactly what @PostConstruct does in production.
        issuanceService.initHospitalAuthority();

        // Tell the mock: when issueCredential() is called with ANY input,
        // return this fake JWT string instead of doing real crypto
        when(credentialService.issueCredential(any(CredentialRequest.class)))
            .thenReturn("eyJhbGciOiJFUzI1NiJ9.fakePayload.fakeSignature");
    }

    // ─── 1. STRUCTURE TESTS ─────────────────────────────────────────────────

    @Test
    void issueCredential_returnedVC_isNotNull() throws Exception {
        VerifiableCredential vc = issuanceService.issueCredential(sampleRequest);
        assertNotNull(vc, "The issued VerifiableCredential must not be null");
    }

    @Test
    void issueCredential_context_containsW3CStandardUrl() throws Exception {
        // The W3C VC standard REQUIRES this exact URL as the first context entry.
        // Any VC without it is not standards-compliant and will be rejected by
        // other systems (like a government verifier app).
        VerifiableCredential vc = issuanceService.issueCredential(sampleRequest);
        assertNotNull(vc.getContext(), "Context list must not be null");
        assertTrue(vc.getContext().contains("https://www.w3.org/2018/credentials/v1"),
            "W3C VC standard requires 'https://www.w3.org/2018/credentials/v1' in context");
    }

    @Test
    void issueCredential_type_containsVerifiableCredential() throws Exception {
        VerifiableCredential vc = issuanceService.issueCredential(sampleRequest);
        assertNotNull(vc.getType(), "Type list must not be null");
        assertTrue(vc.getType().contains("VerifiableCredential"),
            "W3C standard requires 'VerifiableCredential' as a base type");
    }

    @Test
    void issueCredential_type_containsPatientAbhaCredential() throws Exception {
        VerifiableCredential vc = issuanceService.issueCredential(sampleRequest);
        assertTrue(vc.getType().contains("PatientAbhaCredential"),
            "Our custom type 'PatientAbhaCredential' must be in the type list");
    }

    @Test
    void issueCredential_id_isUniqueUuidUrl() throws Exception {
        // Each credential gets a globally unique ID (UUID-based URL).
        // Issue two credentials for different patients — their IDs must differ.
        VerifiableCredential vc1 = issuanceService.issueCredential(sampleRequest);
        VerifiableCredential vc2 = issuanceService.issueCredential(
            new RegistrationRequest("88-8888-8888-8888", "did:veristas:patient:OtherPat"));

        assertNotNull(vc1.getId(), "VC ID must not be null");
        assertTrue(vc1.getId().startsWith("http://veristasid.hospital.com/credentials/"),
            "VC ID must be a full URL");
        assertNotEquals(vc1.getId(), vc2.getId(),
            "Each issued VC must have a globally unique ID — no two VCs share the same ID");
    }

    // ─── 2. ISSUER TESTS ────────────────────────────────────────────────────

    @Test
    void issueCredential_issuer_isHospitalDid() throws Exception {
        VerifiableCredential vc = issuanceService.issueCredential(sampleRequest);
        assertNotNull(vc.getIssuer(), "Issuer DID must not be null");
        assertTrue(vc.getIssuer().startsWith("did:veristas:hospital:"),
            "Issuer must be the hospital DID — not null, not a string like 'hospital'");
    }

    @Test
    void issueCredential_issuanceDate_isIso8601Format() throws Exception {
        // W3C VCs require dates in ISO 8601 format: "2026-07-09T10:30:00Z"
        // Instant.now().toString() produces exactly this format.
        VerifiableCredential vc = issuanceService.issueCredential(sampleRequest);
        assertNotNull(vc.getIssuanceDate(), "Issuance date must not be null");
        assertTrue(vc.getIssuanceDate().contains("T") && vc.getIssuanceDate().endsWith("Z"),
            "Issuance date must be ISO 8601 UTC format ending with 'Z': e.g. '2026-07-09T10:00:00Z'");
    }

    // ─── 3. SUBJECT (PATIENT DATA) TESTS ────────────────────────────────────

    @Test
    void issueCredential_credentialSubject_containsPatientDid() throws Exception {
        VerifiableCredential vc = issuanceService.issueCredential(sampleRequest);
        assertNotNull(vc.getCredentialSubject(), "CredentialSubject must not be null");
        assertEquals("did:veristas:patient:Ab3xYz9k1m2n",
            vc.getCredentialSubject().getId(),
            "CredentialSubject.id must be the patient's DID — the subject of the credential");
    }

    @Test
    void issueCredential_credentialSubject_containsAbhaId() throws Exception {
        VerifiableCredential vc = issuanceService.issueCredential(sampleRequest);
        assertEquals("99-9999-9999-9999",
            vc.getCredentialSubject().getAbhaId(),
            "CredentialSubject must contain the patient's ABHA ID as a verifiable claim");
    }

    @Test
    void issueCredential_differentPatients_getDifferentSubjects() throws Exception {
        VerifiableCredential vc1 = issuanceService.issueCredential(sampleRequest);
        VerifiableCredential vc2 = issuanceService.issueCredential(
            new RegistrationRequest("88-8888-8888-8888", "did:veristas:patient:OtherPat"));

        assertNotEquals(vc1.getCredentialSubject().getAbhaId(),
                        vc2.getCredentialSubject().getAbhaId(),
            "Two different patients must get VCs with different subjects");
    }

    // ─── 4. PROOF (CRYPTOGRAPHIC SIGNATURE) TESTS ───────────────────────────

    @Test
    void issueCredential_proof_isNotNull() throws Exception {
        VerifiableCredential vc = issuanceService.issueCredential(sampleRequest);
        assertNotNull(vc.getProof(), "Proof must not be null — unsigned VC is not verifiable");
    }

    @Test
    void issueCredential_proofType_isJwtProof2020() throws Exception {
        // "JwtProof2020" is the W3C standard name for JWT-based VC proofs.
        // The verifier app uses this string to know HOW to verify the proof.
        VerifiableCredential vc = issuanceService.issueCredential(sampleRequest);
        assertEquals("JwtProof2020", vc.getProof().getType(),
            "Proof type must be 'JwtProof2020' — the W3C standard for JWT-signed VCs");
    }

    @Test
    void issueCredential_proofJwt_isNotNullOrEmpty() throws Exception {
        VerifiableCredential vc = issuanceService.issueCredential(sampleRequest);
        assertNotNull(vc.getProof().getJwt(), "Proof JWT must not be null");
        assertFalse(vc.getProof().getJwt().isEmpty(), "Proof JWT must not be empty");
    }

    @Test
    void issueCredential_proofJwt_hasThreeJwtParts() throws Exception {
        // The proof is a JWT (header.payload.signature)
        VerifiableCredential vc = issuanceService.issueCredential(sampleRequest);
        String jwt = vc.getProof().getJwt();
        assertEquals(3, jwt.split("\\.").length,
            "Proof JWT must have exactly 3 dot-separated parts: header.payload.signature");
    }

    // ─── 5. DEPENDENCY INTERACTION TEST (Mockito verify) ────────────────────

    @Test
    void issueCredential_callsCredentialService_exactlyOnce() throws Exception {
        // This is a Mockito test — not an assertion on the return value,
        // but a verification of HOW the service called its dependency.
        issuanceService.issueCredential(sampleRequest);

        // verify() checks that credentialService.issueCredential() was called
        // exactly 1 time during the above call. If it was called 0 or 2 times, this fails.
        verify(credentialService, times(1))
            .issueCredential(any(CredentialRequest.class));
    }

    @Test
    void issueCredential_credentialRequest_containsPatientDid() throws Exception {
        issuanceService.issueCredential(sampleRequest);

        // Capture what CredentialRequest was actually passed to the mock
        // and verify it has the patient's DID in subjectDid
        verify(credentialService).issueCredential(
            argThat(req -> "did:veristas:patient:Ab3xYz9k1m2n".equals(req.getSubjectDid()))
        );
    }
}
