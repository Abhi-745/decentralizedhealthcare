package com.example.VeristasId.Service;

import com.example.VeristasId.Blockchain.VeristasAudit;
import com.example.VeristasId.Dto.AuditLogBlock;
import com.example.VeristasId.Model.AuditBlockEntity;
import com.example.VeristasId.Repository.AuditBlockRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockchainAuditService
 *
 * Three-layer immutable audit system (all three fire for every access event):
 *
 *   Layer 1 — In-memory SHA-256 linked chain (instant read performance)
 *   Layer 2 — PostgreSQL persistence     (survives restarts, queryable)
 *   Layer 3 — Ethereum smart contract    (tamper-proof, on-chain via VeristasAudit.sol)
 *
 * Layer 3 requires Ganache/testnet running and BLOCKCHAIN_WALLET_PRIVATE_KEY set.
 * If unavailable, Layers 1 & 2 operate independently — no data loss.
 */
@Service
public class BlockchainAuditService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainAuditService.class);

    // ── Layer 1: In-memory chain (rebuilt from DB on startup) ─────────────────
    private final List<AuditLogBlock> blockchain = new ArrayList<>();

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final AuditBlockRepository      auditBlockRepository;
    private final ContractLifecycleService  contractService;

    public BlockchainAuditService(AuditBlockRepository auditBlockRepository,
                                  ContractLifecycleService contractService) {
        this.auditBlockRepository = auditBlockRepository;
        this.contractService      = contractService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Startup — rebuild in-memory chain from PostgreSQL
    // ─────────────────────────────────────────────────────────────────────────

    @PostConstruct
    public void initGenesisBlock() {
        List<AuditBlockEntity> savedBlocks = auditBlockRepository.findAll();

        if (!savedBlocks.isEmpty()) {
            // Rebuild Layer 1 from Layer 2 (PostgreSQL)
            savedBlocks.stream()
                    .sorted((a, b) -> Integer.compare(a.getBlockIndex(), b.getBlockIndex()))
                    .forEach(entity -> {
                        AuditLogBlock block = new AuditLogBlock(
                                entity.getBlockIndex(),
                                entity.getTimestamp(),
                                entity.getAccessorId(),
                                entity.getTargetAbhaId(),
                                entity.getAction(),
                                entity.isAccessGranted(),
                                entity.getPreviousHash(),
                                entity.getHash()
                        );
                        blockchain.add(block);
                    });

            log.info("⛓️  [AUDIT] Reloaded {} blocks from PostgreSQL. In-memory chain restored.", blockchain.size());
        } else {
            // Very first startup — create Genesis Block
            AuditLogBlock genesisBlock = new AuditLogBlock(0, "SYSTEM", "NONE", "GENESIS", true, "0");
            blockchain.add(genesisBlock);
            persistBlockToDB(genesisBlock);
            log.info("⛓️  [AUDIT] Genesis Block created. Immutable audit log is online.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Primary Write — called by all controllers on every access decision
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Records an access attempt across all three audit layers.
     *
     * @param accessorId   Identity of the requester (JWT role or DID)
     * @param targetAbhaId Patient ABHA ID being accessed
     * @param action       "READ" | "WRITE" | "UPDATE"
     * @param accessGranted true if OPA permitted access
     */
    public void recordAccessAttempt(String accessorId,
                                    String targetAbhaId,
                                    String action,
                                    boolean accessGranted) {

        // ── Layer 1 & 2: In-memory + PostgreSQL ──────────────────────────────
        AuditLogBlock lastBlock = blockchain.get(blockchain.size() - 1);
        AuditLogBlock newBlock  = new AuditLogBlock(
                blockchain.size(),
                accessorId,
                targetAbhaId,
                action,
                accessGranted,
                lastBlock.getHash()
        );

        blockchain.add(newBlock);
        persistBlockToDB(newBlock);

        String status = accessGranted ? "✅ GRANTED" : "🛑 DENIED";
        log.info("📝 [AUDIT] Block #{} | {} | Accessor: {} | Hash: {}...",
                newBlock.getIndex(), status, accessorId,
                newBlock.getHash().substring(0, 12));

        // ── Layer 3: Ethereum on-chain via VeristasAudit.sol ─────────────────
        if (contractService.isActive()) {
            callLogAccessOnChain(
                contractService.getHospitalDid(),
                accessorId,
                targetAbhaId,
                action,
                accessGranted
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layer 3 — On-chain logging via VeristasAudit.logAccess()
    // ─────────────────────────────────────────────────────────────────────────

    private void callLogAccessOnChain(String institutionDid,
                                       String accessorId,
                                       String targetAbhaId,
                                       String action,
                                       boolean accessGranted) {
        try {
            VeristasAudit contract = contractService.getContract();

            String roleTag = deriveRoleTag(accessorId);
            // Stage is not yet propagated into recordAccessAttempt() callers;
            // defaulting to "access" here — TODO: pass stage through from OPA context
            String stage = "access";

            TransactionReceipt receipt = contract.logAccess(
                    institutionDid, // _institutionDid — which hospital
                    accessorId,     // _userDid        — accessor's DID or JWT role
                    roleTag,        // _roleTag        — "p", "s", "h", "d"
                    action,         // _action         — "READ", "WRITE", etc.
                    stage,          // _stage          — emergency stage context
                    accessGranted   // _accessGranted
            ).send();

            log.info("⛓️  [BLOCKCHAIN] logAccess() confirmed | Institution: {} | TX: {} | Gas: {}",
                    institutionDid,
                    receipt.getTransactionHash(),
                    receipt.getGasUsed());

        } catch (Exception e) {
            log.warn("⚠️  [BLOCKCHAIN] On-chain log failed (block still in DB): {}", e.getMessage());
        }
    }

    /**
     * Maps an accessor identity string to the VeristasAudit contract's role tag.
     *
     * @param accessorId Free-form identity string (JWT role, DID, etc.)
     * @return Single-char role code: "p" (paramedic), "s" (surgeon),
     *         "h" (hospital), "d" (dispatcher), or "x" (unknown)
     */
    private String deriveRoleTag(String accessorId) {
        if (accessorId == null) return "x";
        String lower = accessorId.toLowerCase();
        if (lower.contains("paramedic"))  return "p";
        if (lower.contains("surgeon"))    return "s";
        if (lower.contains("hospital"))   return "h";
        if (lower.contains("dispatcher")) return "d";
        if (lower.contains("system"))     return "sys";
        return "x";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layer 2 — PostgreSQL persistence
    // ─────────────────────────────────────────────────────────────────────────

    private void persistBlockToDB(AuditLogBlock block) {
        AuditBlockEntity entity = new AuditBlockEntity();
        entity.setBlockIndex(block.getIndex());
        entity.setTimestamp(block.getTimestamp());
        entity.setAccessorId(block.getAccessorId());
        entity.setTargetAbhaId(block.getTargetAbhaId());
        entity.setAction(block.getAction());
        entity.setAccessGranted(block.isAccessGranted());
        entity.setPreviousHash(block.getPreviousHash());
        entity.setHash(block.getHash());
        auditBlockRepository.save(entity);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read Helpers — used by AuditController
    // ─────────────────────────────────────────────────────────────────────────

    public List<AuditLogBlock> getFullLedger() {
        return blockchain;
    }

    /**
     * Verifies mathematical integrity of the in-memory SHA-256 chain.
     * Each block's hash must equal SHA-256(its data + previousHash).
     */
    public boolean isChainValid() {
        for (int i = 1; i < blockchain.size(); i++) {
            AuditLogBlock current  = blockchain.get(i);
            AuditLogBlock previous = blockchain.get(i - 1);
            if (!current.getHash().equals(current.calculateHash())) return false;
            if (!current.getPreviousHash().equals(previous.getHash())) return false;
        }
        return true;
    }

    /**
     * Wipes and re-initialises the audit chain (dev/test only).
     * Does NOT affect the Ethereum contract — on-chain data is immutable.
     */
    public void resetChain() {
        auditBlockRepository.deleteAll();
        blockchain.clear();
        AuditLogBlock genesisBlock = new AuditLogBlock(0, "SYSTEM", "NONE", "GENESIS", true, "0");
        blockchain.add(genesisBlock);
        persistBlockToDB(genesisBlock);
        log.info("⛓️  [AUDIT] Chain reset. New Genesis Block created. (On-chain data unaffected.)");
    }
}