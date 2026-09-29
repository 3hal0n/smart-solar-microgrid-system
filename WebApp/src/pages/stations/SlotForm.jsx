// ============================================================
// File: SlotForm.jsx
// Purpose: Create/edit modal for a station's battery slot, per
//          architecture.md §3 POST /stations/{id}/slots and
//          PUT /slots/{id}. Pure UI: submits to the real endpoint
//          and surfaces the API's validation errors verbatim - no
//          business rule (capacity > 0, duplicate slot number, etc.)
//          is decided here, per the FAT service pattern.
// Author: Shalon
// ============================================================
import { useState } from 'react';
import Modal from '../../components/common/Modal.jsx';
import Input from '../../components/common/Input.jsx';
import Button from '../../components/common/Button.jsx';

const EMPTY_VALUES = { slotNumber: '', type: 'Charging', capacityKWh: '', status: 'Available' };

// Matches Input.jsx's field treatment so native selects sit flush with the rest of the form.
const SELECT_CLASSES =
  'h-9 rounded-md border border-line bg-surface px-2.5 text-sm text-ink shadow-card focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20';

// Converts a loaded slot into the form's flat field values.
function toFormValues(slot) {
  if (!slot) {
    return EMPTY_VALUES;
  }
  return {
    slotNumber: slot.slotNumber ?? '',
    type: slot.type ?? 'Charging',
    capacityKWh: slot.capacityKWh ?? '',
    status: slot.status ?? 'Available',
  };
}

// Create/edit modal for a slot; the parent remounts this component (via a changing `key`) each
// time it opens, so field values are simply derived once from `slot` at mount.
export default function SlotForm({ open, slot, submitting, serverError, onClose, onSubmit }) {
  const [values, setValues] = useState(() => toFormValues(slot));
  const isEdit = Boolean(slot);

  // Updates a single field's value as the user types/selects.
  const handleChange = (field) => (event) => {
    setValues((prev) => ({ ...prev, [field]: event.target.value }));
  };

  // Builds the request payload from form state and hands it to the parent's submit handler.
  // Status is only sent on edit - POST /stations/{id}/slots doesn't accept it (slots always
  // start Available server-side).
  const handleSubmit = (event) => {
    event.preventDefault();
    const payload = {
      slotNumber: Number(values.slotNumber),
      type: values.type,
      capacityKWh: Number(values.capacityKWh),
    };
    if (isEdit) {
      payload.status = values.status;
    }
    onSubmit(payload);
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={isEdit ? 'Edit slot' : 'New slot'}
      description="Battery slot definition for this station."
    >
      <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Input
            label="Slot number"
            type="number"
            step="1"
            required
            className="tnum"
            value={values.slotNumber}
            onChange={handleChange('slotNumber')}
          />
          <label className="flex flex-col gap-1.5">
            <span className="text-[13px] font-medium text-ink">Type</span>
            <select
              className={SELECT_CLASSES}
              value={values.type}
              onChange={handleChange('type')}
            >
              <option value="Charging">Charging</option>
              <option value="Discharging">Discharging</option>
            </select>
          </label>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Input
            label="Capacity (kWh)"
            type="number"
            step="any"
            required
            className="tnum"
            value={values.capacityKWh}
            onChange={handleChange('capacityKWh')}
          />
          {isEdit && (
            <label className="flex flex-col gap-1.5">
              <span className="text-[13px] font-medium text-ink">Status</span>
              <select
                className={SELECT_CLASSES}
                value={values.status}
                onChange={handleChange('status')}
              >
                <option value="Available">Available</option>
                <option value="Reserved">Reserved</option>
                <option value="Maintenance">Maintenance</option>
              </select>
            </label>
          )}
        </div>

        {serverError && (
          <p className="rounded-md border border-error/30 bg-error-soft px-3 py-2 text-[13px] font-medium text-error">
            {serverError}
          </p>
        )}

        <div className="-mx-6 -mb-5 mt-1 flex justify-end gap-2 border-t border-line bg-surface-alt px-6 py-4">
          <Button type="button" variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" disabled={submitting}>
            {submitting ? 'Saving…' : isEdit ? 'Save changes' : 'Add slot'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
