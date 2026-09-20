// ============================================================
// File: StationForm.jsx
// Purpose: Create/edit modal for a microgrid station, per
//          architecture.md §3 POST /stations and PUT /stations/{id}.
//          Pure UI: submits to the real endpoint and surfaces the
//          API's validation errors verbatim — no business rule
//          (GPS range, capacity > 0, etc.) is decided here, per the
//          FAT service pattern; that all lives in StationService.
// Author: Shalon
// ============================================================
import { useState } from 'react';
import Modal from '../../components/common/Modal.jsx';
import Input from '../../components/common/Input.jsx';
import Button from '../../components/common/Button.jsx';

const EMPTY_VALUES = {
  name: '',
  lat: '',
  lng: '',
  capacityKWh: '',
  totalBatterySlots: '',
  opensAt: '',
  closesAt: '',
};

// Converts a loaded station (list-row or full-detail shape) into the form's flat field values.
function toFormValues(station) {
  if (!station) {
    return EMPTY_VALUES;
  }
  const [lng, lat] = station.location?.coordinates ?? [];
  return {
    name: station.name ?? '',
    lat: lat ?? '',
    lng: lng ?? '',
    capacityKWh: station.capacityKWh ?? '',
    totalBatterySlots: station.totalBatterySlots ?? '',
    opensAt: station.operatingSchedule?.opensAt ?? '',
    closesAt: station.operatingSchedule?.closesAt ?? '',
  };
}

// Create/edit modal for a station; renders the shared Modal shell with a form inside.
// The parent remounts this component (via a changing `key`) each time it opens, so field
// values are simply derived once from `station` at mount — no effect-based reset needed.
export default function StationForm({ open, station, submitting, serverError, onClose, onSubmit }) {
  const [values, setValues] = useState(() => toFormValues(station));
  const isEdit = Boolean(station);

  // Updates a single field's value as the user types.
  const handleChange = (field) => (event) => {
    setValues((prev) => ({ ...prev, [field]: event.target.value }));
  };

  // Builds the request payload from form state and hands it to the parent's submit handler.
  const handleSubmit = (event) => {
    event.preventDefault();
    onSubmit({
      name: values.name,
      lat: Number(values.lat),
      lng: Number(values.lng),
      capacityKWh: Number(values.capacityKWh),
      totalBatterySlots: Number(values.totalBatterySlots),
      operatingSchedule: { opensAt: values.opensAt, closesAt: values.closesAt },
    });
  };

  return (
    <Modal open={open} onClose={onClose} title={isEdit ? 'Edit station' : 'New station'}>
      <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
        <Input label="Name" required value={values.name} onChange={handleChange('name')} />

        <div className="grid grid-cols-2 gap-4">
          <Input label="Latitude" type="number" step="any" required value={values.lat} onChange={handleChange('lat')} />
          <Input label="Longitude" type="number" step="any" required value={values.lng} onChange={handleChange('lng')} />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Capacity (kWh)"
            type="number"
            step="any"
            required
            value={values.capacityKWh}
            onChange={handleChange('capacityKWh')}
          />
          <Input
            label="Total battery slots"
            type="number"
            step="1"
            required
            value={values.totalBatterySlots}
            onChange={handleChange('totalBatterySlots')}
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Input label="Opens at" type="time" required value={values.opensAt} onChange={handleChange('opensAt')} />
          <Input label="Closes at" type="time" required value={values.closesAt} onChange={handleChange('closesAt')} />
        </div>

        {serverError && (
          <p className="rounded-sm bg-error/10 px-3 py-2 text-sm font-medium text-error">{serverError}</p>
        )}

        <div className="mt-2 flex justify-end gap-3">
          <Button type="button" variant="ghost" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" variant="accent" disabled={submitting}>
            {submitting ? 'Saving…' : isEdit ? 'Save changes' : 'Create station'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
