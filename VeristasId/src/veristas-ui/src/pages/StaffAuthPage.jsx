import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Shield, Stethoscope, Radio, Key, Copy, CheckCircle2, ChevronRight, Lock } from 'lucide-react';
import axios from 'axios';

// ─── JWT Decoder ──────────────────────────────────────────────────────────────
function decodeJwtPayload(token) {
  try {
    const raw = token.replace(/^Bearer /, '');
    const parts = raw.split('.');
    if (parts.length < 2) return null;
    const payload = parts[1];
    const padded = payload + '='.repeat((4 - payload.length % 4) % 4);
    return JSON.parse(atob(padded.replace(/-/g, '+').replace(/_/g, '/')));
  } catch {
    return null;
  }
}

// ─── Role config ──────────────────────────────────────────────────────────────
const ROLES = [
  {
    key: 'paramedic',
    label: 'Paramedic',
    endpoint: '/api/auth/login-paramedic',
    Icon: Shield,
    color: 'sky',
    gradient: 'from-sky-500/20 to-sky-600/5',
    border: 'border-sky-500/30',
    accent: 'text-sky-400',
    iconBg: 'bg-sky-500/15',
    description: 'Emergency Response Unit · Read-only field access',
    badge: 'EMT-9110',
  },
  {
    key: 'surgeon',
    label: 'Surgeon',
    endpoint: '/api/auth/login-surgeon',
    Icon: Stethoscope,
    color: 'violet',
    gradient: 'from-violet-500/20 to-violet-600/5',
    border: 'border-violet-500/30',
    accent: 'text-violet-400',
    iconBg: 'bg-violet-500/15',
    description: 'Operating Theatre · Full EMR write access',
    badge: 'SURG-1000',
  },
  {
    key: 'dispatcher',
    label: 'Dispatcher',
    endpoint: '/api/auth/login-dispatcher',
    Icon: Radio,
    color: 'amber',
    gradient: 'from-amber-500/20 to-amber-600/5',
    border: 'border-amber-500/30',
    accent: 'text-amber-400',
    iconBg: 'bg-amber-500/15',
    description: 'Control Room Alpha · Emergency session authority',
    badge: 'DISP-0001',
  },
];

// ─── Token Display ────────────────────────────────────────────────────────────
function TokenDisplay({ token, accent, color }) {
  const [copied, setCopied] = useState(false);
  const payload = decodeJwtPayload(token);
  const rawJwt = token.replace(/^Bearer /, '');

  const handleCopy = () => {
    navigator.clipboard.writeText(rawJwt);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const [header, body, sig] = rawJwt.split('.');

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="mt-4 space-y-3"
    >
      {/* Raw JWT with coloured parts */}
      <div className="rounded-xl bg-black/30 border border-white/8 p-3">
        <div className="flex items-center justify-between mb-2">
          <span className="text-white/30 text-[10px] uppercase tracking-widest font-semibold">
            JWT Token
          </span>
          <button
            onClick={handleCopy}
            className={`flex items-center gap-1 text-[10px] px-2 py-0.5 rounded
              border transition-all ${copied
                ? 'bg-emerald-500/15 border-emerald-500/30 text-emerald-400'
                : `bg-white/5 border-white/10 text-white/30 hover:text-white/60`}`}
          >
            {copied ? <CheckCircle2 className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
            {copied ? 'Copied' : 'Copy'}
          </button>
        </div>
        <p className="font-mono text-[10px] break-all leading-relaxed">
          <span className="text-rose-400">{header}</span>
          <span className="text-white/20">.</span>
          <span className="text-amber-400">{body}</span>
          <span className="text-white/20">.</span>
          <span className="text-emerald-400">{sig}</span>
        </p>
        <div className="flex gap-3 mt-2 text-[9px]">
          <span className="text-rose-400/60">■ Header</span>
          <span className="text-amber-400/60">■ Payload</span>
          <span className="text-emerald-400/60">■ Signature</span>
        </div>
      </div>

      {/* Decoded payload */}
      {payload && (
        <div className="rounded-xl bg-black/20 border border-white/8 p-3">
          <p className="text-white/30 text-[10px] uppercase tracking-widest font-semibold mb-2">
            Decoded Payload
          </p>
          <div className="space-y-1">
            {Object.entries(payload).map(([k, v]) => (
              <div key={k} className="flex items-start gap-2 font-mono text-xs">
                <span className="text-white/30 shrink-0">{k}:</span>
                <span className={`${accent} break-all`}>
                  {typeof v === 'number' && k === 'exp'
                    ? new Date(v * 1000).toLocaleString()
                    : String(v)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </motion.div>
  );
}

// ─── Role Card ────────────────────────────────────────────────────────────────
function RoleCard({ role }) {
  const { label, endpoint, Icon, gradient, border, accent, iconBg, description, badge, color } = role;
  const [loading, setLoading]     = useState(false);
  const [token, setToken]         = useState(null);
  const [error, setError]         = useState(null);

  const handleLogin = async () => {
    setLoading(true);
    setError(null);
    setToken(null);
    try {
      const { data } = await axios.post(endpoint);
      setToken(data.token);
    } catch (e) {
      setError(e.response?.data?.message || 'Backend unreachable — check Railway is running.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className={`rounded-2xl bg-gradient-to-br ${gradient} border ${border} p-5`}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className={`w-10 h-10 rounded-xl ${iconBg} border ${border} flex items-center justify-center`}>
            <Icon className={`w-5 h-5 ${accent}`} />
          </div>
          <div>
            <p className="text-white font-semibold text-sm">{label}</p>
            <p className="text-white/35 text-xs">{description}</p>
          </div>
        </div>
        <span className={`font-mono text-[10px] px-2 py-0.5 rounded border ${border} ${accent} bg-black/20`}>
          {badge}
        </span>
      </div>

      {/* Login button */}
      <button
        onClick={handleLogin}
        disabled={loading}
        className={`w-full flex items-center justify-between px-4 py-2.5 rounded-xl
          border ${border} ${accent} bg-black/20 hover:bg-black/30
          transition-all text-sm font-medium disabled:opacity-50`}
      >
        <div className="flex items-center gap-2">
          <Key className="w-4 h-4" />
          {loading ? 'Generating JWT…' : token ? 'Re-issue Token' : 'Issue JWT Token'}
        </div>
        <ChevronRight className="w-4 h-4 opacity-50" />
      </button>

      {/* Error */}
      {error && (
        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="mt-2 text-red-400 text-xs"
        >
          {error}
        </motion.p>
      )}

      {/* Token display */}
      <AnimatePresence>
        {token && <TokenDisplay token={token} accent={accent} color={color} />}
      </AnimatePresence>
    </motion.div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function StaffAuthPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-6">
      <div className="max-w-2xl mx-auto space-y-6">

        {/* Header */}
        <div className="flex items-center gap-3 mb-2">
          <div className="w-10 h-10 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center">
            <Lock className="w-5 h-5 text-white/50" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">Staff Authentication</h1>
            <p className="text-white/40 text-sm">Issue signed JWTs per role · Day 15 endpoint tests</p>
          </div>
        </div>

        {/* Role explanation banner */}
        <div className="p-4 rounded-2xl bg-white/3 border border-white/8">
          <p className="text-white/40 text-xs font-semibold uppercase tracking-wide mb-2">How It Works</p>
          <p className="text-white/50 text-xs leading-relaxed">
            Click a role card below to call <span className="font-mono text-white/70">POST /api/auth/login-*</span>.
            The backend generates an HMAC-SHA256 signed JWT embedding the role, name, and badge.
            The <span className="text-amber-400 font-mono">Payload</span> section is Base64-decoded from the token
            and shows the raw claims. The <span className="text-emerald-400 font-mono">Signature</span> is the
            cryptographic seal — only the server with the secret key can create or verify it.
          </p>
        </div>

        {/* Role cards */}
        <div className="space-y-4">
          {ROLES.map(role => (
            <RoleCard key={role.key} role={role} />
          ))}
        </div>

        {/* API reference */}
        <div className="p-4 rounded-2xl bg-white/3 border border-white/8">
          <p className="text-white/40 text-xs font-semibold uppercase tracking-wide mb-3">
            Day 15 — Endpoints Tested
          </p>
          {[
            { method: 'GET',    path: '/api/audit/ledger',             desc: 'Full blockchain ledger' },
            { method: 'GET',    path: '/api/audit/verify',             desc: 'SHA-256 chain integrity check' },
            { method: 'DELETE', path: '/api/audit/reset',              desc: 'Wipe and reinitialise chain' },
            { method: 'POST',   path: '/api/auth/login-paramedic',     desc: 'Issue paramedic JWT' },
            { method: 'POST',   path: '/api/auth/login-surgeon',       desc: 'Issue surgeon JWT' },
            { method: 'POST',   path: '/api/auth/login-dispatcher',    desc: 'Issue dispatcher JWT + role field' },
          ].map(({ method, path, desc }) => (
            <div key={path} className="flex items-center gap-3 py-1.5 border-b border-white/5 last:border-0">
              <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded font-mono w-14 text-center
                ${method === 'GET'    ? 'bg-emerald-500/15 text-emerald-400' :
                  method === 'DELETE' ? 'bg-red-500/15 text-red-400' :
                                        'bg-sky-500/15 text-sky-400'}`}>
                {method}
              </span>
              <span className="font-mono text-xs text-white/50">{path}</span>
              <span className="text-white/20 text-xs ml-auto hidden sm:block">{desc}</span>
            </div>
          ))}
        </div>

      </div>
    </div>
  );
}
