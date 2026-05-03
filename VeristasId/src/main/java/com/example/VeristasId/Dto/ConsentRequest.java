package com.example.VeristasId.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequest {
    private String delegateDid;   // doctor/hospital being granted access
    private String purpose;       // e.g. "EMERGENCY_CARE", "FOLLOW_UP", "SURGERY"
}