// ============================================================
// File: StationDetailPage.jsx
// Purpose: Station detail & provisioning screen - editable operating
//          schedule, battery slot manager with add/edit/status controls,
//          and live reservations overview.
//          Calls the real Stations/Slots API and renders data as-is.
//          Design: Precision infrastructure aesthetic (stripe.design.md).
// Author: Shalon
// ============================================================
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api from '../../services/api.js';
import { Table, Th, Td } from '../../components/common/Table.jsx';
import Button from '../../components/common/Button.jsx';
import Input from '../../components/common/Input.jsx';
import Badge from '../../components/common/Badge.jsx';
import Toast from '../../components/common/Toast.jsx';
import ConfirmDialog from '../../components/common/ConfirmDialog.jsx';
import Icon from '../../components/common/Icon.jsx';
import SlotForm from './SlotForm.jsx';

// Formats a GeoJSON [lng, lat] coordinate pair as a readable "lat, lng" string.
function formatCoordinates(location) {
  const [lng, lat] = location?.coordinates ?? [];
  if (lat === undefined || lng === undefined) {
    return '-';
  }
  return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
}

// Editable opening/closing hours
function ScheduleEditor({ station, submitting, onSave }) {
  const [opensAt, setOpensAt] = useState(station.operatingSchedule?.opensAt || '08:00');
  const [closesAt, setClosesAt] = useState(station.operatingSchedule?.closesAt || '20:00');

  const handleSubmit = (event) => {
    event.preventDefault();
    onSave({ opensAt, closesAt });
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-4">
      <div className="w-40">
        <Input label="Opens at" type="time" required value={opensAt} onChange={(event) => setOpensAt(event.target.value)} />
      </div>
      <div className="w-40">
        <Input label="Closes at" type="time" required value={closesAt} onChange={(event) => setClosesAt(event.target.value)} />
      </div>
      <Button type="submit" variant="primary" disabled={submitting} className="mb-0.5 shadow-xs">
        {submitting ? 'Saving…' : 'Update Schedule'}
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
  const [activating, setActivating] = useState(false);

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
  const [refreshToken, setRefreshToken] = useState(0);

  // Load station details
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

  // Load reservations for station
  useEffect(() => {
    let cancelled = false;

    async function loadReservations() {
      setReservationsLoading(true);
      setReservationsError('');
      try {
        const { data } = await api.get(`/stations/${id}/reservations-overview`);
        if (!cancelled) {
          setReservations(data ?? []);
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

  const handleSaveSchedule = async ({ opensAt, closesAt }) => {
    setScheduleSubmitting(true);
    try {
      await api.put(`/stations/${id}`, { operatingSchedule: { opensAt, closesAt } });
      setRefreshToken((token) => token + 1);
      setToast({ message: 'Operating schedule updated.', tone: 'success' });
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to update schedule.', tone: 'error' });
    } finally {
      setScheduleSubmitting(false);
    }
  };

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

  const handleActivate = async () => {
    setActivating(true);
    try {
      await api.put(`/stations/${id}/activate`);
      setRefreshToken((token) => token + 1);
      setToast({ message: 'Station reactivated.', tone: 'success' });
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to reactivate station.', tone: 'error' });
    } finally {
      setActivating(false);
    }
  };

  const handleOpenCreateSlot = () => {
    setEditingSlot(null);
    setSlotFormError('');
    setSlotFormKey((key) => key + 1);
    setIsSlotFormOpen(true);
  };

  const handleOpenEditSlot = (slot) => {
    setEditingSlot(slot);
    setSlotFormError('');
    setSlotFormKey((key) => key + 1);
    setIsSlotFormOpen(true);
  };

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

  const slotNumberFor = (slotId) => station?.slots?.find((slot) => slot.id === slotId)?.slotNumber ?? slotId;

  if (loading) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-12 flex items-center justify-center text-sm text-muted">
        Loading station details…
      </div>
    );
  }

  if (loadError || !station) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-8 space-y-4">
        <Link to="/stations" className="text-xs font-semibold text-primary hover:underline">
          ← Back to Microgrid Hubs
        </Link>
        <div className="rounded-xl border border-error/30 bg-error-soft px-4 py-3 text-[13px] font-medium text-error">
          {loadError || 'Station not found.'}
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-8 space-y-8">

      {/* ── Breadcrumb & Back Link ───────────────────── */}
      <div>
        <Link
          to="/stations"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-muted hover:text-primary transition-colors mb-3"
        >
          <span>←</span>
          <span>Back to Microgrid Hubs</span>
        </Link>
      </div>

      {/* ── Station Hero Card ────────────────────────── */}
      <div className="rounded-xl border border-line bg-surface p-6 shadow-card">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div>
            <div className="flex items-center gap-2.5">
              <h1 className="text-2xl font-bold tracking-tight text-ink">{station.name}</h1>
              <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium border ${
                station.status === 'Active'
                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200/80'
                  : 'bg-slate-100 text-slate-600 border-slate-200'
              }`}>
                <span className={`h-1.5 w-1.5 rounded-full ${station.status === 'Active' ? 'bg-emerald-500' : 'bg-slate-400'}`} />
                {station.status}
              </span>
            </div>
            <p className="mt-1.5 font-mono text-[12px] text-muted flex items-center gap-1.5">
              <span>📍 GPS: {formatCoordinates(station.location)}</span>
              <span>·</span>
              <span>ID: {station.id}</span>
            </p>

            <div className="mt-6 flex flex-wrap gap-8 pt-4 border-t border-line/60">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-wider text-muted">Total Capacity</p>
                <p className="tnum mt-1 text-2xl font-bold text-ink">
                  {station.capacityKWh}
                  <span className="ml-1 text-[13px] font-normal text-muted">kWh</span>
                </p>
              </div>
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-wider text-muted">Declared Slots</p>
                <p className="tnum mt-1 text-2xl font-bold text-ink">{station.totalBatterySlots ?? '-'}</p>
              </div>
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-wider text-muted">Active Slot Configs</p>
                <p className="tnum mt-1 text-2xl font-bold text-ink">{station.slots?.length ?? 0}</p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {station.status === 'Active' ? (
              <Button
                variant="danger-outline"
                onClick={() => setConfirmDeactivateOpen(true)}
                disabled={deactivating}
                className="shadow-xs"
              >
                {deactivating ? 'Deactivating…' : 'Deactivate Station'}
              </Button>
            ) : (
              <Button
                variant="secondary"
                onClick={handleActivate}
                disabled={activating}
                className="shadow-xs"
              >
                {activating ? 'Reactivating…' : 'Reactivate Station'}
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* ── Operating Schedule Section ───────────────── */}
      <section className="rounded-xl border border-line bg-surface p-6 shadow-card space-y-4">
        <div>
          <h2 className="text-base font-semibold tracking-tight text-ink">Operating Schedule</h2>
          <p className="text-[12px] text-muted mt-0.5">Opening and closing window for prosumer battery reservation check-ins.</p>
        </div>
        <div className="pt-2">
          <ScheduleEditor
            key={station.updatedAt}
            station={station}
            submitting={scheduleSubmitting}
            onSave={handleSaveSchedule}
          />
        </div>
      </section>

      {/* ── Battery Slots Management ─────────────────── */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-semibold tracking-tight text-ink">Battery Slots Provisioning</h2>
            <p className="text-[12px] text-muted mt-0.5">Individual battery slots and live availability state.</p>
          </div>
          <Button variant="primary" onClick={handleOpenCreateSlot} className="shadow-card">
            + New Slot
          </Button>
        </div>

        <div className="rounded-xl border border-line bg-surface shadow-card overflow-hidden">
          <Table>
            <thead>
              <tr className="bg-surface-alt/70 border-b border-line text-[11px] font-semibold uppercase tracking-wider text-muted">
                <Th className="text-right">Slot #</Th>
                <Th>Battery Type</Th>
                <Th className="text-right">Capacity</Th>
                <Th>Status</Th>
                <Th className="text-right">Actions</Th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line/80">
              {(!station.slots || station.slots.length === 0) && (
                <tr>
                  <Td colSpan={5} className="py-8 text-center text-[13px] text-muted">
                    No battery slots defined for this hub yet. Click "+ New Slot" above to provision one.
                  </Td>
                </tr>
              )}
              {station.slots?.map((slot) => (
                <tr key={slot.id} className="transition-colors hover:bg-surface-alt/50">
                  <Td className="tnum text-right font-bold text-ink py-3.5">
                    Slot {slot.slotNumber}
                  </Td>
                  <Td className="text-body font-medium py-3.5">{slot.type}</Td>
                  <Td className="tnum text-right font-semibold text-ink py-3.5">
                    {slot.capacityKWh}
                    <span className="ml-1 text-[11px] font-normal text-muted">kWh</span>
                  </Td>
                  <Td className="py-3.5">
                    <select
                      value={slot.status}
                      disabled={slotStatusUpdatingId === slot.id}
                      onChange={(event) => handleSlotStatusChange(slot.id, event.target.value)}
                      className="h-8 rounded-md border border-line bg-surface px-2.5 text-xs font-medium text-ink focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 disabled:opacity-50 cursor-pointer shadow-xs"
                    >
                      <option value="Available">Available</option>
                      <option value="Reserved">Reserved</option>
                      <option value="Maintenance">Maintenance</option>
                    </select>
                  </Td>
                  <Td className="text-right py-3.5">
                    <Button variant="secondary" size="sm" onClick={() => handleOpenEditSlot(slot)}>
                      Edit Slot
                    </Button>
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
        </div>
      </section>

      {/* ── Active Reservations Overview ─────────────── */}
      <section className="space-y-4">
        <div>
          <h2 className="text-base font-semibold tracking-tight text-ink">Active Reservations Overview</h2>
          <p className="text-[12px] text-muted mt-0.5">Live schedule of prosumers booked for this microgrid hub.</p>
        </div>

        <div className="rounded-xl border border-line bg-surface shadow-card overflow-hidden">
          <Table>
            <thead>
              <tr className="bg-surface-alt/70 border-b border-line text-[11px] font-semibold uppercase tracking-wider text-muted">
                <Th className="text-right">Slot #</Th>
                <Th>Prosumer NIC</Th>
                <Th>Scheduled Time</Th>
                <Th>Status</Th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line/80">
              {reservationsLoading && (
                <tr>
                  <Td colSpan={4} className="py-8 text-center text-[13px] text-muted">
                    Loading reservation overview…
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
                  <tr key={`${reservation.slotId}-${reservation.scheduledAt}`} className="hover:bg-surface-alt/50 transition-colors">
                    <Td className="tnum text-right font-medium py-3.5">Slot {slotNumberFor(reservation.slotId)}</Td>
                    <Td className="font-mono text-[12px] text-body py-3.5">{reservation.prosumerNic ?? '-'}</Td>
                    <Td className="tnum text-body py-3.5">
                      {reservation.scheduledAt ? new Date(reservation.scheduledAt).toLocaleString() : '-'}
                    </Td>
                    <Td className="py-3.5">
                      <Badge tone={reservation.status === 'Confirmed' ? 'info' : 'neutral'}>{reservation.status}</Badge>
                    </Td>
                  </tr>
                ))}
            </tbody>
          </Table>
        </div>
      </section>

      {/* ── Dialogs & Modals ─────────────────────────── */}
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
        title="Deactivate station node?"
        description={`"${station.name}" will be marked inactive and taken offline for new reservations.`}
        confirmLabel="Deactivate Node"
        tone="danger"
        confirming={deactivating}
        onConfirm={handleDeactivate}
        onClose={() => setConfirmDeactivateOpen(false)}
      />

      <Toast message={toast.message} tone={toast.tone} onDismiss={() => setToast({ message: '', tone: 'error' })} />
    </div>
  );
}
