// ============================================================
// File: LandingPage.jsx
// Purpose: Public marketing/index page at "/" - explains Joule (the
//          Smart Solar Microgrid Trading System) to both operators
//          and prosumers, and routes staff to sign-in. Static UI
//          only: no API calls and no auth logic live here.
// Author: Shalon
// ============================================================
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import Button from '../../components/common/Button.jsx';
import JouleMark from '../../components/common/JouleMark.jsx';
import FadeIn from '../../components/common/FadeIn.jsx';
import AnimatedHeading from '../../components/common/AnimatedHeading.jsx';

const PROSUMER_STEPS = [
  'Install the Android app and register using your NIC.',
  'Wait for Backoffice activation, then sign in.',
  'Reserve a slot and present your QR code at the node.',
];

// Small inline QR-style glyph - a placeholder artifact for the store link, not a scannable code.
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

// Renders the full public landing page: sticky nav, hero, feature grid, prosumer callout and footer.
export default function LandingPage() {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  return (
    <div className="min-h-screen bg-surface">
      {/* Sticky / Fixed Navbar across all scroll states */}
      <header className="fixed top-0 left-0 right-0 z-50 w-full px-4 sm:px-6 md:px-12 lg:px-16 pt-3 sm:pt-6 transition-all duration-300">
        <nav className="liquid-glass rounded-xl px-4 py-2.5 sm:py-2 flex items-center justify-between">
          {/* Left: Logo with Joule brand mark */}
          <Link to="/" className="flex items-center gap-2 sm:gap-2.5" onClick={() => setIsMobileMenuOpen(false)}>
            <JouleMark id="nav-joule-mark" className="h-6 w-7 sm:h-7 sm:w-8 shrink-0" />
            <span className="text-xl sm:text-2xl font-semibold tracking-tight text-white">JOULE</span>
          </Link>

          {/* Center: Nav links (Desktop) */}
          <div className="hidden md:flex items-center gap-8 text-sm text-white/90">
            <a href="#platform" className="transition-colors hover:text-gray-300">
              Platform
            </a>
            <a href="#capabilities" className="transition-colors hover:text-gray-300">
              Capabilities
            </a>
            <a href="#showcase" className="transition-colors hover:text-gray-300">
              Showcase
            </a>
            <a href="#prosumers" className="transition-colors hover:text-gray-300">
              For prosumers
            </a>
          </div>

          {/* Right: Desktop CTA & Mobile Hamburger */}
          <div className="flex items-center gap-3">
            <Link
              to="/login"
              className="hidden sm:inline-block bg-white text-black px-5 sm:px-6 py-2 rounded-lg text-sm font-medium hover:bg-gray-100 transition-colors"
            >
              Staff Sign In
            </Link>

            {/* Mobile Hamburger Button */}
            <button
              type="button"
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="md:hidden flex items-center justify-center p-1.5 rounded-lg text-white hover:bg-white/10 transition-colors"
              aria-label={isMobileMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
              aria-expanded={isMobileMenuOpen}
            >
              {isMobileMenuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
            </button>
          </div>
        </nav>

        {/* Mobile Dropdown Menu */}
        {isMobileMenuOpen && (
          <div className="md:hidden mt-2 liquid-glass rounded-xl p-4 border border-white/20 shadow-2xl flex flex-col gap-2 animate-in fade-in slide-in-from-top-2 duration-200">
            <a
              href="#platform"
              onClick={() => setIsMobileMenuOpen(false)}
              className="text-white/90 hover:text-white px-3 py-2 rounded-lg text-sm font-medium hover:bg-white/10 transition-colors"
            >
              Platform
            </a>
            <a
              href="#capabilities"
              onClick={() => setIsMobileMenuOpen(false)}
              className="text-white/90 hover:text-white px-3 py-2 rounded-lg text-sm font-medium hover:bg-white/10 transition-colors"
            >
              Capabilities
            </a>
            <a
              href="#showcase"
              onClick={() => setIsMobileMenuOpen(false)}
              className="text-white/90 hover:text-white px-3 py-2 rounded-lg text-sm font-medium hover:bg-white/10 transition-colors"
            >
              Showcase
            </a>
            <a
              href="#prosumers"
              onClick={() => setIsMobileMenuOpen(false)}
              className="text-white/90 hover:text-white px-3 py-2 rounded-lg text-sm font-medium hover:bg-white/10 transition-colors"
            >
              For prosumers
            </a>
            <div className="pt-2 border-t border-white/10">
              <Link
                to="/login"
                onClick={() => setIsMobileMenuOpen(false)}
                className="w-full block text-center bg-white text-black px-4 py-2.5 rounded-lg text-sm font-medium hover:bg-gray-100 transition-colors"
              >
                Staff Sign In
              </Link>
            </div>
          </div>
        )}
      </header>

      {/* Fullscreen Video Hero */}
      <section id="platform" className="relative min-h-screen w-full flex flex-col justify-between overflow-hidden bg-black text-white">
        {/* Video Background: raw video with NO dark/gradient overlay, compressed under 2MB */}
        <video
          autoPlay
          loop
          muted
          playsInline
          className="absolute inset-0 h-full w-full object-cover"
          src="/hero-vid.mp4"
        />

        {/* Hero Content (Bottom of viewport) */}
        <div className="relative z-10 w-full px-4 sm:px-6 md:px-12 lg:px-16 flex-1 flex flex-col justify-end pb-8 sm:pb-12 lg:pb-16 pt-28 sm:pt-32">
          <div className="lg:grid lg:grid-cols-2 lg:items-end">
            {/* Left Column - Main content */}
            <div>
              <AnimatedHeading text={"Powering tomorrow\nwith vision and action."} />

              <FadeIn delay={800} duration={1000}>
                <p className="text-sm sm:text-base md:text-lg text-gray-300 mb-6 max-w-xl leading-relaxed">
                  We empower solar prosumers and craft microgrids that define what comes next.
                </p>
              </FadeIn>

              <FadeIn delay={1200} duration={1000}>
                <div className="flex flex-row items-center gap-3 sm:gap-4 flex-wrap">
                  <Link
                    to="/login"
                    className="bg-white text-black px-4 sm:px-8 py-2.5 sm:py-3 rounded-lg font-medium hover:bg-gray-100 transition-colors text-center text-xs sm:text-base"
                  >
                    Staff Sign In
                  </Link>
                  <a
                    href="#capabilities"
                    className="liquid-glass border border-white/20 text-white px-4 sm:px-8 py-2.5 sm:py-3 rounded-lg font-medium hover:bg-white hover:text-black transition-all duration-300 text-center text-xs sm:text-base"
                  >
                    Explore Platform
                  </a>
                </div>
              </FadeIn>
            </div>

            {/* Right Column - Tag Line (Clean text, no button-like outline) */}
            <div className="flex items-end justify-start lg:justify-end mt-6 sm:mt-8 lg:mt-0">
              <FadeIn delay={1400} duration={1000}>
                <p className="text-xs sm:text-sm md:text-xl lg:text-2xl font-light text-white/85 tracking-widest sm:tracking-wide">
                  Solar. Microgrids. Trading.
                </p>
              </FadeIn>
            </div>
          </div>
        </div>
      </section>

      {/* Architecture & Ecosystem Section */}
      <section id="capabilities" className="mx-auto max-w-6xl px-4 sm:px-6 py-16 sm:py-24">
        <div className="text-center max-w-4xl mx-auto mb-12 sm:mb-16">
          <p className="text-[12px] font-semibold uppercase tracking-wider text-primary">
            Power, exchanged precisely.
          </p>
          <h2 className="mt-3 text-3xl sm:text-4xl md:text-5xl font-medium tracking-[-0.03em] text-ink leading-tight md:whitespace-nowrap">
            Your roof makes power.{' '}
            <span className="text-primary whitespace-nowrap">
              Now trade it.
            </span>
          </h2>
          <p className="mt-4 text-base sm:text-lg font-light text-body leading-relaxed max-w-2xl mx-auto">
            Seamless peer-to-peer solar energy exchange settled automatically across managed microgrid infrastructure.
          </p>
        </div>

        {/* 3 Clean Full-Fidelity Showcase Cards */}
        <div className="space-y-8 sm:space-y-12">
          {/* Backoffice Web App & API Enforcer */}
          <div className="overflow-hidden rounded-2xl border border-line bg-surface shadow-card transition-all duration-300 hover:shadow-panel">
            <img
              src="/f_00568.png"
              alt="React Web and ASP.NET Core API - One API enforces every rule"
              width={1920}
              height={1080}
              loading="lazy"
              decoding="async"
              className="w-full h-auto object-cover"
            />
          </div>

          {/* Prosumer Android Booking */}
          <div className="overflow-hidden rounded-2xl border border-line bg-surface shadow-card transition-all duration-300 hover:shadow-panel">
            <img
              src="/t_10.90.png"
              alt="Native Android Prosumer - Book a solar slot from your phone"
              width={1920}
              height={1080}
              loading="lazy"
              decoding="async"
              className="w-full h-auto object-cover"
            />
          </div>

          {/* Grid Operator QR Verification */}
          <div className="overflow-hidden rounded-2xl border border-line bg-surface shadow-card transition-all duration-300 hover:shadow-panel">
            <img
              src="/chk_14.0.png"
              alt="Native Android Grid Operator - Operators scan it at the hub"
              width={1920}
              height={1080}
              loading="lazy"
              decoding="async"
              className="w-full h-auto object-cover"
            />
          </div>
        </div>
      </section>

      {/* Video Showcase Section (video.mp4) */}
      <section id="showcase" className="border-t border-line bg-canvas py-16 sm:py-20 lg:py-24">
        <div className="mx-auto max-w-5xl px-4 sm:px-6">
          <div className="text-center max-w-2xl mx-auto mb-8 sm:mb-10">
            <p className="text-[12px] font-semibold uppercase tracking-wider text-primary">
              Live Demonstration
            </p>
            <h2 className="mt-2 text-3xl sm:text-4xl font-medium tracking-[-0.02em] text-ink">
              Experience the Joule microgrid in motion.
            </h2>
            <p className="mt-3 text-sm sm:text-[16px] font-light text-body">
              Watch how prosumer reservations, operator QR scanning, and backoffice station telemetry connect
              seamlessly.
            </p>
          </div>

          <div className="overflow-hidden rounded-2xl border border-line bg-black shadow-2xl">
            <video
              controls
              playsInline
              preload="metadata"
              poster="/t_02.50.png"
              className="w-full h-auto aspect-video object-contain"
              src="/video.mp4"
            >
              Your browser does not support the video tag.
            </video>
          </div>
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
                {PROSUMER_STEPS.map((step, index) => (
                  <li key={step}>
                    {index + 1}. {step}
                  </li>
                ))}
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
                <JouleMark id="footer-joule-mark" className="h-6 w-7" />
                <span className="text-[14px] font-semibold tracking-tight">Joule</span>
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
                <li>SE4040 - Enterprise Application Development</li>
                <li>BSc (Hons) in Information Technology</li>
                <li>Faculty of Computing</li>
              </ul>
            </div>
          </div>

          <div className="mt-12 border-t border-white/10 pt-6 text-[12px] font-light text-white/40">
            Joule - Smart Solar Microgrid Trading System, academic project, {new Date().getFullYear()}.
          </div>
        </div>
      </footer>
    </div>
  );
}
