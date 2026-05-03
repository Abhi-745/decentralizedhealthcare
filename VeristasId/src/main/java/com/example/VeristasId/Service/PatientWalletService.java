package com.example.VeristasId.Service;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

@Service
public class PatientWalletService {

    // The keys stay private to this service (Simulating the Secure Enclave on a phone)
    private PrivateKey patientPrivateKey;
    private PublicKey patientPublicKey;

    // The public DID that the hospital will see
    private String patientDid;

    // Memory slot to store the VC once the hospital issues it
    private String storedCredential;

    @PostConstruct
    public void initWallet() {
        // 1. Add Bouncy Castle as our Cryptography Provider
        Security.addProvider(new BouncyCastleProvider());

        try {
            java.nio.file.Path privPath = java.nio.file.Paths.get("wallet_private.key");
            java.nio.file.Path pubPath = java.nio.file.Paths.get("wallet_public.key");

            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");

            if (java.nio.file.Files.exists(privPath) && java.nio.file.Files.exists(pubPath)) {
                // Load existing keys from disk
                byte[] privBytes = java.nio.file.Files.readAllBytes(privPath);
                byte[] pubBytes = java.nio.file.Files.readAllBytes(pubPath);

                this.patientPrivateKey = keyFactory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(privBytes));
                this.patientPublicKey = keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(pubBytes));
                System.out.println("\n📱 [WALLET] Loaded existing Patient Wallet from disk!");
            } else {
                // 2. Generate a new ECDSA secp256k1 Key Pair (The Web3/Blockchain Standard)
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
                ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
                keyGen.initialize(ecSpec, new SecureRandom());

                KeyPair keyPair = keyGen.generateKeyPair();
                this.patientPrivateKey = keyPair.getPrivate();
                this.patientPublicKey = keyPair.getPublic();

                // Save to disk for persistence across restarts
                java.nio.file.Files.write(privPath, this.patientPrivateKey.getEncoded());
                java.nio.file.Files.write(pubPath, this.patientPublicKey.getEncoded());
                System.out.println("\n📱 [WALLET] Simulated Patient Phone Initialized (New Wallet Created)!");
            }

            // 3. Create the DID from the Public Key
            String pubKeyBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(this.patientPublicKey.getEncoded());
            this.patientDid = "did:veristas:patient:" + pubKeyBase64.substring(0, 16);

            System.out.println("🔑 [WALLET] DID Generated: " + this.patientDid);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate patient wallet keys", e);
        }
    }

    // --- GETTERS ---
    // The hospital only gets to see the DID, NEVER the Private Key!

    public String getPatientDid() {
        return patientDid;
    }

    public PublicKey getPatientPublicKey() {
        return patientPublicKey;
    }

    // --- CREDENTIAL STORAGE ---
    // This is called later when the hospital returns the final signed ID card

    public void storeVerifiableCredential(String credentialJson) {
        this.storedCredential = credentialJson;
        System.out.println("💾 [WALLET] Verifiable Credential securely saved to device memory!");
    }

    public String getStoredCredential() {
        return storedCredential;
    }
}