import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  UserPlus, ShieldCheck, Key, Wallet, CheckCircle2,
  AlertCircle, Hash, FileText, ChevronRight, Copy, RefreshCw
} from 'lucide-react';
import GlassCard from '../components/GlassCard';
import GradientButton from '../components/GradientButton';
import { useToast } from '../components/Toast';

// ─── Registration steps ───────────────────────────────────────────────────────
const STEPS = [
  { key: 'input',   label: 'Patient Info',    icon: UserPlus,    color: 'indigo' },
  { key: 'keygen',  label: 'Generate Keys',   icon: Key,         color: 'amber'  },
  { key: 'sign',    label: 'Sign VC',         icon: ShieldCheck, color: 'sky'    },
  { key: 'wallet',  label: 'Store in Wallet', icon: Wallet,      color: 'emerald'},
];

// ─── Step Progress Bar ────────────────────────────────────────────────────────
function StepBar({ currentStep }) {
  const idx = STEPS.findIndex(s => s.key === currentStep);
  return (
    <div className="flex items-center gap-0 mb-8">
      {STEPS.map((step, i) => {
        const Icon  = step.icon;
        const done  = i < idx;
        const active = i === idx;
        return (
          <React.Fragment key={step.key}>
            <motion.div
              animate={{ scale: active ? 1.1 : 1 }}
              className="flex flex-col items-center gap-1.5"
            >
              <div className={`w-9 h-9 rounded-xl flex items-center justify-center border-2 transition-all
                ${done   ? 'bg-emerald-500/20 border-emerald-500 text-emerald-400' :
                  active ? `bg-${step.color}-500/20 border-${step.color}-500 text-${step.color}-400` :
                           'bg-white/5 border-white/10 text-white/20'}`}>
                {done
                  ? <CheckCircle2 className="w-4 h-4" />
                  : <Icon className="w-4 h-4" />
                }
              </div>
              <span className={`text-[10px] font-medium text-center leading-tight
                ${done ? 'text-emerald-400' : active ? `text-${step.color}-300` : 'text-white/25'}`}>
                {step.label}
              </span>
            </motion.div>
            {i < STEPS.length - 1 && (
              <div className={`flex-1 h-0.5 mb-4 mx-1 rounded transition-all
                ${i < idx ? 'bg-emerald-500/60' : 'bg-white/10'}`} />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}

// ─── Step 1: Input Form ───────────────────────────────────────────────────────
function InputStep({ onSubmit }) {
  const [abhaId, setAbhaId]         = useState('');
  const [patientDid, setPatientDid] = useState('');
  const [name, setName]             = useState('');
  const [blood, setBlood]           = useState('');

  const inputCls = `w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2.5
    text-white text-sm placeholder-white/25 focus:outline-none focus:border-indigo-500/60
    focus:bg-white/8 transition-all`;

  const handleFill = () => {
    setAbhaId('99-9999-9999-9999');
    setPatientDid('did:veritas:patient-' + Math.random().toString(36).slice(2,7));
    setName('Arjun Mehta');
    setBlood('O-Negative');
  };

  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-white font-bold text-base">Patient Details</h2>
        <button onClick={handleFill}
          className="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
          Fill demo data ↗
        </button>
      </div>
      <div className="space-y-3">
        <div>
          <label className="text-white/45 text-xs font-medium mb-1.5 block">ABHA ID</label>
          <input className={inputCls} placeholder="99-9999-9999-9999"
            value={abhaId} onChange={e => setAbhaId(e.target.value)} />
        </div>
        <div>
          <label className="text-white/45 text-xs font-medium mb-1.5 block">Patient DID</label>
          <input className={inputCls} placeholder="did:veritas:patient-abc12"
            value={patientDid} onChange={e => setPatientDid(e.target.value)} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-white/45 text-xs font-medium mb-1.5 block">Full Name</label>
            <input className={inputCls} placeholder="Arjun Mehta"
              value={name} onChange={e => setName(e.target.value)} />
          </div>
          <div>
            <label className="text-white/45 text-xs font-medium mb-1.5 block">Blood Group</label>
            <input className={inputCls} placeholder="O-Negative"
              value={blood} onChange={e => setBlood(e.target.value)} />
          </div>
        </div>
        <GradientButton
          onClick={() => onSubmit({ abhaId, patientDid, name, blood })}
          disabled={!abhaId || !patientDid}
          className="w-full py-3 text-sm mt-2"
        >
          Register Patient <ChevronRight className="w-4 h-4 inline ml-1" />
        </GradientButton>
      </div>
    </motion.div>
  );
}

// ─── Processing Step (keygen / sign / wallet) ─────────────────────────────────
function ProcessingStep({ step, label, sublabel }) {
  const cfg = STEPS.find(s => s.key === step);
  const Icon = cfg?.icon || RefreshCw;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="flex flex-col items-center py-10 gap-4"
    >
      <motion.div
        animate={{ rotate: 360 }}
        transition={{ repeat: Infinity, duration: 1.5, ease: 'linear' }}
        className={`w-14 h-14 rounded-2xl flex items-center justify-center
          bg-${cfg?.color}-500/15 border border-${cfg?.color}-500/30`}
      >
        <Icon className={`w-7 h-7 text-${cfg?.color}-400`} />
      </motion.div>
      <div className="text-center">
        <p className="text-white font-semibold">{label}</p>
        <p className="text-white/40 text-sm mt-1">{sublabel}</p>
      </div>
      <div className="flex gap-1.5">
        {[0,1,2].map(i => (
          <motion.div
            key={i}
            animate={{ opacity: [0.3, 1, 0.3] }}
            transition={{ repeat: Infinity, duration: 1.2, delay: i * 0.2 }}
            className={`w-1.5 h-1.5 rounded-full bg-${cfg?.color}-400`}
          />
        ))}
      </div>
    </motion.div>
  );
}

// ─── Step 4: VC Issued Card ───────────────────────────────────────────────────
function IssuedVC({ patient }) {
  const [copied, setCopied] = useState(false);
  const toast = useToast();

  const fakeToken = `eyJhbGciOiJFUzI1NiJ9.eyJzdWIiOiIke3BhdGllbnQuYWJoYUlkfSIsIm5hbWUiOiIke3BhdGllbnQubmFtZX0iLCJibG9vZEdyb3VwIjoiJHtwYXRpZW50LmJsb29kfSJ9.ECDSA_SIGNATURE`;

  const handleCopy = () => {
    navigator.clipboard.writeText(fakeToken);
    setCopied(true);
    toast.success('Copied!', 'VC token copied to clipboard');
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ type: 'spring', stiffness: 200, damping: 20 }}
    >
      {/* Success banner */}
      <div className="flex items-center gap-2 mb-5 px-4 py-3 rounded-xl
        bg-emerald-500/10 border border-emerald-500/30">
        <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
        <div>
          <p className="text-emerald-300 font-semibold text-sm">Verifiable Credential Issued!</p>
          <p className="text-emerald-400/60 text-xs">Signed with ECDSA secp256k1 · Stored in patient wallet</p>
        </div>
      </div>

      {/* Patient data */}
      <div className="grid grid-cols-2 gap-2 mb-4">
        {[
          { label: 'Patient Name', value: patient.name     },
          { label: 'ABHA ID',      value: patient.abhaId   },
          { label: 'Blood Group',  value: patient.blood    },
          { label: 'DID',          value: patient.patientDid, mono: true },
        ].map(({ label, value, mono }) => (
          <div key={label} className="px-3 py-2.5 rounded-lg bg-white/5">
            <p className="text-white/35 text-[10px] uppercase tracking-wide mb-0.5">{label}</p>
            <p className={`text-white/80 text-xs font-medium truncate ${mono ? 'font-mono' : ''}`}>{value}</p>
          </div>
        ))}
      </div>

      {/* Token preview */}
      <div className="p-3 rounded-xl bg-white/4 border border-white/8">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-1.5">
            <Hash className="w-3.5 h-3.5 text-white/30" />
            <span className="text-white/40 text-xs">VC Token (ECDSA-signed)</span>
          </div>
          <button onClick={handleCopy}
            className="flex items-center gap-1 text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
            <Copy className="w-3 h-3" />
            {copied ? 'Copied!' : 'Copy'}
          </button>
        </div>
        <p className="font-mono text-[10px] text-white/35 break-all leading-relaxed">
          {fakeToken.slice(0, 80)}…
        </p>
      </div>

      {/* What happens next */}
      <div className="mt-4 p-3 rounded-xl bg-indigo-500/8 border border-indigo-500/20">
        <p className="text-indigo-300 text-xs font-semibold mb-2">What happens next?</p>
        <div className="space-y-1.5">
          {[
            'Patient stores this VC in their mobile wallet',
            'At any hospital, they present the VC to prove identity',
            'Hospital verifies the ECDSA signature cryptographically',
            'If valid + not revoked → patient record access granted',
          ].map(text => (
            <div key={text} className="flex items-start gap-2">
              <ChevronRight className="w-3 h-3 text-indigo-400/60 mt-0.5 shrink-0" />
              <p className="text-white/40 text-xs">{text}</p>
            </div>
          ))}
        </div>
      </div>
    </motion.div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function PatientRegistrationPage() {
  const [step, setStep]         = useState('input');
  const [patient, setPatient]   = useState(null);
  const toast = useToast();

  const handleRegister = async (data) => {
    setPatient(data);

    // Step 1: Key generation
    setStep('keygen');
    await new Promise(r => setTimeout(r, 1200));

    // Step 2: Sign VC
    setStep('sign');
    await new Promise(r => setTimeout(r, 1400));

    // Step 3: Store in wallet
    setStep('wallet');
    await new Promise(r => setTimeout(r, 900));

    // Done — show VC
    setStep('done');
    toast.success('VC Issued', `Credential for ${data.name} stored in wallet`);
  };

  const handleReset = () => {
    setStep('input');
    setPatient(null);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-6">
      <div className="max-w-lg mx-auto space-y-6">

        {/* Header */}
        <div>
          <div className="flex items-center gap-2 mb-1">
            <UserPlus className="w-6 h-6 text-indigo-400" />
            <h1 className="text-2xl font-bold text-white">Patient Registration</h1>
          </div>
          <p className="text-white/40 text-sm">
            Issues a W3C Verifiable Credential signed with ECDSA secp256k1
          </p>
        </div>

        <GlassCard className="p-6">
          {/* Step progress bar — hidden on done */}
          {step !== 'done' && <StepBar currentStep={step} />}

          <AnimatePresence mode="wait">
            {step === 'input' && (
              <motion.div key="input" exit={{ opacity: 0, y: -10 }}>
                <InputStep onSubmit={handleRegister} />
              </motion.div>
            )}

            {step === 'keygen' && (
              <motion.div key="keygen" exit={{ opacity: 0, y: -10 }}>
                <ProcessingStep
                  step="keygen"
                  label="Generating ECDSA Key Pair"
                  sublabel="secp256k1 curve · hospital private key signs the credential"
                />
              </motion.div>
            )}

            {step === 'sign' && (
              <motion.div key="sign" exit={{ opacity: 0, y: -10 }}>
                <ProcessingStep
                  step="sign"
                  label="Signing Verifiable Credential"
                  sublabel="Embedding patient claims · computing SHA-256 hash"
                />
              </motion.div>
            )}

            {step === 'wallet' && (
              <motion.div key="wallet" exit={{ opacity: 0, y: -10 }}>
                <ProcessingStep
                  step="wallet"
                  label="Storing in Patient Wallet"
                  sublabel="Sending VC to PatientWalletService · persisting to ledger"
                />
              </motion.div>
            )}

            {step === 'done' && patient && (
              <motion.div key="done" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                <IssuedVC patient={patient} />
                <button
                  onClick={handleReset}
                  className="w-full mt-4 py-2.5 rounded-lg border border-white/10
                    text-white/50 hover:text-white/80 text-sm transition-all"
                >
                  Register Another Patient
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </GlassCard>

        {/* API reference card */}
        {step === 'input' && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1, transition: { delay: 0.3 } }}>
            <GlassCard className="p-4">
              <div className="flex items-center gap-2 mb-3">
                <FileText className="w-4 h-4 text-white/30" />
                <p className="text-white/50 text-xs font-semibold uppercase tracking-wide">
                  Backend Endpoints (Day 13)
                </p>
              </div>
              {[
                { method: 'POST', path: '/api/vc/issue',   desc: 'Issue a signed VC'         },
                { method: 'POST', path: '/api/vc/verify',  desc: 'Check revocation status'   },
                { method: 'POST', path: '/api/vc/revoke',  desc: 'Revoke a credential'       },
                { method: 'GET',  path: '/api/vc/inspect', desc: 'Decode VC claims'          },
              ].map(({ method, path, desc }) => (
                <div key={path} className="flex items-center gap-3 py-1.5">
                  <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded font-mono
                    ${method === 'POST' ? 'bg-sky-500/15 text-sky-400' : 'bg-emerald-500/15 text-emerald-400'}`}>
                    {method}
                  </span>
                  <span className="font-mono text-xs text-white/50">{path}</span>
                  <span className="text-white/25 text-xs ml-auto">{desc}</span>
                </div>
              ))}
            </GlassCard>
          </motion.div>
        )}

      </div>
    </div>
  );
}
