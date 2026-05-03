package com.example.VeristasId.Controller;

import com.example.VeristasId.Dto.CredentialRequest;
import com.example.VeristasId.Dto.StatusResponse;
import com.example.VeristasId.Service.CredentialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/vc")
public class CredentialController {
    @Autowired private CredentialService service;

    @PostMapping("/issue")
    public ResponseEntity<String> issue(@RequestBody CredentialRequest req) {
        return ResponseEntity.ok(service.issueCredential(req));
    }

    @PostMapping("/verify")
    public ResponseEntity<StatusResponse> verify(@RequestParam String token) {
        return ResponseEntity.ok(service.checkRevocationStatus(token));
    }

    @PostMapping("/revoke")
    public ResponseEntity<String> revoke(@RequestParam String token) {
        service.revokeCredential(token);
        return ResponseEntity.ok("Revoked Successfully");
    }

    @GetMapping("/inspect")
    public ResponseEntity<Map<String, Object>> inspect(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(service.extractClaims(token));
    }
}