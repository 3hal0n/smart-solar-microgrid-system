// ============================================================
// File: ReservationDetailPage.jsx
// Purpose: Single reservation view for Backoffice/GridOperator.
//          Shows lifecycle timestamps, QR token (as text — the
//          web app never renders or scans the QR image, that's
//          a mobile action), and offers Cancel for Confirmed
//          reservations. No business rules computed here.
// Author: Dinil
// ============================================================
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import api from "../../services/api.js";
import Button from "../../components/common/Button.jsx";
import Badge from "../../components/common/Badge.jsx";
import Input from "../../components/common/Input.jsx";
import Modal from "../../components/common/Modal.jsx";
import Toast from "../../components/common/Toast.jsx";

// Formats an ISO timestamp for display, or an em dash for null.
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

export default function ReservationDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [reservation, setReservation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Parent-owned toast state, matching Shalon's Toast.jsx pattern.
  const [toast, setToast] = useState({ message: "", tone: "error" });

  const [showCancel, setShowCancel] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [cancelBusy, setCancelBusy] = useState(false);

  // Small helper so call sites stay readable: show(msg, tone).
  function show(message, tone = "error") {
    setToast({ message, tone });
  }

  // Fetches the reservation by ID.
  async function load() {
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get(`/reservations/${id}`);
      setReservation(data);
    } catch (e) {
      const msg =
        e?.response?.data?.message ||
        (e?.response?.status === 404 ? "Reservation not found." : e.message);
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    let mounted = true;

    async function fetchData() {
      setLoading(true);
      setError(null);
      try {
        const { data } = await api.get(`/reservations/${id}`);
        if (mounted) setReservation(data);
      } catch (e) {
        const msg =
          e?.response?.data?.message ||
          (e?.response?.status === 404 ? "Reservation not found." : e.message);
        if (mounted) setError(msg);
      } finally {
        if (mounted) setLoading(false);
      }
    }

    fetchData();
    return () => {
      mounted = false;
    };
  }, [id]);

  // Cancels the reservation. The 12-hour rejection surfaces verbatim.
  async function confirmCancel() {
    setCancelBusy(true);
    try {
      await api.put(`/reservations/${id}/cancel`, {
        reason: cancelReason || null
      });
      show("Reservation cancelled.", "success");
      setShowCancel(false);
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

  if (loading) {
    return <div className="p-8 text-muted text-sm">Loading reservation…</div>;
  }

  if (error || !reservation) {
    return (
      <div className="p-8">
        <div className="rounded-md border border-line bg-error-soft text-error px-4 py-3 text-sm mb-4">
          {error || "Reservation not found."}
        </div>
        <Button variant="ghost" onClick={() => navigate("/reservations")}>
          ← Back to reservations
        </Button>
      </div>
    );
  }

  const r = reservation;
  const isCancellable = r.status === "Confirmed";

  return (
    <div className="p-6 md:p-8">
      {/* Breadcrumb */}
      <div className="text-sm text-muted mb-4">
        <Link to="/reservations" className="hover:underline">
          Reservations
        </Link>
        <span className="mx-2">/</span>
        <span className="font-mono text-ink">{r.id.slice(-8)}</span>
      </div>

      {/* Header */}
      <div className="flex flex-wrap items-start justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-semibold text-ink">
            Reservation{" "}
            <span className="font-mono text-lg">{r.id.slice(-8)}</span>
          </h1>
          <p className="text-sm text-muted mt-1">
            Full lifecycle detail. Read-only — cancel is the only write action
            available here.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge tone={statusTone(r.status)}>{r.status}</Badge>
          <Button variant="ghost" onClick={load}>
            Refresh
          </Button>
          {isCancellable && (
            <Button variant="danger" onClick={() => setShowCancel(true)}>
              Cancel reservation
            </Button>
          )}
        </div>
      </div>

      {/* Detail grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <DetailCard title="Booking">
          <DetailRow label="Prosumer NIC" value={r.prosumerNic} mono />
          <DetailRow label="Station ID" value={r.stationId} mono />
          <DetailRow label="Slot ID" value={r.slotId} mono />
          <DetailRow
            label="Scheduled at"
            value={
              <span className="tnum">{formatDateTime(r.scheduledAt)}</span>
            }
          />
        </DetailCard>

        <DetailCard title="Lifecycle">
          <DetailRow
            label="Created"
            value={<span className="tnum">{formatDateTime(r.createdAt)}</span>}
          />
          <DetailRow
            label="Completed"
            value={
              <span className="tnum">{formatDateTime(r.completedAt)}</span>
            }
          />
          <DetailRow label="Cancel reason" value={r.cancelReason || "—"} />
        </DetailCard>

        <DetailCard title="QR token" className="md:col-span-2">
          <p className="text-xs text-muted mb-2">
            The token is issued at creation and verified by a Grid Operator's
            mobile scan. The web app never renders or scans it — this is a text
            view only.
          </p>
          <div className="rounded-md border border-line bg-surface-alt p-3">
            <code className="text-xs font-mono break-all text-ink">
              {r.qrToken || "—"}
            </code>
          </div>
        </DetailCard>
      </div>

      {/* Cancel modal */}
      <Modal
        open={showCancel}
        onClose={() => !cancelBusy && setShowCancel(false)}
        title="Cancel reservation"
      >
        <div className="space-y-4">
          <p className="text-sm text-body">
            Cancelling this reservation will free the slot. The server enforces
            a 12-hour notice rule — a late cancel returns a rejection message
            that will be shown here verbatim.
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
              onClick={() => setShowCancel(false)}
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
      </Modal>

      {/* Toast — parent-owned state */}
      <Toast
        message={toast.message}
        tone={toast.tone}
        onDismiss={() => setToast({ message: "", tone: "error" })}
      />
    </div>
  );
}

// Small helper card for grouped detail fields.
function DetailCard({ title, children, className = "" }) {
  return (
    <div
      className={`rounded-lg border border-line bg-surface p-5 ${className}`}
    >
      <h2 className="text-sm font-semibold text-ink uppercase tracking-wide mb-3">
        {title}
      </h2>
      <div className="space-y-3">{children}</div>
    </div>
  );
}

function DetailRow({ label, value, mono = false }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <span className="text-sm text-muted">{label}</span>
      <span
        className={`text-sm text-ink text-right break-all ${mono ? "font-mono text-xs" : ""}`}
      >
        {value}
      </span>
    </div>
  );
}
