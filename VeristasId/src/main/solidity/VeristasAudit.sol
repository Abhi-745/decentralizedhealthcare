// SPDX-License-Identifier: MIT
pragma solidity >=0.8.0 <0.9.0;

/**
 * @title VeristasAudit
 * @dev Immutable audit trail for the AC-ABAC Emergency Access System.
 *      Every access decision made by OPA is recorded here as a tamper-proof
 *      blockchain transaction. Once written, no record can be altered or deleted.
 *
 *      Pragma lowered to >=0.8.0 <0.9.0 for compatibility with the web3j
 *      bundled Solidity compiler (solcJ). All features used are available
 *      since Solidity 0.8.0.
 */
contract VeristasAudit {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Event — Emitted for every access decision (indexed for fast search)
    // ─────────────────────────────────────────────────────────────────────────
    event AccessLogged(
        uint256 indexed timestamp,
        string  institutionDid,  // which hospital/institution made this decision
        string  userDid,
        string  roleTag,
        string  action,
        string  stage,
        bool    accessGranted
    );

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Struct — Holds the data structure of a single audit record
    // ─────────────────────────────────────────────────────────────────────────
    struct AuditRecord {
        uint256 timestamp;      // block.timestamp — set by the network, not the caller
        string  institutionDid; // e.g. "did:veristas:hospital:AIIMS_Delhi"
        string  userDid;        // e.g. "did:veristas:patient:AbCdEf123456"
        string  roleTag;        // "p" (paramedic) | "s" (surgeon) | "h" (hospital) | "d" (dispatcher)
        string  action;         // "READ" | "WRITE" | "UPDATE"
        string  stage;          // "dispatched" | "arrived" | "access"
        bool    accessGranted;  // true = OPA allowed, false = OPA denied
    }

    // Append-only array — the immutable ledger
    AuditRecord[] public auditTrail;

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Write Function — Called by each hospital's Spring Boot instance
    //    Every hospital writes to THIS shared contract using its own wallet.
    //    institutionDid identifies which hospital submitted each record.
    // ─────────────────────────────────────────────────────────────────────────
    function logAccess(
        string memory _institutionDid,
        string memory _userDid,
        string memory _roleTag,
        string memory _action,
        string memory _stage,
        bool          _accessGranted
    ) public {
        AuditRecord memory newRecord = AuditRecord({
            timestamp:      block.timestamp,
            institutionDid: _institutionDid,
            userDid:        _userDid,
            roleTag:        _roleTag,
            action:         _action,
            stage:          _stage,
            accessGranted:  _accessGranted
        });

        auditTrail.push(newRecord);

        emit AccessLogged(
            block.timestamp,
            _institutionDid,
            _userDid,
            _roleTag,
            _action,
            _stage,
            _accessGranted
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Read Functions — Query the ledger
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @return Total number of audit records on-chain.
     */
    function getAuditCount() public view returns (uint256) {
        return auditTrail.length;
    }

    /**
     * @dev Fetch a specific record for forensic review.
     * @param index Zero-based index into auditTrail array.
     */
    function getAuditRecord(uint256 index) public view returns (
        uint256 timestamp,
        string  memory institutionDid,
        string  memory userDid,
        string  memory roleTag,
        string  memory action,
        string  memory stage,
        bool    accessGranted
    ) {
        require(index < auditTrail.length, "VeristasAudit: record index out of bounds");
        AuditRecord memory record = auditTrail[index];
        return (
            record.timestamp,
            record.institutionDid,
            record.userDid,
            record.roleTag,
            record.action,
            record.stage,
            record.accessGranted
        );
    }
}
