import React, { createContext, useContext, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-react';

/**
 * Toast Notification System
 *
 * Provides a global notification system for the entire app.
 * Instead of alert() boxes, we show beautiful animated toasts.
 *
 * How it works (React Context pattern):
 *   1. ToastProvider wraps the entire <App> — it holds the toast list in state
 *   2. useToast() hook — any component calls this to fire a toast
 *   3. ToastContainer renders the visible stack of toasts
 *
 * Example usage inside any component:
 *   const toast = useToast();
 *   toast.success('Record saved!');
 *   toast.error('OPA denied access');
 *   toast.warning('Session expires in 5 minutes');
 *   toast.info('Blockchain TX submitted');
 */

// ─── 1. CONTEXT ────────────────────────────────────────────────────────────
// React Context = a way to share state without passing props down every level

const ToastContext = createContext(null);

// ─── 2. TOAST CONFIG ───────────────────────────────────────────────────────

const toastConfig = {
  success: {
    icon: CheckCircle,
    bg:   'bg-emerald-500/20 border-emerald-500/40',
    icon_color: 'text-emerald-400',
    title_color: 'text-emerald-300',
  },
  error: {
    icon: XCircle,
    bg:   'bg-rose-500/20 border-rose-500/40',
    icon_color: 'text-rose-400',
    title_color: 'text-rose-300',
  },
  warning: {
    icon: AlertTriangle,
    bg:   'bg-amber-500/20 border-amber-500/40',
    icon_color: 'text-amber-400',
    title_color: 'text-amber-300',
  },
  info: {
    icon: Info,
    bg:   'bg-sky-500/20 border-sky-500/40',
    icon_color: 'text-sky-400',
    title_color: 'text-sky-300',
  },
};

// ─── 3. SINGLE TOAST COMPONENT ─────────────────────────────────────────────

function Toast({ id, type, title, message, onClose }) {
  const config = toastConfig[type] ?? toastConfig.info;
  const Icon = config.icon;

  return (
    <motion.div
      layout
      initial={{ opacity: 0, x: 80, scale: 0.9 }}
      animate={{ opacity: 1, x: 0, scale: 1 }}
      exit={{ opacity: 0, x: 80, scale: 0.9 }}
      transition={{ type: 'spring', stiffness: 300, damping: 25 }}
      className={[
        'flex items-start gap-3',
        'p-4 pr-3 rounded-2xl border',
        'backdrop-blur-xl shadow-2xl',
        'w-80 max-w-full',
        config.bg,
      ].join(' ')}
    >
      {/* Icon */}
      <Icon className={`w-5 h-5 mt-0.5 flex-shrink-0 ${config.icon_color}`} />

      {/* Text */}
      <div className="flex-1 min-w-0">
        <p className={`text-sm font-semibold ${config.title_color}`}>{title}</p>
        {message && (
          <p className="text-xs text-slate-400 mt-0.5 leading-relaxed">{message}</p>
        )}
      </div>

      {/* Close button */}
      <button
        onClick={() => onClose(id)}
        className="flex-shrink-0 p-1 rounded-lg text-slate-400 hover:text-white hover:bg-white/10 transition-colors"
      >
        <X className="w-3.5 h-3.5" />
      </button>
    </motion.div>
  );
}

// ─── 4. TOAST CONTAINER (renders the stack) ────────────────────────────────

function ToastContainer({ toasts, removeToast }) {
  return (
    <div className="fixed top-20 right-4 z-[9999] flex flex-col gap-3 pointer-events-none">
      <AnimatePresence mode="popLayout">
        {toasts.map((toast) => (
          <div key={toast.id} className="pointer-events-auto">
            <Toast {...toast} onClose={removeToast} />
          </div>
        ))}
      </AnimatePresence>
    </div>
  );
}

// ─── 5. PROVIDER ───────────────────────────────────────────────────────────
// Wraps <App> in main.jsx — makes toasts available to every component

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  // Remove a specific toast by ID
  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  // Add a new toast — auto-removes after `duration` ms
  const addToast = useCallback(({ type, title, message, duration = 4000 }) => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, type, title, message }]);

    // Auto-dismiss
    setTimeout(() => removeToast(id), duration);
  }, [removeToast]);

  // Convenience methods — the public API for components
  const toast = {
    success: (title, message) => addToast({ type: 'success', title, message }),
    error:   (title, message) => addToast({ type: 'error',   title, message }),
    warning: (title, message) => addToast({ type: 'warning', title, message }),
    info:    (title, message) => addToast({ type: 'info',    title, message }),
  };

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <ToastContainer toasts={toasts} removeToast={removeToast} />
    </ToastContext.Provider>
  );
}

// ─── 6. HOOK ───────────────────────────────────────────────────────────────
// Called inside any component: const toast = useToast();

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used inside a <ToastProvider>');
  }
  return context;
}
