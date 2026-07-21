import React, { useState } from 'react'
import ReactDOM from 'react-dom/client'
import { Provider } from 'react-redux'
import axios from 'axios'
import { store } from './store/store'
import './index.css'
import { ToastProvider } from './components/Toast'

// Set the base URL for all axios requests.
// In local dev, this is empty (so Vite proxy handles /api).
// In production (Vercel), we will set VITE_API_BASE_URL to the Railway URL.
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || '';

// Import the pages we have built
import ConsentManager from './pages/ConsentManager'
import VCViewerPage from './pages/VCViewerPage'
import EmergencyDashboard from './pages/EmergencyDashboard'
import AuditLogViewer from './pages/AuditLogViewer'
import StaffLoginPage from './pages/StaffLoginPage'

function DevNavigator() {
  const [activeTab, setActiveTab] = useState('emergency');

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col">
      {/* Dev Navigation Bar */}
      <div className="bg-slate-900 border-b border-white/10 p-4 flex justify-center gap-4">
        <button 
          onClick={() => setActiveTab('consent')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${activeTab === 'consent' ? 'bg-indigo-500 text-white' : 'bg-white/5 text-white/50 hover:bg-white/10'}`}
        >
          Day 8: Consent Manager
        </button>
        <button 
          onClick={() => setActiveTab('vc')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${activeTab === 'vc' ? 'bg-indigo-500 text-white' : 'bg-white/5 text-white/50 hover:bg-white/10'}`}
        >
          Day 9: VC Viewer
        </button>
        <button 
          onClick={() => setActiveTab('emergency')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${activeTab === 'emergency' ? 'bg-indigo-500 text-white' : 'bg-white/5 text-white/50 hover:bg-white/10'}`}
        >
          Day 10: Emergency Dashboard
        </button>
        <button 
          onClick={() => setActiveTab('audit')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${activeTab === 'audit' ? 'bg-indigo-500 text-white' : 'bg-white/5 text-white/50 hover:bg-white/10'}`}
        >
          Day 11: Audit Log
        </button>
        <button 
          onClick={() => setActiveTab('staff')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${activeTab === 'staff' ? 'bg-indigo-500 text-white' : 'bg-white/5 text-white/50 hover:bg-white/10'}`}
        >
          Day 12: Staff Login
        </button>
      </div>

      {/* Render Active Page */}
      <div className="flex-1 overflow-auto">
        {activeTab === 'consent' && <ConsentManager />}
        {activeTab === 'vc' && <VCViewerPage />}
        {activeTab === 'emergency' && <EmergencyDashboard />}
        {activeTab === 'audit' && <AuditLogViewer />}
        {activeTab === 'staff' && <StaffLoginPage />}
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
