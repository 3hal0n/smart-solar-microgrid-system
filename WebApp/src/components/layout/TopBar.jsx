// ============================================================
// File: TopBar.jsx
// Purpose: Shared top bar for AppShell - mobile sidebar toggle and
//          a breadcrumb for the current page. Sits on the canvas
//          above the raised content panel. Purely presentational:
//          the crumbs come in as props from AppShell.
// Author: Shalon
// ============================================================
import { Link } from 'react-router-dom';
import Icon from '../common/Icon.jsx';

// Renders the hamburger (mobile only) and the breadcrumb trail. Every crumb but the last links back.
export default function TopBar({ onToggleSidebar, crumbs }) {
  return (
    <header className="flex h-14 shrink-0 items-center gap-3 px-4 lg:px-2">
      <button
        type="button"
        onClick={onToggleSidebar}
        className="-ml-1 flex h-8 w-8 items-center justify-center rounded-lg text-body transition-colors hover:bg-surface hover:text-ink lg:hidden"
        aria-label="Toggle navigation"
      >
        <Icon name="menu" className="h-5 w-5" />
      </button>

      <nav aria-label="Breadcrumb" className="flex min-w-0 items-center gap-1.5 text-[13px]">
        {crumbs.map((crumb, index) => {
          const isLast = index === crumbs.length - 1;
          return (
            <span key={crumb.label} className="flex min-w-0 items-center gap-1.5">
              {index > 0 && <Icon name="chevronRight" className="h-3.5 w-3.5 shrink-0 text-line-strong" />}
              {isLast || !crumb.to ? (
                <span className={`truncate ${isLast ? 'font-medium text-ink' : 'text-muted'}`}>{crumb.label}</span>
              ) : (
                <Link to={crumb.to} className="truncate text-muted transition-colors hover:text-ink">
                  {crumb.label}
                </Link>
              )}
            </span>
          );
        })}
      </nav>
    </header>
  );
}
