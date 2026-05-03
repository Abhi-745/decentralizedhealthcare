package com.example.VeristasId.Controller;

import com.example.VeristasId.Dto.EMRUpdateRequest;
import com.example.VeristasId.Model.MedicalRecord;
import com.example.VeristasId.Repository.ConsentRepository;
import com.example.VeristasId.Repository.MedicalRecordRepository;
import com.example.VeristasId.Service.BlockchainAuditService;
import com.example.VeristasId.Service.CredentialService;
import com.example.VeristasId.Service.JwtService;
import com.example.VeristasId.Service.OpaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/emr")
public class EMRController {

    private final CredentialService credentialService;
    private final JwtService jwtService;
    private final OpaService opaService;
    private final ConsentRepository consentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final BlockchainAuditService blockchainAuditService;

    public EMRController(CredentialService credentialService,
                         JwtService jwtService,
                         OpaService opaService,
                         ConsentRepository consentRepository,
                         MedicalRecordRepository medicalRecordRepository,
                         BlockchainAuditService blockchainAuditService) {
        this.credentialService = credentialService;
        this.jwtService = jwtService;
        this.opaService = opaService;
        this.consentRepository = consentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.blockchainAuditService = blockchainAuditService;
    }

    @PutMapping("/{patientDid}")
    public ResponseEntity<?> updateEMR(
            @PathVariable String patientDid,
            @RequestHeader("Authorization") String token,
            @RequestBody EMRUpdateRequest request) {

        // 1. Verify the caller's VC/JWT is valid
        boolean isValidPatient = credentialService.verifyVC(token);
        boolean isValidStaff = jwtService.verifyStaffToken(token);

        if (!isValidPatient && !isValidStaff) {
            return ResponseEntity.status(401).body("Invalid or revoked credential");
        }

        // 2. Extract who is making this call
        Map<String, Object> claims = credentialService.extractClaims(token);
        String callerDid = (String) claims.getOrDefault("sub", "unknown");
        String callerRole = (String) claims.getOrDefault("role", "unknown");

        // 3. OPA check — Does the Zero-Trust Engine permit this as an Emergency Override?
        boolean isEmergencyOverride = opaService.checkAccess(token, request.getEsid(), "update");

        // 4. Consent check — Has the patient explicitly allowed this caller?
        boolean hasConsent = consentRepository
                .findByPatientDidAndDelegateDidAndActiveTrue(patientDid, callerDid)
                .isPresent();

        // 5. Final Access Decision: Must have EITHER an Emergency Override OR Active Consent
        if (!isEmergencyOverride && !hasConsent) {
            blockchainAuditService.recordAccessAttempt(callerDid, patientDid, "EMR_UPDATE_DENIED", false);
            return ResponseEntity.status(403).body("OPA denied: role or session stage does not permit update, and no active consent found.");
        }

        // 5. Write or update the EMR record
        MedicalRecord record = medicalRecordRepository
                .findByPatientDid(patientDid)
                .orElse(new MedicalRecord());

        record.setPatientDid(patientDid);
        record.setUpdatedByDid(callerDid);
        record.setEsid(request.getEsid());
        record.setDiagnosis(request.getDiagnosis());
        record.setVitals(request.getVitals());
        record.setPrescription(request.getPrescription());

        medicalRecordRepository.save(record);

        // 6. Final audit entry — successful write
        blockchainAuditService.recordAccessAttempt(callerDid, patientDid, "EMR_WRITTEN", true);

        return ResponseEntity.ok("EMR updated successfully for patient: " + patientDid);
    }

    // Read endpoint for the patient to view their own record
    @GetMapping("/{patientDid}")
    public ResponseEntity<?> getEMR(
            @PathVariable String patientDid,
            @RequestHeader("Authorization") String token) {

        boolean isValidPatient = credentialService.verifyVC(token);
        boolean isValidStaff = jwtService.verifyStaffToken(token);

        if (!isValidPatient && !isValidStaff) {
            return ResponseEntity.status(401).body("Invalid or revoked credential");
        }

        Map<String, Object> claims = credentialService.extractClaims(token);
        String callerDid = (String) claims.getOrDefault("sub", "unknown");

        // Only the patient themselves or a consented delegate can read
        boolean isSelf = patientDid.equals(callerDid);
        boolean hasConsent = consentRepository
                .findByPatientDidAndDelegateDidAndActiveTrue(patientDid, callerDid)
                .isPresent();

        if (!isSelf && !hasConsent) {
            return ResponseEntity.status(403).body("Access denied: no consent granted");
        }

        return medicalRecordRepository.findByPatientDid(patientDid)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    // For demo purposes, return a blank file instead of a 404 Error
                    MedicalRecord blank = new MedicalRecord();
                    blank.setPatientDid(patientDid);
                    blank.setDiagnosis("No records found (New Patient)");
                    return ResponseEntity.ok(blank);
                });
    }

    // Delete endpoint for Right to be Forgotten
    @DeleteMapping("/{patientDid}")
    public ResponseEntity<?> deleteEMR(
            @PathVariable String patientDid,
            @RequestHeader("Authorization") String token) {

        // Only the patient themselves should be able to delete their record
        boolean isValidPatient = credentialService.verifyVC(token);
        if (!isValidPatient) {
            return ResponseEntity.status(401).body("Invalid or revoked patient credential");
        }

        Map<String, Object> claims = credentialService.extractClaims(token);
        String callerDid = (String) claims.getOrDefault("sub", "unknown");

        if (!patientDid.equals(callerDid)) {
            blockchainAuditService.recordAccessAttempt(callerDid, patientDid, "EMR_DELETE_DENIED", false);
            return ResponseEntity.status(403).body("Access denied: You can only delete your own record.");
        }

        return medicalRecordRepository.findByPatientDid(patientDid)
                .map(record -> {
                    medicalRecordRepository.delete(record);
                    blockchainAuditService.recordAccessAttempt(callerDid, patientDid, "EMR_DELETED", true);
                    return ResponseEntity.ok("EMR successfully deleted. Right to be forgotten fulfilled.");
                })
                .orElse(ResponseEntity.status(404).body("No EMR found to delete."));
    }
}