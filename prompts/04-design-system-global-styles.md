# Prompt 04: Design System & Global Styles

## Context
You are building a professional, finance-grade React frontend for a Mutual Fund OMS. The design should feel premium, modern, and data-dense — similar to Bloomberg Terminal or Charles Schwab dashboards but with a clean, modern aesthetic. Use Inter font for all text and JetBrains Mono for numerical/ID data.

## Task
Create the global CSS design system in `frontend/src/styles/`.

## Files to Create

### 1. `frontend/src/styles/globals.css`
This is the foundational CSS file imported in `main.jsx`. It defines:

**CSS Variables (Design Tokens):**
```css
:root {
  /* Colors - Primary */
  --color-primary-50: #EFF6FF;
  --color-primary-100: #DBEAFE;
  --color-primary-200: #BFDBFE;
  --color-primary-500: #3B82F6;
  --color-primary-600: #2563EB;
  --color-primary-700: #1D4ED8;
  --color-primary-900: #1E3A5F;

  /* Colors - Accent */
  --color-accent: #2DD4BF;
  --color-accent-light: #CCFBF1;

  /* Colors - Semantic */
  --color-success: #10B981;
  --color-warning: #F59E0B;
  --color-error: #F43F5E;
  --color-info: #3B82F6;

  /* Colors - Neutral */
  --color-gray-50: #F9FAFB;
  --color-gray-100: #F3F4F6;
  --color-gray-200: #E5E7EB;
  --color-gray-300: #D1D5DB;
  --color-gray-400: #9CA3AF;
  --color-gray-500: #6B7280;
  --color-gray-600: #4B5563;
  --color-gray-700: #374151;
  --color-gray-800: #1F2937;
  --color-gray-900: #111827;

  /* Sidebar */
  --sidebar-width: 260px;
  --sidebar-collapsed-width: 72px;
  --sidebar-bg: var(--color-gray-900);
  --sidebar-text: var(--color-gray-300);
  --sidebar-active-bg: rgba(59, 130, 246, 0.15);
  --sidebar-active-text: var(--color-primary-500);

  /* Typography */
  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', monospace;

  --text-xs: 0.75rem;
  --text-sm: 0.875rem;
  --text-base: 1rem;
  --text-lg: 1.125rem;
  --text-xl: 1.25rem;
  --text-2xl: 1.5rem;
  --text-3xl: 1.875rem;

  /* Spacing */
  --spacing-1: 0.25rem;
  --spacing-2: 0.5rem;
  --spacing-3: 0.75rem;
  --spacing-4: 1rem;
  --spacing-5: 1.25rem;
  --spacing-6: 1.5rem;
  --spacing-8: 2rem;

  /* Borders & Shadows */
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;

  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1);

  /* Transitions */
  --transition-fast: 150ms ease;
  --transition-normal: 250ms ease;
  --transition-slow: 350ms ease;
}
```

**Reset & Base Styles:**
```css
*, *::before, *::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html {
  font-size: 16px;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

body {
  font-family: var(--font-sans);
  background: var(--color-gray-50);
  color: var(--color-gray-800);
  line-height: 1.6;
}

/* Typography utilities */
.font-mono { font-family: var(--font-mono); }
.text-xs { font-size: var(--text-xs); }
.text-sm { font-size: var(--text-sm); }
.text-lg { font-size: var(--text-lg); }
.text-xl { font-size: var(--text-xl); }
.text-2xl { font-size: var(--text-2xl); }
.text-muted { color: var(--color-gray-500); }

/* Scrollbar styling */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb {
  background: var(--color-gray-300);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover { background: var(--color-gray-400); }
```

### 2. `frontend/src/styles/components.css`
Reusable component classes:

```css
/* Card */
.card {
  background: white;
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-normal);
}
.card:hover {
  box-shadow: var(--shadow-md);
}

/* Button variants */
.btn {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-2) var(--spacing-4);
  border-radius: var(--radius-md);
  font-family: var(--font-sans);
  font-size: var(--text-sm);
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
  text-decoration: none;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: var(--color-primary-600);
  color: white;
}
.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-700);
}

.btn-success {
  background: var(--color-success);
  color: white;
}
.btn-success:hover:not(:disabled) {
  background: #059669;
}

.btn-warning {
  background: var(--color-warning);
  color: white;
}

.btn-danger {
  background: var(--color-error);
  color: white;
}

.btn-outline {
  background: transparent;
  border: 1px solid var(--color-gray-300);
  color: var(--color-gray-700);
}
.btn-outline:hover:not(:disabled) {
  background: var(--color-gray-50);
  border-color: var(--color-gray-400);
}

/* Input / Select */
.form-input, .form-select {
  width: 100%;
  padding: var(--spacing-2) var(--spacing-3);
  border: 1px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  font-family: var(--font-sans);
  font-size: var(--text-sm);
  color: var(--color-gray-800);
  background: white;
  transition: border-color var(--transition-fast);
  outline: none;
}
.form-input:focus, .form-select:focus {
  border-color: var(--color-primary-500);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

/* Label */
.form-label {
  display: block;
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-gray-700);
  margin-bottom: var(--spacing-1);
}

/* Toast notification */
.toast {
  position: fixed;
  bottom: var(--spacing-6);
  right: var(--spacing-6);
  padding: var(--spacing-3) var(--spacing-5);
  border-radius: var(--radius-md);
  color: white;
  font-size: var(--text-sm);
  font-weight: 500;
  box-shadow: var(--shadow-lg);
  z-index: 1000;
  animation: slideUp 0.3s ease;
}
.toast-success { background: var(--color-success); }
.toast-error { background: var(--color-error); }
.toast-warning { background: var(--color-warning); }

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

/* Loading spinner */
.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid var(--color-gray-200);
  border-top-color: var(--color-primary-500);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Empty state */
.empty-state {
  text-align: center;
  padding: var(--spacing-8);
  color: var(--color-gray-400);
}
```

### 3. Update `frontend/src/main.jsx`
Import the styles:
```jsx
import './styles/globals.css'
import './styles/components.css'
```

## Design Principles
1. **No hardcoded colors** in component JSX — use CSS variables or class names
2. **Data-dense but readable** — finance dashboards show lots of data; use compact spacing
3. **Monospace for IDs and numbers** — use `font-mono` class for Order IDs, amounts, quantities
4. **Subtle micro-animations** — hover effects on cards, smooth transitions on state changes
5. **Professional palette** — deep blues, grays, and muted accents; no garish colors
