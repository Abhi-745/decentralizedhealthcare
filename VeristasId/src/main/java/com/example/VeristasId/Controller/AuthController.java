package com.example.VeristasId.Controller;

import com.example.VeristasId.Service.CredentialService;
import com.example.VeristasId.Service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final CredentialService credentialService;

    public AuthController(JwtService jwtService, CredentialService credentialService) {
        this.jwtService = jwtService;
        this.credentialService = credentialService;
    }

    // ─── Staff Login endpoints ─────────────────────────────────────────────────

    @PostMapping("/login-paramedic")
    public ResponseEntity<Map<String, String>> loginParamedic() {
        String token = jwtService.generateToken("Bobbi D'Amore", "paramedic", "EMT-9110");
        return ResponseEntity.ok(Map.of(
                "token", "Bearer " + token,
                "badge", "EMT-9110",
                "name", "Bobbi D'Amore",
                "role", "paramedic"
        ));
    }

    @PostMapping("/login-surgeon")
    public ResponseEntity<Map<String, String>> loginSurgeon() {
        String token = jwtService.generateToken("Dr. Fisher", "surgeon", "SURG-1000");
        return ResponseEntity.ok(Map.of(
                "token", "Bearer " + token,
                "badge", "SURG-1000",
                "name", "Dr. Fisher",
                "role", "surgeon"
        ));
    }

    // Dispatcher is the ONLY role authorized to create Emergency Sessions
    @PostMapping("/login-dispatcher")
    public ResponseEntity<Map<String, String>> loginDispatcher() {
        String token = jwtService.generateToken("Control Room Alpha", "dispatcher", "DISP-0001");
        return ResponseEntity.ok(Map.of(
                "token", "Bearer " + token,
                "badge", "DISP-0001",
                "name", "Control Room Alpha",
                "role", "dispatcher"
        ));
    }

    // ─── Patient Login via Verifiable Credential ───────────────────────────────
    // Patients do NOT use username/password. They present their VC (issued at
    // registration) as their identity token. This endpoint verifies it and
    // returns their claims (DID, ABHA ID, name etc.) so the UI can display them.
    @PostMapping("/patient-login")
    public ResponseEntity<?> patientLogin(@RequestHeader("Authorization") String vcToken) {
        var status = credentialService.checkRevocationStatus(vcToken.replace("Bearer ", "").trim());

        if (!"VALID".equalsIgnoreCase(status.getStatus())) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "VC is " + status.getStatus(),
                    "message", status.getMessage()
            ));
        }

        Map<?, ?> claims = credentialService.extractClaims(vcToken);
        return ResponseEntity.ok(Map.of(
                "status", "AUTHENTICATED",
                "authMethod", "Verifiable Credential (ECDSA ES256)",
                "claims", claims,
                "message", "Identity verified. No password required — your VC is your login."
        ));
    }
}
