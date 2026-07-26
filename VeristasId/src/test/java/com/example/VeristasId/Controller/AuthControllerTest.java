package com.example.VeristasId.Controller;

import com.example.VeristasId.Service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// Hamcrest matchers — explicit to avoid clash with Mockito.startsWith
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
// Mockito — specific imports so 'startsWith' is unambiguous above
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 15 (Part 2) — AuthControllerTest
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: Role-Based Token Issuance
 * ═══════════════════════════════════════════════════════════
 *
 * AuthController provides three login endpoints — one per staff role:
 *
 *   POST /api/auth/login-paramedic  → Issues a PARAMEDIC token
 *   POST /api/auth/login-surgeon    → Issues a SURGEON token
 *   POST /api/auth/login-dispatcher → Issues a DISPATCHER token (+ extra "role" field)
 *
 * Unlike a real-world auth system (which would validate a username/password),
 * this system uses pre-configured staff credentials for demonstration purposes.
 * The JwtService.generateToken(name, role, badge) encodes all three values
 * directly into the JWT payload.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 1: POST with No Request Body
 * ═══════════════════════════════════════════════════════════
 *
 * Usually, @PostMapping methods accept a @RequestBody (some JSON).
 * AuthController's login methods are unusual: they take NO body at all.
 * The staff credentials are hardcoded into the controller source code.
 *
 * This is acceptable for a demo system but would NEVER be done in production.
 * In a real hospital system, you would:
 *   1. Accept { "badgeNumber": "EMT-9110", "password": "..." }
 *   2. Look up the staff member from a database
 *   3. Verify the password (bcrypt)
 *   4. Only then generate the JWT
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 2: The "Bearer" Token Format (RFC 6750)
 * ═══════════════════════════════════════════════════════════
 *
 * The response always wraps the raw JWT in "Bearer " + token.
 * This is the HTTP Authorization header standard:
 *
 *   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ...
 *
 * The word "Bearer" is a TOKEN TYPE — it means "the bearer of this
 * token has the claimed identity." Other token types include "Basic"
 * (username:password in base64) and "Digest".
 *
 * Clients must strip the "Bearer " prefix before decoding the JWT.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 3: Why Dispatcher Gets an Extra "role" Field
 * ═══════════════════════════════════════════════════════════
 *
 * The dispatcher response has a fourth field: "role": "dispatcher"
 * that the paramedic and surgeon responses do NOT have.
 *
 * This is because the dispatcher is the ONLY role authorized to create
 * Emergency Sessions (POST /api/emergency/dispatch). The frontend uses
 * the presence of the "role" field to conditionally render the
 * "Create Emergency" button in the UI — only dispatchers see it.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT 4: Verifying Exact Argument Values
 * ═══════════════════════════════════════════════════════════
 *
 * verify(mock).method(arg1, arg2, arg3) checks that the method was
 * called with EXACTLY those values. Any difference (e.g., "Surgeon"
 * instead of "surgeon") would cause the test to fail with:
 *
 *   Wanted but not invoked:
 *     jwtService.generateToken("Dr. Fisher", "surgeon", "SURG-1000");
 *   However, there was exactly 1 interaction with this mock:
 *     jwtService.generateToken("Dr. Fisher", "Surgeon", "SURG-1000");
 *
 * This precision catches case-sensitivity bugs in the role string,
 * which would cause OPA's Rego policy to fail silently.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(jwtService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ─── POST /api/auth/login-paramedic ───────────────────────────────────────

    @Test
    void loginParamedic_returns200_withBearerToken() throws Exception {
        /*
         * The raw JWT from JwtService is wrapped with "Bearer " prefix.
         * We assert the token field starts with "Bearer " to confirm
         * this concatenation is happening correctly.
         *
         * startsWith() is a Hamcrest string matcher, equivalent to
         * Java's String.startsWith().
         */
        when(jwtService.generateToken("Bobbi D'Amore", "paramedic", "EMT-9110"))
                .thenReturn("eyJhbGciOiJIUzI1NiJ9.paramedic.SIG");

        mockMvc.perform(post("/api/auth/login-paramedic"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.token", startsWith("Bearer ")));
    }

    @Test
    void loginParamedic_includesCorrectBadgeAndName() throws Exception {
        /*
         * The response JSON must carry the badge number and name alongside
         * the token. The frontend uses these to display a welcome card
         * without needing to decode the JWT on the client side.
         */
        when(jwtService.generateToken(anyString(), anyString(), anyString()))
                .thenReturn("fake-jwt");

        mockMvc.perform(post("/api/auth/login-paramedic"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.badge", is("EMT-9110")))
               .andExpect(jsonPath("$.name",  is("Bobbi D'Amore")));
    }

    @Test
    void loginParamedic_callsJwtServiceWithExactRoleArgument() throws Exception {
        /*
         * The role string "paramedic" (all lowercase) MUST match exactly.
         *
         * The OPA Rego policy checks: input.role == "paramedic"
         * If the controller passes "Paramedic" (capital P), the OPA
         * check fails silently — no error is thrown, but the paramedic
         * is denied access to all protected endpoints. This is the kind
         * of subtle bug that only an exact-argument verify() catches.
         */
        when(jwtService.generateToken("Bobbi D'Amore", "paramedic", "EMT-9110"))
                .thenReturn("fake-jwt");

        mockMvc.perform(post("/api/auth/login-paramedic"))
               .andExpect(status().isOk());

        verify(jwtService).generateToken("Bobbi D'Amore", "paramedic", "EMT-9110");
    }

    // ─── POST /api/auth/login-surgeon ─────────────────────────────────────────

    @Test
    void loginSurgeon_returns200_withSurgeonDetails() throws Exception {
        /*
         * The surgeon response must contain the surgeon-specific badge "SURG-1000"
         * and the name "Dr. Fisher". Any mismatch would issue the correct JWT
         * but show wrong staff details in the UI.
         */
        when(jwtService.generateToken("Dr. Fisher", "surgeon", "SURG-1000"))
                .thenReturn("eyJhbGciOiJIUzI1NiJ9.surgeon.SIG");

        mockMvc.perform(post("/api/auth/login-surgeon"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.token", startsWith("Bearer ")))
               .andExpect(jsonPath("$.badge", is("SURG-1000")))
               .andExpect(jsonPath("$.name",  is("Dr. Fisher")));
    }

    // ─── POST /api/auth/login-dispatcher ──────────────────────────────────────

    @Test
    void loginDispatcher_returns200_withDispatcherDetails() throws Exception {
        when(jwtService.generateToken("Control Room Alpha", "dispatcher", "DISP-0001"))
                .thenReturn("eyJhbGciOiJIUzI1NiJ9.dispatcher.SIG");

        mockMvc.perform(post("/api/auth/login-dispatcher"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.token", startsWith("Bearer ")))
               .andExpect(jsonPath("$.badge", is("DISP-0001")))
               .andExpect(jsonPath("$.name",  is("Control Room Alpha")));
    }

    @Test
    void loginDispatcher_uniquelyIncludesRoleField() throws Exception {
        /*
         * The dispatcher response has a "role" field that the other two
         * login responses do NOT have.
         *
         * This is intentional: the StaffLoginPage frontend reads the "role"
         * field from the login response. If "role" == "dispatcher", it shows
         * the "Create Emergency Session" button.
         *
         * For paramedic and surgeon, no "role" field → no dispatch button.
         * This is a front-end authorization check (the back-end OPA check
         * is the actual security gate — the UI is just UX convenience).
         */
        when(jwtService.generateToken("Control Room Alpha", "dispatcher", "DISP-0001"))
                .thenReturn("fake-dispatcher-jwt");

        mockMvc.perform(post("/api/auth/login-dispatcher"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.role", is("dispatcher")));
    }

    @Test
    void loginSurgeon_doesNotHaveRoleField() throws Exception {
        /*
         * Mirror of the previous test — proves the "role" key is
         * EXCLUSIVE to the dispatcher response. jsonPath("$.role")
         * returns empty/null for surgeon and paramedic.
         *
         * doesNotExist() is the correct Hamcrest matcher for asserting
         * a JSON key is absent. Using is(null) would fail if the key
         * is present with a null value — doesNotExist() is stricter.
         */
        when(jwtService.generateToken(anyString(), anyString(), anyString()))
                .thenReturn("fake-jwt");

        mockMvc.perform(post("/api/auth/login-surgeon"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.role").doesNotExist());
    }
}
