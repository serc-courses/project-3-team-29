# Prompt 11: User Frontend — Fund Detail Page

## Context
When a user taps a fund card, they see the fund's details: NAV, order stats, and a list of their orders in that fund. This is also where they can place a new order for this specific fund.

## Task
Create `user-frontend/src/pages/FundDetail/FundDetail.jsx` and `FundDetail.css`.

## API Calls
```js
import { getFunds } from '../../api/portfolioApi'
import { getOrders } from '../../api/ordersApi'
```

Use `useParams()` to get `fundId` from the URL.
- Fetch fund info: `getFunds()` then find the matching fund by `fundID`
- Fetch orders for this fund: `getOrders({ fundID: fundId })`

## Page Layout

```
┌─────────────────────────────┐
│  ← Back       Fund Detail   │  ← top bar with back button
│                             │
│  ┌─────────────────────────┐│
│  │  Technology MF 1        ││  ← fund name, large
│  │  FND001                 ││  ← fund ID, mono, muted
│  │                         ││
│  │     NAV ₹ 142.50       ││  ← hero NAV display
│  └─────────────────────────┘│
│                             │
│  Investment Summary         │
│  ┌──────┐ ┌──────┐ ┌──────┐│
│  │  15  │ │₹25K  │ │175.4 ││  ← stats: orders, amount, quantity
│  │orders│ │invest│ │ units ││
│  └──────┘ └──────┘ └──────┘│
│                             │
│  ┌──────────┐ ┌──────────┐ │
│  │  BUY: 12 │ │ SELL: 3  │ │  ← side split
│  └──────────┘ └──────────┘ │
│                             │
│  Orders in this Fund   All →│
│  ┌─────────────────────────┐│
│  │ OrderCard 1             ││
│  ├─────────────────────────┤│
│  │ OrderCard 2             ││
│  ├─────────────────────────┤│
│  │ ...                     ││
│  └─────────────────────────┘│
│                             │
│  ┌─────────────────────────┐│
│  │  Invest in this Fund    ││  ← sticky bottom CTA
│  └─────────────────────────┘│
└─────────────────────────────┘
```

## Requirements

1. **Top bar**: Back arrow + "Fund Detail" title. This page hides the bottom nav.

2. **Fund hero card**:
   - Fund name large and bold
   - Fund ID in monospace, muted text
   - NAV displayed prominently: "NAV" label above, large emerald-colored amount

3. **Investment summary**: 3 mini stat cards in a row:
   - Order count (total orders in this fund)
   - Total invested (using `formatCompact`)
   - Total quantity (units held)
   - Each: big number on top, small label below

4. **Side split**: Two equal-width cards showing BUY and SELL counts
   - BUY card: emerald/green tint
   - SELL card: red tint
   - Show count from `orderSides` object

5. **Orders in this fund**:
   - Section header with "Orders in this Fund" and "View All →" link (navigates to `/orders` filtered by this fund — but since we can't pass filter state easily, just navigate to `/orders`)
   - List of OrderCards for orders matching this fund
   - Show max 5, sorted by orderID descending
   - If no orders: show EmptyState "No orders for this fund yet"

6. **Sticky bottom CTA**:
   - "Invest in this Fund" primary button
   - Fixed to bottom of page (above safe area)
   - Navigates to `/orders/new` (the place order form)
   - Full width, large size

7. **Loading state**: Skeleton for hero + stats + order list

8. **Not found**: If fund not found in the data, show EmptyState with back button

## Important Notes
- This page hides the bottom nav — update BottomNav to also hide on `/funds/:id`
- The fund data comes from the aggregates endpoint — if it returns empty (no orders exist yet), show the fund name from the URL param and "No data available"
- The CTA button could ideally pre-select this fund in the order form, but for now just navigate to `/orders/new`
- All amounts use `formatCurrency`, compact amounts use `formatCompact`
- Quantity uses `formatQuantity`
- Wire up route in App.jsx: `/funds/:fundId`
