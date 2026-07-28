# VeristasId — Zero-Trust Decentralised Medical Identity Platform

<div align="center">

![Tests](https://img.shields.io/badge/tests-194%20passing-brightgreen)
![Build](https://img.shields.io/badge/build-passing-brightgreen)
![Backend](https://img.shields.io/badge/backend-Railway-blueviolet)
![Frontend](https://img.shields.io/badge/frontend-Vercel-black)
![License](https://img.shields.io/badge/license-MIT-blue)

**A patient-centric, zero-trust healthcare system built across 20 days from scratch.**  
W3C Verifiable Credentials · ECDSA Cryptography · Open Policy Agent ABAC · SHA-256 Blockchain · Ethereum Smart Contract

</div>

---

## Overview

VeristasId is a full-stack zero-trust medical identity platform that ensures every patient record access is:

- **Cryptographically proven** — every identity is backed by an ECDSA Verifiable Credential or HMAC-SHA256 JWT
- **Policy-gated** — Open Policy Agent evaluates role + emergency stage + action before granting access
- **Immutably audited** — every access event is chained into a SHA-256 blockchain (3 layers: in-memory, PostgreSQL, Ethereum)
- **Patient-sovereign** — patients own their credentials and can grant/revoke consent atomically

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND (Vercel)                        │
│   React 18 + Vite  ·  11 pages  ·  Redux + Axios               │
│   Landing · Emergency · Audit · VC · EMR · Consent · Chain...   │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTPS
┌──────────────────────────▼──────────────────────────────────────┐
│                     API GATEWAY (Railway)                        │
│   Spring Boot 3.x  ·  10 REST Controllers                       │
│                                                                  │
│   ┌───────────────┐   ┌─────────────────────────────────────┐  │
│   │ OpaSecurityFilter │  │ Every request authenticated here  │  │
│   │  (Servlet Filter) │  │ BEFORE reaching any controller    │  │
│   └───────┬───────┘   └─────────────────────────────────────┘  │
│           │                                                      │
│   ┌───────▼───────────────────────────────────────────────────┐ │
│   │  IDENTITY LAYER                                            │ │
│   │  JwtService (HMAC-SHA256)  ·  CredentialService (ECDSA)  │ │
│   │  PatientWalletService      ·  CredentialIssuanceService   │ │
│   └───────┬───────────────────────────────────────────────────┘ │
│           │                                                      │
│   ┌───────▼───────────────────────────────────────────────────┐ │
│   │  POLICY LAYER (Open Policy Agent)                          │ │
│   │  OpaService → POST http://localhost:8181/v1/data/...      │ │
│   │  acute_care.rego: role + stage + action → allow/deny      │ │
│   └───────┬───────────────────────────────────────────────────┘ │
│           │                                                      │
│   ┌───────▼───────────────────────────────────────────────────┐ │
│   │  AUDIT LAYER (3 simultaneous writes per access event)      │ │
│   │  Layer 1: In-memory SHA-256 linked chain (instant)        │ │
│   │  Layer 2: PostgreSQL (durable, queryable)                 │ │
│   │  Layer 3: Ethereum via VeristasAudit.sol (immutable)      │ │
│   └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | React 18, Vite, Redux Toolkit, Axios, Framer Motion, Lucide React |
| **Backend** | Spring Boot 3.x, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL (prod) · H2 in-memory (tests) |
| **Identity** | W3C Verifiable Credentials, BouncyCastle ECDSA P-256, HMAC-SHA256 JWT |
| **Policy** | Open Policy Agent, Rego rules |
| **Audit** | SHA-256 blockchain (3-layer), Web3j, Solidity smart contract |
| **Testing** | JUnit 5, Mockito, MockMvc, `@SpringBootTest` integration tests |
| **Deployment** | Railway (backend) · Vercel (frontend) |

---

## API Endpoints

| Method | Endpoint | Role | Description |
|---|---|---|---|
| `POST` | `/api/patients/register` | Public | Register patient, generate ECDSA key pair |
| `POST` | `/api/vc/issue` | Public | Issue W3C Verifiable Credential |
| `POST` | `/api/vc/verify` | Public | Verify VC signature via ECDSA |
| `POST` | `/api/auth/login-paramedic` | Public | Issue HMAC-SHA256 staff JWT |
| `POST` | `/api/emergency/create` | Dispatcher | Create emergency session (stage: dispatched) |
| `POST` | `/api/emergency/update-stage` | Staff | Advance session state machine |
| `GET`  | `/api/emergency/{esid}` | Staff | Get session by ID |
| `POST` | `/api/resource/emergency-access` | Staff | Full zero-trust access pipeline |
| `GET`  | `/api/medical-records/{abhaId}` | Patient/Staff | Fetch EMR (VC or JWT gated) |
| `PUT`  | `/api/emr/update` | Staff | Update EMR (double-auth + OR gate) |
| `POST` | `/api/consent/grant` | Patient | Grant data access to delegate |
| `POST` | `/api/consent/revoke` | Patient | Revoke consent atomically |
| `GET`  | `/api/audit/ledger` | Staff | Full SHA-256 blockchain audit trail |
| `GET`  | `/api/audit/integrity` | Staff | `isChainValid()` tamper check |

---

## Emergency Access Flow

```
Dispatcher → POST /api/emergency/create   → session stage: "dispatched"
                     ↓
Paramedic  → POST /api/auth/login-paramedic → gets JWT (role: paramedic)
                     ↓
Paramedic  → POST /api/resource/emergency-access
             [OPA checks: role="paramedic" + stage="dispatched" + action="read"]
                     ↓ allow
             → EMR returned + AuditBlock sealed into SHA-256 chain
                     ↓
Surgeon    → POST /api/emergency/update-stage  → stage: "arrived"
                     ↓
Surgeon    → POST /api/resource/emergency-access
             [OPA checks: role="surgeon" + stage="arrived" + action="update"]
                     ↓ allow
             → EMR updated + AuditBlock sealed
```

---

## Test Coverage

```
Tests run: 194 — Failures: 0 — Errors: 0 — Skipped: 0 ✅

Controller Tests   (Days 10–16):   70 tests  @WebMvcTest + @MockBean
Service Tests      (Days 5–9, 18): 67 tests  @ExtendWith(MockitoExtension)
Repository Tests   (Days 2–4):     21 tests  @DataJpaTest + H2
DTO/Blockchain     (Day 17):        7 tests  Pure Java + reflection
Integration Tests  (Day 19):        6 tests  @SpringBootTest + real H2
App Context        (Day 1):         1 test   Smoke test
Misc:                              22 tests  Entity models, persistence
```

### Key Testing Patterns Demonstrated

| Pattern | File | Concept |
|---|---|---|
| `MockRestServiceServer` | `AccessControllerTest` | Testing internal `RestTemplate` calls |
| `ReflectionTestUtils` | `BlockchainAuditServiceTest` | Tamper simulation via private field injection |
| `@PostConstruct` in unit tests | `BlockchainAuditServiceTest` | Manual genesis block seeding |
| `@MockBean` in full context | `EmergencySessionIntegrationTest` | `OpaSecurityFilter` bypass pattern |
| Hamcrest vs Mockito conflict | `AuditControllerTest` | Explicit static import disambiguation |
| Immutability contract test | `AuditLogBlockTest` | Reflection scan for absent `setHash()` |

---

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL (or use H2 for tests)
- OPA binary (optional — system degrades gracefully)
- Node.js 18+ (for frontend)

### Backend
```bash
# Clone
git clone https://github.com/Abhi-745/decentralizedhealthcare.git
cd decentralizedhealthcare/VeristasId

# Run (uses H2 in-memory by default if no PostgreSQL configured)
./mvnw spring-boot:run

# Run all tests
./mvnw test
```

### Frontend
```bash
cd src/veristas-ui
npm install
npm run dev
# Vite proxy forwards /api/* to http://localhost:8080
```

### Environment Variables
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://your-host:5432/veristas
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your-password
JWT_SECRET=your-256-bit-secret
VITE_API_BASE_URL=https://your-railway-url.railway.app
```

---

## Project Timeline — Built in 20 Days

| Days | Focus | Deliverable |
|---|---|---|
| 1 | Spring Boot setup | Project scaffold, application context test |
| 2–4 | Data layer | 5 JPA entities, H2 persistence tests, repositories |
| 5–7 | Identity services | JwtService, PatientWalletService, CredentialIssuanceService |
| 8–9 | Security layer | CredentialService (ECDSA), OpaService (Rego), SecurityConfig |
| 10 | Emergency Controller | Full session state machine, dispatcher-only create |
| 11 | Consent + MedRecord | Grant/revoke, EMR CRUD with consent check |
| 12 | Auth + Credential | JWT issuance, VC issuance/verify/revoke controller |
| 13 | Patient Registration | Auto key generation, DID document creation |
| 14 | Resource + Access | Full zero-trust pipeline, OPA integration |
| 15 | EMR deep dive | Double-auth (VC/JWT) + OR security gate implementation |
| 16 | Controller tests | 70 controller-layer unit tests, full coverage |
| 17 | Blockchain DTO | SHA-256 chain properties, avalanche effect, immutability |
| 18 | Service tests | BlockchainAuditService — 3-layer audit, tamper detection |
| 19 | Integration tests | `@SpringBootTest` — HTTP→Service→Repository→H2 pipeline |
| 20 | Polish + Deploy | README, Architecture page, Vercel + Railway live |

---

## Security Design Decisions

**Why ECDSA over RSA for patient credentials?**  
P-256 keys are 256-bit yet provide ~128-bit security strength — smaller than RSA-3072 with equivalent security. Smaller VCs = lower storage costs in a distributed health system.

**Why OPA over Spring Security's built-in RBAC?**  
OPA policies are data files that can be hot-reloaded without redeploying the application. The `acute_care.rego` rules encode clinical access logic (emergency stages, time windows) that would be impossible to express in Spring Security's annotation DSL.

**Why SHA-256 linked chain AND PostgreSQL AND Ethereum?**  
Each layer fails independently. If Ethereum is unreachable (Layer 3), Layers 1+2 continue operating. If the server restarts (Layer 1 lost), Layer 2 rebuilds the in-memory chain from PostgreSQL on `@PostConstruct`. Three independent stores means no single failure erases the audit trail.

---

## License

MIT — built as a portfolio / BTP project.
