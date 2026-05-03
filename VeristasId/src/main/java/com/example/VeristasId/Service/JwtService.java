package com.example.VeristasId.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret.key}")
    private String secretKeyBase64;

    // Fixed key loaded from application.properties — survives restarts!
    private Key hospitalSecretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        // Ensure the key is at least 256 bits for HS256
        if (keyBytes.length < 32) {
            // Pad to 32 bytes if needed
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.hospitalSecretKey = Keys.hmacShaKeyFor(keyBytes);
        System.out.println("🔐 [JWT] Hospital Secret Key loaded from application.properties (fixed across restarts).");
    }

    public String generateToken(String name, String role, String badgeNumber) {

        // 1. The Payload (Who is this user?)
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);      // e.g., "paramedic" or "surgeon"
        claims.put("name", name);
        claims.put("badge", badgeNumber);

        // 2. Build and cryptographically sign the Token
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(badgeNumber)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                // Token expires in exactly 12 hours (end of a hospital shift)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12))
                .signWith(hospitalSecretKey) // Applies the unbreakable signature
                .compact();
    }

    public boolean verifyStaffToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
        String token = authHeader.replace("Bearer ", "").trim();

        try {
            Jwts.parserBuilder()
                    .setSigningKey(hospitalSecretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}