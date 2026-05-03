package com.example.VeristasId.Service;

import com.example.VeristasId.Model.EmergencySessionEntity;
import com.example.VeristasId.Repository.EmergencySessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EmergencySimulationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EmergencySimulationRunner.class);

    private final EmergencySessionRepository sessionRepo;
    private final OpaService opaService;

    // Best Practice: Constructor injection instead of @Autowired on fields
    public EmergencySimulationRunner(EmergencySessionRepository sessionRepo, OpaService opaService) {
        this.sessionRepo = sessionRepo;
        this.opaService = opaService;
    }

    @Override
    public void run(String... args) throws Exception {
        String testEsid = "ES-999";
        String demoPatientId = "99-9999-9999-9999";

        log.info("--- Starting Emergency Access Simulation ---");

        // FIX: Only seed ES-999 if it does NOT already exist in DB
        // This prevents overwriting the stage you manually set via Postman
        if (sessionRepo.findById(testEsid).isEmpty()) {
            sessionRepo.save(new EmergencySessionEntity(testEsid, "PAT-001", "dispatched"));
            log.info("ES-999 seeded fresh. Stage: DISPATCHED");
        } else {
            log.info("ES-999 already exists. Stage preserved from DB (no override).");
        }

        // FIX: Only seed the live break-glass session if it doesn't already exist
        if (sessionRepo.findById(demoPatientId).isEmpty()) {
            sessionRepo.save(new EmergencySessionEntity(demoPatientId, demoPatientId, "dispatched"));
            log.info("Live break-glass session seeded. Stage: DISPATCHED");
        } else {
            log.info("Live break-glass session already exists. Stage preserved from DB.");
        }

        // Run simulation test using mock JWTs
        String mockParamedic = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoicGFyYW1lZGljIn0.1234";
        boolean canRead = opaService.checkAccess(mockParamedic, testEsid, "read");
        log.info("Paramedic Access Result: {}", canRead ? "PERMIT" : "DENY");

        String mockSurgeon = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoic3VyZ2VvbiJ9.1234";
        boolean canUpdate = opaService.checkAccess(mockSurgeon, testEsid, "update");
        log.info("Surgeon Update Result: {}", canUpdate ? "PERMIT" : "DENY");

        log.info("--- Simulation Complete ---");
    }
}