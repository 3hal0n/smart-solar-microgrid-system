// ============================================================
// File: App.jsx
// Purpose: App shell — routing and the shared auth provider.
//          Currently wires up Shalon's Stations screen; other
//          owners add their own routes here per architecture.md §6.
// Author: Shalon
// ============================================================
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext.jsx';
import StationsPage from './pages/stations/StationsPage.jsx';

// Renders the app's routing shell wrapped in the shared auth provider.
export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/stations" element={<StationsPage />} />
          {/* TODO: point this at the real login/home screen once it exists. */}
          <Route path="/" element={<Navigate to="/stations" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
