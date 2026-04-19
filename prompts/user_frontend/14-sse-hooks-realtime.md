# Prompt 14: User Frontend — SSE Real-Time Updates & Hooks

## Context
The backend sends Server-Sent Events (SSE) at `GET /view/stream` when orders are updated. The user app should auto-refresh data when events arrive, and show a connection indicator.

## Task
Create the custom hooks and integrate SSE into the app.

## Files to Create

### `user-frontend/src/hooks/useFetch.js`
Generic data-fetching hook:

```js
import { useState, useEffect, useCallback } from 'react'

export function useFetch(fetchFn, deps = []) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fetchData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await fetchFn()
      setData(result)
    } catch (err) {
      setError(err.message || 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }, deps)

  useEffect(() => {
    fetchData()
  }, [fetchData])

  return { data, loading, error, refetch: fetchData }
}
```

### `user-frontend/src/hooks/useSse.js`
SSE connection hook with auto-reconnect and exponential backoff:

```js
import { useState, useEffect, useRef, useCallback } from 'react'
import { CONFIG } from '../constants/config'

export function useSse() {
  const [connected, setConnected] = useState(false)
  const [eventCount, setEventCount] = useState(0)
  const eventSourceRef = useRef(null)
  const retryTimeoutRef = useRef(null)
  const retryDelayRef = useRef(1000)

  const connect = useCallback(() => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close()
    }

    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const es = new EventSource(`${baseUrl}${CONFIG.SSE_ENDPOINT}`)
    eventSourceRef.current = es

    es.onopen = () => {
      setConnected(true)
      retryDelayRef.current = 1000 // reset backoff
    }

    es.addEventListener('order-updated', () => {
      setEventCount((c) => c + 1)
    })

    es.addEventListener('bulk-order-updated', () => {
      setEventCount((c) => c + 1)
    })

    es.addEventListener('replay-completed', () => {
      setEventCount((c) => c + 1)
    })

    es.onerror = () => {
      setConnected(false)
      es.close()
      // Exponential backoff: 1s → 2s → 4s → 8s → max 30s
      const delay = Math.min(retryDelayRef.current, 30000)
      retryTimeoutRef.current = setTimeout(() => {
        retryDelayRef.current = delay * 2
        connect()
      }, delay)
    }
  }, [])

  useEffect(() => {
    connect()
    return () => {
      if (eventSourceRef.current) eventSourceRef.current.close()
      if (retryTimeoutRef.current) clearTimeout(retryTimeoutRef.current)
    }
  }, [connect])

  return { connected, eventCount }
}
```

### `user-frontend/src/hooks/useAccount.js`
Manages the selected account with localStorage persistence:

```js
import { useState, useCallback } from 'react'
import { CONFIG } from '../constants/config'

const STORAGE_KEY = 'oms_selected_account'

export function useAccount() {
  const [accountID, setAccountID] = useState(() => {
    return localStorage.getItem(STORAGE_KEY) || CONFIG.ACCOUNTS_SEED[0]
  })

  const selectAccount = useCallback((id) => {
    setAccountID(id)
    localStorage.setItem(STORAGE_KEY, id)
  }, [])

  return { accountID, selectAccount }
}
```

### `user-frontend/src/hooks/index.js`
```js
export { useFetch } from './useFetch'
export { useSse } from './useSse'
export { useAccount } from './useAccount'
```

## Integration: Update `App.jsx`

Update the App component to:
1. Initialize SSE via `useSse()` hook
2. Pass `sseEventCount` and `sseConnected` to pages that need auto-refresh
3. Show a small connection indicator in the app

```jsx
import { useSse } from './hooks/useSse'

function App() {
  const { connected, eventCount } = useSse()

  return (
    <BrowserRouter>
      <div className="app-shell">
        {/* Connection indicator — subtle dot in top-right */}
        <div className="connection-indicator">
          <span className={`connection-dot ${connected ? 'connected' : ''}`} />
        </div>

        <main className="app-main">
          <Routes>
            <Route path="/" element={<Home sseEventCount={eventCount} />} />
            <Route path="/orders" element={<Orders sseEventCount={eventCount} />} />
            {/* ... other routes */}
          </Routes>
        </main>
        <BottomNav />
      </div>
    </BrowserRouter>
  )
}
```

### Connection indicator CSS (add to `App.css`)
```css
.connection-indicator {
  position: fixed;
  top: 8px;
  right: 8px;
  z-index: 50;
}

.connection-dot {
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-error);
  transition: background var(--duration-normal);
}

.connection-dot.connected {
  background: var(--color-success);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.15);
}
```

## Auto-Refresh Pattern

Pages that auto-refresh (Home, Orders) should include `sseEventCount` in their `useFetch` deps:

```js
// In Home.jsx:
const { data, loading, error } = useFetch(getDashboard, [sseEventCount])

// In Orders.jsx:
const { data, loading, error } = useFetch(getOrders, [sseEventCount])
```

When an SSE event arrives, `eventCount` increments, which triggers a re-fetch.

## Important Notes
- SSE endpoint is the same as admin: `GET /view/stream`
- The connection dot is very subtle — 8px in the top-right corner, doesn't interfere with content
- Auto-reconnect uses exponential backoff: 1s → 2s → 4s → 8s → 16s → 30s (max)
- Cleanup: close EventSource and clear timeouts on unmount
- `useAccount` persists to localStorage under key `oms_selected_account`
- The `useFetch` hook re-runs when `sseEventCount` changes — this is the auto-refresh mechanism
- Only Home and Orders pages auto-refresh — Fund detail and Order detail are on-demand
- Do NOT add a visible "Live" or "Offline" text label — just the dot (mobile apps are subtle about this)
