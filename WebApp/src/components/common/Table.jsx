// ============================================================
// File: Table.jsx
// Purpose: Shared high-density data table shell (Stripe dashboard
//          treatment: white surface, 1px keylines, compact rows,
//          uppercase micro-labels in the header). Alignment and
//          numeric/mono styling are passed per-cell via className.
//
//          Two ways to use it: the named `Table`/`Th`/`Td` below are
//          compound-component primitives you compose yourself (see
//          StationsPage.jsx/StationDetailPage.jsx). The default
//          export, `DataTable` (added 2026-09-26 while merging in
//          Dinil's ReservationsAdminPage.jsx, which was already
//          written against a columns/data-driven shape), takes
//          `columns` ({ key, header, render?(row) }[]), `data`,
//          `loading`, `emptyMessage`, and `rowKey(row)`, and renders
//          through these same primitives underneath.
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

// Data-driven table: pass columns + rows, get header/body/loading/empty states for free. Each
// column is { key, header, render?(row) } — render falls back to row[key] when omitted.
export default function DataTable({ columns, data, loading, emptyMessage = 'No data.', rowKey }) {
  return (
    <Table>
      <thead>
        <tr>
          {columns.map((column) => (
            <Th key={column.key} className={column.className}>
              {column.header}
            </Th>
          ))}
        </tr>
      </thead>
      <tbody>
        {loading && (
          <tr>
            <Td colSpan={columns.length} className="py-8 text-center text-[13px] text-muted">
              Loading…
            </Td>
          </tr>
        )}
        {!loading && data.length === 0 && (
          <tr>
            <Td colSpan={columns.length} className="py-8 text-center text-[13px] text-muted">
              {emptyMessage}
            </Td>
          </tr>
        )}
        {!loading &&
          data.map((row) => (
            <tr key={rowKey(row)} className="transition-colors hover:bg-surface-alt/60">
              {columns.map((column) => (
                <Td key={column.key} className={column.className}>
                  {column.render ? column.render(row) : row[column.key]}
                </Td>
              ))}
            </tr>
          ))}
      </tbody>
    </Table>
  );
}
