import React, { useState } from 'react';
import { motion } from 'framer-motion';
import {
  Shield, Database, Cpu, Link2, Key, FileCode,
  Globe, Lock, Layers, Server, Zap, ChevronRight
} from 'lucide-react';

// ─── Colour palette per layer ─────────────────────────────────────────────────
const LAYERS = [
  {
    id: 'frontend',
    label: 'Frontend Layer',
    color: '#6366f1',
    bg: 'bg-indigo-500/8',
    border: 'border-indigo-500/20',
    text: 'text-indigo-400',
    icon: Globe,
    description: 'React + Vite SPA, deployed on Vercel. 10 pages covering the full patient and staff journey.',
    components: [
      { name: 'HomePage',            role: 'Landing page with animated hero' },
      { name: 'EmergencyDashboard',  role: 'Real-time OPA-gated session control' },
      { name: 'ChainVisualizer',     role: 'Live SHA-256 blockchain viewer' },
      { name: 'VCViewer',            role: 'W3C Verifiable Credential inspector' },
      { name: 'EMRViewer',           role: 'Electronic Medical Record viewer' },
      { name: 'ConsentManager',      role: 'Patient data sovereignty controls' },
    ],
  },
  {
    id: 'api',
    label: 'API Gateway (Spring Boot)',
    color: '#0ea5e9',
    bg: 'bg-sky-500/8',
    border: 'border-sky-500/20',
    text: 'text-sky-400',
    icon: Server,
    description: 'Spring Boot 3.x REST API on Railway. 10 controllers, 180+ unit + integration tests, constructor-injected dependencies.',
    components: [
      { name: 'EmergencySessionController', role: 'Session state machine (5 stages)' },
      { name: 'EMRController',              role: 'Double-auth + OR security gate' },
      { name: 'AuditController',            role: 'Ledger read + integrity check' },
      { name: 'AuthController',             role: 'HMAC-SHA256 JWT issuance' },
      { name: 'ResourceController',         role: 'Full Zero-Trust pipeline' },
      { name: 'ConsentController',          role: 'Grant / revoke patient consent' },
    ],
  },
  {
    id: 'security',
    label: 'Security & Policy Layer',
    color: '#8b5cf6',
    bg: 'bg-violet-500/8',
    border: 'border-violet-500/20',
    text: 'text-violet-400',
    icon: Shield,
    description: 'Zero-Trust architecture. Every request is verified by identity (VC/JWT) then authorised by OPA Rego policy.',
    components: [
      { name: 'JwtService',               role: 'HMAC-SHA256 token sign/verify' },
      { name: 'CredentialService',        role: 'ECDSA VC issue + verify + revoke' },
      { name: 'OpaService',               role: 'ABAC via Open Policy Agent Rego' },
      { name: 'acute_care.rego',          role: 'Policy: role + stage + action rules' },
      { name: 'PatientWalletService',     role: 'BouncyCastle ECDSA key generation' },
      { name: 'CredentialIssuanceService',role: 'W3C VC creation + DID document' },
    ],
  },
  {
    id: 'audit',
    label: 'Audit Chain (3 Layers)',
    color: '#f59e0b',
    bg: 'bg-amber-500/8',
    border: 'border-amber-500/20',
    text: 'text-amber-400',
    icon: Link2,
    description: 'Every access event is written to 3 independent stores simultaneously. Tamper any one — the others detect it.',
    components: [
      { name: 'Layer 1: In-Memory Chain', role: 'SHA-256 linked list, instant reads' },
      { name: 'Layer 2: PostgreSQL',       role: 'Durable persistence on Railway' },
      { name: 'Layer 3: Ethereum',         role: 'VeristasAudit.sol on Ganache/testnet' },
      { name: 'AuditLogBlock',             role: 'Immutable DTO — no setHash() allowed' },
      { name: 'isChainValid()',            role: 'O(n) tamper detection algorithm' },
      { name: 'BlockchainAuditService',    role: 'Orchestrates all 3 layers' },
    ],
  },
  {
    id: 'data',
    label: 'Data Layer',
    color: '#10b981',
    bg: 'bg-emerald-500/8',
    border: 'border-emerald-500/20',
    text: 'text-emerald-400',
    icon: Database,
    description: 'PostgreSQL (prod) + H2 (test). Spring Data JPA repositories. Entities use constructor injection, no Lombok @Data on audit types.',
    components: [
      { name: 'MedicalRecord',             role: 'Patient EMR (diagnosis, vitals, Rx)' },
      { name: 'EmergencySessionEntity',    role: 'Session ID + stage + patient DID' },
      { name: 'Consent',                   role: 'Patient-granted delegate access' },
      { name: 'AuditBlockEntity',          role: 'Persisted copy of each chain block' },
      { name: 'VerifiableCredentialEntity',role: 'Issued VC + revocation status' },
      { name: 'PatientEntity',             role: 'DID + ECDSA public key' },
    ],
  },
];

// ─── Request flow steps ───────────────────────────────────────────────────────
const FLOW_STEPS = [
  { n: 1, label: 'HTTP Request',   desc: 'React → /api/* (Vercel proxy or Railway)',  color: 'indigo' },
  { n: 2, label: 'Identity Check', desc: 'VC via ECDSA or JWT via HMAC-SHA256',       color: 'violet' },
  { n: 3, label: 'OPA Decision',   desc: 'Rego checks role + stage + action (<20 ms)', color: 'violet' },
  { n: 4, label: 'Business Logic', desc: 'Controller → Service → Repository',          color: 'sky'    },
  { n: 5, label: 'Audit Sealed',   desc: 'SHA-256 block chained + persisted + on-chain',color: 'amber' },
  { n: 6, label: 'Response',       desc: '200 OK or 403 FORBIDDEN with audit trail',   color: 'emerald'},
];

// ─── Component: Layer card ────────────────────────────────────────────────────
function LayerCard({ layer, isActive, onClick }) {
  const Icon = layer.icon;
  return (
    <motion.button
      onClick={onClick}
      whileHover={{ scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      className={`w-full text-left p-4 rounded-2xl border transition-all
        ${isActive
          ? `${layer.bg} ${layer.border} ring-1 ring-inset`
          : 'bg-white/3 border-white/8 hover:border-white/15'}`}
      style={isActive ? { '--tw-ring-color': layer.color + '40' } : {}}
    >
      <div className="flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0"
          style={{ background: layer.color + '20', border: `1px solid ${layer.color}40` }}>
          <Icon className="w-4 h-4" style={{ color: layer.color }} />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-white text-sm font-semibold truncate">{layer.label}</p>
          <p className="text-white/35 text-xs truncate">{layer.components.length} components</p>
        </div>
        <ChevronRight className={`w-4 h-4 shrink-0 transition-transform
          ${isActive ? 'rotate-90 text-white/50' : 'text-white/20'}`} />
      </div>
    </motion.button>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function ArchitecturePage() {
  const [activeLayer, setActiveLayer] = useState('frontend');
  const layer = LAYERS.find(l => l.id === activeLayer);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-6">
      <div className="max-w-4xl mx-auto space-y-6">

        {/* Header */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-violet-500/10 border border-violet-500/20 flex items-center justify-center">
            <Layers className="w-5 h-5 text-violet-400" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">System Architecture</h1>
            <p className="text-white/40 text-sm">Zero-Trust Decentralised Medical Identity · VeristasId</p>
          </div>
        </div>

        {/* Stats row */}
        <div className="grid grid-cols-4 gap-3">
          {[
            { v: '188+', l: 'Tests Passing', c: 'text-emerald-400' },
            { v: '10',   l: 'Controllers',   c: 'text-sky-400' },
            { v: '3',    l: 'Audit Layers',  c: 'text-amber-400' },
            { v: '256',  l: 'Bit ECDSA',     c: 'text-violet-400' },
          ].map(({ v, l, c }) => (
            <div key={l} className="text-center p-3 rounded-xl bg-white/3 border border-white/8">
              <p className={`text-2xl font-bold ${c}`}>{v}</p>
              <p className="text-white/35 text-xs">{l}</p>
            </div>
          ))}
        </div>

        {/* Two-column: layers + detail */}
        <div className="grid sm:grid-cols-5 gap-4">

          {/* Left: layer selector */}
          <div className="sm:col-span-2 space-y-2">
            {LAYERS.map(l => (
              <LayerCard
                key={l.id}
                layer={l}
                isActive={activeLayer === l.id}
                onClick={() => setActiveLayer(l.id)}
              />
            ))}
          </div>

          {/* Right: detail panel */}
          <motion.div
            key={activeLayer}
            initial={{ opacity: 0, x: 12 }}
            animate={{ opacity: 1, x: 0 }}
            className={`sm:col-span-3 rounded-2xl border p-5 space-y-4
              ${layer.bg} ${layer.border}`}
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl flex items-center justify-center"
                style={{ background: layer.color + '20', border: `1px solid ${layer.color}40` }}>
                <layer.icon className="w-5 h-5" style={{ color: layer.color }} />
              </div>
              <h2 className="text-white font-bold text-lg">{layer.label}</h2>
            </div>

            <p className="text-white/55 text-sm leading-relaxed">{layer.description}</p>

            <div className="space-y-1.5">
              {layer.components.map(({ name, role }) => (
                <div key={name} className="flex items-start gap-3 p-3 rounded-xl bg-black/20">
                  <div className="w-1.5 h-1.5 rounded-full mt-1.5 shrink-0"
                    style={{ background: layer.color }} />
                  <div>
                    <p className="text-white/80 text-sm font-mono font-medium">{name}</p>
                    <p className="text-white/35 text-xs">{role}</p>
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        </div>

        {/* Request flow diagram */}
        <div className="p-5 rounded-2xl bg-white/3 border border-white/8 space-y-4">
          <div className="flex items-center gap-2">
            <Zap className="w-4 h-4 text-amber-400" />
            <p className="text-white font-semibold text-sm">Every Request — Zero-Trust Pipeline</p>
          </div>

          <div className="flex items-stretch gap-0 overflow-x-auto pb-2">
            {FLOW_STEPS.map((step, i) => (
              <React.Fragment key={step.n}>
                <div className={`flex flex-col items-center p-3 rounded-xl min-w-[100px]
                  bg-${step.color}-500/8 border border-${step.color}-500/20`}>
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center
                    text-xs font-bold text-${step.color}-400
                    bg-${step.color}-500/20 mb-2 shrink-0`}>
                    {step.n}
                  </div>
                  <p className={`text-${step.color}-400 text-[11px] font-semibold text-center leading-tight`}>
                    {step.label}
                  </p>
                  <p className="text-white/25 text-[10px] text-center mt-1 leading-tight">
                    {step.desc}
                  </p>
                </div>
                {i < FLOW_STEPS.length - 1 && (
                  <div className="flex items-center px-0.5 shrink-0">
                    <div className="w-4 h-px bg-white/15" />
                    <div className="w-0 h-0 border-t-4 border-b-4 border-l-4 border-transparent border-l-white/15" />
                  </div>
                )}
              </React.Fragment>
            ))}
          </div>
        </div>

        {/* Tech stack badges */}
        <div className="p-5 rounded-2xl bg-white/3 border border-white/8">
          <p className="text-white/40 text-xs font-semibold uppercase tracking-wide mb-3">Full Tech Stack</p>
          <div className="flex flex-wrap gap-2">
            {[
              ['Spring Boot 3.x',         'sky'],
              ['React 18 + Vite',          'indigo'],
              ['PostgreSQL + H2 (test)',   'green'],
              ['Open Policy Agent',        'violet'],
              ['BouncyCastle ECDSA',       'amber'],
              ['W3C Verifiable Credentials','blue'],
              ['SHA-256 Blockchain',       'rose'],
              ['Ethereum / Web3j',         'purple'],
              ['JUnit 5 + Mockito',        'emerald'],
              ['Railway + Vercel',         'sky'],
              ['Lombok + MapStruct',       'orange'],
              ['Jackson + Spring MVC',     'teal'],
            ].map(([name, color]) => (
              <span key={name}
                className={`px-3 py-1 rounded-full text-xs font-medium
                  bg-${color}-500/10 border border-${color}-500/25 text-${color}-400`}>
                {name}
              </span>
            ))}
          </div>
        </div>

      </div>
    </div>
  );
}
