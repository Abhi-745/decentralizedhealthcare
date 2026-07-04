import { motion } from 'framer-motion';

/**
 * GradientButton — premium animated call-to-action button.
 *
 * Props:
 *  - children   : button label text or JSX
 *  - onClick    : click handler
 *  - variant    : 'primary' | 'danger' | 'success' | 'ghost'
 *  - loading    : boolean — shows spinner, disables clicks
 *  - disabled   : boolean — greyed out, no clicks
 *  - fullWidth  : boolean — takes 100% width
 *  - size       : 'sm' | 'md' | 'lg'
 *  - icon       : JSX element rendered before the label
 *  - type       : 'button' | 'submit' (default 'button')
 *
 * Usage:
 *   <GradientButton variant="primary" loading={isLoading} onClick={handleSubmit}>
 *     Submit
 *   </GradientButton>
 */

const variantMap = {
  primary: 'from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 shadow-sky-500/30',
  danger:  'from-rose-500 to-red-600   hover:from-rose-400 hover:to-red-500   shadow-rose-500/30',
  success: 'from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 shadow-emerald-500/30',
  ghost:   'from-white/10 to-white/5 hover:from-white/20 hover:to-white/10 border border-white/20',
};

const sizeMap = {
  sm: 'px-4 py-2 text-sm',
  md: 'px-6 py-3 text-sm',
  lg: 'px-8 py-4 text-base',
};

function Spinner() {
  return (
    <svg
      className="animate-spin h-4 w-4"
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
  );
}

export default function GradientButton({
  children,
  onClick,
  variant = 'primary',
  loading = false,
  disabled = false,
  fullWidth = false,
  size = 'md',
  icon,
  type = 'button',
}) {
  const isDisabled = disabled || loading;
  const gradientClass = variantMap[variant] ?? variantMap.primary;
  const sizeClass = sizeMap[size] ?? sizeMap.md;

  return (
    <motion.button
      type={type}
      onClick={!isDisabled ? onClick : undefined}
      whileHover={!isDisabled ? { scale: 1.03 } : {}}
      whileTap={!isDisabled ? { scale: 0.97 } : {}}
      transition={{ type: 'spring', stiffness: 400, damping: 17 }}
      className={[
        // Base styles
        'inline-flex items-center justify-center gap-2',
        'font-semibold rounded-xl',
        'bg-gradient-to-r',
        'text-white',
        'shadow-lg',
        'transition-all duration-200',
        gradientClass,
        sizeClass,
        fullWidth ? 'w-full' : '',
        isDisabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer',
      ].join(' ')}
    >
      {loading ? <Spinner /> : icon}
      {children}
    </motion.button>
  );
}
