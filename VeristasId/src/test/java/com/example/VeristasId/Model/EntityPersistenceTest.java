package com.example.VeristasId.Model;

import com.example.VeristasId.Repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest   // ← starts H2 in-memory DB, loads ONLY JPA layer, NO web/security
class EntityPersistenceTest {

    @Autowired MedicalRecordRepository   medicalRepo;
    @Autowired ConsentRepository         consentRepo;
    @Autowired EmergencySessionRepository sessionRepo;
    @Autowired AuditBlockRepository      auditRepo;
    @Autowired VCRepository              vcRepo;

    @Test
    void medicalRecord_saveAndFind_byAbhaId() {
        MedicalRecord r = new MedicalRecord();
        r.setAbhaId("TEST-001");
        r.setPatientName("Test Patient");
        r.setBloodGroup("B+");
        medicalRepo.save(r);

        Optional<MedicalRecord> found = medicalRepo.findByAbhaId("TEST-001");
        assertTrue(found.isPresent());
        assertEquals("Test Patient", found.get().getPatientName());
        assertNotNull(found.get().getId()); // DB assigned the ID
    }

    @Test
    void consent_saveAndFind_byPatientDid() {
        Consent c = new Consent(null,
            "did:veristas:patient:abc",
            "did:veristas:hospital:xyz",
            "emergency_access", true, "sig123");
        consentRepo.save(c);

        // Verify it's in DB
        assertTrue(consentRepo.count() > 0);
    }

    @Test
    void emergencySession_stringPK_savedCorrectly() {
        EmergencySessionEntity s = new EmergencySessionEntity(
            "ES-TEST-01", "PAT-001", "dispatched", System.currentTimeMillis());
        sessionRepo.save(s);

        Optional<EmergencySessionEntity> found = sessionRepo.findById("ES-TEST-01");
        assertTrue(found.isPresent());
        assertEquals("dispatched", found.get().getStage());
    }

    @Test
    void emergencySession_stageUpdate_persistsCorrectly() {
        EmergencySessionEntity s = new EmergencySessionEntity(
            "ES-TEST-02", "PAT-002", "dispatched", System.currentTimeMillis());
        sessionRepo.save(s);

        // Simulate stage update (like EmergencySessionController does)
        s.setStage("arrived");
        sessionRepo.save(s);

        EmergencySessionEntity updated = sessionRepo.findById("ES-TEST-02").get();
        assertEquals("arrived", updated.getStage());
    }

    @Test
    void auditBlock_savedWithHash_retrievedCorrectly() {
        AuditBlockEntity block = new AuditBlockEntity();
        block.setBlockIndex(0);
        block.setTimestamp(System.currentTimeMillis());
        block.setAccessorId("SYSTEM");
        block.setTargetAbhaId("NONE");
        block.setAction("GENESIS");
        block.setAccessGranted(true);
        block.setPreviousHash("0");
        block.setHash("a".repeat(64));
        auditRepo.save(block);

        assertEquals(1, auditRepo.count());
        assertNotNull(auditRepo.findAll().get(0).getHash());
    }

    @Test
    void vcEntity_prePersist_setsIssuedAt() {
        VCEntity vc = new VCEntity();
        vc.setSubjectDid("did:veristas:patient:abc");
        vc.setProof("eyJhbGciOiJFUzI1NiJ9.test.signature");
        vc.setRevoked(false);
        vcRepo.save(vc);

        VCEntity saved = vcRepo.findAll().get(0);
        assertNotNull(saved.getIssuedAt(), "@PrePersist must set issuedAt");
        assertFalse(saved.isRevoked());
    }

    @Test
    void medicalRecord_notFound_returnsEmpty() {
        Optional<MedicalRecord> result = medicalRepo.findByAbhaId("DOES-NOT-EXIST");
        assertFalse(result.isPresent());
    }
}
