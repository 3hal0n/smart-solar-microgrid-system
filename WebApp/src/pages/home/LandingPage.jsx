// ============================================================
// File: LandingPage.jsx
// Purpose: Public marketing/index page at "/" — explains the Smart
//          Solar Microgrid Trading System to both operators and
//          prosumers, and routes staff to sign-in. Static UI only:
//          no API calls and no auth logic live here.
// Author: Shalon
// ============================================================
import { Link } from 'react-router-dom';
import Button from '../../components/common/Button.jsx';

const FEATURES = [
  {
    title: 'Microgrid hub management',
    body: 'Register solar grid nodes with GPS position, kWh capacity and battery slot inventory. Schedules update live, and deactivation is blocked while energy is still reserved.',
    meta: 'Backoffice',
  },
  {
    title: 'Automated slot reservation',
    body: 'Prosumers reserve charging and discharging slots inside a rolling 7-day window. Updates and cancellations are held to the 12-hour notice rule by the service, not the client.',
    meta: 'Prosumer',
  },
  {
    title: 'Real-time operator verification',
    body: 'Grid operators scan the prosumer transaction QR at the node, verify it against the server and finalise the energy transfer in a single step.',
    meta: 'Grid operator',
  },
];

// Small inline QR-style glyph — a placeholder artifact for the store link, not a scannable code.
function QrPlaceholder() {
  return (
    <svg viewBox="0 0 48 48" className="h-28 w-28" role="img" aria-label="App download QR code placeholder">
      <rect width="48" height="48" rx="4" fill="#ffffff" />
      <g fill="#0f172a">
        <path d="M6 6h12v12H6zm3 3v6h6V9z" />
        <path d="M30 6h12v12H30zm3 3v6h6V9z" />
        <path d="M6 30h12v12H6zm3 3v6h6v-6z" />
        <path d="M22 6h4v4h-4zm0 8h4v4h-4zm-8 8h4v4h-4zm8 0h4v4h-4zm8 0h4v4h-4zm8 0h4v4h-4zm-16 8h4v4h-4zm8 0h4v4h-4zm8 4h4v4h-4zm-8 4h4v4h-4z" />
      </g>
    </svg>
  );
}

// Renders the full public landing page: nav, hero, feature grid, prosumer callout and footer.
export default function LandingPage() {
  return (
    <div className="min-h-screen bg-surface">
      {/* Navigation */}
      <header className="sticky top-0 z-40 border-b border-line bg-surface/90 backdrop-blur">
        <nav className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
          <div className="flex items-center gap-2">
            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-ink text-[11px] font-bold text-on-dark">
              SM
            </span>
            <span className="text-[15px] font-semibold tracking-tight text-ink">Smart Solar Microgrid</span>
          </div>

          <div className="hidden items-center gap-7 md:flex">
            <a href="#platform" className="text-[13px] font-medium text-body transition-colors hover:text-ink">
              Platform
            </a>
            <a href="#capabilities" className="text-[13px] font-medium text-body transition-colors hover:text-ink">
              Capabilities
            </a>
            <a href="#prosumers" className="text-[13px] font-medium text-body transition-colors hover:text-ink">
              For prosumers
            </a>
          </div>

          <Link to="/login">
            <Button variant="primary" size="sm">
              <span className="sm:hidden">Sign in</span>
              <span className="hidden sm:inline">Staff / Operator sign in</span>
            </Button>
          </Link>
        </nav>
      </header>

      {/* Hero — copy paired with a product artifact, not a centered generic block */}
      <section id="platform" className="border-b border-line bg-canvas">
        <div className="mx-auto grid max-w-6xl items-center gap-12 px-6 py-20 lg:grid-cols-2 lg:py-28">
          <div>
            <span className="inline-flex items-center rounded-md border border-primary/20 bg-primary-soft px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wider text-primary">
              Energy trading infrastructure
            </span>
            <h1 className="mt-5 text-4xl font-medium leading-[1.05] tracking-[-0.03em] text-ink sm:text-5xl">
              Peer-to-peer solar energy, settled on managed grid infrastructure.
            </h1>
            <p className="mt-5 max-w-lg text-[17px] font-light leading-relaxed text-body">
              Smart Solar Microgrid connects property owners with solar arrays to a network of operated grid hubs.
              Backoffice teams manage node capacity and schedules, prosumers reserve energy slots from mobile, and grid
              operators verify every transfer at the point of delivery.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Link to="/login">
                <Button variant="dark">Staff / Operator sign in</Button>
              </Link>
              <a href="#capabilities">
                <Button variant="secondary">Explore the platform</Button>
              </a>
            </div>
          </div>

          {/* Product artifact: a condensed view of the real operator hub table */}
          <div className="rounded-lg border border-line bg-surface shadow-hero">
            <div className="flex items-center justify-between border-b border-line px-5 py-3">
              <span className="text-[13px] font-semibold text-ink">Microgrid hubs</span>
              <span className="rounded-md border border-success/20 bg-success-soft px-2 py-0.5 text-[11px] font-medium text-success">
                3 active
              </span>
            </div>
            <table className="w-full text-left text-[13px]">
              <thead>
                <tr className="border-b border-line bg-surface-alt text-[11px] uppercase tracking-wider text-muted">
                  <th className="px-5 py-2 font-semibold">Node</th>
                  <th className="px-5 py-2 text-right font-semibold">Capacity</th>
                  <th className="px-5 py-2 text-right font-semibold">Slots</th>
                </tr>
              </thead>
              <tbody className="text-body">
                {[
                  ['Colombo Central', '240', '12'],
                  ['Kandy Ridge', '180', '8'],
                  ['Galle Coastal', '120', '6'],
                ].map(([name, capacity, slots]) => (
                  <tr key={name} className="border-b border-line last:border-b-0">
                    <td className="px-5 py-2.5 font-medium text-ink">{name}</td>
                    <td className="tnum px-5 py-2.5 text-right">
                      {capacity}
                      <span className="ml-1 text-muted">kWh</span>
                    </td>
                    <td className="tnum px-5 py-2.5 text-right">{slots}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="flex items-center justify-between border-t border-line bg-surface-alt px-5 py-3 text-[12px] text-muted">
              <span className="font-mono">6.9271, 79.8612</span>
              <span>Synced just now</span>
            </div>
          </div>
        </div>
      </section>

      {/* Infrastructure band — real photography, cropped with Stripe's angled-edge signature */}
      <section className="relative overflow-hidden bg-ink">
        <div className="relative h-55 w-full sm:h-75 lg:h-105">
          <img
            src="/images/solar-farm-hero.jpg"
            alt="Aerial view of a solar panel array feeding the managed microgrid"
            width={1920}
            height={900}
            loading="lazy"
            decoding="async"
            className="absolute inset-0 h-full w-full object-cover [clip-path:polygon(0_0,100%_0,100%_100%,0_92%)]"
          />
          <div className="absolute inset-0 bg-linear-to-t from-ink/95 via-ink/15 to-transparent" />
          <div className="absolute inset-x-0 bottom-0">
            <div className="mx-auto max-w-6xl px-6 pb-6 sm:pb-8 lg:pb-10">
              <p className="text-[11px] font-semibold uppercase tracking-wider text-white/60">
                Live grid infrastructure
              </p>
              <p className="mt-2 max-w-xl text-lg font-medium leading-snug text-white sm:text-xl lg:text-2xl">
                Every hub in the network reports capacity and slot availability in real time.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Feature grid */}
      <section id="capabilities" className="mx-auto max-w-6xl px-6 py-20 lg:py-24">
        <h2 className="max-w-2xl text-3xl font-medium tracking-[-0.02em] text-ink">
          One service, three operating roles.
        </h2>
        <p className="mt-3 max-w-2xl text-[17px] font-light text-body">
          Every business rule — capacity checks, reservation windows, QR verification — is enforced centrally in the
          API, so the web and mobile clients stay thin.
        </p>

        <div className="mt-12 grid gap-6 md:grid-cols-3">
          {FEATURES.map((feature) => (
            <article key={feature.title} className="rounded-lg border border-line bg-surface p-6 shadow-card">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-muted">{feature.meta}</span>
              <h3 className="mt-3 text-[18px] font-medium tracking-tight text-ink">{feature.title}</h3>
              <p className="mt-2 text-[14px] font-light leading-relaxed text-body">{feature.body}</p>
            </article>
          ))}
        </div>
      </section>

      {/* Prosumer callout */}
      <section id="prosumers" className="border-y border-line bg-canvas">
        <div className="mx-auto max-w-6xl px-6 py-20">
          <div className="relative flex flex-col items-start gap-10 overflow-hidden rounded-lg border border-line p-8 text-on-dark sm:p-10 lg:flex-row lg:items-center lg:justify-between">
            <img
              src="/images/rooftop-solar.jpg"
              alt="Rooftop solar panel installation on a prosumer's home"
              width={1200}
              height={900}
              loading="lazy"
              decoding="async"
              className="absolute inset-0 h-full w-full object-cover"
            />
            <div className="absolute inset-0 bg-ink/88" />
            <div className="relative max-w-xl">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-white/50">For solar prosumers</span>
              <h2 className="mt-3 text-3xl font-medium tracking-[-0.02em]">Get the Prosumer app</h2>
              <p className="mt-3 text-[15px] font-light leading-relaxed text-white/70">
                Register with your NIC, find nearby grid nodes on the map, and reserve charging or discharging slots in
                a few taps. Every confirmed booking issues a secure transaction QR code that an operator scans at the
                hub.
              </p>
              <ol className="mt-6 space-y-2 text-[14px] font-light text-white/70">
                <li>1. Install the Android app and register using your NIC.</li>
                <li>2. Wait for Backoffice activation, then sign in.</li>
                <li>3. Reserve a slot and present your QR code at the node.</li>
              </ol>
            </div>

            <div className="relative flex shrink-0 flex-col items-center gap-3 rounded-lg bg-white/10 p-6 backdrop-blur-sm">
              <QrPlaceholder />
              <p className="text-center text-[12px] font-light text-white/60">
                Scan to download
                <br />
                Android only · SQLite offline support
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-ink text-on-dark">
        <div className="mx-auto max-w-6xl px-6 py-16">
          <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="flex h-7 w-7 items-center justify-center rounded-md bg-white/10 text-[11px] font-bold">
                  SM
                </span>
                <span className="text-[14px] font-semibold tracking-tight">Smart Solar Microgrid</span>
              </div>
              <p className="mt-3 text-[13px] font-light text-white/50">
                Client-server energy trading platform for solar prosumers and microgrid operators.
              </p>
            </div>

            <div>
              <p className="text-[11px] font-semibold uppercase tracking-wider text-white/40">Platform</p>
              <ul className="mt-3 space-y-2 text-[13px] font-light text-white/60">
                <li>
                  <a href="#platform" className="hover:text-white">
                    Overview
                  </a>
                </li>
                <li>
                  <a href="#capabilities" className="hover:text-white">
                    Capabilities
                  </a>
                </li>
                <li>
                  <a href="#prosumers" className="hover:text-white">
                    Prosumer app
                  </a>
                </li>
              </ul>
            </div>

            <div>
              <p className="text-[11px] font-semibold uppercase tracking-wider text-white/40">Access</p>
              <ul className="mt-3 space-y-2 text-[13px] font-light text-white/60">
                <li>
                  <Link to="/login" className="hover:text-white">
                    Staff sign in
                  </Link>
                </li>
                <li>
                  <Link to="/stations" className="hover:text-white">
                    Operator console
                  </Link>
                </li>
              </ul>
            </div>

            <div>
              <p className="text-[11px] font-semibold uppercase tracking-wider text-white/40">Institution</p>
              <ul className="mt-3 space-y-2 text-[13px] font-light text-white/60">
                <li>SE4040 — Enterprise Application Development</li>
                <li>BSc (Hons) in Information Technology</li>
                <li>Faculty of Computing</li>
              </ul>
            </div>
          </div>

          <div className="mt-12 border-t border-white/10 pt-6 text-[12px] font-light text-white/40">
            Smart Solar Microgrid Trading System — academic project, {new Date().getFullYear()}.
          </div>
        </div>
      </footer>
    </div>
  );
}
