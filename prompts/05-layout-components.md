# Prompt 05: Layout Components (Sidebar, Header, Layout)

## Context
You are building the layout shell for a Mutual Fund OMS dashboard. The layout has a fixed dark sidebar on the left and a header + content area on the right. Use the CSS variables defined in `frontend/src/styles/globals.css`. Import route constants from `frontend/src/constants/routes.js`. Use `react-router-dom` for navigation.

## Task
Create layout components in `frontend/src/components/Layout/`.

## Files to Create

### 1. `frontend/src/components/Layout/Sidebar.jsx`

**Requirements:**
- Fixed left sidebar, full viewport height, width = `var(--sidebar-width)` (260px)
- Background: `var(--sidebar-bg)` (dark gray-900)
- **Logo area** at the top: App name "MF-OMS" with a small chart/building icon (use a Unicode character or SVG inline)
- **Navigation links**: Rendered from `NAV_ITEMS` in `constants/routes.js`
  - Each item: icon area (use simple Unicode/emoji icons: 📊 Dashboard, 📋 Orders, 📦 Bulk Orders, 💰 Funds, 👥 Accounts) + label text
  - Active state: highlighted with `var(--sidebar-active-bg)` and `var(--sidebar-active-text)`
  - Use `NavLink` from `react-router-dom` for active detection
  - Hover effect: subtle background color change, smooth transition
- **Operations section** at the bottom of the sidebar (separated by a divider):
  - "New Order" button (styled as accent, links to `/orders/new`)
  - "Confirm Orders" button (styled as success outline)
  - "Book Orders" button (styled as primary outline)
  - These operations buttons call the respective API functions from `api/operationsApi.js`
  - Show loading spinner while operation in progress
  - Show success/error toast after completion
- Smooth transitions on hover, active states

### 2. `frontend/src/components/Layout/Header.jsx`

**Requirements:**
- Fixed top bar, spans from sidebar right edge to screen right edge
- Height: ~56px
- Background: white with bottom border `var(--color-gray-200)`
- Left side: Current page title (passed as prop or derived from route)
- Right side: 
  - Connection status indicator (green dot when SSE is connected, red when disconnected)
  - "Replay" button (calls `replayProjections()` from operationsApi)

### 3. `frontend/src/components/Layout/Layout.jsx`

**Requirements:**
- Wraps all page content
- Contains `<Sidebar />` and `<Header />` 
- Content area: positioned to the right of the sidebar, below the header, with padding
- Takes `children` prop for page content
- Structure:
```
┌──────────┬──────────────────────────────────┐
│          │  Header                          │
│  Sidebar │──────────────────────────────────│
│          │                                  │
│          │  {children} (page content)       │
│          │                                  │
└──────────┴──────────────────────────────────┘
```

### 4. `frontend/src/components/Layout/Layout.css`

All component-specific styles for the layout. Use CSS variables from globals.css.

**Key styles:**
```css
.layout { display: flex; min-height: 100vh; }

.sidebar {
  position: fixed;
  left: 0; top: 0; bottom: 0;
  width: var(--sidebar-width);
  background: var(--sidebar-bg);
  color: var(--sidebar-text);
  display: flex;
  flex-direction: column;
  z-index: 100;
  overflow-y: auto;
}

.sidebar-logo { /* App branding area */ }
.sidebar-nav { /* Navigation links list, flex-grow: 1 */ }
.sidebar-nav-item { /* Individual nav link */ }
.sidebar-nav-item.active { /* Active state */ }
.sidebar-operations { /* Bottom operations section */ }

.main-content {
  margin-left: var(--sidebar-width);
  flex: 1;
  display: flex;
  flex-direction: column;
}

.header { /* Top header bar */ }
.page-content {
  padding: var(--spacing-6);
  flex: 1;
}
```

### 5. Update `frontend/src/App.jsx`
Wrap routes in `<Layout>`:
```jsx
import Layout from './components/Layout/Layout'
// ... page imports

function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          {/* ... other routes */}
        </Routes>
      </Layout>
    </BrowserRouter>
  )
}
```

## Design Notes
- Sidebar should feel premium — dark background, subtle hover glows, smooth transitions
- Use `transition: all var(--transition-fast)` on interactive elements
- Active nav item should have a left accent border (3px solid var(--color-primary-500))
- Operations buttons should be compact and clearly labeled
- The layout should be responsive: sidebar can collapse on narrow screens (optional enhancement)
