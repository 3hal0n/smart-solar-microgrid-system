// ============================================================
// File: AppShell.jsx
// Purpose: Shared product shell — sidebar + top bar (with logout) +
//          content area — that every authenticated page renders
//          inside, per architecture.md §6, so the web app reads as
//          one product across owners instead of per-page layouts.
//          Used as a React Router layout route: App.jsx nests each
//          page's <Route> under it and this renders them via
//          <Outlet/>. Purely a shell — it makes no business
//          decisions, it only reads auth state to display/clear it.
// Author: Shalon
// ============================================================
import { useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext.jsx";
import Sidebar from "./Sidebar.jsx";
import TopBar from "./TopBar.jsx";

// Nav sections shown in the sidebar. Grouped by owner's module so it's obvious where to add more:
// Rukshan's admin section/items go here once LoginPage/UsersPage/ProsumersPage exist
// (architecture.md §6: /admin/users, /admin/prosumers, /admin/prosumers/pending) — don't hardcode
// links to routes that don't exist yet, since that ships dead nav entries.
const NAV_SECTIONS = [
  {
    title: "Node management",
    items: [{ label: "Microgrid hubs", to: "/stations" }]
  },

  // Dinil — reservation oversight (web admin view of the reservations collection).
  {
    title: "Reservations",
    items: [{ label: "Reservations", to: "/reservations" }]
  }
];

// Renders the shared shell (sidebar/top bar/content area) around whichever page route is active.
export default function AppShell() {
  const { fullName, role, logout } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // Clears the session and returns to the (currently stations-redirecting) home route.
  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <div className="flex h-screen overflow-hidden bg-canvas">
      <Sidebar
        sections={NAV_SECTIONS}
        open={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
      />
      <div className="flex min-w-0 flex-1 flex-col">
        <TopBar
          onToggleSidebar={() => setSidebarOpen((open) => !open)}
          fullName={fullName}
          role={role}
          onLogout={handleLogout}
        />
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
