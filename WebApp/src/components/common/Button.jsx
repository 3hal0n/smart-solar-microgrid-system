// ============================================================
// File: Button.jsx
// Purpose: Shared pill-shaped button with the Wise-style variants
//          (primary/accent/secondary/ghost) reused across every
//          page instead of one-off button markup.
// Author: Shalon
// ============================================================
const VARIANT_CLASSES = {
  primary: 'bg-primary text-on-primary hover:bg-primary-hover active:bg-primary-pressed',
  accent: 'bg-accent text-on-accent hover:bg-accent-hover active:bg-accent-pressed',
  secondary: 'bg-surface-tint text-primary hover:bg-accent/30',
  ghost: 'bg-transparent text-primary hover:bg-surface-tint',
};

// Renders a pill-shaped button in one of the shared visual variants.
export default function Button({ variant = 'primary', className = '', children, ...props }) {
  const variantClasses = VARIANT_CLASSES[variant] ?? VARIANT_CLASSES.primary;
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-pill px-6 py-2.5 font-sans text-sm font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${variantClasses} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}
