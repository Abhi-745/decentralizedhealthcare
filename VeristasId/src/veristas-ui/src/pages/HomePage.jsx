import React, { useEffect, useRef, useState } from 'react';
import { motion, useInView } from 'framer-motion';
import {
  Shield, Link, Zap, Lock, Activity, ChevronRight,
  Database, Eye, Key, Cpu, FileCheck, AlertTriangle
} from 'lucide-react';

// ─── Animated gradient orb ────────────────────────────────────────────────────
function Orb({ className }) {
  return (
    <div className={`absolute rounded-full blur-3xl opacity-20 pointer-events-none ${className}`} />
  );
}

// ─── Stat counter ─────────────────────────────────────────────────────────────
function StatCounter({ value, label, color }) {
  const ref = useRef(null);
  const inView = useInView(ref, { once: true });
  const [count, setCount] = useState(0);

  useEffect(() => {
    if (!inView) return;
    const target = parseInt(value);
    const step = Math.ceil(target / 40);
    let curr = 0;
    const interval = setInterval(() => {
      curr = Math.min(curr + step, target);
      setCount(curr);
      if (curr >= target) clearInterval(interval);
    }, 30);
    return () => clearInterval(interval);
  }, [inView, value]);

  return (
    <motion.div
      ref={ref}
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      className="text-center"
    >
      <p className={`text-4xl font-bold ${color}`}>{count}{value.includes('+') ? '+' : ''}</p>
      <p className="text-white/40 text-sm mt-1">{label}</p>
    </motion.div>
  );
}

// ─── Feature card ─────────────────────────────────────────────────────────────
function FeatureCard({ icon: Icon, title, description, color, gradient, delay }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ delay }}
      className={`relative p-6 rounded-2xl bg-gradient-to-br ${gradient}
        border border-white/8 overflow-hidden group hover:border-white/15 transition-all`}
    >
      <div className="absolute -top-10 -right-10 w-32 h-32 rounded-full blur-2xl
        opacity-0 group-hover:opacity-30 transition-all duration-500"
        style={{ background: color }} />

      <div className={`w-12 h-12 rounded-xl flex items-center justify-center mb-4`}
        style={{ background: color + '22', border: `1px solid ${color}44` }}>
        <Icon className="w-6 h-6" style={{ color }} />
      </div>

      <h3 className="text-white font-bold text-lg mb-2">{title}</h3>
      <p className="text-white/50 text-sm leading-relaxed">{description}</p>
    </motion.div>
  );
}

// ─── Tech badge ───────────────────────────────────────────────────────────────
function TechBadge({ label, color }) {
  return (
    <span className={`px-3 py-1 rounded-full text-xs font-semibold border
      bg-${color}-500/10 border-${color}-500/30 text-${color}-400`}>
      {label}
    </span>
  );
}

// ─── Architecture step ────────────────────────────────────────────────────────
function ArchStep({ number, title, description, icon: Icon, color }) {
  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      whileInView={{ opacity: 1, x: 0 }}
      viewport={{ once: true }}
      className="flex gap-4"
    >
      <div className="flex flex-col items-center">
        <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0
          bg-${color}-500/15 border border-${color}-500/30`}>
          <Icon className={`w-5 h-5 text-${color}-400`} />
        </div>
        <div className="w-px flex-1 bg-white/8 mt-2" />
      </div>
      <div className="pb-8">
        <div className="flex items-center gap-2 mb-1">
          <span className={`text-[10px] font-bold text-${color}-400 font-mono`}>STEP {number}</span>
        </div>
        <h4 className="text-white font-semibold text-sm mb-1">{title}</h4>
        <p className="text-white/40 text-xs leading-relaxed">{description}</p>
      </div>
    </motion.div>
  );
}

// ─── Main Landing Page ────────────────────────────────────────────────────────
export default function HomePage({ onEnter }) {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 overflow-hidden">

      {/* Background orbs */}
      <Orb className="w-96 h-96 bg-indigo-500 top-0 -left-32" />
      <Orb className="w-64 h-64 bg-violet-500 top-1/3 right-0" />
      <Orb className="w-80 h-80 bg-sky-500 bottom-0 left-1/2" />

      <div className="relative max-w-4xl mx-auto px-6 py-16 space-y-24">

        {/* ── Hero ─────────────────────────────────────────────────────── */}
        <section className="text-center space-y-6">
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full
              bg-indigo-500/10 border border-indigo-500/30 text-indigo-400 text-xs font-semibold"
          >
            <span className="w-2 h-2 rounded-full bg-indigo-400 animate-pulse" />
            BTP Project — Decentralised Healthcare Identity
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="text-5xl sm:text-6xl font-extrabold text-white leading-tight"
          >
            Zero-Trust Medical
            <br />
            <span className="bg-gradient-to-r from-indigo-400 via-violet-400 to-sky-400
              bg-clip-text text-transparent">
              Identity Platform
            </span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="text-white/50 text-lg max-w-2xl mx-auto leading-relaxed"
          >
            A patient-centric healthcare system built on W3C Verifiable Credentials,
            ECDSA cryptography, OPA-based ABAC, and an immutable SHA-256 blockchain
            audit trail. Built across 30 days from scratch.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="flex flex-wrap justify-center gap-3"
          >
            <TechBadge label="Spring Boot 3" color="green" />
            <TechBadge label="React + Vite" color="sky" />
            <TechBadge label="PostgreSQL" color="blue" />
            <TechBadge label="Open Policy Agent" color="violet" />
            <TechBadge label="BouncyCastle ECDSA" color="amber" />
            <TechBadge label="W3C Verifiable Credentials" color="indigo" />
            <TechBadge label="SHA-256 Blockchain" color="rose" />
          </motion.div>

          <motion.button
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.4 }}
            onClick={onEnter}
            className="inline-flex items-center gap-2 px-6 py-3 rounded-xl
              bg-gradient-to-r from-indigo-500 to-violet-500
              text-white font-semibold text-sm shadow-lg shadow-indigo-500/25
              hover:shadow-indigo-500/40 hover:scale-105 transition-all duration-200"
          >
            Enter Live Demo
            <ChevronRight className="w-4 h-4" />
          </motion.button>
        </section>

        {/* ── Stats ────────────────────────────────────────────────────── */}
        <section className="grid grid-cols-2 sm:grid-cols-4 gap-6 p-6 rounded-2xl
          bg-white/3 border border-white/8">
          <StatCounter value="76+"  label="Unit Tests Passing" color="text-emerald-400" />
          <StatCounter value="10"   label="REST Controllers"   color="text-sky-400" />
          <StatCounter value="9"    label="Frontend Pages"     color="text-violet-400" />
          <StatCounter value="256"  label="Bit Signature Keys"  color="text-amber-400" />
        </section>

        {/* ── Features ──────────────────────────────────────────────────── */}
        <section className="space-y-6">
          <motion.div
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="text-center"
          >
            <h2 className="text-3xl font-bold text-white mb-2">Three Pillars of Security</h2>
            <p className="text-white/40 text-sm">Every layer is independently testable and cryptographically verifiable</p>
          </motion.div>

          <div className="grid sm:grid-cols-3 gap-4">
            <FeatureCard
              icon={Shield}
              title="Zero-Trust Identity"
              description="Every API call is verified via W3C Verifiable Credentials or HMAC-SHA256 JWTs. No implicit trust — every identity is proven, not assumed."
              color="#6366f1"
              gradient="from-indigo-500/10 to-transparent"
              delay={0}
            />
            <FeatureCard
              icon={Eye}
              title="ABAC via OPA"
              description="Open Policy Agent evaluates Rego rules on every request. Access depends on role, emergency stage, and time — not just authentication."
              color="#8b5cf6"
              gradient="from-violet-500/10 to-transparent"
              delay={0.1}
            />
            <FeatureCard
              icon={Link}
              title="SHA-256 Audit Chain"
              description="Every data access is sealed into a cryptographic blockchain. Tamper any historical record and the chain validation immediately detects it."
              color="#f59e0b"
              gradient="from-amber-500/10 to-transparent"
              delay={0.2}
            />
          </div>

          <div className="grid sm:grid-cols-2 gap-4">
            <FeatureCard
              icon={AlertTriangle}
              title="Break-Glass Emergency"
              description="Parametics can request emergency access to sealed records via a dispatcher-controlled session with 5-stage state transitions."
              color="#ef4444"
              gradient="from-red-500/10 to-transparent"
              delay={0.3}
            />
            <FeatureCard
              icon={FileCheck}
              title="Patient Sovereignty"
              description="Patients own their Verifiable Credentials and grant/revoke consent atomically. Right-to-be-forgotten implemented via DELETE /api/emr/:did."
              color="#10b981"
              gradient="from-emerald-500/10 to-transparent"
              delay={0.4}
            />
          </div>
        </section>

        {/* ── Emergency Access Flow ──────────────────────────────────────── */}
        <section className="grid sm:grid-cols-2 gap-8 items-start">
          <div>
            <motion.h2
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              viewport={{ once: true }}
              className="text-2xl font-bold text-white mb-6"
            >
              How Emergency Access Works
            </motion.h2>

            <div>
              {[
                { number: 1, icon: Cpu,      title: 'Dispatcher Creates Session', description: 'DISP-0001 posts to /api/emergency/dispatch with patient ABHA ID and stage "dispatched".', color: 'sky' },
                { number: 2, icon: Key,      title: 'Staff Issues JWT',            description: 'Paramedic calls /api/auth/login-paramedic → gets a signed JWT with role="paramedic".', color: 'indigo' },
                { number: 3, icon: Shield,   title: 'OPA Evaluates Access',        description: 'acute_care.rego checks: role + session stage + action → returns allow/deny in <20ms.', color: 'violet' },
                { number: 4, icon: Database, title: 'EMR is Read or Written',      description: 'If OPA grants access, EMR is returned. If not, 403 is returned and an audit block is sealed.', color: 'amber' },
                { number: 5, icon: Lock,     title: 'Blockchain Audit Sealed',     description: 'Every outcome (grant or deny) is SHA-256 hashed and linked into the immutable chain.', color: 'rose' },
              ].map(step => (
                <ArchStep key={step.number} {...step} />
              ))}
            </div>
          </div>

          {/* Code snippet panel */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            className="rounded-2xl bg-black/40 border border-white/8 overflow-hidden"
          >
            <div className="px-4 py-3 border-b border-white/8 flex items-center gap-2">
              <div className="w-3 h-3 rounded-full bg-red-500/70" />
              <div className="w-3 h-3 rounded-full bg-amber-500/70" />
              <div className="w-3 h-3 rounded-full bg-emerald-500/70" />
              <span className="text-white/25 text-xs font-mono ml-2">acute_care.rego</span>
            </div>
            <pre className="p-5 text-xs font-mono leading-relaxed overflow-x-auto">
              <span className="text-violet-400">package</span>
              <span className="text-white"> veristas.emergency{'\n\n'}</span>
              <span className="text-sky-400">default</span>
              <span className="text-white"> allow = </span>
              <span className="text-amber-400">false{'\n\n'}</span>
              <span className="text-white/40"># Paramedic can READ in dispatched stage{'\n'}</span>
              <span className="text-emerald-400">allow</span>
              <span className="text-white"> {'{'}{'\n'}</span>
              <span className="text-white">  input.role == </span>
              <span className="text-amber-400">"paramedic"{'\n'}</span>
              <span className="text-white">  input.action == </span>
              <span className="text-amber-400">"read"{'\n'}</span>
              <span className="text-white">  input.session.stage == </span>
              <span className="text-amber-400">"dispatched"{'\n'}</span>
              <span className="text-white">{'}'}{'\n\n'}</span>
              <span className="text-white/40"># Surgeon can WRITE in arrived stage{'\n'}</span>
              <span className="text-emerald-400">allow</span>
              <span className="text-white"> {'{'}{'\n'}</span>
              <span className="text-white">  input.role == </span>
              <span className="text-amber-400">"surgeon"{'\n'}</span>
              <span className="text-white">  input.action == </span>
              <span className="text-amber-400">"update"{'\n'}</span>
              <span className="text-white">  input.session.stage == </span>
              <span className="text-amber-400">"arrived"{'\n'}</span>
              <span className="text-white">{'}'}</span>
            </pre>
          </motion.div>
        </section>

        {/* ── CTA ───────────────────────────────────────────────────────── */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center p-10 rounded-2xl bg-gradient-to-r from-indigo-500/10 to-violet-500/10
            border border-indigo-500/20"
        >
          <h2 className="text-2xl font-bold text-white mb-2">Explore the Live System</h2>
          <p className="text-white/40 text-sm mb-6">
            All 9 pages are wired to the live Railway backend. Click any tab to interact with the real API.
          </p>
          <button
            onClick={onEnter}
            className="inline-flex items-center gap-2 px-6 py-3 rounded-xl
              bg-gradient-to-r from-indigo-500 to-violet-500
              text-white font-semibold text-sm
              hover:scale-105 transition-all duration-200 shadow-lg shadow-indigo-500/25"
          >
            <Activity className="w-4 h-4" />
            Open Dashboard
          </button>
        </motion.section>

      </div>
    </div>
  );
}
