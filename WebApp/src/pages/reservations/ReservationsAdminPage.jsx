// ============================================================
// File: ReservationsAdminPage.jsx
// Purpose: Backoffice/GridOperator admin view of all energy
//          reservations. Supports read, create (on behalf of
//          a Prosumer), and cancel. Update is done on the
//          detail page. All business rules (7-day window,
//          12-hour notice, status transitions) enforced
//          server-side; this page renders API responses and
//          surfaces rejection messages verbatim.
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
  if (!iso) return "—";
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

  // Toast state — parent-owned per Shalon's Toast.jsx pattern.
  const [toast, setToast] = useState({ message: "", tone: "error" });

  // Filters mirror the query params GET /api/reservations accepts.
  const [filterStatus, setFilterStatus] = useState("");
  const [filterNic, setFilterNic] = useState("");
  const [filterStationId, setFilterStationId] = useState("");

  // Cancel confirmation modal state.
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelReason, setCancelReason] = useState("");
  const [cancelBusy, setCancelBusy] = useState(false);

  // Create modal state (operator-assisted booking).
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState(emptyCreateForm());
  const [createBusy, setCreateBusy] = useState(false);

  // Tiny helper to fire a toast without prop-drilling.
  function show(message, tone = "error") {
    setToast({ message, tone });
  }

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

  // Submits a create to the API. Enforces nothing client-side —
  // the server validates the 7-day window and slot availability.
  async function submitCreate() {
    setCreateBusy(true);
    try {
      // Convert datetime-local ("2026-10-05T14:00") to ISO.
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
      await load();
    } catch (e) {
      const msg =
        e?.response?.data?.message ||
        e?.response?.data?.code ||
        e.message ||
        "Create failed.";
      // Surface API rejection verbatim — e.g. "must be within 7 days".
      show(msg, "error");
    } finally {
      setCreateBusy(false);
    }
  }

  // Submits a cancel to the API. 12-hour rule rejections come back as 409.
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
      show(msg, "error");
    } finally {
      setCancelBusy(false);
    }
  }

  // Counts for the summary chips at the top.
  const counts = useMemo(
    () => ({
      total: reservations.length,
      confirmed: reservations.filter((r) => r.status === "Confirmed").length,
      completed: reservations.filter((r) => r.status === "Completed").length,
      cancelled: reservations.filter((r) => r.status === "Cancelled").length
    }),
    [reservations]
  );

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
          <Button
            variant="primary"
            onClick={() => {
              setCreateForm(emptyCreateForm());
              setShowCreate(true);
            }}
          >
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
            reservations.map((r) => (
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

      {/* Create modal — operator-assisted booking */}
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

          <Input
            label="Prosumer NIC *"
            placeholder="e.g. 199012345678"
            value={createForm.prosumerNic}
            onChange={(e) =>
              setCreateForm({ ...createForm, prosumerNic: e.target.value })
            }
          />
          <Input
            label="Station ID *"
            placeholder="Mongo ObjectId"
            value={createForm.stationId}
            onChange={(e) =>
              setCreateForm({ ...createForm, stationId: e.target.value })
            }
          />
          <Input
            label="Slot ID *"
            placeholder="Mongo ObjectId"
            value={createForm.slotId}
            onChange={(e) =>
              setCreateForm({ ...createForm, slotId: e.target.value })
            }
          />
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
              disabled={
                createBusy ||
                !createForm.prosumerNic.trim() ||
                !createForm.stationId.trim() ||
                !createForm.slotId.trim() ||
                !createForm.scheduledAt
              }
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
              The server enforces the 12-hour notice rule — a late cancel will
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
