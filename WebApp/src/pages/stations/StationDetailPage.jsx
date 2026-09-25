// ============================================================
// File: StationDetailPage.jsx
// Purpose: Station detail screen — editable operating schedule, a
//          slot list with add/edit/status controls, and a read-only
//          reservations overview, per architecture.md §6 "Web —
//          Station Detail". Calls the real Stations/Slots API and
//          renders its data/errors as-is; no business rules live
//          here, per the FAT service pattern.
// Author: Shalon
// ============================================================
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import api from '../../services/api.js';
import { Table, Th, Td } from '../../components/common/Table.jsx';
import Button from '../../components/common/Button.jsx';
import Input from '../../components/common/Input.jsx';
import Badge from '../../components/common/Badge.jsx';
import Toast from '../../components/common/Toast.jsx';
import ConfirmDialog from '../../components/common/ConfirmDialog.jsx';
import SlotForm from './SlotForm.jsx';

// Formats a GeoJSON [lng, lat] coordinate pair as a readable "lat, lng" string.
function formatCoordinates(location) {
  const [lng, lat] = location?.coordinates ?? [];
  if (lat === undefined || lng === undefined) {
    return '—';
  }
  return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
}

// Editable opening/closing hours as a self-contained subform, so its local edit state doesn't
// need to be synced back from the parent via an effect. The parent remounts it (via `key`) after
// a successful save, so it re-derives fresh values from the latest station.
function ScheduleEditor({ station, submitting, onSave }) {
  const [opensAt, setOpensAt] = useState(station.operatingSchedule.opensAt);
  const [closesAt, setClosesAt] = useState(station.operatingSchedule.closesAt);

  // Submits the edited opening/closing times to the parent's save handler.
  const handleSubmit = (event) => {
    event.preventDefault();
    onSave({ opensAt, closesAt });
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-4">
      <Input label="Opens at" type="time" required value={opensAt} onChange={(event) => setOpensAt(event.target.value)} />
      <Input label="Closes at" type="time" required value={closesAt} onChange={(event) => setClosesAt(event.target.value)} />
      <Button type="submit" variant="primary" disabled={submitting}>
        {submitting ? 'Saving…' : 'Save schedule'}
      </Button>
    </form>
  );
}

export default function StationDetailPage() {
  const { id } = useParams();

  const [station, setStation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [scheduleSubmitting, setScheduleSubmitting] = useState(false);
  const [deactivating, setDeactivating] = useState(false);
  const [confirmDeactivateOpen, setConfirmDeactivateOpen] = useState(false);

  const [reservations, setReservations] = useState([]);
  const [reservationsLoading, setReservationsLoading] = useState(true);
  const [reservationsError, setReservationsError] = useState('');

  const [isSlotFormOpen, setIsSlotFormOpen] = useState(false);
  const [editingSlot, setEditingSlot] = useState(null);
  const [slotFormKey, setSlotFormKey] = useState(0);
  const [slotSubmitting, setSlotSubmitting] = useState(false);
  const [slotFormError, setSlotFormError] = useState('');
  const [slotStatusUpdatingId, setSlotStatusUpdatingId] = useState(null);

  const [toast, setToast] = useState({ message: '', tone: 'error' });

  // Bumped by mutation handlers (schedule save, deactivate, slot add/edit/status change) to
  // trigger a re-fetch below, instead of calling a state-setting function directly from an effect.
  const [refreshToken, setRefreshToken] = useState(0);

  // Loads the full station (info + slots) from the real API; re-runs whenever refreshToken changes.
  useEffect(() => {
    let cancelled = false;

    async function loadStation() {
      setLoading(true);
      setLoadError('');
      try {
        const { data } = await api.get(`/stations/${id}`);
        if (!cancelled) {
          setStation(data);
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err.response?.data?.message || 'Failed to load station.');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadStation();
    return () => {
      cancelled = true;
    };
  }, [id, refreshToken]);

  // Loads the read-only reservations overview.
  //
  // NOTE(Dinil dependency): GET /stations/{id}/reservations-overview reads Dinil's Reservations
  // collection, which doesn't exist in this repo yet (architecture.md §2.4) — the endpoint is
  // currently stubbed server-side (StationService.GetReservationsOverviewAsync) to always return
  // []. This panel is built to degrade gracefully against that stub: empty state below, not a
  // broken one. Re-test this panel once Dinil's Reservations backend is live, to confirm real
  // rows (slot/prosumer NIC/scheduled time/status) render correctly.
  useEffect(() => {
    let cancelled = false;

    async function loadReservations() {
      setReservationsLoading(true);
      setReservationsError('');
      try {
        const { data } = await api.get(`/stations/${id}/reservations-overview`);
        if (!cancelled) {
          setReservations(data);
        }
      } catch (err) {
        if (!cancelled) {
          setReservationsError(err.response?.data?.message || 'Failed to load reservations overview.');
        }
      } finally {
        if (!cancelled) {
          setReservationsLoading(false);
        }
      }
    }

    loadReservations();
    return () => {
      cancelled = true;
    };
  }, [id]);

  // Saves the edited operating schedule via the existing generic partial-update endpoint
  // (PUT /stations/{id} already accepts operatingSchedule as one of its partial fields).
  const handleSaveSchedule = async ({ opensAt, closesAt }) => {
    setScheduleSubmitting(true);
    try {
      await api.put(`/stations/${id}`, { operatingSchedule: { opensAt, closesAt } });
      setRefreshToken((token) => token + 1);
      setToast({ message: 'Schedule updated.', tone: 'success' });
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to update schedule.', tone: 'error' });
    } finally {
      setScheduleSubmitting(false);
    }
  };

  // Deactivates the station; shows the API's exact response message (including a 409 conflict
  // naming which reservations/slots blocked it) in a toast, rather than pre-guessing client-side
  // whether the deactivation would be allowed.
  const handleDeactivate = async () => {
    setDeactivating(true);
    try {
      await api.put(`/stations/${id}/deactivate`);
      setRefreshToken((token) => token + 1);
      setToast({ message: 'Station deactivated.', tone: 'success' });
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to deactivate station.', tone: 'error' });
    } finally {
      setDeactivating(false);
      setConfirmDeactivateOpen(false);
    }
  };

  // Opens the slot modal in "create" mode.
  const handleOpenCreateSlot = () => {
    setEditingSlot(null);
    setSlotFormError('');
    setSlotFormKey((key) => key + 1);
    setIsSlotFormOpen(true);
  };

  // Opens the slot modal in "edit" mode for the given slot.
  const handleOpenEditSlot = (slot) => {
    setEditingSlot(slot);
    setSlotFormError('');
    setSlotFormKey((key) => key + 1);
    setIsSlotFormOpen(true);
  };

  // Submits the create/edit slot form to the real endpoint and reloads the station on success.
  const handleSubmitSlot = async (values) => {
    setSlotSubmitting(true);
    setSlotFormError('');
    try {
      if (editingSlot) {
        await api.put(`/slots/${editingSlot.id}`, values);
      } else {
        await api.post(`/stations/${id}/slots`, values);
      }
      setIsSlotFormOpen(false);
      setRefreshToken((token) => token + 1);
      setToast({ message: editingSlot ? 'Slot updated.' : 'Slot created.', tone: 'success' });
    } catch (err) {
      setSlotFormError(err.response?.data?.message || 'Something went wrong. Please try again.');
    } finally {
      setSlotSubmitting(false);
    }
  };

  // Quick inline status change for a slot, without opening the full edit modal.
  const handleSlotStatusChange = async (slotId, status) => {
    setSlotStatusUpdatingId(slotId);
    try {
      await api.put(`/slots/${slotId}`, { status });
      setRefreshToken((token) => token + 1);
      setToast({ message: 'Slot status updated.', tone: 'success' });
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to update slot status.', tone: 'error' });
    } finally {
      setSlotStatusUpdatingId(null);
    }
  };

  // Looks up a slot's number for display in the reservations overview (which only carries slotId).
  const slotNumberFor = (slotId) => station?.slots?.find((slot) => slot.id === slotId)?.slotNumber ?? slotId;

  if (loading) {
    return <div className="mx-auto max-w-6xl px-6 py-8 text-[13px] text-muted">Loading station…</div>;
  }

  if (loadError || !station) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-8">
        <p className="rounded-md border border-error/30 bg-error-soft px-3 py-2 text-[13px] font-medium text-error">
          {loadError || 'Station not found.'}
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      <div className="mb-8 flex flex-col gap-4 rounded-lg border border-line bg-surface p-6 shadow-card sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-xl font-semibold tracking-tight text-ink">{station.name}</h1>
            <Badge tone={station.status === 'Active' ? 'success' : 'neutral'}>{station.status}</Badge>
          </div>
          <p className="mt-1.5 font-mono text-[12px] text-muted">{formatCoordinates(station.location)}</p>
          <div className="mt-4 flex gap-8">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-wider text-muted">Capacity</p>
              <p className="tnum mt-0.5 text-lg text-ink">
                {station.capacityKWh}
                <span className="ml-1 text-[13px] text-muted">kWh</span>
              </p>
            </div>
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-wider text-muted">Declared slots</p>
              <p className="tnum mt-0.5 text-lg text-ink">{station.totalBatterySlots}</p>
            </div>
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-wider text-muted">Defined slots</p>
              <p className="tnum mt-0.5 text-lg text-ink">{station.slots.length}</p>
            </div>
          </div>
        </div>
        {station.status === 'Active' ? (
          <Button variant="danger-outline" onClick={() => setConfirmDeactivateOpen(true)} disabled={deactivating}>
            {deactivating ? 'Deactivating…' : 'Deactivate station'}
          </Button>
        ) : (
          <span className="text-[13px] text-muted">Station is inactive.</span>
        )}
      </div>

      <section className="mb-8">
        <h2 className="mb-1 text-base font-semibold tracking-tight text-ink">Operating schedule</h2>
        <p className="mb-3 text-[13px] text-muted">Opening hours applied to this node.</p>
        <div className="rounded-lg border border-line bg-surface p-5 shadow-card">
          <ScheduleEditor
            key={station.updatedAt}
            station={station}
            submitting={scheduleSubmitting}
            onSave={handleSaveSchedule}
          />
        </div>
      </section>

      <section className="mb-8">
        <div className="mb-3 flex items-end justify-between">
          <div>
            <h2 className="text-base font-semibold tracking-tight text-ink">Battery slots</h2>
            <p className="mt-1 text-[13px] text-muted">Availability is updated inline and saved immediately.</p>
          </div>
          <Button variant="primary" onClick={handleOpenCreateSlot}>
            New slot
          </Button>
        </div>
        <Table>
          <thead>
            <tr>
              <Th className="text-right">Slot</Th>
              <Th>Type</Th>
              <Th className="text-right">Capacity</Th>
              <Th>Status</Th>
              <Th className="text-right">Actions</Th>
            </tr>
          </thead>
          <tbody>
            {station.slots.length === 0 && (
              <tr>
                <Td colSpan={5} className="py-8 text-center text-[13px] text-muted">
                  No slots yet.
                </Td>
              </tr>
            )}
            {station.slots.map((slot) => (
              <tr key={slot.id} className="transition-colors hover:bg-surface-alt/60">
                <Td className="tnum text-right font-medium">{slot.slotNumber}</Td>
                <Td className="text-body">{slot.type}</Td>
                <Td className="tnum text-right">
                  {slot.capacityKWh}
                  <span className="ml-1 text-[12px] text-muted">kWh</span>
                </Td>
                <Td>
                  <select
                    value={slot.status}
                    disabled={slotStatusUpdatingId === slot.id}
                    onChange={(event) => handleSlotStatusChange(slot.id, event.target.value)}
                    className="h-8 rounded-md border border-line bg-surface px-2 text-[13px] text-ink focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 disabled:opacity-50"
                  >
                    <option value="Available">Available</option>
                    <option value="Reserved">Reserved</option>
                    <option value="Maintenance">Maintenance</option>
                  </select>
                </Td>
                <Td className="text-right">
                  <Button variant="secondary" size="sm" onClick={() => handleOpenEditSlot(slot)}>
                    Edit
                  </Button>
                </Td>
              </tr>
            ))}
          </tbody>
        </Table>
      </section>

      <section>
        <h2 className="mb-1 text-base font-semibold tracking-tight text-ink">Reservations overview</h2>
        <p className="mb-3 text-[13px] text-muted">Read-only — who's currently booked at this station.</p>
        <Table>
          <thead>
            <tr>
              <Th className="text-right">Slot</Th>
              <Th>Prosumer NIC</Th>
              <Th>Scheduled time</Th>
              <Th>Status</Th>
            </tr>
          </thead>
          <tbody>
            {reservationsLoading && (
              <tr>
                <Td colSpan={4} className="py-8 text-center text-[13px] text-muted">
                  Loading reservations…
                </Td>
              </tr>
            )}
            {!reservationsLoading && reservationsError && (
              <tr>
                <Td colSpan={4} className="py-8 text-center text-[13px] text-error">
                  {reservationsError}
                </Td>
              </tr>
            )}
            {!reservationsLoading && !reservationsError && reservations.length === 0 && (
              <tr>
                <Td colSpan={4} className="py-8 text-center text-[13px] text-muted">
                  No active reservations for this station.
                </Td>
              </tr>
            )}
            {!reservationsLoading &&
              !reservationsError &&
              reservations.map((reservation) => (
                <tr key={`${reservation.slotId}-${reservation.scheduledAt}`}>
                  <Td className="tnum text-right font-medium">{slotNumberFor(reservation.slotId)}</Td>
                  <Td className="font-mono text-[12px] text-body">{reservation.prosumerNic ?? '—'}</Td>
                  <Td className="tnum text-body">
                    {reservation.scheduledAt ? new Date(reservation.scheduledAt).toLocaleString() : '—'}
                  </Td>
                  <Td>
                    <Badge tone={reservation.status === 'Confirmed' ? 'info' : 'neutral'}>{reservation.status}</Badge>
                  </Td>
                </tr>
              ))}
          </tbody>
        </Table>
      </section>

      <SlotForm
        key={slotFormKey}
        open={isSlotFormOpen}
        slot={editingSlot}
        submitting={slotSubmitting}
        serverError={slotFormError}
        onClose={() => setIsSlotFormOpen(false)}
        onSubmit={handleSubmitSlot}
      />

      <ConfirmDialog
        open={confirmDeactivateOpen}
        title="Deactivate station?"
        description={`"${station.name}" will be marked inactive. This can't be undone from here.`}
        confirmLabel="Deactivate"
        tone="danger"
        confirming={deactivating}
        onConfirm={handleDeactivate}
        onClose={() => setConfirmDeactivateOpen(false)}
      />

      <Toast message={toast.message} tone={toast.tone} onDismiss={() => setToast({ message: '', tone: 'error' })} />
    </div>
  );
}
