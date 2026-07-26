package com.example.VeristasId.Service;

import com.example.VeristasId.Blockchain.VeristasAudit;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;

/**
 * ContractLifecycleService
 *
 * Manages the full lifecycle of the VeristasAudit smart contract:
 *   - On first startup (no VERISTAS_CONTRACT_ADDRESS set): deploys the contract
 *     to the configured Ethereum node and logs the address.
 *   - On subsequent startups: loads the existing deployed contract.
 *
 * All sensitive config is read from environment variables — never hardcoded.
 * If the Ethereum node is unreachable, this service degrades gracefully and
 * the rest of the app continues in in-memory + PostgreSQL mode.
 */
@Service
public class ContractLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(ContractLifecycleService.class);

    // ── Environment Variables ──────────────────────────────────────────────
    // Defaults enable local Ganache dev with zero config.
    // Override in production via actual environment variables.

    @Value("${blockchain.node.url:http://127.0.0.1:8545}")
    private String nodeUrl;

    // Raw 32-byte secp256k1 private key in hex (no 0x prefix).
    // For local dev: use Ganache's first account key.
    // For testnet:   use your MetaMask wallet key.
    // NEVER commit the real value to version control.
    @Value("${blockchain.wallet.private.key:#{null}}")
    private String walletPrivateKey;

    // Set to "NONE" until the contract is deployed for the first time.
    // After first deploy, set this env var to the logged contract address.
    @Value("${blockchain.contract.address:NONE}")
    private String contractAddress;

    // Unique DID for this hospital instance.
    // Stamped on every on-chain audit record so multi-hospital deployments
    // can be distinguished on the shared ledger.
    @Value("${hospital.did:did:veristas:hospital:default}")
    private String hospitalDid;

    // ── State ──────────────────────────────────────────────────────────────
    private Web3j        web3j;
    private VeristasAudit contract;
    private boolean      isActive = false;

    // ─────────────────────────────────────────────────────────────────────────
    // Startup
    // ─────────────────────────────────────────────────────────────────────────

    @PostConstruct
    public void init() {
        if (walletPrivateKey == null || walletPrivateKey.isBlank()) {
            log.warn("⚠️  [BLOCKCHAIN] BLOCKCHAIN_WALLET_PRIVATE_KEY is not set.");
            log.warn("⚠️  [BLOCKCHAIN] Real-chain audit logging is DISABLED.");
            log.warn("⚠️  [BLOCKCHAIN] Falling back to in-memory + PostgreSQL audit only.");
            return;
        }

        log.info("🏥 [BLOCKCHAIN] Hospital Identity: {}", hospitalDid);

        try {
            // 1. Connect to the Ethereum node (Ganache / Sepolia / etc.)
            log.info("🌐 [BLOCKCHAIN] Connecting to Ethereum node at {}", nodeUrl);
            web3j = Web3j.build(new HttpService(nodeUrl));

            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            log.info("✅ [BLOCKCHAIN] Connected to: {}", clientVersion);

            // 2. Load the wallet (credentials) from the private key
            Credentials credentials = Credentials.create(walletPrivateKey);
            log.info("🔑 [BLOCKCHAIN] Deployer/caller wallet address: {}", credentials.getAddress());

            // 3. Deploy or load the VeristasAudit contract
            if ("NONE".equalsIgnoreCase(contractAddress) || contractAddress.isBlank()) {
                deployContract(web3j, credentials);
            } else {
                loadContract(web3j, credentials);
            }

        } catch (Exception e) {
            log.warn("⚠️  [BLOCKCHAIN] Ethereum node unreachable: {}", e.getMessage());
            log.warn("⚠️  [BLOCKCHAIN] Falling back to in-memory + PostgreSQL audit mode.");
            isActive = false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Deploy (first run)
    // ─────────────────────────────────────────────────────────────────────────

    private void deployContract(Web3j web3j, Credentials credentials) {
        try {
            log.info("🚀 [BLOCKCHAIN] VERISTAS_CONTRACT_ADDRESS=NONE — deploying VeristasAudit...");

            contract = VeristasAudit.deploy(web3j, credentials, new DefaultGasProvider()).send();
            String deployedAddress = contract.getContractAddress();

            log.info("✅ [BLOCKCHAIN] VeristasAudit deployed successfully!");
            log.info("📍 [BLOCKCHAIN] Contract address: {}", deployedAddress);
            log.info("");
            log.info("══════════════════════════════════════════════════════════");
            log.info("  ACTION REQUIRED — Save this address to your environment:");
            log.info("  VERISTAS_CONTRACT_ADDRESS={}", deployedAddress);
            log.info("  Set it in your .env file or CI/CD secrets to avoid");
            log.info("  re-deploying on every restart.");
            log.info("══════════════════════════════════════════════════════════");

            isActive = true;

        } catch (Exception e) {
            log.error("❌ [BLOCKCHAIN] Contract deployment failed: {}", e.getMessage());
            log.error("   Ensure your wallet has enough ETH for gas and the node is running.");
            isActive = false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load (subsequent restarts)
    // ─────────────────────────────────────────────────────────────────────────

    private void loadContract(Web3j web3j, Credentials credentials) {
        try {
            log.info("🔗 [BLOCKCHAIN] Loading VeristasAudit at {}", contractAddress);

            contract = VeristasAudit.load(
                contractAddress,
                web3j,
                credentials,
                new DefaultGasProvider()
            );

            // Sanity check — read the current audit count from chain
            BigInteger count = contract.getAuditCount().send();
            log.info("✅ [BLOCKCHAIN] Contract loaded. On-chain audit records: {}", count);

            isActive = true;

        } catch (Exception e) {
            log.error("❌ [BLOCKCHAIN] Failed to load contract at {}: {}", contractAddress, e.getMessage());
            log.error("   Verify the address is correct and the node is synced.");
            isActive = false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API — used by BlockchainAuditService
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @return The live contract wrapper, or null if blockchain is inactive.
     */
    public VeristasAudit getContract() {
        return contract;
    }

    /**
     * @return true if connected to an Ethereum node and contract is loaded/deployed.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * @return The Web3j connection, or null if inactive.
     */
    public Web3j getWeb3j() {
        return web3j;
    }

    /**
     * @return The DID of this hospital instance, stamped on every on-chain audit record.
     */
    public String getHospitalDid() {
        return hospitalDid;
    }
}
