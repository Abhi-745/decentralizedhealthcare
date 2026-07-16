import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Shield, ShieldCheck, ShieldX, Hash, Clock, User,
  ChevronDown, ChevronUp, Link, CheckCircle2, XCircle,
  AlertTriangle, Database, RefreshCw, Lock
} from 'lucide-react';
import GlassCard from '../components/GlassCard';
import StatusBadge from '../components/StatusBadge';
import { useToast } from '../components/Toast';

// ─── Seed audit blocks — mirrors the SHA-256 linked chain in BlockchainAuditService ──
const SEED_BLOCKS = [
  {
    index: 0,
    timestamp: Date.now() - 7200000,
    actor: 'SYSTEM',
    patientId: 'GENESIS',
    action: 'CHAIN_INIT',
    success: true,
    previousHash: '0000000000000000',
    hash: 'a3f8d2c1e9b74506',
  },
  {
    index: 1,
    timestamp: Date.now() - 5400000,
    actor: 'Dispatcher_JWT',
    patientId: 'PAT-9981',
    action: 'EMERGENCY_CREATE',
    success: true,
    previousHash: 'a3f8d2c1e9b74506',
    hash: 'b7e1a4c3d8f92015',
  },
  {
    index: 2,
    timestamp: Date.now() - 3600000,
    actor: 'Paramedic_JWT',
    patientId: 'PAT-9981',
    action: 'READ',
    success: true,
    previousHash: 'b7e1a4c3d8f92015',
    hash: 'c9d5b2e7a1f43028',
  },
  {
    index: 3,
    timestamp: Date.now() - 1800000,
    actor: 'Anonymous',
    patientId: 'PAT-1123',
    action: 'READ',
    success: false,
    previousHash: 'c9d5b2e7a1f43028',
    hash: 'd2f6c4a8b3e51047',
  },
  {
    index: 4,
    timestamp: Date.now() - 900000,
    actor: 'Surgeon_JWT',
    patientId: 'PAT-9981',
    action: 'WRITE',
    success: true,
    previousHash: 'd2f6c4a8b3e51047',
    hash: 'e4a7d1c9b6f82056',
  },
];

// ─── Helpers ──────────────────────────────────────────────────────────────────
function timeAgo(ms) {
  const diff = Math.floor((Date.now() - ms) / 1000);
  if (diff < 60)   return `${diff}s ago`;
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  return `${Math.floor(diff / 3600)}h ago`;
}

function actionColor(action, success) {
  if (!success) return 'rose';
  if (action === 'WRITE')          return 'amber';
  if (action === 'CHAIN_INIT')     return 'indigo';
  if (action === 'EMERGENCY_CREATE') return 'sky';
  return 'emerald';
}

// ─── Chain Integrity Banner ───────────────────────────────────────────────────
function IntegrityBanner({ blocks }) {
  const [verifying, setVerifying] = useState(false);
  const [result, setResult]     = useState(null);
  const toast = useToast();

  const handleVerify = async () => {
    setVerifying(true);
    setResult(null);
    await new Promise(r => setTimeout(r, 1600));

    // Simulate SHA-256 chain verification
    let valid = true;
    for (let i = 1; i < blocks.length; i++) {
      if (blocks[i].previousHash !== blocks[i - 1].hash) {
        valid = false;
        break;
      }
    }
    setResult(valid);
    setVerifying(false);
    if (valid) toast.success('Chain Verified', 'All blocks are mathematically intact');
    else       toast.error('Corruption Detected', 'Hash mismatch found in audit chain');
  };

  return (
    <GlassCard className="p-5">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-3">
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center border
            ${result === false
              ? 'bg-rose-500/15 border-rose-500/30'
              : 'bg-emerald-500/15 border-emerald-500/30'}`}>
            <Shield className={`w-5 h-5 ${result === false ? 'text-rose-400' : 'text-emerald-400'}`} />
          </div>
          <div>
            <p className="text-white font-semibold text-sm">SHA-256 Audit Chain</p>
            <p className="text-white/40 text-xs">
              {blocks.length} blocks · Each block cryptographically linked to its predecessor
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {result !== null && (
            <motion.div
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium
                ${result
                  ? 'bg-emerald-500/15 border border-emerald-500/30 text-emerald-300'
                  : 'bg-rose-500/15 border border-rose-500/30 text-rose-300'}`}
            >
              {result ? <CheckCircle2 className="w-3.5 h-3.5" /> : <XCircle className="w-3.5 h-3.5" />}
              {result ? 'Chain Intact' : 'Corruption Detected'}
            </motion.div>
          )}

          <button
            onClick={handleVerify}
            disabled={verifying}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-indigo-500/15 border
              border-indigo-500/30 text-indigo-300 text-sm font-medium hover:bg-indigo-500/25
              transition-all disabled:opacity-50"
          >
            <motion.div animate={verifying ? { rotate: 360 } : {}}
              transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}>
              <RefreshCw className="w-3.5 h-3.5" />
            </motion.div>
            {verifying ? 'Verifying…' : 'Verify Chain'}
          </button>
        </div>
      </div>
    </GlassCard>
  );
}

// ─── Block Card ───────────────────────────────────────────────────────────────
function BlockCard({ block, isExpanded, onToggle }) {
  const color = actionColor(block.action, block.success);
  const isGenesis = block.index === 0;

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: block.index * 0.05 }}
    >
      {/* Chain link connector (not shown on genesis block) */}
      {!isGenesis && (
        <div className="flex justify-center my-0">
          <div className="flex flex-col items-center">
            <div className="w-px h-4 bg-white/10" />
            <Link className="w-3 h-3 text-white/20" />
            <div className="w-px h-4 bg-white/10" />
          </div>
        </div>
      )}

      <GlassCard className="p-4 cursor-pointer hover:border-white/20 transition-all"
        onClick={onToggle}>
        {/* Block header row */}
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            {/* Block index badge */}
            <div className={`w-9 h-9 rounded-lg flex items-center justify-center shrink-0
              bg-${color}-500/15 border border-${color}-500/30`}>
              <span className={`text-xs font-bold text-${color}-400`}>#{block.index}</span>
            </div>

            <div className="min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className={`text-xs font-semibold px-2 py-0.5 rounded-full
                  bg-${color}-500/15 text-${color}-300 border border-${color}-500/25`}>
                  {block.action}
                </span>
                {block.success
                  ? <ShieldCheck className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  : <ShieldX    className="w-3.5 h-3.5 text-rose-400 shrink-0" />
                }
              </div>
              <p className="text-white/40 text-xs mt-0.5 truncate">
                <span className="text-white/60">{block.actor}</span>
                {' → '}
                <span className="font-mono">{block.patientId}</span>
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <span className="text-white/30 text-xs hidden sm:block">{timeAgo(block.timestamp)}</span>
            {isExpanded
              ? <ChevronUp   className="w-4 h-4 text-white/30" />
              : <ChevronDown className="w-4 h-4 text-white/30" />
            }
          </div>
        </div>

        {/* Expanded details */}
        <AnimatePresence>
          {isExpanded && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden"
            >
              <div className="mt-4 pt-4 border-t border-white/8 space-y-3">
                {/* Hash chain visualization */}
                <div className="space-y-2">
                  <HashRow label="Previous Hash" value={block.previousHash} color="white/30" />
                  <HashRow label="This Block Hash" value={block.hash} color={`${color}-400`} highlight />
                </div>

                {/* Metadata grid */}
                <div className="grid grid-cols-2 gap-2 mt-3">
                  {[
                    { icon: User,     label: 'Actor',     value: block.actor },
                    { icon: Database, label: 'Patient ID', value: block.patientId },
                    { icon: Clock,    label: 'Timestamp',  value: new Date(block.timestamp).toLocaleTimeString() },
                    { icon: Lock,     label: 'Outcome',    value: block.success ? 'ALLOWED' : 'DENIED' },
                  ].map(({ icon: Icon, label, value }) => (
                    <div key={label} className="flex items-start gap-2 px-3 py-2 rounded-lg bg-white/4">
                      <Icon className="w-3.5 h-3.5 text-white/30 mt-0.5 shrink-0" />
                      <div className="min-w-0">
                        <p className="text-white/35 text-[10px] uppercase tracking-wide">{label}</p>
                        <p className="text-white/80 text-xs font-medium truncate">{value}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </GlassCard>
    </motion.div>
  );
}

// ─── Hash Row ─────────────────────────────────────────────────────────────────
function HashRow({ label, value, color, highlight }) {
  return (
    <div className={`flex items-center gap-2 px-3 py-2 rounded-lg
      ${highlight ? 'bg-white/6 border border-white/8' : 'bg-white/3'}`}>
      <Hash className={`w-3.5 h-3.5 text-${color} shrink-0`} />
      <div className="min-w-0 flex-1">
        <p className="text-white/30 text-[10px] uppercase tracking-wide mb-0.5">{label}</p>
        <p className={`font-mono text-xs text-${color} truncate`}>{value}</p>
      </div>
    </div>
  );
}

// ─── Stats Bar ────────────────────────────────────────────────────────────────
function StatsBar({ blocks }) {
  const allowed = blocks.filter(b => b.success && b.index > 0).length;
  const denied  = blocks.filter(b => !b.success).length;
  const writes  = blocks.filter(b => b.action === 'WRITE').length;

  return (
    <div className="grid grid-cols-3 gap-3">
      {[
        { label: 'Allowed',  count: allowed, color: 'emerald', icon: ShieldCheck },
        { label: 'Denied',   count: denied,  color: 'rose',    icon: ShieldX },
        { label: 'Writes',   count: writes,  color: 'amber',   icon: AlertTriangle },
      ].map(({ label, count, color, icon: Icon }) => (
        <GlassCard key={label} className="p-4">
          <div className="flex items-center gap-2">
            <Icon className={`w-4 h-4 text-${color}-400`} />
            <div>
              <p className={`text-xl font-bold text-${color}-400`}>{count}</p>
              <p className="text-white/40 text-xs">{label}</p>
            </div>
          </div>
        </GlassCard>
      ))}
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function AuditLogViewer() {
  const [blocks, setBlocks]       = useState(SEED_BLOCKS);
  const [expandedId, setExpandedId] = useState(null);
  const [filter, setFilter]       = useState('all');

  const toggleBlock = (index) =>
    setExpandedId(prev => prev === index ? null : index);

  const FILTERS = [
    { key: 'all',     label: 'All Blocks' },
    { key: 'allowed', label: 'Allowed' },
    { key: 'denied',  label: 'Denied' },
    { key: 'write',   label: 'Writes' },
  ];

  const visible = blocks.filter(b => {
    if (filter === 'allowed') return b.success && b.index > 0;
    if (filter === 'denied')  return !b.success;
    if (filter === 'write')   return b.action === 'WRITE';
    return true;
  });

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-6">
      <div className="max-w-2xl mx-auto space-y-5">

        {/* Header */}
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Shield className="w-6 h-6 text-indigo-400" />
            <h1 className="text-2xl font-bold text-white">Audit Log Chain</h1>
          </div>
          <p className="text-white/40 text-sm">
            Every access attempt — allowed or denied — permanently recorded in a
            SHA-256 linked chain
          </p>
        </div>

        {/* Stats */}
        <StatsBar blocks={blocks} />

        {/* Integrity banner */}
        <IntegrityBanner blocks={blocks} />

        {/* Filter tabs */}
        <div className="flex gap-2 flex-wrap">
          {FILTERS.map(f => (
            <button
              key={f.key}
              onClick={() => setFilter(f.key)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all
                ${filter === f.key
                  ? 'bg-indigo-500/20 border border-indigo-500/40 text-indigo-300'
                  : 'bg-white/5 border border-white/10 text-white/40 hover:text-white/70'}`}
            >
              {f.label}
            </button>
          ))}
        </div>

        {/* Block chain */}
        <div>
          <AnimatePresence>
            {visible.map(block => (
              <BlockCard
                key={block.index}
                block={block}
                isExpanded={expandedId === block.index}
                onToggle={() => toggleBlock(block.index)}
              />
            ))}
          </AnimatePresence>
        </div>

      </div>
    </div>
  );
}
