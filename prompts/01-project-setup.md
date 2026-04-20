# Prompt 01: Frontend Project Setup

## Context
You are building a frontend UI for a Mutual Fund Order Management System (OMS). The backend is a Java application running at `http://localhost:8080` with REST APIs and SSE streaming. The frontend will be a **separate** React application inside a `frontend/` directory at the project root (alongside the existing `src/`, `pom.xml`, etc.).

## Task
Create a **React + Vite** project in the `frontend/` directory. Use JavaScript (JSX), not TypeScript.

## Requirements

### 1. Initialize the project
```bash
cd project-3-team-29
npx -y create-vite@latest frontend -- --template react
cd frontend
npm install
```

### 2. Install dependencies
```bash
npm install react-router-dom recharts
```

### 3. Configure Vite Proxy
In `frontend/vite.config.js`, add a proxy to forward API calls to the backend during development:

```js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
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

### 4. Create the folder structure
```
frontend/src/
├── api/          # API service functions
├── components/   # Reusable UI components
│   ├── Layout/
│   ├── DataTable/
│   ├── StatusBadge/
│   ├── SummaryCard/
│   └── Charts/
├── pages/        # Page-level components
├── hooks/        # Custom React hooks
├── constants/    # Configuration, enums, colors
├── styles/       # Global CSS, design tokens
├── utils/        # Formatters, helpers
├── App.jsx
└── main.jsx
```

Create placeholder `index.js` files in each subdirectory (empty exports).

### 5. Set up React Router in `App.jsx`
```jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom'

// Pages (will be created in later prompts)
// For now, create simple placeholder components

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<div>Dashboard</div>} />
        <Route path="/orders" element={<div>Orders</div>} />
        <Route path="/bulk-orders" element={<div>Bulk Orders</div>} />
        <Route path="/funds" element={<div>Funds</div>} />
        <Route path="/accounts" element={<div>Accounts</div>} />
        <Route path="/orders/new" element={<div>New Order</div>} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
```

### 6. Add Google Font to `index.html`
Add in `<head>`:
```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
```
Set `<title>` to `Mutual Fund OMS`.

### 7. Update `.gitignore` at project root
Append to the existing `.gitignore`:
```
# Frontend
frontend/node_modules/
frontend/dist/
```

## Verification
After setup, run:
```bash
cd frontend
npm run dev
```
The app should launch at `http://localhost:5173` and display placeholder route text.

## Important Notes
- Do NOT modify any Java files
- Do NOT rename any existing directories
- The `frontend/` directory is a sibling of `src/`, `pom.xml`, `scripts/`, etc.
