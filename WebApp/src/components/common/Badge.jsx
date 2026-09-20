// ============================================================
// File: Badge.jsx
// Purpose: Small rounded status pill (station/slot status, etc.),
//          shared so every page renders status the same way.
// Author: Shalon
// ============================================================
const TONE_CLASSES = {
  success: 'bg-surface-tint text-success',
  neutral: 'bg-surface-alt text-muted',
  warning: 'bg-warning/25 text-ink',
  error: 'bg-error/10 text-error',
};

// Renders a small pill-shaped status label in the given semantic tone.
export default function Badge({ tone = 'neutral', children }) {
  const toneClasses = TONE_CLASSES[tone] ?? TONE_CLASSES.neutral;
  return (
    <span className={`inline-flex items-center rounded-pill px-3 py-1 text-xs font-semibold ${toneClasses}`}>
      {children}
    </span>
  );
}
