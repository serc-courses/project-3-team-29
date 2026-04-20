# MF-OMS Invest — User Frontend

A mobile-first investor app for the Mutual Fund Order Management System. Built with React 18 + Vite, it runs alongside the existing admin dashboard as a separate project on a different port.

## Quick Start

```bash
# From project root — backend must be running on :8080
cd user-frontend
npm install
npm run dev
# → http://localhost:5174
```

## Tech Stack

| | |
|---|---|
| Framework | React 18 + Vite 5 |
| Routing | React Router v6 |
| Styling | Custom CSS (no component library) |
| Icons | Inline SVG only |
| Fonts | Inter + JetBrains Mono (Google Fonts) |
| Backend | Shared OMS at `http://localhost:8080` |

## Pages & Routes

| Route | Page | Auth |
|---|---|---|
| `/login` | Account login | Public |
| `/` | Portfolio overview (Home) | Protected |
| `/orders` | Order history with filters | Protected |
| `/orders/new` | Place a new order | Protected |
| `/orders/:orderId` | Order detail + progress tracker | Protected |
| `/funds` | Browse mutual funds | Protected |
| `/funds/:fundId` | Fund detail + invest CTA | Protected |
| `/account` | Account selector + status breakdown | Protected |

## Authentication

There are no passwords in the system. Login works by selecting an investor account from a dropdown populated via `GET /accounts`. The selected account is validated via `POST /auth/login` which checks the account exists in the backend database. On success, `{ accountID, accountName }` is stored in `localStorage`.

**Test accounts (seeded by backend):**

| Account ID | Name |
|---|---|
| ACCT00001 | John Miller |
| ACCT00002 | Emma Johnson |
| ACCT00003 | Liam Davis |
| ACCT00004 | Olivia Brown |
| ACCT00005 | Noah Wilson |
| ACCT00006 | Ava Moore |
| ACCT00007 | William Taylor |
| ACCT00008 | Sophia Anderson |
| ACCT00009 | James Thomas |
| ACCT00010 | Isabella Jackson |

## Backend API Endpoints Used

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/accounts` | Fetch investor accounts for login dropdown |
| `POST` | `/auth/login` | Validate account and start session |
| `GET` | `/view/orders` | Fetch order list (with optional filters) |
| `POST` | `/orders/plan` | Place a new order |
| `GET` | `/orders/status` | Get single order status |
| `GET` | `/view/aggregates/funds` | Fund list with NAV and stats |
| `GET` | `/view/aggregates/accounts` | Account aggregates |
| `GET` | `/view/dashboard` | Portfolio summary for home page |
| `GET` | `/view/stream` | SSE stream for real-time updates |

## Project Structure

```
src/
├── api/              # Fetch wrappers (client, orders, portfolio, auth)
├── components/
│   ├── BottomNav/    # 4-tab mobile bottom navigation
│   ├── OrderCard/    # Tappable order summary card
│   ├── FundCard/     # Fund info card with stats
│   ├── StatusPill/   # Colored order status badge
│   ├── AmountDisplay/# Formatted ₹ currency display
│   ├── EmptyState/   # Empty state with optional CTA
│   ├── Sheet/        # Bottom sheet / modal
│   ├── Toast/        # Top-slide notification toast
│   └── ProtectedRoute/ # Auth guard wrapper
├── constants/        # Order statuses, colors, routes, config
├── context/
│   └── AuthContext/  # Login state + localStorage persistence
├── hooks/
│   ├── useFetch.js   # Generic data-fetching hook with loading/error
│   ├── useSse.js     # SSE connection with exponential backoff reconnect
│   └── useAccount.js # Selected account with localStorage persistence
├── pages/
│   ├── Login/        # Account selection login form
│   ├── Home/         # Portfolio overview
│   ├── Orders/       # Order list with filter chips + search
│   ├── OrderDetail/  # Order progress stepper
│   ├── Funds/        # Fund browser with search
│   ├── FundDetail/   # Fund info + orders + invest CTA
│   ├── PlaceOrder/   # New order form with success sheet
│   └── Account/      # Account switcher + CSS bar chart
├── styles/
│   ├── globals.css   # CSS variables, reset, typography
│   └── components.css# Cards, buttons, inputs, skeletons, toasts
└── utils/
    └── formatters.js # Currency (₹), quantity, compact (L/Cr), ID truncation
```

## Design System

Colors follow a consumer fintech palette (emerald primary on slate neutral):

- **Primary:** `#059669` (emerald-600) — CTAs, active states
- **Background:** `#F8FAFC` (slate-50) — page background
- **Surface:** `#FFFFFF` — cards
- **Buy:** `#059669` green · **Sell:** `#DC2626` red
- **Success:** `#10B981` · **Warning:** `#F59E0B` · **Error:** `#EF4444`

All interactive elements meet the 44px minimum tap target (Apple HIG). Touch feedback uses `scale(0.97–0.98)` on `:active`.

## Real-Time Updates

The app connects to the backend SSE stream (`/view/stream`) on load. When an `order-updated`, `bulk-order-updated`, or `replay-completed` event is received, the Home and Orders pages automatically re-fetch their data. The connection status is shown as a small dot in the top-right corner (green = connected, red = disconnected). Reconnection uses exponential backoff: 1s → 2s → 4s → … → 30s max.

## Differences from Admin Frontend

| | Admin (`frontend/`) | User (`user-frontend/`) |
|---|---|---|
| Port | 5173 | 5174 |
| Layout | Desktop sidebar | Mobile bottom tabs |
| Design | Dense data tables | Card-based, light |
| Auth | None | Account-based login |
| Operations | Confirm, book, replay | View, place orders only |
