// ============================================================
// File: LoginPage.jsx
// Purpose: Handles user authentication UI with side-by-side branding
//          and role-based redirection (Backoffice -> /stations,
//          GridOperator -> /reservations).
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
      // 1. Call the backend API (FAT Service Pattern: server validates credentials)
      const response = await api.post('/auth/login', { username, password });
      const { token, role, fullName } = response.data;

      // 2. Update global auth state via AuthContext (persists to localStorage)
      login({ token, role, fullName });

      // 3. All roles land on the Dashboard (architecture.md §6 — central overview first)
      if (role === 'Backoffice' || role === 'GridOperator') {
        navigate('/dashboard');
      } else {
        navigate('/');
      }
    } catch (err) {
      // Surface exact API error message verbatim
      setError(err.response?.data?.message || 'Login failed. Check credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen w-full bg-canvas">
      {/* Left Column: Visual branding & tagline (visible on lg screens) */}
      <div className="relative hidden w-1/2 flex-col justify-between overflow-hidden bg-slate-950 p-12 text-white lg:flex">
        {/* Background Image with layered gradient */}
        <div
          className="absolute inset-0 bg-cover bg-center opacity-45 mix-blend-luminosity filter transition-transform duration-700 hover:scale-105"
          style={{ backgroundImage: `url('/images/solar-farm-hero.jpg')` }}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/75 to-slate-900/60" />

        {/* Brand header */}
        <div className="relative z-10">
          <Link to="/" className="inline-flex items-center gap-3">
            <JouleMark id="login-hero-joule-mark" className="h-7 w-8" />
            <span className="text-xl font-bold tracking-tight text-white">Joule</span>
          </Link>
        </div>

        {/* Center/Bottom Headline & Tagline */}
        <div className="relative z-10 max-w-lg space-y-6">
          <div className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-medium tracking-wide text-cyan-300 backdrop-blur-md">
            Smart Solar Microgrid System
          </div>

          <div className="space-y-2">
            <h2 className="text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
              Power, exchanged precisely.
            </h2>
            <p className="text-sm leading-relaxed text-slate-300">
              Coordinated energy reservation, distributed battery slot management,
              and real-time grid dispatch for clean microgrid communities.
            </p>
          </div>

          {/* Feature Highlights */}
          <div className="grid grid-cols-2 gap-3 pt-2">
            <div className="rounded-xl border border-white/10 bg-white/5 p-3.5 backdrop-blur-sm">
              <p className="text-xs font-medium text-slate-400">Microgrid Nodes</p>
              <p className="mt-1 text-sm font-semibold text-white">Automated Capacity</p>
            </div>
            <div className="rounded-xl border border-white/10 bg-white/5 p-3.5 backdrop-blur-sm">
              <p className="text-xs font-medium text-slate-400">Oversight</p>
              <p className="mt-1 text-sm font-semibold text-white">Real-Time Validation</p>
            </div>
          </div>
        </div>

        {/* Footer info */}
        <div className="relative z-10 flex items-center justify-between text-xs text-slate-400 border-t border-white/10 pt-4">

        </div>
      </div>

      {/* Right Column: Sign In Form */}
      <div className="flex flex-1 flex-col justify-between px-6 py-10 sm:px-12 lg:w-1/2 lg:px-16 xl:px-24">
        {/* Top bar */}
        <div className="flex items-center justify-between">
          <div className="lg:hidden">
            <Link to="/" className="flex items-center gap-2">
              <JouleMark id="login-mobile-joule-mark" className="h-6 w-7" />
              <span className="text-[17px] font-bold tracking-tight text-ink">Joule</span>
            </Link>
          </div>
          <Link
            to="/"
            className="ml-auto inline-flex items-center gap-1.5 text-xs font-medium text-muted hover:text-ink transition-colors"
          >
            ← Back to home
          </Link>
        </div>

        {/* Main Sign In Box */}
        <div className="mx-auto w-full max-w-sm my-auto py-8">
          <div className="mb-6">
            <h1 className="text-2xl font-bold tracking-tight text-ink">Staff sign in</h1>
            <p className="mt-1.5 text-[13px] text-muted">
              Sign in with your Backoffice or Grid Operator credentials.
            </p>
          </div>

          {error && (
            <div className="mb-5 rounded-lg border border-error/30 bg-error-soft px-3.5 py-2.5 text-[13px] font-medium text-error">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <Input
              label="Username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              placeholder="e.g. operator1"
              autoFocus
            />
            <Input
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="••••••••"
            />

            <Button
              type="submit"
              variant="primary"
              className="mt-3 w-full justify-center py-2.5 text-sm font-medium shadow-sm transition-all"
              disabled={isLoading}
            >
              {isLoading ? 'Signing in…' : 'Sign in'}
            </Button>
          </form>

          {/* Security & Access note */}
          <div className="mt-8 rounded-lg border border-line bg-surface-alt/70 p-3.5 text-center text-xs text-muted">
            Staff and operator accounts are provisioned exclusively by Backoffice administrators. Prosumers use the mobile application.
          </div>
        </div>

        {/* Bottom copyright */}
        <div className="text-center text-xs text-muted">
          &copy; {new Date().getFullYear()} Joule Smart Microgrid System. All rights reserved.
        </div>
      </div>
    </div>
  );
}
