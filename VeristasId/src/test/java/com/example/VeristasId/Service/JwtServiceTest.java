package com.example.VeristasId.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Day 5 — JwtService Tests
 *
 * A JWT (JSON Web Token) has 3 parts separated by dots:
 *
 *   eyJhbGciOiJIUzI1NiJ9  .  eyJyb2xlIjoicGFyYW1lZGljIn0  .  Hk3x9aBcD...
 *   ↑ HEADER (base64)         ↑ PAYLOAD (base64, NOT encrypted)   ↑ SIGNATURE
 *
 * The HEADER says which algorithm was used (HS256 = HMAC-SHA256).
 * The PAYLOAD contains the claims (role, name, badge, expiry).
 * The SIGNATURE = HMAC_SHA256(header + "." + payload, secretKey)
 *
 * Why does this matter?
 *   - Anyone can DECODE the payload (it's just base64 — not encrypted)
 *   - But only the hospital with the SECRET KEY can SIGN it
 *   - A tampered token has an invalid signature → rejected by verifyStaffToken()
 *
 * The secret key is: VmVyaXN0YXNJZE1lZGljYWxJZGVudGl0eVN5c3RlbTIwMjZTZWN1cmVLZXk=
 * Base64 decoded → "VeristasIdMedicalIdentitySystem2026SecureKey"
 * This is 44 chars > 32 bytes (256 bits) → meets HS256 minimum requirement
 */
class JwtServiceTest {

    private JwtService jwtService;

    // The same key configured in application.properties
    private static final String TEST_SECRET =
        "VmVyaXN0YXNJZE1lZGljYWxJZGVudGl0eVN5c3RlbTIwMjZTZWN1cmVLZXk=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Inject the secret key via Spring's ReflectionTestUtils
        // (bypasses @Value — allows testing without starting full Spring context)
        ReflectionTestUtils.setField(jwtService, "secretKeyBase64", TEST_SECRET);
        jwtService.init(); // triggers @PostConstruct manually
    }

    // ─── 1. TOKEN GENERATION ─────────────────────────────────────────────

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken("Dr. Sharma", "surgeon", "BADGE-001");
        assertNotNull(token, "Token must not be null");
    }

    @Test
    void generateToken_hasThreeParts_validJwtStructure() {
        String token = jwtService.generateToken("John", "paramedic", "BADGE-002");
        // JWT = header.payload.signature — always exactly 3 parts
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length,
            "A valid JWT must have exactly 3 parts: header.payload.signature");
    }

    @Test
    void generateToken_differentRoles_produceDifferentTokens() {
        String paramedicToken = jwtService.generateToken("John", "paramedic", "B-001");
        String surgeonToken   = jwtService.generateToken("John", "surgeon",   "B-001");
        // Different role claim → different signature → different token
        assertNotEquals(paramedicToken, surgeonToken,
            "Tokens with different roles must be different");
    }

    // ─── 2. TOKEN VERIFICATION ───────────────────────────────────────────

    @Test
    void verifyStaffToken_validTokenWithBearerPrefix_returnsTrue() {
        String token = jwtService.generateToken("Ravi", "dispatcher", "D-999");
        assertTrue(jwtService.verifyStaffToken("Bearer " + token),
            "A freshly generated token must be valid");
    }

    @Test
    void verifyStaffToken_nullHeader_returnsFalse() {
        // Null check before any parsing — must not throw NullPointerException
        assertFalse(jwtService.verifyStaffToken(null),
            "Null Authorization header must return false, not throw NPE");
    }

    @Test
    void verifyStaffToken_missingBearerPrefix_returnsFalse() {
        String token = jwtService.generateToken("Ravi", "paramedic", "P-001");
        // Raw token without "Bearer " prefix — should be rejected
        assertFalse(jwtService.verifyStaffToken(token),
            "Token without 'Bearer ' prefix must be rejected");
    }

    @Test
    void verifyStaffToken_emptyString_returnsFalse() {
        assertFalse(jwtService.verifyStaffToken(""),
            "Empty authorization header must return false");
    }

    @Test
    void verifyStaffToken_tamperedSignature_returnsFalse() {
        String token = jwtService.generateToken("Dr. Mehta", "surgeon", "S-007");
        // Corrupt the last 5 characters of the signature
        // This simulates an attacker trying to forge a token
        String tampered = "Bearer " + token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtService.verifyStaffToken(tampered),
            "Tampered signature must be rejected — HMAC verification must fail");
    }

    @Test
    void verifyStaffToken_randomGibberish_returnsFalse() {
        assertFalse(jwtService.verifyStaffToken("Bearer not.a.real.jwt.at.all"),
            "Random string that looks like Bearer token must be rejected");
    }

    @Test
    void verifyStaffToken_onlyBearerWord_returnsFalse() {
        assertFalse(jwtService.verifyStaffToken("Bearer "),
            "'Bearer ' with empty token must return false");
    }

    // ─── 3. ROLE EXTRACTION ──────────────────────────────────────────────

    @Test
    void extractRole_paramedic_returnsCorrectRole() {
        String token = jwtService.generateToken("Arjun", "paramedic", "P-042");
        String role  = jwtService.extractRole("Bearer " + token);
        assertEquals("paramedic", role,
            "Role claim 'paramedic' must be extractable from the token payload");
    }

    @Test
    void extractRole_surgeon_returnsCorrectRole() {
        String token = jwtService.generateToken("Dr. Priya", "surgeon", "S-010");
        String role  = jwtService.extractRole("Bearer " + token);
        assertEquals("surgeon", role);
    }

    @Test
    void extractRole_dispatcher_returnsCorrectRole() {
        String token = jwtService.generateToken("Control", "dispatcher", "D-001");
        String role  = jwtService.extractRole("Bearer " + token);
        assertEquals("dispatcher", role);
    }

    @Test
    void extractRole_nullHeader_returnsUnknown() {
        // Must degrade gracefully — return "unknown" not throw NPE
        String role = jwtService.extractRole(null);
        assertEquals("unknown", role,
            "Null header must return 'unknown' — graceful degradation");
    }

    @Test
    void extractRole_tamperedToken_returnsUnknown() {
        String token = jwtService.generateToken("Dr. Singh", "surgeon", "S-099");
        String tampered = "Bearer " + token.substring(0, token.length() - 5) + "YYYYY";
        assertEquals("unknown", jwtService.extractRole(tampered),
            "Tampered token must return 'unknown' — not crash the service");
    }

    // ─── 4. ROUND-TRIP ───────────────────────────────────────────────────
    // The most important test: generate a token → verify it → extract the role
    // This is exactly the path every HTTP request takes in production

    @Test
    void fullRoundTrip_generateVerifyExtract_allPass() {
        // Step 1: Dispatcher issues tokens at login
        String paramedicToken  = jwtService.generateToken("Karan", "paramedic",  "P-100");
        String surgeonToken    = jwtService.generateToken("Dr. Rao", "surgeon",  "S-200");
        String dispatcherToken = jwtService.generateToken("Control", "dispatcher", "D-300");

        // Step 2: Each incoming request — verify the token
        assertTrue(jwtService.verifyStaffToken("Bearer " + paramedicToken));
        assertTrue(jwtService.verifyStaffToken("Bearer " + surgeonToken));
        assertTrue(jwtService.verifyStaffToken("Bearer " + dispatcherToken));

        // Step 3: Extract role for OPA input
        assertEquals("paramedic",  jwtService.extractRole("Bearer " + paramedicToken));
        assertEquals("surgeon",    jwtService.extractRole("Bearer " + surgeonToken));
        assertEquals("dispatcher", jwtService.extractRole("Bearer " + dispatcherToken));
    }
}
