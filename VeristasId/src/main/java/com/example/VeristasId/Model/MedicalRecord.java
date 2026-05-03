package com.example.VeristasId.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientDid;
    private String abhaId;
    private String updatedByDid;
    private String esid;

    // Patient profile fields (persisted from DataInjector)
    private String patientName;
    private String bloodGroup;
    private String allergies;
    private String status; // e.g. PENDING_BIOMETRIC_MERGE for provisional

    private String diagnosis;
    private String vitals;
    private String prescription;

    @CreationTimestamp
    private LocalDateTime updatedAt;
}