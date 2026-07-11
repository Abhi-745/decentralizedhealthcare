import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ShieldCheck, ShieldOff, Clock, User, AlertTriangle,
  Plus, Trash2, Eye, Edit3, CheckCircle2, XCircle
} from 'lucide-react';
import GlassCard from '../components/GlassCard';
import GradientButton from '../components/GradientButton';
import StatusBadge from '../components/StatusBadge';
import { useToast } from '../components/Toast';

// ═══════════════════════════════════════════════════
//  ConsentManager — Day 8 Frontend
//
//  Patients can GRANT and REVOKE access to their
//  medical records for specific roles (paramedic,
//  surgeon, dispatcher). Each consent has:
//    - grantedTo: the role allowed
//    - permission: READ or UPDATE
//    - expiresIn: how long access lasts
//    - grantedAt: when it was created
// ═══════════════════════════════════════════════════

// Role visual config
const roleConfig = {
  paramedic:  { color: 'sky',     label: 'Paramedic',   icon: '🚑' },
  surgeon:    { color: 'indigo',  label: 'Surgeon',     icon: '🔬' },
  dispatcher: { color: 'amber',   label: 'Dispatcher',  icon: '📡' },
};

const permissionConfig = {
  READ:   { icon: Eye,   color: 'text-sky-400',     label: 'Read Only'    },
  UPDATE: { icon: Edit3, color: 'text-indigo-400',  label: 'Read + Write' },
};

// Formats ms since epoch to a readable string
function timeAgo(ts) {
  const seconds = Math.floor((Date.now() - ts) / 1000);
  if (seconds < 60) return 'just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

// ─── Single Consent Card ─────────────────────────────────────────────────────

function ConsentCard({ consent, onRevoke }) {
  const [revoking, setRevoking] = useState(false);
  const cfg = roleConfig[consent.grantedTo] || roleConfig.paramedic;
  const perm = permissionConfig[consent.permission] || permissionConfig.READ;
  const PermIcon = perm.icon;

  const isExpired = consent.expiresAt && Date.now() > consent.expiresAt;

  const handleRevoke = async () => {
    setRevoking(true);
    await new Promise((r) => setTimeout(r, 600)); // simulate API call
    onRevoke(consent.id);
  };

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, x: -20, height: 0, marginBottom: 0 }}
      transition={{ type: 'spring', stiffness: 300, damping: 25 }}
    >
      <GlassCard
        glow={isExpired ? 'rose' : cfg.color}
        className="p-4"
        hover={false}
      >
        <div className="flex items-start justify-between gap-3">
          {/* Left — role + permission info */}
          <div className="flex items-start gap-3 flex-1 min-w-0">
            {/* Role emoji badge */}
            <div className={`text-xl w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0
              bg-${cfg.color}-500/20`}>
              {cfg.icon}
            </div>

            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-sm font-bold text-white">{cfg.label}</span>
                <StatusBadge
                  status={isExpired ? 'EXPIRED' : 'ACTIVE'}
                  pulse={!isExpired}
                />
              </div>

              {/* Permission row */}
              <div className={`flex items-center gap-1 mt-1 ${perm.color}`}>
                <PermIcon className="w-3.5 h-3.5" />
                <span className="text-xs font-medium">{perm.label}</span>
              </div>

              {/* Time info */}
              <div className="flex items-center gap-1 mt-1.5 text-slate-500">
                <Clock className="w-3 h-3" />
                <span className="text-xs">
                  Granted {timeAgo(consent.grantedAt)}
                  {consent.expiresAt && (
                    isExpired
                      ? <span className="text-rose-400 ml-1">· Expired</span>
                      : <span className="ml-1">
                          · Expires {timeAgo(consent.expiresAt - Date.now() * 2)}
                        </span>
                  )}
                </span>
              </div>
            </div>
          </div>

          {/* Right — revoke button */}
          <motion.button
            whileTap={{ scale: 0.93 }}
            onClick={handleRevoke}
            disabled={revoking}
            className="flex-shrink-0 p-2 rounded-xl border border-rose-500/30 bg-rose-500/10
                       text-rose-400 hover:bg-rose-500/20 hover:border-rose-500/50
                       transition-all disabled:opacity-40"
            title="Revoke consent"
          >
            {revoking
              ? <motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 0.8 }}>
                  <Trash2 className="w-4 h-4" />
                </motion.div>
              : <Trash2 className="w-4 h-4" />}
          </motion.button>
        </div>
      </GlassCard>
    </motion.div>
  );
}

// ─── Grant New Consent Form ───────────────────────────────────────────────────

function GrantConsentForm({ onGrant, onClose }) {
  const [role, setRole]           = useState('paramedic');
  const [permission, setPermission] = useState('READ');
  const [duration, setDuration]   = useState('8'); // hours
  const [loading, setLoading]     = useState(false);
  const toast = useToast();

  const handleGrant = async () => {
    setLoading(true);
    await new Promise((r) => setTimeout(r, 700));
    const newConsent = {
      id: Date.now(),
      grantedTo: role,
      permission,
      grantedAt: Date.now(),
      expiresAt: Date.now() + Number(duration) * 60 * 60 * 1000,
    };
    onGrant(newConsent);
    toast.success('Consent Granted', `${roleConfig[role].label} can now ${permission} your records for ${duration}h`);
    setLoading(false);
    onClose();
  };

  return (
    <motion.div
      initial={{ opacity: 0, height: 0 }}
      animate={{ opacity: 1, height: 'auto' }}
      exit={{ opacity: 0, height: 0 }}
      className="overflow-hidden"
    >
      <GlassCard glow="emerald" className="p-5 mt-3">
        <h4 className="text-sm font-bold text-white mb-4 flex items-center gap-2">
          <Plus className="w-4 h-4 text-emerald-400" />
          Grant New Access
        </h4>

        <div className="space-y-4">
          {/* Role selector */}
          <div>
            <label className="text-xs text-slate-400 font-medium mb-2 block">
              Grant access to
            </label>
            <div className="grid grid-cols-3 gap-2">
              {Object.entries(roleConfig).map(([key, cfg]) => (
                <button
                  key={key}
                  onClick={() => setRole(key)}
                  className={`p-2.5 rounded-xl border text-center transition-all text-xs font-medium
                    ${role === key
                      ? 'bg-emerald-500/20 border-emerald-500/60 text-white'
                      : 'bg-white/5 border-white/10 text-slate-400 hover:bg-white/8'}`}
                >
                  <div className="text-lg mb-0.5">{cfg.icon}</div>
                  {cfg.label}
                </button>
              ))}
            </div>
          </div>

          {/* Permission selector */}
          <div>
            <label className="text-xs text-slate-400 font-medium mb-2 block">
              Permission level
            </label>
            <div className="grid grid-cols-2 gap-2">
              {Object.entries(permissionConfig).map(([key, cfg]) => {
                const Icon = cfg.icon;
                return (
                  <button
                    key={key}
                    onClick={() => setPermission(key)}
                    className={`p-3 rounded-xl border flex items-center gap-2 transition-all text-xs font-medium
                      ${permission === key
                        ? 'bg-emerald-500/20 border-emerald-500/60 text-white'
                        : 'bg-white/5 border-white/10 text-slate-400 hover:bg-white/8'}`}
                  >
                    <Icon className={`w-4 h-4 ${permission === key ? 'text-emerald-400' : ''}`} />
                    {cfg.label}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Duration */}
          <div>
            <label className="text-xs text-slate-400 font-medium mb-2 block">
              Duration (hours)
            </label>
            <div className="flex gap-2">
              {['2', '4', '8', '24'].map((h) => (
                <button
                  key={h}
                  onClick={() => setDuration(h)}
                  className={`flex-1 py-2 rounded-xl border text-xs font-bold transition-all
                    ${duration === h
                      ? 'bg-emerald-500/20 border-emerald-500/60 text-white'
                      : 'bg-white/5 border-white/10 text-slate-400 hover:bg-white/8'}`}
                >
                  {h}h
                </button>
              ))}
            </div>
          </div>

          {/* Warning */}
          <div className="flex items-start gap-2 p-3 rounded-xl bg-amber-500/10 border border-amber-500/30">
            <AlertTriangle className="w-4 h-4 text-amber-400 flex-shrink-0 mt-0.5" />
            <p className="text-xs text-amber-300">
              You can revoke this consent at any time. Access will also automatically expire after {duration} hours.
            </p>
          </div>

          {/* Action buttons */}
          <div className="flex gap-2">
            <GradientButton variant="success" onClick={handleGrant} loading={loading}
              icon={<CheckCircle2 className="w-4 h-4" />} className="flex-1">
              Grant Access
            </GradientButton>
            <button
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl border border-white/10 bg-white/5
                         text-slate-400 hover:text-white hover:bg-white/10 transition-all text-sm"
            >
              Cancel
            </button>
          </div>
        </div>
      </GlassCard>
    </motion.div>
  );
}

// ─── MAIN EXPORT ─────────────────────────────────────────────────────────────

export default function ConsentManager({ abhaId }) {
  const [consents, setConsents] = useState([
    // Seed with one active consent so the UI is not empty on first load
    {
      id: 1,
      grantedTo: 'paramedic',
      permission: 'READ',
      grantedAt: Date.now() - 1000 * 60 * 30, // 30 min ago
      expiresAt: Date.now() + 1000 * 60 * 60 * 7.5, // 7.5h from now
    },
  ]);
  const [showForm, setShowForm] = useState(false);
  const toast = useToast();

  const handleRevoke = (id) => {
    setConsents((prev) => prev.filter((c) => c.id !== id));
    toast.warning('Consent Revoked', 'Access has been immediately withdrawn');
  };

  const handleGrant = (newConsent) => {
    setConsents((prev) => [...prev, newConsent]);
  };

  const activeCount  = consents.filter((c) => !c.expiresAt || Date.now() < c.expiresAt).length;
  const expiredCount = consents.length - activeCount;

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <ShieldCheck className="w-5 h-5 text-emerald-400" />
            Consent Manager
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Control who can access your medical records
          </p>
        </div>
        <div className="text-right">
          <p className="text-2xl font-bold text-white">{activeCount}</p>
          <p className="text-xs text-slate-400">active grants</p>
        </div>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-3 gap-3">
        {[
          { label: 'Total',   value: consents.length,   color: 'text-white' },
          { label: 'Active',  value: activeCount,        color: 'text-emerald-400' },
          { label: 'Expired', value: expiredCount,       color: 'text-rose-400' },
        ].map(({ label, value, color }) => (
          <GlassCard key={label} glow="sky" className="p-3 text-center" hover={false}>
            <p className={`text-2xl font-bold ${color}`}>{value}</p>
            <p className="text-xs text-slate-400 mt-0.5">{label}</p>
          </GlassCard>
        ))}
      </div>

      {/* Grant button + form */}
      <div>
        {!showForm && (
          <GradientButton
            variant="primary"
            fullWidth
            onClick={() => setShowForm(true)}
            icon={<Plus className="w-4 h-4" />}
          >
            Grant New Access
          </GradientButton>
        )}

        <AnimatePresence>
          {showForm && (
            <GrantConsentForm
              onGrant={handleGrant}
              onClose={() => setShowForm(false)}
            />
          )}
        </AnimatePresence>
      </div>

      {/* Active consent list */}
      <div className="space-y-3">
        <p className="text-xs text-slate-400 font-medium uppercase tracking-wider">
          Current Grants
        </p>
        <AnimatePresence>
          {consents.length === 0 ? (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="text-center py-10"
            >
              <ShieldOff className="w-10 h-10 text-slate-600 mx-auto mb-3" />
              <p className="text-slate-400 text-sm">No active consents</p>
              <p className="text-slate-500 text-xs mt-1">
                Grant access to healthcare providers when needed
              </p>
            </motion.div>
          ) : (
            consents.map((consent) => (
              <ConsentCard
                key={consent.id}
                consent={consent}
                onRevoke={handleRevoke}
              />
            ))
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
