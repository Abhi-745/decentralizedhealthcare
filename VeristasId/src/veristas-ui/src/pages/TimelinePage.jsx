import React, { useState } from 'react';

const base = {
  fontFamily: "'Inter', sans-serif",
  background: '#080808',
  minHeight: '100vh',
  padding: '48px 24px',
  color: '#e5e5e5',
};

const DAYS = [
  {
    range: '1', title: 'Foundation',
    tests: 1,
    items: ['Spring Boot 3.x scaffold + application.properties', '@SpringBootApplication entry point', 'VeristasIdApplicationTest — context smoke test'],
    concept: '@SpringBootApplication bootstraps the entire IoC container in a single annotation. It combines @Configuration, @EnableAutoConfiguration, and @ComponentScan.',
  },
  {
    range: '2–4', title: 'Data Layer',
    tests: 21,
    items: ['5 JPA entities: Patient, MedicalRecord, EmergencySession, Consent, VerifiableCredential', '@DataJpaTest with H2 — real table creation + CRUD tests', 'EmergencySessionRepository — findFirstByPatientIdOrderByCreatedAtDesc (fixed @Query bug)'],
    concept: '@DataJpaTest slices the Spring context to only the persistence layer. It is 10× faster than @SpringBootTest for database-only tests.',
  },
  {
    range: '5–9', title: 'Identity & Security Services',
    tests: 67,
    items: ['JwtService — HMAC-SHA256 sign/verify (16 tests)', 'PatientWalletService — BouncyCastle P-256 ECDSA key generation (14 tests)', 'CredentialIssuanceService — W3C VC creation with DID (16 tests)', 'OpaService — Open Policy Agent integration, graceful degradation (13 tests)'],
    concept: 'Constructor injection (not @Autowired on fields) makes dependencies explicit and enables clean unit testing without a Spring context.',
  },
  {
    range: '10–15', title: 'REST Controllers',
    tests: 70,
    items: ['EmergencySessionController — dispatcher-only create, 5-stage state machine', 'EMRController — double-auth (VC AND JWT) + OR security gate', 'ResourceController — full zero-trust: identity → OPA → EMR → audit', 'AccessController — MockRestServiceServer for internal RestTemplate testing'],
    concept: '@WebMvcTest(Controller.class) loads only the web layer. Services are @MockBean. This isolates HTTP routing and response serialisation from business logic.',
  },
  {
    range: '16–17', title: 'Blockchain Integrity',
    tests: 7,
    items: ['AuditLogBlock — immutable DTO, no public setHash() allowed', 'SHA-256 determinism: same input → always same 64-char hex output', 'Avalanche effect: change one character → ~50% of output bits flip', 'Chain linking: block[N].previousHash must equal block[N-1].hash', 'Design-contract test: reflection scan confirms setHash() does not exist'],
    concept: 'If setHash() existed, an attacker could modify stored data and update the hash to match — making tampering invisible. Immutability at the DTO level forces tampers to be visible at the chain validation level.',
  },
  {
    range: '18', title: 'Three-Layer Audit Service',
    tests: 8,
    items: ['Layer 1: in-memory SHA-256 chain (instant reads)', 'Layer 2: PostgreSQL persistence (survives restarts)', 'Layer 3: Ethereum via VeristasAudit.sol (immutable, on-chain)', '@PostConstruct does not fire in unit tests — resetChain() called in @BeforeEach', 'ReflectionTestUtils.getField() reaches private blockchain list to simulate tampering'],
    concept: 'When @SpringBootTest is not used, @PostConstruct never fires. You must manually call the initialisation method in @BeforeEach to seed required state.',
  },
  {
    range: '19', title: 'Integration Tests',
    tests: 6,
    items: ['@SpringBootTest loads the COMPLETE Spring context (not a slice)', '@MockBean replaces OpaService, ContractLifecycleService, JwtService inside Spring context', 'OpaSecurityFilter intercepts ALL requests — lenient().when(jwtService...).thenReturn(true) bypasses it', 'sessionRepo.findById() queries REAL H2 database after each HTTP call', '@AfterEach deleteAll() for isolation — NOT @Transactional (MockMvc runs in separate transaction)'],
    concept: '@MockBean differs from @Mock: @Mock creates a mock outside Spring. @MockBean replaces the real Spring bean in the running application context.',
  },
  {
    range: '20', title: 'Final Polish',
    tests: 0,
    items: ['Professional README with architecture diagram, API table, testing summary', 'Timeline Page — 20-day build story', '194 tests passing across 5 testing layers, zero failures', 'Vercel + Railway live deployment'],
    concept: 'A project is only as good as its documentation. A recruiter who cannot understand what you built in 60 seconds will move on.',
  },
];

export default function TimelinePage() {
  const [open, setOpen] = useState('20');
  const total = DAYS.reduce((s, d) => s + d.tests, 0);

  return (
    <div style={base}>
      <div style={{ maxWidth: '640px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '40px' }}>

        {/* Header */}
        <div>
          <h1 style={{ fontSize: '22px', fontWeight: '600', letterSpacing: '-0.02em', color: '#fff', margin: '0 0 6px' }}>
            Build Timeline
          </h1>
          <p style={{ fontSize: '13px', color: '#999', margin: 0 }}>
            VeristasId · 20 days · {total} tests written · 0 failures
          </p>
        </div>

        {/* Stats row */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', border: '1px solid #1c1c1c', borderRadius: '8px', overflow: 'hidden' }}>
          {[['20', 'Days'], [`${total}`, 'Tests'], ['11', 'Pages'], ['0', 'Failures']].map(([n, l], i) => (
            <div key={l} style={{
              padding: '16px',
              borderRight: i < 3 ? '1px solid #1c1c1c' : 'none',
            }}>
              <div style={{ fontSize: '20px', fontWeight: '600', fontFamily: 'JetBrains Mono, monospace', letterSpacing: '-0.02em', color: '#fff' }}>{n}</div>
              <div style={{ fontSize: '11px', color: '#999', marginTop: '2px' }}>{l}</div>
            </div>
          ))}
        </div>

        {/* Testing pyramid */}
        <div style={{ border: '1px solid #1c1c1c', borderRadius: '8px', padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <p style={{ fontSize: '11px', color: '#999', fontFamily: 'JetBrains Mono, monospace', letterSpacing: '0.06em', margin: 0 }}>TESTING PYRAMID</p>
          {[
            { label: 'Integration (@SpringBootTest)', n: 6,  w: '3%'  },
            { label: 'DTO / Pure Java + Reflection',  n: 7,  w: '4%'  },
            { label: 'Service (MockitoExtension)',     n: 67, w: '35%' },
            { label: 'Controller (@WebMvcTest)',       n: 70, w: '36%' },
            { label: 'Repository (@DataJpaTest)',      n: 44, w: '23%' },
          ].map(({ label, n, w }) => (
            <div key={label} style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{ width: '120px', height: '2px', background: '#111', borderRadius: '1px', flexShrink: 0, position: 'relative', overflow: 'hidden' }}>
                <div style={{ width: w, height: '100%', background: '#3b82f6', borderRadius: '1px' }} />
              </div>
              <span style={{ fontSize: '12px', color: '#bbb', flex: 1 }}>{label}</span>
              <span style={{ fontSize: '12px', fontFamily: 'JetBrains Mono, monospace', color: '#777', flexShrink: 0 }}>{n}</span>
            </div>
          ))}
        </div>

        {/* Day list */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1px' }}>
          {DAYS.map((day) => (
            <div key={day.range}>
              {/* Row */}
              <div
                onClick={() => setOpen(open === day.range ? null : day.range)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '16px',
                  padding: '14px 0',
                  borderBottom: '1px solid #111',
                  cursor: 'pointer',
                  userSelect: 'none',
                }}
              >
                <span style={{
                  fontFamily: 'JetBrains Mono, monospace',
                  fontSize: '11px',
                  color: '#777',
                  width: '36px',
                  flexShrink: 0,
                }}>
                  D{day.range}
                </span>
                <span style={{ fontSize: '13px', fontWeight: '500', color: '#ccc', flex: 1 }}>
                  {day.title}
                </span>
                {day.tests > 0 && (
                  <span style={{
                    fontFamily: 'JetBrains Mono, monospace',
                    fontSize: '11px',
                    color: '#3b82f6',
                    flexShrink: 0,
                  }}>
                    {day.tests} tests
                  </span>
                )}
                <span style={{ color: '#777', fontSize: '12px', flexShrink: 0 }}>
                  {open === day.range ? '−' : '+'}
                </span>
              </div>

              {/* Expanded */}
              {open === day.range && (
                <div style={{
                  padding: '16px 0 20px 52px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '16px',
                  borderBottom: '1px solid #111',
                }}>
                  <ul style={{ margin: 0, padding: 0, listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {day.items.map((item, i) => (
                      <li key={i} style={{ display: 'flex', gap: '10px', alignItems: 'flex-start' }}>
                        <span style={{ color: '#777', marginTop: '5px', flexShrink: 0, fontSize: '10px' }}>—</span>
                        <span style={{ fontSize: '12px', color: '#bbb', lineHeight: '1.5' }}>{item}</span>
                      </li>
                    ))}
                  </ul>
                  <div style={{
                    padding: '12px 14px',
                    background: '#0d0d0d',
                    border: '1px solid #1a1a1a',
                    borderRadius: '6px',
                  }}>
                    <p style={{ fontSize: '11px', color: '#777', fontFamily: 'JetBrains Mono, monospace', letterSpacing: '0.06em', margin: '0 0 6px' }}>CONCEPT</p>
                    <p style={{ fontSize: '12px', color: '#bbb', lineHeight: '1.6', margin: 0 }}>{day.concept}</p>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

      </div>
    </div>
  );
}
