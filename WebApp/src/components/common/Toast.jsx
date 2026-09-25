// ============================================================
// File: Toast.jsx
// Purpose: Shared transient notification. Used to surface the API's
//          exact response message (success or error, including a 409
//          conflict) verbatim — the client never pre-guesses whether
//          an action is allowed, per the FAT service pattern; it
//          just relays what the server said.
// Author: Shalon
// ============================================================
import { useEffect } from 'react';

const TONE_CLASSES = {
  error: 'border-error/30 bg-error-soft text-error',
  success: 'border-success/30 bg-success-soft text-success',
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
        className={`flex items-start gap-3 rounded-lg border px-4 py-3 text-[13px] font-medium shadow-panel ${toneClasses}`}
        role="status"
      >
        <span className="flex-1">{message}</span>
        <button type="button" onClick={onDismiss} className="shrink-0 opacity-60 hover:opacity-100" aria-label="Dismiss">
          &#10005;
        </button>
      </div>
    </div>
  );
}

// ============================================================
// TEMPORARY FIX ADDED BY MIGARA TO UNBLOCK BUILD
// TODO: @Shalon - Please implement the actual useToast context/hook.
// ============================================================
export const useToast = () => {
  return {
    showToast: (message, tone = 'error') => {
      console.warn(`[useToast Dummy] ${tone.toUpperCase()}: ${message}`);
    },
    hideToast: () => {}
  };
};