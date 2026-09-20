// ============================================================
// File: Input.jsx
// Purpose: Shared labeled input with an inline error slot. Stripe
//          form treatment: 13px medium label, white field on a 1px
//          keyline, and a subtle violet focus ring.
// Author: Shalon
// ============================================================
// Renders a labeled form input, optionally showing an inline error message below it.
export default function Input({ label, error, hint, className = '', ...props }) {
  return (
    <label className="flex flex-col gap-1.5">
      {label && <span className="text-[13px] font-medium text-ink">{label}</span>}
      <input
        className={`h-9 rounded-md border bg-surface px-3 text-sm text-ink shadow-card transition-shadow placeholder:text-muted focus:outline-none focus:ring-2 ${
          error
            ? 'border-error focus:border-error focus:ring-error/20'
            : 'border-line focus:border-primary focus:ring-primary/20'
        } ${className}`}
        {...props}
      />
      {error ? (
        <span className="text-xs text-error">{error}</span>
      ) : (
        hint && <span className="text-xs text-muted">{hint}</span>
      )}
    </label>
  );
}
