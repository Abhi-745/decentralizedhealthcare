import React, { useState } from 'react';

const base = {
  fontFamily: "'Inter', sans-serif",
  background: '#080808',
  minHeight: '100vh',
  padding: '48px 24px',
  color: '#e5e5e5',
};

const LAYERS = [
  {
    id: 'frontend',
    label: 'Frontend',
    desc: 'React 18 + Vite SPA deployed on Vercel. 12 pages. Redux + Axios.',
    components: [
      ['HomePage',           'Landing page'],
      ['EmergencyDashboard', 'Session state machine control'],
      ['ChainVisualizer',    'Live SHA-256 blockchain viewer'],
      ['VCViewer',           'W3C Verifiable Credential inspector'],
      ['EMRViewer',          'Electronic Medical Record viewer'],
      ['ConsentManager',     'Patient data sovereignty controls'],
    ],
  },
  {
    id: 'api',
    label: 'API (Spring Boot)',
    desc: 'Spring Boot 3.x on Railway. 10 REST controllers. 194 tests.',
    components: [
      ['EmergencySessionController', 'Session state machine (5 stages)'],
      ['EMRController',              'Double-auth + OR security gate'],
      ['AuditController',            'Ledger read + integrity check'],
      ['AuthController',             'HMAC-SHA256 JWT issuance'],
      ['ResourceController',         'Full zero-trust pipeline'],
      ['ConsentController',          'Grant / revoke patient consent'],
    ],
  },
  {
    id: 'security',
    label: 'Identity & Policy',
    desc: 'Zero-Trust. Every request verified by identity, then authorised by OPA Rego.',
    components: [
      ['JwtService',                'HMAC-SHA256 token sign/verify'],
      ['CredentialService',         'ECDSA VC issue + verify + revoke'],
      ['OpaService',                'ABAC via Open Policy Agent'],
      ['acute_care.rego',           'role + stage + action → allow/deny'],
      ['PatientWalletService',      'BouncyCastle ECDSA key generation'],
      ['CredentialIssuanceService', 'W3C VC creation + DID document'],
    ],
  },
  {
    id: 'audit',
    label: 'Audit Chain',
    desc: 'Every access written to 3 independent stores simultaneously.',
    components: [
      ['Layer 1: In-Memory',  'SHA-256 linked list, instant reads'],
      ['Layer 2: PostgreSQL', 'Durable persistence on Railway'],
      ['Layer 3: Ethereum',   'VeristasAudit.sol on testnet'],
      ['AuditLogBlock',       'Immutable DTO — no setHash() allowed'],
      ['isChainValid()',       'O(n) tamper detection algorithm'],
      ['BlockchainAuditService', 'Orchestrates all 3 layers'],
    ],
  },
  {
    id: 'data',
    label: 'Data Layer',
    desc: 'PostgreSQL in prod. H2 in tests. Spring Data JPA repositories.',
    components: [
      ['MedicalRecord',              'Patient EMR'],
      ['EmergencySessionEntity',     'Session ID + stage + patient DID'],
      ['Consent',                    'Patient-granted delegate access'],
      ['AuditBlockEntity',           'Persisted chain block'],
      ['VerifiableCredentialEntity', 'Issued VC + revocation status'],
      ['PatientEntity',              'DID + ECDSA public key'],
    ],
  },
];

const FLOW = [
  ['HTTP Request',    'React → /api/* via Vercel proxy or Railway directly'],
  ['Identity Check',  'OpaSecurityFilter: verifyVC() || verifyStaffToken()'],
  ['OPA Decision',    'Rego: role + stage + action → allow / deny (<20ms)'],
  ['Business Logic',  'Controller → Service → Repository'],
  ['Audit Sealed',    'SHA-256 block linked + persisted + Ethereum'],
  ['Response',        '200 OK or 403 FORBIDDEN with audit trail'],
];

export default function ArchitecturePage() {
  const [active, setActive] = useState('api');
  const layer = LAYERS.find(l => l.id === active);

  return (
    <div style={base}>
      <div style={{ maxWidth: '800px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '40px' }}>

        {/* Header */}
        <div>
          <h1 style={{ fontSize: '22px', fontWeight: '600', letterSpacing: '-0.02em', color: '#fff', margin: '0 0 6px' }}>
            System Architecture
          </h1>
          <p style={{ fontSize: '13px', color: '#444', margin: 0 }}>
            Zero-Trust Decentralised Medical Identity · VeristasId
          </p>
        </div>

        {/* Request flow */}
        <div style={{ border: '1px solid #1c1c1c', borderRadius: '8px', overflow: 'hidden' }}>
          <div style={{ padding: '14px 16px', borderBottom: '1px solid #1c1c1c' }}>
            <p style={{ fontSize: '11px', color: '#444', fontFamily: 'JetBrains Mono, monospace', letterSpacing: '0.06em', margin: 0 }}>
              EVERY REQUEST — ZERO-TRUST PIPELINE
            </p>
          </div>
          {FLOW.map(([step, desc], i) => (
            <div key={step} style={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: '16px',
              padding: '12px 16px',
              borderBottom: i < FLOW.length - 1 ? '1px solid #111' : 'none',
            }}>
              <span style={{
                fontFamily: 'JetBrains Mono, monospace',
                fontSize: '11px',
                color: '#3b82f6',
                width: '16px',
                flexShrink: 0,
                marginTop: '1px',
              }}>{i + 1}</span>
              <span style={{ fontSize: '12px', fontWeight: '500', color: '#888', width: '140px', flexShrink: 0 }}>{step}</span>
              <span style={{ fontSize: '12px', color: '#444', lineHeight: '1.5' }}>{desc}</span>
            </div>
          ))}
        </div>

        {/* Layer explorer */}
        <div style={{ display: 'grid', gridTemplateColumns: '160px 1fr', gap: '0', border: '1px solid #1c1c1c', borderRadius: '8px', overflow: 'hidden' }}>

          {/* Sidebar */}
          <div style={{ borderRight: '1px solid #1c1c1c' }}>
            {LAYERS.map((l) => (
              <button
                key={l.id}
                onClick={() => setActive(l.id)}
                style={{
                  display: 'block',
                  width: '100%',
                  textAlign: 'left',
                  padding: '12px 16px',
                  fontSize: '12px',
                  fontWeight: active === l.id ? '500' : '400',
                  color: active === l.id ? '#fff' : '#444',
                  background: active === l.id ? '#0f0f0f' : 'none',
                  border: 'none',
                  borderBottom: '1px solid #111',
                  borderLeft: active === l.id ? '2px solid #3b82f6' : '2px solid transparent',
                  cursor: 'pointer',
                  transition: 'color 0.1s',
                }}
              >
                {l.label}
              </button>
            ))}
          </div>

          {/* Detail */}
          <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <p style={{ fontSize: '13px', fontWeight: '500', color: '#ccc', margin: '0 0 4px' }}>{layer.label}</p>
              <p style={{ fontSize: '12px', color: '#444', margin: 0, lineHeight: '1.5' }}>{layer.desc}</p>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0' }}>
              {layer.components.map(([name, role], i) => (
                <div key={name} style={{
                  display: 'flex',
                  gap: '16px',
                  padding: '10px 0',
                  borderBottom: i < layer.components.length - 1 ? '1px solid #111' : 'none',
                  alignItems: 'baseline',
                }}>
                  <span style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '11px', color: '#555', width: '180px', flexShrink: 0 }}>{name}</span>
                  <span style={{ fontSize: '12px', color: '#333', lineHeight: '1.4' }}>{role}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Tech stack */}
        <div style={{ border: '1px solid #1c1c1c', borderRadius: '8px', padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <p style={{ fontSize: '11px', color: '#444', fontFamily: 'JetBrains Mono, monospace', letterSpacing: '0.06em', margin: 0 }}>TECH STACK</p>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
            {['Spring Boot 3', 'React 18 + Vite', 'PostgreSQL + H2', 'Open Policy Agent',
              'BouncyCastle ECDSA', 'W3C Verifiable Credentials', 'SHA-256 Blockchain',
              'Ethereum / Web3j', 'JUnit 5', 'Mockito', 'Railway', 'Vercel'].map(t => (
              <span key={t} style={{
                padding: '3px 8px',
                border: '1px solid #1c1c1c',
                borderRadius: '3px',
                fontSize: '11px',
                fontFamily: 'JetBrains Mono, monospace',
                color: '#333',
              }}>{t}</span>
            ))}
          </div>
        </div>

      </div>
    </div>
  );
}
