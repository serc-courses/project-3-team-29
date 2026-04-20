# Prompt 07: User Frontend — Home Page (Portfolio Overview)

## Context
The Home page is the user's landing screen — a portfolio overview showing their investment summary, recent orders, and quick actions. Think of it like the home screen of Groww or Zerodha.

## Task
Create `user-frontend/src/pages/Home/Home.jsx` and `Home.css`.

## API Calls
```js
import { getDashboard } from '../../api/portfolioApi'
import { getOrders } from '../../api/ordersApi'
```

`GET /view/dashboard` returns:
```json
{
  "totalOrders": 50,
  "totalBulkOrders": 10,
  "ordersByStatus": { "BOOKED": 20, "BULKED": 15, "ERRORED": 2, ... },
  "orders": [ { "orderID": "ORD123", "accountID": "ACCT00001", ... } ],
  "bulkOrders": [...]
}
```

## Page Layout

```
┌─────────────────────────────┐
│  Good Morning 👋             │  ← greeting (time-based, no emoji, use text)
│  Your Portfolio              │
│                              │
│  ┌─────────────────────────┐ │
│  │  Total Invested          │ │  ← hero amount card
│  │  ₹ 2,45,000.00          │ │
│  │  50 orders · 10 funds    │ │
│  └─────────────────────────┘ │
│                              │
│  ┌────┐ ┌────┐ ┌────┐       │
│  │ 20 │ │ 15 │ │  2 │       │  ← status summary row (3 mini cards)
│  │Done│ │Pend│ │Fail│       │
│  └────┘ └────┘ └────┘       │
│                              │
│  Quick Actions               │
│  ┌──────────┐ ┌──────────┐  │
│  │ + Invest │ │ My Orders│  │  ← action buttons
│  └──────────┘ └──────────┘  │
│                              │
│  Recent Orders          All →│
│  ┌─────────────────────────┐ │
│  │ Technology MF 1    BUY  │ │  ← OrderCard
│  │ ORD123          ₹1,000  │ │
│  │ ACCT00001   ● Processing│ │
│  ├─────────────────────────┤ │
│  │ Healthcare MF 2   SELL  │ │
│  │ ORD124          ₹2,500  │ │
│  │ ACCT00002    ● Completed│ │
│  └─────────────────────────┘ │
│                              │
└─────────────────────────────┘
```

## Requirements

1. **Greeting section**: Time-based text greeting (no emoji):
   - Before 12:00 → "Good Morning"
   - 12:00–17:00 → "Good Afternoon"  
   - After 17:00 → "Good Evening"
   - Subtitle: "Your Portfolio"

2. **Hero investment card**:
   - Total invested amount (sum of all order amounts) displayed large using `AmountDisplay` with `size="lg"`
   - Subtitle: "{totalOrders} orders · {number of unique funds} funds"
   - Card has a subtle emerald gradient background (from `--color-primary-50` to white)

3. **Status summary row**: 3 mini metric cards in a horizontal row:
   - **Completed**: count of BOOKED orders, green accent
   - **Pending**: count of BULKED + CONFIRMED + CONTRACTED, amber accent
   - **Failed**: count of ERRORED, red accent (only show if > 0, otherwise show "Processing" count)
   - Each card: number large (font-mono), label small below

4. **Quick actions**: Two buttons in a row:
   - "+ Invest" → navigates to `/orders/new` (primary button style)
   - "My Orders" → navigates to `/orders` (outline button style)
   - Both buttons full-height, equal width, with inline SVG icons

5. **Recent orders**: 
   - Section header with "Recent Orders" title and "View All →" link to `/orders`
   - Show the 5 most recent orders using `OrderCard` component
   - Sort by orderID descending (highest ID = most recent)
   - If no orders, show `EmptyState` with message "No investments yet" and a CTA to invest

6. **Loading state**: Show skeleton placeholders (shimmer) for:
   - Hero card: one large skeleton rectangle
   - Stats row: three small skeleton rectangles
   - Order cards: three skeleton card shapes

7. **Error state**: Show a subtle error banner at the top with retry button

8. **Data derivation**: Calculate all stats from the dashboard API response:
   - Total invested = sum of all `orders[].amount`
   - Unique funds = count of distinct `fundID` values from orders
   - Status counts = from `ordersByStatus` object

## Important Notes
- Use `useFetch` hook (create it if not done yet — same pattern as admin: takes a fetch function, returns `{ data, loading, error, refetch }`)
- Greeting should NOT use any emoji — plain text only
- The hero card should feel premium — slightly larger padding, larger font, subtle background
- All amounts use `formatCurrency` from utils
- All order IDs display in monospace font
- Page scrolls naturally — no fixed sections except the bottom nav
- Quick action buttons need inline SVG icons (plus icon for invest, list icon for orders)
- Wire up the route in App.jsx: replace the Home placeholder with the real component
