/**
 * StatusBadge — shows a coloured pill for status values.
 *
 * Used for:
 *  - OPA decisions:      "GRANTED" (green), "DENIED" (red)
 *  - Emergency stages:   "dispatched" (amber), "arrived" (sky), "completed" (grey)
 *  - VC status:          "VALID" (green), "REVOKED" (red)
 *  - Chain status:       "INTACT" (green), "TAMPERED" (red)
 *
 * Props:
 *  - status  : string value (see statusConfig below)
 *  - pulse   : boolean — adds animated glow dot for "active" states
 *  - size    : 'sm' | 'md'
 *
 * Usage:
 *   <StatusBadge status="GRANTED" />
 *   <StatusBadge status="dispatched" pulse />
 */

const statusConfig = {
  // OPA decisions
  GRANTED:     { color: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30', dot: 'bg-emerald-400' },
  DENIED:      { color: 'bg-rose-500/20    text-rose-400    border-rose-500/30',    dot: 'bg-rose-400' },

  // Emergency stages
  dispatched:  { color: 'bg-amber-500/20   text-amber-400   border-amber-500/30',   dot: 'bg-amber-400' },
  arrived:     { color: 'bg-sky-500/20     text-sky-400     border-sky-500/30',      dot: 'bg-sky-400' },
  completed:   { color: 'bg-slate-500/20   text-slate-400   border-slate-500/30',   dot: 'bg-slate-400' },

  // VC status
  VALID:       { color: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30', dot: 'bg-emerald-400' },
  REVOKED:     { color: 'bg-rose-500/20    text-rose-400    border-rose-500/30',    dot: 'bg-rose-400' },

  // Chain integrity
  INTACT:      { color: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30', dot: 'bg-emerald-400' },
  TAMPERED:    { color: 'bg-rose-500/20    text-rose-400    border-rose-500/30',    dot: 'bg-rose-400' },

  // Blockchain
  MINED:       { color: 'bg-indigo-500/20  text-indigo-400  border-indigo-500/30',  dot: 'bg-indigo-400' },
  PENDING:     { color: 'bg-amber-500/20   text-amber-400   border-amber-500/30',   dot: 'bg-amber-400' },

  // Fallback
  DEFAULT:     { color: 'bg-slate-500/20   text-slate-300   border-slate-500/30',   dot: 'bg-slate-400' },
};

const sizeMap = {
  sm: 'text-xs px-2 py-0.5 gap-1',
  md: 'text-xs px-3 py-1   gap-1.5',
};

export default function StatusBadge({ status = '', pulse = false, size = 'md' }) {
  const config = statusConfig[status] ?? statusConfig.DEFAULT;
  const sizeClass = sizeMap[size] ?? sizeMap.md;

  return (
    <span
      className={[
        'inline-flex items-center font-semibold rounded-full border',
        config.color,
        sizeClass,
      ].join(' ')}
    >
      {/* Animated pulsing dot for active states */}
      {pulse && (
        <span className="relative flex h-2 w-2">
          <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${config.dot}`} />
          <span className={`relative inline-flex rounded-full h-2 w-2 ${config.dot}`} />
        </span>
      )}
      {status}
    </span>
  );
}
