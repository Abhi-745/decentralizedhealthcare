package com.example.VeristasId.Config;

import com.example.VeristasId.Service.CredentialService;
import com.example.VeristasId.Service.JwtService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;

@Component
public class OpaSecurityFilter implements Filter {

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private JwtService jwtService;

    // Define endpoints that do NOT require a Verifiable Credential token
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/vc/issue",
            "/api/vc/verify",
            "/api/vc/revoke",
            "/api/patients/register",
            "/api/patients/auto-register-demo",
            "/api/medical-records/provisional",
            "/api/auth/", // Allow paramedics to get their tokens!
            "/h2-console"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI();

        // 1. PUBLIC ENDPOINTS: Bypass all checks
        boolean isPublic = PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
        if (isPublic) {
            chain.doFilter(req, res);
            return;
        }

        // 2. PROTECTED ENDPOINTS: Check for the token
        String authHeader = request.getHeader("Authorization");

        boolean isValidPatientToken = credentialService.verifyVC(authHeader);
        boolean isValidStaffToken = jwtService.verifyStaffToken(authHeader);

        if (authHeader == null || (!isValidPatientToken && !isValidStaffToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized: Missing or Invalid VC/JWT\"}");
            return;
        }

        chain.doFilter(req, res);
    }
}