package com.example.VeristasId.Service;

import com.example.VeristasId.Dto.CredentialRequest;
import com.example.VeristasId.Dto.StatusResponse;
import com.example.VeristasId.Model.VCEntity;
import com.example.VeristasId.Repository.VCRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CredentialService {

    @Autowired private VCRepository vcRepository;

    @Value("${issuer.did}") private String issuerDid;
    @Value("${issuer.private.key}") private String privateKeyHex;

    private PrivateKey privateKey;

    @PostConstruct
    public void init() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            byte[] keyBytes = Hex.decode(privateKeyHex.trim());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
            this.privateKey = kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("CRITICAL: Failed to load ECDSA Private Key", e);
        }
    }

    public String issueCredential(CredentialRequest request) {
        String token = Jwts.builder()
                .setIssuer(issuerDid)
                .setSubject(request.getSubjectDid())
                .setId(UUID.randomUUID().toString())
                .addClaims(request.getClaims())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 31536000000L))
                .signWith(privateKey, SignatureAlgorithm.ES256)
                .compact();

        VCEntity vc = new VCEntity();
        vc.setSubjectDid(request.getSubjectDid());
        vc.setProof(token);
        vc.setRevoked(false);
        vcRepository.save(vc);

        return token;
    }

    // THIS IS THE METHOD THAT WAS MISSING/RED
    public StatusResponse checkRevocationStatus(String token) {
        Optional<VCEntity> vcOpt = vcRepository.findByProof(token.trim());

        if (vcOpt.isEmpty()) return new StatusResponse("NOT_FOUND", "Identity not found in Ledger.");
        if (vcOpt.get().isRevoked()) return new StatusResponse("REVOKED", "Identity Revoked.");

        return new StatusResponse("VALID", "Identity Active.");
    }

    public void revokeCredential(String token) {
        vcRepository.findByProof(token.trim()).ifPresent(vc -> {
            vc.setRevoked(true);
            vcRepository.save(vc);
        });
    }

    public Map extractClaims(String token) {
        if (token == null || token.isEmpty()) return Map.of();

        String clean = token.replace("Bearer ", "").trim();
        String[] parts = clean.split("\\.");

        if (parts.length < 2) {
            return Map.of("role", "anonymous"); // Fallback for simulation tokens
        }

        try {
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(payload, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    public boolean verifyVC(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
        String token = authHeader.replace("Bearer ", "").trim();

        try {
            // Since we only have the ECDSA Private Key loaded (and io.jsonwebtoken throws an 
            // IllegalArgumentException if you try to verify ES256 with a PrivateKey), 
            // we rely on the Immutable DB Ledger as the source of truth for verification.
            StatusResponse status = checkRevocationStatus(token);
            return "VALID".equalsIgnoreCase(status.getStatus());
        } catch (Exception e) {
            System.out.println("JWT Verification Failed: " + e.getMessage());
            return false;
        }
    }
}