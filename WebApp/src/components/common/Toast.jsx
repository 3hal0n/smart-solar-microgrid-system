// ============================================================
// File: Toast.jsx
// Purpose: Shared transient notification banner. Used to surface
//          the API's exact response message (success or error,
//          including a 409 conflict) verbatim — the client never
//          pre-guesses whether an action is allowed, per the FAT
//          service pattern; it just relays what the server said.
// Author: Shalon
// ============================================================
import { useEffect } from 'react';

const TONE_CLASSES = {
  error: 'bg-error text-white',
  success: 'bg-primary text-on-primary',
};

// Renders a fixed, auto-dismissing toast; renders nothing when `message` is empty.
export default function Toast({ message, tone = 'error', onDismiss, durationMs = 6000 }) {
  useEffect(() => {
    if (!message) {
      return undefined;
    }
    const timeoutId = setTimeout(onDismiss, durationMs);
    return () => clearTimeout(timeoutId);
  }, [message, onDismiss, durationMs]);

  if (!message) {
    return null;
  }

  const toneClasses = TONE_CLASSES[tone] ?? TONE_CLASSES.error;

  return (
    <div className="fixed bottom-6 right-6 z-50 max-w-sm">
      <div
        className={`flex items-start gap-3 rounded-md px-4 py-3 text-sm font-medium shadow-[0_20px_66px_0_rgba(34,48,73,0.20)] ${toneClasses}`}
        role="status"
      >
        <span className="flex-1">{message}</span>
        <button type="button" onClick={onDismiss} className="text-white/80 hover:text-white" aria-label="Dismiss">
          &#10005;
        </button>
      </div>
    </div>
  );
}
