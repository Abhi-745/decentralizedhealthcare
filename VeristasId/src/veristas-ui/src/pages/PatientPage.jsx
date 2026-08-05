import React, { useState } from 'react';
import axios from 'axios';

// ─── Shared styles ────────────────────────────────────────────────────────────
const S = {
  page: {
    minHeight: '100vh', background: '#fafafa',
    fontFamily: "'Inter', sans-serif", padding: '40px 24px',
  },
  inner: { maxWidth: '760px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '32px' },
  card: { border: '1px solid #eaeaea', borderRadius: '10px', background: '#fff', overflow: 'hidden' },
  cardHead: { padding: '12px 16px', borderBottom: '1px solid #eaeaea', background: '#fafafa', display: 'flex', alignItems: 'center', gap: '10px' },
  cardBody: { padding: '20px', display: 'flex', flexDirection: 'column', gap: '14px' },
  label: { fontSize: '11px', color: '#666', marginBottom: '5px', display: 'block' },
  input: {
    width: '100%', padding: '9px 12px', border: '1px solid #eaeaea', borderRadius: '6px',
    fontSize: '12px', fontFamily: 'JetBrains Mono, monospace', color: '#111',
    background: '#fafafa', outline: 'none', boxSizing: 'border-box',
  },
  sectionLabel: {
    fontSize: '11px', color: '#999', fontFamily: 'JetBrains Mono, monospace',
    letterSpacing: '0.07em', textTransform: 'uppercase', margin: 0,
  },
};

const METHOD = {
  GET:    { bg: '#f0fdf4', color: '#15803d', border: '#bbf7d0' },
  POST:   { bg: '#eff6ff', color: '#1d4ed8', border: '#bfdbfe' },
  DELETE: { bg: '#fef2f2', color: '#b91c1c', border: '#fecaca' },
};

function Badge({ method }) {
  const s = METHOD[method] || METHOD.GET;
  return (
    <span style={{
      padding: '2px 7px', borderRadius: '4px', fontSize: '10px', fontWeight: '600',
      fontFamily: 'JetBrains Mono, monospace', letterSpacing: '0.04em',
      background: s.bg, color: s.color, border: `1px solid ${s.border}`,
    }}>{method}</span>
  );
}

function Btn({ onClick, disabled, children, color = '#3b82f6', outline = false }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      style={{
        padding: '9px 18px', border: outline ? `1px solid ${color}` : 'none',
        borderRadius: '6px', background: outline ? 'transparent' : (disabled ? '#e5e7eb' : color),
        color: outline ? color : (disabled ? '#9ca3af' : '#fff'),
        fontSize: '12px', fontWeight: '500', cursor: disabled ? 'not-allowed' : 'pointer',
        transition: 'opacity 0.15s', alignSelf: 'flex-start',
        fontFamily: "'Inter', sans-serif",
      }}
    >{disabled ? 'Loading…' : children}</button>
  );
}

function Result({ data, error }) {
  if (!data && !error) return null;
  const ok = !error;
  return (
    <div style={{
      padding: '14px', borderRadius: '6px',
      background: ok ? '#f0fdf4' : '#fef2f2',
      border: `1px solid ${ok ? '#bbf7d0' : '#fecaca'}`,
    }}>
      <p style={{ margin: '0 0 6px', fontSize: '11px', fontFamily: 'JetBrains Mono, monospace', color: ok ? '#15803d' : '#b91c1c' }}>
        {ok ? '200 OK' : `${error.status || 'ERROR'}`}
      </p>
      <pre style={{ margin: 0, fontSize: '11px', color: '#111', fontFamily: 'JetBrains Mono, monospace', whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
        {JSON.stringify(ok ? data : (error.data || error.message), null, 2)}
      </pre>
    </div>
  );
}

// ─── Registration panel ───────────────────────────────────────────────────────
function RegistrationPanel() {
  const [abhaId, setAbhaId]   = useState('99-9999-9999-9999');
  const [did, setDid]         = useState('did:veristas:patient:abc123');
  const [loading, setLoading] = useState(false);
  const [result, setResult]   = useState(null);
  const [error, setError]     = useState(null);

  const register = async () => {
    setLoading(true); setResult(null); setError(null);
    try {
      const { data } = await axios.post('/api/patients/register', { abhaId, patientDid: did });
      setResult(data);
    } catch (e) {
      setError({ status: e.response?.status, data: e.response?.data || e.message });
    } finally { setLoading(false); }
  };

  const autoRegister = async () => {
    setLoading(true); setResult(null); setError(null);
    try {
      const { data } = await axios.get('/api/patients/auto-register-demo');
      setResult(data);
    } catch (e) {
      setError({ status: e.response?.status, data: e.response?.data || e.message });
    } finally { setLoading(false); }
  };

  return (
    <div style={S.card}>
      <div style={S.cardHead}>
        <Badge method="POST" />
        <span style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '12px', color: '#111' }}>
          /api/patients/register
        </span>
        <span style={{ marginLeft: 'auto', fontSize: '11px', color: '#999' }}>
          Issues a signed W3C Verifiable Credential to the patient
        </span>
      </div>
      <div style={S.cardBody}>
        <p style={{ margin: 0, fontSize: '12px', color: '#555', lineHeight: 1.6 }}>
          Patient onboarding: the hospital generates an <strong>ECDSA key pair</strong> for the patient,
          creates a <strong>DID</strong>, issues a signed <strong>W3C VC</strong>, and stores it in the patient wallet.
          The VC acts as the patient's identity credential for all future access.
        </p>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
          <div>
            <label style={S.label}>ABHA ID (14-digit health number)</label>
            <input style={S.input} value={abhaId} onChange={e => setAbhaId(e.target.value)} placeholder="99-9999-9999-9999" />
          </div>
          <div>
            <label style={S.label}>Patient DID</label>
            <input style={S.input} value={did} onChange={e => setDid(e.target.value)} placeholder="did:veristas:patient:..." />
          </div>
        </div>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <Btn onClick={register} disabled={loading}>POST /api/patients/register</Btn>
          <Btn onClick={autoRegister} disabled={loading} color="#8b5cf6" outline>
            GET /api/patients/auto-register-demo
          </Btn>
        </div>
        <Result data={result} error={error} />
      </div>
    </div>
  );
}

// ─── Patient Login panel ──────────────────────────────────────────────────────
function PatientLoginPanel() {
  const [vcToken, setVcToken] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult]   = useState(null);
  const [error, setError]     = useState(null);

  const login = async () => {
    if (!vcToken.trim()) { setError({ status: 400, data: 'Paste your VC token from registration above.' }); return; }
    setLoading(true); setResult(null); setError(null);
    try {
      const token = vcToken.startsWith('Bearer ') ? vcToken : `Bearer ${vcToken}`;
      const { data } = await axios.post('/api/auth/patient-login', null, { headers: { Authorization: token } });
      setResult(data);
    } catch (e) {
      setError({ status: e.response?.status, data: e.response?.data || e.message });
    } finally { setLoading(false); }
  };

  return (
    <div style={S.card}>
      <div style={S.cardHead}>
        <Badge method="POST" />
        <span style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '12px', color: '#111' }}>
          /api/auth/patient-login
        </span>
        <span style={{ marginLeft: 'auto', fontSize: '11px', color: '#999' }}>
          Verifiable Credential → Identity verified (no password)
        </span>
      </div>
      <div style={S.cardBody}>

        {/* Key concept box */}
        <div style={{ padding: '12px 14px', background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: '6px' }}>
          <p style={{ margin: '0 0 4px', fontSize: '12px', fontWeight: '600', color: '#1d4ed8' }}>
            Why no password?
          </p>
          <p style={{ margin: 0, fontSize: '12px', color: '#3b82f6', lineHeight: 1.6 }}>
            In VeristasId, a patient's identity IS their Verifiable Credential. The VC is
            cryptographically signed by the hospital's ECDSA private key. Presenting it IS the
            authentication — no server-side session, no password hash, no cookies.
          </p>
        </div>

        <div>
          <label style={S.label}>Patient VC Token (from /api/patients/register above)</label>
          <textarea
            style={{ ...S.input, height: '80px', resize: 'vertical', lineHeight: 1.5 }}
            value={vcToken}
            onChange={e => setVcToken(e.target.value)}
            placeholder="eyJhbGciOiJFUzI1NiJ9..."
          />
        </div>
        <Btn onClick={login} disabled={loading}>POST /api/auth/patient-login</Btn>
        <Result data={result} error={error} />
      </div>
    </div>
  );
}

// ─── Endpoint reference table ─────────────────────────────────────────────────
const ENDPOINTS = [
  { method: 'POST', path: '/api/patients/register',          desc: 'Register patient — issues ECDSA key pair, DID, and signed W3C VC' },
  { method: 'GET',  path: '/api/patients/auto-register-demo', desc: 'Auto-register with demo ABHA ID (no body required)' },
  { method: 'POST', path: '/api/auth/patient-login',          desc: 'Patient presents VC → identity verified, claims returned' },
  { method: 'POST', path: '/api/auth/login-paramedic',        desc: 'Issue paramedic HMAC-SHA256 JWT' },
  { method: 'POST', path: '/api/auth/login-surgeon',          desc: 'Issue surgeon HMAC-SHA256 JWT' },
  { method: 'POST', path: '/api/auth/login-dispatcher',       desc: 'Issue dispatcher JWT — only role allowed to create emergency sessions' },
];

// ─── Main page ────────────────────────────────────────────────────────────────
export default function PatientPage() {
  const [tab, setTab] = useState('register');

  return (
    <div style={S.page}>
      <div style={S.inner}>

        {/* Header */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
            <span style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '14px', fontWeight: '700', color: '#111' }}>
              /api/patients
            </span>
            <Badge method="POST" />
            <Badge method="GET" />
          </div>
          <p style={{ margin: 0, fontSize: '13px', color: '#666', lineHeight: 1.6 }}>
            Patient registration and identity verification. Patients get a cryptographic DID + W3C VC at
            registration. That VC replaces all future passwords — presenting it IS authentication.
          </p>
        </div>

        {/* Endpoint reference */}
        <div style={S.card}>
          <div style={S.cardHead}>
            <p style={S.sectionLabel}>All Endpoints</p>
          </div>
          <div style={{ padding: '0 16px' }}>
            {ENDPOINTS.map(({ method, path, desc }) => (
              <div key={path} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', padding: '10px 0', borderBottom: '1px solid #f5f5f5' }}>
                <Badge method={method} />
                <span style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '11px', color: '#111', width: '260px', flexShrink: 0 }}>{path}</span>
                <span style={{ fontSize: '12px', color: '#888', lineHeight: 1.4 }}>{desc}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Tabs */}
        <div style={{ display: 'flex', borderBottom: '1px solid #eaeaea', gap: '0' }}>
          {[
            { key: 'register', label: 'POST /api/patients/register' },
            { key: 'login',    label: 'POST /api/auth/patient-login' },
          ].map(({ key, label }) => (
            <button
              key={key}
              onClick={() => setTab(key)}
              style={{
                padding: '10px 16px', fontSize: '12px',
                fontFamily: 'JetBrains Mono, monospace',
                fontWeight: tab === key ? '600' : '400',
                color: tab === key ? '#111' : '#888',
                background: 'none', border: 'none',
                borderBottom: tab === key ? '2px solid #3b82f6' : '2px solid transparent',
                cursor: 'pointer', transition: 'color 0.1s',
              }}
            >{label}</button>
          ))}
        </div>

        {tab === 'register' && <RegistrationPanel />}
        {tab === 'login'    && <PatientLoginPanel />}

      </div>
    </div>
  );
}
