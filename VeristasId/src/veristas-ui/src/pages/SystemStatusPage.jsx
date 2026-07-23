import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  Activity, Shield, Database, CheckCircle2, XCircle,
  RefreshCw, Link, Hash, Clock, Zap, AlertTriangle
} from 'lucide-react';
import axios from 'axios';

// ─── Status badge ─────────────────────────────────────────────────────────────
function StatusBadge({ status }) {
  const cfg = {
    healthy:  { color: 'emerald', label: 'Healthy',  Icon: CheckCircle2 },
    degraded: { color: 'amber',   label: 'Degraded', Icon: AlertTriangle },
    down:     { color: 'red',     label: 'Down',     Icon: XCircle },
    checking: { color: 'sky',     label: 'Checking', Icon: RefreshCw },
  }[status] || { color: 'white', label: 'Unknown', Icon: Activity };

  const { color, label, Icon } = cfg;

  return (
    <span className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold
      bg-${color}-500/15 text-${color}-400 border border-${color}-500/30`}>
      <motion.div
        animate={status === 'checking' ? { rotate: 360 } : {}}
        transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}
      >
        <Icon className="w-3 h-3" />
      </motion.div>
      {label}
    </span>
  );
}

// ─── Service Card ─────────────────────────────────────────────────────────────
function ServiceCard({ icon: Icon, name, description, status, meta }) {
  const active = status === 'healthy';
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className="relative p-5 rounded-2xl bg-white/4 border border-white/8 overflow-hidden"
    >
      {/* Glow */}
      {active && (
        <div className="absolute -top-8 -right-8 w-24 h-24 rounded-full
          bg-emerald-500/10 blur-2xl pointer-events-none" />
      )}

      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-3">
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center
            ${active ? 'bg-emerald-500/15 border border-emerald-500/30'
                     : 'bg-white/5 border border-white/10'}`}>
            <Icon className={`w-5 h-5 ${active ? 'text-emerald-400' : 'text-white/30'}`} />
          </div>
          <div>
            <p className="text-white font-semibold text-sm">{name}</p>
            <p className="text-white/35 text-xs">{description}</p>
          </div>
        </div>
        <StatusBadge status={status} />
      </div>

      {/* Pulse line */}
      <div className="h-0.5 rounded-full bg-white/5 overflow-hidden">
        {active && (
          <motion.div
            animate={{ x: ['-100%', '200%'] }}
            transition={{ repeat: Infinity, duration: 2.2, ease: 'easeInOut' }}
            className="h-full w-1/3 rounded-full bg-gradient-to-r from-transparent via-emerald-400 to-transparent"
          />
        )}
      </div>

      {/* Meta info */}
      {meta && (
        <div className="mt-3 flex flex-wrap gap-2">
          {Object.entries(meta).map(([k, v]) => (
            <span key={k} className="text-[10px] px-2 py-0.5 rounded-md
              bg-white/5 text-white/40 font-mono">
              {k}: {v}
            </span>
          ))}
        </div>
      )}
    </motion.div>
  );
}

// ─── Blockchain integrity panel ───────────────────────────────────────────────
function ChainIntegrityPanel({ blockCount, isValid, loading, onCheck }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0, transition: { delay: 0.3 } }}
      className="p-5 rounded-2xl bg-white/4 border border-white/8"
    >
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Link className="w-4 h-4 text-indigo-400" />
          <span className="text-white font-semibold text-sm">Blockchain Audit Ledger</span>
        </div>
        <button
          onClick={onCheck}
          disabled={loading}
          className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg
            bg-indigo-500/15 border border-indigo-500/30 text-indigo-400
            hover:bg-indigo-500/25 transition-all disabled:opacity-40"
        >
          <motion.div
            animate={loading ? { rotate: 360 } : {}}
            transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}
          >
            <RefreshCw className="w-3 h-3" />
          </motion.div>
          {loading ? 'Checking…' : 'Run Integrity Check'}
        </button>
      </div>

      <div className="grid grid-cols-3 gap-3">
        {/* Block count */}
        <div className="p-3 rounded-xl bg-white/5 text-center">
          <p className="text-2xl font-bold text-white">{blockCount ?? '–'}</p>
          <p className="text-white/35 text-[10px] uppercase tracking-wide mt-0.5">Total Blocks</p>
        </div>

        {/* Integrity status */}
        <div className={`p-3 rounded-xl text-center
          ${isValid === true  ? 'bg-emerald-500/10' :
            isValid === false ? 'bg-red-500/10'     : 'bg-white/5'}`}>
          <p className={`text-2xl font-bold
            ${isValid === true ? 'text-emerald-400' : isValid === false ? 'text-red-400' : 'text-white/30'}`}>
            {isValid === null ? '–' : isValid ? '✓' : '✗'}
          </p>
          <p className="text-white/35 text-[10px] uppercase tracking-wide mt-0.5">SHA-256 Chain</p>
        </div>

        {/* Status message */}
        <div className="p-3 rounded-xl bg-white/5 text-center flex flex-col items-center justify-center">
          <Hash className={`w-5 h-5 mb-1
            ${isValid === true ? 'text-emerald-400' : isValid === false ? 'text-red-400' : 'text-white/20'}`} />
          <p className="text-white/35 text-[10px] leading-tight text-center">
            {isValid === null ? 'Run check' : isValid ? 'Untampered' : 'CORRUPTED!'}
          </p>
        </div>
      </div>

      {isValid === false && (
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="mt-3 p-3 rounded-xl bg-red-500/10 border border-red-500/30 flex items-center gap-2"
        >
          <AlertTriangle className="w-4 h-4 text-red-400 shrink-0" />
          <p className="text-red-300 text-xs">
            Chain integrity check failed. A previous audit block may have been tampered with.
          </p>
        </motion.div>
      )}
    </motion.div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function SystemStatusPage() {
  const [services, setServices]       = useState({
    backend:    'checking',
    blockchain: 'checking',
    opa:        'checking',
  });
  const [blockCount, setBlockCount]   = useState(null);
  const [chainValid, setChainValid]   = useState(null);
  const [checking, setChecking]       = useState(false);
  const [lastChecked, setLastChecked] = useState(null);

  const runChecks = async () => {
    setChecking(true);
    setServices({ backend: 'checking', blockchain: 'checking', opa: 'checking' });

    try {
      // Check backend + blockchain via /api/audit/ledger
      const ledgerResp = await axios.get('/api/audit/ledger');
      const blocks = ledgerResp.data;
      setBlockCount(Array.isArray(blocks) ? blocks.length : 0);
      setServices(s => ({ ...s, backend: 'healthy', blockchain: 'healthy' }));
    } catch {
      setServices(s => ({ ...s, backend: 'down', blockchain: 'down' }));
    }

    try {
      // Check OPA via /api/audit/verify (OPA runs during every audit call)
      const verifyResp = await axios.get('/api/audit/verify');
      const valid = verifyResp.data?.isChainMathematicallyValid ?? false;
      setChainValid(valid);
      setServices(s => ({ ...s, opa: valid ? 'healthy' : 'degraded' }));
    } catch {
      setServices(s => ({ ...s, opa: 'down' }));
    }

    setLastChecked(new Date().toLocaleTimeString());
    setChecking(false);
  };

  useEffect(() => { runChecks(); }, []);

  const serviceList = [
    {
      icon: Zap,
      name: 'Spring Boot Backend',
      description: 'REST API · Port 8080 · Railway',
      status: services.backend,
      meta: { runtime: 'Java 17', framework: 'Spring Boot 3' },
    },
    {
      icon: Database,
      name: 'PostgreSQL Database',
      description: 'Patient records · Consent ledger · VC store',
      status: services.backend,
      meta: { version: 'Postgres 15', orm: 'Hibernate/JPA' },
    },
    {
      icon: Shield,
      name: 'Open Policy Agent',
      description: 'ABAC engine · Rego policies · Zero-Trust',
      status: services.opa,
      meta: { policy: 'acute_care.rego', endpoint: '/v1/data/veristas/allow' },
    },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-6">
      <div className="max-w-2xl mx-auto space-y-6">

        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Activity className="w-6 h-6 text-indigo-400" />
              <h1 className="text-2xl font-bold text-white">System Status</h1>
            </div>
            <p className="text-white/40 text-sm">
              Live health monitoring for all backend services
            </p>
          </div>
          <div className="text-right">
            {lastChecked && (
              <div className="flex items-center gap-1.5 text-xs text-white/30">
                <Clock className="w-3 h-3" />
                Last checked: {lastChecked}
              </div>
            )}
            <button
              onClick={runChecks}
              disabled={checking}
              className="mt-2 flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg
                bg-white/5 border border-white/10 text-white/50 hover:text-white/80
                hover:bg-white/10 transition-all disabled:opacity-40"
            >
              <motion.div
                animate={checking ? { rotate: 360 } : {}}
                transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}
              >
                <RefreshCw className="w-3 h-3" />
              </motion.div>
              Refresh All
            </button>
          </div>
        </div>

        {/* Service cards */}
        <div className="space-y-3">
          {serviceList.map((svc, i) => (
            <motion.div
              key={svc.name}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0, transition: { delay: i * 0.1 } }}
            >
              <ServiceCard {...svc} />
            </motion.div>
          ))}
        </div>

        {/* Blockchain integrity */}
        <ChainIntegrityPanel
          blockCount={blockCount}
          isValid={chainValid}
          loading={checking}
          onCheck={runChecks}
        />

        {/* Day 14 API reference */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1, transition: { delay: 0.5 } }}
          className="p-4 rounded-2xl bg-white/4 border border-white/8"
        >
          <p className="text-white/40 text-xs font-semibold uppercase tracking-wide mb-3">
            Day 14 — Endpoints Tested Today
          </p>
          {[
            { method: 'POST', path: '/api/patients/register',         desc: 'Register patient → issue + store VC' },
            { method: 'GET',  path: '/api/patients/auto-register-demo', desc: 'Register demo patient automatically' },
          ].map(({ method, path, desc }) => (
            <div key={path} className="flex items-center gap-3 py-1.5">
              <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded font-mono
                ${method === 'POST' ? 'bg-sky-500/15 text-sky-400' : 'bg-emerald-500/15 text-emerald-400'}`}>
                {method}
              </span>
              <span className="font-mono text-xs text-white/50">{path}</span>
              <span className="text-white/25 text-xs ml-auto hidden sm:block">{desc}</span>
            </div>
          ))}
        </motion.div>

      </div>
    </div>
  );
}
