// ============================================================
// File: StatCard.jsx
// Purpose: Shared small stat tile (label, big number, optional hint)
//          for putting a quick-glance summary row above a list/table
//          page — the {typography.title-md}/{statistic-tile} treatment
//          from docs/stripe.design.md, reusable by any page that
//          wants one, not just Stations.
// Author: Shalon
// ============================================================
export default function StatCard({ label, value, hint }) {
  return (
    <div className="rounded-xl border border-line bg-surface p-4 shadow-card">
      <p className="text-[12px] font-medium uppercase tracking-wider text-muted">{label}</p>
      <p className="tnum mt-2 text-2xl font-semibold tracking-tight text-ink">{value}</p>
      {hint && <p className="mt-1 text-[12px] text-muted">{hint}</p>}
    </div>
  );
}
