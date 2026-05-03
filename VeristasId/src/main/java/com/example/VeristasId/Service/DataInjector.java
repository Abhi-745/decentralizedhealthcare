package com.example.VeristasId.Service;

import com.example.VeristasId.Controller.MedicalRecordController;
import com.example.VeristasId.Repository.MedicalRecordRepository;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class DataInjector implements CommandLineRunner {

    private final MedicalRecordController recordController;
    private final JwtService jwtService;
    private final MedicalRecordRepository medicalRecordRepository;

    public DataInjector(MedicalRecordController recordController, JwtService jwtService,
                        MedicalRecordRepository medicalRecordRepository) {
        this.recordController = recordController;
        this.jwtService = jwtService;
        this.medicalRecordRepository = medicalRecordRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n=======================================================");
        System.out.println("🏥 [SYSTEM] INITIALIZING VERISTAS-ID HOSPITAL DATABASE");
        System.out.println("=======================================================");

        // --- Check if seed data already exists (persistence check) ---
        long existingRecords = medicalRecordRepository.count();
        if (existingRecords > 1) {
            // More than just the John Doe demo patient — data already seeded
            System.out.println("\n✅ [DB] Found " + existingRecords + " existing records in PostgreSQL. Skipping seed generation.");
        } else {
            // First run — generate seed data
            Faker faker = new Faker(new Locale("en-IN"));
            System.out.println("\n[PATIENT ROSTER GENERATED]");
            for (int i = 1; i <= 3; i++) {
                String abhaId = String.format("%02d-%04d-%04d-%04d",
                        faker.number().numberBetween(10, 99), faker.number().numberBetween(1000, 9999),
                        faker.number().numberBetween(1000, 9999), faker.number().numberBetween(1000, 9999));
                Map<String, String> patient = new HashMap<>();
                patient.put("abhaId", abhaId);
                patient.put("patientName", faker.name().fullName());
                patient.put("bloodGroup", faker.bloodtype().bloodGroup());

                recordController.addPatientRecord(abhaId, patient);
                System.out.println("👤 Patient " + i + ": " + patient.get("patientName") + " | ABHA: " + abhaId);
            }
        }

        // --- Staff tokens are always printed (useful for Postman) ---
        System.out.println("\n[STAFF ROSTER & SECURITY TOKENS GENERATED]");

        String paramedicName = "Bobbi D'Amore";
        String paramedicBadge = "EMT-9110";
        String paramedicToken = jwtService.generateToken(paramedicName, "paramedic", paramedicBadge);
        System.out.println("🚑 Paramedic: " + paramedicName + " (" + paramedicBadge + ")");
        System.out.println("🔑 JWT: Bearer " + paramedicToken + "\n");

        String surgeonName = "Dr. Fisher";
        String surgeonBadge = "SURG-1000";
        String surgeonToken = jwtService.generateToken(surgeonName, "surgeon", surgeonBadge);
        System.out.println("🩺 Surgeon: " + surgeonName + " (" + surgeonBadge + ")");
        System.out.println("🔑 JWT: Bearer " + surgeonToken);

        System.out.println("=======================================================\n");
    }
}