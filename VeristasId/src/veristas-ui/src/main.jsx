import React, { useState } from 'react'
import ReactDOM from 'react-dom/client'
import { Provider } from 'react-redux'
import axios from 'axios'
import { store } from './store/store'
import './index.css'
import { ToastProvider } from './components/Toast'

axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || '';

import HomePage               from './pages/HomePage'
import StaffLoginPage         from './pages/StaffLoginPage'
import PatientRegistrationPage from './pages/PatientRegistrationPage'
import VCViewerPage           from './pages/VCViewerPage'
import EmergencyDashboard     from './pages/EmergencyDashboard'
import ConsentManager         from './pages/ConsentManager'
import EMRViewerPage          from './pages/EMRViewerPage'
import StaffAuthPage          from './pages/StaffAuthPage'
import ResourcesPage          from './pages/ResourcesPage'
import AuditLogViewer         from './pages/AuditLogViewer'
import ChainVisualizerPage    from './pages/ChainVisualizerPage'
import SystemStatusPage       from './pages/SystemStatusPage'
import ArchitecturePage       from './pages/ArchitecturePage'
import TimelinePage           from './pages/TimelinePage'

// ─── Nav groups ──────────────────────────────────────────────────────────────
// Organised by REST controller / API concern
const NAV = [
  {
    group: 'Auth & Identity',
    items: [
      { key: 'staff',        label: '/api/auth',       methods: ['POST'] },
      { key: 'registration', label: '/api/patients',   methods: ['POST', 'GET'] },
      { key: 'vc',           label: '/api/vc',         methods: ['POST', 'GET', 'DELETE'] },
    ],
  },
  {
    group: 'Core Operations',
    items: [
      { key: 'emergency', label: '/api/emergency', methods: ['POST', 'PUT'] },
      { key: 'consent',   label: '/api/consent',   methods: ['POST', 'GET', 'DELETE'] },
      { key: 'emr',       label: '/api/emr',        methods: ['GET', 'PUT'] },
      { key: 'resources', label: '/api/resources',  methods: ['GET'] },
    ],
  },
  {
    group: 'Audit & System',
    items: [
      { key: 'audit',  label: '/api/audit',        methods: ['GET', 'DELETE'] },
      { key: 'chain',  label: '/api/audit/chain',  methods: ['GET'] },
      { key: 'status', label: '/api/status',       methods: ['GET'] },
    ],
  },
  {
    group: 'Docs',
    items: [
      { key: 'arch',     label: 'Architecture', methods: [] },
      { key: 'timeline', label: 'Timeline',     methods: [] },
    ],
  },
];

const ALL_TABS = NAV.flatMap(g => g.items);

const METHOD_COLOR = {
  GET:    '#22c55e',
  POST:   '#3b82f6',
  PUT:    '#f59e0b',
  DELETE: '#ef4444',
  PATCH:  '#8b5cf6',
};

function NavBar({ activeTab, setActiveTab }) {
  return (
    <nav style={{
      borderBottom: '1px solid #eaeaea',
      background: '#ffffff',
      overflowX: 'auto',
      flexShrink: 0,
      scrollbarWidth: 'none',
    }}>
      {/* Top strip: logo + group labels */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        height: '40px',
        padding: '0 20px',
        borderBottom: '1px solid #f0f0f0',
        gap: '0',
      }}>
        {/* Logo */}
        <button
          onClick={() => setActiveTab('home')}
          style={{
            fontSize: '12px', fontWeight: '700', letterSpacing: '-0.02em',
            color: '#111', background: 'none', border: 'none', cursor: 'pointer',
            padding: '0 16px 0 0', marginRight: '16px',
            borderRight: '1px solid #eaeaea', flexShrink: 0, height: '100%',
          }}
        >
          VeristasId
        </button>

        {/* Group labels */}
        {NAV.map((g, i) => (
          <span key={g.group} style={{
            fontSize: '10px', fontWeight: '500', letterSpacing: '0.06em',
            color: '#bbb', textTransform: 'uppercase', padding: '0 20px 0 0',
            marginRight: i < NAV.length - 1 ? '0' : '0',
            flexShrink: 0,
          }}>
            {g.group}
          </span>
        ))}
      </div>

      {/* Bottom strip: endpoint tabs */}
      <div style={{ display: 'flex', alignItems: 'center', height: '38px', padding: '0 20px', gap: '0' }}>
        {NAV.map((g, gi) => (
          <React.Fragment key={g.group}>
            <div style={{ display: 'flex', gap: '0' }}>
              {g.items.map(({ key, label, methods }) => {
                const isActive = activeTab === key;
                return (
                  <button
                    key={key}
                    onClick={() => setActiveTab(key)}
                    style={{
                      display: 'flex', alignItems: 'center', gap: '5px',
                      padding: '0 10px', height: '38px',
                      fontSize: '11px', fontFamily: 'JetBrains Mono, monospace',
                      fontWeight: isActive ? '500' : '400',
                      color: isActive ? '#111' : '#888',
                      background: 'none', border: 'none',
                      borderBottom: isActive ? '2px solid #3b82f6' : '2px solid transparent',
                      cursor: 'pointer', whiteSpace: 'nowrap', flexShrink: 0,
                      transition: 'color 0.1s',
                    }}
                    onMouseEnter={e => { if (!isActive) e.currentTarget.style.color = '#333' }}
                    onMouseLeave={e => { if (!isActive) e.currentTarget.style.color = '#888' }}
                  >
                    {/* Method pills */}
                    {methods.length > 0 && (
                      <span style={{
                        fontSize: '8px', fontWeight: '600', letterSpacing: '0.04em',
                        color: METHOD_COLOR[methods[0]],
                        opacity: isActive ? 1 : 0.6,
                      }}>
                        {methods[0]}
                      </span>
                    )}
                    {label}
                  </button>
                );
              })}
            </div>
            {/* Divider between groups */}
            {gi < NAV.length - 1 && (
              <div style={{ width: '1px', height: '16px', background: '#eaeaea', margin: '0 6px', flexShrink: 0 }} />
            )}
          </React.Fragment>
        ))}
      </div>
    </nav>
  );
}

function AppShell() {
  const [activeTab, setActiveTab] = useState('home');

  if (activeTab === 'home') {
    return <HomePage onEnter={() => setActiveTab('staff')} />;
  }

  return (
    <div style={{ minHeight: '100vh', background: '#fafafa', display: 'flex', flexDirection: 'column' }}>
      <NavBar activeTab={activeTab} setActiveTab={setActiveTab} />

      <div style={{ flex: 1, overflow: 'auto' }}>
        {activeTab === 'staff'        && <StaffLoginPage />}
        {activeTab === 'registration' && <PatientRegistrationPage />}
        {activeTab === 'vc'           && <VCViewerPage />}
        {activeTab === 'emergency'    && <EmergencyDashboard />}
        {activeTab === 'consent'      && <ConsentManager />}
        {activeTab === 'emr'          && <EMRViewerPage />}
        {activeTab === 'resources'    && <ResourcesPage />}
        {activeTab === 'audit'        && <AuditLogViewer />}
        {activeTab === 'chain'        && <ChainVisualizerPage />}
        {activeTab === 'status'       && <SystemStatusPage />}
        {activeTab === 'arch'         && <ArchitecturePage />}
        {activeTab === 'timeline'     && <TimelinePage />}
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <Provider store={store}>
      <ToastProvider>
        <AppShell />
      </ToastProvider>
    </Provider>
  </React.StrictMode>,
)
