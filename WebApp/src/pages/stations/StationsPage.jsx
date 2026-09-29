// ============================================================
// File: StationsPage.jsx
// Purpose: Microgrid hub & node management screen - search, filter,
//          inspect node capacity, battery slot counts, and launch
//          create/edit modals.
//          Calls the real Stations API and renders its data/errors as-is.
//          Design: Precision infrastructure aesthetic (stripe.design.md).
// Author: Shalon
// ============================================================
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../services/api.js';
import { Table, Th, Td } from '../../components/common/Table.jsx';
import Button from '../../components/common/Button.jsx';
import Input from '../../components/common/Input.jsx';
import Badge from '../../components/common/Badge.jsx';
import Toast from '../../components/common/Toast.jsx';
import ConfirmDialog from '../../components/common/ConfirmDialog.jsx';
import StatCard from '../../components/common/StatCard.jsx';
import Icon from '../../components/common/Icon.jsx';
import StationForm from './StationForm.jsx';

// Formats a GeoJSON [lng, lat] coordinate pair as a fixed-width "lat, lng" string.
function formatCoordinates(location) {
  const [lng, lat] = location?.coordinates ?? [];
  if (lat === undefined || lng === undefined) {
    return '-';
  }
  return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
}

export default function StationsPage() {
  const [stations, setStations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [viewMode, setViewMode] = useState('table'); // 'table' | 'grid'

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingStation, setEditingStation] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');
  const [formKey, setFormKey] = useState(0);

  const [deactivatingId, setDeactivatingId] = useState(null);
  const [confirmingStation, setConfirmingStation] = useState(null);
  const [activatingId, setActivatingId] = useState(null);
  const [toast, setToast] = useState({ message: '', tone: 'error' });

  // Quick-glance summary row, derived from the already-loaded list
  const summary = useMemo(
    () => ({
      total: stations.length,
      active: stations.filter((s) => s.status === 'Active').length,
      inactive: stations.filter((s) => s.status !== 'Active').length,
      capacityKWh: stations.reduce((sum, s) => sum + (s.capacityKWh ?? 0), 0),
      slots: stations.reduce((sum, s) => sum + (s.batterySlots?.length ?? s.totalBatterySlots ?? 0), 0),
    }),
    [stations],
  );

  const maxCapacity = useMemo(
    () => Math.max(1, ...(stations.map((s) => s.capacityKWh ?? 0))),
    [stations],
  );

  // Filtered station list based on status tab
  const filteredStations = useMemo(() => {
    if (statusFilter === 'All') return stations;
    return stations.filter((s) => s.status === statusFilter);
  }, [stations, statusFilter]);

  // Loads the station list from the real API
  const loadStations = useCallback(async (term) => {
    setLoading(true);
    setLoadError('');
    try {
      const { data } = await api.get('/stations', { params: term ? { search: term } : undefined });
      setStations(data);
    } catch (err) {
      setLoadError(err.response?.data?.message || 'Failed to load stations.');
    } finally {
      setLoading(false);
    }
  }, []);

  // Re-fetches the list 300ms after the user stops typing
  useEffect(() => {
    const timeoutId = setTimeout(() => loadStations(search), 300);
    return () => clearTimeout(timeoutId);
  }, [search, loadStations]);

  // Opens the modal in "create" mode
  const handleOpenCreate = () => {
    setEditingStation(null);
    setFormError('');
    setFormKey((key) => key + 1);
    setIsFormOpen(true);
  };

  // Opens the modal in "edit" mode
  const handleOpenEdit = async (stationId) => {
    try {
      const { data } = await api.get(`/stations/${stationId}`);
      setEditingStation(data);
      setFormError('');
      setFormKey((key) => key + 1);
      setIsFormOpen(true);
    } catch (err) {
      setLoadError(err.response?.data?.message || 'Failed to load station details.');
    }
  };

  // Submits create/edit form
  const handleSubmit = async (values) => {
    setSubmitting(true);
    setFormError('');
    try {
      if (editingStation) {
        await api.put(`/stations/${editingStation.id}`, values);
        setToast({ message: 'Station updated successfully.', tone: 'success' });
      } else {
        await api.post('/stations', values);
        setToast({ message: 'New station created successfully.', tone: 'success' });
      }
      setIsFormOpen(false);
      await loadStations(search);
    } catch (err) {
      setFormError(err.response?.data?.message || 'Something went wrong. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRequestDeactivate = (station) => {
    setConfirmingStation(station);
  };

  const handleConfirmDeactivate = async () => {
    const station = confirmingStation;
    if (!station) return;
    setDeactivatingId(station.id);
    try {
      await api.put(`/stations/${station.id}/deactivate`);
      setToast({ message: 'Station deactivated.', tone: 'success' });
      setConfirmingStation(null);
      await loadStations(search);
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to deactivate station.', tone: 'error' });
      setConfirmingStation(null);
    } finally {
      setDeactivatingId(null);
    }
  };

  const handleActivate = async (station) => {
    setActivatingId(station.id);
    try {
      await api.put(`/stations/${station.id}/activate`);
      setToast({ message: 'Station reactivated.', tone: 'success' });
      await loadStations(search);
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to reactivate station.', tone: 'error' });
    } finally {
      setActivatingId(null);
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-6 py-8 space-y-8">

      {/* ── Page Header ───────────────────────────────── */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-line/60">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-ink">Microgrid Hubs</h1>
            <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
              {summary.total} nodes
            </span>
          </div>
          <p className="mt-1 text-[13px] text-muted">
            Solar microgrid stations, capacity specifications, and battery slot provisioning.
          </p>
        </div>
        <Button variant="primary" onClick={handleOpenCreate} className="shadow-card">
          <span className="flex items-center gap-1.5">
            <span>+</span>
            <span>New Station</span>
          </span>
        </Button>
      </div>

      {/* ── Summary Stat Bar ──────────────────────────── */}
      <div className="grid grid-cols-2 gap-3.5 sm:grid-cols-4">
        <StatCard
          icon="hubs"
          label="Total Hubs"
          value={loading ? '…' : summary.total}
          hint="Registered grid nodes"
        />
        <StatCard
          icon="pulse"
          label="Active Nodes"
          value={loading ? '…' : summary.active}
          hint={`${summary.inactive} currently inactive`}
        />
        <StatCard
          icon="battery"
          label="Total Capacity"
          value={loading ? '…' : `${summary.capacityKWh.toLocaleString()} kWh`}
          hint="Across all solar stations"
        />
        <StatCard
          icon="bolt"
          label="Battery Slots"
          value={loading ? '…' : summary.slots}
          hint="Total declared slots"
        />
      </div>

      {/* ── Controls Toolbar: Search, Filter Tabs & View Toggle ── */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 bg-surface p-2.5 rounded-xl border border-line shadow-card">
        {/* Search */}
        <div className="relative w-full sm:max-w-xs">
          <Input
            placeholder="Search by station name…"
            aria-label="Search stations"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            className="w-full pl-3 pr-8"
          />
          {search && (
            <button
              type="button"
              onClick={() => setSearch('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted hover:text-ink text-xs"
            >
              ✕
            </button>
          )}
        </div>

        {/* Status Filter Tabs & Layout Switcher */}
        <div className="flex items-center justify-between sm:justify-end gap-3 w-full sm:w-auto">
          {/* Status Tabs */}
          <div className="inline-flex rounded-lg bg-surface-alt p-1 border border-line text-xs font-medium">
            {['All', 'Active', 'Inactive'].map((tab) => (
              <button
                key={tab}
                type="button"
                onClick={() => setStatusFilter(tab)}
                className={`px-3 py-1 rounded-md transition-all ${
                  statusFilter === tab
                    ? 'bg-surface text-ink font-semibold shadow-xs'
                    : 'text-muted hover:text-body'
                }`}
              >
                {tab}
                <span className="ml-1.5 text-[10px] opacity-70">
                  {tab === 'All' ? summary.total : tab === 'Active' ? summary.active : summary.inactive}
                </span>
              </button>
            ))}
          </div>

          {/* View Toggle */}
          <div className="inline-flex rounded-lg bg-surface-alt p-1 border border-line text-xs">
            <button
              type="button"
              onClick={() => setViewMode('table')}
              title="Table view"
              className={`p-1.5 rounded-md transition-all ${
                viewMode === 'table' ? 'bg-surface text-primary shadow-xs' : 'text-muted hover:text-body'
              }`}
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>
            <button
              type="button"
              onClick={() => setViewMode('grid')}
              title="Grid card view"
              className={`p-1.5 rounded-md transition-all ${
                viewMode === 'grid' ? 'bg-surface text-primary shadow-xs' : 'text-muted hover:text-body'
              }`}
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      {loadError && (
        <div className="rounded-xl border border-error/30 bg-error-soft px-4 py-3 text-[13px] font-medium text-error flex items-center justify-between">
          <span>{loadError}</span>
          <button type="button" onClick={() => loadStations(search)} className="underline hover:no-underline">
            Retry
          </button>
        </div>
      )}

      {/* ── Main View: Table or Grid ──────────────────── */}
      {viewMode === 'table' ? (
        <div className="rounded-xl border border-line bg-surface shadow-card overflow-hidden">
          <Table>
            <thead>
              <tr className="bg-surface-alt/70 border-b border-line text-[11px] font-semibold uppercase tracking-wider text-muted">
                <Th>Hub Station</Th>
                <Th>GPS Coordinates</Th>
                <Th className="min-w-[170px]">Capacity Spec</Th>
                <Th className="text-right">Slots</Th>
                <Th>Status</Th>
                <Th className="text-right">Actions</Th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line/80">
              {loading && (
                <tr>
                  <Td colSpan={6} className="py-12 text-center text-[13px] text-muted">
                    Loading stations…
                  </Td>
                </tr>
              )}
              {!loading && filteredStations.length === 0 && (
                <tr>
                  <Td colSpan={6} className="py-12 text-center text-[13px] text-muted">
                    {search ? `No stations matching "${search}"` : 'No stations registered.'}
                  </Td>
                </tr>
              )}
              {!loading &&
                filteredStations.map((station) => {
                  const percent = Math.min(100, Math.round(((station.capacityKWh ?? 0) / maxCapacity) * 100));
                  return (
                    <tr key={station.id} className="transition-colors hover:bg-surface-alt/50">
                      <Td className="font-medium py-3.5">
                        <Link
                          to={`/stations/${station.id}`}
                          className="font-semibold text-ink hover:text-primary transition-colors block"
                        >
                          {station.name}
                        </Link>
                        <span className="text-[11px] text-muted font-mono">ID: {station.id?.slice(-8)}</span>
                      </Td>
                      <Td className="font-mono text-[12px] text-muted py-3.5">
                        <span className="inline-flex items-center gap-1.5 rounded bg-surface-alt px-2 py-0.5 border border-line text-[11px]">
                          <Icon name="mapPin" className="h-3 w-3 text-muted" />
                          <span>{formatCoordinates(station.location)}</span>
                        </span>
                      </Td>
                      <Td className="py-3.5">
                        <div className="flex items-center justify-between text-xs mb-1">
                          <span className="font-semibold text-ink tnum">{station.capacityKWh} kWh</span>
                          <span className="text-muted text-[10px]">{percent}%</span>
                        </div>
                        <div className="h-1.5 w-full rounded-full bg-surface-alt overflow-hidden">
                          <div className="h-full rounded-full bg-emerald-500" style={{ width: `${percent}%` }} />
                        </div>
                      </Td>
                      <Td className="tnum text-right py-3.5">
                        <span className="inline-flex items-center justify-center rounded-md bg-surface-alt px-2.5 py-0.5 text-xs text-body font-semibold">
                          {station.batterySlots?.length ?? station.totalBatterySlots ?? '-'}
                        </span>
                      </Td>
                      <Td className="py-3.5">
                        <span className={`inline-flex items-center rounded-md px-2.5 py-0.5 text-[11px] font-medium border ${
                          station.status === 'Active'
                            ? 'bg-emerald-50 text-emerald-700 border-emerald-200/80'
                            : 'bg-slate-100 text-slate-600 border-slate-200'
                        }`}>
                          {station.status}
                        </span>
                      </Td>
                      <Td className="text-right py-3.5">
                        <div className="flex justify-end items-center gap-1.5">
                          <Link
                            to={`/stations/${station.id}`}
                            className="inline-flex items-center rounded-md border border-line bg-surface px-2.5 py-1 text-xs font-medium text-ink hover:bg-surface-alt hover:text-primary transition-colors"
                          >
                            Details
                          </Link>
                          <Button variant="secondary" size="sm" onClick={() => handleOpenEdit(station.id)}>
                            Edit
                          </Button>
                          {station.status === 'Active' ? (
                            <Button
                              variant="danger-outline"
                              size="sm"
                              disabled={deactivatingId === station.id}
                              onClick={() => handleRequestDeactivate(station)}
                            >
                              {deactivatingId === station.id ? '…' : 'Deactivate'}
                            </Button>
                          ) : (
                            <Button
                              variant="secondary"
                              size="sm"
                              disabled={activatingId === station.id}
                              onClick={() => handleActivate(station)}
                            >
                              {activatingId === station.id ? '…' : 'Activate'}
                            </Button>
                          )}
                        </div>
                      </Td>
                    </tr>
                  );
                })}
            </tbody>
          </Table>
        </div>
      ) : (
        /* ── Grid Card View ── */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {loading && (
            <div className="col-span-full py-16 text-center text-muted text-sm">
              Loading station cards…
            </div>
          )}
          {!loading && filteredStations.length === 0 && (
            <div className="col-span-full py-16 text-center text-muted text-sm">
              {search ? `No stations matching "${search}"` : 'No stations registered.'}
            </div>
          )}
          {!loading &&
            filteredStations.map((station) => {
              const percent = Math.min(100, Math.round(((station.capacityKWh ?? 0) / maxCapacity) * 100));
              const slotsCount = station.batterySlots?.length ?? station.totalBatterySlots ?? 0;
              return (
                <div
                  key={station.id}
                  className="rounded-xl border border-line bg-surface p-5 shadow-card hover:shadow-panel transition-all flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <Link
                          to={`/stations/${station.id}`}
                          className="font-bold text-ink hover:text-primary transition-colors text-base"
                        >
                          {station.name}
                        </Link>
                        <p className="text-[11px] text-muted font-mono mt-0.5">ID: {station.id?.slice(-8)}</p>
                      </div>
                      <span className={`inline-flex items-center rounded-md px-2.5 py-0.5 text-[11px] font-medium border ${
                        station.status === 'Active'
                          ? 'bg-emerald-50 text-emerald-700 border-emerald-200/80'
                          : 'bg-slate-100 text-slate-600 border-slate-200'
                      }`}>
                        {station.status}
                      </span>
                    </div>

                    <div className="mt-4 pt-3 border-t border-line/60 space-y-2.5">
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-muted">Coordinates</span>
                        <span className="font-mono text-body font-medium">{formatCoordinates(station.location)}</span>
                      </div>
                      <div>
                        <div className="flex items-center justify-between text-xs mb-1">
                          <span className="text-muted">Capacity Spec</span>
                          <span className="font-bold text-ink tnum">{station.capacityKWh} kWh</span>
                        </div>
                        <div className="h-1.5 w-full rounded-full bg-surface-alt overflow-hidden">
                          <div className="h-full rounded-full bg-emerald-500" style={{ width: `${percent}%` }} />
                        </div>
                      </div>
                      <div className="flex items-center justify-between text-xs pt-1">
                        <span className="text-muted">Battery Slots</span>
                        <span className="inline-flex items-center rounded-md bg-surface-alt px-2 py-0.5 font-bold text-ink">
                          {slotsCount} slots
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="mt-5 pt-3 border-t border-line/60 flex items-center justify-between gap-2">
                    <Link
                      to={`/stations/${station.id}`}
                      className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
                    >
                      Manage Node →
                    </Link>
                    <div className="flex items-center gap-1.5">
                      <Button variant="secondary" size="sm" onClick={() => handleOpenEdit(station.id)}>
                        Edit
                      </Button>
                      {station.status === 'Active' ? (
                        <Button
                          variant="danger-outline"
                          size="sm"
                          disabled={deactivatingId === station.id}
                          onClick={() => handleRequestDeactivate(station)}
                        >
                          {deactivatingId === station.id ? '…' : 'Deactivate'}
                        </Button>
                      ) : (
                        <Button
                          variant="secondary"
                          size="sm"
                          disabled={activatingId === station.id}
                          onClick={() => handleActivate(station)}
                        >
                          {activatingId === station.id ? '…' : 'Activate'}
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
        </div>
      )}

      {/* ── Modals & Dialogs ─────────────────────────── */}
      <StationForm
        key={formKey}
        open={isFormOpen}
        station={editingStation}
        submitting={submitting}
        serverError={formError}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleSubmit}
      />

      <ConfirmDialog
        open={Boolean(confirmingStation)}
        title="Deactivate station node?"
        description={
          confirmingStation
            ? `"${confirmingStation.name}" will be marked inactive and taken offline for new reservations. Historical bookings will remain intact.`
            : ''
        }
        confirmLabel="Deactivate Node"
        tone="danger"
        confirming={deactivatingId === confirmingStation?.id}
        onConfirm={handleConfirmDeactivate}
        onClose={() => setConfirmingStation(null)}
      />

      <Toast message={toast.message} tone={toast.tone} onDismiss={() => setToast({ message: '', tone: 'error' })} />
    </div>
  );
}
