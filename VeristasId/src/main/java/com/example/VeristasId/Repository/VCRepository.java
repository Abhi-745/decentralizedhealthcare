package com.example.VeristasId.Repository;

import com.example.VeristasId.Model.VCEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VCRepository extends JpaRepository<VCEntity, Long> {

    // Custom query to find credential by the JWT string (proof)
    Optional<VCEntity> findByProof(String proof);
}