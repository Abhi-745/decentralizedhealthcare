package com.example.VeristasId.Repository;

import com.example.VeristasId.Model.EmergencySessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmergencySessionRepository extends JpaRepository<EmergencySessionEntity, String> {
    // Find session by patient ID to support Rule R3
    Optional<EmergencySessionEntity> findByPatientId(String patientId);
}