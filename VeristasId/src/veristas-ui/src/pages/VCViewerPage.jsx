import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ShieldCheck, RefreshCw, AlertCircle, Fingerprint,
  CheckCircle2, Lock, Unlock, ChevronRight
} from 'lucide-react';
import GlassCard from '../components/GlassCard';
import GradientButton from '../components/GradientButton';
import StatusBadge from '../components/StatusBadge';
import { VerifiableCredentialCard, VCViewer } from '../components/VerifiableCredentialCard';
import { useToast } from '../components/Toast';
import { useSelector } from 'react-redux';

// ═══════════════════════════════════════════════════
//  VCViewerPage — Day 9 Frontend
//
//  The patient's digital credential wallet.
//  Shows all W3C Verifiable Credentials issued by
//  the hospital. Lets the patient:
//   - View credential details + cryptographic proof
//   - Verify any credential is still valid on-chain
//   - Revoke a credential instantly
//   - Understand the security model visually
// ═══════════════════════════════════════════════════

// ─── Security Explainer Row ──────────────────────────────────────────────────
function SecurityPill({ icon: Icon, label, color }) {
  return (
    <div className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full
      bg-${color}-500/10 border border-${color}-500/30`}>
      <Icon className={`w-3.5 h-3.5 text-${color}-400`} />
      <span className={`text-xs font-medium text-${color}-300`}>{label}</span>
    </div>
  );
}

// ─── Live Verification Indicator ─────────────────────────────────────────────
function VerificationBanner({ status }) {
  const configs = {
    idle:       { icon: Lock,          color: 'slate',   text: 'Not verified yet'             },
    verifying:  { icon: RefreshCw,     color: 'sky',     text: 'Checking ledger…'             },
    valid:      { icon: CheckCircle2,  color: 'emerald', text: 'Credential is VALID on ledger' },
    invalid:    { icon: AlertCircle,   color: 'rose',    text: 'Credential NOT found / REVOKED' },
  };
  const cfg = configs[status] || configs.idle;
  const Icon = cfg.icon;

  return (
    <motion.div
      key={status}
      initial={{ opacity: 0, y: -6 }}
      animate={{ opacity: 1, y: 0 }}
      className={`flex items-center gap-2 p-3 rounded-xl
        bg-${cfg.color}-500/10 border border-${cfg.color}-500/30`}
    >
      <motion.div
        animate={status === 'verifying' ? { rotate: 360 } : {}}
        transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}
      >
        <Icon className={`w-4 h-4 text-${cfg.color}-400`} />
      </motion.div>
      <span className={`text-xs font-semibold text-${cfg.color}-300`}>{cfg.text}</span>
    </motion.div>
  );
}

// ─── Credential Stats Bar ─────────────────────────────────────────────────────
function StatsBar({ total, active, revoked }) {
  return (
    <div className="grid grid-cols-3 gap-3">
      {[
        { label: 'Total Issued',  value: total,   color: 'sky'     },
        { label: 'Active',        value: active,  color: 'emerald' },
        { label: 'Revoked',       value: revoked, color: 'rose'    },
      ].map(({ label, value, color }) => (
        <GlassCard key={label} glow={color} hover={false} className="p-3 text-center">
          <p className={`text-2xl font-bold text-${color}-400`}>{value}</p>
          <p className="text-xs text-slate-400 mt-0.5">{label}</p>
        </GlassCard>
      ))}
    </div>
  );
}

// ─── How It Works explainer ───────────────────────────────────────────────────
function HowItWorks() {
  const [open, setOpen] = useState(false);
  const steps = [
    { icon: '🏥', title: 'Hospital Issues',  desc: 'Hospital signs your ABHA ID with their private key (ECDSA ES256). Creates a W3C VC document.' },
    { icon: '🔐', title: 'Saved to Ledger',  desc: 'The signed JWT is stored in the hospital\'s PostgreSQL ledger. Your DID is linked to this entry.' },
    { icon: '📱', title: 'In Your Wallet',   desc: 'You receive the VC in your patient portal. Nobody else can create a credential for your DID.' },
    { icon: '✅', title: 'Verifiable by All', desc: 'Any hospital worldwide can verify this credential using the hospital\'s public key — without contacting the issuer.' },
  ];

  return (
    <GlassCard glow="sky" hover={false} className="p-0 overflow-hidden">
      <button
        onClick={() => setOpen(p => !p)}
        className="w-full flex items-center justify-between p-4"
      >
        <div className="flex items-center gap-2">
          <Fingerprint className="w-4 h-4 text-sky-400" />
          <span className="text-sm font-semibold text-white">How Verifiable Credentials Work</span>
        </div>
        <motion.div animate={{ rotate: open ? 90 : 0 }}>
          <ChevronRight className="w-4 h-4 text-slate-400" />
        </motion.div>
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ height: 0 }}
            animate={{ height: 'auto' }}
            exit={{ height: 0 }}
            className="overflow-hidden"
          >
            <div className="px-4 pb-4 grid grid-cols-1 gap-3">
              {steps.map((step, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.07 }}
                  className="flex gap-3 p-3 rounded-xl bg-white/5 border border-white/10"
                >
                  <span className="text-xl flex-shrink-0">{step.icon}</span>
                  <div>
                    <p className="text-xs font-bold text-white">{step.title}</p>
                    <p className="text-xs text-slate-400 mt-0.5 leading-relaxed">{step.desc}</p>
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </GlassCard>
  );
}

// ─── MAIN PAGE EXPORT ─────────────────────────────────────────────────────────
export default function VCViewerPage() {
  const toast = useToast();

  // In a real app these come from Redux store (set after login)
  // For now we use realistic demo values
  const patientDid  = 'did:veristas:patient:Ab3xYz9k1m2n3p4q';
  const abhaId      = '99-9999-9999-9999';

  // Mock VC list — in production fetched from GET /api/credentials/:did
  const [vcs, setVcs] = useState([
    {
      id: 'vc-001',
      issuer: 'did:veristas:hospital:Hosp1Key8x9y',
      issuanceDate: new Date(Date.now() - 1000 * 60 * 60 * 24 * 2).toISOString(), // 2 days ago
      credentialSubject: { id: patientDid, abhaId },
      proof: {
        type: 'JwtProof2020',
        jwt: 'eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkaWQ6dmVyaXN0YXM6aG9zcGl0YWw6SG9zcDFLZXk4eDl5Iiwic3ViIjoiZGlkOnZlcmlzdGFzOnBhdGllbnQ6QWIzeVl6OWsxbTJuM3A0cSIsInJvbGUiOiJwYXRpZW50IiwiYWJoYUlkIjoiOTktOTk5OS05OTk5LTk5OTkifQ.HOSPITAL_ECDSA_SIGNATURE',
      },
      status: 'VALID',
    },
  ]);

  const [verifyStatus, setVerifyStatus] = useState('idle');

  // Simulates calling GET /api/vc/verify with the JWT
  const handleVerify = async () => {
    setVerifyStatus('verifying');
    await new Promise(r => setTimeout(r, 1800)); // Simulate API latency
    setVerifyStatus('valid');
    toast.success('Verified!', 'Credential is active on the hospital ledger');
    setTimeout(() => setVerifyStatus('idle'), 5000);
  };

  const handleRevoke = (vcId) => {
    setVcs(prev => prev.map(v => v.id === vcId ? { ...v, status: 'REVOKED' } : v));
    toast.warning('Credential Revoked', 'Your VC has been revoked from the hospital ledger');
  };

  const activeCount  = vcs.filter(v => v.status === 'VALID').length;
  const revokedCount = vcs.filter(v => v.status === 'REVOKED').length;

  return (
    <div className="max-w-xl mx-auto space-y-6 px-4 py-6">

      {/* ── Page Header ─────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-2"
      >
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-indigo-500/20">
            <ShieldCheck className="w-5 h-5 text-indigo-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">Credential Wallet</h1>
            <p className="text-xs text-slate-400">W3C Verifiable Credentials · ECDSA ES256</p>
          </div>
        </div>

        {/* Security pills */}
        <div className="flex flex-wrap gap-2 pt-1">
          <SecurityPill icon={Lock}      label="Cryptographically Signed"  color="indigo" />
          <SecurityPill icon={ShieldCheck} label="Tamper-Proof"             color="emerald" />
          <SecurityPill icon={Fingerprint} label="Self-Sovereign Identity"  color="sky" />
        </div>
      </motion.div>

      {/* ── Stats Bar ───────────────────────────────────── */}
      <StatsBar total={vcs.length} active={activeCount} revoked={revokedCount} />

      {/* ── Live Verification ───────────────────────────── */}
      <div className="space-y-2">
        <VerificationBanner status={verifyStatus} />
        <GradientButton
          variant="primary"
          fullWidth
          onClick={handleVerify}
          loading={verifyStatus === 'verifying'}
          icon={<CheckCircle2 className="w-4 h-4" />}
        >
          Verify on Ledger
        </GradientButton>
      </div>

      {/* ── VC Cards ────────────────────────────────────── */}
      <div className="space-y-4">
        <p className="text-xs text-slate-400 font-medium uppercase tracking-wider">
          Issued Credentials ({vcs.length})
        </p>
        <AnimatePresence>
          {vcs.length === 0 ? (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="text-center py-12"
            >
              <Unlock className="w-10 h-10 text-slate-600 mx-auto mb-3" />
              <p className="text-slate-400 text-sm">No credentials issued yet</p>
              <p className="text-slate-500 text-xs mt-1">
                Register your ABHA ID to receive your first VC
              </p>
            </motion.div>
          ) : (
            vcs.map(vc => (
              <motion.div
                key={vc.id}
                layout
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
              >
                <VerifiableCredentialCard
                  vc={vc}
                  onRevoke={vc.status === 'VALID' ? () => handleRevoke(vc.id) : null}
                />
              </motion.div>
            ))
          )}
        </AnimatePresence>
      </div>

      {/* ── How It Works ────────────────────────────────── */}
      <HowItWorks />

    </div>
  );
}
