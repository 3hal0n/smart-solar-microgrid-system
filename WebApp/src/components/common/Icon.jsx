// ============================================================
// File: Icon.jsx
// Purpose: Small shared set of inline stroke icons (nav, stat
//          cards, top bar) — drawn inline instead of pulling in an
//          icon library for a handful of glyphs. 20x20 grid, 1.6
//          stroke, currentColor, so color/size come from className.
// Author: Shalon 
// ============================================================

const PATHS = {
  hubs: (
    <>
      <circle cx="10" cy="10" r="2.2" />
      <circle cx="4" cy="4.5" r="1.6" />
      <circle cx="16" cy="4.5" r="1.6" />
      <circle cx="10" cy="16.5" r="1.6" />
      <path d="M5.2 5.6 8.4 8.6M14.8 5.6l-3.2 3M10 12.2v2.7" strokeLinecap="round" />
    </>
  ),
  bolt: <path d="M11 2.5 4.5 11h5l-1 6.5L15.5 9h-5l.5-6.5Z" strokeLinejoin="round" />,
  grid: (
    <>
      <rect x="3" y="3" width="5.5" height="5.5" rx="1.2" />
      <rect x="11.5" y="3" width="5.5" height="5.5" rx="1.2" />
      <rect x="3" y="11.5" width="5.5" height="5.5" rx="1.2" />
      <rect x="11.5" y="11.5" width="5.5" height="5.5" rx="1.2" />
    </>
  ),
  pulse: <path d="M2.5 10h3.5l2-5 4 10 2-5h3.5" strokeLinecap="round" strokeLinejoin="round" />,
  battery: (
    <>
      <rect x="2.5" y="6" width="13" height="8" rx="1.5" />
      <path d="M17.5 8.5v3" strokeLinecap="round" />
      <path d="M5.5 9v2M8.5 9v2" strokeLinecap="round" />
    </>
  ),
  logout: (
    <>
      <path d="M8 3.5H5A1.5 1.5 0 0 0 3.5 5v10A1.5 1.5 0 0 0 5 16.5h3" strokeLinecap="round" />
      <path d="M12.5 13.5 16 10l-3.5-3.5M16 10H8" strokeLinecap="round" strokeLinejoin="round" />
    </>
  ),
  menu: <path d="M3.5 5.5h13M3.5 10h13M3.5 14.5h13" strokeLinecap="round" />,
  chevronRight: <path d="m8 5 5 5-5 5" strokeLinecap="round" strokeLinejoin="round" />,
  calendar: (
    <>
      <rect x="3.5" y="4.5" width="13" height="12" rx="1.5" />
      <path d="M3.5 8.5h13M7 3.5v3M13 3.5v3" strokeLinecap="round" />
    </>
  ),
  user: (
    <>
      <circle cx="10" cy="7" r="3" />
      <path d="M4 16.5c.9-2.7 3.2-4 6-4s5.1 1.3 6 4" strokeLinecap="round" strokeLinejoin="round" />
    </>
  ),
  check: <path d="M4 10.5 8 14l8-8" strokeLinecap="round" strokeLinejoin="round" />,
  
  // Rukshan (Added new icons for Pending Prosumers Page)
  clock: (
    <>
      <circle cx="10" cy="10" r="7.5" />
      <path d="M10 6.5V10l2.5 2" strokeLinecap="round" strokeLinejoin="round" />
    </>
  ),
  prosumer: (
    <>
      <path d="M3 10.5L10 4l7 6.5" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M5 9.5V16h10V9.5" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M7 12h6v3H7z" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M10 5.5V3" strokeLinecap="round" strokeLinejoin="round" />
    </>
  ),

  alert: (
    <>
      <path d="M10 6v4M10 14v2" strokeLinecap="round" />
      <path d="M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16Z" />
    </>
  ),
};

// Renders the named icon; decorative by default (aria-hidden), so pair it with visible text.
export default function Icon({ name, className = 'h-4 w-4' }) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth={1.6} className={className} aria-hidden="true">
      {PATHS[name]}
    </svg>
  );
}