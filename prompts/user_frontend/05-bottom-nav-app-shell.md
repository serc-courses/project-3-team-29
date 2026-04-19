# Prompt 05: User Frontend — Bottom Navigation & App Shell

## Context
The user app uses a mobile bottom tab bar (like iOS/Android) instead of a sidebar. The app shell wraps all pages with the bottom nav persistently visible except on certain routes (like the order form).

## Task
Create the BottomNav component and update App.jsx to use it as the app shell.

## Page Layout

```
┌─────────────────────────────┐
│                             │
│         Page Content        │
│         (scrollable)        │
│                             │
│                             │
├─────────────────────────────┤
│  🏠        📋       📈  👤  │
│  Home    Orders   Funds Acct│
│                             │  ← safe area padding at bottom
└─────────────────────────────┘
```

## Files to Create

### `user-frontend/src/components/BottomNav/BottomNav.jsx`

Build a bottom tab bar with 4 tabs:

| Tab | Label | Icon (inline SVG) | Route |
|-----|-------|--------------------|-------|
| 1 | Home | House/home icon | `/` |
| 2 | Orders | List/receipt icon | `/orders` |
| 3 | Funds | Trending-up/chart icon | `/funds` |
| 4 | Account | User/person icon | `/account` |

Requirements:
1. Use `NavLink` from react-router-dom for each tab
2. Active tab: primary emerald color (`var(--color-primary)`), inactive: `var(--color-text-400)`
3. Each tab icon is a small inline SVG (24×24px viewBox, stroke-based, 2px stroke)
4. Label below icon in `var(--text-xs)` size
5. Fixed to bottom of viewport, full width, with `safe-area-inset-bottom` padding
6. White background with top border and subtle shadow
7. The nav should NOT show on `/orders/new` route (check `useLocation`)
8. Minimum tap target: 44px height per tab
9. Active tab has a subtle scale(1.05) transform on the icon
10. No emojis — all icons must be inline SVG elements

### `user-frontend/src/components/BottomNav/BottomNav.css`
```css
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  max-width: 480px;
  margin: 0 auto;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: var(--bottom-nav-height);
  padding-bottom: var(--safe-area-bottom);
  z-index: 100;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
}

.bottom-nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex: 1;
  min-height: 44px;
  padding: var(--sp-1) 0;
  text-decoration: none;
  color: var(--color-text-400);
  font-size: var(--text-xs);
  font-weight: 500;
  transition: color var(--duration-fast);
  -webkit-user-select: none;
  user-select: none;
}

.bottom-nav-item.active {
  color: var(--color-primary);
}

.bottom-nav-icon {
  width: 24px;
  height: 24px;
  transition: transform var(--duration-fast) var(--ease-out);
}

.bottom-nav-item.active .bottom-nav-icon {
  transform: scale(1.1);
}

.bottom-nav-label {
  line-height: 1;
}
```

### `user-frontend/src/components/BottomNav/index.js`
```js
export { default } from './BottomNav'
```

### Update `user-frontend/src/App.jsx`

Update the App component to:
1. Include the BottomNav component
2. Set up all routes from the routes constant
3. Keep the `app-shell` and `app-main` structure from prompt 01
4. Pass SSE connection state down (placeholder for now)

```jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import BottomNav from './components/BottomNav/BottomNav'
import './App.css'

// Placeholder pages
function Placeholder({ name }) {
  return (
    <div className="page">
      <h1 className="page-title">{name}</h1>
      <p className="page-subtitle">Coming soon</p>
    </div>
  )
}

function App() {
  return (
    <BrowserRouter>
      <div className="app-shell">
        <main className="app-main">
          <Routes>
            <Route path="/" element={<Placeholder name="Home" />} />
            <Route path="/orders" element={<Placeholder name="Orders" />} />
            <Route path="/orders/new" element={<Placeholder name="Place Order" />} />
            <Route path="/orders/:orderId" element={<Placeholder name="Order Detail" />} />
            <Route path="/funds" element={<Placeholder name="Funds" />} />
            <Route path="/funds/:fundId" element={<Placeholder name="Fund Detail" />} />
            <Route path="/account" element={<Placeholder name="Account" />} />
          </Routes>
        </main>
        <BottomNav />
      </div>
    </BrowserRouter>
  )
}

export default App
```

## SVG Icon Examples

Here are the exact SVG icons to use (24×24, stroke-based):

**Home:**
```html
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
  <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
  <polyline points="9 22 9 12 15 12 15 22"/>
</svg>
```

**Orders (receipt/list):**
```html
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
  <polyline points="14 2 14 8 20 8"/>
  <line x1="8" y1="13" x2="16" y2="13"/>
  <line x1="8" y1="17" x2="12" y2="17"/>
</svg>
```

**Funds (trending up):**
```html
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
  <polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/>
  <polyline points="16 7 22 7 22 13"/>
</svg>
```

**Account (user):**
```html
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
  <circle cx="12" cy="7" r="4"/>
</svg>
```

## Important Notes
- Bottom nav is hidden on `/orders/new` route — the order form has its own back button
- On desktop (>480px), the app centers in a phone-like container — the bottom nav should respect `max-width: 480px`
- Use `NavLink` with `end` prop on the home route to avoid it always being active
- In JSX, SVG attributes must use camelCase: `strokeWidth`, `strokeLinecap`, `strokeLinejoin`, `viewBox`
- Do NOT use `<img>` tags for icons — everything inline for color control via `currentColor`
