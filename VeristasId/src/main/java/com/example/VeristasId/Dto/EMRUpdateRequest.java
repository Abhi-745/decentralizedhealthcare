package com.example.VeristasId.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EMRUpdateRequest {
    private String esid;         // which emergency session this update belongs to
    private String diagnosis;
    private String vitals;
    private String prescription;
}