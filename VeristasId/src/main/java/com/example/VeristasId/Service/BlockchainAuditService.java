package com.example.VeristasId.Service;

import com.example.VeristasId.Dto.AuditLogBlock;
import com.example.VeristasId.Model.AuditBlockEntity;
import com.example.VeristasId.Repository.AuditBlockRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlockchainAuditService {

    // In-memory chain for fast reads (rebuilt from DB on startup)
    private final List<AuditLogBlock> blockchain = new ArrayList<>();

    private final AuditBlockRepository auditBlockRepository;

    // Web3 components
    private Web3j web3j;
    private Credentials credentials;
    private boolean isWeb3Active = false;

    public BlockchainAuditService(AuditBlockRepository auditBlockRepository) {
        this.auditBlockRepository = auditBlockRepository;
    }

    @PostConstruct
    public void initGenesisBlock() {
        // --- PERSISTENCE: Reload existing blocks from DB into memory ---
        List<AuditBlockEntity> savedBlocks = auditBlockRepository.findAll();
        if (!savedBlocks.isEmpty()) {
            // Rebuild the in-memory chain from PostgreSQL
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
            System.out.println("⛓️ [BLOCKCHAIN] Loaded " + blockchain.size() + " blocks from PostgreSQL. Audit Log restored.");
        } else {
            // First ever startup — create Genesis Block
            AuditLogBlock genesisBlock = new AuditLogBlock(0, "SYSTEM", "NONE", "INIT", true, "0");
            blockchain.add(genesisBlock);
            persistBlockToDB(genesisBlock);
            System.out.println("⛓️ [BLOCKCHAIN] Genesis Block Created. Immutable Audit Log is online.");
        }

        // Try to connect Web3j to Ganache
        try {
            System.out.println("🌐 [WEB3] Connecting to local Ethereum node (Ganache)...");
            web3j = Web3j.build(new HttpService("http://127.0.0.1:8545"));
            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            System.out.println("✅ [WEB3] Connected to Node: " + clientVersion);
            credentials = Credentials.create(Keys.createEcKeyPair());
            isWeb3Active = true;
        } catch (Exception e) {
            System.out.println("⚠️ [WEB3] Falling back to In-Memory + PostgreSQL Blockchain Simulation.");
            isWeb3Active = false;
        }
    }

    // Called by controllers to log an access event
    public void recordAccessAttempt(String accessorId, String targetAbhaId, String action, boolean accessGranted) {
        AuditLogBlock lastBlock = blockchain.get(blockchain.size() - 1);

        AuditLogBlock newBlock = new AuditLogBlock(
                blockchain.size(),
                accessorId,
                targetAbhaId,
                action,
                accessGranted,
                lastBlock.getHash()
        );

        blockchain.add(newBlock);

        // --- PERSISTENCE: Save each new block to PostgreSQL ---
        persistBlockToDB(newBlock);

        String status = accessGranted ? "✅ GRANTED" : "🛑 DENIED";
        System.out.println("📝 [AUDIT LOG] Block #" + newBlock.getIndex() + " Added | " + status +
                " | Hash: " + newBlock.getHash().substring(0, 15) + "...");

        // Optional: also write to Ethereum
        if (isWeb3Active) {
            broadcastToEthereum(newBlock, status);
        }
    }

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

    private void broadcastToEthereum(AuditLogBlock block, String status) {
        try {
            String logData = String.format("VERISTAS_LOG|%s|%s|%s|%s|HASH:%s",
                    block.getAccessorId(), block.getTargetAbhaId(), block.getAction(), status, block.getHash());
            String hexData = Numeric.toHexString(logData.getBytes());

            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                    credentials.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();
            BigInteger nonce = ethGetTransactionCount.getTransactionCount();

            BigInteger gasPrice = BigInteger.valueOf(20000000000L);
            BigInteger gasLimit = BigInteger.valueOf(21000 + logData.length() * 68L + 50000);

            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce, gasPrice, gasLimit, credentials.getAddress(), BigInteger.ZERO, hexData);

            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            if (ethSendTransaction.hasError()) {
                System.out.println("❌ [WEB3] Failed to broadcast: " + ethSendTransaction.getError().getMessage());
            } else {
                System.out.println("⛓️  [WEB3] TX Hash: " + ethSendTransaction.getTransactionHash());
            }
        } catch (Exception e) {
            System.out.println("⚠️ [WEB3] Error broadcasting, logged to DB only. " + e.getMessage());
        }
    }

    public List<AuditLogBlock> getFullLedger() {
        return blockchain;
    }

    public boolean isChainValid() {
        for (int i = 1; i < blockchain.size(); i++) {
            AuditLogBlock current = blockchain.get(i);
            AuditLogBlock previous = blockchain.get(i - 1);
            if (!current.getHash().equals(current.calculateHash())) return false;
            if (!current.getPreviousHash().equals(previous.getHash())) return false;
        }
        return true;
    }
}