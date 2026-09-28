// ============================================================
// File: AuthContext.jsx
// Purpose: Shared auth state (token/role/fullName) backed by
//          localStorage, so any page can read "who's logged in"
//          without re-implementing storage. Built jointly Day 1 per
//          architecture.md §8; the client never decides *whether*
//          an action is allowed from this state — that's still
//          enforced server-side per the FAT service pattern.
// Author: Shalon
// ============================================================
import { createContext, useCallback, useContext, useState } from 'react';
import { AUTH_STORAGE_KEY } from '../services/api.js';

const AuthContext = createContext(null);

// Reads whatever auth state was persisted from a previous session, if any.
function readStoredAuth() {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

// Provides { token, role, fullName, isAuthenticated, login, logout } to the app.
export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(readStoredAuth);

  // Persists the logged-in user's token/role/fullName/userId/profilePicture and updates state.
  const login = useCallback(({ token, role, fullName, userId, profilePicture }) => {
    const next = { token, role, fullName, userId, profilePicture };
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(next));
    setAuth(next);
  }, []);

  // Updates stored user profile metadata (e.g. after editing profile name or picture)
  const updateUser = useCallback((updatedFields) => {
    setAuth((prev) => {
      if (!prev) return prev;
      const next = { ...prev, ...updatedFields };
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(next));
      return next;
    });
  }, []);

  // Clears the persisted session and resets state to logged-out.
  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    setAuth(null);
  }, []);

  const value = {
    token: auth?.token ?? null,
    role: auth?.role ?? null,
    fullName: auth?.fullName ?? null,
    userId: auth?.userId ?? null,
    profilePicture: auth?.profilePicture ?? null,
    isAuthenticated: Boolean(auth?.token),
    login,
    logout,
    updateUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// Reads the current auth state; must be called from inside an AuthProvider.
// eslint-disable-next-line react-refresh/only-export-components -- colocated hook, not a component
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
