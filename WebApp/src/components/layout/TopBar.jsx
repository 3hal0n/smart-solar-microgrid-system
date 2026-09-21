// ============================================================
// File: TopBar.jsx
// Purpose: Shared top bar for AppShell — mobile sidebar toggle, the
//          signed-in user's name/role, and logout. Purely
//          presentational: current user and callbacks come in as
//          props from AppShell.
// Author: Shalon
// ============================================================
import Button from '../common/Button.jsx';

// Renders the hamburger (mobile only), signed-in user info, and a logout button.
export default function TopBar({ onToggleSidebar, fullName, role, onLogout }) {
  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b border-line bg-surface px-4 sm:px-6">
      <button
        type="button"
        onClick={onToggleSidebar}
        className="-ml-1 flex h-8 w-8 items-center justify-center rounded-md text-body transition-colors hover:bg-surface-alt hover:text-ink lg:hidden"
        aria-label="Toggle navigation"
      >
        &#9776;
      </button>

      <div className="flex-1" />

      <div className="flex items-center gap-3">
        {fullName && (
          <div className="hidden items-center gap-2 sm:flex">
            <span className="text-[13px] font-medium text-ink">{fullName}</span>
            {role && (
              <span className="rounded-md border border-line bg-surface-alt px-1.5 py-0.5 text-[11px] font-medium text-muted">
                {role}
              </span>
            )}
          </div>
        )}
        <Button variant="secondary" size="sm" onClick={onLogout}>
          Sign out
        </Button>
      </div>
    </header>
  );
}
