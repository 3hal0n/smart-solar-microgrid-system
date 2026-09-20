// ============================================================
// File: Input.jsx
// Purpose: Shared labeled text/number input with an inline error
//          slot, reused across search boxes and create/edit forms.
// Author: Shalon
// ============================================================
// Renders a labeled form input, optionally showing an inline error message below it.
export default function Input({ label, error, className = '', ...props }) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-sm font-semibold text-ink">{label}</span>
      <input
        className={`rounded-md border bg-canvas px-3.5 py-2.5 text-sm text-ink placeholder:text-muted focus:outline-none focus:ring-2 focus:ring-accent/40 ${
          error ? 'border-error' : 'border-line focus:border-primary'
        } ${className}`}
        {...props}
      />
      {error && <span className="text-xs font-medium text-error">{error}</span>}
    </label>
  );
}
