# Prompt 03: Types, Constants & Configuration

## Context
You are building a Mutual Fund OMS React frontend. The backend has specific enum values and data structures that the frontend needs to reference consistently. No values should be hardcoded in components.

## Task
Create constants, type definitions, and configuration files in `frontend/src/constants/` and `frontend/src/utils/`.

## Files to Create

### 1. `frontend/src/constants/config.js`
```js
// Application configuration - all environment-dependent values
export const CONFIG = {
  API_BASE_URL: import.meta.env.VITE_API_BASE_URL || '',
  SSE_ENDPOINT: '/view/stream',
  APP_NAME: 'Mutual Fund OMS',
};
```

### 2. `frontend/src/constants/routes.js`
```js
export const ROUTES = {
  DASHBOARD: '/',
  ORDERS: '/orders',
  NEW_ORDER: '/orders/new',
  BULK_ORDERS: '/bulk-orders',
  FUNDS: '/funds',
  ACCOUNTS: '/accounts',
};

// Sidebar navigation items
export const NAV_ITEMS = [
  { label: 'Dashboard', path: ROUTES.DASHBOARD, icon: 'dashboard' },
  { label: 'Orders', path: ROUTES.ORDERS, icon: 'orders' },
  { label: 'Bulk Orders', path: ROUTES.BULK_ORDERS, icon: 'bulk' },
  { label: 'Funds', path: ROUTES.FUNDS, icon: 'funds' },
  { label: 'Accounts', path: ROUTES.ACCOUNTS, icon: 'accounts' },
];
```

### 3. `frontend/src/constants/orderStatus.js`
These must match the backend enum values exactly.

```js
// Individual order statuses (from com.iiit.oms.model.OrderStatus)
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
};

// Bulk order statuses (from com.iiit.oms.model.BulkOrderStatus)
export const BULK_ORDER_STATUS = {
  BULKED: 'BULKED',
  CONFIRMED: 'CONFIRMED',
  CONTRACTED: 'CONTRACTED',
  BOOKED: 'BOOKED',
};

// Order sides (from com.iiit.oms.model.OrderSide)
export const ORDER_SIDE = {
  BUY: 'BUY',
  SELL: 'SELL',
};

// Order lifecycle progression (for pipeline visualization)
export const ORDER_LIFECYCLE = [
  ORDER_STATUS.PLANNED,
  ORDER_STATUS.VALIDATED,
  ORDER_STATUS.ENRICHED,
  ORDER_STATUS.PLACED,
  ORDER_STATUS.BULKED,
  ORDER_STATUS.CONFIRMED,
  ORDER_STATUS.CONTRACTED,
  ORDER_STATUS.BOOKED,
];
```

### 4. `frontend/src/constants/statusColors.js`
Color mapping for status badges and charts. Must NOT be hardcoded in components.

```js
import { ORDER_STATUS, BULK_ORDER_STATUS } from './orderStatus';

// Colors for individual order statuses
export const ORDER_STATUS_COLORS = {
  [ORDER_STATUS.PLANNED]: { bg: '#F3F4F6', text: '#6B7280', border: '#D1D5DB' },
  [ORDER_STATUS.VALIDATED]: { bg: '#DBEAFE', text: '#1D4ED8', border: '#93C5FD' },
  [ORDER_STATUS.ENRICHED]: { bg: '#E0E7FF', text: '#4338CA', border: '#A5B4FC' },
  [ORDER_STATUS.PLACED]: { bg: '#EDE9FE', text: '#7C3AED', border: '#C4B5FD' },
  [ORDER_STATUS.BULKED]: { bg: '#FEF3C7', text: '#D97706', border: '#FCD34D' },
  [ORDER_STATUS.CONFIRMED]: { bg: '#CCFBF1', text: '#0D9488', border: '#5EEAD4' },
  [ORDER_STATUS.CONTRACTED]: { bg: '#CFFAFE', text: '#0891B2', border: '#67E8F9' },
  [ORDER_STATUS.BOOKED]: { bg: '#D1FAE5', text: '#059669', border: '#6EE7B7' },
  [ORDER_STATUS.ERRORED]: { bg: '#FFE4E6', text: '#E11D48', border: '#FDA4AF' },
};

// Colors for bulk order statuses
export const BULK_STATUS_COLORS = {
  [BULK_ORDER_STATUS.BULKED]: ORDER_STATUS_COLORS[ORDER_STATUS.BULKED],
  [BULK_ORDER_STATUS.CONFIRMED]: ORDER_STATUS_COLORS[ORDER_STATUS.CONFIRMED],
  [BULK_ORDER_STATUS.CONTRACTED]: ORDER_STATUS_COLORS[ORDER_STATUS.CONTRACTED],
  [BULK_ORDER_STATUS.BOOKED]: ORDER_STATUS_COLORS[ORDER_STATUS.BOOKED],
};

// Chart colors (for Recharts)
export const CHART_COLORS = [
  '#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6',
  '#06B6D4', '#EC4899', '#84CC16', '#F97316',
];
```

### 5. `frontend/src/utils/formatters.js`
```js
// Format a number as currency (USD)
export const formatCurrency = (value) => {
  if (value == null) return '—';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
};

// Format a quantity with up to 8 decimal places
export const formatQuantity = (value) => {
  if (value == null) return '—';
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 8,
  }).format(value);
};

// Format a number compactly (e.g., 1.2K, 3.4M)
export const formatCompact = (value) => {
  if (value == null) return '—';
  return new Intl.NumberFormat('en-US', {
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(value);
};
```

### 6. `frontend/src/constants/index.js`
```js
export * from './config';
export * from './routes';
export * from './orderStatus';
export * from './statusColors';
```

## Important Notes
- All enum values must exactly match the backend Java enum names (case-sensitive)
- Colors are defined centrally — components import from `statusColors.js`, never define their own
- Formatters use `Intl.NumberFormat` for proper locale-aware formatting
