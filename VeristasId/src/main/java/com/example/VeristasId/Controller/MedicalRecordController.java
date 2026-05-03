package com.example.VeristasId.Controller;

import com.example.VeristasId.Model.MedicalRecord;
import com.example.VeristasId.Repository.MedicalRecordRepository;
import com.example.VeristasId.Service.BlockchainAuditService;
import com.example.VeristasId.Service.OpaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final OpaService opaService;
    private final BlockchainAuditService auditService;
    private final MedicalRecordRepository medicalRecordRepository;

    public MedicalRecordController(OpaService opaService,
                                   BlockchainAuditService auditService,
                                   MedicalRecordRepository medicalRecordRepository) {
        this.opaService = opaService;
        this.auditService = auditService;
        this.medicalRecordRepository = medicalRecordRepository;

        // Pre-load the standard demo patient into DB if not already there
        if (medicalRecordRepository.findByAbhaId("99-9999-9999-9999").isEmpty()) {
            MedicalRecord standardPatient = new MedicalRecord();
            standardPatient.setAbhaId("99-9999-9999-9999");
            standardPatient.setPatientName("John Doe");
            standardPatient.setBloodGroup("O-Negative");
            standardPatient.setAllergies("Penicillin, Latex");
            standardPatient.setDiagnosis("No records yet");
            medicalRecordRepository.save(standardPatient);
            System.out.println("✅ [DB] Standard demo patient saved to PostgreSQL.");
        } else {
            System.out.println("✅ [DB] Standard demo patient already exists in PostgreSQL.");
        }
    }

    // Create a Provisional "John Doe" Identity for unidentified patients
    @PostMapping("/provisional")
    public ResponseEntity<Map<String, String>> createProvisionalIdentity() {
        String tempId = "TEMP-JD-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        System.out.println("\n[SYSTEM] 🚨 Unidentified Patient Alert! Generating Provisional Identity: " + tempId);

        MedicalRecord provisional = new MedicalRecord();
        provisional.setAbhaId(tempId);
        provisional.setPatientName("UNIDENTIFIED (JOHN DOE)");
        provisional.setBloodGroup("UNKNOWN - ADMINISTER O-NEGATIVE ONLY");
        provisional.setAllergies("UNKNOWN - EXERCISE CAUTION");
        provisional.setStatus("PENDING_BIOMETRIC_MERGE");
        medicalRecordRepository.save(provisional);

        Map<String, String> response = new HashMap<>();
        response.put("abhaId", tempId);
        response.put("patientName", provisional.getPatientName());
        response.put("bloodGroup", provisional.getBloodGroup());
        response.put("allergies", provisional.getAllergies());
        response.put("status", provisional.getStatus());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{abhaId}")
    public ResponseEntity<?> getPatientRecord(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String abhaId) {

        System.out.println("\n[GATEWAY] Intercepted request for ID: " + abhaId);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("[GATEWAY] BLOCKED ⛔ - No Authorization Header.");
            auditService.recordAccessAttempt("Anonymous", abhaId, "READ", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Missing JWT");
        }

        System.out.println("[GATEWAY] Consulting Zero-Trust Engine (OPA)...");
        boolean isPermitted = opaService.checkAccess(authHeader, abhaId, "read");

        if (!isPermitted) {
            System.out.println("[GATEWAY] DENIED 🛑 - Emergency Bypass conditions not met.");
            auditService.recordAccessAttempt("Paramedic_JWT", abhaId, "READ", false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access Denied: No active emergency dispatch detected for this ID.");
        }

        System.out.println("[GATEWAY] APPROVED ✅ - Zero-Trust Break-Glass Activated!");
        auditService.recordAccessAttempt("Paramedic_JWT", abhaId, "READ", true);

        return medicalRecordRepository.findByAbhaId(abhaId)
                .map(record -> {
                    // Return as a simple map for the frontend
                    Map<String, String> resp = new HashMap<>();
                    resp.put("abhaId", record.getAbhaId());
                    resp.put("patientName", record.getPatientName());
                    resp.put("bloodGroup", record.getBloodGroup());
                    resp.put("allergies", record.getAllergies());
                    if (record.getDiagnosis() != null) resp.put("diagnosis", record.getDiagnosis());
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    // Called by DataInjector to seed patients — now saves to DB instead of HashMap
    public void addPatientRecord(String abhaId, Map<String, String> record) {
        if (medicalRecordRepository.findByAbhaId(abhaId).isEmpty()) {
            MedicalRecord mr = new MedicalRecord();
            mr.setAbhaId(abhaId);
            mr.setPatientName(record.get("patientName"));
            mr.setBloodGroup(record.get("bloodGroup"));
            medicalRecordRepository.save(mr);
        }
    }
}