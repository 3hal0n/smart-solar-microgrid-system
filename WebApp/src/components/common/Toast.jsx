// ============================================================
// File: Toast.jsx
// Purpose: Shared transient notification. Used to surface the API's
//          exact response message (success or error, including a 409
//          conflict) verbatim — the client never pre-guesses whether
//          an action is allowed, per the FAT service pattern; it
//          just relays what the server said.
// Author: Shalon
// ============================================================
import { useEffect, useState } from 'react';

const TONE_CONFIG = {
  error: { accent: 'bg-error', iconBg: 'bg-error-soft text-error' },
  success: { accent: 'bg-success', iconBg: 'bg-success-soft text-success' },
};

// A tick and a triangle-exclamation, drawn inline rather than pulled from an icon library — this
// codebase already draws its handful of glyphs inline (see QrPlaceholder in LandingPage.jsx)
// rather than taking on a dependency for a couple of icons.
function ToneIcon({ tone }) {
  if (tone === 'success') {
    return (
      <svg viewBox="0 0 20 20" className="h-3.5 w-3.5" fill="none" aria-hidden="true">
        <path d="M4 10.5 8 14l8-8" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 20 20" className="h-3.5 w-3.5" fill="none" aria-hidden="true">
      <path
        d="M10 3.5 2.5 16h15L10 3.5Z"
        stroke="currentColor"
        strokeWidth={1.6}
        strokeLinejoin="round"
      />
      <path d="M10 8.25v3.5" stroke="currentColor" strokeWidth={1.6} strokeLinecap="round" />
      <circle cx="10" cy="14" r="0.9" fill="currentColor" />
    </svg>
  );
}

// Renders a fixed, auto-dismissing toast; renders nothing when `message` is empty. Animates in
// from a slight offset rather than popping in instantly.
export default function Toast({ message, tone = 'error', onDismiss, durationMs = 6000 }) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (!message) {
      setVisible(false);
      return undefined;
    }
    const raf = requestAnimationFrame(() => setVisible(true));
    const timeoutId = setTimeout(onDismiss, durationMs);
    return () => {
      cancelAnimationFrame(raf);
      clearTimeout(timeoutId);
    };
  }, [message, onDismiss, durationMs]);

  if (!message) {
    return null;
  }

  const config = TONE_CONFIG[tone] ?? TONE_CONFIG.error;

  return (
    <div className="fixed bottom-6 right-6 z-50 max-w-sm">
      <div
        className={`flex items-stretch overflow-hidden rounded-xl border border-line bg-surface shadow-panel transition-all duration-300 ease-out ${
          visible ? 'translate-y-0 opacity-100' : 'translate-y-2 opacity-0'
        }`}
        role="status"
      >
        <span className={`w-1 shrink-0 ${config.accent}`} aria-hidden="true" />
        <div className="flex flex-1 items-start gap-3 px-3.5 py-3.5">
          <span className={`mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full ${config.iconBg}`}>
            <ToneIcon tone={tone} />
          </span>
          <span className="flex-1 pt-0.5 text-[13px] font-medium leading-snug text-ink">{message}</span>
          <button
            type="button"
            onClick={onDismiss}
            className="shrink-0 pt-0.5 text-muted transition-colors hover:text-ink"
            aria-label="Dismiss"
          >
            &#10005;
          </button>
        </div>
      </div>
    </div>
  );
}
