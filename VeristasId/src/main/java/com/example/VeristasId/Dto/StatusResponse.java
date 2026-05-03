package com.example.VeristasId.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatusResponse {
    private String status;  // "VALID", "REVOKED", or "NOT_FOUND"
    private String message; // Human-readable explanation (e.g., "Credential is active")
}