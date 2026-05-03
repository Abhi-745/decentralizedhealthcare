package com.example.VeristasId.Controller;

import com.example.VeristasId.Service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/resource")
public class AccessController {

    @Value("${OPA_URL:http://localhost:8181/v1/data/veritas/emergency/allow}")
    private String opaUrl;

    @Autowired
    private AuditService auditService;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/emergency-access")
    public ResponseEntity<String> requestEmergencyAccess(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> requestBody) {

        String action = (String) requestBody.getOrDefault("action", "read");
        String stage = (String) requestBody.getOrDefault("stage", "dispatched");
        String userDid = (String) requestBody.getOrDefault("user_did", "Unknown_DID");

        try {
            // 1. Prepare input for OPA
            Map<String, Object> opaInput = new HashMap<>();
            Map<String, Object> inputDetails = new HashMap<>();
            Map<String, Object> sessionContext = new HashMap<>();

            sessionContext.put("stage", stage);
            inputDetails.put("action", action);
            inputDetails.put("session", sessionContext);
            inputDetails.put("token", token);

            opaInput.put("input", inputDetails);

            // 2. Query OPA Policy Engine
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(opaInput, headers);

            ResponseEntity<OpaResponse> opaResponse = restTemplate.postForEntity(opaUrl, requestEntity, OpaResponse.class);
            boolean isAllowed = (opaResponse.getBody() != null && opaResponse.getBody().isResult());

            // 3. SECURE AUDIT: Always log the outcome
            auditService.logDecision(userDid, action.toUpperCase() + "_" + stage.toUpperCase(), isAllowed);

            // 4. Return Final Response
            if (isAllowed) {
                return ResponseEntity.ok("ACCESS GRANTED: Patient vital data unsealed.");
            } else {
                return ResponseEntity.status(403).body("ACCESS DENIED: AC-ABAC Policy rejected the request.");
            }

        } catch (Exception e) {
            auditService.logDecision(userDid, "SYSTEM_ERROR_OPA", false);
            return ResponseEntity.status(500).body("Internal Security Engine Error: " + e.getMessage());
        }
    }

    public static class OpaResponse {
        private boolean result;
        public boolean isResult() { return result; }
        public void setResult(boolean result) { this.result = result; }
    }
}