// ============================================================
// File: ConfirmDialog.jsx
// Purpose: Shared "are you sure" confirmation, built on Modal instead
//          of the native window.confirm() - keeps destructive actions
//          (station deactivation, etc.) inside the app's own visual
//          language instead of an unstyled browser dialog.
// Author: Shalon
// ============================================================
import Modal from './Modal.jsx';
import Button from './Button.jsx';

// Renders a small confirm/cancel dialog. `tone` picks the confirm button's variant - "danger" for
// destructive actions, "primary" otherwise.
export default function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Confirm',
  tone = 'primary',
  confirming = false,
  onConfirm,
  onClose,
}) {
  return (
    <Modal open={open} onClose={onClose} title={title} description={description} suppressClose={confirming}>
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onClose} disabled={confirming}>
          Cancel
        </Button>
        <Button variant={tone} onClick={onConfirm} disabled={confirming}>
          {confirming ? 'Working…' : confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
