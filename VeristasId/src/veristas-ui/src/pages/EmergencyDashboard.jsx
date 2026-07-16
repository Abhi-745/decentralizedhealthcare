import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Siren, ShieldAlert, CheckCircle2, Clock, User,
  Stethoscope, Ambulance, Radio, AlertTriangle, ChevronRight, X
} from 'lucide-react';
import GlassCard from '../components/GlassCard';
import StatusBadge from '../components/StatusBadge';
import GradientButton from '../components/GradientButton';
import { useToast } from '../components/Toast';

// ─── Stage metadata ───────────────────────────────────────────────────────────
const STAGES = [
  {
    key: 'dispatched',
    label: 'Dispatched',
    icon: Radio,
    color: 'sky',
    description: 'Paramedic en route. Read-only access to blood type & allergies granted.',
    access: 'Paramedic: Read-Only',
  },
  {
    key: 'arrived',
    label: 'Arrived',
    icon: Ambulance,
    color: 'amber',
    description: 'Patient at hospital. Surgeon gains full read/write access to EMR.',
    access: 'Surgeon: Read + Write',
  },
  {
    key: 'resolved',
    label: 'Resolved',
    icon: CheckCircle2,
    color: 'emerald',
    description: 'Emergency closed. All emergency access revoked. Audit trail locked.',
    access: 'All access revoked',
  },
];

// ─── Seed data ────────────────────────────────────────────────────────────────
const SEED_SESSIONS = [
  { esid: 'ESID-2024-001', patientId: 'PAT-9981', patientName: 'Arjun Mehta',    stage: 'dispatched', createdAt: Date.now() - 420000 },
  { esid: 'ESID-2024-002', patientId: 'PAT-1123', patientName: 'Priya Sharma',   stage: 'arrived',    createdAt: Date.now() - 1800000 },
  { esid: 'ESID-2024-003', patientId: 'PAT-5540', patientName: 'Rohit Verma',    stage: 'resolved',   createdAt: Date.now() - 7200000 },
];

// ─── Helper ───────────────────────────────────────────────────────────────────
function timeAgo(ms) {
  const diff = Math.floor((Date.now() - ms) / 1000);
  if (diff < 60) return `${diff}s ago`;
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  return `${Math.floor(diff / 3600)}h ago`;
}

function stageIndex(stage) {
  return STAGES.findIndex(s => s.key === stage);
}

// ─── Stage Progress Bar ───────────────────────────────────────────────────────
function StageProgress({ stage }) {
  const idx = stageIndex(stage);
  return (
    <div className="flex items-center gap-0 mt-3">
      {STAGES.map((s, i) => {
        const Icon = s.icon;
        const done = i <= idx;
        const active = i === idx;
        return (
          <React.Fragment key={s.key}>
            <motion.div
              initial={{ scale: 0.8, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ delay: i * 0.1 }}
              className={`flex flex-col items-center gap-1`}
            >
              <div className={`w-8 h-8 rounded-full flex items-center justify-center border-2 transition-all
                ${done
                  ? `bg-${s.color}-500/20 border-${s.color}-500 text-${s.color}-400`
                  : 'bg-white/5 border-white/10 text-white/20'}`}
              >
                <Icon className="w-3.5 h-3.5" />
              </div>
              <span className={`text-[10px] font-medium ${done ? `text-${s.color}-400` : 'text-white/25'}`}>
                {s.label}
              </span>
            </motion.div>
            {i < STAGES.length - 1 && (
              <div className={`flex-1 h-0.5 mb-4 mx-1 rounded transition-all
                ${i < idx ? 'bg-emerald-500/60' : 'bg-white/10'}`}
              />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}

// ─── Session Card ─────────────────────────────────────────────────────────────
function SessionCard({ session, onAdvance, onView }) {
  const stage = STAGES.find(s => s.key === session.stage);
  const isResolved = session.stage === 'resolved';
  const nextStage = STAGES[stageIndex(session.stage) + 1];

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.95 }}
    >
      <GlassCard className="p-5">
        {/* Header row */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center
              bg-${stage.color}-500/15 border border-${stage.color}-500/30`}>
              <Siren className={`w-5 h-5 text-${stage.color}-400`} />
            </div>
            <div>
              <p className="text-white font-semibold text-sm">{session.patientName}</p>
              <p className="text-white/40 text-xs font-mono">{session.esid}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <StatusBadge status={session.stage} />
            <span className="text-white/30 text-xs">{timeAgo(session.createdAt)}</span>
          </div>
        </div>

        {/* Stage progress */}
        <StageProgress stage={session.stage} />

        {/* Access note */}
        <div className={`mt-3 px-3 py-2 rounded-lg bg-${stage.color}-500/8 border border-${stage.color}-500/20`}>
          <p className={`text-xs text-${stage.color}-300`}>
            <span className="font-semibold">Current access: </span>{stage.access}
          </p>
          <p className="text-white/40 text-xs mt-0.5">{stage.description}</p>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2 mt-4">
          <button
            onClick={() => onView(session)}
            className="flex-1 py-2 rounded-lg border border-white/10 text-white/60
              hover:border-white/25 hover:text-white/90 text-xs font-medium transition-all"
          >
            View Details
          </button>
          {!isResolved && nextStage && (
            <GradientButton
              onClick={() => onAdvance(session.esid, nextStage.key)}
              className="flex-1 py-2 text-xs"
            >
              <ChevronRight className="w-3.5 h-3.5 inline mr-1" />
              Move to {nextStage.label}
            </GradientButton>
          )}
        </div>
      </GlassCard>
    </motion.div>
  );
}

// ─── Create Session Modal ─────────────────────────────────────────────────────
function CreateModal({ onClose, onCreate }) {
  const [esid, setEsid] = useState('');
  const [patientId, setPatientId] = useState('');
  const [patientName, setPatientName] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!esid || !patientId || !patientName) return;
    setLoading(true);
    await new Promise(r => setTimeout(r, 800));
    onCreate({ esid, patientId, patientName });
    setLoading(false);
    onClose();
  };

  const inputCls = `w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2.5
    text-white text-sm placeholder-white/25 focus:outline-none focus:border-sky-500/60
    focus:bg-white/8 transition-all`;

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
      onClick={onClose}
    >
      <motion.div
        initial={{ scale: 0.9, y: 20 }}
        animate={{ scale: 1, y: 0 }}
        exit={{ scale: 0.9, y: 20 }}
        onClick={e => e.stopPropagation()}
        className="w-full max-w-md"
      >
        <GlassCard className="p-6">
          <div className="flex items-center justify-between mb-5">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-rose-500/15 border border-rose-500/30 flex items-center justify-center">
                <ShieldAlert className="w-5 h-5 text-rose-400" />
              </div>
              <div>
                <h3 className="text-white font-bold text-base">New Emergency Session</h3>
                <p className="text-white/40 text-xs">Dispatcher role required</p>
              </div>
            </div>
            <button onClick={onClose} className="text-white/30 hover:text-white/70 transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-3">
            <div>
              <label className="text-white/50 text-xs font-medium mb-1.5 block">Emergency Session ID</label>
              <input
                className={inputCls}
                placeholder="e.g. ESID-2024-007"
                value={esid}
                onChange={e => setEsid(e.target.value)}
              />
            </div>
            <div>
              <label className="text-white/50 text-xs font-medium mb-1.5 block">Patient ID (ABHA)</label>
              <input
                className={inputCls}
                placeholder="e.g. PAT-4421"
                value={patientId}
                onChange={e => setPatientId(e.target.value)}
              />
            </div>
            <div>
              <label className="text-white/50 text-xs font-medium mb-1.5 block">Patient Name</label>
              <input
                className={inputCls}
                placeholder="Full name"
                value={patientName}
                onChange={e => setPatientName(e.target.value)}
              />
            </div>

            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2.5 rounded-lg border border-white/10 text-white/50
                  hover:text-white/80 text-sm transition-all"
              >
                Cancel
              </button>
              <GradientButton type="submit" className="flex-1 py-2.5 text-sm" disabled={loading}>
                {loading ? 'Creating…' : 'Create Session'}
              </GradientButton>
            </div>
          </form>
        </GlassCard>
      </motion.div>
    </motion.div>
  );
}

// ─── Detail Panel ─────────────────────────────────────────────────────────────
function DetailPanel({ session, onClose }) {
  const stage = STAGES.find(s => s.key === session.stage);
  const Icon = stage.icon;
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
      onClick={onClose}
    >
      <motion.div
        initial={{ x: 80, opacity: 0 }}
        animate={{ x: 0, opacity: 1 }}
        exit={{ x: 80, opacity: 0 }}
        onClick={e => e.stopPropagation()}
        className="w-full max-w-md"
      >
        <GlassCard className="p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-white font-bold">Session Details</h3>
            <button onClick={onClose} className="text-white/30 hover:text-white/70 transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Stage icon hero */}
          <div className={`flex flex-col items-center py-6 rounded-xl bg-${stage.color}-500/10
            border border-${stage.color}-500/20`}>
            <div className={`w-16 h-16 rounded-2xl flex items-center justify-center
              bg-${stage.color}-500/20 border border-${stage.color}-500/40 mb-3`}>
              <Icon className={`w-8 h-8 text-${stage.color}-400`} />
            </div>
            <p className={`text-${stage.color}-300 font-bold text-lg capitalize`}>{session.stage}</p>
            <p className="text-white/40 text-xs mt-1">{stage.description}</p>
          </div>

          {/* Data rows */}
          {[
            { label: 'ESID',        value: session.esid,        icon: ShieldAlert },
            { label: 'Patient',     value: session.patientName, icon: User },
            { label: 'Patient ID',  value: session.patientId,   icon: Stethoscope },
            { label: 'Created',     value: timeAgo(session.createdAt), icon: Clock },
            { label: 'Access',      value: stage.access,        icon: AlertTriangle },
          ].map(({ label, value, icon: I }) => (
            <div key={label} className="flex items-center gap-3 px-3 py-2.5 rounded-lg bg-white/4">
              <I className="w-4 h-4 text-white/30 shrink-0" />
              <div className="flex-1 min-w-0">
                <p className="text-white/40 text-xs">{label}</p>
                <p className="text-white text-sm font-medium truncate">{value}</p>
              </div>
            </div>
          ))}
        </GlassCard>
      </motion.div>
    </motion.div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function EmergencyDashboard() {
  const [sessions, setSessions] = useState(SEED_SESSIONS);
  const [showCreate, setShowCreate] = useState(false);
  const [viewSession, setViewSession] = useState(null);
  const [filter, setFilter] = useState('all');
  const toast = useToast();

  const handleCreate = (data) => {
    const newSession = {
      ...data,
      stage: 'dispatched',
      createdAt: Date.now(),
    };
    setSessions(prev => [newSession, ...prev]);
    toast.success('Session Created', `Emergency ${data.esid} is now DISPATCHED`);
  };

  const handleAdvance = async (esid, nextStage) => {
    setSessions(prev =>
      prev.map(s => s.esid === esid ? { ...s, stage: nextStage } : s)
    );
    const label = STAGES.find(s => s.key === nextStage)?.label;
    toast.info('Stage Advanced', `${esid} → ${label}`);
  };

  const FILTERS = ['all', 'dispatched', 'arrived', 'resolved'];
  const visible = filter === 'all'
    ? sessions
    : sessions.filter(s => s.stage === filter);

  const counts = {
    dispatched: sessions.filter(s => s.stage === 'dispatched').length,
    arrived:    sessions.filter(s => s.stage === 'arrived').length,
    resolved:   sessions.filter(s => s.stage === 'resolved').length,
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-6">
      <div className="max-w-3xl mx-auto space-y-6">

        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Siren className="w-6 h-6 text-rose-400" />
              <h1 className="text-2xl font-bold text-white">Emergency Dashboard</h1>
            </div>
            <p className="text-white/40 text-sm">
              Break-glass sessions · Dispatcher role required to create
            </p>
          </div>
          <GradientButton onClick={() => setShowCreate(true)} className="px-4 py-2.5 text-sm">
            <ShieldAlert className="w-4 h-4 inline mr-1.5" />
            New Session
          </GradientButton>
        </div>

        {/* Stat pills */}
        <div className="grid grid-cols-3 gap-3">
          {[
            { label: 'Dispatched', count: counts.dispatched, color: 'sky'     },
            { label: 'Arrived',    count: counts.arrived,    color: 'amber'   },
            { label: 'Resolved',   count: counts.resolved,   color: 'emerald' },
          ].map(({ label, count, color }) => (
            <GlassCard key={label} className="p-4 text-center">
              <p className={`text-2xl font-bold text-${color}-400`}>{count}</p>
              <p className="text-white/40 text-xs mt-0.5">{label}</p>
            </GlassCard>
          ))}
        </div>

        {/* Filter tabs */}
        <div className="flex gap-2">
          {FILTERS.map(f => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium capitalize transition-all
                ${filter === f
                  ? 'bg-indigo-500/20 border border-indigo-500/40 text-indigo-300'
                  : 'bg-white/5 border border-white/10 text-white/40 hover:text-white/70'}`}
            >
              {f === 'all' ? `All (${sessions.length})` : f}
            </button>
          ))}
        </div>

        {/* Session list */}
        <AnimatePresence mode="popLayout">
          {visible.length === 0 ? (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="text-center py-16 text-white/30"
            >
              <Siren className="w-12 h-12 mx-auto mb-3 opacity-30" />
              <p>No sessions in this stage</p>
            </motion.div>
          ) : (
            <div className="space-y-4">
              {visible.map(session => (
                <SessionCard
                  key={session.esid}
                  session={session}
                  onAdvance={handleAdvance}
                  onView={setViewSession}
                />
              ))}
            </div>
          )}
        </AnimatePresence>
      </div>

      {/* Modals */}
      <AnimatePresence>
        {showCreate && (
          <CreateModal onClose={() => setShowCreate(false)} onCreate={handleCreate} />
        )}
        {viewSession && (
          <DetailPanel session={viewSession} onClose={() => setViewSession(null)} />
        )}
      </AnimatePresence>
    </div>
  );
}
