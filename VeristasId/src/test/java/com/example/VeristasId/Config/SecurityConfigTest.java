package com.example.VeristasId.Config;

import com.example.VeristasId.Service.CredentialService;
import com.example.VeristasId.Service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Day 9 — OpaSecurityFilter Tests
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: The HTTP Filter Chain
 * ═══════════════════════════════════════════════════════════
 *
 * Every HTTP request passes through a filter chain BEFORE
 * reaching any @RestController in Spring Boot:
 *
 *   Request → [OpaSecurityFilter] → Controller (or 401)
 *
 * OpaSecurityFilter decides:
 *   1. PUBLIC endpoint? (/api/auth/, /api/vc/issue …)
 *      → chain.doFilter() — pass immediately, no token check.
 *   2. PROTECTED endpoint?
 *      → Read "Authorization: Bearer <token>" header.
 *      → Valid Patient VC OR valid Staff JWT → pass through.
 *      → Missing / invalid → return HTTP 401 immediately.
 *
 * ═══════════════════════════════════════════════════════════
 *  WHY standaloneSetup() INSTEAD OF @WebMvcTest?
 * ═══════════════════════════════════════════════════════════
 *
 * @WebMvcTest loads a partial Spring context. It tries to wire
 * OpaSecurityFilter's @Autowired fields (CredentialService,
 * JwtService) which depend on the DB and OPA server → context fails.
 *
 * MockMvcBuilders.standaloneSetup() requires ZERO Spring context:
 *   1. Create the filter ourselves (new OpaSecurityFilter()).
 *   2. Inject Mockito mocks into its @Autowired fields via ReflectionTestUtils.
 *   3. Register a tiny DummyController so MockMvc has something to route to.
 *   4. Add the filter to the MockMvc chain with .addFilter().
 *
 * Every test runs in milliseconds with no database or OPA server.
 */

// Tiny in-test controller — exists only so the filter has something
// to hand requests off to when it decides to ALLOW them.
@RestController
class DummyController {
    @GetMapping("/**")
    public String ok() { return "ok"; }
}

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock private CredentialService credentialService;
    @Mock private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 1. Create the real filter — no Spring, just new()
        OpaSecurityFilter filter = new OpaSecurityFilter();

        // 2. Inject our mocks into its private @Autowired fields via reflection
        ReflectionTestUtils.setField(filter, "credentialService", credentialService);
        ReflectionTestUtils.setField(filter, "jwtService", jwtService);

        // 3. Build standalone MockMvc — DummyController handles any allowed request.
        //    .addFilter(filter) places OpaSecurityFilter into the chain.
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyController())
                .addFilter(filter)
                .build();
    }

    // ─── 1. PUBLIC ENDPOINTS: no token required ───────────────────────────────

    @Test
    void publicEndpoint_authLogin_passesFilterWithoutToken() throws Exception {
        // /api/auth/ is in PUBLIC_ENDPOINTS → filter calls chain.doFilter()
        // DummyController responds 200 OK → proves the filter allowed it through.
        mockMvc.perform(get("/api/auth/login"))
               .andExpect(status().isOk());
    }

    @Test
    void publicEndpoint_vcIssue_passesFilterWithoutToken() throws Exception {
        mockMvc.perform(get("/api/vc/issue"))
               .andExpect(status().isOk());
    }

    @Test
    void publicEndpoint_patientRegister_passesFilterWithoutToken() throws Exception {
        mockMvc.perform(get("/api/patients/register"))
               .andExpect(status().isOk());
    }

    @Test
    void publicEndpoint_h2Console_passesFilterWithoutToken() throws Exception {
        mockMvc.perform(get("/h2-console"))
               .andExpect(status().isOk());
    }

    // ─── 2. PROTECTED ENDPOINTS: missing / invalid token → 401 ───────────────

    @Test
    void protectedEndpoint_noHeader_returns401() throws Exception {
        // No Authorization header → filter writes 401 and returns immediately
        mockMvc.perform(get("/api/medical-records/99-9999-9999-9999"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_bothServicesRejectToken_returns401() throws Exception {
        when(credentialService.verifyVC(anyString())).thenReturn(false);
        when(jwtService.verifyStaffToken(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/medical-records/99-9999-9999-9999")
                .header("Authorization", "Bearer forgedToken"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_emptyAuthHeader_returns401() throws Exception {
        when(credentialService.verifyVC(anyString())).thenReturn(false);
        when(jwtService.verifyStaffToken(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/emergency/create")
                .header("Authorization", ""))
               .andExpect(status().isUnauthorized());
    }

    // ─── 3. PROTECTED ENDPOINTS: valid token → passes through ────────────────

    @Test
    void protectedEndpoint_validPatientVC_returns200() throws Exception {
        // Patient VC valid → filter allows → DummyController returns 200
        when(credentialService.verifyVC("Bearer patientVC")).thenReturn(true);
        when(jwtService.verifyStaffToken(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/medical-records/99-9999-9999-9999")
                .header("Authorization", "Bearer patientVC"))
               .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_validStaffJWT_returns200() throws Exception {
        // Staff JWT valid → filter allows → DummyController returns 200
        when(credentialService.verifyVC(anyString())).thenReturn(false);
        when(jwtService.verifyStaffToken("Bearer staffJWT")).thenReturn(true);

        mockMvc.perform(get("/api/emergency/session/ESID-001")
                .header("Authorization", "Bearer staffJWT"))
               .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_bothTokensValid_returns200() throws Exception {
        // OR logic — if EITHER is valid, the request passes
        when(credentialService.verifyVC(anyString())).thenReturn(true);
        when(jwtService.verifyStaffToken(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/medical-records/TEST")
                .header("Authorization", "Bearer someToken"))
               .andExpect(status().isOk());
    }
}
