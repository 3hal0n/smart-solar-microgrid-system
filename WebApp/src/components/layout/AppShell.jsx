// ============================================================
// File: AppShell.jsx
// Purpose: Shared product shell - sidebar on the left and full
//          content panel from top to bottom - that every authenticated
//          page renders inside.
// Author: Shalon
// ============================================================
import { useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import Sidebar from './Sidebar.jsx';
import ScrollArea from '../common/ScrollArea.jsx';
import Icon from '../common/Icon.jsx';

// Nav sections shown in the sidebar. `icon` is a name from components/common/Icon.jsx.
const NAV_SECTIONS = [
  {
    title: 'Overview',
    items: [{ label: 'Dashboard', to: '/dashboard', icon: 'grid' }],
  },
  {
    title: 'Node management',
    items: [{ label: 'Microgrid hubs', to: '/stations', icon: 'hubs' }],
  },
  // Dinil - reservation oversight (web admin view of the reservations collection).
  {
    title: 'Reservations',
    items: [{ label: 'Reservations', to: '/reservations', icon: 'calendar' }],
  },
  // Rukshan - prosumer account management (Backoffice only).
  {
    title: 'Prosumer management',
    items: [
      { label: 'Prosumers', to: '/admin/prosumers', icon: 'prosumer', end: true },
      { label: 'Pending Prosumers', to: '/admin/prosumers/pending', icon: 'clock' },
    ],
  },
  // Migara - user management (Backoffice/GridOperator accounts).
  {
    title: 'Administration',
    items: [{ label: 'Users', to: '/admin/users', icon: 'user' }],
  },
];

// Renders the shared shell (sidebar/content panel) around whichever page route is active.
export default function AppShell() {
  const { fullName, role, profilePicture, logout } = useAuth();
  const navigate = useNavigate();
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
        profilePicture={profilePicture}
        onLogout={handleLogout}
      />

      {/* Mobile sidebar toggle button */}
      <button
        type="button"
        onClick={() => setSidebarOpen(true)}
        className="fixed top-3 left-3 z-20 flex h-9 w-9 items-center justify-center rounded-lg bg-surface border border-line text-body shadow-card transition-colors hover:bg-surface-alt hover:text-ink lg:hidden"
        aria-label="Open navigation"
      >
        <Icon name="menu" className="h-5 w-5" />
      </button>

      <div className="flex min-w-0 flex-1 flex-col h-full">
        <main className="flex-1 min-h-0 bg-surface border-l border-line overflow-hidden">
          <ScrollArea className="h-full w-full">
            <Outlet />
          </ScrollArea>
        </main>
      </div>
    </div>
  );
}
