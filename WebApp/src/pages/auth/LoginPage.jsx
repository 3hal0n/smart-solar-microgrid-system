// ============================================================
// File: LoginPage.jsx
// Purpose: Handles user authentication UI and role-based
//          redirection (Backoffice -> /stations, GridOperator ->
//          /reservations — the actual oversight page architecture.md
//          documents for Grid Operators on web; there is no separate
//          "/operator/dashboard" page by design, see §6).
// Author: Migara (restyled to the Joule/Stripe design system and the
//          broken GridOperator redirect fixed, 2026-09-26 — see
//          chat notes for the team)
// ============================================================

import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import JouleMark from '../../components/common/JouleMark';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      // 1. Call the backend API (FAT Service Pattern: server validates everything)
      const response = await api.post('/auth/login', { username, password });
      const { token, role, fullName } = response.data;

      // 2. Update global auth state via AuthContext (persists to localStorage)
      login({ token, role, fullName });

      // 3. Role-based redirection. GridOperator used to point at "/operator/dashboard", a route
      // that's never existed in App.jsx — every operator login landed on a blank page. Grid
      // Operators' actual web oversight is the Reservations page (architecture.md §6).
      if (role === 'Backoffice') {
        navigate('/stations');
      } else if (role === 'GridOperator') {
        navigate('/reservations');
      } else {
        navigate('/');
      }
    } catch (err) {
      // Surface the exact API error message verbatim (FAT service pattern)
      setError(err.response?.data?.message || 'Login failed. Check credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas px-4">
      <div className="w-full max-w-sm">
        <Link to="/" className="mb-8 flex items-center justify-center gap-2">
          <JouleMark id="login-joule-mark" className="h-6 w-7" />
          <span className="text-[16px] font-semibold tracking-[-0.03em] text-ink">Joule</span>
        </Link>

        <div className="rounded-lg border border-line bg-surface p-8 shadow-card">
          <h1 className="text-lg font-semibold tracking-tight text-ink">Staff sign in</h1>
          <p className="mt-1 text-[13px] text-muted">Backoffice and Grid Operator accounts only.</p>

          {error && (
            <p className="mt-4 rounded-md border border-error/30 bg-error-soft px-3 py-2 text-[13px] font-medium text-error">
              {error}
            </p>
          )}

          <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
            <Input
              label="Username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              placeholder="Enter username"
              autoFocus
            />
            <Input
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="Enter password"
            />
            <Button type="submit" variant="primary" className="mt-2 w-full" disabled={isLoading}>
              {isLoading ? 'Signing in…' : 'Sign in'}
            </Button>
          </form>
        </div>

        {/* Staff/operator accounts are provisioned by a Backoffice admin via Users management
            (see UsersPage.jsx) — there's no self-serve registration for this app by design, so
            no "Create account" link belongs here. */}
        <Link to="/" className="mt-6 block text-center text-[13px] font-medium text-muted hover:text-ink">
          ← Back to home
        </Link>
      </div>
    </div>
  );
}
