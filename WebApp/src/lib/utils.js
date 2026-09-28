// Utility: cn() — merges class names, handling conditional and dynamic classes cleanly.
// Mirrors the shadcn convention so any copied component that calls cn() works without changes.
export function cn(...classes) {
  return classes.filter(Boolean).join(' ');
}
