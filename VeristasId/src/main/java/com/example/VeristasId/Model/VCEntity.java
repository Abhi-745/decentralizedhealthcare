package com.example.VeristasId.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "verifiable_credentials") // Maps to a table in PostgreSQL
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VCEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String subjectDid; // The 'owner' of the credential

    @Column(columnDefinition = "TEXT", nullable = false)
    private String proof; // The full JWT string (TEXT type allows for long strings)

    @Column(nullable = false)
    private boolean revoked = false; // The Status List flag (false = Valid, true = Revoked)

    private LocalDateTime issuedAt;

    @PrePersist
    protected void onCreate() {
        this.issuedAt = LocalDateTime.now();
    }
}