package com.example.VeristasId.Repository;

import com.example.VeristasId.Model.EmergencySessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmergencySessionRepository extends JpaRepository<EmergencySessionEntity, String> {
    /*
     * Spring Data JPA derives the query from the method name:
     *   findFirst    → LIMIT 1        (returns at most 1 row)
     *   ByPatientId  → WHERE patient_id = ?
     *   OrderByCreatedAtDesc → ORDER BY created_at DESC (newest session first)
     *
     * Previously this used @Query with a bare SELECT (no LIMIT), which caused
     * IncorrectResultSizeDataAccessException when multiple sessions existed for
     * the same patient. Removing @Query lets Spring Data JPA apply LIMIT 1.
     */
    Optional<EmergencySessionEntity> findFirstByPatientIdOrderByCreatedAtDesc(String patientId);
}