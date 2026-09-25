// ============================================================
// File: App.jsx
// Purpose: App shell — routing and the shared auth provider. Pages
//          that should render inside the shared AppShell (sidebar +
//          top bar) nest under its layout route below; a route
//          placed outside it (e.g. login screen) renders
//          full-page with no sidebar. Other owners add their own
//          routes here per architecture.md §6.
// Author: Shalon (Updated by Migara to add Login route)
// ============================================================
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext.jsx";
import AppShell from "./components/layout/AppShell.jsx";
import LandingPage from "./pages/home/LandingPage.jsx";
import LoginPage from "./pages/auth/LoginPage.jsx"; // ADDED
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
          {/* Public pages — deliberately outside AppShell (no sidebar/top bar). */}
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} /> {/* ADDED */}

          <Route element={<AppShell />}>
            <Route path="/stations" element={<StationsPage />} />
            <Route path="/stations/:id" element={<StationDetailPage />} />

            {/* Migara: User Management route (Placeholder for next step) */}
            <Route path="/admin/users" element={<div className="p-8"><h1 className="text-2xl font-bold">User Management (Coming Next)</h1></div>} />

            {/* reservation admin oversight */}
            <Route path="/reservations" element={<ReservationsAdminPage />} />
            <Route path="/reservations/:id" element={<ReservationDetailPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}