import React, { useState } from 'react'
import ReactDOM from 'react-dom/client'
import { Provider } from 'react-redux'
import axios from 'axios'
import { store } from './store/store'
import './index.css'
import { ToastProvider } from './components/Toast'

axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || '';

import HomePage             from './pages/HomePage'
import ConsentManager       from './pages/ConsentManager'
import VCViewerPage         from './pages/VCViewerPage'
import EmergencyDashboard   from './pages/EmergencyDashboard'
import AuditLogViewer       from './pages/AuditLogViewer'
import StaffLoginPage       from './pages/StaffLoginPage'
import PatientRegistrationPage from './pages/PatientRegistrationPage'
import SystemStatusPage     from './pages/SystemStatusPage'
import StaffAuthPage        from './pages/StaffAuthPage'
import EMRViewerPage        from './pages/EMRViewerPage'
import ChainVisualizerPage  from './pages/ChainVisualizerPage'
import ArchitecturePage     from './pages/ArchitecturePage'
import TimelinePage         from './pages/TimelinePage'

const TABS = [
  { key: 'consent',      label: 'Consent'       },
  { key: 'vc',           label: 'VC Viewer'     },
  { key: 'emergency',    label: 'Emergency'     },
  { key: 'audit',        label: 'Audit Log'     },
  { key: 'staff',        label: 'Staff Login'   },
  { key: 'registration', label: 'Registration'  },
  { key: 'status',       label: 'Status'        },
  { key: 'staffauth',    label: 'Staff Auth'    },
  { key: 'emr',          label: 'EMR'           },
  { key: 'chain',        label: 'Chain'         },
  { key: 'arch',         label: 'Architecture'  },
  { key: 'timeline',     label: 'Timeline'      },
];

function AppShell() {
  const [activeTab, setActiveTab] = useState('home');

  if (activeTab === 'home') {
    return <HomePage onEnter={() => setActiveTab('emergency')} />;
  }

  return (
    <div style={{ minHeight: '100vh', background: '#080808', display: 'flex', flexDirection: 'column' }}>

      {/* Nav */}
      <nav style={{
        borderBottom: '1px solid #1c1c1c',
        background: '#080808',
        padding: '0 24px',
        display: 'flex',
        alignItems: 'center',
        gap: '4px',
        overflowX: 'auto',
        flexShrink: 0,
        height: '48px',
      }}>
        {/* Logo / home link */}
        <button
          onClick={() => setActiveTab('home')}
          style={{
            color: '#ffffff',
            fontSize: '13px',
            fontWeight: '600',
            letterSpacing: '-0.02em',
            padding: '6px 12px 6px 0',
            marginRight: '8px',
            borderRight: '1px solid #1c1c1c',
            background: 'none',
            border: 'none',
            borderRight: '1px solid #1c1c1c',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
            flexShrink: 0,
          }}
        >
          VeristasId
        </button>

        {TABS.map(({ key, label }) => (
          <button
            key={key}
            onClick={() => setActiveTab(key)}
            style={{
              padding: '6px 10px',
              fontSize: '12px',
              fontWeight: activeTab === key ? '500' : '400',
              color: activeTab === key ? '#ffffff' : '#555555',
              background: 'none',
              border: 'none',
              borderBottom: activeTab === key ? '1px solid #3b82f6' : '1px solid transparent',
              cursor: 'pointer',
              whiteSpace: 'nowrap',
              flexShrink: 0,
              transition: 'color 0.15s',
            }}
            onMouseEnter={e => { if (activeTab !== key) e.target.style.color = '#888888' }}
            onMouseLeave={e => { if (activeTab !== key) e.target.style.color = '#555555' }}
          >
            {label}
          </button>
        ))}
      </nav>

      {/* Page content */}
      <div style={{ flex: 1, overflow: 'auto' }}>
        {activeTab === 'consent'      && <ConsentManager />}
        {activeTab === 'vc'           && <VCViewerPage />}
        {activeTab === 'emergency'    && <EmergencyDashboard />}
        {activeTab === 'audit'        && <AuditLogViewer />}
        {activeTab === 'staff'        && <StaffLoginPage />}
        {activeTab === 'registration' && <PatientRegistrationPage />}
        {activeTab === 'status'       && <SystemStatusPage />}
        {activeTab === 'staffauth'    && <StaffAuthPage />}
        {activeTab === 'emr'          && <EMRViewerPage />}
        {activeTab === 'chain'        && <ChainVisualizerPage />}
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
