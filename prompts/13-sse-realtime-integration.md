# Prompt 13: Real-Time SSE Integration

## Context
The backend provides a Server-Sent Events (SSE) endpoint at `GET /view/stream` for real-time updates. When orders are created, confirmed, or booked, the backend pushes events to all connected SSE clients. The frontend should subscribe to this stream and automatically refresh data when updates arrive.

## SSE Event Types
The backend emits these event types:
- `connected` — Initial connection confirmation
- `order-updated` — An individual order status changed
- `bulk-order-updated` — A bulk order status changed
- `replay-completed` — Projection replay finished

## Task
Create an SSE custom hook and integrate it into the Dashboard and other pages.

## Files to Create / Modify

### 1. `frontend/src/hooks/useSse.js`

Custom React hook for SSE connection management:

```js
import { useEffect, useRef, useCallback } from 'react';
import { CONFIG } from '../constants/config';

/**
 * Hook to subscribe to Server-Sent Events.
 * 
 * @param {Object} handlers - Map of event type → callback function
 *   e.g., { 'order-updated': (data) => {...}, 'bulk-order-updated': (data) => {...} }
 * @returns {{ connected: boolean }} - connection status
 */
export function useSse(handlers) {
  // Implementation:
  // 1. Create EventSource connected to CONFIG.SSE_ENDPOINT
  // 2. Register event listeners for each handler key
  // 3. Parse incoming data as JSON
  // 4. Track connection status (connected/disconnected)
  // 5. Clean up on unmount (close EventSource)
  // 6. Reconnect on error with exponential backoff (1s, 2s, 4s, max 30s)
  // 7. Return { connected } state
}
```

### 2. `frontend/src/hooks/useFetch.js`

Generic data fetching hook with refetch capability:

```js
import { useState, useEffect, useCallback } from 'react';

/**
 * Hook for fetching data from an API function.
 * 
 * @param {Function} fetchFn - async function that returns data
 * @param {Array} deps - dependency array to re-fetch on change
 * @returns {{ data, loading, error, refetch }}
 */
export function useFetch(fetchFn, deps = []) {
  // Implementation:
  // 1. Manage data, loading, error state
  // 2. Call fetchFn on mount and when deps change
  // 3. Provide refetch function for manual refresh
  // 4. Handle errors gracefully
}
```

### 3. Integrate SSE into Dashboard

Modify `frontend/src/pages/Dashboard.jsx`:
- Use `useSse` hook to listen for `order-updated` and `bulk-order-updated` events
- When any event arrives, call `refetch()` from `useFetch` to reload dashboard data
- Show the connection status indicator in the header (green dot = connected, red = disconnected)

```jsx
import { useSse } from '../hooks/useSse';
import { useFetch } from '../hooks/useFetch';

function Dashboard() {
  const { data, loading, error, refetch } = useFetch(getDashboard);
  
  const { connected } = useSse({
    'order-updated': () => refetch(),
    'bulk-order-updated': () => refetch(),
    'replay-completed': () => refetch(),
  });
  
  // ... render dashboard with data
}
```

### 4. (Optional) Integrate SSE into Orders and Bulk Orders pages similarly

Same pattern: listen for events, refetch data on update.

## Important Notes
- **SSE_ENDPOINT** is `/view/stream` — use it from `CONFIG.SSE_ENDPOINT`, do NOT hardcode
- The Vite proxy already forwards `/view/*` requests to the backend
- EventSource is a long-lived connection — only create ONE per page, not multiple
- Clean up the EventSource on component unmount to prevent memory leaks
- The backend sends keepalive comments every 15 seconds — these should be silently ignored
- Error handling: if the SSE connection fails, show a disconnected indicator and retry
- SSE data payloads contain fields like:
  ```json
  {
    "orderID": "ORD900",
    "accountID": "ACCT00001",
    "fundID": "FND001",
    "orderStatus": "BOOKED",
    "amount": 1000.00,
    "quantity": 87.72,
    "bulkOrderID": "BLK500"
  }
  ```
  But we don't need to use the payload data directly — just trigger a refetch when events arrive.
