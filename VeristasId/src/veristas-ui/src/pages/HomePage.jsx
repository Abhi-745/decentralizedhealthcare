import React from 'react';

const FEATURES = [
  {
    title: 'Zero-Trust Identity',
    desc: 'ECDSA Verifiable Credentials. Every identity is cryptographically proven, not assumed.',
    accent: '#3b82f6',
  },
  {
    title: 'OPA Policy Engine',
    desc: 'Open Policy Agent evaluates role + emergency stage + action in under 20ms.',
    accent: '#8b5cf6',
  },
  {
    title: 'SHA-256 Audit Chain',
    desc: 'Every access is sealed into a linked chain. Tamper any block — validation catches it.',
    accent: '#f59e0b',
  },
  {
    title: 'Break-Glass Access',
    desc: '5-stage emergency state machine. Dispatcher creates, OPA gates, blockchain records.',
    accent: '#22c55e',
  },
];

const TECH = [
  'Spring Boot 3', 'React + Vite', 'PostgreSQL',
  'Open Policy Agent', 'BouncyCastle ECDSA',
  'W3C Verifiable Credentials', 'SHA-256 Blockchain', 'Web3j / Ethereum',
];

export default function HomePage({ onEnter }) {
  return (
    <div style={{
      minHeight: '100vh',
      background: '#fafafa',
      fontFamily: "'Inter', sans-serif",
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      padding: '80px 24px 60px',
    }}>
      <style>{`
        @keyframes pulse2 { 0%,100%{opacity:1} 50%{opacity:.5} }
        .btn-blue:hover { background: #2563eb !important; }
        .btn-ghost:hover { border-color: #ccc !important; color: #333 !important; }
        .feat-card:hover { background: #fafafa !important; }
      `}</style>

      <div style={{ maxWidth: '620px', width: '100%', display: 'flex', flexDirection: 'column', gap: '52px' }}>

        {/* — Status pill — */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{
            width: 7, height: 7, borderRadius: '50%', background: '#22c55e',
            display: 'inline-block', animation: 'pulse2 2s infinite',
          }} />
          <span style={{ fontSize: '12px', fontFamily: 'JetBrains Mono, monospace', color: '#666', letterSpacing: '0.03em' }}>
            194 tests passing · live on Railway + Vercel
          </span>
        </div>

        {/* — Hero — */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          <h1 style={{
            margin: 0,
            fontSize: 'clamp(36px, 6vw, 54px)',
            fontWeight: 700,
            letterSpacing: '-0.035em',
            lineHeight: 1.08,
            color: '#111111',
          }}>
            Zero-Trust<br />
            <span style={{
              background: 'linear-gradient(90deg, #3b82f6, #8b5cf6)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundClip: 'text',
            }}>
              Medical Identity
            </span>
          </h1>

          <p style={{
            margin: 0, fontSize: '15px', lineHeight: 1.7,
            color: '#666', maxWidth: '480px',
          }}>
            A patient-sovereign healthcare system built on W3C Verifiable Credentials,
            ECDSA cryptography, Open Policy Agent ABAC, and an immutable SHA-256
            blockchain audit trail. Built across 20 days.
          </p>
        </div>

        {/* — Tech tags — */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '7px' }}>
          {TECH.map(t => (
            <span key={t} style={{
              padding: '4px 10px',
              border: '1px solid #eaeaea',
              borderRadius: '4px',
              fontSize: '11px',
              fontFamily: 'JetBrains Mono, monospace',
              color: '#555',
              background: '#ffffff',
            }}>
              {t}
            </span>
          ))}
        </div>

        {/* — Stats row — */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          border: '1px solid #eaeaea',
          borderRadius: '10px',
          overflow: 'hidden',
          background: '#ffffff',
        }}>
          {[
            { n: '194', l: 'Tests Passing', c: '#3b82f6' },
            { n: '10',  l: 'Controllers',   c: '#8b5cf6' },
            { n: '12',  l: 'Pages Built',   c: '#22c55e' },
            { n: '3',   l: 'Audit Layers',  c: '#f59e0b' },
          ].map(({ n, l, c }, i) => (
            <div key={l} style={{
              padding: '20px 14px',
              borderRight: i < 3 ? '1px solid #eaeaea' : 'none',
              display: 'flex', flexDirection: 'column', gap: '4px',
            }}>
              <span style={{
                fontSize: '26px', fontWeight: 700,
                fontFamily: 'JetBrains Mono, monospace',
                letterSpacing: '-0.04em',
                color: c,
              }}>{n}</span>
              <span style={{ fontSize: '11px', color: '#666', lineHeight: 1.3 }}>{l}</span>
            </div>
          ))}
        </div>

        {/* — Feature grid — */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '1px',
          background: '#eaeaea',
          border: '1px solid #eaeaea',
          borderRadius: '10px',
          overflow: 'hidden',
        }}>
          {FEATURES.map(({ title, desc, accent }) => (
            <div
              key={title}
              className="feat-card"
              style={{
                padding: '20px',
                background: '#ffffff',
                borderLeft: `3px solid ${accent}`,
                display: 'flex', flexDirection: 'column', gap: '8px',
                transition: 'background 0.15s',
              }}
            >
              <p style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: '#111' }}>
                {title}
              </p>
              <p style={{ margin: 0, fontSize: '12px', color: '#666', lineHeight: 1.6 }}>
                {desc}
              </p>
            </div>
          ))}
        </div>

        {/* — CTA — */}
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <button
            className="btn-blue"
            onClick={onEnter}
            style={{
              padding: '11px 22px',
              background: '#3b82f6',
              border: 'none',
              borderRadius: '7px',
              color: '#fff',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
              letterSpacing: '-0.01em',
              transition: 'background 0.15s',
            }}
          >
            Open Dashboard →
          </button>
          <a
            className="btn-ghost"
            href="https://github.com/Abhi-745/decentralizedhealthcare"
            target="_blank"
            rel="noopener noreferrer"
            style={{
              padding: '11px 22px',
              border: '1px solid #eaeaea',
              borderRadius: '7px',
              color: '#666',
              fontSize: '13px',
              fontWeight: 400,
              cursor: 'pointer',
              textDecoration: 'none',
              transition: 'border-color 0.15s, color 0.15s',
              display: 'inline-block',
              background: '#ffffff',
            }}
          >
            GitHub
          </a>
        </div>

      </div>
    </div>
  );
}
