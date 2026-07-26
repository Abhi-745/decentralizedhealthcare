import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  FileHeart, Search, AlertCircle, HeartPulse, Pill,
  Stethoscope, Clock, User, ShieldCheck, RefreshCw
} from 'lucide-react';
import axios from 'axios';

// ─── Field Card ───────────────────────────────────────────────────────────────
function DataField({ icon: Icon, label, value, color = 'indigo', empty }) {
  return (
    <div className={`flex gap-3 p-4 rounded-xl border
      ${empty ? 'bg-white/3 border-white/6' : `bg-${color}-500/8 border-${color}-500/20`}`}>
      <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0
        ${empty ? 'bg-white/5' : `bg-${color}-500/15`}`}>
        <Icon className={`w-4 h-4 ${empty ? 'text-white/20' : `text-${color}-400`}`} />
      </div>
      <div>
        <p className="text-white/35 text-[10px] uppercase tracking-wide font-semibold mb-0.5">{label}</p>
        <p className={`text-sm font-medium ${empty ? 'text-white/20 italic' : 'text-white/85'}`}>
          {value || '—'}
        </p>
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function EMRViewerPage() {
  const [patientDid, setPatientDid] = useState('did:veritas:patient-001');
  const [token, setToken]           = useState('');
  const [record, setRecord]         = useState(null);
  const [loading, setLoading]       = useState(false);
  const [error, setError]           = useState(null);

  const fetchEMR = async () => {
    if (!patientDid.trim()) return;
    setLoading(true);
    setError(null);
    setRecord(null);
    try {
      const { data } = await axios.get(`/api/emr/${encodeURIComponent(patientDid)}`, {
        headers: { Authorization: token || 'Bearer demo-token' },
      });
      setRecord(data);
    } catch (e) {
      const status = e.response?.status;
      setError(
        status === 401 ? '401 — Invalid or revoked credential.' :
        status === 403 ? '403 — Access denied. No consent or emergency session found.' :
        'Backend unreachable — check Railway is running.'
      );
    } finally {
      setLoading(false);
    }
  };

  const isEmpty = (v) => !v || v === 'No records found (New Patient)';

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-6">
      <div className="max-w-xl mx-auto space-y-5">

        {/* Header */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center">
            <FileHeart className="w-5 h-5 text-rose-400" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">EMR Viewer</h1>
            <p className="text-white/40 text-sm">Electronic Medical Record · GET /api/emr/:did</p>
          </div>
        </div>

        {/* Search panel */}
        <div className="p-5 rounded-2xl bg-white/4 border border-white/8 space-y-3">
          <div>
            <label className="text-white/40 text-xs font-semibold uppercase tracking-wide block mb-1.5">
              Patient DID
            </label>
            <input
              value={patientDid}
              onChange={e => setPatientDid(e.target.value)}
              placeholder="did:veritas:patient-001"
              className="w-full bg-black/30 border border-white/10 rounded-xl px-4 py-2.5
                text-white text-sm font-mono placeholder-white/20
                focus:outline-none focus:border-rose-500/50 transition-all"
            />
          </div>

          <div>
            <label className="text-white/40 text-xs font-semibold uppercase tracking-wide block mb-1.5">
              Authorization Token (optional)
            </label>
            <input
              value={token}
              onChange={e => setToken(e.target.value)}
              placeholder="Bearer eyJhbGciOiJIUzI1NiJ9..."
              className="w-full bg-black/30 border border-white/10 rounded-xl px-4 py-2.5
                text-white text-sm font-mono placeholder-white/20
                focus:outline-none focus:border-rose-500/50 transition-all"
            />
          </div>

          <button
            onClick={fetchEMR}
            disabled={loading}
            className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl
              bg-rose-500/15 border border-rose-500/30 text-rose-400
              hover:bg-rose-500/25 transition-all font-medium text-sm disabled:opacity-50"
          >
            {loading
              ? <><motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}><RefreshCw className="w-4 h-4" /></motion.div> Fetching…</>
              : <><Search className="w-4 h-4" /> Fetch Medical Record</>
            }
          </button>
        </div>

        {/* Error */}
        <AnimatePresence>
          {error && (
            <motion.div
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              className="flex items-center gap-3 p-4 rounded-2xl bg-red-500/10 border border-red-500/30"
            >
              <AlertCircle className="w-5 h-5 text-red-400 shrink-0" />
              <p className="text-red-300 text-sm">{error}</p>
            </motion.div>
          )}
        </AnimatePresence>

        {/* EMR Card */}
        <AnimatePresence>
          {record && (
            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              className="rounded-2xl bg-white/4 border border-white/8 overflow-hidden"
            >
              {/* Card header */}
              <div className="px-5 py-4 bg-gradient-to-r from-rose-500/10 to-transparent border-b border-white/8 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <FileHeart className="w-4 h-4 text-rose-400" />
                  <span className="text-white font-semibold text-sm">Medical Record</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                  <span className="text-emerald-400 text-xs font-medium">Access Granted</span>
                </div>
              </div>

              {/* Fields */}
              <div className="p-5 space-y-3">
                <DataField
                  icon={User}
                  label="Patient DID"
                  value={record.patientDid}
                  color="indigo"
                  empty={!record.patientDid}
                />
                <DataField
                  icon={Stethoscope}
                  label="Diagnosis"
                  value={record.diagnosis}
                  color="rose"
                  empty={isEmpty(record.diagnosis)}
                />
                <DataField
                  icon={HeartPulse}
                  label="Vitals"
                  value={record.vitals}
                  color="sky"
                  empty={!record.vitals}
                />
                <DataField
                  icon={Pill}
                  label="Prescription"
                  value={record.prescription}
                  color="violet"
                  empty={!record.prescription}
                />

                {/* Meta */}
                <div className="pt-2 grid grid-cols-2 gap-2">
                  {record.updatedByDid && (
                    <div className="p-3 rounded-xl bg-white/5">
                      <p className="text-white/30 text-[10px] uppercase tracking-wide">Updated By</p>
                      <p className="text-white/60 text-xs font-mono mt-0.5 truncate">{record.updatedByDid}</p>
                    </div>
                  )}
                  {record.esid && (
                    <div className="p-3 rounded-xl bg-white/5">
                      <p className="text-white/30 text-[10px] uppercase tracking-wide">Emergency Session</p>
                      <p className="text-white/60 text-xs font-mono mt-0.5">{record.esid}</p>
                    </div>
                  )}
                </div>
              </div>

              {/* Footer timestamp */}
              <div className="px-5 py-3 border-t border-white/6 flex items-center gap-1.5">
                <Clock className="w-3 h-3 text-white/20" />
                <span className="text-white/25 text-xs">
                  {record.updatedAt
                    ? `Last updated: ${new Date(record.updatedAt).toLocaleString()}`
                    : 'Timestamp not available'}
                </span>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* OR security gate explainer */}
        <div className="p-4 rounded-2xl bg-white/3 border border-white/8">
          <p className="text-white/40 text-xs font-semibold uppercase tracking-wide mb-2">
            Day 16 — The OR Security Gate
          </p>
          <div className="grid grid-cols-2 gap-2 mb-3">
            {[
              { label: 'OPA Override', desc: 'Active emergency session at correct stage', color: 'sky' },
              { label: 'Patient Consent', desc: 'Explicit grant from patient wallet', color: 'violet' },
            ].map(({ label, desc, color }) => (
              <div key={label} className={`p-3 rounded-xl bg-${color}-500/8 border border-${color}-500/20`}>
                <p className={`text-${color}-400 text-xs font-semibold`}>{label}</p>
                <p className="text-white/30 text-[10px] mt-0.5 leading-tight">{desc}</p>
              </div>
            ))}
          </div>
          <p className="text-white/30 text-xs text-center font-mono bg-black/20 rounded-lg py-2">
            if (!isEmergencyOverride && !hasConsent) → 403 FORBIDDEN
          </p>
        </div>

      </div>
    </div>
  );
}
