// ============================================================
// File: DashboardPage.jsx
// Purpose: Main landing page after login — shows live summary stats
//          across stations, reservations and users with a mini activity
//          feed and quick-action links. All data comes from real API
//          calls. Adapts to role: Backoffice sees everything;
//          GridOperator sees reservations + stations only.
//          Design: Stripe design system (stripe.design.md).
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
  if (!iso) return '—';
  const diff = Date.now() - new Date(iso).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1) return 'just now';
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}

function formatDate(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

const STATUS_STYLES = {
  Confirmed: 'bg-amber-50 text-amber-700 border border-amber-200',
  Completed: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
  Cancelled: 'bg-red-50 text-red-700 border border-red-200',
};

// Build a 7-day reservation-count trend from real reservation data
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
    day: new Date(d).toLocaleDateString(undefined, { weekday: 'short' }),
    count: counts[d],
  }));
}

// Build a per-station capacity bar from real station data
function buildStationChart(stations) {
  return stations.slice(0, 6).map(s => ({
    name: s.name?.split(' ')[0] ?? s.id.slice(-4),
    kWh: s.capacityKWh ?? 0,
  }));
}

// ── Sub-components ────────────────────────────────────────────────────────────
function KpiCard({ label, value, hint, icon }) {
  return (
    <div className="rounded-xl border border-line bg-surface p-5 shadow-card flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <span className="text-[12px] font-medium uppercase tracking-wide text-muted">{label}</span>
        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/8 text-primary">
          <Icon name={icon} className="h-4 w-4" />
        </span>
      </div>
      <div>
        <p className="tnum text-2xl font-bold tracking-tight text-ink">{value ?? '—'}</p>
        {hint && <p className="mt-0.5 text-[12px] text-muted">{hint}</p>}
      </div>
    </div>
  );
}

function SectionHeader({ title, subtitle, action }) {
  return (
    <div className="flex items-start justify-between mb-4">
      <div>
        <h2 className="text-[15px] font-semibold tracking-tight text-ink">{title}</h2>
        {subtitle && <p className="mt-0.5 text-[12px] text-muted">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

function ActivityItem({ reservation }) {
  const tone = reservation.status;
  return (
    <li className="flex items-center gap-3 py-2.5 border-b border-line last:border-0">
      <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
        <Icon name="calendar" className="h-3.5 w-3.5" />
      </span>
      <div className="flex-1 min-w-0">
        <p className="text-[13px] text-ink font-medium truncate">NIC {reservation.prosumerNic}</p>
        <p className="text-[11px] text-muted truncate">Station {reservation.stationId?.slice(-8) ?? '—'}</p>
      </div>
      <div className="flex flex-col items-end gap-1 shrink-0">
        <span className={`inline-flex px-2 py-0.5 rounded text-[10px] font-semibold ${STATUS_STYLES[tone] ?? 'bg-slate-100 text-slate-600 border border-slate-200'}`}>
          {reservation.status}
        </span>
        <span className="text-[10px] text-muted">{formatRelative(reservation.scheduledAt)}</span>
      </div>
    </li>
  );
}

function QuickAction({ label, description, icon, to, color = 'bg-primary/10 text-primary' }) {
  return (
    <Link
      to={to}
      className="flex items-start gap-3 rounded-xl border border-line bg-surface p-4 shadow-card hover:bg-surface-alt transition-colors group"
    >
      <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${color}`}>
        <Icon name={icon} className="h-4 w-4" />
      </span>
      <div>
        <p className="text-[13px] font-semibold text-ink group-hover:text-primary transition-colors">{label}</p>
        <p className="text-[11px] text-muted mt-0.5">{description}</p>
      </div>
    </Link>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────
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

  // Derived KPIs from real data
  const activeStations = useMemo(() => stations?.filter(s => s.status === 'Active').length ?? 0, [stations]);
  const totalCapacity = useMemo(() => stations?.reduce((acc, s) => acc + (s.capacityKWh ?? 0), 0) ?? 0, [stations]);
  const confirmedRes = useMemo(() => reservations?.filter(r => r.status === 'Confirmed').length ?? 0, [reservations]);
  const completedRes = useMemo(() => reservations?.filter(r => r.status === 'Completed').length ?? 0, [reservations]);
  const cancelledRes = useMemo(() => reservations?.filter(r => r.status === 'Cancelled').length ?? 0, [reservations]);
  const totalSlots = useMemo(() => stations?.reduce((acc, s) => acc + (s.batterySlots?.length ?? 0), 0) ?? 0, [stations]);

  // Real chart data derived from fetched data
  const reservationTrend = useMemo(() => reservations ? buildReservationTrend(reservations) : [], [reservations]);
  const stationChart = useMemo(() => stations ? buildStationChart(stations) : [], [stations]);

  const recentActivity = useMemo(() =>
    reservations?.slice().sort((a, b) => new Date(b.scheduledAt) - new Date(a.scheduledAt)).slice(0, 8) ?? [],
    [reservations]
  );

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  const firstName = fullName?.split(' ')[0] ?? 'there';

  return (
    <div className="mx-auto max-w-6xl px-6 py-8 space-y-8">

      {/* ── Welcome Header ────────────────────────────── */}
      <div>
        <h1 className="text-xl font-bold tracking-tight text-ink">
          {greeting}, {firstName} 👋
        </h1>
        <p className="mt-1 text-[13px] text-muted">
          {new Date().toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
          {' · '}
          <span className="font-medium text-body">{role}</span>
        </p>
      </div>

      {/* ── KPI Row ───────────────────────────────────── */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        <KpiCard
          label="Active Hubs"
          value={loading ? '…' : activeStations}
          hint={`of ${stations?.length ?? 0} total`}
          icon="hubs"
        />
        <KpiCard
          label="Total Capacity"
          value={loading ? '…' : `${totalCapacity.toLocaleString()} kWh`}
          hint="Across all hubs"
          icon="battery"
        />
        <KpiCard
          label="Confirmed Bookings"
          value={loading ? '…' : confirmedRes}
          hint="Awaiting transfer"
          icon="calendar"
        />
        <KpiCard
          label="Completed"
          value={loading ? '…' : completedRes}
          hint="Energy transferred"
          icon="bolt"
        />
        {isBackoffice && (
          <>
            <KpiCard
              label="Battery Slots"
              value={loading ? '…' : totalSlots}
              hint="Declared across hubs"
              icon="pulse"
            />
            <KpiCard
              label="Staff Users"
              value={loading ? '…' : (users?.length ?? '—')}
              hint="Backoffice & Operators"
              icon="user"
            />
            <KpiCard
              label="Cancelled"
              value={loading ? '…' : cancelledRes}
              hint="This period"
              icon="check"
            />
          </>
        )}
      </div>

      {/* ── Charts Row ────────────────────────────────── */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Reservations over last 7 days — real data */}
        <div className="rounded-xl border border-line bg-surface p-5 shadow-card">
          <SectionHeader
            title="Booking Volume"
            subtitle="Reservations scheduled each day (last 7 days)"
          />
          <div className="h-48">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={reservationTrend} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="resGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#533afd" stopOpacity={0.18} />
                    <stop offset="95%" stopColor="#533afd" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e7ecf1" vertical={false} />
                <XAxis dataKey="day" tick={{ fontSize: 11, fill: '#727f96' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 11, fill: '#727f96' }} axisLine={false} tickLine={false} allowDecimals={false} />
                <Tooltip
                  contentStyle={{ fontSize: 12, borderRadius: 6, border: '1px solid #e7ecf1', boxShadow: '0 2px 8px rgba(0,0,0,0.08)' }}
                  formatter={(v) => [v, 'Bookings']}
                />
                <Area type="monotone" dataKey="count" stroke="#533afd" strokeWidth={2} fill="url(#resGrad)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Station capacity bar chart — real data */}
        <div className="rounded-xl border border-line bg-surface p-5 shadow-card">
          <SectionHeader
            title="Station Capacity"
            subtitle="Declared kWh per hub (top 6)"
          />
          <div className="h-48">
            {loading ? (
              <div className="flex h-full items-center justify-center text-muted text-sm">Loading…</div>
            ) : stationChart.length === 0 ? (
              <div className="flex h-full items-center justify-center text-muted text-sm">No stations registered</div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={stationChart} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e7ecf1" vertical={false} />
                  <XAxis dataKey="name" tick={{ fontSize: 10, fill: '#727f96' }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: '#727f96' }} axisLine={false} tickLine={false} />
                  <Tooltip
                    contentStyle={{ fontSize: 12, borderRadius: 6, border: '1px solid #e7ecf1', boxShadow: '0 2px 8px rgba(0,0,0,0.08)' }}
                    formatter={(v) => [`${v} kWh`, 'Capacity']}
                  />
                  <Bar dataKey="kWh" fill="#09825d" radius={[3, 3, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      {/* ── Activity + Quick Actions ─────────────────── */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Recent activity feed */}
        <div className="lg:col-span-2 rounded-xl border border-line bg-surface p-5 shadow-card">
          <SectionHeader
            title="Recent Reservations"
            subtitle="Latest activity across all stations"
            action={
              <Link to="/reservations" className="text-[12px] font-medium text-primary hover:underline">
                View all →
              </Link>
            }
          />
          {loading ? (
            <div className="flex items-center justify-center h-32 text-muted text-sm">Loading…</div>
          ) : recentActivity.length === 0 ? (
            <div className="flex items-center justify-center h-32 text-muted text-sm">No reservations yet</div>
          ) : (
            <ul>
              {recentActivity.map(r => (
                <ActivityItem key={r.id} reservation={r} />
              ))}
            </ul>
          )}
        </div>

        {/* Quick actions */}
        <div className="space-y-3">
          <SectionHeader title="Quick Actions" />
          <QuickAction
            label="Microgrid Hubs"
            description="View and manage solar grid nodes"
            icon="hubs"
            to="/stations"
            color="bg-violet-50 text-violet-600"
          />
          <QuickAction
            label="Reservations"
            description="Oversee all energy slot bookings"
            icon="calendar"
            to="/reservations"
            color="bg-sky-50 text-sky-600"
          />
          {isBackoffice && (
            <QuickAction
              label="User Management"
              description="Provision Backoffice & Operator accounts"
              icon="user"
              to="/admin/users"
              color="bg-emerald-50 text-emerald-600"
            />
          )}
        </div>
      </div>

      {/* ── Station Overview Table ────────────────────── */}
      <div className="rounded-xl border border-line bg-surface shadow-card overflow-hidden">
        <div className="px-5 py-4 border-b border-line flex items-center justify-between">
          <div>
            <h2 className="text-[15px] font-semibold tracking-tight text-ink">Station Overview</h2>
            <p className="text-[12px] text-muted mt-0.5">All microgrid nodes by capacity</p>
          </div>
          <Link to="/stations" className="text-[12px] font-medium text-primary hover:underline">
            Manage →
          </Link>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-[13px]">
            <thead>
              <tr className="bg-surface-alt border-b border-line">
                <th className="px-5 py-3 text-[11px] font-semibold uppercase tracking-wide text-muted">Name</th>
                <th className="px-5 py-3 text-[11px] font-semibold uppercase tracking-wide text-muted">Capacity</th>
                <th className="px-5 py-3 text-[11px] font-semibold uppercase tracking-wide text-muted">Slots</th>
                <th className="px-5 py-3 text-[11px] font-semibold uppercase tracking-wide text-muted">Status</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr><td colSpan={4} className="px-5 py-8 text-center text-muted">Loading…</td></tr>
              )}
              {!loading && (!stations || stations.length === 0) && (
                <tr><td colSpan={4} className="px-5 py-8 text-center text-muted">No stations registered yet.</td></tr>
              )}
              {!loading && stations?.map(s => (
                <tr key={s.id} className="border-b border-line last:border-0 hover:bg-surface-alt transition-colors">
                  <td className="px-5 py-3 font-medium text-ink">{s.name}</td>
                  <td className="px-5 py-3 tnum text-muted">{(s.capacityKWh ?? 0).toLocaleString()} kWh</td>
                  <td className="px-5 py-3 tnum text-muted">{s.batterySlots?.length ?? 0}</td>
                  <td className="px-5 py-3">
                    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${
                      s.status === 'Active' ? 'bg-success/10 text-success' : 'bg-muted/10 text-muted'
                    }`}>
                      {s.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

    </div>
  );
}
