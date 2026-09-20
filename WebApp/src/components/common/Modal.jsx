// ============================================================
// File: Modal.jsx
// Purpose: Shared overlay modal shell (backdrop + rounded panel)
//          used for every create/edit dialog across pages.
// Author: Shalon
// ============================================================
import { useEffect } from 'react';

// Renders a centered modal panel over a dimmed backdrop; closes on backdrop click or Escape.
export default function Modal({ open, onClose, title, children }) {
  useEffect(() => {
    if (!open) {
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
  }, [open, onClose]);

  if (!open) {
    return null;
  }

  // Closes the modal only when the backdrop itself (not the panel) is clicked.
  const handleBackdropClick = (event) => {
    if (event.target === event.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-ink/40 px-4"
      onClick={handleBackdropClick}
    >
      <div
        className="w-full max-w-lg rounded-lg border border-line bg-canvas p-6 shadow-[0_20px_66px_0_rgba(34,48,73,0.20)]"
        role="dialog"
        aria-modal="true"
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-bold text-ink">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            className="flex h-8 w-8 items-center justify-center rounded-pill text-muted hover:bg-surface-alt hover:text-ink"
            aria-label="Close"
          >
            &#10005;
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
