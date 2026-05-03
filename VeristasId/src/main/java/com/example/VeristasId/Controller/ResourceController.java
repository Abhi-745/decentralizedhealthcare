package com.example.VeristasId.Controller;

import com.example.VeristasId.Service.AuditService;
import com.example.VeristasId.Service.CredentialService;
import com.example.VeristasId.Service.OpaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    @Autowired
    private OpaService opaService;

    @Autowired
    private CredentialService credService;

    @Autowired
    private AuditService auditService;

    @GetMapping("/secure-report")
    public ResponseEntity<String> getReport(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-Simulated-Time", required = false) String simTime) {

        // 1. Identity Verification via Credential Service
        if (!credService.checkRevocationStatus(token).getStatus().equals("VALID")) {
            return ResponseEntity.status(401).body("Identity Revoked or Invalid");
        }

        // 2. Extract Attribute Claims from the Verifiable Credential
        Map<String, Object> user = credService.extractClaims(token);
        String subject = (String) user.get("sub");

        // 3. Define the Requested Clinical Resource
        Map<String, Object> resource = Map.of(
                "name", "Q1_Report",
                "required_clearance", 2
        );

        // 4. State-Aware ABAC Decision via OPA Engine
        // This provides the nearly-constant response time recommended for emergency access.
        boolean allowed = opaService.checkAccess(user.toString(), resource.toString(), simTime);

        // 5. Immutable Audit Logging
        // Fixed: We call the service method instead of nesting a definition here.
        auditService.logDecision(subject, "Q1_Report", allowed);

        return allowed
                ? ResponseEntity.ok("ACCESS GRANTED: [CONFIDENTIAL CLINICAL DATA]")
                : ResponseEntity.status(403).body("ACCESS DENIED: Policy Violation for current Stage");
    }
}