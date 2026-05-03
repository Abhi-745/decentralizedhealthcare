package com.example.VeristasId.Controller;

import com.example.VeristasId.Dto.AuditLogBlock;
import com.example.VeristasId.Service.BlockchainAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final BlockchainAuditService auditService;

    public AuditController(BlockchainAuditService auditService) {
        this.auditService = auditService;
    }

    // Endpoint 1: View the entire blockchain ledger
    @GetMapping("/ledger")
    public ResponseEntity<List<AuditLogBlock>> getLedger() {
        return ResponseEntity.ok(auditService.getFullLedger());
    }

    // Endpoint 2: Prove mathematical integrity to evaluators
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyChain() {
        boolean isValid = auditService.isChainValid();

        Map<String, Object> response = Map.of(
                "isChainMathematicallyValid", isValid,
                "totalBlocks", auditService.getFullLedger().size(),
                "statusMessage", isValid
                        ? "Chain is secure and untampered."
                        : "WARNING: Chain corruption detected!"
        );

        return ResponseEntity.ok(response);
    }
}