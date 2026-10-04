// ============================================================
// File: api.js
// Purpose: Shared Axios instance for all API calls - base URL and
//          JWT bearer attachment live here once, so pages never
//          talk to fetch()/localStorage directly. Built jointly
//          Day 1 per architecture.md §8; every owner's pages use
//          this same instance.
// Author: Shalon
// ============================================================
import axios from 'axios';

// localStorage key AuthContext reads/writes; exported so it stays in exactly one place.
export const AUTH_STORAGE_KEY = 'smartmicrogrid_auth';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000/api',
});

// Attaches the stored JWT (if any) to every outgoing request as a Bearer token.
api.interceptors.request.use((config) => {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (raw) {
    try {
      const { token } = JSON.parse(raw);
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch {
      // Malformed stored auth - ignore and send the request unauthenticated.
    }
  }
  return config;
});

export default api;
