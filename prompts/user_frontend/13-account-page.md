# Prompt 13: User Frontend — Account Page

## Context
The Account page lets users view their account information and investment summary. Since the backend doesn't have user authentication, users select from available accounts (ACCT00001–ACCT00010) and view that account's data.

## Task
Create `user-frontend/src/pages/Account/Account.jsx` and `Account.css`.

## API Calls
```js
import { getAccounts } from '../../api/portfolioApi'
import { getOrders } from '../../api/ordersApi'
```

`GET /view/aggregates/accounts` returns:
```json
[
  {
    "accountID": "ACCT00001",
    "orderCount": 12,
    "totalAmount": 45000.00,
    "totalQuantity": 315.67,
    "statuses": { "BOOKED": 5, "BULKED": 3, "ERRORED": 1, ... }
  }
]
```

## Page Layout

```
┌─────────────────────────────┐
│  My Account                 │
│                             │
│  ┌─────────────────────────┐│
│  │  Account                ││
│  │  ┌───────────────────┐  ││
│  │  │ ▼ ACCT00001       │  ││  ← account selector dropdown
│  │  └───────────────────┘  ││
│  └─────────────────────────┘│
│                             │
│  ┌─────────────────────────┐│
│  │       ₹ 45,000.00      ││  ← total invested (hero)
│  │    12 orders · 315 units││
│  └─────────────────────────┘│
│                             │
│  Status Breakdown           │
│  ┌─────────────────────────┐│
│  │  Completed    ████░ 5   ││  ← horizontal bar chart
│  │  Pending      ███░░ 3   ││
│  │  Processing   ██░░░ 2   ││
│  │  Failed       █░░░░ 1   ││
│  └─────────────────────────┘│
│                             │
│  Recent Orders          All→│
│  ┌─────────────────────────┐│
│  │ OrderCard 1             ││
│  ├─────────────────────────┤│
│  │ OrderCard 2             ││
│  └─────────────────────────┘│
│                             │
│  ┌─────────────────────────┐│
│  │  App Info                ││
│  │  MF-OMS Invest v1.0     ││
│  │  Backend: localhost:8080 ││
│  └─────────────────────────┘│
└─────────────────────────────┘
```

## Requirements

1. **Page header**: "My Account" title

2. **Account selector**:
   - `<select>` dropdown showing available accounts
   - Default: first account from the aggregates response, or ACCT00001
   - When changed, re-fetch data for the selected account
   - Store selected account in `localStorage` so it persists across page loads
   - If aggregates return empty, show `CONFIG.ACCOUNTS_SEED` as options

3. **Investment summary card**:
   - Total invested amount (large, using AmountDisplay)
   - Subtitle: "{orderCount} orders · {totalQuantity} units"
   - Subtle emerald gradient background similar to Home page hero

4. **Status breakdown** — horizontal bar chart (pure CSS, no chart library):
   - Group the `statuses` object into user-friendly categories:
     - Completed: BOOKED count
     - Pending: BULKED + CONFIRMED + CONTRACTED
     - Processing: PLANNED + VALIDATED + ENRICHED + PLACED
     - Failed: ERRORED
   - Each row: label, a colored horizontal bar (width proportional to count), count number
   - Bar colors: green (completed), amber (pending), blue (processing), red (failed)
   - Only show rows with count > 0
   - Max bar width based on the largest count

5. **Recent orders**:
   - Section header with "Recent Orders" and "View All →" link
   - Fetch orders filtered by selected account: `getOrders({ accountID: selectedAccount })`
   - Show 5 most recent using OrderCard
   - If no orders: EmptyState "No orders for this account"

6. **App info card** (at the bottom):
   - App name from `CONFIG.APP_NAME`
   - Version: "v1.0"
   - Backend connection status (just show the URL)
   - Subtle, muted card style

7. **Loading state**: Skeleton for all sections

8. **Data refresh**: When account changes, show loading skeleton for the investment summary and orders section (not the whole page)

## CSS Bar Chart

```css
.status-bar-row {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  margin-bottom: var(--sp-2);
}

.status-bar-label {
  font-size: var(--text-sm);
  color: var(--color-text-500);
  width: 80px;
  flex-shrink: 0;
}

.status-bar-track {
  flex: 1;
  height: 8px;
  background: var(--color-bg-secondary);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.status-bar-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width var(--duration-slow) var(--ease-out);
}

.status-bar-count {
  font-size: var(--text-sm);
  font-weight: 600;
  font-family: var(--font-mono);
  color: var(--color-text-700);
  width: 32px;
  text-align: right;
}
```

## Important Notes
- The account selector persists via `localStorage.getItem('selectedAccountID')` / `setItem`
- When the selected account changes, only the data below the selector refreshes
- The status breakdown uses CSS bars, NOT Recharts or any chart library
- Group statuses using `STATUS_GROUP` from constants
- If an account has 0 orders, still show the account with "0 orders" summary
- The app info section is for development — in a real app this would be settings/profile
- Wire up route in App.jsx: `/account`
