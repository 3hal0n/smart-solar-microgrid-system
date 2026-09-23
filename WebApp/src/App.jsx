// ============================================================
// File: App.jsx
// Purpose: App shell — routing and the shared auth provider. Pages
//          that should render inside the shared AppShell (sidebar +
//          top bar) nest under its layout route below; a route
//          placed outside it (e.g. a future login screen) renders
//          full-page with no sidebar. Other owners add their own
//          routes here per architecture.md §6.
// Author: Shalon
// ============================================================
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext.jsx";
import AppShell from "./components/layout/AppShell.jsx";
import LandingPage from "./pages/home/LandingPage.jsx";
import StationsPage from "./pages/stations/StationsPage.jsx";
import StationDetailPage from "./pages/stations/StationDetailPage.jsx";
import ReservationsAdminPage from "./pages/reservations/ReservationsAdminPage.jsx";
import ReservationDetailPage from "./pages/reservations/ReservationDetailPage.jsx";

// Renders the app's routing shell wrapped in the shared auth provider.
export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public marketing page — deliberately outside AppShell (no sidebar/top bar). */}
          <Route path="/" element={<LandingPage />} />

          <Route element={<AppShell />}>
            <Route path="/stations" element={<StationsPage />} />
            <Route path="/stations/:id" element={<StationDetailPage />} />

            {/* Dinil — reservation admin oversight */}
            <Route path="/reservations" element={<ReservationsAdminPage />} />
            <Route
              path="/reservations/:id"
              element={<ReservationDetailPage />}
            />

            {/* Rukshan's admin routes nest here too once built, e.g.:
                <Route path="/admin/users" element={<UsersPage />} />
                <Route path="/admin/prosumers" element={<ProsumersPage />} />
                <Route path="/admin/prosumers/pending" element={<PendingProsumersPage />} /> */}
          </Route>
          {/* TODO(Rukshan): /login has no route yet — the landing page's sign-in CTA points here and
              will render blank until LoginPage.jsx exists. It belongs outside AppShell. */}
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
