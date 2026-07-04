import { motion } from 'framer-motion';

/**
 * GlassCard — the reusable glassmorphism container used throughout the app.
 *
 * Props:
 *  - children   : content inside the card
 *  - className  : extra Tailwind classes (overrides/additions)
 *  - glow       : 'sky' | 'indigo' | 'emerald' | 'rose' | 'amber' | 'none'
 *  - hover      : boolean — enables lift + glow on hover (default true)
 *  - onClick    : optional click handler
 *
 * Usage:
 *   <GlassCard glow="sky">
 *     <p>Any content here</p>
 *   </GlassCard>
 */

const glowMap = {
  sky:     'hover:shadow-sky-500/25 hover:border-sky-500/50',
  indigo:  'hover:shadow-indigo-500/25 hover:border-indigo-500/50',
  emerald: 'hover:shadow-emerald-500/25 hover:border-emerald-500/50',
  rose:    'hover:shadow-rose-500/25 hover:border-rose-500/50',
  amber:   'hover:shadow-amber-500/25 hover:border-amber-500/50',
  none:    '',
};

export default function GlassCard({
  children,
  className = '',
  glow = 'sky',
  hover = true,
  onClick,
}) {
  const glowClass = glowMap[glow] ?? glowMap.sky;

  return (
    <motion.div
      onClick={onClick}
      whileHover={hover ? { y: -4, scale: 1.01 } : {}}
      transition={{ type: 'spring', stiffness: 300, damping: 20 }}
      className={[
        // Glassmorphism base
        'bg-white/5 backdrop-blur-md',
        'border border-white/10',
        'rounded-2xl',
        // Shadow + glow on hover
        'shadow-xl',
        hover ? `transition-all duration-300 ${glowClass} hover:shadow-2xl` : '',
        // Cursor
        onClick ? 'cursor-pointer' : '',
        className,
      ].join(' ')}
    >
      {children}
    </motion.div>
  );
}
