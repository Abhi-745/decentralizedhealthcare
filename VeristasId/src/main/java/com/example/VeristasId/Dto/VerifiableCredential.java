package com.example.VeristasId.Dto;

import java.util.List;

public class VerifiableCredential {

    // The W3C Standard requires these exact field names
    private List<String> context;
    private String id;
    private List<String> type;
    private String issuer;
    private String issuanceDate;
    private CredentialSubject credentialSubject;
    private Proof proof;

    // Constructors
    public VerifiableCredential() {}

    // --- Nested Class: The Patient's Data ---
    public static class CredentialSubject {
        private String id; // The Patient's DID
        private String abhaId;

        public CredentialSubject() {}
        public CredentialSubject(String id, String abhaId) {
            this.id = id;
            this.abhaId = abhaId;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAbhaId() { return abhaId; }
        public void setAbhaId(String abhaId) { this.abhaId = abhaId; }
    }

    // --- Nested Class: The Cryptographic Signature ---
    public static class Proof {
        private String type;
        private String jwt; // The cryptographic signature made by the Hospital

        public Proof() {}
        public Proof(String type, String jwt) {
            this.type = type;
            this.jwt = jwt;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getJwt() { return jwt; }
        public void setJwt(String jwt) { this.jwt = jwt; }
    }

    // --- Getters and Setters for Main Class ---
    public List<String> getContext() { return context; }
    public void setContext(List<String> context) { this.context = context; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<String> getType() { return type; }
    public void setType(List<String> type) { this.type = type; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getIssuanceDate() { return issuanceDate; }
    public void setIssuanceDate(String issuanceDate) { this.issuanceDate = issuanceDate; }

    public CredentialSubject getCredentialSubject() { return credentialSubject; }
    public void setCredentialSubject(CredentialSubject credentialSubject) { this.credentialSubject = credentialSubject; }

    public Proof getProof() { return proof; }
    public void setProof(Proof proof) { this.proof = proof; }
}