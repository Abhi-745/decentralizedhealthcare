import React, { useState, useEffect } from 'react';
import { Key, ShieldCheck, CheckCircle2, AlertCircle, Smartphone, Lock, Fingerprint } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import axios from 'axios';

export default function PatientPortal() {
  const [abhaId, setAbhaId] = useState('');
  const [otp, setOtp] = useState('');
  
  // States: loading, login (returning user), input_abha (new user), input_otp, processing, done, error
  const [step, setStep] = useState('loading'); 
  const [errorMsg, setErrorMsg] = useState('');
  const [wallet, setWallet] = useState(null);

  // Check for returning user on component mount
  useEffect(() => {
    const savedWallet = localStorage.getItem('veristas_wallet');
    if (savedWallet) {
      setWallet(JSON.parse(savedWallet));
      setStep('login');
    } else {
      setStep('input_abha');
    }
  }, []);

  const handleRequestOtp = () => {
    if (!abhaId || abhaId.length < 5) {
      setErrorMsg('Please enter a valid ABHA ID.');
      return;
    }
    setErrorMsg('');
    setStep('input_otp');
  };

  const handleVerifyOtp = async () => {
    if (otp !== '123456') {
      setErrorMsg('Invalid OTP. For this demo, please use 123456.');
      return;
    }
    
    setStep('processing');
    setErrorMsg('');

    try {
      const keyPair = await window.crypto.subtle.generateKey(
        { name: "ECDSA", namedCurve: "P-256" },
        true,
        ["sign", "verify"]
      );

      const exportedPublicKey = await window.crypto.subtle.exportKey("spki", keyPair.publicKey);
      const exportedPublicKeyBuffer = new Uint8Array(exportedPublicKey);
      const base64PublicKey = btoa(String.fromCharCode(...exportedPublicKeyBuffer));
      
      const uniqueKeyBytes = base64PublicKey.substring(base64PublicKey.length - 32).replace(/[^a-zA-Z0-9]/g, "");
      const generatedDid = `did:veristas:patient:${uniqueKeyBytes}`;

      const response = await axios.post('/api/patients/register', {
        abhaId: abhaId,
        did: generatedDid
      });

      const newWallet = {
        did: generatedDid,
        jwt: response.data.proof.jwt,
        // In a real app, private keys shouldn't be serialized to localStorage without encryption!
        // But for this frontend simulation, we store a mock reference.
        isSecured: true 
      };

      localStorage.setItem('veristas_wallet', JSON.stringify(newWallet));
      setWallet(newWallet);
      setStep('done');
    } catch (error) {
      setStep('error');
      setErrorMsg(error.response?.data?.error || error.response?.data || 'Failed to generate identity. This ABHA ID might already be registered.');
    }
  };

  const handleBiometricLogin = () => {
    setStep('processing');
    // Simulate biometric delay
    setTimeout(() => {
      setStep('done');
    }, 1500);
  };

  const resetFlow = () => {
    localStorage.removeItem('veristas_wallet');
    setStep('input_abha');
    setAbhaId('');
    setOtp('');
    setWallet(null);
    setErrorMsg('');
  };

  if (step === 'loading') return null;

  return (
    <div className="max-w-2xl mx-auto mt-10">
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="glass-panel p-8"
      >
        <div className="flex items-center space-x-4 mb-6">
          <div className="bg-sky-500/20 p-3 rounded-xl border border-sky-500/30">
            <Key className="w-6 h-6 text-sky-400" />
          </div>
          <div>
            <h2 className="text-2xl font-bold text-white">Patient Identity Wallet</h2>
            <p className="text-slate-400">Manage your Web3 Zero-Trust Identity</p>
          </div>
        </div>

        <AnimatePresence mode="wait">
          
          {/* --- RETURNING USER LOGIN SCREEN --- */}
          {step === 'login' && wallet && (
            <motion.div key="login" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} className="space-y-6 text-center py-6">
              <div className="inline-flex items-center justify-center w-20 h-20 bg-slate-800 rounded-full mb-4 shadow-[0_0_30px_rgba(56,189,248,0.2)]">
                <Fingerprint className="w-10 h-10 text-sky-400" />
              </div>
              <h3 className="text-xl font-medium text-white">Welcome Back</h3>
              <p className="text-slate-400 text-sm max-w-md mx-auto mb-6">
                Your cryptographic keys are stored safely on this device. Please authenticate to unlock your wallet.
              </p>
              
              <button onClick={handleBiometricLogin} className="w-full btn-primary flex justify-center items-center space-x-2">
                <Fingerprint className="w-5 h-5" />
                <span>Simulate Biometric Unlock (FaceID)</span>
              </button>
              
              <button onClick={resetFlow} className="text-sm text-slate-500 hover:text-rose-400 transition underline mt-4">
                This isn't my device (Clear Wallet)
              </button>
            </motion.div>
          )}

          {/* --- NEW USER REGISTRATION FLOW --- */}
          {(step === 'input_abha' || step === 'error') && !wallet && (
            <motion.div key="step1" initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 20 }} className="space-y-6">
              <div className="bg-sky-500/10 border border-sky-500/20 p-4 rounded-lg mb-6">
                <p className="text-sky-300 text-sm font-medium">New Device Detected</p>
                <p className="text-slate-400 text-xs mt-1">Please register your ABHA ID to provision keys to this device.</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">ABHA ID (Indian Health ID)</label>
                <input
                  type="text"
                  placeholder="11-1111-1111-1111"
                  className="input-field"
                  value={abhaId}
                  onChange={(e) => setAbhaId(e.target.value)}
                />
              </div>
              
              {errorMsg && (
                <div className="bg-rose-500/10 border border-rose-500/30 text-rose-400 p-4 rounded-lg flex items-start space-x-3">
                  <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
                  <p className="text-sm">{errorMsg}</p>
                </div>
              )}

              <button onClick={handleRequestOtp} className="w-full btn-primary flex justify-center items-center space-x-2">
                <Smartphone className="w-5 h-5" />
                <span>Send Authentication OTP</span>
              </button>
            </motion.div>
          )}

          {step === 'input_otp' && (
            <motion.div key="step2" initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 20 }} className="space-y-6">
              <div className="bg-indigo-500/10 border border-indigo-500/30 p-4 rounded-lg flex items-start space-x-3">
                <Smartphone className="w-5 h-5 text-indigo-400 flex-shrink-0 mt-0.5" />
                <div>
                  <p className="text-sm text-indigo-300 font-medium">OTP sent to Aadhaar-linked mobile</p>
                  <p className="text-xs text-indigo-400/70 mt-1">For this demo, please enter 123456</p>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Enter 6-Digit OTP</label>
                <input
                  type="text"
                  placeholder="123456"
                  className="input-field tracking-widest font-mono text-center text-xl"
                  maxLength={6}
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                />
              </div>

              {errorMsg && (
                <div className="bg-rose-500/10 border border-rose-500/30 text-rose-400 p-4 rounded-lg flex items-start space-x-3">
                  <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
                  <p className="text-sm">{errorMsg}</p>
                </div>
              )}

              <div className="flex space-x-3">
                <button onClick={() => setStep('input_abha')} className="px-6 py-2.5 rounded-lg border border-slate-700 text-slate-300 hover:bg-slate-800 transition">
                  Back
                </button>
                <button onClick={handleVerifyOtp} className="flex-1 btn-success flex justify-center items-center space-x-2">
                  <Lock className="w-5 h-5" />
                  <span>Verify & Generate Identity</span>
                </button>
              </div>
            </motion.div>
          )}

          {step === 'processing' && (
            <motion.div key="processing" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="flex flex-col items-center justify-center py-12 space-y-4">
              <div className="w-12 h-12 border-4 border-sky-500/30 border-t-sky-500 rounded-full animate-spin" />
              <h3 className="text-lg font-medium text-sky-400">Unlocking Hardware Keystore...</h3>
              <p className="text-slate-500 text-sm">Securing your identity on the device.</p>
            </motion.div>
          )}

          {step === 'done' && wallet && (
            <motion.div key="done" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="space-y-6">
              <div className="bg-emerald-500/10 border border-emerald-500/30 p-4 rounded-lg flex items-center space-x-3">
                <CheckCircle2 className="w-6 h-6 text-emerald-400" />
                <div>
                  <h3 className="text-emerald-400 font-medium">Wallet Active</h3>
                  <p className="text-emerald-500/70 text-sm">Your private key is unlocked and ready for use.</p>
                </div>
              </div>

              <div className="space-y-4">
                <div className="glass-card">
                  <label className="text-xs text-slate-400 uppercase tracking-wider font-semibold">Your Decentralized ID (DID)</label>
                  <div className="font-mono text-sm text-sky-300 mt-1 break-all bg-slate-900/50 p-2 rounded border border-slate-700/50">
                    {wallet.did}
                  </div>
                </div>
                <div className="glass-card">
                  <label className="text-xs text-slate-400 uppercase tracking-wider font-semibold">Verifiable Credential (JWT)</label>
                  <div className="font-mono text-xs text-slate-400 mt-1 break-all bg-slate-900/50 p-2 rounded border border-slate-700/50 h-24 overflow-y-auto">
                    {wallet.jwt}
                  </div>
                </div>
              </div>
              
              <button onClick={resetFlow} className="w-full bg-slate-800 hover:bg-slate-700 text-white font-medium py-2.5 px-6 rounded-lg transition-colors border border-slate-700">
                Wipe Device Wallet (Simulate Device Lost)
              </button>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    </div>
  );
}
