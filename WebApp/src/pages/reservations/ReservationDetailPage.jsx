// ============================================================
// File: ReservationDetailPage.jsx
// Purpose: Single reservation view for Backoffice/GridOperator.
//          Shows lifecycle timestamps, QR token (as text), and
//          offers Edit + Cancel for Confirmed reservations.
//          Server enforces 7-day and 12-hour rules; rejections
//          are surfaced verbatim.
//
//          On load, fetches the Prosumer's profile via
//          GET /api/prosumers/{nic} so the operator sees the
//          name and email — not just the NIC.
//
//          The Edit modal loads Stations and (for the selected
//          station) its available Slots as dropdowns. The
//          reservation's own current slot is included in the
//          list even if its status isn't Available, so the
//          operator can leave it unchanged.
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

// Converts an ISO string to a value usable by <input type="datetime-local">.
function toLocalDateTimeInput(iso) {
  if (!iso) return "";
  const d = new Date(iso);
  const pad = (n) => String(n).padStart(2, "0");
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}`
  );
}

// Minimum allowed scheduled time: now + 15 minutes.
function minScheduledAt() {
  const d = new Date(Date.now() + 15 * 60 * 1000);
  const pad = (n) => String(n).padStart(2, "0");
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}`
  );
}

// Maximum allowed scheduled time: now + 7 days (matches server window).
function maxScheduledAt() {
  const d = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
  const pad = (n) => String(n).padStart(2, "0");
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}`
  );
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
  const [prosumer, setProsumer] = useState(null); // Fetched by NIC
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Parent-owned toast state.
  const [toast, setToast] = useState({ message: "", tone: "error" });

  const [showCancel, setShowCancel] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [cancelBusy, setCancelBusy] = useState(false);

  // Edit modal state.
  const [showEdit, setShowEdit] = useState(false);
  const [editForm, setEditForm] = useState(null);
  const [editBusy, setEditBusy] = useState(false);

  // Dropdown data for the Edit modal.
  const [stations, setStations] = useState([]);
  const [slots, setSlots] = useState([]);
  const [dropdownsLoading, setDropdownsLoading] = useState({
    stations: false,
    slots: false
  });
  const [dropdownsError, setDropdownsError] = useState(null);

  function show(message, tone = "error") {
    setToast({ message, tone });
  }

  // -----------------------
  // Data load
  // -----------------------

  async function load() {
    setLoading(true);
    setError(null);
    setProsumer(null);
    try {
      const { data } = await api.get(`/reservations/${id}`);
      setReservation(data);

      if (data?.prosumerNic) {
        try {
          const p = await api.get(`/prosumers/${data.prosumerNic}`);
          setProsumer(p.data);
        } catch {
          setProsumer(null);
        }
      }
    } catch (e) {
      const msg =
        e?.response?.data?.message ||
        (e?.response?.status === 404 ? "Reservation not found." : e.message);
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  // Initial load — every setState runs after an await.
  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        const { data } = await api.get(`/reservations/${id}`);
        if (cancelled) return;
        setReservation(data);

        if (data?.prosumerNic) {
          try {
            const p = await api.get(`/prosumers/${data.prosumerNic}`);
            if (!cancelled) setProsumer(p.data);
          } catch {
            if (!cancelled) setProsumer(null);
          }
        }
      } catch (e) {
        if (cancelled) return;
        const msg =
          e?.response?.data?.message ||
          (e?.response?.status === 404 ? "Reservation not found." : e.message);
        setError(msg);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  // -----------------------
  // Dropdown loaders
  // -----------------------

  // Load Stations once (Active only).
  useEffect(() => {
    let cancelled = false;

    (async () => {
      setDropdownsLoading((s) => ({ ...s, stations: true }));
      try {
        const { data } = await api.get("/stations", {
          params: { status: "Active" }
        });
        if (!cancelled) setStations(Array.isArray(data) ? data : []);
      } catch (e) {
        if (!cancelled) {
          setDropdownsError(
            e?.response?.data?.message || "Could not load Stations."
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

  // When edit modal is open and station is chosen, load that station's slots.
  // Include the reservation's own current slot even if it's not Available,
  // so opening the modal and saving without changes never errors out.
  useEffect(() => {
    if (!showEdit || !editForm?.stationId) {
      setSlots([]);
      return;
    }

    let cancelled = false;
    (async () => {
      setDropdownsLoading((s) => ({ ...s, slots: true }));
      try {
        const { data } = await api.get(`/stations/${editForm.stationId}`);
        const all = Array.isArray(data?.slots) ? data.slots : [];

        // Include Available slots + the reservation's current slot
        // (which may be Reserved because this very reservation owns it).
        const currentSlotId = reservation?.slotId;
        const visible = all.filter(
          (s) => s.status === "Available" || s.id === currentSlotId
        );

        if (!cancelled) setSlots(visible);
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
  }, [showEdit, editForm?.stationId]);

  // -----------------------
  // Edit
  // -----------------------

  function openEdit() {
    setEditForm({
      stationId: reservation.stationId || "",
      slotId: reservation.slotId || "",
      scheduledAt: toLocalDateTimeInput(reservation.scheduledAt)
    });
    setSlots([]);
    setShowEdit(true);
  }

  async function submitEdit() {
    setEditBusy(true);
    try {
      const payload = {
        stationId: editForm.stationId.trim(),
        slotId: editForm.slotId.trim(),
        scheduledAt: editForm.scheduledAt
          ? new Date(editForm.scheduledAt).toISOString()
          : null
      };
      await api.put(`/reservations/${id}`, payload);
      show("Reservation updated.", "success");
      setShowEdit(false);
      await load();
    } catch (e) {
      const msg =
        e?.response?.data?.message ||
        e?.response?.data?.code ||
        e.message ||
        "Update failed.";
      show(msg, "error");
    } finally {
      setEditBusy(false);
    }
  }

  // -----------------------
  // Cancel
  // -----------------------

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

  // -----------------------
  // Render
  // -----------------------

  if (loading) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-8 text-[13px] text-muted">
        Loading reservation…
      </div>
    );
  }

  if (error || !reservation) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-8">
        <p className="mb-4 rounded-md border border-error/30 bg-error-soft px-3 py-2 text-[13px] font-medium text-error">
          {error || "Reservation not found."}
        </p>
        <Button variant="secondary" onClick={() => navigate("/reservations")}>
          ← Back to reservations
        </Button>
      </div>
    );
  }

  const r = reservation;
  const isCancellable = r.status === "Confirmed";
  const isEditable = r.status === "Confirmed";

  const canSave =
    editForm &&
    !editBusy &&
    editForm.stationId &&
    editForm.slotId &&
    editForm.scheduledAt;

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
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
          <h1 className="text-xl font-semibold tracking-tight text-ink">
            Reservation{" "}
            <span className="font-mono text-lg">{r.id.slice(-8)}</span>
          </h1>
          <p className="text-sm text-muted mt-1">
            Full lifecycle detail. Edit and cancel are available while the
            reservation is Confirmed.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge tone={statusTone(r.status)}>{r.status}</Badge>
          <Button variant="secondary" onClick={load}>
            Refresh
          </Button>
          {isEditable && (
            <Button variant="primary" onClick={openEdit}>
              Edit
            </Button>
          )}
          {isCancellable && (
            <Button variant="danger" onClick={() => setShowCancel(true)}>
              Cancel reservation
            </Button>
          )}
        </div>
      </div>

      {/* Detail grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Prosumer card */}
        <DetailCard title="Prosumer">
          <DetailRow label="NIC" value={r.prosumerNic} mono />
          {prosumer ? (
            <>
              <DetailRow label="Name" value={prosumer.fullName} />
              <DetailRow label="Email" value={prosumer.email} />
              {prosumer.phone && (
                <DetailRow label="Phone" value={prosumer.phone} />
              )}
              <DetailRow
                label="Account status"
                value={<Badge tone="neutral">{prosumer.status}</Badge>}
              />
            </>
          ) : (
            <p className="text-xs text-muted pt-1">
              Profile not available (may be pending activation, deactivated, or
              lookup restricted).
            </p>
          )}
        </DetailCard>

        <DetailCard title="Booking">
          <DetailRow label="Station ID" value={r.stationId} mono />
          <DetailRow label="Slot ID" value={r.slotId} mono />
          <DetailRow
            label="Scheduled at"
            value={
              <span className="tnum">{formatDateTime(r.scheduledAt)}</span>
            }
          />
        </DetailCard>

        <DetailCard title="Lifecycle" className="md:col-span-2">
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

      {/* Edit modal */}
      <Modal
        open={showEdit}
        onClose={() => !editBusy && setShowEdit(false)}
        title="Edit reservation"
      >
        {editForm && (
          <div className="space-y-4">
            <p className="text-sm text-body">
              Update the reservation details. The server enforces the 7-day
              window and the 12-hour modification notice.
            </p>

            {dropdownsError && (
              <p className="rounded-md border border-error/30 bg-error-soft px-3 py-2 text-xs font-medium text-error">
                {dropdownsError}
              </p>
            )}

            {/* Prosumer — read-only (ownership is not editable) */}
            <div className="rounded-md border border-line bg-surface-alt px-3 py-2">
              <p className="text-xs text-muted">Prosumer (not editable)</p>
              <p className="text-sm font-medium text-ink">
                {prosumer
                  ? `${prosumer.fullName} — ${r.prosumerNic}`
                  : r.prosumerNic}
              </p>
            </div>

            {/* Station dropdown — clears slot when it changes */}
            <Input
              label="Station *"
              as="select"
              value={editForm.stationId}
              onChange={(e) => {
                setEditForm({
                  ...editForm,
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

            {/* Slot dropdown — includes the current slot even if not Available */}
            <Input
              label="Slot *"
              as="select"
              value={editForm.slotId}
              onChange={(e) =>
                setEditForm({ ...editForm, slotId: e.target.value })
              }
              disabled={
                !editForm.stationId ||
                dropdownsLoading.slots ||
                slots.length === 0
              }
            >
              <option value="">
                {!editForm.stationId
                  ? "Pick a station first"
                  : dropdownsLoading.slots
                    ? "Loading slots…"
                    : slots.length === 0
                      ? "No available slots at this station"
                      : "Select a slot…"}
              </option>
              {slots.map((s) => (
                <option key={s.id} value={s.id}>
                  #{s.slotNumber} — {s.type} — {s.capacityKWh} kWh
                  {s.id === r.slotId ? " (current)" : ""}
                </option>
              ))}
            </Input>

            {/* Scheduled at — bounded to the 7-day window */}
            <Input
              label="Scheduled at *"
              type="datetime-local"
              value={editForm.scheduledAt}
              min={minScheduledAt()}
              max={maxScheduledAt()}
              onChange={(e) =>
                setEditForm({ ...editForm, scheduledAt: e.target.value })
              }
            />

            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="secondary"
                onClick={() => setShowEdit(false)}
                disabled={editBusy}
              >
                Discard changes
              </Button>
              <Button
                variant="primary"
                onClick={submitEdit}
                disabled={!canSave}
              >
                {editBusy ? "Saving…" : "Save changes"}
              </Button>
            </div>
          </div>
        )}
      </Modal>

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
              variant="secondary"
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

      {/* Toast */}
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
        className={`text-sm text-ink text-right break-all ${
          mono ? "font-mono text-xs" : ""
        }`}
      >
        {value}
      </span>
    </div>
  );
}
