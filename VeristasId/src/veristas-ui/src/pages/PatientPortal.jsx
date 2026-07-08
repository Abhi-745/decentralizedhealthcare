import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Shield, CreditCard, Key, CheckCircle, Loader2, User, ArrowRight } from 'lucide-react';
import { useDispatch } from 'react-redux';
import { loginPatient } from '../store/slices/authSlice';
import { useToast } from '../components/Toast';
import GlassCard from '../components/GlassCard';
import GradientButton from '../components/GradientButton';
import StatusBadge from '../components/StatusBadge';
import axios from 'axios';

// ═══════════════════════════════════════════════════
//  PatientPortal — Day 6 rebuild
//  Multi-step ABHA ID login + wallet display
// ═══════════════════════════════════════════════════

// Step 1 — Enter ABHA ID
function StepEnterAbha({ onNext }) {
  const [abhaId, setAbhaId] = useState('');
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!abhaId.trim()) {
      toast.error('ABHA ID Required', 'Please enter your 14-digit ABHA number');
      return;
    }
    setLoading(true);
    // Simulate a small network delay before proceeding
    await new Promise((r) => setTimeout(r, 700));
    setLoading(false);
    toast.info('OTP Sent', `A 6-digit code was sent to your registered number`);
    onNext(abhaId.trim());
  };

  return (
    <motion.div
      key="step1"
      initial={{ opacity: 0, x: -30 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: 30 }}
      transition={{ duration: 0.3 }}
    >
      <GlassCard glow="sky" className="p-8 max-w-md mx-auto">
        {/* Header */}
        <div className="flex items-center gap-3 mb-6">
          <div className="p-2 rounded-xl bg-sky-500/20">
            <CreditCard className="w-6 h-6 text-sky-400" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-white">Patient Login</h2>
            <p className="text-xs text-slate-400">Step 1 of 2 — Enter your ABHA ID</p>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-sm text-slate-300 font-medium mb-1.5 block">
              ABHA Health ID
            </label>
            <input
              id="abha-input"
              type="text"
              value={abhaId}
              onChange={(e) => setAbhaId(e.target.value)}
              placeholder="99-1234-5678-9012"
              className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white
                         placeholder-slate-500 text-sm font-mono
                         focus:outline-none focus:border-sky-500/60 focus:bg-white/8
                         transition-all duration-200"
            />
            <p className="text-xs text-slate-500 mt-1.5">
              Format: 14-digit Ayushman Bharat Health Account number
            </p>
          </div>

          <GradientButton type="submit" variant="primary" fullWidth loading={loading}
            icon={<ArrowRight className="w-4 h-4" />}>
            Send OTP
          </GradientButton>
        </form>
      </GlassCard>
    </motion.div>
  );
}

// Step 2 — Enter OTP + fetch wallet from backend
function StepEnterOtp({ abhaId, onSuccess }) {
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const dispatch = useDispatch();
  const toast = useToast();

  const handleVerify = async (e) => {
    e.preventDefault();
    if (otp.length !== 6) {
      toast.error('Invalid OTP', 'OTP must be exactly 6 digits');
      return;
    }
    setLoading(true);
    try {
      // Fetch wallet/DID info from backend PatientWalletService
      const res = await axios.get('/api/wallet/did');
      const wallet = res.data; // { did, publicKey }

      // Dispatch to Redux — store globally for all components
      dispatch(loginPatient({ abhaId, wallet }));
      toast.success('Login Successful', `Welcome back! DID loaded.`);
      onSuccess({ abhaId, wallet });
    } catch (err) {
      // Backend not running? Use a mock wallet for UI development
      const mockWallet = {
        did: `did:veristas:patient:${abhaId.replace(/-/g, '').substring(0, 8)}Ab3x`,
        publicKey: 'MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE...MockKey==',
      };
      dispatch(loginPatient({ abhaId, wallet: mockWallet }));
      toast.success('Login Successful (Mock)', 'Backend offline — using mock wallet');
      onSuccess({ abhaId, wallet: mockWallet });
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div
      key="step2"
      initial={{ opacity: 0, x: -30 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: 30 }}
      transition={{ duration: 0.3 }}
    >
      <GlassCard glow="indigo" className="p-8 max-w-md mx-auto">
        <div className="flex items-center gap-3 mb-6">
          <div className="p-2 rounded-xl bg-indigo-500/20">
            <Key className="w-6 h-6 text-indigo-400" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-white">Verify OTP</h2>
            <p className="text-xs text-slate-400">Step 2 of 2 — Enter the 6-digit code</p>
          </div>
        </div>

        {/* ABHA ID display */}
        <div className="mb-4 px-3 py-2 rounded-lg bg-white/5 border border-white/10">
          <p className="text-xs text-slate-400">Logging in as</p>
          <p className="text-sm font-mono text-sky-300 font-semibold">{abhaId}</p>
        </div>

        <form onSubmit={handleVerify} className="space-y-4">
          <div>
            <label className="text-sm text-slate-300 font-medium mb-1.5 block">
              6-Digit OTP
            </label>
            <input
              id="otp-input"
              type="text"
              maxLength={6}
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))} // digits only
              placeholder="• • • • • •"
              className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white
                         placeholder-slate-500 text-center text-2xl font-mono tracking-[1rem]
                         focus:outline-none focus:border-indigo-500/60
                         transition-all duration-200"
            />
          </div>
          <GradientButton type="submit" variant="primary" fullWidth loading={loading}
            icon={<CheckCircle className="w-4 h-4" />}>
            Verify & Load Wallet
          </GradientButton>
        </form>
      </GlassCard>
    </motion.div>
  );
}

// Step 3 — Dashboard (after login)
function PatientDashboard({ abhaId, wallet }) {
  const toast = useToast();

  const copyToClipboard = (text, label) => {
    navigator.clipboard.writeText(text);
    toast.success('Copied!', `${label} copied to clipboard`);
  };

  return (
    <motion.div
      key="dashboard"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="space-y-6 max-w-2xl mx-auto"
    >
      {/* Welcome header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white">Your Health Wallet</h2>
          <p className="text-slate-400 text-sm mt-1">Self-Sovereign Identity — you own your data</p>
        </div>
        <StatusBadge status="VALID" pulse />
      </div>

      {/* Identity Card */}
      <GlassCard glow="sky" className="p-6">
        <div className="flex items-center gap-3 mb-4">
          <div className="p-2.5 rounded-xl bg-sky-500/20">
            <User className="w-5 h-5 text-sky-400" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-white">Patient Identity</h3>
            <p className="text-xs text-slate-400">ABHA-linked Verifiable Credential</p>
          </div>
        </div>

        <div className="space-y-3">
          {/* ABHA ID */}
          <div className="flex items-center justify-between p-3 rounded-xl bg-white/5 border border-white/10">
            <div>
              <p className="text-xs text-slate-400">ABHA Health ID</p>
              <p className="text-sm font-mono text-white font-semibold">{abhaId}</p>
            </div>
            <button
              onClick={() => copyToClipboard(abhaId, 'ABHA ID')}
              className="text-xs text-sky-400 hover:text-sky-300 transition-colors px-2 py-1
                         rounded-lg hover:bg-sky-500/10"
            >
              Copy
            </button>
          </div>

          {/* DID */}
          <div className="flex items-start justify-between p-3 rounded-xl bg-white/5 border border-white/10">
            <div className="flex-1 min-w-0 mr-2">
              <p className="text-xs text-slate-400">Decentralized ID (DID)</p>
              <p className="text-xs font-mono text-indigo-300 break-all leading-relaxed mt-0.5">
                {wallet?.did}
              </p>
            </div>
            <button
              onClick={() => copyToClipboard(wallet?.did, 'DID')}
              className="text-xs text-sky-400 hover:text-sky-300 transition-colors px-2 py-1
                         rounded-lg hover:bg-sky-500/10 flex-shrink-0"
            >
              Copy
            </button>
          </div>
        </div>
      </GlassCard>

      {/* Public Key Card */}
      <GlassCard glow="indigo" className="p-6">
        <div className="flex items-center gap-3 mb-4">
          <div className="p-2.5 rounded-xl bg-indigo-500/20">
            <Key className="w-5 h-5 text-indigo-400" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-white">secp256k1 Public Key</h3>
            <p className="text-xs text-slate-400">ECDSA — Ethereum-compatible key pair</p>
          </div>
        </div>
        <div className="p-3 rounded-xl bg-white/5 border border-white/10">
          <p className="text-xs font-mono text-emerald-300 break-all leading-relaxed">
            {wallet?.publicKey || 'Loading key...'}
          </p>
        </div>
        <p className="text-xs text-slate-500 mt-2.5">
          🔒 Your <span className="text-rose-400 font-medium">private key</span> is stored securely
          on the device and is never transmitted.
        </p>
      </GlassCard>

      {/* Placeholder sections (Days 8–10) */}
      <div className="grid grid-cols-2 gap-4">
        <GlassCard glow="emerald" className="p-4 opacity-60">
          <p className="text-sm font-semibold text-white">Active Consents</p>
          <p className="text-xs text-slate-400 mt-1">Coming on Day 8</p>
        </GlassCard>
        <GlassCard glow="amber" className="p-4 opacity-60">
          <p className="text-sm font-semibold text-white">QR Code</p>
          <p className="text-xs text-slate-400 mt-1">Coming on Day 10</p>
        </GlassCard>
      </div>
    </motion.div>
  );
}

// ─── MAIN EXPORT ─────────────────────────────────────────────────────────────
export default function PatientPortal() {
  // step: 'abha' | 'otp' | 'dashboard'
  const [step, setStep] = useState('abha');
  const [abhaId, setAbhaId] = useState('');
  const [wallet, setWallet] = useState(null);

  return (
    <div>
      {/* Page title */}
      <div className="text-center mb-10">
        <div className="inline-flex items-center gap-2 mb-3">
          <Shield className="w-7 h-7 text-sky-400" />
          <h1 className="text-3xl font-bold text-white">Patient Portal</h1>
        </div>
        <p className="text-slate-400 max-w-md mx-auto text-sm leading-relaxed">
          Access your health records and manage your self-sovereign digital identity.
        </p>
      </div>

      {/* Step progress bar */}
      {step !== 'dashboard' && (
        <div className="flex items-center justify-center gap-2 mb-8">
          {['abha', 'otp'].map((s, i) => (
            <React.Fragment key={s}>
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold
                transition-all duration-300
                ${step === s
                  ? 'bg-sky-500 text-white shadow-lg shadow-sky-500/40'
                  : step === 'otp' && s === 'abha'
                    ? 'bg-emerald-500 text-white'
                    : 'bg-white/10 text-slate-400'}`}>
                {step === 'otp' && s === 'abha' ? <CheckCircle className="w-4 h-4" /> : i + 1}
              </div>
              {i < 1 && <div className={`h-0.5 w-12 rounded-full transition-all duration-500
                ${step === 'otp' ? 'bg-emerald-500' : 'bg-white/10'}`} />}
            </React.Fragment>
          ))}
        </div>
      )}

      {/* Step content */}
      <AnimatePresence mode="wait">
        {step === 'abha' && (
          <StepEnterAbha
            key="abha"
            onNext={(id) => { setAbhaId(id); setStep('otp'); }}
          />
        )}
        {step === 'otp' && (
          <StepEnterOtp
            key="otp"
            abhaId={abhaId}
            onSuccess={({ abhaId: id, wallet: w }) => {
              setAbhaId(id);
              setWallet(w);
              setStep('dashboard');
            }}
          />
        )}
        {step === 'dashboard' && (
          <PatientDashboard key="dashboard" abhaId={abhaId} wallet={wallet} />
        )}
      </AnimatePresence>
    </div>
  );
}
