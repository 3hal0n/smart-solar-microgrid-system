// ============================================================
// File: Table.jsx
// Purpose: Shared bordered/rounded table shell (Wise pricing-table
//          pattern: rounded border, row dividers, 40px min rows),
//          reused by any page that needs a data table.
// Author: Shalon
// ============================================================
// Wraps table content in the shared bordered, rounded container.
export function Table({ children }) {
  return (
    <div className="overflow-x-auto rounded-md border border-line bg-canvas">
      <table className="w-full border-collapse text-left text-sm">{children}</table>
    </div>
  );
}

// Header cell styled consistently across every table built on this shell.
export function Th({ children, className = '', ...props }) {
  return (
    <th
      className={`border-b border-line bg-surface-alt px-4 py-3 text-left text-xs font-semibold text-muted ${className}`}
      {...props}
    >
      {children}
    </th>
  );
}

// Body cell with the shared minimum row height and divider.
export function Td({ children, className = '', ...props }) {
  return (
    <td className={`min-h-[40px] border-b border-line px-4 py-3 text-ink last:border-b-0 ${className}`} {...props}>
      {children}
    </td>
  );
}
