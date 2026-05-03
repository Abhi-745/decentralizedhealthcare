// Global State
let patientDid = "";
let patientJwt = "";
let paramedicJwt = "";

// DOM Elements
const btnRegister = document.getElementById('btn-register');
const walletStatus = document.getElementById('wallet-status');
const didDisplay = document.getElementById('did-display');
const didText = document.getElementById('did-text');
const consentSection = document.getElementById('consent-section');
const btnGrantConsent = document.getElementById('btn-grant-consent');
const btnFetchEmr = document.getElementById('btn-fetch-emr');
const inputPatientDid = document.getElementById('patient-did-input');
const emrResult = document.getElementById('emr-result');
const emrData = document.getElementById('emr-data');
const btnTrigger911 = document.getElementById('btn-trigger-911');

// Initialization: Fetch Paramedic Token on load
window.addEventListener('DOMContentLoaded', async () => {
    try {
        const response = await fetch('/api/auth/login-paramedic', { method: 'POST' });
        const data = await response.json();
        paramedicJwt = data.token;
        console.log("Hospital Portal Online. Paramedic Token fetched.");
    } catch (err) {
        console.error("Failed to load Paramedic Auth Token");
    }
});

// 1. Patient Auto-Registration
btnRegister.addEventListener('click', async () => {
    btnRegister.innerText = "Generating DID...";
    try {
        const response = await fetch('/api/patients/auto-register-demo');
        const vc = await response.json();
        
        patientDid = vc.subjectDid;
        patientJwt = "Bearer " + vc.proof.jwt;

        walletStatus.className = "status-badge status-online";
        walletStatus.innerText = "Active";
        
        didText.innerText = patientDid;
        inputPatientDid.value = patientDid; // Auto-fill the hospital side for demo purposes
        
        didDisplay.classList.remove('hidden');
        consentSection.classList.remove('hidden');
        btnRegister.classList.add('hidden');
        
    } catch (err) {
        alert("Failed to generate Digital ID");
        btnRegister.innerText = "Try Again";
    }
});

// 2. Patient Grants Consent
btnGrantConsent.addEventListener('click', async () => {
    btnGrantConsent.innerText = "Securing Consent on Ledger...";
    try {
        const payload = {
            delegateDid: "EMT-9110",
            purpose: "Emergency Medical Response"
        };
        
        const response = await fetch('/api/consent/grant', {
            method: 'POST',
            headers: {
                'Authorization': patientJwt,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            btnGrantConsent.innerText = "✅ Authorized";
            btnGrantConsent.classList.remove('btn-success');
            btnGrantConsent.classList.add('btn-secondary');
            btnGrantConsent.disabled = true;
        } else {
            throw new Error("Consent failed");
        }
    } catch (err) {
        alert("Failed to record cryptographic consent.");
        btnGrantConsent.innerText = "Approve";
    }
});

// 3. Hospital Fetches EMR
btnFetchEmr.addEventListener('click', async () => {
    const targetDid = inputPatientDid.value.trim();
    if (!targetDid) {
        alert("Please enter a Patient DID");
        return;
    }

    btnFetchEmr.innerText = "Querying...";
    try {
        const response = await fetch(`/api/emr/${targetDid}`, {
            headers: {
                'Authorization': paramedicJwt
            }
        });

        if (response.status === 403 || response.status === 401) {
            alert("ACCESS DENIED: Zero-Trust Engine rejected the request. Did you grant consent?");
            btnFetchEmr.innerText = "Fetch EMR";
            return;
        }

        const data = await response.json();
        
        emrData.innerHTML = '';
        Object.keys(data).forEach(key => {
            if (data[key] && key !== 'id') {
                const row = document.createElement('div');
                row.className = 'emr-row';
                row.innerHTML = `
                    <div class="emr-label">${key.charAt(0).toUpperCase() + key.slice(1)}</div>
                    <div class="emr-value">${data[key]}</div>
                `;
                emrData.appendChild(row);
            }
        });

        emrResult.classList.remove('hidden');
    } catch (err) {
        console.error(err);
        alert("Error fetching medical record.");
    } finally {
        btnFetchEmr.innerText = "Fetch EMR";
    }
});

// 4. Trigger 911 Dispatch
btnTrigger911.addEventListener('click', async () => {
    // This is just a visual demo for the Break-Glass override on the UI
    const targetDid = inputPatientDid.value.trim() || "99-9999-9999-9999";
    
    // In a real app we'd hit /api/emergency/dispatch here. 
    // Since we already hardcoded "99-9999-9999-9999" into the EmergencySimulationRunner,
    // we can just fetch it directly to show the override working!
    
    try {
        btnTrigger911.innerText = "Bypassing Consent...";
        
        const response = await fetch(`/api/medical-records/99-9999-9999-9999`, {
            headers: {
                'Authorization': paramedicJwt
            }
        });

        const data = await response.json();
        
        emrData.innerHTML = `
            <div class="emr-row"><div class="emr-label">Alert</div><div class="emr-value" style="color:#ef4444;font-weight:bold;">BREAK-GLASS ACTIVATED</div></div>
            <div class="emr-row"><div class="emr-label">Name</div><div class="emr-value">${data.patientName || "Unknown"}</div></div>
            <div class="emr-row"><div class="emr-label">Blood Type</div><div class="emr-value">${data.bloodGroup || "Unknown"}</div></div>
            <div class="emr-row"><div class="emr-label">Allergies</div><div class="emr-value">${data.allergies || "Unknown"}</div></div>
        `;
        
        emrResult.classList.remove('hidden');
    } catch (err) {
        alert("Failed to trigger emergency override.");
    } finally {
        btnTrigger911.innerText = "Trigger 911 Dispatch Session";
    }
});
