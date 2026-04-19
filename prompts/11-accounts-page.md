# Prompt 11: Accounts Page

## Context
You are building the Accounts page (`/accounts`) for a Mutual Fund OMS dashboard. This page displays account-level aggregated data. The backend seeds 10 mock accounts with US names. Each account may have multiple orders across different funds.

## Task
Create `frontend/src/pages/Accounts.jsx` and `frontend/src/pages/Accounts.css`.

## API Call
```js
import { getAccountAggregates } from '../api/aggregatesApi';
```

The `GET /view/aggregates/accounts` endpoint returns:
```json
[
  {
    "accountID": "ACCT00001",
    "orderCount": 10,
    "totalAmount": 15000.00,
    "totalQuantity": 1200.50,
    "statuses": { "BOOKED": 5, "BULKED": 3, "ERRORED": 2 }
  }
]
```

## Page Layout

```
┌──────────────────────────────────────────────────────┐
│  Page Title: "Accounts"                              │
├──────────────────────────────────────────────────────┤
│  [🔍 Search by Account ID]                           │
├──────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────┐│
│  │ Account ID │ Orders │ Total Amount │ Qty │ Status││
│  ├──────────────────────────────────────────────────┤│
│  │ ACCT00001  │   10   │  $15,000.00  │1200 │ ●●●● ││
│  │ ACCT00002  │    8   │  $12,000.00  │ 940 │ ●●●  ││
│  │ ...        │        │              │     │      ││
│  └──────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────┘
```

## Requirements

1. **Fetch data** on mount using `getAccountAggregates()`
2. **Search**: Filter by account ID (case-insensitive)
3. **Data Table** (use `<DataTable />`):
   - Columns:
     | Column | Key | Render |
     |--------|-----|--------|
     | Account ID | accountID | font-mono |
     | Order Count | orderCount | numeric |
     | Total Amount | totalAmount | `formatCurrency()` |
     | Total Quantity | totalQuantity | `formatQuantity()` |
     | Status Breakdown | statuses | mini badge row (see below) |
   - **Status Breakdown column**: Render each status as a small inline badge with count. Example: `BOOKED: 5  BULKED: 3  ERRORED: 2`. Use `<StatusBadge />` in a compact inline format, or mini colored dots with tooltips showing counts.
4. **Row click**: Navigate to Orders page filtered by account: `/orders?accountID=ACCT00001`
5. **Sort**: By order count, total amount (descending useful)
6. **Loading / Empty states**

## Design Notes
- Account IDs in monospace font
- The status breakdown column is the most complex — show as small pills with counts inside
- Consider a horizontal mini bar chart in the status column showing the proportional distribution
- Keep the design consistent with the Orders and Bulk Orders pages
