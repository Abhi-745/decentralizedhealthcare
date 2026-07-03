import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import { Shield, Activity, Users } from 'lucide-react';
import PatientPortal from './pages/PatientPortal';
import HospitalPortal from './pages/HospitalPortal';

function Navigation() {
  return (
    <nav className="fixed top-0 w-full z-50 bg-slate-900/80 backdrop-blur-md border-b border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center space-x-2">
            <Shield className="w-8 h-8 text-sky-400" />
            <span className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-sky-400 to-indigo-400">
              VeristasId
            </span>
          </div>
          <div className="flex space-x-4">
            <Link to="/" className="flex items-center space-x-1 px-3 py-2 rounded-md text-sm font-medium text-slate-300 hover:text-white hover:bg-slate-800 transition">
              <Users className="w-4 h-4" />
              <span>Patient</span>
            </Link>
            <Link to="/hospital" className="flex items-center space-x-1 px-3 py-2 rounded-md text-sm font-medium text-slate-300 hover:text-white hover:bg-slate-800 transition">
              <Activity className="w-4 h-4" />
              <span>Hospital</span>
            </Link>
          </div>
        </div>
      </div>
    </nav>
  );
}

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-slate-950 text-slate-100 relative">
        {/* Background Orbs */}
        <div className="absolute top-[-10%] left-[-10%] w-96 h-96 bg-sky-600/20 rounded-full blur-[120px] pointer-events-none" />
        <div className="absolute bottom-[-10%] right-[-10%] w-96 h-96 bg-indigo-600/20 rounded-full blur-[120px] pointer-events-none" />
        
        <Navigation />
        
        <main className="pt-24 pb-12 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto relative z-10">
          <Routes>
            <Route path="/" element={<PatientPortal />} />
            <Route path="/hospital" element={<HospitalPortal />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
