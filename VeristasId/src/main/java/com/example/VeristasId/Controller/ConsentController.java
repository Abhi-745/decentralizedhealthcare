package com.example.VeristasId.Controller;

import com.example.VeristasId.Dto.ConsentRequest;
import com.example.VeristasId.Model.Consent;
import com.example.VeristasId.Repository.ConsentRepository;
import com.example.VeristasId.Service.BlockchainAuditService;
import com.example.VeristasId.Service.CredentialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consent")
public class ConsentController {

    private final ConsentRepository consentRepository;
    private final CredentialService credentialService;
    private final BlockchainAuditService blockchainAuditService;

    public ConsentController(ConsentRepository consentRepository,
                             CredentialService credentialService,
                             BlockchainAuditService blockchainAuditService) {
        this.consentRepository = consentRepository;
        this.credentialService = credentialService;
        this.blockchainAuditService = blockchainAuditService;
    }

    // ── GRANT consent ──────────────────────────────────────────────
    @PostMapping("/grant")
    public ResponseEntity<?> grantConsent(
            @RequestHeader("Authorization") String token,
            @RequestBody ConsentRequest request) {

        if (!credentialService.verifyVC(token)) {
            return ResponseEntity.status(401).body("Invalid or revoked credential");
        }

        Map<String, Object> claims = credentialService.extractClaims(token);
        String patientDid = (String) claims.getOrDefault("sub", "unknown");

        boolean alreadyExists = consentRepository
                .findByPatientDidAndDelegateDidAndActiveTrue(patientDid, request.getDelegateDid())
                .isPresent();

        if (alreadyExists) {
            return ResponseEntity.badRequest().body("Active consent already exists for this delegate");
        }

        Consent consent = new Consent();
        consent.setPatientDid(patientDid);
        consent.setDelegateDid(request.getDelegateDid());
        consent.setPurpose(request.getPurpose());
        consent.setActive(true);
        consent.setSignature(sha256(patientDid + request.getDelegateDid() + request.getPurpose()));

        consentRepository.save(consent);

        blockchainAuditService.recordAccessAttempt(
                patientDid, request.getDelegateDid(), "CONSENT_GRANTED", true);

        return ResponseEntity.ok("Consent granted to: " + request.getDelegateDid()
                + " for purpose: " + request.getPurpose());
    }

    // ── REVOKE consent ─────────────────────────────────────────────
    @PostMapping("/revoke")
    public ResponseEntity<?> revokeConsent(
            @RequestHeader("Authorization") String token,
            @RequestBody ConsentRequest request) {

        if (!credentialService.verifyVC(token)) {
            return ResponseEntity.status(401).body("Invalid or revoked credential");
        }

        Map<String, Object> claims = credentialService.extractClaims(token);
        String patientDid = (String) claims.getOrDefault("sub", "unknown");

        Consent consent = consentRepository
                .findByPatientDidAndDelegateDidAndActiveTrue(patientDid, request.getDelegateDid())
                .orElse(null);

        if (consent == null) {
            return ResponseEntity.badRequest().body("No active consent found for this delegate");
        }

        consent.setActive(false);
        consentRepository.save(consent);

        blockchainAuditService.recordAccessAttempt(
                patientDid, request.getDelegateDid(), "CONSENT_REVOKED", true);

        return ResponseEntity.ok("Consent revoked for: " + request.getDelegateDid());
    }

    // ── VIEW all consents for this patient ─────────────────────────
    @GetMapping("/mine")
    public ResponseEntity<?> myConsents(
            @RequestHeader("Authorization") String token) {

        if (!credentialService.verifyVC(token)) {
            return ResponseEntity.status(401).body("Invalid or revoked credential");
        }

        Map<String, Object> claims = credentialService.extractClaims(token);
        String patientDid = (String) claims.getOrDefault("sub", "unknown");

        List<Consent> consents = consentRepository.findAllByPatientDid(patientDid);

        return ResponseEntity.ok(consents);
    }

    // ── SHA-256 helper for consent signature ───────────────────────
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "SIG_FAILED";
        }
    }
}