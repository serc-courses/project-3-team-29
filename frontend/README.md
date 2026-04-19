# Mutual Fund OMS — Frontend

React + Vite frontend for the Mutual Fund Order Management System. Connects to the Java backend at `http://localhost:8080`.

---

## Prerequisites

- Node.js 18+
- Backend running at `http://localhost:8080` (see root `README.md` or run `mvn spring-boot:run` / the main OMS launcher)

---

## Running the Frontend

```bash
cd frontend
npm install       # only needed once
npm run dev
```

Opens at **http://localhost:5173**

The Vite dev server proxies all `/orders/*` and `/view/*` requests to `http://localhost:8080`, so no CORS issues in dev.

---

## Pages to Test

### `/` — Dashboard
- Shows 4 summary cards: Total Orders, Total Bulk Orders, Booked Orders, Errored Orders
- Bar chart of order status distribution
- Donut chart of bulk order status distribution
- Table of the 10 most recent orders
- **Auto-refreshes** every 30s and also on SSE events (real-time)
- Green dot in the top-right = SSE live connection

### `/orders` — Orders
- Full table of all individual orders from the CQRS read model
- Filter by: Status, Account, Side (BUY/SELL)
- Search by Order ID
- Shows "Showing X of Y orders" count
- Click **+ New Order** to go to the order form

### `/bulk-orders` — Bulk Orders
- Table of all bulk orders
- Filter by: Status, Side
- Search by Bulk ID
- **Click any row** to expand and see matched individual Order IDs (click an ID to go to orders filtered by that bulk order)

### `/funds` — Funds
- Card grid (3 cols) showing each fund's NAV, order count, total amount, BUY/SELL split, total quantity
- Search by fund name or fund ID
- **Click a card** to go to Orders filtered by that fund

### `/accounts` — Accounts
- Table of all accounts with their order aggregates
- Status breakdown column shows color-coded mini badges (e.g., `BOOKED: 5  BULKED: 3`)
- Search by Account ID
- **Click a row** to go to Orders filtered by that account

### `/orders/new` — New Order Form
- Add one or more orders in a single submission
- Each row: Fund (dropdown), Account (dropdown), Amount, Side
- Client-side validation (required fields, amount > 0)
- Summary at the bottom shows order count + total amount
- On success: shows order IDs and redirects to `/orders` after 2s

---

## Sidebar Operations

The left sidebar has three operation buttons at the bottom:

| Button | What it does |
|---|---|
| **+ New Order** | Navigates to the new order form |
| **Confirm Orders** | Calls `POST /orders/confirm` — advances all BULKED bulk orders to CONFIRMED |
| **Book Orders** | Calls `POST /orders/book` — books all CONFIRMED bulk orders |

The **Replay** button in the top-right header calls `POST /view/replay` to rebuild the read model from the write model.

---

## SSE Real-Time Updates

The app connects to `GET /view/stream` (Server-Sent Events). When orders are confirmed or booked, the Dashboard auto-refreshes without polling. Connection status is shown as a dot in the header:

- **Green** = connected
- **Red** = disconnected (reconnects automatically with exponential backoff: 1s → 2s → 4s → max 30s)

---

## Project Structure

```
frontend/src/
├── api/              # All backend API calls
│   ├── client.js         # Base fetch wrapper
│   ├── ordersApi.js
│   ├── bulkOrdersApi.js
│   ├── dashboardApi.js
│   ├── aggregatesApi.js
│   └── operationsApi.js
├── components/
│   ├── Layout/           # Sidebar, Header, Layout wrapper
│   ├── DataTable/        # Sortable table with loading/empty states
│   ├── StatusBadge/      # Colored status pill (order & bulk variants)
│   ├── SummaryCard/      # Metric card with accent bar
│   ├── Charts/           # StatusChart (bar or donut via Recharts)
│   ├── FilterBar/        # Dropdown filters + search input
│   └── Toast/            # Notification toast
├── constants/
│   ├── orderStatus.js    # ORDER_STATUS, BULK_ORDER_STATUS, ORDER_SIDE enums
│   ├── statusColors.js   # Color map for each status (used by badges & charts)
│   ├── routes.js         # Route paths + sidebar nav items
│   └── config.js         # SSE endpoint, app name
├── hooks/
│   ├── useSse.js         # SSE connection hook with auto-reconnect
│   └── useFetch.js       # Generic data fetching hook with refetch
├── pages/
│   ├── Dashboard.jsx
│   ├── Orders.jsx
│   ├── BulkOrders.jsx
│   ├── Funds.jsx
│   ├── Accounts.jsx
│   └── NewOrder.jsx
├── styles/
│   ├── globals.css       # CSS variables (design tokens) + reset
│   └── components.css    # Shared component classes (card, btn, form-input…)
└── utils/
    └── formatters.js     # formatCurrency, formatQuantity, formatCompact
```

---

## Build for Production

```bash
npm run build
```

Output goes to `frontend/dist/`. To point at a different backend, set:

```bash
VITE_API_BASE_URL=https://your-backend.example.com npm run build
```

CORS origin on the backend side can be set via the `OMS_CORS_ORIGIN` environment variable (defaults to `*`).
