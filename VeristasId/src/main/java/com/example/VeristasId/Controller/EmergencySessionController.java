package com.example.VeristasId.Controller;

import com.example.VeristasId.Model.EmergencySessionEntity;
import com.example.VeristasId.Repository.EmergencySessionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/emergency")
public class EmergencySessionController {

    private final EmergencySessionRepository sessionRepository;

    public EmergencySessionController(EmergencySessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    // Endpoint to CREATE a brand new emergency session for any patient
    @PostMapping("/create")
    public ResponseEntity<?> createEmergency(@RequestBody Map<String, String> request) {
        String esid = request.get("esid");
        String patientId = request.get("patientId");

        if (sessionRepository.findById(esid).isPresent()) {
            return ResponseEntity.status(409).body("Emergency Session " + esid + " already exists.");
        }

        EmergencySessionEntity newSession = new EmergencySessionEntity(esid, patientId, "dispatched");
        sessionRepository.save(newSession);
        return ResponseEntity.ok("Emergency Session " + esid + " created for patient " + patientId + ". Stage: DISPATCHED");
    }

    // Endpoint to manually change the status of an emergency
    // Use stage="DISPATCHED" for Paramedic access
    // Use stage="ARRIVED" for Surgeon access
    @PostMapping("/update-stage")
    public ResponseEntity<?> updateStage(@RequestBody Map<String, String> request) {
        String esid = request.get("esid");
        String newStage = request.get("stage").toLowerCase(); // always normalize to lowercase

        return sessionRepository.findById(esid)
                .map(session -> {
                    session.setStage(newStage);
                    sessionRepository.save(session);
                    return ResponseEntity.ok("Emergency Session " + esid + " moved to stage: " + newStage);
                })
                .orElse(ResponseEntity.status(404).body("Emergency Session not found."));
    }

    // Endpoint to check current stage of any emergency session
    @GetMapping("/{esid}")
    public ResponseEntity<?> getSession(@PathVariable String esid) {
        return sessionRepository.findById(esid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(null));
    }
}
