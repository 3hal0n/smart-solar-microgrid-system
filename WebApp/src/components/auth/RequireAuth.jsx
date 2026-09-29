// ============================================================
// File: RequireAuth.jsx
// Purpose: Route guard for the authenticated AppShell routes
//          (/stations, /admin/users, /reservations, ...) - redirects
//          to /login when nobody's signed in. Added 2026-09-26: these
//          routes were reachable with no login check at all until
//          Migara's real auth existed to gate them against. Optional
//          `roles` restricts further (e.g. /admin/users to
//          Backoffice); unauthorized-but-authenticated users are sent
//          to /stations rather than bounced back to /login.
// Author: Shalon
// ============================================================
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';

export default function RequireAuth({ children, roles }) {
  const { isAuthenticated, role } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roles && !roles.includes(role)) {
    return <Navigate to="/stations" replace />;
  }

  return children;
}
