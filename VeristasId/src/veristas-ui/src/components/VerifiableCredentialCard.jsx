import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Shield, Award, Copy, CheckCircle2, AlertCircle,
  Calendar, Hash, Building2, ChevronDown, ChevronUp
} from 'lucide-react';
import GlassCard from '../components/GlassCard';
import StatusBadge from '../components/StatusBadge';
import { useToast } from '../components/Toast';

/**
 * VerifiableCredentialCard — Day 7 Frontend
 *
 * Displays a single W3C Verifiable Credential as an interactive
 * digital ID card. Shows:
 *  - The credential type and status badge
 *  - Issuer DID (hospital)
 *  - Subject / Patient claims (ABHA ID, DID)
 *  - Issuance date
 *  - The JWT proof (expandable, with copy button)
 *
 * Props:
 *  - vc: the VerifiableCredential object from the backend
 *  - onRevoke: optional callback if the patient revokes this VC
 */

// Helper: truncates a long string with "..." in the middle
function truncateMiddle(str = '', start = 12, end = 8) {
  if (!str || str.length <= start + end + 3) return str;
  return `${str.slice(0, start)}...${str.slice(-end)}`;
}

// One copy-able field row
function CopyField({ label, value, mono = true }) {
  const [copied, setCopied] = useState(false);
  const toast = useToast();

  const handleCopy = () => {
    navigator.clipboard.writeText(value);
    setCopied(true);
    toast.success('Copied!', `${label} copied to clipboard`);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="flex items-start justify-between gap-3 p-3 rounded-xl bg-white/5 border border-white/10">
      <div className="flex-1 min-w-0">
        <p className="text-xs text-slate-400 mb-0.5">{label}</p>
        <p className={`text-xs text-white break-all leading-relaxed ${mono ? 'font-mono' : ''}`}>
          {truncateMiddle(value, 20, 10)}
        </p>
      </div>
      <motion.button
        whileTap={{ scale: 0.9 }}
        onClick={handleCopy}
        className="flex-shrink-0 p-1.5 rounded-lg text-slate-400 hover:text-white
                   hover:bg-white/10 transition-colors mt-0.5"
      >
        {copied
          ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
          : <Copy className="w-3.5 h-3.5" />}
      </motion.button>
    </div>
  );
}

// The main card component
export function VerifiableCredentialCard({ vc, onRevoke }) {
  const [expanded, setExpanded] = useState(false);
  const toast = useToast();
  const isRevoked = vc?.status === 'REVOKED';

  return (
    <GlassCard
      glow={isRevoked ? 'rose' : 'indigo'}
      className="p-0 overflow-hidden"
    >
      {/* ── Top accent bar ───────────────────────────── */}
      <div className={`h-1 w-full ${isRevoked
        ? 'bg-gradient-to-r from-rose-500 to-red-600'
        : 'bg-gradient-to-r from-indigo-500 to-sky-500'}`}
      />

      <div className="p-6 space-y-5">

        {/* ── Header row ───────────────────────────────── */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className={`p-2.5 rounded-xl ${isRevoked ? 'bg-rose-500/20' : 'bg-indigo-500/20'}`}>
              <Award className={`w-5 h-5 ${isRevoked ? 'text-rose-400' : 'text-indigo-400'}`} />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white">Patient ABHA Credential</h3>
              <p className="text-xs text-slate-400 mt-0.5">W3C Verifiable Credential · JwtProof2020</p>
            </div>
          </div>
          <StatusBadge status={isRevoked ? 'REVOKED' : 'VALID'} pulse={!isRevoked} />
        </div>

        {/* ── Issuer ───────────────────────────────────── */}
        <div className="space-y-2">
          <div className="flex items-center gap-1.5 text-xs text-slate-400 font-medium">
            <Building2 className="w-3.5 h-3.5" />
            <span>Issued by</span>
          </div>
          <CopyField
            label="Issuer DID (Hospital)"
            value={vc?.issuer || 'did:veristas:hospital:loading...'}
          />
        </div>

        {/* ── Subject (Patient Claims) ──────────────────── */}
        <div className="space-y-2">
          <div className="flex items-center gap-1.5 text-xs text-slate-400 font-medium">
            <Shield className="w-3.5 h-3.5" />
            <span>Subject (You)</span>
          </div>
          <CopyField
            label="Patient DID"
            value={vc?.credentialSubject?.id || 'did:veristas:patient:...'}
          />
          <CopyField
            label="ABHA Health ID"
            value={vc?.credentialSubject?.abhaId || '99-9999-9999-9999'}
          />
        </div>

        {/* ── Dates ────────────────────────────────────── */}
        <div className="flex items-center gap-2 text-xs text-slate-400">
          <Calendar className="w-3.5 h-3.5 flex-shrink-0" />
          <span>
            Issued: <span className="text-slate-200">
              {vc?.issuanceDate
                ? new Date(vc.issuanceDate).toLocaleDateString('en-IN', {
                    day: '2-digit', month: 'short', year: 'numeric'
                  })
                : 'Today'}
            </span>
            {' · '}
            Valid for <span className="text-emerald-400 font-medium">1 year</span>
          </span>
        </div>

        {/* ── JWT Proof (expandable) ────────────────────── */}
        <div>
          <button
            onClick={() => setExpanded((p) => !p)}
            className="w-full flex items-center justify-between p-3 rounded-xl
                       bg-white/5 border border-white/10 hover:bg-white/8 transition-colors"
          >
            <div className="flex items-center gap-2 text-xs font-medium text-slate-300">
              <Hash className="w-3.5 h-3.5 text-indigo-400" />
              <span>Cryptographic Proof (JWT)</span>
            </div>
            {expanded
              ? <ChevronUp className="w-3.5 h-3.5 text-slate-400" />
              : <ChevronDown className="w-3.5 h-3.5 text-slate-400" />}
          </button>

          <AnimatePresence>
            {expanded && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                transition={{ duration: 0.25 }}
                className="overflow-hidden"
              >
                <div className="mt-2 p-3 rounded-xl bg-slate-900/60 border border-white/10">
                  <p className="text-xs font-mono text-emerald-300 break-all leading-relaxed">
                    {vc?.proof?.jwt || 'eyJhbGciOiJFUzI1NiJ9...'}
                  </p>
                  <button
                    onClick={() => {
                      navigator.clipboard.writeText(vc?.proof?.jwt || '');
                      toast.success('JWT Copied', 'Full proof JWT copied to clipboard');
                    }}
                    className="mt-2 text-xs text-indigo-400 hover:text-indigo-300 transition-colors
                               flex items-center gap-1"
                  >
                    <Copy className="w-3 h-3" /> Copy full JWT
                  </button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* ── Revoke button (only if not already revoked) ── */}
        {!isRevoked && onRevoke && (
          <motion.button
            whileTap={{ scale: 0.97 }}
            onClick={onRevoke}
            className="w-full py-2 rounded-xl text-xs font-semibold text-rose-400
                       border border-rose-500/30 bg-rose-500/10
                       hover:bg-rose-500/20 hover:border-rose-500/50 transition-all"
          >
            Revoke this Credential
          </motion.button>
        )}
      </div>
    </GlassCard>
  );
}

// ─── Demo wrapper — shows a sample VC in the Patient Portal ──────────────────

export function VCViewer({ patientDid, abhaId }) {
  // In a real app, this would call axios.get('/api/credentials/' + patientDid)
  // For now, we construct a realistic mock VC to demonstrate the UI
  const mockVC = {
    issuer: `did:veristas:hospital:Ab3xYz9k1m2n3p4q`,
    issuanceDate: new Date().toISOString(),
    credentialSubject: {
      id: patientDid || 'did:veristas:patient:Xy1zAb2c3d4e5f',
      abhaId: abhaId || '99-9999-9999-9999',
    },
    proof: {
      type: 'JwtProof2020',
      jwt: 'eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkaWQ6dmVyaXN0YXM6aG9zcGl0YWw6QWIzeVl6OWsxbTJuM3A0cSIsInN1YiI6ImRpZDp2ZXJpc3Rhczpwb3RpZW50Olh5MXpBYjJjM2Q0ZTVmIiwicm9sZSI6InBhdGllbnQiLCJhYmhhSWQiOiI5OS05OTk5LTk5OTktOTk5OSJ9.SIGNATURE_PLACEHOLDER',
    },
    status: 'VALID',
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-bold text-white">Your Verifiable Credentials</h3>
        <span className="text-xs text-slate-400">W3C Standard · ECDSA P-256</span>
      </div>
      <VerifiableCredentialCard
        vc={mockVC}
        onRevoke={() => alert('Revoke flow — coming Day 8')}
      />
    </div>
  );
}
