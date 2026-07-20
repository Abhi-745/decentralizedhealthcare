import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Radio, Stethoscope, PhoneCall, CheckCircle2,
  Copy, Eye, EyeOff, Shield, User, BadgeCheck, AlertTriangle
} from 'lucide-react';
import GlassCard from '../components/GlassCard';
import GradientButton from '../components/GradientButton';
import { useToast } from '../components/Toast';

// ─── Role definitions ─────────────────────────────────────────────────────────
const ROLES = [
  {
    key:         'paramedic',
    label:       'Paramedic',
    badge:       'EMT-9110',
    name:        'Bobbi D\'Amore',
    endpoint:    '/api/auth/login-paramedic',
    icon:        Ambulance,
    color:       'sky',
    description: 'Read-only access during DISPATCHED emergency sessions',
    permissions: ['Read blood type', 'Read allergies', 'View active ESID'],
  },
  {
    key:         'surgeon',
    label:       'Surgeon',
    badge:       'SURG-1000',
    name:        'Dr. Fisher',
    endpoint:    '/api/auth/login-surgeon',
    icon:        Stethoscope,
    color:       'emerald',
    description: 'Full EMR access during ARRIVED emergency sessions',
    permissions: ['Read full EMR', 'Write diagnosis', 'Add surgery notes'],
  },
  {
    key:         'dispatcher',
    label:       'Dispatcher',
    badge:       'DISP-0001',
    name:        'Control Room Alpha',
    endpoint:    '/api/auth/login-dispatcher',
    icon:        PhoneCall,
    color:       'amber',
    description: 'Sole authority to create emergency sessions',
    permissions: ['Create emergency sessions', 'Advance session stages', 'View all sessions'],
  },
];

// Ambulance icon inline (not in lucide-react v0.263)
function Ambulance(props) {
  return <Radio {...props} />;
}

// ─── Token Display ─────────────────────────────────────────────────────────────
function TokenDisplay({ token, role }) {
  const [visible, setVisible] = useState(false);
  const toast = useToast();

  const handleCopy = () => {
    navigator.clipboard.writeText(token);
    toast.success('Copied!', 'Token copied to clipboard');
  };

  const displayToken = visible ? token : token.replace(/(?<=Bearer .{8}).+/, '••••••••••••••••••••');

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className={`mt-4 p-4 rounded-xl bg-${role.color}-500/8 border border-${role.color}-500/25`}
    >
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-1.5">
          <CheckCircle2 className={`w-4 h-4 text-${role.color}-400`} />
          <span className={`text-xs font-semibold text-${role.color}-300`}>
            HMAC-SHA256 JWT Issued
          </span>
        </div>
        <div className="flex items-center gap-1">
          <button
            onClick={() => setVisible(v => !v)}
            className="p-1.5 rounded-lg text-white/30 hover:text-white/70 transition-colors"
          >
            {visible ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
          </button>
          <button
            onClick={handleCopy}
            className="p-1.5 rounded-lg text-white/30 hover:text-white/70 transition-colors"
          >
            <Copy className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
      <p className="font-mono text-[11px] text-white/50 break-all leading-relaxed">
        {displayToken}
      </p>
      <div className="flex items-center gap-3 mt-3 pt-3 border-t border-white/8">
        <div>
          <p className="text-white/30 text-[10px] uppercase tracking-wide">Name</p>
          <p className="text-white/70 text-xs font-medium">{role.name}</p>
        </div>
        <div>
          <p className="text-white/30 text-[10px] uppercase tracking-wide">Badge</p>
          <p className="text-white/70 text-xs font-mono">{role.badge}</p>
        </div>
        <div>
          <p className="text-white/30 text-[10px] uppercase tracking-wide">Role</p>
          <p className={`text-xs font-medium text-${role.color}-300`}>{role.label}</p>
        </div>
      </div>
    </motion.div>
  );
}

// ─── Role Card ─────────────────────────────────────────────────────────────────
function RoleCard({ role, isActive, onLogin }) {
  const Icon = role.icon;
  const [token, setToken]     = useState(null);
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  const handleLogin = async () => {
    setLoading(true);
    // Simulate POST /api/auth/login-{role}
    await new Promise(r => setTimeout(r, 900));
    const fakeToken = `Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIke3JvbGUubmFtZX0iLCJyb2xlIjoiJHtyb2xlLmtleX0iLCJiYWRnZSI6IiR7cm9sZS5iYWRnZX0ifQ.HMAC_SHA256_SIGNATURE`;
    setToken(fakeToken);
    setLoading(false);
    onLogin(role.key, fakeToken);
    toast.success(`${role.label} Logged In`, `JWT issued for ${role.name}`);
  };

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <GlassCard
        className={`p-5 transition-all cursor-default
          ${isActive ? `border-${role.color}-500/40` : ''}`}
      >
        {/* Header */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0
              bg-${role.color}-500/15 border border-${role.color}-500/30`}>
              <Icon className={`w-5 h-5 text-${role.color}-400`} />
            </div>
            <div>
              <p className="text-white font-bold text-sm">{role.label}</p>
              <p className="text-white/40 text-xs font-mono">{role.badge}</p>
            </div>
          </div>
          {isActive && (
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              className={`flex items-center gap-1 px-2 py-1 rounded-full
                bg-${role.color}-500/15 border border-${role.color}-500/30`}
            >
              <div className={`w-1.5 h-1.5 rounded-full bg-${role.color}-400 animate-pulse`} />
              <span className={`text-[10px] font-medium text-${role.color}-300`}>Active</span>
            </motion.div>
          )}
        </div>

        {/* Description */}
        <p className="text-white/40 text-xs mt-3 leading-relaxed">{role.description}</p>

        {/* Permissions */}
        <div className="mt-3 space-y-1">
          {role.permissions.map(perm => (
            <div key={perm} className="flex items-center gap-2">
              <Shield className={`w-3 h-3 text-${role.color}-500/60 shrink-0`} />
              <span className="text-white/50 text-xs">{perm}</span>
            </div>
          ))}
        </div>

        {/* Login Button */}
        <div className="mt-4">
          <GradientButton
            onClick={handleLogin}
            disabled={loading || isActive}
            className="w-full py-2.5 text-sm"
          >
            {loading ? 'Generating JWT…' : isActive ? '✓ Token Issued' : `Login as ${role.label}`}
          </GradientButton>
        </div>

        {/* Token display (shown after login) */}
        <AnimatePresence>
          {token && <TokenDisplay token={token} role={role} />}
        </AnimatePresence>
      </GlassCard>
    </motion.div>
  );
}

// ─── Architecture Callout ─────────────────────────────────────────────────────
function ArchitectureNote() {
  return (
    <GlassCard className="p-5">
      <div className="flex items-center gap-2 mb-3">
        <AlertTriangle className="w-4 h-4 text-amber-400" />
        <p className="text-white font-semibold text-sm">How This Works Under the Hood</p>
      </div>
      <div className="space-y-2">
        {[
          { step: '1', text: 'POST /api/auth/login-{role} → JwtService.generateToken(name, role, badge)' },
          { step: '2', text: 'HMAC-SHA256 signs the payload with the server\'s secret key' },
          { step: '3', text: 'Token stored in frontend (Redux store / localStorage)' },
          { step: '4', text: 'Every protected request sends "Authorization: Bearer <token>"' },
          { step: '5', text: 'OpaSecurityFilter verifies the signature before any controller runs' },
          { step: '6', text: 'OPA evaluates role + stage context → allow or deny' },
        ].map(({ step, text }) => (
          <div key={step} className="flex items-start gap-3">
            <span className="w-5 h-5 rounded-full bg-indigo-500/20 border border-indigo-500/30
              text-indigo-400 text-[10px] font-bold flex items-center justify-center shrink-0 mt-0.5">
              {step}
            </span>
            <p className="text-white/45 text-xs leading-relaxed font-mono">{text}</p>
          </div>
        ))}
      </div>
    </GlassCard>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function StaffLoginPage() {
  const [activeTokens, setActiveTokens] = useState({});

  const handleLogin = (roleKey, token) => {
    setActiveTokens(prev => ({ ...prev, [roleKey]: token }));
  };

  const activeCount = Object.keys(activeTokens).length;

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-6">
      <div className="max-w-3xl mx-auto space-y-6">

        {/* Header */}
        <div>
          <div className="flex items-center gap-2 mb-1">
            <BadgeCheck className="w-6 h-6 text-indigo-400" />
            <h1 className="text-2xl font-bold text-white">Staff Authentication</h1>
          </div>
          <p className="text-white/40 text-sm">
            HMAC-SHA256 JWT issuance per role · Each token is OPA-verified on every request
          </p>
        </div>

        {/* Active session counter */}
        {activeCount > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <GlassCard className="p-4 flex items-center gap-3">
              <User className="w-4 h-4 text-emerald-400" />
              <p className="text-emerald-300 text-sm font-medium">
                {activeCount} active session{activeCount > 1 ? 's' : ''} — tokens ready for OPA-gated requests
              </p>
            </GlassCard>
          </motion.div>
        )}

        {/* Role cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {ROLES.map(role => (
            <RoleCard
              key={role.key}
              role={role}
              isActive={!!activeTokens[role.key]}
              onLogin={handleLogin}
            />
          ))}
        </div>

        {/* Architecture explanation */}
        <ArchitectureNote />
      </div>
    </div>
  );
}
