package com.example.VeristasId.Repository;

import com.example.VeristasId.Model.VCEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VCRepository extends JpaRepository<VCEntity, Long> {

    // Custom query to find credential by the JWT string (proof)
    Optional<VCEntity> findByProof(String proof);

    // Custom query to check if a patient already has an active credential
    // We use "findFirst" because the database might already be flooded with duplicates from before the patch!
    Optional<VCEntity> findFirstBySubjectDidAndRevokedFalse(String subjectDid);

    // Security check to ensure a single ABHA ID cannot be registered to multiple DIDs
    Optional<VCEntity> findFirstByAbhaIdAndRevokedFalse(String abhaId);
}