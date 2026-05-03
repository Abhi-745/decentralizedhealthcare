package com.example.VeristasId.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpaInput {
    private Map<String, Object> user;      // Attributes from the VC (Role, Dept)
    private Map<String, Object> resource;  // Attributes of the file/API (Sensitivity)
    private Map<String, Object> env;       // Context (Time, IP)
}