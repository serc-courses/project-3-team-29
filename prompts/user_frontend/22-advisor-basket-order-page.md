# Prompt 22: User Frontend — Advisor Basket Order Page

## Context
Unlike investor mode, advisor mode must support placing multiple orders across different client accounts in one flow. The backend already supports this because `POST /orders/plan` accepts an array.

## Task
Create `user-frontend/src/pages/AdvisorBasketOrder/AdvisorBasketOrder.jsx` and `AdvisorBasketOrder.css`.

## API Calls
```js
import { getAdvisorClients } from '../../api/advisorApi'
import { getFunds } from '../../api/portfolioApi'
import { planAdvisorBasket } from '../../api/advisorApi'
```

## Payload Shape
```json
[
  { "productID": "FND001", "amount": 1000, "accountID": "ACCT00001", "orderSide": "BUY" },
  { "productID": "FND002", "amount": 2000, "accountID": "ACCT00003", "orderSide": "SELL" }
]
```

## Page Layout

```
┌─────────────────────────────┐
│ ← Back     New Basket       │
│                             │
│  Basket Rows                │
│  ┌─────────────────────────┐│
│  │ Client ▼                ││
│  │ Fund ▼                  ││
│  │ Amount                  ││
│  │ BUY / SELL              ││
│  │ Remove row              ││
│  └─────────────────────────┘│
│  + Add Another Row          │
│                             │
│  Basket Summary             │
│  4 orders · ₹18,000 total   │
│                             │
│  [ Review Basket ]          │
└─────────────────────────────┘
```

## Requirements
1. Start with one editable basket row by default
2. Each row contains:
   - client dropdown restricted to advisor-owned accounts
   - fund dropdown
   - amount input
   - side toggle BUY / SELL
   - remove row action
3. Add `Add Another Row` button to append more rows
4. Prevent removing the final remaining row; there must always be at least one row
5. Validation per row:
   - client required
   - fund required
   - amount > 0
   - side required, default BUY
6. Basket summary section shows:
   - total row count
   - total amount
   - BUY count
   - SELL count
   - distinct client count
7. CTA button should be `Review Basket`, not immediate submit
8. Save basket draft rows to localStorage after every change
9. If a selected client was passed from client detail, prefill the first row with that account ID
10. This page hides the bottom nav for focus

## Important Notes
- Keep the UI vertically stacked and thumb-friendly, not table-based
- Reuse field and toggle styling from the investor place-order page
- Client identity is the most important piece of information in advisor mode; show it clearly on every row card