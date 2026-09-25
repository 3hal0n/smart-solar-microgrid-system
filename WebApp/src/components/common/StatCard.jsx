// ============================================================
// File: StatCard.jsx
// Purpose: Shared stat tile for a quick-glance summary row above a
//          list/table page. Tray treatment: a tinted outer frame with
//          an icon + label header, wrapping an inner white card that
//          holds the number (tabular numerals, per the doc's
//          {statistic-tile}) and an optional context line.
// Author: Shalon
// ============================================================
import Icon from './Icon.jsx';

export default function StatCard({ label, value, hint, icon }) {
  return (
    <div className="rounded-xl border border-line bg-surface-alt p-1">
      <div className="flex items-center gap-2 px-2.5 pb-1.5 pt-1">
        {icon && <Icon name={icon} className="h-3.5 w-3.5 text-primary" />}
        <p className="text-[12px] font-medium text-body">{label}</p>
      </div>
      <div className="rounded-lg border border-line bg-surface px-3.5 py-3 shadow-card">
        <p className="tnum text-2xl font-semibold tracking-tight text-ink">{value}</p>
        <p className="mt-0.5 text-[12px] text-muted">{hint ?? ' '}</p>
      </div>
    </div>
  );
}
