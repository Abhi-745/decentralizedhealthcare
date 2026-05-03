package com.example.VeristasId.Dto;

import lombok.Data;

@Data
public class OpaResponse {
    private boolean result; // Maps to the boolean output from OPA policy
}