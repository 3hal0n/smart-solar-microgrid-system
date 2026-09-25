// ============================================================
// File: StationsPage.jsx
// Purpose: Microgrid hub management screen — search/list stations
//          and launch the create/edit modal, per architecture.md §6
//          "Web — Microgrid Hubs". Calls the real Stations API and
//          renders its data/errors as-is; no business rules live
//          here, per the FAT service pattern.
// Author: Shalon
// ============================================================
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../services/api.js';
import { Table, Th, Td } from '../../components/common/Table.jsx';
import Button from '../../components/common/Button.jsx';
import Input from '../../components/common/Input.jsx';
import Badge from '../../components/common/Badge.jsx';
import Toast from '../../components/common/Toast.jsx';
import StationForm from './StationForm.jsx';

// Formats a GeoJSON [lng, lat] coordinate pair as a fixed-width "lat, lng" string.
function formatCoordinates(location) {
  const [lng, lat] = location?.coordinates ?? [];
  if (lat === undefined || lng === undefined) {
    return '—';
  }
  return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
}

export default function StationsPage() {
  const [stations, setStations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [search, setSearch] = useState('');

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingStation, setEditingStation] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');
  // Bumped every time the modal opens so StationForm remounts fresh instead of needing an
  // effect to reset its fields (see StationForm.jsx).
  const [formKey, setFormKey] = useState(0);

  // Loads the station list from the real API, optionally filtered by the current search term.
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

  // Re-fetches the list 300ms after the user stops typing in the search box.
  useEffect(() => {
    const timeoutId = setTimeout(() => loadStations(search), 300);
    return () => clearTimeout(timeoutId);
  }, [search, loadStations]);

  // Opens the modal in "create" mode.
  const handleOpenCreate = () => {
    setEditingStation(null);
    setFormError('');
    setFormKey((key) => key + 1);
    setIsFormOpen(true);
  };

  // Loads the full station detail (operatingSchedule/totalBatterySlots aren't in the list row)
  // and opens the modal in "edit" mode.
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

  // Submits the create/edit form to the real endpoint and reloads the list on success.
  const handleSubmit = async (values) => {
    setSubmitting(true);
    setFormError('');
    try {
      if (editingStation) {
        await api.put(`/stations/${editingStation.id}`, values);
      } else {
        await api.post('/stations', values);
      }
      setIsFormOpen(false);
      await loadStations(search);
    } catch (err) {
      setFormError(err.response?.data?.message || 'Something went wrong. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight text-ink">Microgrid hubs</h1>
          <p className="mt-1 text-[13px] text-muted">
            Solar grid nodes, their capacity specs and battery slot availability.
          </p>
        </div>
        <Button variant="primary" onClick={handleOpenCreate}>
          New station
        </Button>
      </div>

      <div className="mb-4 max-w-xs">
        <Input
          placeholder="Search by station name"
          aria-label="Search stations"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>

      {loadError && (
        <p className="mb-4 rounded-md border border-error/30 bg-error-soft px-3 py-2 text-[13px] font-medium text-error">
          {loadError}
        </p>
      )}

      <Table>
        <thead>
          <tr>
            <Th>Name</Th>
            <Th>Coordinates</Th>
            <Th className="text-right">Capacity</Th>
            <Th className="text-right">Slots</Th>
            <Th>Status</Th>
            <Th className="text-right">Actions</Th>
          </tr>
        </thead>
        <tbody>
          {loading && (
            <tr>
              <Td colSpan={6} className="py-8 text-center text-[13px] text-muted">
                Loading stations…
              </Td>
            </tr>
          )}
          {!loading && stations.length === 0 && (
            <tr>
              <Td colSpan={6} className="py-8 text-center text-[13px] text-muted">
                No stations found.
              </Td>
            </tr>
          )}
          {!loading &&
            stations.map((station) => (
              <tr key={station.id} className="transition-colors hover:bg-surface-alt/60">
                <Td className="font-medium">
                  <Link to={`/stations/${station.id}`} className="text-ink hover:text-primary">
                    {station.name}
                  </Link>
                </Td>
                <Td className="font-mono text-[12px] text-muted">{formatCoordinates(station.location)}</Td>
                <Td className="tnum text-right">
                  {station.capacityKWh}
                  <span className="ml-1 text-[12px] text-muted">kWh</span>
                </Td>
                <Td className="tnum text-right">{station.totalBatterySlots ?? '—'}</Td>
                <Td>
                  <Badge tone={station.status === 'Active' ? 'success' : 'neutral'}>{station.status}</Badge>
                </Td>
                <Td className="text-right">
                  <Button variant="secondary" size="sm" onClick={() => handleOpenEdit(station.id)}>
                    Edit
                  </Button>
                </Td>
              </tr>
            ))}
        </tbody>
      </Table>

      <StationForm
        key={formKey}
        open={isFormOpen}
        station={editingStation}
        submitting={submitting}
        serverError={formError}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleSubmit}
      />
    </div>
  );
}
