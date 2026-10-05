// ============================================================
// File: DashboardPage.jsx
// Purpose: Backoffice & Grid Operator control dashboard.
//          Displays live system metrics, booking trends, hub capacities,
//          recent activity feed, and quick navigation modules.
//          All metrics derived from live backend API endpoints.
//          Design: Precision infrastructure aesthetic (stripe.design.md).
// Author: Shalon
// ============================================================
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar,
} from 'recharts';
import { useAuth } from '../../context/AuthContext.jsx';
import api from '../../services/api.js';
import Icon from '../../components/common/Icon.jsx';

// ── Helpers ───────────────────────────────────────────────────────────────────
function formatRelative(iso) {
  if (!iso) return '-';
  const diff = Date.now() - new Date(iso).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1) return 'Just now';
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}

function formatDate(iso) {
  if (!iso) return '-';
  try {
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  } catch (_) {
    return iso;
  }
}

const STATUS_CONFIG = {
  Confirmed: {
    bg: 'bg-amber-50 text-amber-700 border-amber-200/80',
    dot: 'bg-amber-500',
  },
  Completed: {
    bg: 'bg-emerald-50 text-emerald-700 border-emerald-200/80',
    dot: 'bg-emerald-500',
  },
  Cancelled: {
    bg: 'bg-rose-50 text-rose-700 border-rose-200/80',
    dot: 'bg-rose-500',
  },
  Active: {
    bg: 'bg-emerald-50 text-emerald-700 border-emerald-200/80',
    dot: 'bg-emerald-500',
  },
  Inactive: {
    bg: 'bg-slate-100 text-slate-600 border-slate-200',
    dot: 'bg-slate-400',
  },
};

// Build 7-day reservation volume trend from live reservations
function buildReservationTrend(reservations) {
  const days = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    days.push(d.toISOString().slice(0, 10));
  }
  const counts = {};
  days.forEach(d => { counts[d] = 0; });
  reservations.forEach(r => {
    const day = r.scheduledAt?.slice(0, 10);
    if (day && counts[day] !== undefined) counts[day]++;
  });
  return days.map(d => ({
    fullDate: d,
    day: new Date(d).toLocaleDateString(undefined, { weekday: 'short', month: 'numeric', day: 'numeric' }),
    shortDay: new Date(d).toLocaleDateString(undefined, { weekday: 'short' }),
    count: counts[d],
  }));
}

// Build per-station capacity chart data
function buildStationChart(stations) {
  return (stations || []).slice(0, 6).map(s => ({
    id: s.id,
    name: s.name || `Station ${s.id?.slice(-4)}`,
    shortName: s.name?.length > 12 ? `${s.name.slice(0, 10)}…` : (s.name || s.id?.slice(-4)),
    kWh: s.capacityKWh ?? 0,
    slots: s.batterySlots?.length ?? s.totalBatterySlots ?? 0,
  }));
}

// ── Custom Chart Tooltip ──────────────────────────────────────────────────────
function CustomChartTooltip({ active, payload, label, unit = 'Bookings' }) {
  if (!active || !payload || !payload.length) return null;
  const data = payload[0];
  return (
    <div className="rounded-lg border border-line bg-surface/95 px-3 py-2 text-xs shadow-panel backdrop-blur-sm">
      <p className="font-semibold text-ink">{label || data.payload?.name || data.payload?.day}</p>
      <div className="mt-1 flex items-center gap-1.5 text-body">
        <span className="h-2 w-2 rounded-full" style={{ backgroundColor: data.color || '#635bff' }} />
        <span>{unit}:</span>
        <span className="font-bold text-ink tnum">{data.value?.toLocaleString()}</span>
      </div>
    </div>
  );
}

// ── Refined Subcomponents ─────────────────────────────────────────────────────
function PrimaryKpiCard({ label, value, hint, icon, accent = 'primary', loading }) {
  const accentStyles = {
    primary: 'bg-primary/10 text-primary border-primary/20',
    emerald: 'bg-emerald-500/10 text-emerald-600 border-emerald-500/20',
    amber: 'bg-amber-500/10 text-amber-600 border-amber-500/20',
    cyan: 'bg-sky-500/10 text-sky-600 border-sky-500/20',
  };

  return (
    <div className="group relative overflow-hidden rounded-xl border border-line bg-surface p-5 shadow-card transition-all duration-200 hover:shadow-panel hover:border-line-strong">
      <div className="flex items-center justify-between">
        <span className="text-[12px] font-medium tracking-wider text-muted uppercase">{label}</span>
        <span className={`flex h-9 w-9 items-center justify-center rounded-lg border transition-transform duration-200 group-hover:scale-105 ${accentStyles[accent] || accentStyles.primary}`}>
          <Icon name={icon} className="h-4.5 w-4.5" />
        </span>
      </div>
      <div className="mt-4">
        <p className="tnum text-3xl font-bold tracking-tight text-ink">
          {loading ? '…' : (value ?? '-')}
        </p>
        {hint && (
          <p className="mt-1.5 text-[12px] text-muted flex items-center gap-1">
            {hint}
          </p>
        )}
      </div>
    </div>
  );
}

function SecondaryKpiStrip({ label, value, icon, hint, loading }) {
  return (
    <div className="flex items-center gap-3.5 rounded-lg border border-line bg-surface px-4 py-3 shadow-card">
      <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-surface-alt text-body">
        <Icon name={icon} className="h-4 w-4" />
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-[11px] font-medium uppercase tracking-wider text-muted truncate">{label}</p>
        <p className="tnum text-lg font-bold text-ink leading-tight">
          {loading ? '…' : (value ?? '-')}
        </p>
      </div>
      {hint && <span className="text-[11px] text-muted shrink-0">{hint}</span>}
    </div>
  );
}

function SectionHeader({ title, subtitle, badge, action }) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-4">
      <div>
        <div className="flex items-center gap-2">
          <h2 className="text-[15px] font-semibold tracking-tight text-ink">{title}</h2>
          {badge && (
            <span className="inline-flex items-center rounded-full bg-surface-alt px-2 py-0.5 text-[11px] font-medium text-muted border border-line">
              {badge}
            </span>
          )}
        </div>
        {subtitle && <p className="mt-0.5 text-[12px] text-muted">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

function ActivityItem({ reservation }) {
  const status = reservation.status || 'Confirmed';
  const conf = STATUS_CONFIG[status] || STATUS_CONFIG.Confirmed;

  return (
    <li className="flex items-center gap-3.5 py-3 border-b border-line/80 last:border-0 hover:bg-surface-alt/40 px-2 rounded-lg transition-colors">
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary-soft text-primary">
        <Icon name="calendar" className="h-4 w-4" />
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <p className="text-[13px] font-semibold text-ink truncate">NIC {reservation.prosumerNic}</p>
          <span className="text-[11px] text-muted">·</span>
          <span className="text-[11px] text-muted truncate">Station {reservation.stationId?.slice(-6) ?? '-'}</span>
        </div>
        <p className="text-[11px] text-muted mt-0.5">{formatDate(reservation.scheduledAt)}</p>
      </div>
      <div className="flex flex-col items-end gap-1 shrink-0">
        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-md text-[11px] font-medium border ${conf.bg}`}>
          {status}
        </span>
        <span className="text-[10px] text-muted">{formatRelative(reservation.scheduledAt)}</span>
      </div>
    </li>
  );
}

function QuickActionCard({ label, description, icon, to, color = 'bg-primary-soft text-primary' }) {
  return (
    <Link
      to={to}
      className="group flex items-start justify-between rounded-xl border border-line bg-surface p-4 shadow-card hover:border-primary/40 hover:bg-surface-alt transition-all duration-200"
    >
      <div className="flex items-start gap-3.5">
        <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl transition-transform duration-200 group-hover:scale-105 ${color}`}>
          <Icon name={icon} className="h-5 w-5" />
        </span>
        <div>
          <p className="text-[13px] font-semibold text-ink group-hover:text-primary transition-colors flex items-center gap-1">
            {label}
          </p>
          <p className="text-[12px] text-muted mt-0.5 leading-snug">{description}</p>
        </div>
      </div>
      <span className="text-muted group-hover:text-primary group-hover:translate-x-0.5 transition-all text-sm font-semibold mt-1">
        →
      </span>
    </Link>
  );
}

// ── Main Page Component ───────────────────────────────────────────────────────
export default function DashboardPage() {
  const { fullName, role } = useAuth();
  const isBackoffice = role === 'Backoffice';

  const [stations, setStations] = useState(null);
  const [reservations, setReservations] = useState(null);
  const [users, setUsers] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchAll() {
      try {
        const [stRes, rvRes] = await Promise.all([
          api.get('/stations'),
          api.get('/reservations'),
        ]);
        setStations(stRes.data ?? []);
        setReservations(rvRes.data ?? []);

        if (isBackoffice) {
          const uRes = await api.get('/users');
          setUsers(uRes.data ?? []);
        }
      } catch (_) {
        setStations(prev => prev ?? []);
        setReservations(prev => prev ?? []);
      } finally {
        setLoading(false);
      }
    }
    fetchAll();
  }, [isBackoffice]);

  // Derived KPIs
  const activeStations = useMemo(() => stations?.filter(s => s.status === 'Active').length ?? 0, [stations]);
  const totalCapacity = useMemo(() => stations?.reduce((acc, s) => acc + (s.capacityKWh ?? 0), 0) ?? 0, [stations]);
  const maxStationCapacity = useMemo(() => Math.max(1, ...(stations?.map(s => s.capacityKWh ?? 0) ?? [1])), [stations]);
  const confirmedRes = useMemo(() => reservations?.filter(r => r.status === 'Confirmed').length ?? 0, [reservations]);
  const completedRes = useMemo(() => reservations?.filter(r => r.status === 'Completed').length ?? 0, [reservations]);
  const cancelledRes = useMemo(() => reservations?.filter(r => r.status === 'Cancelled').length ?? 0, [reservations]);
  const totalSlots = useMemo(() => stations?.reduce((acc, s) => acc + (s.batterySlots?.length ?? s.totalBatterySlots ?? 0), 0) ?? 0, [stations]);

  // Chart data
  const reservationTrend = useMemo(() => reservations ? buildReservationTrend(reservations) : [], [reservations]);
  const stationChart = useMemo(() => stations ? buildStationChart(stations) : [], [stations]);

  const recentActivity = useMemo(() =>
    reservations?.slice().sort((a, b) => new Date(b.scheduledAt) - new Date(a.scheduledAt)).slice(0, 7) ?? [],
    [reservations]
  );

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  const firstName = fullName?.split(' ')[0] ?? 'there';

  return (
    <div className="mx-auto max-w-6xl px-6 py-8 space-y-8">

      {/* ── Welcome & Status Header ───────────────────── */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pb-2 border-b border-line/60">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-ink">
              {greeting}, {firstName}
            </h1>
          </div>
          <p className="mt-1 text-[13px] text-muted flex items-center gap-2">
            <span>{new Date().toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</span>
            <span>·</span>
            <span className="font-semibold text-body bg-surface-alt px-2 py-0.5 rounded border border-line text-[11px]">{role}</span>
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link
            to="/stations"
            className="inline-flex items-center gap-1.5 rounded-lg border border-line bg-surface px-3.5 py-2 text-[13px] font-medium text-ink shadow-card hover:bg-surface-alt transition-colors"
          >
            <Icon name="hubs" className="h-4 w-4 text-primary" />
            <span>Manage Hubs</span>
          </Link>
          <Link
            to="/reservations"
            className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3.5 py-2 text-[13px] font-medium text-on-primary shadow-card hover:bg-primary-hover transition-colors"
          >
            <Icon name="calendar" className="h-4 w-4" />
            <span>All Bookings</span>
          </Link>
        </div>
      </div>

      {/* ── Primary KPI Grid (4 Hero Cards) ───────────── */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <PrimaryKpiCard
          label="Active Hubs"
          value={activeStations}
          hint={`${activeStations} of ${stations?.length ?? 0} operational nodes`}
          icon="hubs"
          accent="primary"
          loading={loading}
        />
        <PrimaryKpiCard
          label="Total Capacity"
          value={`${totalCapacity.toLocaleString()} kWh`}
          hint="Across all solar stations"
          icon="battery"
          accent="emerald"
          loading={loading}
        />
        <PrimaryKpiCard
          label="Confirmed Bookings"
          value={confirmedRes}
          hint="Awaiting slot check-in"
          icon="calendar"
          accent="amber"
          loading={loading}
        />
        <PrimaryKpiCard
          label="Completed Transfers"
          value={completedRes}
          hint="Successfully dispatched energy"
          icon="bolt"
          accent="cyan"
          loading={loading}
        />
      </div>

      {/* ── Secondary Metrics Strip (For Backoffice) ──── */}
      {isBackoffice && (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <SecondaryKpiStrip
            label="Total Battery Slots"
            value={totalSlots}
            icon="pulse"
            hint="Provisioned"
            loading={loading}
          />
          <SecondaryKpiStrip
            label="Staff Accounts"
            value={users?.length ?? '-'}
            icon="user"
            hint="Operators & Admins"
            loading={loading}
          />
          <SecondaryKpiStrip
            label="Cancelled Bookings"
            value={cancelledRes}
            icon="check"
            hint="Total period"
            loading={loading}
          />
        </div>
      )}

      {/* ── Interactive Charts Row ────────────────────── */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Booking Trend Chart */}
        <div className="rounded-xl border border-line bg-surface p-5 shadow-card">
          <SectionHeader
            title="Booking Volume"
            subtitle="Reservations scheduled each day"
            badge="Last 7 Days"
          />
          <div className="h-52 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={reservationTrend} margin={{ top: 8, right: 8, left: -24, bottom: 0 }}>
                <defs>
                  <linearGradient id="bookingGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#635bff" stopOpacity={0.25} />
                    <stop offset="100%" stopColor="#635bff" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                <XAxis dataKey="shortDay" tick={{ fontSize: 11, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 11, fill: '#64748b' }} axisLine={false} tickLine={false} allowDecimals={false} />
                <Tooltip content={<CustomChartTooltip unit="Reservations" />} />
                <Area
                  type="monotone"
                  dataKey="count"
                  stroke="#635bff"
                  strokeWidth={2.5}
                  fill="url(#bookingGrad)"
                  activeDot={{ r: 5, fill: '#635bff', stroke: '#ffffff', strokeWidth: 2 }}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Station Capacity Chart */}
        <div className="rounded-xl border border-line bg-surface p-5 shadow-card">
          <SectionHeader
            title="Station Capacity"
            subtitle="Declared kWh per solar microgrid hub"
            badge="Top Hubs"
          />
          <div className="h-52 w-full">
            {loading ? (
              <div className="flex h-full items-center justify-center text-muted text-xs">Loading station metrics…</div>
            ) : stationChart.length === 0 ? (
              <div className="flex h-full items-center justify-center text-muted text-xs">No registered stations</div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={stationChart} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                  <XAxis dataKey="shortName" tick={{ fontSize: 10, fill: '#64748b' }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: '#64748b' }} axisLine={false} tickLine={false} />
                  <Tooltip content={<CustomChartTooltip unit="Capacity (kWh)" />} />
                  <Bar dataKey="kWh" fill="#059669" radius={[4, 4, 0, 0]} maxBarSize={36} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      {/* ── Activity Feed & Quick Actions ─────────────── */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Left 2 Cols: Activity Feed */}
        <div className="lg:col-span-2 rounded-xl border border-line bg-surface p-5 shadow-card flex flex-col justify-between">
          <div>
            <SectionHeader
              title="Recent Reservations"
              subtitle="Live reservation events across grid nodes"
              action={
                <Link to="/reservations" className="text-[12px] font-semibold text-primary hover:text-primary-hover flex items-center gap-1 transition-colors">
                  View all →
                </Link>
              }
            />
            {loading ? (
              <div className="flex items-center justify-center h-40 text-muted text-xs">Loading activity feed…</div>
            ) : recentActivity.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-40 text-center py-6">
                <span className="flex h-10 w-10 items-center justify-center rounded-full bg-surface-alt text-muted mb-2">
                  <Icon name="calendar" className="h-5 w-5" />
                </span>
                <p className="text-[13px] font-medium text-ink">No reservations recorded yet</p>
                <p className="text-[12px] text-muted mt-0.5">New bookings will appear here in real time.</p>
              </div>
            ) : (
              <ul className="space-y-0.5">
                {recentActivity.slice(0, 5).map(r => (
                  <ActivityItem key={r.id} reservation={r} />
                ))}
              </ul>
            )}
          </div>

          {!loading && (reservations?.length ?? 0) > 0 && (
            <div className="pt-3 mt-3 border-t border-line/60 flex items-center justify-between text-xs">
              <span className="text-muted">
                Showing recent <span className="font-semibold text-ink">{Math.min(5, reservations.length)}</span> of{' '}
                <span className="font-semibold text-ink">{reservations.length}</span> total reservations
              </span>
              <Link
                to="/reservations"
                className="inline-flex items-center gap-1 font-semibold text-primary hover:text-primary-hover transition-colors"
              >
                <span>View in Reservations Page</span>
                <span>→</span>
              </Link>
            </div>
          )}
        </div>

        {/* Right 1 Col: Quick Navigation Modules */}
        <div className="space-y-3.5">
          <SectionHeader title="Quick Actions" subtitle="Frequently accessed workflows" />
          <QuickActionCard
            label="Microgrid Hubs"
            description="Manage solar grid nodes, specs & battery slots"
            icon="hubs"
            to="/stations"
            color="bg-primary-soft text-primary"
          />
          <QuickActionCard
            label="Reservation Log"
            description="Inspect all energy charging bookings"
            icon="calendar"
            to="/reservations"
            color="bg-sky-50 text-sky-600"
          />
          {isBackoffice && (
            <QuickActionCard
              label="Staff Directory"
              description="Provision Operator and Backoffice accounts"
              icon="user"
              to="/admin/users"
              color="bg-emerald-50 text-emerald-600"
            />
          )}
        </div>
      </div>

      {/* ── Station Overview Table ────────────────────── */}
      <div className="rounded-xl border border-line bg-surface shadow-card overflow-hidden">
        <div className="px-5 py-4 border-b border-line flex items-center justify-between bg-surface">
          <div>
            <h2 className="text-[15px] font-semibold tracking-tight text-ink">Microgrid Stations Overview</h2>
            <p className="text-[12px] text-muted mt-0.5">Summary of capacity and slot allocations per node</p>
          </div>
          <Link
            to="/stations"
            className="text-[12px] font-semibold text-primary hover:text-primary-hover transition-colors"
          >
            Manage all stations →
          </Link>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-[13px]">
            <thead>
              <tr className="bg-surface-alt/70 border-b border-line text-[11px] font-semibold uppercase tracking-wider text-muted">
                <th className="px-5 py-3">Station Node</th>
                <th className="px-5 py-3">Capacity Utilization</th>
                <th className="px-5 py-3 text-right">Slots</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line/80">
              {loading && (
                <tr><td colSpan={5} className="px-5 py-8 text-center text-muted text-xs">Loading station data…</td></tr>
              )}
              {!loading && (!stations || stations.length === 0) && (
                <tr><td colSpan={5} className="px-5 py-8 text-center text-muted text-xs">No stations registered yet.</td></tr>
              )}
              {!loading && stations?.slice(0, 5).map(s => {
                const percent = Math.min(100, Math.round(((s.capacityKWh ?? 0) / maxStationCapacity) * 100));
                return (
                  <tr key={s.id} className="hover:bg-surface-alt/50 transition-colors">
                    <td className="px-5 py-3.5">
                      <div className="font-semibold text-ink">{s.name}</div>
                      <div className="text-[11px] text-muted font-mono">ID: {s.id?.slice(-8)}</div>
                    </td>
                    <td className="px-5 py-3.5 min-w-[180px]">
                      <div className="flex items-center justify-between text-[12px] mb-1">
                        <span className="font-semibold text-ink tnum">{(s.capacityKWh ?? 0).toLocaleString()} kWh</span>
                        <span className="text-muted text-[11px]">{percent}%</span>
                      </div>
                      <div className="h-1.5 w-full rounded-full bg-surface-alt overflow-hidden">
                        <div className="h-full rounded-full bg-emerald-500" style={{ width: `${percent}%` }} />
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-right font-medium text-ink tnum">
                      <span className="inline-flex items-center justify-center rounded-md bg-surface-alt px-2 py-0.5 text-xs text-body font-semibold">
                        {s.batterySlots?.length ?? s.totalBatterySlots ?? 0}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className={`inline-flex items-center rounded-md px-2.5 py-0.5 text-[11px] font-medium border ${
                        s.status === 'Active'
                          ? 'bg-emerald-50 text-emerald-700 border-emerald-200/80'
                          : 'bg-slate-100 text-slate-600 border-slate-200'
                      }`}>
                        {s.status}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <Link
                        to={`/stations/${s.id}`}
                        className="inline-flex items-center gap-1 text-[12px] font-medium text-primary hover:underline"
                      >
                        Details
                      </Link>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {!loading && (stations?.length ?? 0) > 0 && (
          <div className="px-5 py-3 border-t border-line/60 bg-surface-alt/30 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs">
            <span className="text-muted">
              Displaying <span className="font-semibold text-ink">{Math.min(5, stations.length)}</span> of{' '}
              <span className="font-semibold text-ink">{stations.length}</span> registered microgrid nodes
            </span>
            <Link
              to="/stations"
              className="inline-flex items-center gap-1.5 font-semibold text-primary hover:text-primary-hover transition-colors"
            >
              <span>View in Microgrid Hubs Page</span>
              <span>→</span>
            </Link>
          </div>
        )}
      </div>

    </div>
  );
}
