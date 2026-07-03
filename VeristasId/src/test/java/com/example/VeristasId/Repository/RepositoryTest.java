package com.example.VeristasId.Repository;

import com.example.VeristasId.Model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest  // H2 in-memory DB — no PostgreSQL needed
class RepositoryTest {

    @Autowired MedicalRecordRepository   medicalRepo;
    @Autowired AuditBlockRepository      auditRepo;
    @Autowired ConsentRepository         consentRepo;
    @Autowired EmergencySessionRepository sessionRepo;
    @Autowired VCRepository              vcRepo;

    // ── MedicalRecordRepository ────────────────────────────────────────────

    @Test
    void findByAbhaId_found() {
        MedicalRecord r = new MedicalRecord();
        r.setAbhaId("99-9999-9999-9999");
        r.setPatientName("John Doe");
        medicalRepo.save(r);

        Optional<MedicalRecord> result = medicalRepo.findByAbhaId("99-9999-9999-9999");
        assertTrue(result.isPresent());
        assertEquals("John Doe", result.get().getPatientName());
    }

    @Test
    void findByAbhaId_notFound_returnsEmpty() {
        Optional<MedicalRecord> result = medicalRepo.findByAbhaId("NONEXISTENT");
        // Optional.empty() — never throws NullPointerException
        assertFalse(result.isPresent());
    }

    @Test
    void findByPatientDid_found() {
        MedicalRecord r = new MedicalRecord();
        r.setPatientDid("did:veristas:patient:abc123");
        r.setPatientName("Jane Doe");
        medicalRepo.save(r);

        Optional<MedicalRecord> result = medicalRepo.findByPatientDid("did:veristas:patient:abc123");
        assertTrue(result.isPresent());
        assertEquals("Jane Doe", result.get().getPatientName());
    }

    // ── AuditBlockRepository ───────────────────────────────────────────────

    @Test
    void findTopByOrderByBlockIndexDesc_returnsLatestBlock() {
        // Save 3 blocks in order
        for (int i = 0; i <= 2; i++) {
            AuditBlockEntity b = new AuditBlockEntity();
            b.setBlockIndex(i);
            b.setTimestamp(System.currentTimeMillis());
            b.setAccessorId("SYSTEM");
            b.setTargetAbhaId("NONE");
            b.setAction("TEST");
            b.setAccessGranted(true);
            b.setPreviousHash("0");
            b.setHash("a".repeat(64));
            auditRepo.save(b);
        }

        AuditBlockEntity latest = auditRepo.findTopByOrderByBlockIndexDesc();
        // Must return block index 2 — the highest
        assertEquals(2, latest.getBlockIndex());
    }

    @Test
    void existsByBlockIndex_true_whenExists() {
        AuditBlockEntity b = new AuditBlockEntity();
        b.setBlockIndex(0);
        b.setTimestamp(System.currentTimeMillis());
        b.setAccessorId("SYSTEM");
        b.setTargetAbhaId("NONE");
        b.setAction("GENESIS");
        b.setAccessGranted(true);
        b.setPreviousHash("0");
        b.setHash("a".repeat(64));
        auditRepo.save(b);

        assertTrue(auditRepo.existsByBlockIndex(0));
        assertFalse(auditRepo.existsByBlockIndex(999)); // doesn't exist
    }

    // ── ConsentRepository ──────────────────────────────────────────────────

    @Test
    void findByPatientDidAndDelegateDidAndActiveTrue_findsActiveConsent() {
        Consent c = new Consent(null,
            "did:veristas:patient:abc",
            "did:veristas:hospital:xyz",
            "emergency_access", true, "sig");
        consentRepo.save(c);

        Optional<Consent> found = consentRepo
            .findByPatientDidAndDelegateDidAndActiveTrue(
                "did:veristas:patient:abc",
                "did:veristas:hospital:xyz");

        assertTrue(found.isPresent());
        assertEquals("emergency_access", found.get().getPurpose());
    }

    @Test
    void findByPatientDidAndDelegateDidAndActiveTrue_ignoresRevokedConsent() {
        // Save a REVOKED consent (active = false)
        Consent c = new Consent(null,
            "did:veristas:patient:abc",
            "did:veristas:hospital:xyz",
            "emergency_access", false, "sig"); // ← active=false
        consentRepo.save(c);

        Optional<Consent> found = consentRepo
            .findByPatientDidAndDelegateDidAndActiveTrue(
                "did:veristas:patient:abc",
                "did:veristas:hospital:xyz");

        // Must NOT find it — the AndActiveTrue filters it out
        assertFalse(found.isPresent());
    }

    @Test
    void findAllByPatientDid_returnsAllConsents_activeAndRevoked() {
        String patientDid = "did:veristas:patient:history";
        // Save 1 active + 1 revoked consent for same patient
        consentRepo.save(new Consent(null, patientDid, "did:h:A", "ref", true,  "s1"));
        consentRepo.save(new Consent(null, patientDid, "did:h:B", "emg", false, "s2"));

        List<Consent> all = consentRepo.findAllByPatientDid(patientDid);
        // Returns BOTH — no active filter here
        assertEquals(2, all.size());
    }

    // ── EmergencySessionRepository ─────────────────────────────────────────

    @Test
    void findById_withStringPK_works() {
        sessionRepo.save(new EmergencySessionEntity(
            "ES-001", "PAT-001", "dispatched", System.currentTimeMillis()));

        Optional<EmergencySessionEntity> found = sessionRepo.findById("ES-001");
        assertTrue(found.isPresent());
        assertEquals("dispatched", found.get().getStage());
    }

    @Test
    void findFirstByPatientId_returnsLatestSession() {
        long now = System.currentTimeMillis();
        // Two sessions for same patient — different timestamps
        sessionRepo.save(new EmergencySessionEntity("ES-OLD", "PAT-X", "completed", now - 10000));
        sessionRepo.save(new EmergencySessionEntity("ES-NEW", "PAT-X", "dispatched", now));

        Optional<EmergencySessionEntity> found = sessionRepo.findFirstByPatientId("PAT-X");
        assertTrue(found.isPresent());
        // ORDER BY createdAt DESC → should return the newest session
        assertEquals("ES-NEW", found.get().getEsid());
    }

    @Test
    void findFirstByPatientId_notFound_returnsEmpty() {
        Optional<EmergencySessionEntity> found = sessionRepo.findFirstByPatientId("UNKNOWN-PAT");
        assertFalse(found.isPresent());
    }

    // ── VCRepository ───────────────────────────────────────────────────────

    @Test
    void findFirstBySubjectDidAndRevokedFalse_findsActiveVC() {
        VCEntity vc = new VCEntity();
        vc.setSubjectDid("did:veristas:patient:abc");
        vc.setProof("eyJhbGci.test.sig");
        vc.setRevoked(false);
        vcRepo.save(vc);

        Optional<VCEntity> found = vcRepo
            .findFirstBySubjectDidAndRevokedFalse("did:veristas:patient:abc");

        assertTrue(found.isPresent());
        assertFalse(found.get().isRevoked());
    }

    @Test
    void findFirstBySubjectDidAndRevokedFalse_ignoresRevokedVC() {
        VCEntity vc = new VCEntity();
        vc.setSubjectDid("did:veristas:patient:xyz");
        vc.setProof("eyJhbGci.test.sig2");
        vc.setRevoked(true); // ← revoked
        vcRepo.save(vc);

        Optional<VCEntity> found = vcRepo
            .findFirstBySubjectDidAndRevokedFalse("did:veristas:patient:xyz");

        // Must not find it — the AndRevokedFalse filters it out
        assertFalse(found.isPresent());
    }

    @Test
    void findFirstByAbhaIdAndRevokedFalse_preventsDoubleRegistration() {
        // First patient registered with this ABHA ID
        VCEntity vc = new VCEntity();
        vc.setSubjectDid("did:veristas:patient:first");
        vc.setAbhaId("12-3456-7890-0001");
        vc.setProof("proof1");
        vc.setRevoked(false);
        vcRepo.save(vc);

        // Check: does this ABHA ID already have an active VC?
        Optional<VCEntity> existing = vcRepo
            .findFirstByAbhaIdAndRevokedFalse("12-3456-7890-0001");

        // YES — block second registration
        assertTrue(existing.isPresent());
        assertEquals("did:veristas:patient:first", existing.get().getSubjectDid());
    }
}
