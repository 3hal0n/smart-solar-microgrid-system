// ============================================================
// File: Input.jsx
// Purpose: Shared labeled form field. Renders an <input> by
//          default, or a <select> when as="select" is passed
//          (children become <option>s). Same label/error/hint
//          treatment either way.
// Author: Shalon (select support added by Dinil)
// ============================================================
export default function Input({
  label,
  error,
  hint,
  as = "input",
  className = "",
  children,
  ...props
}) {
  const fieldClasses = `rounded-md border bg-surface px-3 text-sm text-ink shadow-card transition-shadow placeholder:text-muted focus:outline-none focus:ring-2 ${
    error
      ? "border-error focus:border-error focus:ring-error/20"
      : "border-line focus:border-primary focus:ring-primary/20"
  } ${className}`;

  return (
    <label className="flex flex-col gap-1.5">
      {label && (
        <span className="text-[13px] font-medium text-ink">{label}</span>
      )}

      {as === "select" ? (
        <select className={`h-9 ${fieldClasses}`} {...props}>
          {children}
        </select>
      ) : (
        <input className={`h-9 ${fieldClasses}`} {...props} />
      )}

      {error ? (
        <span className="text-xs text-error">{error}</span>
      ) : (
        hint && <span className="text-xs text-muted">{hint}</span>
      )}
    </label>
  );
}
