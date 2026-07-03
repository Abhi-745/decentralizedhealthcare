import React, { useState } from 'react';
import { Activity, Stethoscope, Search, AlertTriangle, CheckCircle, FileText } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import axios from 'axios';

export default function HospitalPortal() {
  const [activeRole, setActiveRole] = useState('paramedic');
  const [abhaId, setAbhaId] = useState('');
  
  // Paramedic State
  const [paramedicToken, setParamedicToken] = useState('');
  const [paramedicStatus, setParamedicStatus] = useState(''); // dispatching, fetching, success, error
  const [esid, setEsid] = useState('');
  const [record, setRecord] = useState(null);
  
  // Surgeon State
  const [surgeonToken, setSurgeonToken] = useState('');
  const [surgeonStatus, setSurgeonStatus] = useState('');
  const [diagnosisUpdate, setDiagnosisUpdate] = useState('');

  // 1. Paramedic triggers dispatch
  const triggerDispatch = async () => {
    if (!abhaId) { alert("Enter ABHA ID"); return; }
    setParamedicStatus('dispatching');
    try {
      const resp = await axios.post('/api/emergency/dispatch', { patientId: abhaId });
      setEsid(resp.data.esid);
      setParamedicToken(resp.data.paramedicToken);
      setSurgeonToken(resp.data.surgeonToken);
      setParamedicStatus('dispatched');
    } catch (e) {
      setParamedicStatus('error');
      alert("Failed to dispatch: " + (e.response?.data || e.message));
    }
  };

  // 2. Paramedic fetches record
  const fetchRecord = async () => {
    if (!paramedicToken) { alert("You need a Dispatch Token first!"); return; }
    setParamedicStatus('fetching');
    try {
      const resp = await axios.get(`/api/medical-records/${abhaId}`, {
        headers: { Authorization: `Bearer ${paramedicToken}` }
      });
      setRecord(resp.data);
      setParamedicStatus('success');
    } catch (e) {
      setParamedicStatus('error');
      alert("Access Denied or Not Found");
    }
  };

  // 3. Surgeon Arrives
  const surgeonArrive = async () => {
    if (!esid) { alert("No active ESID"); return; }
    try {
      await axios.post(`/api/emergency/arrive/${esid}`);
      setSurgeonStatus('arrived');
    } catch (e) {
      alert("Failed to arrive: " + e.message);
    }
  };

  return (
    <div className="max-w-4xl mx-auto mt-10">
      <div className="flex justify-center space-x-4 mb-8">
        <button
          onClick={() => setActiveRole('paramedic')}
          className={`flex items-center space-x-2 px-6 py-3 rounded-lg font-medium transition-all ${
            activeRole === 'paramedic' ? 'bg-rose-500 text-white shadow-[0_0_20px_rgba(244,63,94,0.4)]' : 'bg-slate-800 text-slate-400 hover:bg-slate-700'
          }`}
        >
          <Activity className="w-5 h-5" />
          <span>Paramedic Dashboard</span>
        </button>
        <button
          onClick={() => setActiveRole('surgeon')}
          className={`flex items-center space-x-2 px-6 py-3 rounded-lg font-medium transition-all ${
            activeRole === 'surgeon' ? 'bg-indigo-500 text-white shadow-[0_0_20px_rgba(99,102,241,0.4)]' : 'bg-slate-800 text-slate-400 hover:bg-slate-700'
          }`}
        >
          <Stethoscope className="w-5 h-5" />
          <span>Surgeon Dashboard</span>
        </button>
      </div>

      <AnimatePresence mode="wait">
        {activeRole === 'paramedic' && (
          <motion.div
            key="paramedic"
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: 20 }}
            className="glass-panel p-8 border-rose-500/30"
          >
            <div className="flex items-center space-x-3 mb-6 border-b border-slate-800 pb-4">
              <div className="bg-rose-500/20 p-2 rounded-lg">
                <AlertTriangle className="w-6 h-6 text-rose-400" />
              </div>
              <h2 className="text-2xl font-bold text-white">Emergency Response Team</h2>
            </div>

            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Patient ABHA ID (Scan from Victim)</label>
                <div className="flex space-x-3">
                  <input
                    type="text"
                    value={abhaId}
                    onChange={(e) => setAbhaId(e.target.value)}
                    className="input-field flex-1"
                    placeholder="e.g. 99-9999-9999-9999"
                  />
                  <button onClick={triggerDispatch} className="btn-danger whitespace-nowrap">
                    1. Trigger 911 Dispatch
                  </button>
                </div>
              </div>

              {esid && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="bg-rose-500/10 border border-rose-500/20 rounded-xl p-5">
                  <div className="flex items-center space-x-2 mb-2 text-rose-400">
                    <CheckCircle className="w-5 h-5" />
                    <span className="font-semibold">Dispatch Confirmed: {esid}</span>
                  </div>
                  <p className="text-sm text-slate-400 mb-4">You have been granted a temporary Break-Glass JWT.</p>
                  
                  <button onClick={fetchRecord} className="btn-primary w-full flex justify-center items-center space-x-2">
                    <Search className="w-5 h-5" />
                    <span>2. Fetch Zero-Trust Medical Record</span>
                  </button>
                </motion.div>
              )}

              {record && (
                <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="glass-card mt-6">
                  <h3 className="text-lg font-bold text-sky-400 mb-4 flex items-center space-x-2">
                    <FileText className="w-5 h-5" />
                    <span>Patient Vitals & Allergies</span>
                  </h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="bg-slate-900/50 p-3 rounded-lg border border-slate-700">
                      <span className="text-xs text-slate-400 uppercase block">Name</span>
                      <span className="font-medium text-white">{record.patientName}</span>
                    </div>
                    <div className="bg-slate-900/50 p-3 rounded-lg border border-rose-500/30 text-rose-300">
                      <span className="text-xs text-rose-500/70 uppercase block">Blood Group</span>
                      <span className="font-bold">{record.bloodGroup}</span>
                    </div>
                    <div className="col-span-2 bg-slate-900/50 p-3 rounded-lg border border-amber-500/30 text-amber-300">
                      <span className="text-xs text-amber-500/70 uppercase block">Critical Allergies</span>
                      <span className="font-medium">{record.allergies}</span>
                    </div>
                  </div>
                </motion.div>
              )}
            </div>
          </motion.div>
        )}

        {activeRole === 'surgeon' && (
          <motion.div
            key="surgeon"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className="glass-panel p-8 border-indigo-500/30"
          >
            <div className="flex items-center space-x-3 mb-6 border-b border-slate-800 pb-4">
              <div className="bg-indigo-500/20 p-2 rounded-lg">
                <Stethoscope className="w-6 h-6 text-indigo-400" />
              </div>
              <h2 className="text-2xl font-bold text-white">Trauma Surgeon Handover</h2>
            </div>

            {surgeonToken ? (
              <div className="space-y-6">
                <div className="bg-indigo-500/10 border border-indigo-500/20 p-4 rounded-lg">
                  <span className="text-indigo-400 text-sm font-semibold">Active Session: {esid}</span>
                  <p className="text-slate-400 text-sm mt-1">Patient is en-route. Awaiting arrival confirmation.</p>
                </div>

                <button 
                  onClick={surgeonArrive}
                  disabled={surgeonStatus === 'arrived'}
                  className={`w-full py-3 rounded-lg font-bold text-white transition-all ${surgeonStatus === 'arrived' ? 'bg-emerald-500 shadow-[0_0_15px_rgba(16,185,129,0.5)]' : 'bg-indigo-600 hover:bg-indigo-500'}`}
                >
                  {surgeonStatus === 'arrived' ? 'Patient Arrived (Stage Updated)' : 'Mark Patient as Arrived'}
                </button>

                {surgeonStatus === 'arrived' && (
                  <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4 pt-4 border-t border-slate-800">
                    <label className="block text-sm font-medium text-slate-300">Add Post-Op Diagnosis</label>
                    <textarea 
                      className="input-field min-h-[100px]" 
                      placeholder="Enter treatment details..."
                      value={diagnosisUpdate}
                      onChange={(e) => setDiagnosisUpdate(e.target.value)}
                    />
                    <button className="btn-success w-full">Save to Blockchain Audit Log</button>
                  </motion.div>
                )}
              </div>
            ) : (
               <div className="text-center py-10">
                 <p className="text-slate-400">Waiting for Paramedic Dispatch...</p>
                 <p className="text-sm text-slate-500 mt-2">Trigger a dispatch from the Paramedic dashboard first to receive the Surgeon JWT.</p>
               </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
