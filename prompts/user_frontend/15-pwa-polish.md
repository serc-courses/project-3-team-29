# Prompt 15: User Frontend — PWA Polish & Final Touch-ups

## Context
Final polish pass to make the user-frontend feel like a real mobile web app: PWA manifest, proper meta tags, page transitions, loading consistency, and touch feedback.

## Task
Apply final polish to the user-frontend app.

## Files to Create/Update

### 1. `user-frontend/public/manifest.json`
```json
{
  "name": "MF-OMS Invest",
  "short_name": "MF Invest",
  "description": "Mutual Fund Order Management",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#F8FAFC",
  "theme_color": "#059669",
  "orientation": "portrait",
  "icons": [
    {
      "src": "/icon-192.png",
      "sizes": "192x192",
      "type": "image/png"
    },
    {
      "src": "/icon-512.png",
      "sizes": "512x512",
      "type": "image/png"
    }
  ]
}
```

### 2. Update `user-frontend/index.html`

Ensure the following meta tags and link tags are present in `<head>`:

```html
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover" />
<meta name="theme-color" content="#059669" />
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style" content="default" />
<meta name="apple-mobile-web-app-title" content="MF Invest" />
<meta name="description" content="Mutual Fund Order Management - Mobile Investor App" />

<link rel="manifest" href="/manifest.json" />
<link rel="icon" type="image/svg+xml" href="/vite.svg" />

<title>MF-OMS Invest</title>
```

### 3. Page Transition Animations

Add to `user-frontend/src/styles/globals.css`:

```css
/* Page transition: fade + slight slide up */
.page-enter {
  opacity: 0;
  transform: translateY(8px);
}

.page-enter-active {
  opacity: 1;
  transform: translateY(0);
  transition: opacity var(--duration-normal) var(--ease-out),
              transform var(--duration-normal) var(--ease-out);
}
```

Each page's root div should have class `page` and apply the animation on mount:

```css
.page {
  animation: pageIn var(--duration-normal) var(--ease-out);
}

@keyframes pageIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
```

### 4. Touch Feedback

Add to `user-frontend/src/styles/components.css`:

```css
/* Tap highlight for interactive elements */
.tappable {
  -webkit-tap-highlight-color: transparent;
  cursor: pointer;
  transition: transform var(--duration-fast) var(--ease-out);
}

.tappable:active {
  transform: scale(0.97);
}

/* Apply to cards, buttons, nav items */
.order-card,
.fund-card,
.nav-item,
.btn {
  -webkit-tap-highlight-color: transparent;
}

.order-card:active,
.fund-card:active {
  transform: scale(0.98);
  transition: transform 0.1s ease;
}
```

### 5. Safe Area Handling

```css
/* iOS safe area insets */
.app-shell {
  padding-top: env(safe-area-inset-top);
}

.bottom-nav {
  padding-bottom: env(safe-area-inset-bottom);
}

.sticky-cta {
  padding-bottom: calc(var(--sp-4) + env(safe-area-inset-bottom));
}
```

### 6. Pull-to-Refresh Visual Cue (CSS only)

Add to `globals.css`:
```css
/* Overscroll indicator */
html {
  overscroll-behavior-y: contain;
}
```

This prevents the default browser pull-to-refresh and bounce effects.

### 7. Scrollbar Styling (for Android/desktop)

```css
/* Hide scrollbar on mobile, subtle on desktop */
.app-main {
  scrollbar-width: thin;
  scrollbar-color: var(--color-text-300) transparent;
}

.app-main::-webkit-scrollbar {
  width: 4px;
}

.app-main::-webkit-scrollbar-thumb {
  background: var(--color-text-300);
  border-radius: var(--radius-full);
}
```

### 8. Loading Skeleton Consistency Check

Every page must show a skeleton loading state while data loads. Verify:
- Home: skeleton hero + summary row + recent orders
- Orders: skeleton filter chips + 3 order cards
- Funds: skeleton search bar + 4 fund cards
- Account: skeleton dropdown + hero + status bars
- Order Detail: skeleton top bar + stepper
- Fund Detail: skeleton hero + stats + orders

Skeleton pattern (from globals.css):
```css
.skeleton {
  background: linear-gradient(90deg, var(--color-bg-secondary) 25%, var(--color-bg-tertiary) 50%, var(--color-bg-secondary) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-md);
}
```

### 9. Final Route Configuration

Confirm the complete route setup in `App.jsx`:

```jsx
<Routes>
  <Route path="/" element={<Home sseEventCount={eventCount} />} />
  <Route path="/orders" element={<Orders sseEventCount={eventCount} />} />
  <Route path="/orders/new" element={<PlaceOrder />} />
  <Route path="/orders/:orderId" element={<OrderDetail />} />
  <Route path="/funds" element={<Funds />} />
  <Route path="/funds/:fundId" element={<FundDetail />} />
  <Route path="/account" element={<Account />} />
  <Route path="*" element={<Navigate to="/" replace />} />
</Routes>
```

### 10. Bottom Nav Visibility Rules

Bottom nav shows on:
- `/` (Home)
- `/orders` (Orders list)
- `/funds` (Funds browse)
- `/account` (Account)

Bottom nav hides on:
- `/orders/new` (Place Order)
- `/orders/:orderId` (Order Detail)
- `/funds/:fundId` (Fund Detail)

## Important Notes
- Do NOT add a service worker — just the manifest for "Add to Home Screen"
- PWA icons can be placeholder (use a simple emerald circle SVG or Vite default)
- `user-scalable=no` prevents zoom on double-tap (intentional for app-like feel)
- `viewport-fit=cover` is required for `env(safe-area-inset-*)` to work on iOS
- `overscroll-behavior-y: contain` prevents pull-to-refresh on Android Chrome
- Page animations should be subtle (8px translate, not dramatic)
- Touch feedback `scale(0.97)` is barely perceptible but gives tactile response
- This is the final prompt — after this, the user-frontend should be fully functional
