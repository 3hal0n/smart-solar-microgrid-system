// ============================================================
// File: Badge.jsx
// Purpose: Compact status badge (station/slot status, etc.). Stripe
//          dashboard treatment: tinted fill, 1px tone border, small
//          medium-weight label — data-like, not celebratory.
// Author: Shalon
// ============================================================
const TONE_CLASSES = {
  success: 'bg-success-soft text-success border-success/20',
  neutral: 'bg-surface-alt text-body border-line',
  warning: 'bg-warning-soft text-warning border-warning/20',
  error: 'bg-error-soft text-error border-error/20',
  info: 'bg-primary-soft text-primary border-primary/20',
};

// Renders a compact status label in the given semantic tone.
export default function Badge({ tone = 'neutral', children }) {
  const toneClasses = TONE_CLASSES[tone] ?? TONE_CLASSES.neutral;
  return (
    <span
      className={`inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium ${toneClasses}`}
    >
      {children}
    </span>
  );
}
