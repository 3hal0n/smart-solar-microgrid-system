// ============================================================
// File: Sidebar.jsx
// Purpose: Shared left navigation rail for AppShell — renders the
//          product's nav sections and slides in as an overlay on
//          small screens. Purely presentational: takes its sections
//          as a prop so it isn't hardcoded to any one owner's pages.
// Author: Shalon
// ============================================================
import { NavLink } from 'react-router-dom';

// Renders one nav section (a heading plus its links).
function NavSection({ section, onNavigate }) {
  return (
    <div className="mb-6">
      <p className="mb-2 px-3 text-xs font-semibold uppercase tracking-wide text-muted/80">{section.title}</p>
      <div className="flex flex-col gap-1">
        {section.items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            onClick={onNavigate}
            className={({ isActive }) =>
              `rounded-pill px-3 py-2 text-sm font-semibold transition-colors ${
                isActive ? 'bg-accent text-on-accent' : 'text-primary hover:bg-surface-tint'
              }`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </div>
    </div>
  );
}

// Renders the sidebar itself: brand header, nav sections, and (on small screens) a backdrop +
// slide-in transform driven by the `open` prop.
export default function Sidebar({ sections, open, onClose }) {
  return (
    <>
      {open && (
        <div className="fixed inset-0 z-30 bg-ink/40 lg:hidden" onClick={onClose} aria-hidden="true" />
      )}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-line bg-canvas p-4 transition-transform duration-200 lg:static lg:translate-x-0 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="mb-6 px-2">
          <span className="font-display text-lg font-bold tracking-tight text-ink">Smart Microgrid</span>
        </div>
        <nav className="flex-1 overflow-y-auto">
          {sections.map((section) => (
            <NavSection key={section.title} section={section} onNavigate={onClose} />
          ))}
        </nav>
      </aside>
    </>
  );
}
