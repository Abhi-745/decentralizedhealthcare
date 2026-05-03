package com.example.VeristasId.Controller;

import com.example.VeristasId.Service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login-paramedic")
    public ResponseEntity<Map<String, String>> loginParamedic() {
        String token = jwtService.generateToken("Bobbi D'Amore", "paramedic", "EMT-9110");
        return ResponseEntity.ok(Map.of(
                "token", "Bearer " + token,
                "badge", "EMT-9110",
                "name", "Bobbi D'Amore"
        ));
    }

    @PostMapping("/login-surgeon")
    public ResponseEntity<Map<String, String>> loginSurgeon() {
        String token = jwtService.generateToken("Dr. Fisher", "surgeon", "SURG-1000");
        return ResponseEntity.ok(Map.of(
                "token", "Bearer " + token,
                "badge", "SURG-1000",
                "name", "Dr. Fisher"
        ));
    }
}
