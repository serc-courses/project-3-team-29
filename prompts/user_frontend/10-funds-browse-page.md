# Prompt 10: User Frontend — Funds Browse Page

## Context
The Funds page lets users browse available mutual funds. It's a scrollable grid of fund cards. Users can search funds and tap a card to see fund details or place an order.

## Task
Create `user-frontend/src/pages/Funds/Funds.jsx` and `Funds.css`.

## API Call
```js
import { getFunds } from '../../api/portfolioApi'
```

`GET /view/aggregates/funds` returns:
```json
[
  {
    "fundID": "FND001",
    "fundName": "Technology MF 1",
    "orderCount": 15,
    "totalAmount": 25000.00,
    "totalQuantity": 175.421,
    "orderSides": { "BUY": 12, "SELL": 3 },
    "nav": 142.50
  }
]
```

## Page Layout

```
┌─────────────────────────────┐
│  Explore Funds              │
│  Browse mutual funds        │
│                             │
│  ┌─ Search ───────────────┐│
│  │ 🔍 Search funds...      ││
│  └─────────────────────────┘│
│                             │
│  50 funds available         │
│                             │
│  ┌─────────────────────────┐│
│  │  Technology MF 1  FND001││
│  │  NAV ₹142.50           ││
│  │  15 orders · ₹25K total ││
│  │  BUY: 12  SELL: 3      ││
│  ├─────────────────────────┤│
│  │  Healthcare MF 2 FND002││
│  │  NAV ₹95.30            ││
│  │  8 orders · ₹12K total  ││
│  │  BUY: 8   SELL: 0      ││
│  ├─────────────────────────┤│
│  │  ...more cards...       ││
│  └─────────────────────────┘│
└─────────────────────────────┘
```

## Requirements

1. **Page header**: "Explore Funds" title, "Browse mutual funds" subtitle

2. **Search bar**:
   - Input with inline SVG search icon
   - Placeholder: "Search by name or fund ID"
   - Filters client-side: matches on `fundName` OR `fundID` (case-insensitive)
   - Clear button appears when text is present

3. **Results count**: Show "{n} funds available" or "{n} funds matching '{query}'" below search

4. **Fund cards list**: Stacked `FundCard` components
   - Each card shows: fund name, fund ID, NAV, order count, total amount (compact), buy/sell split
   - Cards are clickable → navigate to `/funds/{fundID}`
   - Sorted alphabetically by fund name
   - 8px gap between cards

5. **Empty states**:
   - No funds at all: "No funds available" (this shouldn't happen with seeded data)
   - No search results: "No funds matching '{query}'"

6. **Loading state**: 5 skeleton FundCards (shimmer)

## Important Notes
- FundCard component was created in prompt 06 — use it here, don't rebuild
- Search is instant (no debounce needed — small dataset)
- Fund names may be long — ensure they truncate with ellipsis on small screens
- NAV formatting: use `formatCurrency` from utils
- Total amount: use `formatCompact` for the summary (e.g., "₹25K" not "₹25,000")
- All fund IDs display in monospace
- This page is purely read-only — no mutation operations
- Wire up route in App.jsx
