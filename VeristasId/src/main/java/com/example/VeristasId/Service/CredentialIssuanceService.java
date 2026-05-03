package com.example.VeristasId.Service;

import com.example.VeristasId.Dto.CredentialRequest;
import com.example.VeristasId.Dto.RegistrationRequest;
import com.example.VeristasId.Dto.VerifiableCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class CredentialIssuanceService {

    @Autowired
    private CredentialService credentialService;

    // The Hospital's Master Keys
    private PrivateKey hospitalPrivateKey;
    private PublicKey hospitalPublicKey;
    private String hospitalDid;

    @PostConstruct
    public void initHospitalAuthority() {
        try {
            // 1. Generate the Hospital's Master ECDSA Keys
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            keyGen.initialize(256, new SecureRandom()); // Standard 256-bit curve
            KeyPair keyPair = keyGen.generateKeyPair();

            this.hospitalPrivateKey = keyPair.getPrivate();
            this.hospitalPublicKey = keyPair.getPublic();

            // 2. Create the Hospital's official DID
            String pubKeyBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(this.hospitalPublicKey.getEncoded());
            this.hospitalDid = "did:veristas:hospital:" + pubKeyBase64.substring(0, 16);

            System.out.println("🏥 [HOSPITAL] Master Cryptographic Authority Online.");
            System.out.println("📜 [HOSPITAL] Issuer DID: " + this.hospitalDid);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Hospital Master Keys", e);
        }
    }

    // The core method that builds and signs the digital ID card
    public VerifiableCredential issueCredential(RegistrationRequest request) throws Exception {

        System.out.println("⚙️ [HOSPITAL] Processing VC for ABHA: " + request.getAbhaId());

        VerifiableCredential vc = new VerifiableCredential();

        // 1. Set the W3C Standard Context and Types
        vc.setContext(Arrays.asList("https://www.w3.org/2018/credentials/v1"));
        vc.setId("http://veristasid.hospital.com/credentials/" + UUID.randomUUID().toString());
        vc.setType(Arrays.asList("VerifiableCredential", "PatientAbhaCredential"));

        // 2. Set Issuer and Date
        vc.setIssuer(this.hospitalDid);
        vc.setIssuanceDate(Instant.now().toString());

        // 3. Attach the Patient's Data
        vc.setCredentialSubject(new VerifiableCredential.CredentialSubject(request.getDid(), request.getAbhaId()));

        // 4. MATHEMATICALLY SIGN THE CREDENTIAL
        // Use the centralized CredentialService to generate a valid JWT and save it to the DB ledger
        CredentialRequest credReq = new CredentialRequest();
        credReq.setSubjectDid(request.getDid());
        credReq.setCredentialType("PatientAbhaCredential");
        credReq.setClaims(Map.of("sub", request.getDid(), "role", "patient", "abhaId", request.getAbhaId()));

        String jwtToken = credentialService.issueCredential(credReq);

        // 5. Attach the Signature to the Credential Proof
        vc.setProof(new VerifiableCredential.Proof("JwtProof2020", jwtToken));

        System.out.println("✅ [HOSPITAL] Credential successfully issued and signed.");
        return vc;
    }
}