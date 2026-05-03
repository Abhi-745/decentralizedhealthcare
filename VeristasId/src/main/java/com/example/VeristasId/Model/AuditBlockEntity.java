package com.example.VeristasId.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_blockchain")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditBlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int blockIndex;
    private long timestamp;
    private String accessorId;
    private String targetAbhaId;
    private String action;
    private boolean accessGranted;
    private String previousHash;

    @Column(length = 64)
    private String hash;
}
