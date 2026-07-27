import React, { useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Link2, ShieldCheck, ShieldAlert, RefreshCw, Hash,
  User, Activity, Clock, ChevronDown, Lock, Zap
} from 'lucide-react';
import axios from 'axios';

// ─── Hex color for action type ────────────────────────────────────────────────
const ACTION_STYLE = {
  GENESIS:       { bg: 'bg-slate-500/15',  border: 'border-slate-500/30',  text: 'text-slate-400'  },
  READ:          { bg: 'bg-sky-500/15',    border: 'border-sky-500/30',    text: 'text-sky-400'    },
  WRITE:         { bg: 'bg-violet-500/15', border: 'border-violet-500/30', text: 'text-violet-400' },
  EMR_WRITTEN:   { bg: 'bg-violet-500/15', border: 'border-violet-500/30', text: 'text-violet-400' },
  EMR_DELETED:   { bg: 'bg-red-500/15',    border: 'border-red-500/30',    text: 'text-red-400'    },
  PERMIT:        { bg: 'bg-emerald-500/15',border: 'border-emerald-500/30',text: 'text-emerald-400'},
  DENY:          { bg: 'bg-red-500/15',    border: 'border-red-500/30',    text: 'text-red-400'    },
};
const getActionStyle = (action) =>
  ACTION_STYLE[action?.toUpperCase()] || ACTION_STYLE.READ;

// ─── Truncate hash ─────────────────────────────────────────────────────────────
const trunc = (hash, n = 10) => hash ? `${hash.slice(0, n)}…` : '—';

// ─── Single Block Card ────────────────────────────────────────────────────────
function BlockCard({ block, index, isLast, isFirst }) {
  const [expanded, setExpanded] = useState(false);
  const style = getActionStyle(block.action);

  return (
    <div className="flex flex-col items-center">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: index * 0.05 }}
        className={`w-full rounded-2xl border overflow-hidden
          ${isFirst
            ? 'bg-amber-500/8 border-amber-500/25'
            : 'bg-white/4 border-white/10'}
          hover:border-white/20 transition-all cursor-pointer`}
        onClick={() => setExpanded(v => !v)}
      >
        {/* Block header */}
        <div className="px-4 py-3 flex items-center justify-between gap-3">
          {/* Index badge */}
          <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 text-xs font-bold
            ${isFirst ? 'bg-amber-500/20 text-amber-400' : 'bg-indigo-500/15 text-indigo-400'}`}>
            #{block.index}
          </div>

          {/* Hash preview */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-1.5 mb-0.5">
              <Hash className="w-3 h-3 text-white/30 shrink-0" />
              <span className="text-white/70 text-xs font-mono truncate">{trunc(block.hash, 16)}</span>
            </div>
            {!isFirst && (
              <div className="flex items-center gap-1.5">
                <Link2 className="w-3 h-3 text-white/20 shrink-0" />
                <span className="text-white/30 text-xs font-mono truncate">{trunc(block.previousHash, 16)}</span>
              </div>
            )}
          </div>

          {/* Action + granted badge */}
          <div className="flex items-center gap-2 shrink-0">
            <span className={`px-2 py-0.5 rounded-full text-xs font-semibold border ${style.bg} ${style.border} ${style.text}`}>
              {block.action}
            </span>
            {block.index > 0 && (
              <span className={`w-5 h-5 rounded-full flex items-center justify-center text-xs
                ${block.accessGranted ? 'bg-emerald-500/20 text-emerald-400' : 'bg-red-500/20 text-red-400'}`}>
                {block.accessGranted ? '✓' : '✗'}
              </span>
            )}
            <ChevronDown className={`w-3.5 h-3.5 text-white/30 transition-transform ${expanded ? 'rotate-180' : ''}`} />
          </div>
        </div>

        {/* Expanded detail */}
        <AnimatePresence>
          {expanded && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="border-t border-white/6 px-4 py-3 space-y-2"
            >
              <DetailRow icon={User}     label="Accessor"      value={block.accessorId} />
              <DetailRow icon={Activity} label="Target (ABHA)" value={block.targetAbhaId} />
              <DetailRow icon={Clock}    label="Timestamp"     value={block.timestamp ? new Date(block.timestamp).toLocaleString() : '—'} />
              <DetailRow icon={Hash}     label="Full Hash"     value={block.hash} mono />
              {!isFirst && <DetailRow icon={Link2} label="Previous Hash" value={block.previousHash} mono />}
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>

      {/* Chain link arrow between blocks */}
      {!isLast && (
        <div className="flex flex-col items-center py-1">
          <div className="w-px h-4 bg-gradient-to-b from-white/20 to-white/5" />
          <Link2 className="w-3 h-3 text-white/20" />
          <div className="w-px h-4 bg-gradient-to-b from-white/5 to-white/20" />
        </div>
      )}
    </div>
  );
}

function DetailRow({ icon: Icon, label, value, mono }) {
  return (
    <div className="flex gap-2.5 items-start">
      <div className="w-5 h-5 rounded flex items-center justify-center shrink-0 bg-white/5 mt-0.5">
        <Icon className="w-3 h-3 text-white/30" />
      </div>
      <div className="min-w-0">
        <p className="text-white/30 text-[10px] uppercase tracking-wide">{label}</p>
        <p className={`text-white/70 text-xs break-all mt-0.5 ${mono ? 'font-mono' : ''}`}>{value || '—'}</p>
      </div>
    </div>
  );
}

// ─── Integrity Banner ──────────────────────────────────────────────────────────
function IntegrityBanner({ valid }) {
  if (valid === null) return null;
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className={`flex items-center gap-3 p-4 rounded-2xl border
        ${valid
          ? 'bg-emerald-500/10 border-emerald-500/30'
          : 'bg-red-500/10 border-red-500/30'}`}
    >
      {valid
        ? <ShieldCheck className="w-5 h-5 text-emerald-400 shrink-0" />
        : <ShieldAlert className="w-5 h-5 text-red-400 shrink-0" />}
      <div>
        <p className={`font-bold text-sm ${valid ? 'text-emerald-400' : 'text-red-400'}`}>
          {valid ? 'Chain Integrity: VALID ✓' : 'Chain Integrity: TAMPERED ✗'}
        </p>
        <p className="text-white/40 text-xs mt-0.5">
          {valid
            ? 'All SHA-256 hashes verified — no tampering detected across the full audit chain.'
            : 'ALERT: Hash mismatch detected. At least one block has been modified after sealing.'}
        </p>
      </div>
    </motion.div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function ChainVisualizerPage() {
  const [blocks, setBlocks]     = useState([]);
  const [valid, setValid]       = useState(null);
  const [loading, setLoading]   = useState(false);
  const [checking, setChecking] = useState(false);
  const [error, setError]       = useState(null);

  const fetchLedger = useCallback(async () => {
    setLoading(true);
    setError(null);
    setValid(null);
    try {
      const { data } = await axios.get('/api/audit/ledger');
      // Support both plain array and { chain: [...] } response shapes
      setBlocks(Array.isArray(data) ? data : (data.chain || []));
    } catch {
      setError('Backend unreachable — ensure Railway is running.');
    } finally {
      setLoading(false);
    }
  }, []);

  const verifyIntegrity = useCallback(async () => {
    setChecking(true);
    setValid(null);
    try {
      const { data } = await axios.get('/api/audit/integrity');
      // API returns { valid: true } or { chainIntact: true } or just true
      const result =
        typeof data === 'boolean' ? data :
        data.valid !== undefined  ? data.valid :
        data.chainIntact !== undefined ? data.chainIntact :
        data.status === 'INTACT';
      setValid(result);
    } catch {
      setError('Integrity check failed — backend unreachable.');
    } finally {
      setChecking(false);
    }
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-6">
      <div className="max-w-2xl mx-auto space-y-5">

        {/* Header */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center">
            <Link2 className="w-5 h-5 text-amber-400" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">Chain Visualizer</h1>
            <p className="text-white/40 text-sm">Live SHA-256 blockchain · GET /api/audit/ledger</p>
          </div>
        </div>

        {/* Action buttons */}
        <div className="flex gap-3">
          <button
            onClick={fetchLedger}
            disabled={loading}
            className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl
              bg-amber-500/10 border border-amber-500/25 text-amber-400 text-sm font-medium
              hover:bg-amber-500/20 transition-all disabled:opacity-50"
          >
            {loading
              ? <motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}><RefreshCw className="w-4 h-4" /></motion.div>
              : <Zap className="w-4 h-4" />}
            {loading ? 'Loading…' : 'Load Blockchain'}
          </button>

          <button
            onClick={verifyIntegrity}
            disabled={checking || blocks.length === 0}
            className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl
              bg-emerald-500/10 border border-emerald-500/25 text-emerald-400 text-sm font-medium
              hover:bg-emerald-500/20 transition-all disabled:opacity-50"
          >
            {checking
              ? <motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}><RefreshCw className="w-4 h-4" /></motion.div>
              : <ShieldCheck className="w-4 h-4" />}
            {checking ? 'Verifying…' : 'Verify Integrity'}
          </button>
        </div>

        {/* Integrity result */}
        <IntegrityBanner valid={valid} />

        {/* Error */}
        {error && (
          <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/25 text-red-300 text-sm">
            {error}
          </div>
        )}

        {/* Stats bar */}
        {blocks.length > 0 && (
          <div className="grid grid-cols-3 gap-3 p-4 rounded-2xl bg-white/3 border border-white/8">
            <div className="text-center">
              <p className="text-2xl font-bold text-amber-400">{blocks.length}</p>
              <p className="text-white/40 text-xs">Total Blocks</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-emerald-400">
                {blocks.filter(b => b.accessGranted).length}
              </p>
              <p className="text-white/40 text-xs">Granted</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-red-400">
                {blocks.filter(b => !b.accessGranted).length}
              </p>
              <p className="text-white/40 text-xs">Denied</p>
            </div>
          </div>
        )}

        {/* Legend */}
        {blocks.length > 0 && (
          <div className="flex flex-wrap gap-2 text-xs">
            <span className="text-white/30">Click any block to expand full hashes.</span>
            <span className="px-2 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/25 text-amber-400">
              #0 = Genesis
            </span>
            <span className="px-2 py-0.5 rounded-full bg-emerald-500/10 border border-emerald-500/25 text-emerald-400">
              ✓ Granted
            </span>
            <span className="px-2 py-0.5 rounded-full bg-red-500/10 border border-red-500/25 text-red-400">
              ✗ Denied
            </span>
          </div>
        )}

        {/* Chain blocks */}
        <div className="space-y-0">
          {blocks.map((block, i) => (
            <BlockCard
              key={block.index ?? i}
              block={block}
              index={i}
              isFirst={i === 0}
              isLast={i === blocks.length - 1}
            />
          ))}
        </div>

        {/* Empty state */}
        {!loading && blocks.length === 0 && !error && (
          <div className="text-center py-16">
            <div className="w-16 h-16 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center mx-auto mb-4">
              <Lock className="w-8 h-8 text-amber-400/50" />
            </div>
            <p className="text-white/30 text-sm">Click "Load Blockchain" to fetch the audit chain</p>
            <p className="text-white/15 text-xs mt-1">Each block contains a SHA-256 fingerprint of the previous one</p>
          </div>
        )}

        {/* Day 18 explainer */}
        <div className="p-4 rounded-2xl bg-white/3 border border-white/8 space-y-3">
          <p className="text-white/40 text-xs font-semibold uppercase tracking-wide">Day 18 — Three-Layer Audit Architecture</p>
          <div className="grid grid-cols-3 gap-2">
            {[
              { n: 1, label: 'In-Memory Chain', desc: 'SHA-256 linked list (instant reads)', color: 'indigo' },
              { n: 2, label: 'PostgreSQL',       desc: 'Durable DB (survives restarts)',     color: 'sky' },
              { n: 3, label: 'Ethereum',         desc: 'On-chain via VeristasAudit.sol',     color: 'violet' },
            ].map(({ n, label, desc, color }) => (
              <div key={n} className={`p-3 rounded-xl bg-${color}-500/8 border border-${color}-500/20`}>
                <p className={`text-${color}-400 text-xs font-bold`}>Layer {n}</p>
                <p className="text-white/60 text-xs font-medium mt-1">{label}</p>
                <p className="text-white/25 text-[10px] mt-0.5 leading-tight">{desc}</p>
              </div>
            ))}
          </div>
        </div>

      </div>
    </div>
  );
}
