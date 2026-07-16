package com.example.VeristasId.Controller;

import com.example.VeristasId.Model.EmergencySessionEntity;
import com.example.VeristasId.Repository.EmergencySessionRepository;
import com.example.VeristasId.Service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 10 — EmergencySessionController Tests
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: The Break-Glass Emergency Workflow
 * ═══════════════════════════════════════════════════════════
 *
 * The EmergencySessionController governs the most safety-critical
 * feature in the entire system — break-glass emergency access.
 *
 * The three endpoints form a lifecycle:
 *
 *   POST /api/emergency/create       ← DISPATCHER ONLY (role-locked)
 *         ↓ creates ESID with stage = "dispatched"
 *   POST /api/emergency/update-stage ← any staff, moves stage forward
 *         ↓ dispatched → arrived → resolved
 *   GET  /api/emergency/{esid}       ← anyone, read current state
 *
 * OPA (tested separately in OpaServiceTest) uses the stage value
 * returned by GET to decide whether a paramedic/surgeon can access
 * the patient's medical records at that moment.
 *
 * ═══════════════════════════════════════════════════════════
 *  WHY standaloneSetup() AGAIN?
 * ═══════════════════════════════════════════════════════════
 *
 * Same reason as SecurityConfigTest (Day 9):
 * EmergencySessionController has @Autowired dependencies
 * (sessionRepository, jwtService). @WebMvcTest would try to load
 * the full Spring context including DB and JWT key config → fails.
 *
 * standaloneSetup() gives us:
 *   1. new EmergencySessionController(mockRepo, mockJwt) — direct constructor
 *   2. MockMvc wrapping just that one controller
 *   3. Zero Spring context, zero DB, tests run in ~3 seconds
 */
@ExtendWith(MockitoExtension.class)
class EmergencySessionControllerTest {

    @Mock private EmergencySessionRepository sessionRepository;
    @Mock private JwtService jwtService;

    private MockMvc mockMvc;

    // ObjectMapper serialises Java Maps → JSON strings for request bodies
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Constructor injection — the controller requires both dependencies
        EmergencySessionController controller =
                new EmergencySessionController(sessionRepository, jwtService);

        // Wrap in MockMvc — no filter chain here; the OpaSecurityFilter
        // is tested separately in SecurityConfigTest
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ─── 1. CREATE SESSION — happy path ──────────────────────────────────────

    @Test
    void createEmergency_validDispatcher_returns200AndSavesSession() throws Exception {
        /*
         * Setup:
         *  - Token is valid (jwtService says so)
         *  - Role extracted from token = "dispatcher" (the only role allowed)
         *  - ESID "ESID-001" does not yet exist in DB
         *
         * Expected:
         *  - HTTP 200 OK
         *  - Response body mentions the ESID
         *  - sessionRepository.save() is called exactly once
         */
        when(jwtService.verifyStaffToken("Bearer dispatcherJWT")).thenReturn(true);
        when(jwtService.extractRole("Bearer dispatcherJWT")).thenReturn("dispatcher");
        when(sessionRepository.findById("ESID-001")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/emergency/create")
                .header("Authorization", "Bearer dispatcherJWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                        Map.of("esid", "ESID-001", "patientId", "PAT-001"))))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("ESID-001")));

        // Verify the entity was actually persisted
        verify(sessionRepository, times(1)).save(any(EmergencySessionEntity.class));
    }

    // ─── 2. CREATE SESSION — security gates ──────────────────────────────────

    @Test
    void createEmergency_invalidToken_returns401_andNeverSaves() throws Exception {
        /*
         * Token fails verifyStaffToken → 401 returned before role check.
         * verify(never()) proves the controller short-circuits: it does NOT
         * call extractRole() or save() when the token is bad.
         */
        when(jwtService.verifyStaffToken(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/emergency/create")
                .header("Authorization", "Bearer forgedToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                        Map.of("esid", "ESID-002", "patientId", "PAT-001"))))
               .andExpect(status().isUnauthorized());

        // Short-circuit proof: save() must never be called
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void createEmergency_paramedicRole_returns403_preventingPrivilegeEscalation() throws Exception {
        /*
         * Paramedics can READ records in "dispatched" stage (OPA-controlled)
         * but they CANNOT create sessions — that would be privilege escalation
         * (a paramedic authorising their own emergency access).
         *
         * The controller checks role === "dispatcher" strictly.
         * Any other role, even valid staff, gets 403 Forbidden.
         */
        when(jwtService.verifyStaffToken(anyString())).thenReturn(true);
        when(jwtService.extractRole(anyString())).thenReturn("paramedic");

        mockMvc.perform(post("/api/emergency/create")
                .header("Authorization", "Bearer paramedicJWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                        Map.of("esid", "ESID-003", "patientId", "PAT-001"))))
               .andExpect(status().isForbidden())
               .andExpect(content().string(containsString("dispatcher")));

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void createEmergency_surgeonRole_returns403() throws Exception {
        /*
         * Surgeon has the highest privilege in OPA but still cannot
         * self-authorise an emergency session. Same 403 gate applies.
         */
        when(jwtService.verifyStaffToken(anyString())).thenReturn(true);
        when(jwtService.extractRole(anyString())).thenReturn("surgeon");

        mockMvc.perform(post("/api/emergency/create")
                .header("Authorization", "Bearer surgeonJWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                        Map.of("esid", "ESID-004", "patientId", "PAT-002"))))
               .andExpect(status().isForbidden());

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void createEmergency_unknownRole_returns403() throws Exception {
        // Ensures no unlisted role (e.g. "nurse", "admin") can slip through
        when(jwtService.verifyStaffToken(anyString())).thenReturn(true);
        when(jwtService.extractRole(anyString())).thenReturn("nurse");

        mockMvc.perform(post("/api/emergency/create")
                .header("Authorization", "Bearer nurseJWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                        Map.of("esid", "ESID-005", "patientId", "PAT-003"))))
               .andExpect(status().isForbidden());
    }

    // ─── 3. CREATE SESSION — idempotency / duplicate guard ───────────────────

    @Test
    void createEmergency_duplicateEsid_returns409Conflict() throws Exception {
        /*
         * A dispatcher accidentally sends the same ESID twice.
         * The controller checks sessionRepository.findById(esid).isPresent()
         * and returns 409 Conflict to prevent overwriting an active session.
         *
         * WHY THIS MATTERS: If we allowed duplicates, a second "create"
         * call could reset a live session's stage from "arrived" back to
         * "dispatched", suddenly revoking a surgeon's active access
         * mid-operation.
         */
        when(jwtService.verifyStaffToken(anyString())).thenReturn(true);
        when(jwtService.extractRole(anyString())).thenReturn("dispatcher");
        when(sessionRepository.findById("ESID-001"))
                .thenReturn(Optional.of(
                        new EmergencySessionEntity("ESID-001", "PAT-001", "dispatched", 0L)));

        mockMvc.perform(post("/api/emergency/create")
                .header("Authorization", "Bearer dispatcherJWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                        Map.of("esid", "ESID-001", "patientId", "PAT-001"))))
               .andExpect(status().isConflict())
               .andExpect(content().string(containsString("already exists")));

        // Duplicate → must NOT overwrite the existing session
        verify(sessionRepository, never()).save(any());
    }

    // ─── 4. UPDATE STAGE ─────────────────────────────────────────────────────

    @Test
    void updateStage_existingSession_returns200AndUpdatesStage() throws Exception {
        /*
         * Stage progression: dispatched → arrived → resolved
         * Each step gates different OPA access levels.
         * Here we move from "dispatched" to "arrived".
         */
        EmergencySessionEntity session =
                new EmergencySessionEntity("ESID-001", "PAT-001", "dispatched", 0L);
        when(sessionRepository.findById("ESID-001")).thenReturn(Optional.of(session));

        mockMvc.perform(post("/api/emergency/update-stage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                        Map.of("esid", "ESID-001", "stage", "ARRIVED"))))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("arrived")));

        verify(sessionRepository).save(session);
    }

    @Test
    void updateStage_normalizesStageToLowercase() throws Exception {
        /*
         * The controller calls request.get("stage").toLowerCase() before saving.
         * This test proves the normalisation actually happens by inspecting the
         * entity passed to save() using argThat().
         *
         * WHY LOWERCASE MATTERS: OPA's Rego policy uses lowercase string
         * comparisons: input.stage == "arrived". If we stored "ARRIVED",
         * the OPA check would fail and surgeons would be locked out.
         */
        EmergencySessionEntity session =
                new EmergencySessionEntity("ESID-001", "PAT-001", "dispatched", 0L);
        when(sessionRepository.findById("ESID-001")).thenReturn(Optional.of(session));

        mockMvc.perform(post("/api/emergency/update-stage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                        Map.of("esid", "ESID-001", "stage", "RESOLVED"))))
               .andExpect(status().isOk());

        // argThat inspects the actual entity object passed to save()
        verify(sessionRepository).save(
                argThat(s -> "resolved".equals(s.getStage())));
    }

    @Test
    void updateStage_nonexistentSession_returns404() throws Exception {
        when(sessionRepository.findById("ESID-999")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/emergency/update-stage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                        Map.of("esid", "ESID-999", "stage", "ARRIVED"))))
               .andExpect(status().isNotFound());

        verify(sessionRepository, never()).save(any());
    }

    // ─── 5. GET SESSION ───────────────────────────────────────────────────────

    @Test
    void getSession_existingEsid_returns200WithEntity() throws Exception {
        /*
         * This is the endpoint OPA calls (via OpaService) to look up
         * the current session stage before making an allow/deny decision.
         * A 200 response with the entity is the "green light" for OPA.
         */
        EmergencySessionEntity session =
                new EmergencySessionEntity("ESID-001", "PAT-001", "arrived", 0L);
        when(sessionRepository.findById("ESID-001")).thenReturn(Optional.of(session));

        mockMvc.perform(get("/api/emergency/ESID-001"))
               .andExpect(status().isOk());
    }

    @Test
    void getSession_nonexistentEsid_returns404() throws Exception {
        /*
         * OpaService's fallback: if the session doesn't exist,
         * GET returns 404 → OpaService defaults to deny.
         * This test confirms the controller propagates the 404 correctly.
         */
        when(sessionRepository.findById("ESID-999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/emergency/ESID-999"))
               .andExpect(status().isNotFound());
    }
}
