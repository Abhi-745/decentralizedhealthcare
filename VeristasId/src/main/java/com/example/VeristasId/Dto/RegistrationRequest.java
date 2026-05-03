package com.example.VeristasId.Dto;

public class RegistrationRequest {

    private String abhaId;
    private String did;

    // Constructors
    public RegistrationRequest() {}

    public RegistrationRequest(String abhaId, String did) {
        this.abhaId = abhaId;
        this.did = did;
    }

    // Getters and Setters
    public String getAbhaId() { return abhaId; }
    public void setAbhaId(String abhaId) { this.abhaId = abhaId; }

    public String getDid() { return did; }
    public void setDid(String did) { this.did = did; }
}