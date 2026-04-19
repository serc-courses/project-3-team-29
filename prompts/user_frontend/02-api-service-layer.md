# Prompt 02: User Frontend — API Service Layer

## Context
You are building the API layer for the user-facing mobile web app. The backend is the same OMS at `http://localhost:8080`. The user frontend needs a subset of the admin APIs, organized around what an investor cares about: their orders, available funds, and their account.

## Task
Create the API service files in `user-frontend/src/api/`.

## Files to Create

### `user-frontend/src/api/client.js`
Base fetch wrapper. Same pattern as admin but simpler.

```js
const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

async function request(url, options = {}) {
  const config = {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  }

  const response = await fetch(`${BASE_URL}${url}`, config)

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `Request failed: ${response.status}`)
  }

  const contentType = response.headers.get('Content-Type') || ''
  if (contentType.includes('application/json')) {
    return response.json()
  }
  return response.text()
}

export const api = {
  get: (url) => request(url),
  post: (url, body) => request(url, { method: 'POST', body: JSON.stringify(body) }),
}
```

### `user-frontend/src/api/ordersApi.js`
Order-related API calls for the user:

```js
import { api } from './client'

// Plan (create) new orders
// POST /orders/plan
// Body: [{ productID, amount, accountID, orderSide }]
// Response: { message, count, orderIDs[] }
export function planOrders(orders) {
  return api.post('/orders/plan', orders)
}

// Get order status by ID
// GET /orders/status?orderID=ORD123
// Response: { orderID, orderStatus, errorDescription }
export function getOrderStatus(orderID) {
  return api.get(`/orders/status?orderID=${encodeURIComponent(orderID)}`)
}

// Get all order views (CQRS read model) with optional filters
// GET /view/orders?accountID=X&fundID=Y&orderID=Z
// Response: OrderView[]
export function getOrders(filters = {}) {
  const params = new URLSearchParams()
  if (filters.accountID) params.set('accountID', filters.accountID)
  if (filters.fundID) params.set('fundID', filters.fundID)
  if (filters.orderID) params.set('orderID', filters.orderID)
  if (filters.bulkOrderID) params.set('bulkOrderID', filters.bulkOrderID)
  const qs = params.toString()
  return api.get(`/view/orders${qs ? `?${qs}` : ''}`)
}
```

### `user-frontend/src/api/portfolioApi.js`
Fund and account data:

```js
import { api } from './client'

// Get fund aggregates (fund list with NAV, order stats)
// GET /view/aggregates/funds
// Response: [{ fundID, fundName, orderCount, totalAmount, totalQuantity, orderSides{}, nav }]
export function getFunds() {
  return api.get('/view/aggregates/funds')
}

// Get account aggregates
// GET /view/aggregates/accounts
// Response: [{ accountID, orderCount, totalAmount, totalQuantity, statuses{} }]
export function getAccounts() {
  return api.get('/view/aggregates/accounts')
}

// Get dashboard summary (for home page overview)
// GET /view/dashboard
// Response: { totalOrders, totalBulkOrders, ordersByStatus{}, orders[], bulkOrders[] }
export function getDashboard() {
  return api.get('/view/dashboard')
}
```

### `user-frontend/src/api/index.js`
Barrel export:

```js
export { planOrders, getOrderStatus, getOrders } from './ordersApi'
export { getFunds, getAccounts, getDashboard } from './portfolioApi'
```

## Important Notes
- The API layer is intentionally thinner than the admin frontend — no bulk order APIs, no confirm/book/replay operations (those are admin-only)
- `productID` in the plan API corresponds to `fundID` — the mapping happens in the PlaceOrder page, not here
- All filter parameters use `encodeURIComponent` for safety
- The `client.js` throws on non-OK responses — pages handle errors via try/catch or the `useFetch` hook
- Do NOT add any admin operation endpoints (confirm, book, replay) — those belong to the admin frontend only
