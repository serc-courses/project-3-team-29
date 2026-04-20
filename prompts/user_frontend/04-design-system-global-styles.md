# Prompt 04: User Frontend — Design System & Global Styles

## Context
Setting up the visual foundation for the user-facing mobile web app. This must feel like a consumer fintech app (Groww, Zerodha, Revolut) — clean, light, card-based, mobile-first.

## Task
Create `user-frontend/src/styles/globals.css` and `user-frontend/src/styles/components.css`.

## Design Principles
- **Mobile-first**: base styles target 375px width, scale up
- **No emojis**: all icons are inline SVGs or CSS shapes
- **Card-based**: content in rounded white cards on a light gray background
- **Thumb-friendly**: minimum tap target 44px height
- **Consumer fintech**: clean emerald accent on neutral slate surfaces

## Files to Create

### `user-frontend/src/styles/globals.css`

```css
:root {
  /* Primary — Emerald */
  --color-primary: #059669;
  --color-primary-50: #ECFDF5;
  --color-primary-100: #D1FAE5;
  --color-primary-200: #A7F3D0;
  --color-primary-500: #10B981;
  --color-primary-600: #059669;
  --color-primary-700: #047857;
  --color-primary-800: #065F46;

  /* Surfaces */
  --color-bg: #F8FAFC;
  --color-bg-secondary: #F1F5F9;
  --color-surface: #FFFFFF;
  --color-surface-raised: #FFFFFF;

  /* Text — Slate scale */
  --color-text-900: #0F172A;
  --color-text-700: #334155;
  --color-text-500: #64748B;
  --color-text-400: #94A3B8;
  --color-text-300: #CBD5E1;

  /* Semantic */
  --color-success: #10B981;
  --color-warning: #F59E0B;
  --color-error: #EF4444;
  --color-info: #3B82F6;

  /* Buy / Sell */
  --color-buy: #059669;
  --color-sell: #DC2626;

  /* Borders */
  --color-border: #E2E8F0;
  --color-border-light: #F1F5F9;
  --color-divider: #F1F5F9;

  /* Typography */
  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', monospace;

  --text-xs: 0.6875rem;    /* 11px */
  --text-sm: 0.8125rem;    /* 13px */
  --text-base: 0.9375rem;  /* 15px */
  --text-lg: 1.0625rem;    /* 17px */
  --text-xl: 1.25rem;      /* 20px */
  --text-2xl: 1.5rem;      /* 24px */
  --text-3xl: 1.875rem;    /* 30px */

  /* Spacing */
  --sp-1: 4px;
  --sp-2: 8px;
  --sp-3: 12px;
  --sp-4: 16px;
  --sp-5: 20px;
  --sp-6: 24px;
  --sp-8: 32px;
  --sp-10: 40px;
  --sp-12: 48px;

  /* Radius */
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --radius-full: 9999px;

  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-md: 0 2px 8px rgba(0, 0, 0, 0.06);
  --shadow-lg: 0 4px 16px rgba(0, 0, 0, 0.08);
  --shadow-xl: 0 8px 32px rgba(0, 0, 0, 0.12);

  /* Transitions */
  --ease-out: cubic-bezier(0.16, 1, 0.3, 1);
  --duration-fast: 150ms;
  --duration-normal: 250ms;
  --duration-slow: 400ms;

  /* Layout */
  --bottom-nav-height: 64px;
  --safe-area-bottom: env(safe-area-inset-bottom, 0px);
}

*, *::before, *::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html {
  font-size: 16px;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  -webkit-tap-highlight-color: transparent;
}

body {
  font-family: var(--font-sans);
  background: var(--color-bg);
  color: var(--color-text-700);
  line-height: 1.5;
  overscroll-behavior-y: contain;
}

/* Utility classes */
.font-mono { font-family: var(--font-mono); font-feature-settings: 'tnum'; }
.text-xs { font-size: var(--text-xs); }
.text-sm { font-size: var(--text-sm); }
.text-base { font-size: var(--text-base); }
.text-lg { font-size: var(--text-lg); }
.text-xl { font-size: var(--text-xl); }
.text-2xl { font-size: var(--text-2xl); }

.text-primary { color: var(--color-primary); }
.text-muted { color: var(--color-text-500); }
.text-light { color: var(--color-text-400); }
.text-success { color: var(--color-success); }
.text-error { color: var(--color-error); }
.text-buy { color: var(--color-buy); }
.text-sell { color: var(--color-sell); }

.font-medium { font-weight: 500; }
.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }

/* Custom scrollbar — thin, subtle */
::-webkit-scrollbar { width: 4px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--color-text-300); border-radius: var(--radius-full); }

/* Focus visible — accessible */
:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* Selection */
::selection {
  background: var(--color-primary-100);
  color: var(--color-primary-800);
}
```

### `user-frontend/src/styles/components.css`

```css
/* Page container */
.page {
  padding: var(--sp-4);
  padding-bottom: calc(var(--sp-4) + var(--bottom-nav-height) + var(--safe-area-bottom));
}

.page-title {
  font-size: var(--text-xl);
  font-weight: 700;
  color: var(--color-text-900);
  letter-spacing: -0.02em;
  margin-bottom: var(--sp-2);
}

.page-subtitle {
  font-size: var(--text-sm);
  color: var(--color-text-500);
  margin-bottom: var(--sp-5);
}

/* Card */
.card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--sp-4);
  transition: box-shadow var(--duration-fast) var(--ease-out);
}

.card-elevated {
  box-shadow: var(--shadow-sm);
  border-color: transparent;
}

.card-interactive {
  cursor: pointer;
  -webkit-user-select: none;
  user-select: none;
}

.card-interactive:active {
  transform: scale(0.98);
  transition: transform 100ms var(--ease-out);
}

/* Button */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--sp-2);
  font-family: var(--font-sans);
  font-size: var(--text-base);
  font-weight: 600;
  border: none;
  cursor: pointer;
  border-radius: var(--radius-sm);
  padding: var(--sp-3) var(--sp-5);
  min-height: 44px;
  transition: all var(--duration-fast) var(--ease-out);
  text-decoration: none;
  -webkit-user-select: none;
  user-select: none;
}

.btn:active:not(:disabled) {
  transform: scale(0.97);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: var(--color-primary);
  color: white;
}

.btn-primary:active:not(:disabled) {
  background: var(--color-primary-700);
}

.btn-secondary {
  background: var(--color-bg-secondary);
  color: var(--color-text-700);
}

.btn-outline {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-700);
}

.btn-ghost {
  background: transparent;
  color: var(--color-primary);
}

.btn-danger {
  background: var(--color-error);
  color: white;
}

.btn-full {
  width: 100%;
}

.btn-lg {
  padding: var(--sp-4) var(--sp-6);
  font-size: var(--text-lg);
  min-height: 52px;
  border-radius: var(--radius-md);
}

/* Input */
.input-group {
  margin-bottom: var(--sp-4);
}

.input-label {
  display: block;
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-700);
  margin-bottom: var(--sp-1);
}

.input, .select {
  width: 100%;
  padding: var(--sp-3) var(--sp-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-family: var(--font-sans);
  font-size: var(--text-base);
  color: var(--color-text-900);
  background: var(--color-surface);
  min-height: 44px;
  transition: border-color var(--duration-fast);
  outline: none;
  appearance: none;
  -webkit-appearance: none;
}

.input:focus, .select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-100);
}

.input-error {
  border-color: var(--color-error);
}

.input-error:focus {
  box-shadow: 0 0 0 3px #FEE2E2;
}

.input-hint {
  font-size: var(--text-xs);
  color: var(--color-text-400);
  margin-top: var(--sp-1);
}

.input-error-text {
  font-size: var(--text-xs);
  color: var(--color-error);
  margin-top: var(--sp-1);
}

/* Divider */
.divider {
  height: 1px;
  background: var(--color-divider);
  margin: var(--sp-4) 0;
}

/* Section */
.section {
  margin-bottom: var(--sp-6);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--sp-3);
}

.section-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-text-900);
}

.section-action {
  font-size: var(--text-sm);
  color: var(--color-primary);
  font-weight: 500;
  cursor: pointer;
  background: none;
  border: none;
  font-family: var(--font-sans);
}

/* Loading skeleton */
.skeleton {
  background: linear-gradient(90deg, var(--color-bg-secondary) 25%, #E2E8F0 50%, var(--color-bg-secondary) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.skeleton-text { height: 14px; margin-bottom: var(--sp-2); }
.skeleton-text-sm { height: 10px; margin-bottom: var(--sp-1); width: 60%; }
.skeleton-heading { height: 24px; margin-bottom: var(--sp-3); width: 40%; }
.skeleton-card { height: 80px; border-radius: var(--radius-md); }
.skeleton-circle { border-radius: 50%; }

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

/* Spinner */
.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Empty state container */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--sp-10) var(--sp-6);
  text-align: center;
}

.empty-state-icon {
  width: 64px;
  height: 64px;
  margin-bottom: var(--sp-4);
  color: var(--color-text-300);
}

.empty-state-title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-700);
  margin-bottom: var(--sp-2);
}

.empty-state-text {
  font-size: var(--text-sm);
  color: var(--color-text-400);
  max-width: 280px;
  margin-bottom: var(--sp-5);
}

/* Toast */
.toast {
  position: fixed;
  top: var(--sp-4);
  left: var(--sp-4);
  right: var(--sp-4);
  max-width: 448px;
  margin: 0 auto;
  padding: var(--sp-3) var(--sp-4);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: 500;
  box-shadow: var(--shadow-lg);
  z-index: 9999;
  animation: toastSlideDown var(--duration-normal) var(--ease-out);
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}

.toast-success { background: var(--color-success); color: white; }
.toast-error { background: var(--color-error); color: white; }
.toast-warning { background: #92400E; color: white; }
.toast-info { background: var(--color-text-900); color: white; }

@keyframes toastSlideDown {
  from { transform: translateY(-100%); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
```

### `user-frontend/src/styles/index.js`
```js
// Styles are imported directly in main.jsx, this is just a barrel
```

## Important Notes
- Font sizes are slightly smaller than admin (mobile context) — 15px base instead of 16px
- All interactive elements have `min-height: 44px` for touch targets (Apple HIG)
- `overscroll-behavior-y: contain` prevents pull-to-refresh on the body (we'll add our own)
- Toast appears from the TOP (mobile pattern) not bottom like admin
- Skeleton loading is built-in from the start — every page should use it
- `-webkit-tap-highlight-color: transparent` removes the blue tap flash on mobile
- `100dvh` handles the dynamic viewport on mobile Safari (address bar hide/show)
- Border radius is larger than admin (12px default vs 8px) for a softer mobile feel
- Do NOT use `hover` pseudo-class for primary interactions — use `active` (touch devices)
