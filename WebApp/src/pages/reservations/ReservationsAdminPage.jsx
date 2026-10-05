// ============================================================
// File: ReservationsAdminPage.jsx
// Purpose: Backoffice/GridOperator admin view of all energy
//          reservations. Supports read, create (on behalf of
//          a Prosumer), and cancel. Update is done on the
//          detail page. All business rules (7-day window,
//          12-hour notice, status transitions) enforced
//          server-side; this page renders API responses and
//          surfaces rejection messages verbatim.
//
//          The Create modal pulls Prosumers, Stations, and
//          that station's Slots from the API and renders them
//          as dropdowns, so operators pick instead of typing
//          raw ObjectIds.
// Author: Dinil
// ============================================================
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../../services/api.js";
import Button from "../../components/common/Button.jsx";
import Badge from "../../components/common/Badge.jsx";
import Input from "../../components/common/Input.jsx";
import { Table, Th, Td } from "../../components/common/Table.jsx";
import Modal from "../../components/common/Modal.jsx";
import Toast from "../../components/common/Toast.jsx";
import StatCard from "../../components/common/StatCard.jsx";

// Formats an ISO timestamp for display (local time, minute precision).
function formatDateTime(iso) {
  if (!iso) return "-";
  const d = new Date(iso);
  return d.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

// Maps a reservation status string to a Badge tone.
function statusTone(status) {
  switch (status) {
    case "Confirmed":
      return "warning";
    case "Completed":
      return "success";
    case "Cancelled":
      return "neutral";
    default:
      return "neutral";
  }
}

// Builds the empty form state for the create modal.
function emptyCreateForm() {
  return {
    prosumerNic: "",
    stationId: "",
    slotId: "",
    scheduledAt: ""
  };
}

export default function ReservationsAdminPage() {
  const navigate = useNavigate();

  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Toast state - parent-owned per Shalon's Toast.jsx pattern.
  const [toast, setToast] = useState({ message: "", tone: "error" });

  // Filters mirror the query params GET /api/reservations accepts.
  const [filterStatus, setFilterStatus] = useState("");
  const [filterNic, setFilterNic] = useState("");
  const [filterStationId, setFilterStationId] = useState("");

  // Cancel confirmation modal state.
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelReason, setCancelReason] = useState("");
  const [cancelBusy, setCancelBusy] = useState(false);

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(8);

  // Create modal state (operator-assisted booking).
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState(emptyCreateForm());
  const [createBusy, setCreateBusy] = useState(false);

  // Dropdown data for the create modal.
  // prosumers: [{ nic, fullName, status }]
  // stations:  [{ id, name, status }]
  // slots:     [{ id, slotNumber, type, capacityKWh, status }]  ← filtered by selected station
  const [prosumers, setProsumers] = useState([]);
  const [stations, setStations] = useState([]);
  const [slots, setSlots] = useState([]);
  const [dropdownsLoading, setDropdownsLoading] = useState({
    prosumers: false,
    stations: false,
    slots: false
  });
  const [dropdownsError, setDropdownsError] = useState(null);

  // Tiny helper to fire a toast without prop-drilling.
  function show(message, tone = "error") {
    setToast({ message, tone });
  }

  // ---------------------------------------------------------------
  // List load
  // ---------------------------------------------------------------

  // Loads the reservation list using the current filters.
  async function load() {
    setLoading(true);
    setError(null);
    try {
      const params = {};
      if (filterStatus) params.status = filterStatus;
      if (filterNic) params.nic = filterNic;
      if (filterStationId) params.stationId = filterStationId;

      const { data } = await api.get("/reservations", { params });
      setReservations(data);
    } catch (e) {
      const msg =
        e?.response?.data?.message ||
        e.message ||
        "Failed to load reservations.";
      setError(msg);
      show(msg, "error");
    } finally {
      setLoading(false);
    }
  }

  // Reload whenever filters change (debounced for typed fields).
  useEffect(() => {
    const t = setTimeout(load, 250);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterStatus, filterNic, filterStationId]);

  // ---------------------------------------------------------------
  // Dropdown loaders
  // ---------------------------------------------------------------

  // Loads Prosumers + Stations once when the page first mounts.
  // Kept separate from the reservation list so a dropdown failure
  // never blocks the main table.
  useEffect(() => {
    let cancelled = false;

    (async () => {
      setDropdownsLoading((s) => ({ ...s, prosumers: true, stations: true }));
      setDropdownsError(null);

      // Prosumers - only Active ones make sense for a new booking.
      try {
        const { data } = await api.get("/prosumers", {
          params: { status: "Active" }
        });
        if (!cancelled) setProsumers(Array.isArray(data) ? data : []);
      } catch (e) {
        if (!cancelled) {
          setDropdownsError(
            e?.response?.data?.message || "Could not load Prosumers list."
          );
        }
      } finally {
        if (!cancelled)
          setDropdownsLoading((s) => ({ ...s, prosumers: false }));
      }

      // Stations - Active only.
      try {
        const { data } = await api.get("/stations", {
          params: { status: "Active" }
        });
        if (!cancelled) setStations(Array.isArray(data) ? data : []);
      } catch (e) {
        if (!cancelled) {
          setDropdownsError(
            (prev) =>
              prev ||
              e?.response?.data?.message ||
              "Could not load Stations list."
          );
        }
      } finally {
        if (!cancelled) setDropdownsLoading((s) => ({ ...s, stations: false }));
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  // Whenever the selected station changes, load that station's slots.
  useEffect(() => {
    const stationId = createForm.stationId;
    if (!stationId) {
      setSlots([]);
      return;
    }

    let cancelled = false;
    (async () => {
      setDropdownsLoading((s) => ({ ...s, slots: true }));
      try {
        const { data } = await api.get(`/stations/${stationId}`);
        // StationDetailResponse shape - slots array inside.
        const list = Array.isArray(data?.slots) ? data.slots : [];
        if (!cancelled) {
          // Only show Available slots - Reserved/Inactive would fail the
          // server-side check anyway.
          setSlots(list.filter((s) => s.status === "Available"));
        }
      } catch (e) {
        if (!cancelled) {
          setSlots([]);
          show(
            e?.response?.data?.message || "Could not load slots for station.",
            "error"
          );
        }
      } finally {
        if (!cancelled) setDropdownsLoading((s) => ({ ...s, slots: false }));
      }
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [createForm.stationId]);

  // ---------------------------------------------------------------
  // Create
  // ---------------------------------------------------------------

  // Resets the create modal to a clean slate.
  function openCreate() {
    setCreateForm(emptyCreateForm());
    setSlots([]);
    setShowCreate(true);
  }

  // Submits a create to the API. Enforces nothing client-side -
  // the server validates the 7-day window and slot availability.
  async function submitCreate() {
    setCreateBusy(true);
    try {
      const payload = {
        prosumerNic: createForm.prosumerNic.trim(),
        stationId: createForm.stationId.trim(),
        slotId: createForm.slotId.trim(),
        scheduledAt: createForm.scheduledAt
          ? new Date(createForm.scheduledAt).toISOString()
          : null
      };

      await api.post("/reservations", payload);
      show("Reservation created.", "success");
      setShowCreate(false);
      setCreateForm(emptyCreateForm());
      setSlots([]);
      await load();
    } catch (e) {
      const msg =
        e?.response?.data?.message ||
        e?.response?.data?.code ||
        e.message ||
        "Create failed.";
      show(msg, "error");
    } finally {
      setCreateBusy(false);
    }
  }

  // ---------------------------------------------------------------
  // Cancel
  // ---------------------------------------------------------------

  async function confirmCancel() {
    if (!cancelTarget) return;
    setCancelBusy(true);
    try {
      await api.put(`/reservations/${cancelTarget.id}/cancel`, {
        reason: cancelReason || null
      });
      show("Reservation cancelled.", "success");
      setCancelTarget(null);
      setCancelReason("");
      await load();
    } catch (e) {
      const msg =
        e?.response?.data?.message ||
        e?.response?.data?.code ||
        e.message ||
        "Cancel failed.";
      // Surface the API's rejection message verbatim - a rubric expectation.
      show(msg, "error");
    } finally {
      setCancelBusy(false);
    }
  }

  // ---------------------------------------------------------------
  // Derived values
  // ---------------------------------------------------------------

  const counts = useMemo(
    () => ({
      total: reservations.length,
      confirmed: reservations.filter((r) => r.status === "Confirmed").length,
      completed: reservations.filter((r) => r.status === "Completed").length,
      cancelled: reservations.filter((r) => r.status === "Cancelled").length
    }),
    [reservations]
  );

  const totalPages = useMemo(
    () => Math.max(1, Math.ceil(reservations.length / pageSize)),
    [reservations, pageSize]
  );

  const paginatedReservations = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return reservations.slice(start, start + pageSize);
  }, [reservations, currentPage, pageSize]);

  // Reset to page 1 on filter or page size change
  useEffect(() => {
    setCurrentPage(1);
  }, [filterStatus, filterNic, filterStationId, pageSize]);

  const canCreate =
    !createBusy &&
    createForm.prosumerNic &&
    createForm.stationId &&
    createForm.slotId &&
    createForm.scheduledAt;

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      {/* Page header */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight text-ink">
            Reservations
          </h1>
          <p className="mt-1 text-[13px] text-muted">
            All energy slot reservations across microgrid nodes. Operators may
            create bookings on behalf of Prosumers (phone-in or walk-in).
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="primary" onClick={openCreate}>
            + New reservation
          </Button>
          <Button variant="secondary" onClick={load} disabled={loading}>
            {loading ? "Refreshing…" : "Refresh"}
          </Button>
        </div>
      </div>

      {/* Summary row */}
      <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard
          icon="calendar"
          label="Total"
          value={counts.total}
          hint="All reservations"
        />
        <StatCard
          icon="pulse"
          label="Confirmed"
          value={counts.confirmed}
          hint="Awaiting check-in"
        />
        <StatCard
          icon="check"
          label="Completed"
          value={counts.completed}
          hint="Transfer finished"
        />
        <StatCard
          icon="bolt"
          label="Cancelled"
          value={counts.cancelled}
          hint="Slot released"
        />
      </div>

      {/* Filters */}
      <div className="mb-4 grid grid-cols-1 gap-3 md:grid-cols-3">
        <Input
          label="Status"
          as="select"
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value)}
        >
          <option value="">All statuses</option>
          <option value="Confirmed">Confirmed</option>
          <option value="Completed">Completed</option>
          <option value="Cancelled">Cancelled</option>
        </Input>
        <Input
          label="Prosumer NIC"
          placeholder="e.g. 199012345678"
          value={filterNic}
          onChange={(e) => setFilterNic(e.target.value)}
        />
        <Input
          label="Station ID"
          placeholder="Mongo ObjectId"
          value={filterStationId}
          onChange={(e) => setFilterStationId(e.target.value)}
        />
      </div>

      {/* Error state */}
      {error && (
        <p className="mb-4 rounded-md border border-error/30 bg-error-soft px-3 py-2 text-[13px] font-medium text-error">
          {error}
        </p>
      )}

      {/* Table */}
      <Table>
        <thead>
          <tr>
            <Th>ID</Th>
            <Th>Prosumer NIC</Th>
            <Th>Scheduled</Th>
            <Th>Status</Th>
            <Th className="text-right">Actions</Th>
          </tr>
        </thead>
        <tbody>
          {loading && (
            <tr>
              <Td colSpan={5} className="text-center text-muted">
                Loading…
              </Td>
            </tr>
          )}
          {!loading && reservations.length === 0 && (
            <tr>
              <Td colSpan={5} className="text-center text-muted">
                No reservations match the current filters.
              </Td>
            </tr>
          )}
          {!loading &&
            paginatedReservations.map((r) => (
              <tr key={r.id}>
                <Td>
                  <Link
                    to={`/reservations/${r.id}`}
                    className="font-mono text-xs text-primary hover:underline"
                  >
                    {r.id.slice(-8)}
                  </Link>
                </Td>
                <Td>{r.prosumerNic}</Td>
                <Td className="tnum">{formatDateTime(r.scheduledAt)}</Td>
                <Td>
                  <Badge tone={statusTone(r.status)}>{r.status}</Badge>
                </Td>
                <Td className="text-right">
                  <div className="flex items-center gap-2 justify-end">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => navigate(`/reservations/${r.id}`)}
                    >
                      View
                    </Button>
                    {r.status === "Confirmed" && (
                      <Button
                        variant="danger"
                        size="sm"
                        onClick={() => {
                          setCancelTarget(r);
                          setCancelReason("");
                        }}
                      >
                        Cancel
                      </Button>
                    )}
                  </div>
                </Td>
              </tr>
            ))}
        </tbody>
      </Table>

      {/* Pagination Controls */}
      {!loading && reservations.length > 0 && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t border-line/60 mt-4">
          <div className="flex items-center gap-3 text-xs text-muted">
            <span>
              Showing <span className="font-semibold text-ink">{(currentPage - 1) * pageSize + 1}</span> to{' '}
              <span className="font-semibold text-ink">
                {Math.min(currentPage * pageSize, reservations.length)}
              </span>{' '}
              of <span className="font-semibold text-ink">{reservations.length}</span> reservations
            </span>
            <span className="text-line">|</span>
            <div className="flex items-center gap-1.5">
              <span>Per page:</span>
              <select
                aria-label="Reservations per page"
                value={pageSize}
                onChange={(e) => setPageSize(Number(e.target.value))}
                className="rounded-md border border-line bg-surface px-2 py-1 text-xs font-medium text-ink focus:outline-none focus:ring-1 focus:ring-primary"
              >
                <option value={8}>8</option>
                <option value={16}>16</option>
                <option value={32}>32</option>
              </select>
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="px-2.5"
            >
              ← Prev
            </Button>
            <div className="flex items-center gap-1">
              {Array.from({ length: totalPages }, (_, i) => i + 1).map((pageNum) => (
                <button
                  key={pageNum}
                  type="button"
                  onClick={() => setCurrentPage(pageNum)}
                  className={`min-w-[32px] h-8 rounded-md text-xs font-semibold transition-all ${
                    currentPage === pageNum
                      ? 'bg-primary text-white shadow-xs'
                      : 'border border-line bg-surface text-ink hover:bg-surface-alt'
                  }`}
                >
                  {pageNum}
                </button>
              ))}
            </div>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
              className="px-2.5"
            >
              Next →
            </Button>
          </div>
        </div>
      )}

      {/* Create modal - dropdowns for Prosumer / Station / Slot */}
      <Modal
        open={showCreate}
        onClose={() => !createBusy && setShowCreate(false)}
        title="New reservation"
      >
        <div className="space-y-4">
          <p className="text-xs text-muted">
            Create a reservation on behalf of a Prosumer. The server enforces
            the 7-day booking window and slot availability.
          </p>

          {dropdownsError && (
            <p className="rounded-md border border-error/30 bg-error-soft px-3 py-2 text-xs font-medium text-error">
              {dropdownsError}
            </p>
          )}

          {/* Prosumer dropdown */}
          <Input
            label="Prosumer *"
            as="select"
            value={createForm.prosumerNic}
            onChange={(e) => {
              // Changing prosumer does not reset station/slot.
              setCreateForm({ ...createForm, prosumerNic: e.target.value });
            }}
            disabled={dropdownsLoading.prosumers}
          >
            <option value="">
              {dropdownsLoading.prosumers
                ? "Loading prosumers…"
                : prosumers.length === 0
                  ? "No active prosumers"
                  : "Select a prosumer…"}
            </option>
            {prosumers.map((p) => (
              <option key={p.nic} value={p.nic}>
                {p.fullName} - {p.nic}
              </option>
            ))}
          </Input>

          {/* Station dropdown - clears slot when it changes */}
          <Input
            label="Station *"
            as="select"
            value={createForm.stationId}
            onChange={(e) => {
              setCreateForm({
                ...createForm,
                stationId: e.target.value,
                slotId: "" // reset slot; new list loads
              });
              setSlots([]);
            }}
            disabled={dropdownsLoading.stations}
          >
            <option value="">
              {dropdownsLoading.stations
                ? "Loading stations…"
                : stations.length === 0
                  ? "No active stations"
                  : "Select a station…"}
            </option>
            {stations.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </Input>

          {/* Slot dropdown - disabled until a station is chosen */}
          <Input
            label="Slot *"
            as="select"
            value={createForm.slotId}
            onChange={(e) =>
              setCreateForm({ ...createForm, slotId: e.target.value })
            }
            disabled={
              !createForm.stationId ||
              dropdownsLoading.slots ||
              slots.length === 0
            }
          >
            <option value="">
              {!createForm.stationId
                ? "Pick a station first"
                : dropdownsLoading.slots
                  ? "Loading slots…"
                  : slots.length === 0
                    ? "No available slots at this station"
                    : "Select a slot…"}
            </option>
            {slots.map((s) => (
              <option key={s.id} value={s.id}>
                #{s.slotNumber} - {s.type} - {s.capacityKWh} kWh
              </option>
            ))}
          </Input>

          {/* Scheduled at - free text (datetime-local) */}
          <Input
            label="Scheduled at *"
            type="datetime-local"
            value={createForm.scheduledAt}
            onChange={(e) =>
              setCreateForm({ ...createForm, scheduledAt: e.target.value })
            }
          />

          <div className="flex justify-end gap-2 pt-2">
            <Button
              variant="secondary"
              onClick={() => setShowCreate(false)}
              disabled={createBusy}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={submitCreate}
              disabled={!canCreate}
            >
              {createBusy ? "Creating…" : "Create reservation"}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Cancel confirmation modal */}
      <Modal
        open={!!cancelTarget}
        onClose={() => !cancelBusy && setCancelTarget(null)}
        title="Cancel reservation"
      >
        {cancelTarget && (
          <div className="space-y-4">
            <p className="text-sm text-body">
              Are you sure you want to cancel reservation{" "}
              <span className="font-mono text-ink">
                {cancelTarget.id.slice(-8)}
              </span>{" "}
              for NIC{" "}
              <span className="font-semibold">{cancelTarget.prosumerNic}</span>?
            </p>
            <p className="text-xs text-muted">
              The server enforces the 12-hour notice rule - a late cancel will
              be rejected and the reason shown below.
            </p>
            <Input
              label="Reason (optional)"
              placeholder="e.g. Prosumer requested via phone"
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
            />
            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="secondary"
                onClick={() => setCancelTarget(null)}
                disabled={cancelBusy}
              >
                Keep reservation
              </Button>
              <Button
                variant="danger"
                onClick={confirmCancel}
                disabled={cancelBusy}
              >
                {cancelBusy ? "Cancelling…" : "Confirm cancel"}
              </Button>
            </div>
          </div>
        )}
      </Modal>
      {/* Toast */}
      <Toast
        message={toast.message}
        tone={toast.tone}
        onDismiss={() => setToast({ message: "", tone: "error" })}
      />
    </div>
  );
}
