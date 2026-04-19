# Prompt 10: Funds Page

## Context
You are building the Funds page (`/funds`) for a Mutual Fund OMS dashboard. This page shows fund-level aggregated data including total orders, total invested amount, and NAV information. The backend has 50 seeded funds across 10 sectors, all under the "CharlesSchwabb" fund family.

## Task
Create `frontend/src/pages/Funds.jsx` and `frontend/src/pages/Funds.css`.

## API Call
```js
import { getFundAggregates } from '../api/aggregatesApi';
```

The `GET /view/aggregates/funds` endpoint returns:
```json
[
  {
    "fundID": "FND001",
    "fundName": "CharlesSchwabb Technology Mutual Fund 1",
    "orderCount": 20,
    "totalAmount": 50000.00,
    "totalQuantity": 4385.96,
    "orderSides": { "BUY": 12, "SELL": 8 },
    "nav": 11.40
  }
]
```

## Page Layout

```
┌──────────────────────────────────────────────────────┐
│  Page Title: "Funds"                                 │
├──────────────────────────────────────────────────────┤
│  [🔍 Search by Fund Name/ID]                         │
├──────────────────────────────────────────────────────┤
│  Fund Cards Grid (3 columns):                        │
│  ┌─────────────────┐  ┌─────────────────┐            │
│  │  FND001          │  │  FND002          │           │
│  │  Tech MF 1       │  │  Tech MF 2       │          │
│  │  NAV: $11.40     │  │  NAV: $12.77     │          │
│  │  ─────────────── │  │  ─────────────── │          │
│  │  Orders: 20      │  │  Orders: 15      │          │
│  │  Total: $50,000  │  │  Total: $35,000  │          │
│  │  BUY: 12 SELL: 8 │  │  BUY: 9  SELL: 6 │          │
│  │  Qty: 4,385.96   │  │  Qty: 2,740.32   │          │
│  └─────────────────┘  └─────────────────┘            │
│  ┌─────────────────┐  ...                            │
│  │  FND003          │                                │
│  │  ...             │                                │
│  └─────────────────┘                                 │
└──────────────────────────────────────────────────────┘
```

## Requirements

1. **Fetch data** on mount using `getFundAggregates()`
2. **Search**: Text input to filter by fund name or fund ID (case-insensitive)
3. **Fund Cards**: Each fund displayed as a card with:
   - Fund ID (font-mono, small text, muted)
   - Fund Name (prominent title)
   - NAV price (large, colored, font-mono)
   - Divider line
   - Order count
   - Total amount invested (`formatCurrency()`)
   - BUY/SELL split (small colored badges: green for BUY count, red for SELL count)
   - Total quantity (`formatQuantity()`)
4. **Card grid**: CSS grid, 3 columns on desktop, 2 on tablet, 1 on mobile
5. **Card hover**: Subtle lift + shadow increase
6. **Click behavior**: Clicking a fund card navigates to Orders page filtered by that fund: `/orders?fundID=FND001`
7. **Empty state**: When no funds have orders, show "No fund data available yet"

## Design Notes
- NAV should be displayed prominently in green or primary blue
- Cards should have a white background with subtle border
- BUY/SELL badges are small inline indicators (not full StatusBadge components)
- Use `formatCurrency` and `formatQuantity` from utils
- The sector can be extracted from the fund name if needed for grouping (optional enhancement)
