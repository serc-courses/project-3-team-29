# Prompt 03: User Frontend — Constants, Config & Utilities

## Context
Setting up shared constants, configuration, and utility functions for the user-facing mobile app.

## Task
Create constants, config, and utility files in `user-frontend/src/constants/` and `user-frontend/src/utils/`.

## Files to Create

### `user-frontend/src/constants/config.js`
```js
export const CONFIG = {
  APP_NAME: 'MF-OMS Invest',
  SSE_ENDPOINT: '/view/stream',
  ACCOUNTS_SEED: Array.from({ length: 10 }, (_, i) => `ACCT${String(i + 1).padStart(5, '0')}`),
}
```

### `user-frontend/src/constants/orderStatus.js`
Same enums as admin — order lifecycle is the same:

```js
export const ORDER_STATUS = {
  PLANNED: 'PLANNED',
  VALIDATED: 'VALIDATED',
  ENRICHED: 'ENRICHED',
  PLACED: 'PLACED',
  BULKED: 'BULKED',
  CONFIRMED: 'CONFIRMED',
  CONTRACTED: 'CONTRACTED',
  BOOKED: 'BOOKED',
  ERRORED: 'ERRORED',
}

export const ORDER_SIDE = {
  BUY: 'BUY',
  SELL: 'SELL',
}

// Simplified status groups for user-facing display
export const STATUS_GROUP = {
  PROCESSING: ['PLANNED', 'VALIDATED', 'ENRICHED', 'PLACED'],
  PENDING: ['BULKED', 'CONFIRMED', 'CONTRACTED'],
  COMPLETED: ['BOOKED'],
  FAILED: ['ERRORED'],
}

// User-friendly status labels (investors don't need to see internal states)
export const STATUS_LABEL = {
  PLANNED: 'Processing',
  VALIDATED: 'Processing',
  ENRICHED: 'Processing',
  PLACED: 'Processing',
  BULKED: 'Pending',
  CONFIRMED: 'Pending',
  CONTRACTED: 'Pending',
  BOOKED: 'Completed',
  ERRORED: 'Failed',
}
```

### `user-frontend/src/constants/statusColors.js`
Color mapping for user-friendly status groups:

```js
export const STATUS_COLORS = {
  // Processing states — blue/info
  PLANNED:   { bg: '#EFF6FF', text: '#2563EB', border: '#BFDBFE' },
  VALIDATED: { bg: '#EFF6FF', text: '#2563EB', border: '#BFDBFE' },
  ENRICHED:  { bg: '#EFF6FF', text: '#2563EB', border: '#BFDBFE' },
  PLACED:    { bg: '#EFF6FF', text: '#2563EB', border: '#BFDBFE' },

  // Pending states — amber/warning
  BULKED:     { bg: '#FFFBEB', text: '#D97706', border: '#FDE68A' },
  CONFIRMED:  { bg: '#FFFBEB', text: '#D97706', border: '#FDE68A' },
  CONTRACTED: { bg: '#FFFBEB', text: '#D97706', border: '#FDE68A' },

  // Completed — green/success
  BOOKED: { bg: '#ECFDF5', text: '#059669', border: '#A7F3D0' },

  // Failed — red/error
  ERRORED: { bg: '#FEF2F2', text: '#DC2626', border: '#FECACA' },
}

export const SIDE_COLORS = {
  BUY:  { text: '#059669', bg: '#ECFDF5' },
  SELL: { text: '#DC2626', bg: '#FEF2F2' },
}
```

### `user-frontend/src/constants/routes.js`
```js
export const ROUTES = {
  HOME: '/',
  ORDERS: '/orders',
  PLACE_ORDER: '/orders/new',
  ORDER_DETAIL: '/orders/:orderId',
  FUNDS: '/funds',
  FUND_DETAIL: '/funds/:fundId',
  ACCOUNT: '/account',
}

export const TAB_ITEMS = [
  { label: 'Home', path: ROUTES.HOME, icon: 'home' },
  { label: 'Orders', path: ROUTES.ORDERS, icon: 'orders' },
  { label: 'Funds', path: ROUTES.FUNDS, icon: 'funds' },
  { label: 'Account', path: ROUTES.ACCOUNT, icon: 'account' },
]
```

### `user-frontend/src/constants/index.js`
```js
export { CONFIG } from './config'
export { ORDER_STATUS, ORDER_SIDE, STATUS_GROUP, STATUS_LABEL } from './orderStatus'
export { STATUS_COLORS, SIDE_COLORS } from './statusColors'
export { ROUTES, TAB_ITEMS } from './routes'
```

### `user-frontend/src/utils/formatters.js`
```js
export function formatCurrency(amount) {
  if (amount == null) return '—'
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function formatAmount(amount) {
  if (amount == null) return '—'
  return new Intl.NumberFormat('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function formatQuantity(qty) {
  if (qty == null) return '—'
  return new Intl.NumberFormat('en-IN', {
    minimumFractionDigits: 3,
    maximumFractionDigits: 3,
  }).format(qty)
}

export function formatCompact(num) {
  if (num == null) return '—'
  if (num >= 10000000) return `${(num / 10000000).toFixed(2)} Cr`
  if (num >= 100000) return `${(num / 100000).toFixed(2)} L`
  if (num >= 1000) return `${(num / 1000).toFixed(1)} K`
  return num.toString()
}

export function truncateId(id) {
  if (!id) return '—'
  return id.length > 10 ? `${id.slice(0, 4)}...${id.slice(-4)}` : id
}
```

### `user-frontend/src/utils/index.js`
```js
export { formatCurrency, formatAmount, formatQuantity, formatCompact, truncateId } from './formatters'
```

## Important Notes
- `STATUS_LABEL` maps internal statuses to user-friendly labels — investors should NOT see "BULKED" or "ENRICHED"
- Currency formatting uses `en-IN` (Indian Rupees with lakh/crore system) — adjust locale if needed
- `formatCompact` supports Crore (Cr) and Lakh (L) abbreviations for Indian financial context
- `CONFIG.ACCOUNTS_SEED` provides the known account IDs (ACCT00001–ACCT00010) as fallback when the aggregates API returns empty
- Do NOT include bulk order statuses — users don't interact with bulk orders
