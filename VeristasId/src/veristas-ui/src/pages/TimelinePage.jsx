import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Database, Shield, Server, Link2, Cpu, Code2,
  TestTube, Globe, Layers, Zap, GitBranch, BookOpen,
  ChevronDown, CheckCircle2, Calendar
} from 'lucide-react';

const DAYS = [
  {
    range: '1',
    title: 'Foundation',
    subtitle: 'Spring Boot scaffold + context test',
    icon: Server,
    color: '#6366f1',
    tests: 1,
    items: [
      'Spring Boot 3.x project initialisation via Spring Initializr',
      'application.properties configured (H2 for dev, PostgreSQL for prod)',
      'VeristasIdApplicationTest — smoke test proving Spring context loads',
      '@SpringBootApplication entry point with graceful startup logging',
    ],
    concept: '@SpringBootApplication bootstraps the entire IoC container, component scan, and auto-configuration in a single annotation.',
  },
  {
    range: '2–4',
    title: 'Data Layer',
    subtitle: '5 JPA entities · H2 persistence tests · repositories',
    icon: Database,
    color: '#10b981',
    tests: 21,
    items: [
      'PatientEntity, MedicalRecord, EmergencySessionEntity, Consent, VerifiableCredentialEntity',
      'EmergencySessionRepository — findFirstByPatientIdOrderByCreatedAtDesc fix (Day 17 regression)',
      '@DataJpaTest with H2 — tests table creation, CRUD, unique constraints',
      'EntityModelTest — constructor + getter contracts, 12 pure-Java tests',
    ],
    concept: '@DataJpaTest slices the Spring context to ONLY the persistence layer (repositories + Hibernate + H2). It is 10× faster than @SpringBootTest for database-only tests.',
  },
  {
    range: '5–9',
    title: 'Identity & Security Services',
    subtitle: 'JWT · ECDSA · OPA · VC lifecycle',
    icon: Shield,
    color: '#8b5cf6',
    tests: 67,
    items: [
      'JwtService — HMAC-SHA256 sign/verify, 16 tests covering edge cases',
      'PatientWalletService — BouncyCastle P-256 ECDSA key generation, 14 tests',
      'CredentialIssuanceService — W3C VC creation with DID, 16 tests',
      'OpaService — delegates to OPA server, degrades gracefully when OPA is down',
      'CredentialService — ECDSA verify + revocation, OpaSecurityFilter',
    ],
    concept: 'Constructor injection (not @Autowired field injection) is Spring\'s recommended pattern. It makes dependencies explicit and enables clean unit testing without a Spring context.',
  },
  {
    range: '10–15',
    title: 'REST Controllers',
    subtitle: '10 controllers · full zero-trust pipeline',
    icon: Cpu,
    color: '#0ea5e9',
    tests: 70,
    items: [
      'EmergencySessionController — dispatcher-only create, 5-stage state machine',
      'EMRController — double-auth (VC AND JWT) + OR security gate',
      'ResourceController — full zero-trust: identity → OPA → EMR → audit',
      'AccessController — MockRestServiceServer pattern (internal RestTemplate)',
      'AuthController — JWT issuance for paramedic/surgeon roles',
      'ConsentController, MedicalRecordController, PatientRegistrationController',
    ],
    concept: '@WebMvcTest(Controller.class) loads ONLY the web layer. Services and repositories are @MockBean. This isolates the HTTP routing, request validation, and response serialisation from business logic.',
  },
  {
    range: '16–17',
    title: 'Blockchain Integrity',
    subtitle: 'SHA-256 chain · immutability · avalanche effect',
    icon: Link2,
    color: '#f59e0b',
    tests: 7,
    items: [
      'AuditLogBlock — immutable DTO, no public setHash() method',
      'hash_isReproducible_givenSameInputs — SHA-256 determinism test',
      'hash_avalancheEffect — single-bit input change flips ~50% output bits',
      'chain_linking — block[N].previousHash === block[N-1].hash',
      'hash_hasNoPublicSetterMethod — reflection scan for absent setter (design contract test)',
    ],
    concept: 'Design-contract testing: if a developer accidentally adds Lombok @Data (which auto-generates setHash()), this test fails immediately — catching the regression before it reaches production.',
  },
  {
    range: '18',
    title: 'Three-Layer Audit Service',
    subtitle: 'BlockchainAuditService · tamper detection · @PostConstruct',
    icon: Layers,
    color: '#ef4444',
    tests: 8,
    items: [
      'Layer 1 in-memory chain · Layer 2 PostgreSQL · Layer 3 Ethereum (VeristasAudit.sol)',
      '@PostConstruct cannot fire in unit tests — resetChain() used in @BeforeEach instead',
      'ReflectionTestUtils.getField() reaches the private blockchain list to simulate tampering',
      'isChainValid_tamperedBlock_detectsCorruptionAndReturnsFalse — tamper simulation',
      'Ethereum layer disabled in tests via contractService.isActive() = false (mocked)',
    ],
    concept: 'When @SpringBootTest is NOT used, @PostConstruct never fires. You must manually call the initialisation method in @BeforeEach to seed the required state.',
  },
  {
    range: '19',
    title: 'Integration Tests',
    subtitle: '@SpringBootTest · real H2 database · OpaSecurityFilter bypass',
    icon: TestTube,
    color: '#06b6d4',
    tests: 6,
    items: [
      '@SpringBootTest loads the COMPLETE Spring context (not just web or persistence slices)',
      '@MockBean replaces OpaService, ContractLifecycleService, JwtService inside Spring context',
      'OpaSecurityFilter intercepts ALL requests — lenient().when(jwtService...).thenReturn(true) to bypass',
      'sessionRepo.findById() queries REAL H2 database after each HTTP call',
      '@AfterEach deleteAll() for isolation (NOT @Transactional — MockMvc runs in separate transaction)',
    ],
    concept: '@MockBean differs from @Mock: @Mock creates a mock outside Spring, @MockBean replaces the real Spring bean. Only @MockBean affects the running application context.',
  },
  {
    range: '20',
    title: 'Final Polish',
    subtitle: 'README · Timeline · Vercel + Railway live',
    icon: Globe,
    color: '#a855f7',
    tests: 0,
    items: [
      'Professional README with architecture diagram, API table, testing summary',
      'This Timeline page — 20-day build story for portfolio demos',
      '194 tests passing with zero failures across 5 testing layers',
      'Vercel frontend + Railway backend fully deployed and live',
      'Architecture Page — interactive 5-layer system explorer',
    ],
    concept: 'A project is only as good as its documentation. A recruiter who cannot understand what you built in 60 seconds will move on. The README is your cover letter.',
  },
];

function DayCard({ day, isOpen, onToggle }) {
  const Icon = day.icon;
  return (
    <motion.div
      initial={{ opacity: 0, x: -16 }}
      whileInView={{ opacity: 1, x: 0 }}
      viewport={{ once: true }}
      className="relative"
    >
      {/* Timeline spine */}
      <div className="absolute left-5 top-14 bottom-0 w-px bg-white/6" />

      {/* Card */}
      <div
        className="rounded-2xl border overflow-hidden cursor-pointer hover:border-white/20 transition-all"
        style={{
          background: isOpen ? `${day.color}08` : 'rgba(255,255,255,0.03)',
          borderColor: isOpen ? `${day.color}30` : 'rgba(255,255,255,0.08)',
        }}
        onClick={onToggle}
      >
        {/* Header */}
        <div className="flex items-center gap-4 p-4">
          {/* Day badge */}
          <div
            className="w-10 h-10 rounded-xl flex items-center justify-center shrink-0 text-xs font-bold"
            style={{ background: `${day.color}20`, border: `1px solid ${day.color}40`, color: day.color }}
          >
            D{day.range}
          </div>

          {/* Icon */}
          <div className="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
            style={{ background: `${day.color}15` }}>
            <Icon className="w-4 h-4" style={{ color: day.color }} />
          </div>

          {/* Title */}
          <div className="flex-1 min-w-0">
            <p className="text-white font-bold text-sm">{day.title}</p>
            <p className="text-white/35 text-xs truncate">{day.subtitle}</p>
          </div>

          {/* Tests badge */}
          {day.tests > 0 && (
            <div className="flex items-center gap-1 px-2 py-0.5 rounded-full
              bg-emerald-500/10 border border-emerald-500/25 shrink-0">
              <CheckCircle2 className="w-3 h-3 text-emerald-400" />
              <span className="text-emerald-400 text-xs font-semibold">{day.tests}</span>
            </div>
          )}

          <ChevronDown className={`w-4 h-4 text-white/25 transition-transform shrink-0
            ${isOpen ? 'rotate-180' : ''}`} />
        </div>

        {/* Expanded body */}
        <AnimatePresence>
          {isOpen && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="border-t px-4 py-4 space-y-4"
              style={{ borderColor: `${day.color}20` }}
            >
              {/* What was built */}
              <div className="space-y-2">
                {day.items.map((item, i) => (
                  <div key={i} className="flex items-start gap-2.5">
                    <div className="w-1 h-1 rounded-full mt-2 shrink-0"
                      style={{ background: day.color }} />
                    <p className="text-white/60 text-xs leading-relaxed">{item}</p>
                  </div>
                ))}
              </div>

              {/* Key concept */}
              <div className="p-3 rounded-xl bg-black/20 border border-white/6">
                <p className="text-white/30 text-[10px] uppercase tracking-wide font-semibold mb-1">
                  Key Concept
                </p>
                <p className="text-white/55 text-xs leading-relaxed">{day.concept}</p>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  );
}

export default function TimelinePage() {
  const [openDay, setOpenDay] = useState('20');
  const totalTests = DAYS.reduce((sum, d) => sum + d.tests, 0);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-6">
      <div className="max-w-2xl mx-auto space-y-6">

        {/* Header */}
        <div className="flex items-start gap-3">
          <div className="w-10 h-10 rounded-xl bg-violet-500/10 border border-violet-500/20
            flex items-center justify-center mt-0.5">
            <Calendar className="w-5 h-5 text-violet-400" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">20-Day Build Timeline</h1>
            <p className="text-white/40 text-sm">VeristasId · built from scratch · every commit documented</p>
          </div>
        </div>

        {/* Summary bar */}
        <div className="grid grid-cols-4 gap-3">
          {[
            { v: '20',           l: 'Days',          c: 'text-violet-400' },
            { v: `${totalTests}`, l: 'Tests Written', c: 'text-emerald-400' },
            { v: '11',           l: 'Pages Built',   c: 'text-sky-400' },
            { v: '0',            l: 'Failures',      c: 'text-amber-400' },
          ].map(({ v, l, c }) => (
            <div key={l} className="text-center p-3 rounded-xl bg-white/3 border border-white/8">
              <p className={`text-2xl font-bold ${c}`}>{v}</p>
              <p className="text-white/30 text-xs">{l}</p>
            </div>
          ))}
        </div>

        {/* Testing pyramid */}
        <div className="p-4 rounded-2xl bg-white/3 border border-white/8 space-y-3">
          <div className="flex items-center gap-2">
            <TestTube className="w-4 h-4 text-emerald-400" />
            <p className="text-white font-semibold text-sm">Testing Pyramid</p>
          </div>
          <div className="space-y-1.5">
            {[
              { label: 'Integration (@SpringBootTest)',          n: 6,  pct: 3,  c: 'bg-cyan-500' },
              { label: 'DTO / Chain (Pure Java + Reflection)',   n: 7,  pct: 4,  c: 'bg-amber-500' },
              { label: 'Service (@ExtendWith + MockitoExtension)',n: 67, pct: 35, c: 'bg-violet-500' },
              { label: 'Controller (@WebMvcTest + @MockBean)',   n: 70, pct: 36, c: 'bg-sky-500' },
              { label: 'Repository/Entity (@DataJpaTest)',       n: 44, pct: 22, c: 'bg-emerald-500' },
            ].map(({ label, n, pct, c }) => (
              <div key={label} className="flex items-center gap-3">
                <div className="w-32 shrink-0">
                  <div className="h-2 rounded-full bg-white/5 overflow-hidden">
                    <motion.div
                      initial={{ width: 0 }}
                      whileInView={{ width: `${pct}%` }}
                      viewport={{ once: true }}
                      transition={{ duration: 0.8, ease: 'easeOut' }}
                      className={`h-full rounded-full ${c}`}
                    />
                  </div>
                </div>
                <p className="text-white/50 text-xs flex-1 truncate">{label}</p>
                <p className="text-white/40 text-xs font-mono shrink-0">{n}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Day cards */}
        <div className="space-y-2">
          {DAYS.map((day) => (
            <DayCard
              key={day.range}
              day={day}
              isOpen={openDay === day.range}
              onToggle={() => setOpenDay(openDay === day.range ? null : day.range)}
            />
          ))}
        </div>

        {/* Footer */}
        <div className="text-center py-4 space-y-1">
          <p className="text-white/20 text-xs">Built by Abhi · Zero-Trust Decentralised Healthcare</p>
          <div className="flex justify-center gap-4">
            <a
              href="https://github.com/Abhi-745/decentralizedhealthcare"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1 text-indigo-400 text-xs hover:text-indigo-300 transition-colors"
            >
              <GitBranch className="w-3 h-3" />
              GitHub
            </a>
            <a
              href="/api/audit/ledger"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1 text-amber-400 text-xs hover:text-amber-300 transition-colors"
            >
              <Link2 className="w-3 h-3" />
              Live Audit Chain
            </a>
          </div>
        </div>

      </div>
    </div>
  );
}
