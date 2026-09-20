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
    <header className="flex h-16 items-center justify-between border-b border-line bg-canvas px-4 sm:px-6">
      <button
        type="button"
        onClick={onToggleSidebar}
        className="flex h-10 w-10 items-center justify-center rounded-pill text-ink hover:bg-surface-alt lg:hidden"
        aria-label="Toggle navigation"
      >
        &#9776;
      </button>

      <div className="flex-1" />

      <div className="flex items-center gap-4">
        {fullName && (
          <div className="hidden text-right sm:block">
            <p className="text-sm font-semibold text-ink">{fullName}</p>
            {role && <p className="text-xs text-muted">{role}</p>}
          </div>
        )}
        <Button variant="ghost" onClick={onLogout}>
          Log out
        </Button>
      </div>
    </header>
  );
}
