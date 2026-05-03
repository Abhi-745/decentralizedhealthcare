package com.example.VeristasId.Dto;

import lombok.Data;

@Data
public class VerifyRequest {
    private String proof; // The full JWT/Proof string you got during issuance
}