package com.example.VeristasId.Controller;

import com.example.VeristasId.Service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 16 (Part 2) — AccessControllerTest
 *
 * ═══════════════════════════════════════════════════════════
 *  BIG PICTURE: AccessController & The Testability Anti-Pattern
 * ═══════════════════════════════════════════════════════════
 *
 * AccessController orchestrates emergency access using OPA.
 * It makes a real HTTP call to the OPA engine via RestTemplate.
 *
 * The problem: RestTemplate is created internally:
 *
 *   private final RestTemplate restTemplate = new RestTemplate();
 *
 * This is a TESTABILITY ANTI-PATTERN. Because the RestTemplate
 * is not injected (not a Spring bean), you can't mock it directly
 * with @Mock + @InjectMocks.
 *
 * ═══════════════════════════════════════════════════════════
 *  KEY CONCEPT: MockRestServiceServer
 * ═══════════════════════════════════════════════════════════
 *
 * Instead of replacing the RestTemplate with a mock, Spring provides
 * MockRestServiceServer — a test server that wraps an EXISTING
 * RestTemplate instance and intercepts its HTTP calls.
 *
 *   RestTemplate realTemplate = (RestTemplate) ReflectionTestUtils
 *       .getField(controller, "restTemplate");
 *   MockRestServiceServer server = MockRestServiceServer.createServer(realTemplate);
 *
 * Now when the controller calls restTemplate.postForEntity(opaUrl, ...),
 * the MockRestServiceServer intercepts the request and returns a
 * pre-configured fake JSON response — without making any real network call.
 *
 * This is the proper Spring way to test code that uses RestTemplate
 * without dependency injection.
 *
 * ═══════════════════════════════════════════════════════════
 *  WHY THIS IS A CODE SMELL (Interview Answer)
 * ═══════════════════════════════════════════════════════════
 *
 * The "correct" way to design AccessController would be:
 *
 * @Configuration class:
 *   @Bean
 *   public RestTemplate restTemplate() { return new RestTemplate(); }
 *
 * AccessController:
 *   private final RestTemplate restTemplate;
 *   public AccessController(RestTemplate restTemplate, AuditService ...) {
 *       this.restTemplate = restTemplate; // injected!
 *   }
 *
 * Then in tests, you can simply @Mock RestTemplate and use @InjectMocks.
 * This is why Spring recommends constructor injection for all dependencies,
 * including infrastructure beans like RestTemplate.
 */
@ExtendWith(MockitoExtension.class)
class AccessControllerTest {

    @Mock AuditService auditService;

    @InjectMocks
    AccessController controller;

    private MockMvc             mockMvc;
    private MockRestServiceServer mockServer;

    private static final String TEST_OPA_URL =
            "http://localhost:8181/v1/data/veritas/emergency/allow";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        doNothing().when(auditService).logDecision(anyString(), anyString(), anyBoolean());

        // Override @Value field with our test OPA URL
        ReflectionTestUtils.setField(controller, "opaUrl", TEST_OPA_URL);

        // Get the actual RestTemplate from the controller and wrap it in a mock server
        RestTemplate restTemplate =
                (RestTemplate) ReflectionTestUtils.getField(controller, "restTemplate");
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Test
    void emergencyAccess_opaAllows_returns200() throws Exception {
        /*
         * OPA responds with {"result": true} → access is granted.
         * The controller returns 200 + "ACCESS GRANTED" message.
         *
         * We program the MockRestServiceServer to:
         *   - Expect a POST to the OPA URL
         *   - Respond with a JSON body where result = true
         */
        mockServer.expect(requestTo(TEST_OPA_URL))
                  .andExpect(method(HttpMethod.POST))
                  .andRespond(withSuccess("{\"result\":true}", MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/resource/emergency-access")
                .header("Authorization", "Bearer valid-staff-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"read\",\"stage\":\"dispatched\",\"user_did\":\"did:veritas:emt-001\"}"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("ACCESS GRANTED")));

        mockServer.verify(); // Assert the expected HTTP call was actually made
    }

    @Test
    void emergencyAccess_opaDenies_returns403() throws Exception {
        /*
         * OPA responds with {"result": false} → access is denied.
         * The controller returns 403 FORBIDDEN.
         *
         * Example scenario: paramedic is still in "dispatched" stage
         * but is trying to "write" data, which OPA's Rego policy
         * only permits during the "arrived" stage.
         */
        mockServer.expect(requestTo(TEST_OPA_URL))
                  .andExpect(method(HttpMethod.POST))
                  .andRespond(withSuccess("{\"result\":false}", MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/resource/emergency-access")
                .header("Authorization", "Bearer valid-staff-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"write\",\"stage\":\"dispatched\",\"user_did\":\"did:veritas:emt-001\"}"))
               .andExpect(status().isForbidden())
               .andExpect(content().string(containsString("ACCESS DENIED")));
    }

    @Test
    void emergencyAccess_opaUnreachable_returns500AndAuditsError() throws Exception {
        /*
         * If OPA is down (network error, container crash, misconfiguration),
         * RestTemplate throws an exception.
         *
         * The catch (Exception e) block must:
         *   1. Log "SYSTEM_ERROR_OPA" to the audit trail (so the incident is recorded).
         *   2. Return 500 Internal Server Error with an error message.
         *
         * This test is critical for proving the system degrades safely.
         * An unreachable OPA engine must never silently grant access!
         *
         * withServerError() tells the MockRestServiceServer to respond
         * with a 500, which causes RestTemplate to throw an exception.
         */
        mockServer.expect(requestTo(TEST_OPA_URL))
                  .andRespond(withServerError());

        mockMvc.perform(post("/api/resource/emergency-access")
                .header("Authorization", "Bearer valid-staff-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"read\",\"stage\":\"dispatched\",\"user_did\":\"did:veritas:emt-001\"}"))
               .andExpect(status().isInternalServerError());

        // The audit MUST record the system error — security incidents must always be logged
        verify(auditService).logDecision(
                eq("did:veritas:emt-001"),
                eq("SYSTEM_ERROR_OPA"),
                eq(false)
        );
    }
}
