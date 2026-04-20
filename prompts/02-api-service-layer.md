# Prompt 02: API Service Layer

## Context
You are building the API service layer for a Mutual Fund OMS React frontend. The backend runs at `http://localhost:8080` (proxied via Vite in dev). All responses are JSON. The API layer should be clean, centralized, and avoid hardcoded URLs.

## Task
Create all API service files in `frontend/src/api/`.

## Files to Create

### 1. `frontend/src/api/client.js`
A base HTTP client wrapper using `fetch`. All API calls go through this.

```js
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

async function request(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  };

  const response = await fetch(url, config);

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(error.message || `HTTP ${response.status}`);
  }

  return response.json();
}

export const api = {
  get: (endpoint) => request(endpoint),
  post: (endpoint, data) => request(endpoint, {
    method: 'POST',
    body: JSON.stringify(data),
  }),
};
```

### 2. `frontend/src/api/ordersApi.js`
```js
import { api } from './client';

// Plan (create) new orders
// Payload: array of { productID, amount, accountID, orderSide }
export const planOrders = (orders) => api.post('/orders/plan', orders);

// List all raw orders
export const listOrders = () => api.get('/orders');

// Get status of a specific order
export const getOrderStatus = (orderID) => api.get(`/orders/status?orderID=${encodeURIComponent(orderID)}`);

// Get projected order views (from CQRS read model)
// Supports optional filters: orderID, accountID, fundID, bulkOrderID
export const getOrderViews = (filters = {}) => {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.append(key, value);
  });
  const query = params.toString();
  return api.get(`/view/orders${query ? `?${query}` : ''}`);
};
```

### 3. `frontend/src/api/bulkOrdersApi.js`
```js
import { api } from './client';

// Get projected bulk order views
// Supports optional filters: bulkOrderID, fundID
export const getBulkOrderViews = (filters = {}) => {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.append(key, value);
  });
  const query = params.toString();
  return api.get(`/view/bulk-orders${query ? `?${query}` : ''}`);
};
```

### 4. `frontend/src/api/dashboardApi.js`
```js
import { api } from './client';

// Get dashboard summary (totalOrders, totalBulkOrders, ordersByStatus, etc.)
export const getDashboard = () => api.get('/view/dashboard');
```

### 5. `frontend/src/api/aggregatesApi.js`
```js
import { api } from './client';

// Get account-level aggregates
// Returns: Array of { accountID, orderCount, totalAmount, totalQuantity, statuses }
export const getAccountAggregates = () => api.get('/view/aggregates/accounts');

// Get fund-level aggregates
// Returns: Array of { fundID, fundName, orderCount, totalAmount, totalQuantity, orderSides, nav }
export const getFundAggregates = () => api.get('/view/aggregates/funds');
```

### 6. `frontend/src/api/operationsApi.js`
```js
import { api } from './client';

// Confirm all BULKED bulk orders and their individual orders
export const confirmOrders = () => api.post('/orders/confirm', {});

// Book all CONFIRMED bulk orders
export const bookOrders = () => api.post('/orders/book', {});

// Replay projections from write model → read model
export const replayProjections = () => api.post('/view/replay', {});
```

### 7. `frontend/src/api/index.js`
Re-export everything for convenient imports:
```js
export * from './ordersApi';
export * from './bulkOrdersApi';
export * from './dashboardApi';
export * from './aggregatesApi';
export * from './operationsApi';
```

## Important Notes
- **No hardcoded URLs**: All paths are relative (proxy handles routing in dev)
- The `VITE_API_BASE_URL` env var can be set for production builds to point to the backend
- All functions return Promises
- Error handling is centralized in `client.js`
- Do NOT create any mock data in the API layer — all data comes from the backend
