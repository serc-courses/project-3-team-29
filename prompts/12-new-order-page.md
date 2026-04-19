# Prompt 12: New Order Page (Order Submission Form)

## Context
You are building the New Order page (`/orders/new`) for a Mutual Fund OMS. This page is a form that allows users to submit one or more orders at a time. Orders are submitted to the backend via `POST /orders/plan`.

## Task
Create `frontend/src/pages/NewOrder.jsx` and `frontend/src/pages/NewOrder.css`.

## API Calls
```js
import { planOrders } from '../api/ordersApi';
import { getFundAggregates } from '../api/aggregatesApi';
import { getAccountAggregates } from '../api/aggregatesApi';
```

The `POST /orders/plan` endpoint accepts:
```json
[
  {
    "productID": "FND001",
    "amount": 1000,
    "accountID": "ACCT00001",
    "orderSide": "BUY"
  }
]
```

Response (201):
```json
{
  "message": "Orders planned",
  "count": 1,
  "orderIDs": ["ORD1234"]
}
```

## Page Layout

```
┌──────────────────────────────────────────────────────┐
│  Page Title: "Plan New Orders"                       │
├──────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────┐│
│  │  Order 1                               [Remove] ││
│  │  ┌──────────┐ ┌──────────┐ ┌──────┐ ┌────────┐  ││
│  │  │Fund ▼    │ │Account ▼ │ │Amount│ │Side ▼  │  ││
│  │  │FND001    │ │ACCT00001 │ │$1000 │ │BUY     │  ││
│  │  └──────────┘ └──────────┘ └──────┘ └────────┘  ││
│  └──────────────────────────────────────────────────┘│
│  ┌──────────────────────────────────────────────────┐│
│  │  Order 2                               [Remove] ││
│  │  ┌──────────┐ ┌──────────┐ ┌──────┐ ┌────────┐  ││
│  │  │Fund ▼    │ │Account ▼ │ │Amount│ │Side ▼  │  ││
│  │  └──────────┘ └──────────┘ └──────┘ └────────┘  ││
│  └──────────────────────────────────────────────────┘│
│                                                      │
│  [+ Add Another Order]                               │
│                                                      │
│  ─────────────────────────────────────────────────── │
│  Summary: 2 orders, Total: $2,500.00                 │
│  [Cancel]                      [Submit Orders]       │
└──────────────────────────────────────────────────────┘
```

## Requirements

1. **Fetch dropdown options** on mount:
   - Funds: from `getFundAggregates()` — extract `fundID` + `fundName` for the dropdown
   - Accounts: from `getAccountAggregates()` — extract `accountID` for the dropdown
   - NOTE: If these endpoints return no data (no orders exist yet), fetch raw data from `GET /orders` and `GET /view/orders` instead, or hardcode the known accountIDs ACCT00001-ACCT00010 and fundIDs FND001-FND050 as the initial seed data is always present. Actually, the best approach: fetch funds list by using a call that returns all fund data. Since there is no dedicated funds list endpoint, use the existing fund data visible through the order views or consider calling `GET /view/aggregates/funds`. If that returns empty, fall back to the known seeded IDs.
   
2. **Order row management**:
   - Start with one empty order row
   - "Add Another Order" button adds a new row
   - Each row has a "Remove" button (disabled if only one row)
   - Each order row contains:
     - **Fund**: `<select>` dropdown with `fundID — fundName` (e.g., "FND001 — Technology MF 1")
     - **Account**: `<select>` dropdown with account IDs
     - **Amount**: `<input type="number">` with min=1, step=100
     - **Side**: `<select>` with "BUY" and "SELL" from `ORDER_SIDE` constants

3. **Client-side validation** before submission:
   - All fields are required
   - Amount must be > 0
   - Show inline error messages under invalid fields
   - Disable submit button if any validation errors

4. **Form submission**:
   - On submit, POST the orders array via `planOrders()`
   - Show loading state on submit button
   - On success:
     - Show success toast with assigned order IDs
     - Navigate to `/orders` after 2 seconds
   - On error:
     - Show error toast with error message
     - Keep form data intact

5. **Summary section** at the bottom:
   - Count of orders
   - Total amount across all orders
   - Formatted with `formatCurrency()`

6. **Cancel button**: Navigate back to `/orders`

## Important Notes
- Import `ORDER_SIDE` from `constants/orderStatus.js` for the side dropdown — do NOT hardcode
- The `productID` field in the API corresponds to `fundID` — map accordingly when building the payload
- Do not send `orderID`, `quantity`, `orderStatus`, or `isProcessed` — the backend sets these
- The form should feel clean and professional — well-spaced, clear labels, proper focus states
