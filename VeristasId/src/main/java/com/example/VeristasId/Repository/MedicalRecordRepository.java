package com.example.VeristasId.Repository;

import com.example.VeristasId.Model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    Optional<MedicalRecord> findByPatientDid(String patientDid);
    Optional<MedicalRecord> findByAbhaId(String abhaId);
}