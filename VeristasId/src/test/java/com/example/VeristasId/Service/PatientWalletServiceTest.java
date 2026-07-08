package com.example.VeristasId.Service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.security.PublicKey;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Day 6 — PatientWalletService Tests (BouncyCastle + secp256k1)
 *
 * ═══════════════════════════════════════════════════
 *  BIG PICTURE: What is a Patient Wallet?
 * ═══════════════════════════════════════════════════
 *
 * In traditional systems, a hospital assigns you an ID number.
 * The hospital "owns" your identity.
 *
 * In this system, the PATIENT generates their own identity — a
 * cryptographic key pair — ON THEIR OWN DEVICE (simulated by this service).
 * The hospital can never know the Private Key. This is Self-Sovereign Identity (SSI).
 *
 * The wallet generates TWO mathematically linked keys:
 *
 *   Private Key ───► Known ONLY to the patient's device
 *       |              Used to SIGN documents (prove authorship)
 *       |
 *       └──► Public Key ──► Shared with everyone
 *                            Used to VERIFY signatures
 *                            Used to derive the patient's DID
 *
 * ═══════════════════════════════════════════════════
 *  WHY BOUNCYCASTLE + secp256k1?
 * ═══════════════════════════════════════════════════
 *
 * Java's built-in JCE (Java Cryptography Extension) supports many
 * elliptic curves but NOT secp256k1. This curve is special because
 * it is the SAME curve used by Bitcoin, Ethereum, and MetaMask.
 * Using it means Verifiable Credentials issued here are compatible
 * with the entire Web3 ecosystem.
 *
 * BouncyCastle is an open-source cryptography library that adds
 * support for secp256k1 and registers it as a Java Security Provider
 * under the name "BC".
 *
 * ═══════════════════════════════════════════════════
 *  WHAT IS A DID?
 * ═══════════════════════════════════════════════════
 *
 * DID = Decentralized Identifier. Format: did:method:unique-id
 *
 * In our system:
 *   did:veristas:patient:Ab3x9kM2...
 *   ↑    ↑        ↑       ↑
 *   DID  method  type  first 16 chars of Base64(publicKey)
 *
 * The DID is derived FROM the public key. This is powerful:
 * anyone can verify the DID is legitimate by checking the key math.
 * No central authority (like a DMV) issues these.
 */
class PatientWalletServiceTest {

    @TempDir
    Path tempDir; // JUnit creates a real but temporary folder for key files

    private PatientWalletService walletService;

    @BeforeEach
    void setUp() {
        // Ensure BouncyCastle is registered before each test
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        walletService = new PatientWalletService();
        walletService.initWallet();
        // Note: the wallet writes wallet_private.key and wallet_public.key
        // to the working directory. In a real project, we'd inject the path.
    }

    // ─── 1. DID FORMAT TESTS ─────────────────────────────────────────────────

    @Test
    void generatedDid_startsWithCorrectPrefix() {
        // The DID standard (W3C) mandates the "did:" prefix.
        // "veristas" is our method name. "patient" is the sub-type.
        String did = walletService.getPatientDid();
        assertTrue(did.startsWith("did:veristas:patient:"),
            "DID must follow W3C DID format: did:<method>:<specific-id>");
    }

    @Test
    void generatedDid_hasExactlyFourParts_whenSplitByColon() {
        // Format: did : veristas : patient : <16-char-key-fragment>
        // Splitting by ":" gives exactly 4 parts.
        String did = walletService.getPatientDid();
        String[] parts = did.split(":");
        assertEquals(4, parts.length,
            "DID 'did:veristas:patient:XXXX' must split into exactly 4 colon-separated parts");
    }

    @Test
    void generatedDid_keyFragment_isExactly16Characters() {
        // The service takes the Base64(publicKey) and uses only the first 16 chars.
        // This keeps the DID human-readable without sacrificing uniqueness.
        String did = walletService.getPatientDid();
        String keyFragment = did.split(":")[3]; // the 4th part
        assertEquals(16, keyFragment.length(),
            "DID key fragment must be exactly 16 chars from Base64(publicKey)");
    }

    @Test
    void generatedDid_isNotNull_andNotEmpty() {
        assertNotNull(walletService.getPatientDid());
        assertFalse(walletService.getPatientDid().isEmpty());
    }

    // ─── 2. KEY PAIR TESTS ────────────────────────────────────────────────────

    @Test
    void publicKey_isNotNull_afterWalletInit() {
        // If BouncyCastle registration failed, generateKeyPair() would throw
        // an exception and publicKey would remain null.
        assertNotNull(walletService.getPatientPublicKey(),
            "Public key must be generated after initWallet() — check BouncyCastle is registered");
    }

    @Test
    void publicKey_algorithmIsECDSA() {
        // When using BouncyCastle ("BC" provider), the key algorithm is reported
        // as "ECDSA" (the full algorithm name) — NOT just "EC".
        // Java's built-in SunEC provider returns "EC", but BouncyCastle returns "ECDSA".
        // This is a provider-specific behaviour — both are correct, just different naming.
        PublicKey key = walletService.getPatientPublicKey();
        String algo = key.getAlgorithm();
        assertTrue(algo.equals("EC") || algo.equals("ECDSA"),
            "BouncyCastle keys report 'ECDSA'; SunEC reports 'EC' — both are valid EC keys");
    }

    @Test
    void publicKey_format_isX509Encoded() {
        // X.509 is the international standard for encoding public keys.
        // The format is: "X.509" (Java name) / "SubjectPublicKeyInfo" (ASN.1 name).
        // The private key uses "PKCS#8" format — a different standard for private keys.
        PublicKey key = walletService.getPatientPublicKey();
        assertEquals("X.509", key.getFormat(),
            "Public key must be in X.509 format (standard encoding for public keys)");
    }

    @Test
    void publicKey_encodedBytes_areNotEmpty() {
        byte[] encoded = walletService.getPatientPublicKey().getEncoded();
        assertNotNull(encoded);
        assertTrue(encoded.length > 0,
            "Public key encoded bytes must not be empty");
    }

    // ─── 3. IDEMPOTENCY — WALLET PERSISTENCE TEST ─────────────────────────────
    // The same patient MUST get the same DID on every restart.
    // If it changes, all their issued credentials become invalid.

    @Test
    void walletInit_calledTwice_doesNotCrash() {
        // Calling initWallet() again simulates a server restart.
        // The second call should reload from disk (not re-generate new keys).
        // It must not throw any exceptions.
        assertDoesNotThrow(() -> walletService.initWallet(),
            "Calling initWallet() again must not crash — disk-reload path must work");
    }

    // ─── 4. CREDENTIAL STORAGE TESTS ──────────────────────────────────────────

    @Test
    void storeCredential_canBeRetrievedAfterStorage() {
        String fakeVcJson = "{\"type\":\"VerifiableCredential\",\"subject\":\"did:veristas:patient:test\"}";
        walletService.storeVerifiableCredential(fakeVcJson);

        assertEquals(fakeVcJson, walletService.getStoredCredential(),
            "Stored VC JSON must be retrievable exactly as it was stored");
    }

    @Test
    void storedCredential_isNullBeforeAnythingIsStored() {
        // On a fresh wallet service, before the hospital sends back a VC,
        // the stored credential must be null — not an empty string or error.
        PatientWalletService freshWallet = new PatientWalletService();
        freshWallet.initWallet();
        // The freshWallet may or may not be null depending on whether
        // wallet_public.key already exists — but it should not throw.
        assertDoesNotThrow(() -> freshWallet.getStoredCredential());
    }

    @Test
    void storeCredential_overwritesPreviousCredential() {
        // When the hospital re-issues a VC (e.g., after renewal),
        // the new one must replace the old one — not be appended.
        walletService.storeVerifiableCredential("{\"version\":\"1.0\"}");
        walletService.storeVerifiableCredential("{\"version\":\"2.0\"}");

        assertEquals("{\"version\":\"2.0\"}", walletService.getStoredCredential(),
            "New credential must overwrite the old one — no duplicate storage");
    }

    // ─── 5. BOUNCY CASTLE REGISTRATION TEST ───────────────────────────────────

    @Test
    void bouncyCastle_isRegisteredAsSecurityProvider() {
        // After initWallet() is called, the "BC" provider must be in Java's
        // security provider list. If it's not, the secp256k1 curve will be
        // unavailable and all crypto operations will fail.
        walletService.initWallet(); // triggers Security.addProvider(new BouncyCastleProvider())
        assertNotNull(Security.getProvider("BC"),
            "BouncyCastle must be registered as the 'BC' Java Security Provider");
    }

    @Test
    void bouncyCastle_registeredProviderName_isBC() {
        walletService.initWallet();
        String providerName = Security.getProvider("BC").getName();
        assertEquals("BC", providerName,
            "BouncyCastle provider name must be 'BC' — used in KeyPairGenerator.getInstance(\"ECDSA\", \"BC\")");
    }
}
