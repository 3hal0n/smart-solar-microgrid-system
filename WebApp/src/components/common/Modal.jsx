// ============================================================
// File: Modal.jsx
// Purpose: Shared overlay dialog shell (backdrop + bordered panel)
//          used for every create/edit dialog. Stripe treatment:
//          white surface, 1px keyline, blue-tinted elevation, and a
//          ruled header rather than a floating title.
// Author: Shalon
// ============================================================
import { useEffect } from 'react';

// Renders a centered modal panel over a dimmed backdrop; closes on backdrop click or Escape.
// `suppressClose` is for a modal that renders a second modal on top of itself (e.g. a map picker
// opened from a form modal) — the parent passes true while the child is open, so Escape/backdrop
// only closes the topmost one instead of both at once.
export default function Modal({ open, onClose, title, description, children, suppressClose = false }) {
  useEffect(() => {
    if (!open || suppressClose) {
      return undefined;
    }
    // Closes the modal when the user presses Escape while it's open.
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, suppressClose, onClose]);

  if (!open) {
    return null;
  }

  // Closes the modal only when the backdrop itself (not the panel) is clicked, and only when
  // this modal isn't currently suppressed by a child modal on top of it.
  const handleBackdropClick = (event) => {
    if (!suppressClose && event.target === event.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-ink/50 px-4 py-6"
      onClick={handleBackdropClick}
    >
      <div
        className="max-h-full w-full max-w-xl overflow-y-auto rounded-lg border border-line bg-surface shadow-panel"
        role="dialog"
        aria-modal="true"
      >
        <div className="flex items-start justify-between gap-4 border-b border-line px-6 py-4">
          <div>
            <h2 className="text-base font-semibold tracking-tight text-ink">{title}</h2>
            {description && <p className="mt-0.5 text-[13px] text-muted">{description}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="-mr-1.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-muted transition-colors hover:bg-surface-alt hover:text-ink"
            aria-label="Close"
          >
            &#10005;
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
      </div>
    </div>
  );
}
