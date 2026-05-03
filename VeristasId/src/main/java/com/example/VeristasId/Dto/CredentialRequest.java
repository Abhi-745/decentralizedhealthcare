package com.example.VeristasId.Dto;

import lombok.Data;
import java.util.Map;

@Data
public class CredentialRequest {
    private String subjectDid;       // "did:ethr:0x123..."
    private String credentialType;   // "EmployeeID"

    // THE IMPORTANT PART FOR ABAC:
    private Map<String, Object> claims;
    // Example: { "role": "Manager", "clearance": 3, "dept": "IT" }
}