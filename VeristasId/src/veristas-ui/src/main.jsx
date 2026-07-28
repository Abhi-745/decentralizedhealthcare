import React, { useState } from 'react'
import ReactDOM from 'react-dom/client'
import { Provider } from 'react-redux'
import axios from 'axios'
import { store } from './store/store'
import './index.css'
import { ToastProvider } from './components/Toast'

// Set base URL: empty in dev (Vite proxy), Railway URL in production
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || '';

// Pages
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

const TABS = [
  { key: 'consent',      label: 'Day 8: Consent'        },
  { key: 'vc',           label: 'Day 9: VC Viewer'      },
  { key: 'emergency',    label: 'Day 10: Emergency'      },
  { key: 'audit',        label: 'Day 11: Audit Log'      },
  { key: 'staff',        label: 'Day 12: Staff Login'    },
  { key: 'registration', label: 'Day 13: Registration'   },
  { key: 'status',       label: 'Day 14: System Status'  },
  { key: 'staffauth',    label: 'Day 15: Staff Auth'     },
  { key: 'emr',          label: 'Day 16: EMR Viewer'     },
  { key: 'chain',        label: 'Day 18: Chain Viewer'    },
  { key: 'arch',         label: 'Day 19: Architecture'    },
];

function DevNavigator() {
  const [activeTab, setActiveTab] = useState('home');

  if (activeTab === 'home') {
    return <HomePage onEnter={() => setActiveTab('emergency')} />;
  }

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col">
      {/* Compact nav bar */}
      <div className="bg-slate-900 border-b border-white/10 p-3 flex flex-wrap justify-center gap-2">
        <button
          onClick={() => setActiveTab('home')}
          className="px-3 py-1.5 rounded-lg text-xs font-semibold transition-all bg-indigo-500/15 text-indigo-400 border border-indigo-500/30 hover:bg-indigo-500/25"
        >
          ← Home
        </button>
        {TABS.map(({ key, label }) => (
          <button
            key={key}
            onClick={() => setActiveTab(key)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all
              ${activeTab === key
                ? 'bg-indigo-500 text-white'
                : 'bg-white/5 text-white/50 hover:bg-white/10'}`}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Active page content */}
      <div className="flex-1 overflow-auto">
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
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <Provider store={store}>
      <ToastProvider>
        <DevNavigator />
      </ToastProvider>
    </Provider>
  </React.StrictMode>,
)
