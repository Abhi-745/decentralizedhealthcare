package com.example.VeristasId.Service;

import com.example.VeristasId.Controller.AccessController;
import com.example.VeristasId.Dto.OpaResponse;
import com.example.VeristasId.Model.EmergencySessionEntity;
import com.example.VeristasId.Repository.EmergencySessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpaService {

    private static final Logger log = LoggerFactory.getLogger(OpaService.class);

    private final RestTemplate restTemplate;
    private final EmergencySessionRepository sessionRepo;

    // The OPA endpoint. Default is localhost:8181.
    // "veritas/emergency/allow" maps to the package name and rule in your Rego file.
    @Value("${opa.endpoint:http://localhost:8181/v1/data/veritas/emergency/allow}")
    private String opaEndpoint;

    public OpaService(RestTemplate restTemplate, EmergencySessionRepository sessionRepo) {
        this.restTemplate = restTemplate;
        this.sessionRepo = sessionRepo;
    }

    public boolean checkAccess(String token, String esid, String action) {
        // 1. Fetch the current state of the emergency session
        EmergencySessionEntity session = sessionRepo.findById(esid).orElse(null);

        // FIX 1: Ensure the token has the "Bearer " prefix so OPA can split it correctly
        String safeToken = token.startsWith("Bearer ") ? token : "Bearer " + token;

        // FIX 2: Force the stage to lowercase so "DISPATCHED" matches Rego's "dispatched"
        String safeStage = (session != null && session.getStage() != null) ? session.getStage().toString().toLowerCase() : "";

        // Build a safe session object for OPA
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("stage", safeStage);

        // 2. Build the input payload
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("token", safeToken);
        inputMap.put("action", action.toLowerCase()); // Lowercase action just to be safe too!
        inputMap.put("session", sessionData);

        Map<String, Object> opaRequest = new HashMap<>();
        opaRequest.put("input", inputMap);

        try {
            // 3. Send the POST request to the OPA server
            OpaResponse opaResponse = restTemplate.postForObject(opaEndpoint, opaRequest, OpaResponse.class);

            // 4. Return the boolean result
            return opaResponse != null && opaResponse.isResult();

        } catch (Exception e) {
            log.error("Failed to connect to OPA server at {}: {}", opaEndpoint, e.getMessage());
            return false;
        }
    }
}