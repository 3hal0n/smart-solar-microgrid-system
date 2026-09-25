// ============================================================
// File: Sidebar.jsx
// Purpose: Shared left navigation rail for AppShell — renders the
//          product's nav sections and slides in as an overlay on
//          small screens. Purely presentational: takes its sections
//          as a prop so it isn't hardcoded to any one owner's pages.
// Author: Shalon
// ============================================================
import { NavLink } from 'react-router-dom';
import JouleMark from '../common/JouleMark.jsx';

// Renders one nav section (a micro-label heading plus its links).
function NavSection({ section, onNavigate }) {
  return (
    <div className="mb-6">
      <p className="mb-1.5 px-2 text-[11px] font-semibold uppercase tracking-wider text-muted">{section.title}</p>
      <div className="flex flex-col gap-0.5">
        {section.items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            onClick={onNavigate}
            className={({ isActive }) =>
              `rounded-md px-2 py-1.5 text-[13px] font-medium transition-colors ${
                isActive ? 'bg-primary-soft text-primary' : 'text-body hover:bg-surface-alt hover:text-ink'
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
      {open && <div className="fixed inset-0 z-30 bg-ink/50 lg:hidden" onClick={onClose} aria-hidden="true" />}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-60 flex-col border-r border-line bg-surface px-3 py-4 transition-transform duration-200 lg:static lg:translate-x-0 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="mb-6 flex items-center gap-2 px-2">
          <JouleMark id="sidebar-joule-mark" className="h-5 w-6" />
          <span className="text-sm font-semibold tracking-tight text-ink">Joule</span>
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
