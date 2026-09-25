// ============================================================
// File: Table.jsx
// Purpose: Shared high-density data table shell (Stripe dashboard
//          treatment: white surface, 1px keylines, compact rows,
//          uppercase micro-labels in the header). Alignment and
//          numeric/mono styling are passed per-cell via className.
// Author: Shalon
// ============================================================
// Wraps table content in the shared bordered, rounded container.
// The arbitrary variant strips the divider from the final row so the card edge stays clean.
export function Table({ children }) {
  return (
    <div className="overflow-x-auto rounded-lg border border-line bg-surface shadow-card">
      <table className="w-full border-collapse text-left text-sm [&_tbody_tr:last-child_td]:border-b-0">
        {children}
      </table>
    </div>
  );
}

// Header cell: small, tracked, muted — reads as a column key rather than content.
export function Th({ children, className = '', ...props }) {
  return (
    <th
      className={`border-b border-line bg-surface-alt px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted ${className}`}
      {...props}
    >
      {children}
    </th>
  );
}

// Body cell with the shared row height and divider.
export function Td({ children, className = '', ...props }) {
  return (
    <td className={`border-b border-line px-4 py-3 align-middle text-ink ${className}`} {...props}>
      {children}
    </td>
  );
}
export default Table;
