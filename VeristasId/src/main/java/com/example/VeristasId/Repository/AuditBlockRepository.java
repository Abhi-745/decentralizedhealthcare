package com.example.VeristasId.Repository;

import com.example.VeristasId.Model.AuditBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditBlockRepository extends JpaRepository<AuditBlockEntity, Long> {
    // Returns the last saved block (highest blockIndex)
    AuditBlockEntity findTopByOrderByBlockIndexDesc();
    boolean existsByBlockIndex(int blockIndex);
}
