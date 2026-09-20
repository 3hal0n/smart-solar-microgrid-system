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
import MapPicker from '../../components/common/MapPicker.jsx';

// Small inline pin glyph for the "Pick on map" trigger — avoids pulling in an icon library
// for a single icon.
function PinIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
      <path
        d="M7 0C3.7 0 1 2.7 1 6c0 4.5 6 8 6 8s6-3.5 6-8c0-3.3-2.7-6-6-6z"
        fill="currentColor"
      />
      <circle cx="7" cy="6" r="2.2" fill="white" />
    </svg>
  );
}

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
  const [isMapPickerOpen, setIsMapPickerOpen] = useState(false);
  const isEdit = Boolean(station);

  // Updates a single field's value as the user types.
  const handleChange = (field) => (event) => {
    setValues((prev) => ({ ...prev, [field]: event.target.value }));
  };

  // Writes the map picker's confirmed coordinates into the same lat/lng fields manual entry
  // uses — both paths converge on the same string-valued form state, so nothing downstream
  // needs to know which one was used. Values arrive as plain decimal numbers (see MapPicker's
  // roundCoordinate), matching the backend's lat/lng number contract.
  const handleMapConfirm = ({ lat, lng }) => {
    setValues((prev) => ({ ...prev, lat: String(lat), lng: String(lng) }));
    setIsMapPickerOpen(false);
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
    <Modal
      open={open}
      onClose={onClose}
      title={isEdit ? 'Edit station' : 'New station'}
      description="GPS position, capacity specs and operating hours for this grid node."
    >
      <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
        <Input label="Station name" required value={values.name} onChange={handleChange('name')} />

        {/* Paired inputs sit in a balanced 2-column grid, stacking on narrow screens. */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Input
            label="Latitude"
            type="number"
            step="any"
            required
            placeholder="6.9271"
            className="font-mono"
            value={values.lat}
            onChange={handleChange('lat')}
          />
          <Input
            label="Longitude"
            type="number"
            step="any"
            required
            placeholder="79.8612"
            className="font-mono"
            value={values.lng}
            onChange={handleChange('lng')}
          />
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
          <Input
            label="Total battery slots"
            type="number"
            step="1"
            required
            className="tnum"
            value={values.totalBatterySlots}
            onChange={handleChange('totalBatterySlots')}
          />
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Input label="Opens at" type="time" required value={values.opensAt} onChange={handleChange('opensAt')} />
          <Input label="Closes at" type="time" required value={values.closesAt} onChange={handleChange('closesAt')} />
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
            {submitting ? 'Saving…' : isEdit ? 'Save changes' : 'Create station'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
