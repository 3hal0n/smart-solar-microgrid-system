// ============================================================
// File: App.jsx
// Purpose: App shell — routing and the shared auth provider. Pages
//          that should render inside the shared AppShell (sidebar +
//          top bar) nest under its layout route below; a route
//          placed outside it (e.g. login screen) renders
//          full-page with no sidebar. Other owners add their own
//          routes here per architecture.md §6.
// Author: Shalon (Updated by Migara to add Login route)
// Author: Rukshan (Update Prosumers Pages route)
// ============================================================
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext.jsx";
import { ToastProvider } from "./components/common/Toast.jsx";
import RequireAuth from "./components/auth/RequireAuth.jsx";
import AppShell from "./components/layout/AppShell.jsx";
import LandingPage from "./pages/home/LandingPage.jsx";
import LoginPage from "./pages/auth/LoginPage.jsx"; // ADDED
import StationsPage from "./pages/stations/StationsPage.jsx";
import StationDetailPage from "./pages/stations/StationDetailPage.jsx";
import ReservationsAdminPage from "./pages/reservations/ReservationsAdminPage.jsx";
import ReservationDetailPage from "./pages/reservations/ReservationDetailPage.jsx";
import UsersPage from "./pages/admin/UsersPage.jsx";
import DashboardPage from "./pages/dashboard/DashboardPage.jsx";

import ProsumersPage from './pages/admin/ProsumersPage.jsx';
import PendingProsumersPage from './pages/admin/PendingProsumersPage.jsx';


// Renders the app's routing shell wrapped in the shared auth/toast providers.
export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <Routes>
            {/* Public pages — deliberately outside AppShell (no sidebar/top bar). */}
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} /> {/* ADDED */}

            {/* Everything below requires a signed-in session (2026-09-26: these routes were
                reachable with no login check at all until Migara's real auth existed to gate
                them against) — see RequireAuth.jsx. */}
            <Route
              element={
                <RequireAuth>
                  <AppShell />
                </RequireAuth>
              }
            >
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/stations" element={<StationsPage />} />
              <Route path="/stations/:id" element={<StationDetailPage />} />

              {/* Migara: User Management — Backoffice only. */}
              <Route
                path="/admin/users"
                element={
                  <RequireAuth roles={["Backoffice"]}>
                    <UsersPage />
                  </RequireAuth>
                }
              />

              {/* Dinil: reservation admin oversight */}
              <Route path="/reservations" element={<ReservationsAdminPage />} />
              <Route path="/reservations/:id" element={<ReservationDetailPage />} />

              {/* Rukshan: Prosumers management — Backoffice only. */}
              <Route
                    path="/admin/prosumers"
                    element={
                      <RequireAuth roles={["Backoffice"]}>
                        <ProsumersPage />
                      </RequireAuth>
                    }
                  />
                  <Route
                    path="/admin/prosumers/pending"
                    element={
                      <RequireAuth roles={["Backoffice"]}>
                        <PendingProsumersPage />
                      </RequireAuth>
                    }
                  />
            </Route>
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  );
}