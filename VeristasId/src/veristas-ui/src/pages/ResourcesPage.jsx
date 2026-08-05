import React, { useState } from 'react';
import axios from 'axios';

// ─── Shared minimal styles ────────────────────────────────────────────────────
const page = {
  minHeight: '100vh', background: '#fafafa', fontFamily: "'Inter', sans-serif",
  padding: '40px 24px',
};
const card = {
  border: '1px solid #eaeaea', borderRadius: '10px', background: '#fff',
  overflow: 'hidden',
};
const sectionTitle = {
  fontSize: '11px', color: '#999', fontFamily: 'JetBrains Mono, monospace',
  letterSpacing: '0.07em', textTransform: 'uppercase', margin: 0,
};
const label = { fontSize: '11px', color: '#666', marginBottom: '6px', display: 'block' };
const input = {
  width: '100%', padding: '9px 12px', border: '1px solid #eaeaea',
  borderRadius: '6px', fontSize: '12px', fontFamily: 'JetBrains Mono, monospace',
  color: '#111', background: '#fafafa', outline: 'none', boxSizing: 'border-box',
};
const METHOD = {
  GET:    { bg: '#f0fdf4', color: '#15803d', border: '#bbf7d0' },
  POST:   { bg: '#eff6ff', color: '#1d4ed8', border: '#bfdbfe' },
  PUT:    { bg: '#fffbeb', color: '#b45309', border: '#fde68a' },
  DELETE: { bg: '#fef2f2', color: '#b91c1c', border: '#fecaca' },
};

function MethodBadge({ method }) {
  const s = METHOD[method] || METHOD.GET;
  return (
    <span style={{
      padding: '2px 7px', borderRadius: '4px', fontSize: '10px', fontWeight: '600',
      fontFamily: 'JetBrains Mono, monospace', letterSpacing: '0.04em',
      background: s.bg, color: s.color, border: `1px solid ${s.border}`,
    }}>
      {method}
    </span>
  );
}

function EndpointRow({ method, path, desc, active }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'flex-start', gap: '12px', padding: '11px 0',
      borderBottom: '1px solid #f0f0f0',
    }}>
      <MethodBadge method={method} />
      <span style={{
        fontFamily: 'JetBrains Mono, monospace', fontSize: '11px', color: '#111',
        flex: '0 0 240px',
      }}>{path}</span>
      <span style={{ fontSize: '12px', color: '#888', lineHeight: '1.4' }}>{desc}</span>
      {active && (
        <span style={{
          marginLeft: 'auto', fontSize: '10px', color: '#22c55e',
          fontFamily: 'JetBrains Mono, monospace', flexShrink: 0,
        }}>● live</span>
      )}
    </div>
  );
}

// ─── /api/resources/secure-report demo ───────────────────────────────────────
export default function ResourcesPage() {
  const [vcToken, setVcToken]   = useState('');
  const [simTime, setSimTime]   = useState('');
  const [result, setResult]     = useState(null);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState(null);

  const callEndpoint = async () => {
    if (!vcToken.trim()) { setError('Paste a Verifiable Credential token first.'); return; }
    setLoading(true); setResult(null); setError(null);
    try {
      const headers = { Authorization: vcToken.startsWith('Bearer ') ? vcToken : `Bearer ${vcToken}` };
      if (simTime.trim()) headers['X-Simulated-Time'] = simTime.trim();
      const { data } = await axios.get('/api/resources/secure-report', { headers });
      setResult({ ok: true, body: data });
    } catch (e) {
      const status = e.response?.status;
      const body   = e.response?.data || e.message;
      setResult({ ok: false, status, body });
    } finally { setLoading(false); }
  };

  return (
    <div style={page}>
      <div style={{ maxWidth: '720px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '32px' }}>

        {/* ── Header ── */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '13px', fontWeight: '600', color: '#111' }}>
              /api/resources
            </span>
            <MethodBadge method="GET" />
          </div>
          <p style={{ margin: 0, fontSize: '13px', color: '#666', lineHeight: 1.6 }}>
            Zero-Trust protected resource endpoint. Runs the full pipeline:
            VC identity check → OPA ABAC decision → immutable audit log.
          </p>
        </div>

        {/* ── Endpoint table ── */}
        <div style={card}>
          <div style={{ padding: '12px 16px', borderBottom: '1px solid #eaeaea', background: '#fafafa' }}>
            <p style={sectionTitle}>Endpoints</p>
          </div>
          <div style={{ padding: '0 16px' }}>
            <EndpointRow
              method="GET"
              path="/api/resources/secure-report"
              desc="Full zero-trust pipeline: verify VC → OPA ABAC → audit log. Returns clinical data or 403."
              active
            />
          </div>
        </div>

        {/* ── Zero-Trust pipeline explained ── */}
        <div style={card}>
          <div style={{ padding: '12px 16px', borderBottom: '1px solid #eaeaea', background: '#fafafa' }}>
            <p style={sectionTitle}>How Every Request Is Processed</p>
          </div>
          <div style={{ padding: '0' }}>
            {[
              ['1', 'Identity Verification',   'CredentialService.checkRevocationStatus(token) — VC must be VALID and not revoked.', '#3b82f6'],
              ['2', 'Attribute Extraction',     'credService.extractClaims(token) — pulls role, subject, and clearance from the VC payload.', '#8b5cf6'],
              ['3', 'OPA ABAC Decision',        'OpaService.checkAccess(user, resource, stage) — Rego policy evaluates role + session stage + clearance.', '#f59e0b'],
              ['4', 'Immutable Audit Log',      'AuditService.logDecision(subject, resource, decision) — every access burned into the SHA-256 chain.', '#22c55e'],
              ['5', 'Response',                 '200 ACCESS GRANTED or 403 ACCESS DENIED — the decision is always audited regardless of outcome.', '#111'],
            ].map(([n, title, desc, color]) => (
              <div key={n} style={{ display: 'flex', gap: '16px', padding: '14px 16px', borderBottom: '1px solid #f5f5f5' }}>
                <span style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '11px', color, flexShrink: 0, width: '16px' }}>{n}</span>
                <div>
                  <p style={{ margin: '0 0 3px', fontSize: '12px', fontWeight: '600', color: '#111' }}>{title}</p>
                  <p style={{ margin: 0, fontSize: '12px', color: '#666', lineHeight: 1.5 }}>{desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* ── Live Demo ── */}
        <div style={card}>
          <div style={{ padding: '12px 16px', borderBottom: '1px solid #eaeaea', background: '#fafafa' }}>
            <p style={sectionTitle}>Live Demo — GET /api/resources/secure-report</p>
          </div>
          <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '14px' }}>

            <div>
              <label style={label}>Verifiable Credential Token</label>
              <input
                style={input}
                placeholder="Paste VC token or JWT from /api/auth or /api/vc"
                value={vcToken}
                onChange={e => setVcToken(e.target.value)}
              />
            </div>

            <div>
              <label style={label}>X-Simulated-Time (optional — simulates OPA time-based policy)</label>
              <input
                style={input}
                placeholder="e.g. dispatched"
                value={simTime}
                onChange={e => setSimTime(e.target.value)}
              />
            </div>

            <button
              onClick={callEndpoint}
              disabled={loading}
              style={{
                padding: '10px 20px', background: loading ? '#e5e7eb' : '#3b82f6',
                border: 'none', borderRadius: '6px', color: loading ? '#9ca3af' : '#fff',
                fontSize: '13px', fontWeight: '500', cursor: loading ? 'not-allowed' : 'pointer',
                transition: 'background 0.15s', alignSelf: 'flex-start',
              }}
            >
              {loading ? 'Requesting…' : 'GET /api/resources/secure-report'}
            </button>

            {error && (
              <div style={{ padding: '12px 14px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px' }}>
                <p style={{ margin: 0, fontSize: '12px', color: '#b91c1c', fontFamily: 'JetBrains Mono, monospace' }}>{error}</p>
              </div>
            )}

            {result && (
              <div style={{
                padding: '14px', background: result.ok ? '#f0fdf4' : '#fef2f2',
                border: `1px solid ${result.ok ? '#bbf7d0' : '#fecaca'}`, borderRadius: '6px',
              }}>
                <p style={{ margin: '0 0 6px', fontSize: '11px', fontFamily: 'JetBrains Mono, monospace', color: result.ok ? '#15803d' : '#b91c1c' }}>
                  {result.ok ? '200 OK' : `${result.status} DENIED`}
                </p>
                <p style={{ margin: 0, fontSize: '12px', color: '#111', fontFamily: 'JetBrains Mono, monospace' }}>
                  {typeof result.body === 'string' ? result.body : JSON.stringify(result.body, null, 2)}
                </p>
              </div>
            )}
          </div>
        </div>

        {/* ── Security architecture note ── */}
        <div style={{ padding: '14px 16px', border: '1px solid #eaeaea', borderRadius: '8px', background: '#fff' }}>
          <p style={{ ...sectionTitle, marginBottom: '8px' }}>Why this matters for the resume</p>
          <p style={{ margin: 0, fontSize: '12px', color: '#555', lineHeight: 1.7 }}>
            Most projects do authentication (is this user logged in?) but not authorisation (is this specific user
            allowed to perform this specific action on this specific resource right now?). This endpoint demonstrates
            full <strong>ABAC</strong> — every request is evaluated by an external policy engine (OPA) against the
            current emergency session stage. Change the stage, and the same token gets a different decision.
          </p>
        </div>

      </div>
    </div>
  );
}
