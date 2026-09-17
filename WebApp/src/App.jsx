import React from 'react';

export default function App() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-slate-900 px-4 text-white">
      <div className="max-w-md rounded-2xl bg-slate-800 p-8 shadow-2xl border border-slate-700 text-center transition-all hover:scale-[1.02]">
        
        {/* Animated Tailwind Indicator */}
        <div className="mx-auto mb-4 flex h-16 w-16 animate-bounce items-center justify-center rounded-full bg-cyan-500/10 text-cyan-400">
          <svg xmlns="http://w3.org" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-8 h-8">
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
          </svg>
        </div>

        {/* Text Styling Test */}
        <h1 className="bg-gradient-to-r from-cyan-400 to-blue-500 bg-clip-text text-3xl font-extrabold tracking-tight text-transparent sm:text-4xl">
          Tailwind is Active!
        </h1>
        
        <p className="mt-4 text-sm text-slate-400">
          If you see a centered dark card, gradient text, a subtle border, and a bouncing checkmark icon, your configuration is successfully loaded.
        </p>

        {/* Interactive Hover Test */}
        <div className="mt-6">
          <button className="rounded-lg bg-cyan-500 px-5 py-2.5 text-sm font-semibold text-slate-950 shadow-md transition-colors hover:bg-cyan-400 focus:outline-none focus:ring-2 focus:ring-cyan-400 focus:ring-offset-2 focus:ring-offset-slate-800">
            Hover & Click Test
          </button>
        </div>

      </div>
    </div>
  );
}
