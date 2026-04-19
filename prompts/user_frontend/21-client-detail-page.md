# Prompt 21: User Frontend — Client Detail Page

## Context
An advisor needs a focused view of one client's account: recent orders, amount summary, status mix, and quick actions to place new orders for that client.

## Task
Create `user-frontend/src/pages/AdvisorClientDetail/AdvisorClientDetail.jsx` and `AdvisorClientDetail.css`.

## API Calls
```js
import { getAdvisorClients, getAdvisorClientOrders } from '../../api/advisorApi'
import { useParams, useNavigate } from 'react-router-dom'
```

## Page Layout

```
┌─────────────────────────────┐
│ ← Back      Client Detail   │
│                             │
│  ACCT00001                  │
│  ₹45,000 · 315 units        │
│                             │
│  ┌──────┐ ┌──────┐ ┌──────┐ │
│  │ 12   │ │  5   │ │  1   │ │
│  │orders│ │active│ │failed│ │
│  └──────┘ └──────┘ └──────┘ │
│                             │
│  Status Breakdown           │
│  [bar rows]                 │
│                             │
│  Recent Orders          All→│
│  ┌─────────────────────────┐│
│  │ account ID visible      ││
│  │ fund, amount, status    ││
│  └─────────────────────────┘│
│                             │
│  [ Place Order For Client ] │
└─────────────────────────────┘
```

## Requirements
1. Top bar with back button and `Client Detail` title
2. Hero section showing selected `accountID`
3. Summary metrics for total orders, active orders, failed orders
4. Status breakdown using the same CSS bar pattern as the investor account page
5. Recent orders list:
   - fetch via `getAdvisorClientOrders(accountId)`
   - show last 5 orders
   - each order card should include fund, amount, side, status, and account ID badge
6. Primary CTA button:
   - `Place Order For Client`
   - navigates to `/advisor/orders/new`
   - preselect this client in the basket form if possible
7. Secondary CTA or text link:
   - `View All Activity`
   - navigates to `/advisor/activity` with client context retained via query param or local state
8. Show an empty state if the account ID is not part of the advisor roster

## Important Notes
- This page is the advisor parallel to the investor account page, but it must always preserve explicit client identity
- Persist the selected client ID in localStorage for reuse by the basket entry page
- Hide the bottom nav on this detail page only if you want a more focused detail layout; otherwise it may remain visible for advisor mode