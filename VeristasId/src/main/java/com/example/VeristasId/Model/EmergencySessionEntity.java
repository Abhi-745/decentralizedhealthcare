package com.example.VeristasId.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "emergency_sessions")
@Data
@NoArgsConstructor    // Required by JPA
@AllArgsConstructor   // Fixes the "Create constructor" error
public class EmergencySessionEntity {
    @Id
    private String esid;
    private String patientId;
    private String stage;
}