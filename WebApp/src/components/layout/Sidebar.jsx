// ============================================================
// File: Sidebar.jsx
// Purpose: Shared left navigation rail for AppShell - brand at the
//          top, icon nav sections in the middle, and the signed-in
//          user + sign out pinned to the bottom. Sits flat on the
//          canvas (the content panel is the one raised surface) and
//          slides in as an overlay on small screens. Purely
//          presentational: sections/user/callbacks come in as props.
// Author: Shalon
// ============================================================
import { NavLink } from 'react-router-dom';
import JouleMark from '../common/JouleMark.jsx';
import Icon from '../common/Icon.jsx';

// Initials for the avatar chip, e.g. "Shalon Fernando" -> "SF".
function initialsOf(name) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
}

// Renders one nav section's items as icon links, no title header.
function NavSection({ section, onNavigate }) {
  return (
    <div className="mb-1">
      <div className="flex flex-col gap-0.5">
        {section.items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            onClick={onNavigate}
            className={({ isActive }) =>
              `flex items-center gap-2.5 rounded-lg px-3 py-2 text-[13px] font-medium transition-colors ${
                isActive
                  ? 'bg-surface text-ink shadow-card ring-1 ring-line'
                  : 'text-body hover:bg-surface/70 hover:text-ink'
              }`
            }
          >
            {({ isActive }) => (
              <>
                <Icon name={item.icon} className={`h-4 w-4 ${isActive ? 'text-primary' : 'text-muted'}`} />
                {item.label}
              </>
            )}
          </NavLink>
        ))}
      </div>
    </div>
  );
}

// Renders the sidebar: brand, nav sections, and the user/sign-out footer. On small screens it's a
// fixed drawer driven by `open`; on desktop (lg:) it's a static rail.
export default function Sidebar({ sections, open, onClose, fullName, role, profilePicture, onLogout }) {
  return (
    <>
      {open && <div className="fixed inset-0 z-30 bg-ink/40 lg:hidden" onClick={onClose} aria-hidden="true" />}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-canvas px-3 py-4 shadow-panel transition-transform duration-200 lg:static lg:translate-x-0 lg:shadow-none ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="mb-8 flex items-center gap-2.5 px-3 pt-1">
          <JouleMark id="sidebar-joule-mark" className="h-6 w-7" />
          <span className="text-[16px] font-semibold tracking-[-0.03em] text-ink">Joule</span>
        </div>

        <nav className="flex-1 overflow-y-auto">
          {sections.map((section) => (
            <NavSection key={section.title} section={section} onNavigate={onClose} />
          ))}
        </nav>

        <div className="mt-4 border-t border-line pt-4">
          <NavLink
            to="/profile"
            onClick={onClose}
            className={({ isActive }) =>
              `flex items-center gap-2.5 rounded-xl p-2 transition-colors ${
                isActive
                  ? 'bg-surface text-ink ring-1 ring-line shadow-card'
                  : 'hover:bg-surface/70'
              }`
            }
          >
            {profilePicture ? (
              <img
                src={profilePicture}
                alt={fullName || 'Avatar'}
                className="h-8 w-8 shrink-0 rounded-full object-cover ring-1 ring-line"
              />
            ) : (
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary-soft text-[12px] font-semibold text-primary">
                {fullName ? initialsOf(fullName) : '-'}
              </span>
            )}
            <div className="min-w-0 flex-1">
              <p className="truncate text-[13px] font-medium text-ink">{fullName ?? 'Not signed in'}</p>
              {role && <p className="truncate text-[11px] text-muted">{role}</p>}
            </div>
          </NavLink>
          <button
            type="button"
            onClick={onLogout}
            className="mt-2 flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-[13px] font-medium text-body transition-colors hover:bg-surface/70 hover:text-ink"
          >
            <Icon name="logout" className="h-4 w-4 text-muted" />
            Sign out
          </button>
        </div>
      </aside>
    </>
  );
}
