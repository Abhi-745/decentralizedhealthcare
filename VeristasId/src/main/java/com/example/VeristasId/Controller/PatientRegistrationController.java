package com.example.VeristasId.Controller;

import com.example.VeristasId.Dto.RegistrationRequest;
import com.example.VeristasId.Dto.VerifiableCredential;
import com.example.VeristasId.Service.CredentialIssuanceService;
import com.example.VeristasId.Service.PatientWalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
public class PatientRegistrationController {

    private final CredentialIssuanceService issuanceService;
    private final PatientWalletService walletService;

    // Constructor Injection
    public PatientRegistrationController(CredentialIssuanceService issuanceService, PatientWalletService walletService) {
        this.issuanceService = issuanceService;
        this.walletService = walletService;
    }

    @PostMapping("/register")
    public ResponseEntity<VerifiableCredential> registerPatient(@RequestBody RegistrationRequest request) {
        try {
            // 1. Hospital generates and signs the Verifiable Credential
            VerifiableCredential issuedVc = issuanceService.issueCredential(request);

            // 2. Send the VC back to the Patient's phone (Simulated)
            walletService.storeVerifiableCredential(issuedVc.toString());

            // 3. Return a 200 OK with the full JSON payload
            return ResponseEntity.ok(issuedVc);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // A helper endpoint so you can test it easily without a request body
    @GetMapping("/auto-register-demo")
    public ResponseEntity<VerifiableCredential> autoRegisterDemoPatient() {
        // Automatically grab the dummy data from the wallet and register them
        RegistrationRequest req = new RegistrationRequest("99-9999-9999-9999", walletService.getPatientDid());
        return registerPatient(req);
    }
}