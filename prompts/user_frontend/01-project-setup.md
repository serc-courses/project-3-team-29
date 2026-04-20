# Prompt 01: User Frontend — Project Setup

## Context
You are building a **user-facing mobile web app** for a Mutual Fund OMS. This is a **separate project** from the existing admin dashboard in `frontend/`. The backend runs at `http://localhost:8080` and is shared between both frontends.

Read `prompts/user_frontend/00-architecture-overview.md` for full context on the architecture, color palette, and design principles.

## Task
Create a React + Vite project in the `user-frontend/` directory at the project root (sibling to `frontend/`). Use JavaScript (JSX), not TypeScript.

## Requirements

### 1. Initialize the project
```bash
cd project-3-team-29
npx -y create-vite@latest user-frontend -- --template react
cd user-frontend
npm install
```

### 2. Install dependencies
```bash
npm install react-router-dom
```
No other libraries. No UI frameworks, no icon libraries, no animation libraries.

### 3. Configure Vite
Create `user-frontend/vite.config.js`:

```js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,  // Different port from admin frontend (5173)
    proxy: {
      '/orders': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/view': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

Note: Port `5174` so both frontends can run simultaneously.

### 4. Configure `index.html`
Add mobile viewport meta tags and Inter font:

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <meta name="theme-color" content="#059669" />
  <meta name="apple-mobile-web-app-capable" content="yes" />
  <meta name="apple-mobile-web-app-status-bar-style" content="default" />
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet" />
  <title>MF-OMS Invest</title>
</head>
<body>
  <div id="root"></div>
  <script type="module" src="/src/main.jsx"></script>
</body>
</html>
```

### 5. Create folder structure
```
user-frontend/src/
├── api/
├── components/
│   ├── BottomNav/
│   ├── OrderCard/
│   ├── FundCard/
│   ├── StatusPill/
│   ├── AmountDisplay/
│   ├── EmptyState/
│   ├── Sheet/
│   └── Toast/
├── constants/
├── hooks/
├── pages/
│   ├── Home/
│   ├── Orders/
│   ├── PlaceOrder/
│   ├── OrderDetail/
│   ├── Funds/
│   ├── FundDetail/
│   └── Account/
├── styles/
├── utils/
├── App.jsx
├── App.css
└── main.jsx
```

Create empty `index.js` barrel files in each subdirectory under `components/`, `pages/`, `constants/`, `hooks/`, `utils/`, and `styles/`.

### 6. Set up React Router in `App.jsx`

```jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './App.css'

// Placeholder pages — will be built in later prompts
function Placeholder({ name }) {
  return <div style={{ padding: '24px', textAlign: 'center', color: '#64748B' }}>{name}</div>
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
        {/* BottomNav will go here */}
      </div>
    </BrowserRouter>
  )
}

export default App
```

### 7. Create `main.jsx`
```jsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './styles/globals.css'
import './styles/components.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
```

### 8. Create `App.css`
```css
.app-shell {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  max-width: 480px;
  margin: 0 auto;
  position: relative;
  background: var(--color-bg);
}

.app-main {
  flex: 1;
  padding-bottom: 72px; /* space for bottom nav */
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}
```

## Important Notes
- This is a **mobile-first** app — `max-width: 480px` centered on desktop, full-width on mobile
- Port `5174` to avoid conflict with the admin frontend on `5173`
- The proxy config is identical to the admin frontend — same backend APIs
- Do NOT install any icon libraries or UI component frameworks
- Use `100dvh` (dynamic viewport height) for proper mobile behavior
- Do NOT copy any code from the admin `frontend/` — this is a clean build with different architecture
