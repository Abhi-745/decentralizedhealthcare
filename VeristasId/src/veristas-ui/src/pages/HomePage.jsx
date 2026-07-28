import React from 'react';

const S = {
  page: {
    minHeight: '100vh',
    background: '#080808',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '80px 24px',
    fontFamily: "'Inter', sans-serif",
  },
  inner: {
    maxWidth: '600px',
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    gap: '48px',
  },
  badge: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '6px',
    padding: '4px 10px',
    border: '1px solid #1c1c1c',
    borderRadius: '4px',
    fontSize: '11px',
    fontFamily: "'JetBrains Mono', monospace",
    color: '#555555',
    letterSpacing: '0.04em',
    width: 'fit-content',
  },
  dot: {
    width: '6px',
    height: '6px',
    borderRadius: '50%',
    background: '#22c55e',
    animation: 'pulse 2s infinite',
  },
  h1: {
    fontSize: '48px',
    fontWeight: '600',
    letterSpacing: '-0.03em',
    lineHeight: '1.1',
    color: '#ffffff',
    margin: 0,
  },
  h1sub: {
    color: '#444444',
  },
  p: {
    fontSize: '15px',
    lineHeight: '1.7',
    color: '#666666',
    margin: 0,
    maxWidth: '480px',
  },
  stack: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '6px',
  },
  tag: {
    padding: '3px 8px',
    border: '1px solid #1c1c1c',
    borderRadius: '3px',
    fontSize: '11px',
    fontFamily: "'JetBrains Mono', monospace",
    color: '#444444',
    letterSpacing: '0.02em',
  },
  divider: {
    height: '1px',
    background: '#111111',
    border: 'none',
    margin: 0,
  },
  stats: {
    display: 'grid',
    gridTemplateColumns: 'repeat(4, 1fr)',
    gap: '0',
    border: '1px solid #1c1c1c',
    borderRadius: '8px',
    overflow: 'hidden',
  },
  stat: {
    padding: '20px 16px',
    borderRight: '1px solid #1c1c1c',
    display: 'flex',
    flexDirection: 'column',
    gap: '4px',
  },
  statLast: {
    padding: '20px 16px',
    display: 'flex',
    flexDirection: 'column',
    gap: '4px',
  },
  statN: {
    fontSize: '24px',
    fontWeight: '600',
    fontFamily: "'JetBrains Mono', monospace",
    color: '#ffffff',
    letterSpacing: '-0.03em',
  },
  statL: {
    fontSize: '11px',
    color: '#444444',
    letterSpacing: '0.02em',
  },
  features: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '0',
    border: '1px solid #1c1c1c',
    borderRadius: '8px',
    overflow: 'hidden',
  },
  feature: {
    padding: '20px',
    borderRight: '1px solid #1c1c1c',
    borderBottom: '1px solid #1c1c1c',
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  featureTitle: {
    fontSize: '13px',
    fontWeight: '500',
    color: '#cccccc',
    margin: 0,
  },
  featureDesc: {
    fontSize: '12px',
    color: '#444444',
    lineHeight: '1.5',
    margin: 0,
  },
  cta: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
  },
  btnPrimary: {
    padding: '10px 20px',
    background: '#3b82f6',
    border: 'none',
    borderRadius: '6px',
    color: '#ffffff',
    fontSize: '13px',
    fontWeight: '500',
    cursor: 'pointer',
    transition: 'background 0.15s',
    letterSpacing: '-0.01em',
  },
  btnSecondary: {
    padding: '10px 20px',
    background: 'none',
    border: '1px solid #1c1c1c',
    borderRadius: '6px',
    color: '#555555',
    fontSize: '13px',
    fontWeight: '400',
    cursor: 'pointer',
    transition: 'border-color 0.15s, color 0.15s',
    letterSpacing: '-0.01em',
    textDecoration: 'none',
    display: 'inline-block',
  },
};

const FEATURES = [
  { title: 'Zero-Trust Identity', desc: 'ECDSA Verifiable Credentials. No implicit trust — every identity is cryptographically proven.' },
  { title: 'OPA Policy Engine', desc: 'Rego rules evaluate role + emergency stage + action in under 20ms.' },
  { title: 'SHA-256 Audit Chain', desc: 'Every access is sealed into a linked chain. Tamper any block — validation catches it.' },
  { title: 'Break-Glass Access', desc: '5-stage emergency state machine. Dispatcher creates, OPA gates, auditor traces.' },
];

export default function HomePage({ onEnter }) {
  return (
    <div style={S.page}>
      <style>{`
        @keyframes pulse { 0%, 100% { opacity: 1 } 50% { opacity: 0.4 } }
      `}</style>
      <div style={S.inner}>

        {/* Status badge */}
        <div style={S.badge}>
          <span style={S.dot} />
          194 tests passing · deployed on Railway + Vercel
        </div>

        {/* Hero */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <h1 style={S.h1}>
            Zero-Trust Medical<br />
            <span style={S.h1sub}>Identity Platform</span>
          </h1>
          <p style={S.p}>
            A patient-sovereign healthcare system built on W3C Verifiable Credentials,
            ECDSA cryptography, Open Policy Agent ABAC, and an immutable SHA-256
            blockchain audit trail.
          </p>
        </div>

        {/* Tech stack */}
        <div style={S.stack}>
          {['Spring Boot 3', 'React + Vite', 'PostgreSQL', 'Open Policy Agent',
            'BouncyCastle ECDSA', 'W3C Verifiable Credentials', 'SHA-256 Blockchain',
            'Ethereum / Web3j', 'JUnit 5 + Mockito'].map(t => (
            <span key={t} style={S.tag}>{t}</span>
          ))}
        </div>

        {/* Stats */}
        <div style={S.stats}>
          {[
            { n: '194', l: 'Tests Passing' },
            { n: '10',  l: 'REST Controllers' },
            { n: '12',  l: 'Frontend Pages' },
            { n: '3',   l: 'Audit Layers' },
          ].map(({ n, l }, i) => (
            <div key={l} style={i === 3 ? S.statLast : S.stat}>
              <span style={S.statN}>{n}</span>
              <span style={S.statL}>{l}</span>
            </div>
          ))}
        </div>

        {/* Features grid */}
        <div style={S.features}>
          {FEATURES.map(({ title, desc }, i) => (
            <div key={title} style={{
              ...S.feature,
              borderRight: (i % 2 === 0) ? '1px solid #1c1c1c' : 'none',
              borderBottom: (i < 2) ? '1px solid #1c1c1c' : 'none',
            }}>
              <p style={S.featureTitle}>{title}</p>
              <p style={S.featureDesc}>{desc}</p>
            </div>
          ))}
        </div>

        {/* CTA */}
        <div style={S.cta}>
          <button
            style={S.btnPrimary}
            onClick={onEnter}
            onMouseEnter={e => e.target.style.background = '#2563eb'}
            onMouseLeave={e => e.target.style.background = '#3b82f6'}
          >
            Open Dashboard →
          </button>
          <a
            href="https://github.com/Abhi-745/decentralizedhealthcare"
            target="_blank"
            rel="noopener noreferrer"
            style={S.btnSecondary}
            onMouseEnter={e => { e.target.style.borderColor = '#333'; e.target.style.color = '#888'; }}
            onMouseLeave={e => { e.target.style.borderColor = '#1c1c1c'; e.target.style.color = '#555'; }}
          >
            View on GitHub
          </a>
        </div>

      </div>
    </div>
  );
}
