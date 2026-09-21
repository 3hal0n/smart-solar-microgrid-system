// ============================================================
// File: Button.jsx
// Purpose: Shared button with the Stripe-style action variants
//          (primary violet / dark ink / secondary outline / ghost /
//          danger). Compact geometric radius, not a consumer pill.
// Author: Shalon
// ============================================================
const VARIANT_CLASSES = {
  primary: 'bg-primary text-on-primary hover:bg-primary-hover active:bg-primary-pressed shadow-card',
  dark: 'bg-ink text-on-dark hover:bg-ink-soft active:bg-ink shadow-card',
  secondary: 'bg-surface text-ink border border-line hover:bg-surface-alt active:bg-surface-alt shadow-card',
  ghost: 'bg-transparent text-body hover:bg-surface-alt hover:text-ink',
  danger: 'bg-error text-on-primary hover:opacity-90 shadow-card',
};

const SIZE_CLASSES = {
  sm: 'h-8 px-3 text-[13px]',
  md: 'h-9 px-3.5 text-sm',
};

// Renders a button in one of the shared visual variants and sizes.
export default function Button({ variant = 'primary', size = 'md', className = '', children, ...props }) {
  const variantClasses = VARIANT_CLASSES[variant] ?? VARIANT_CLASSES.primary;
  const sizeClasses = SIZE_CLASSES[size] ?? SIZE_CLASSES.md;
  return (
    <button
      className={`inline-flex items-center justify-center gap-1.5 whitespace-nowrap rounded-md font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/30 disabled:cursor-not-allowed disabled:opacity-50 ${variantClasses} ${sizeClasses} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}
