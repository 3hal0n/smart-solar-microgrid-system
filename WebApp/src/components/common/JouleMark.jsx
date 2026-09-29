// ============================================================
// File: JouleMark.jsx
// Purpose: Shared Joule brand mark (the four-bar lightning-bolt
//          glyph) - violet-to-cyan gradient by default, or a
//          single-color `monochrome` variant for dark surfaces/
//          favicons. `id` must be unique per render since it names
//          the SVG gradient def - reused across the landing page nav,
//          footer, and the app Sidebar.
// Author: Shalon
// ============================================================
export default function JouleMark({ id, className = '', monochrome = false }) {
  const fill = monochrome ? 'currentColor' : `url(#${id})`;

  return (
    <svg aria-label="Joule" className={className} role="img" viewBox="0 0 220 180" xmlns="http://www.w3.org/2000/svg">
      {!monochrome && (
        <defs>
          <linearGradient id={id} gradientUnits="userSpaceOnUse" x1="18" x2="202" y1="90" y2="90">
            <stop stopColor="#533afd" />
            <stop offset="1" stopColor="#11efe3" />
          </linearGradient>
        </defs>
      )}
      <g fill={fill} transform="rotate(-30 110 90)">
        <rect height="30" rx="4" width="83" x="18" y="50" />
        <rect height="30" rx="4" width="87" x="115" y="50" />
        <rect height="30" rx="4" width="109" x="18" y="100" />
        <rect height="30" rx="4" width="61" x="141" y="100" />
      </g>
    </svg>
  );
}
