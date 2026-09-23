// ============================================================
// File: ReservationsAdminPage.jsx
// Purpose: Backoffice/GridOperator admin view of all energy
//          reservations. Read + cancel only — creation and edit
//          are Prosumer mobile actions per architecture.md §3.
//          All business rules (12-hour notice, status transitions)
//          enforced server-side; this page renders what the API
//          returns and surfaces rejection messages verbatim.
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
      // Surface the API's rejection message verbatim — a rubric expectation.
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

  // Column definitions for the shared Table component.
  // const columns = useMemo(
  //   () => [
  //     {
  //       key: "id",
  //       header: "ID",
  //       render: (r) => (
  //         <Link
  //           to={`/reservations/${r.id}`}
  //           className="font-mono text-xs text-primary hover:underline"
  //         >
  //           {r.id.slice(-8)}
  //         </Link>
  //       )
  //     },
  //     { key: "prosumerNic", header: "Prosumer NIC" },
  //     {
  //       key: "scheduledAt",
  //       header: "Scheduled",
  //       render: (r) => (
  //         <span className="tnum">{formatDateTime(r.scheduledAt)}</span>
  //       )
  //     },
  //     {
  //       key: "status",
  //       header: "Status",
  //       render: (r) => <Badge tone={statusTone(r.status)}>{r.status}</Badge>
  //     },
  //     {
  //       key: "actions",
  //       header: "",
  //       render: (r) => (
  //         <div className="flex items-center gap-2 justify-end">
  //           <Button
  //             variant="ghost"
  //             size="sm"
  //             onClick={() => navigate(`/reservations/${r.id}`)}
  //           >
  //             View
  //           </Button>
  //           {r.status === "Confirmed" && (
  //             <Button
  //               variant="danger"
  //               size="sm"
  //               onClick={() => {
  //                 setCancelTarget(r);
  //                 setCancelReason("");
  //               }}
  //             >
  //               Cancel
  //             </Button>
  //           )}
  //         </div>
  //       )
  //     }
  //   ],
  //   [navigate]
  // );

  return (
    <div className="p-6 md:p-8">
      {/* Page header */}
      <div className="flex flex-wrap items-start justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-semibold text-ink">Reservations</h1>
          <p className="text-sm text-muted mt-1">
            All energy slot reservations across microgrid nodes. Read-only
            oversight — creation and edits happen on the Prosumer mobile app.
          </p>
        </div>
        <Button variant="ghost" onClick={load} disabled={loading}>
          {loading ? "Refreshing…" : "Refresh"}
        </Button>
      </div>

      {/* Summary chips */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
        <SummaryChip label="Total" value={counts.total} tone="neutral" />
        <SummaryChip
          label="Confirmed"
          value={counts.confirmed}
          tone="warning"
        />
        <SummaryChip
          label="Completed"
          value={counts.completed}
          tone="success"
        />
        <SummaryChip
          label="Cancelled"
          value={counts.cancelled}
          tone="neutral"
        />
      </div>

      {/* Filters */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-4">
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
        <div className="mb-4 rounded-md border border-line bg-error-soft text-error px-4 py-3 text-sm">
          {error}
        </div>
      )}

      {/* Table — Shalon's shared shell, rendered with named exports */}
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
                variant="ghost"
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
      {/* Toast — Shalon's component, parent-owned state */}
      <Toast
        message={toast.message}
        tone={toast.tone}
        onDismiss={() => setToast({ message: "", tone: "error" })}
      />
    </div>
  );
}

// Small reusable summary chip for the counts row.
function SummaryChip({ label, value, tone }) {
  const toneClass =
    {
      neutral: "bg-surface-alt text-ink",
      warning: "bg-warning-soft text-warning",
      success: "bg-success-soft text-success",
      error: "bg-error-soft text-error"
    }[tone] || "bg-surface-alt text-ink";

  return (
    <div className="rounded-lg border border-line bg-surface p-4">
      <div className="text-xs uppercase tracking-wide text-muted">{label}</div>
      <div
        className={`mt-1 inline-block rounded-md px-2 py-0.5 text-lg font-semibold tnum ${toneClass}`}
      >
        {value}
      </div>
    </div>
  );
}
