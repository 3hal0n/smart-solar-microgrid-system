// ============================================================
// File: AppShell.jsx
// Purpose: Shared product shell — flat sidebar on the canvas, a
//          breadcrumb top bar, and one raised content panel — that
//          every authenticated page renders inside, per
//          architecture.md §6, so the web app reads as one product
//          across owners instead of per-page layouts. Used as a React
//          Router layout route: App.jsx nests each page's <Route>
//          under it and this renders them via <Outlet/>. Purely a
//          shell — it makes no business decisions, it only reads
//          auth state to display/clear it.
// Author: Shalon
// ============================================================
import { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import Sidebar from './Sidebar.jsx';
import TopBar from './TopBar.jsx';

// Nav sections shown in the sidebar. `icon` is a name from components/common/Icon.jsx. Grouped by
// owner's module so it's obvious where to add more: Rukshan's admin section/items go here once
// LoginPage/UsersPage/ProsumersPage exist (architecture.md §6: /admin/users, /admin/prosumers,
// /admin/prosumers/pending) — don't hardcode links to routes that don't exist yet, since that
// ships dead nav entries.
const NAV_SECTIONS = [
  {
    title: 'Node management',
    items: [{ label: 'Microgrid hubs', to: '/stations', icon: 'hubs' }],
  },
];

// Builds the breadcrumb trail from the current path: the matching nav item as the root, plus a
// "Details" crumb for anything nested under it (e.g. /stations/:id).
function breadcrumbsFor(pathname) {
  const item = NAV_SECTIONS.flatMap((section) => section.items).find(
    (candidate) => pathname === candidate.to || pathname.startsWith(`${candidate.to}/`),
  );
  if (!item) {
    return [{ label: 'Joule' }];
  }
  const crumbs = [{ label: item.label, to: item.to }];
  if (pathname !== item.to) {
    crumbs.push({ label: 'Details' });
  }
  return crumbs;
}

// Renders the shared shell (sidebar/top bar/content panel) around whichever page route is active.
export default function AppShell() {
  const { fullName, role, logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // Clears the session and returns to the public landing page.
  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="flex h-screen overflow-hidden bg-canvas">
      <Sidebar
        sections={NAV_SECTIONS}
        open={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        fullName={fullName}
        role={role}
        onLogout={handleLogout}
      />
      <div className="flex min-w-0 flex-1 flex-col lg:pr-3">
        <TopBar onToggleSidebar={() => setSidebarOpen((open) => !open)} crumbs={breadcrumbsFor(pathname)} />
        <main className="flex-1 overflow-y-auto border-line bg-surface sm:mx-3 sm:mb-3 sm:rounded-2xl sm:border sm:shadow-card lg:mx-0">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
