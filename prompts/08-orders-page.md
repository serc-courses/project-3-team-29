# Prompt 08: Orders Page

## Context
You are building the Orders page (`/orders`) for a Mutual Fund OMS dashboard. This page shows all individual orders in a filterable, sortable data table. It is the most data-dense page in the app.

## Task
Create `frontend/src/pages/Orders.jsx` and `frontend/src/pages/Orders.css`.

## API Call
```js
import { getOrderViews } from '../api/ordersApi';
```

The `GET /view/orders` endpoint returns an array of `OrderView` objects:
```json
[
  {
    "orderID": "ORD900",
    "accountID": "ACCT00001",
    "fundID": "FND001",
    "fundName": "CharlesSchwabb Technology Mutual Fund 1",
    "orderSide": "BUY",
    "amount": 1000.00,
    "quantity": 87.71929825,
    "nav": 11.40,
    "orderStatus": "BOOKED",
    "bulkOrderID": "BLK500"
  }
]
```

Optional query parameters for filtering: `?accountID=X`, `?fundID=X`, `?bulkOrderID=X`.

## Page Layout

```
┌──────────────────────────────────────────────────────┐
│  Page Title: "Orders"          [+ New Order] button  │
├──────────────────────────────────────────────────────┤
│  Filter Bar:                                         │
│  [Status ▼] [Account ▼] [Side ▼] [🔍 Search by ID] │
├──────────────────────────────────────────────────────┤
│  Showing 85 of 100 orders                            │
├──────────────────────────────────────────────────────┤
│  ┌──────┬──────┬────────┬────┬─────────┬────┬──────┐│
│  │Ord ID│Acct  │Fund    │Side│ Amount  │Qty │Status││
│  ├──────┼──────┼────────┼────┼─────────┼────┼──────┤│
│  │ORD900│ACT01 │Tech MF1│BUY │$1,000.00│87.7│ ● BK ││
│  │ORD901│ACT02 │Tech MF1│SELL│$1,500.00│131 │ ● PL ││
│  │...   │      │        │    │         │    │      ││
│  └──────┴──────┴────────┴────┴─────────┴────┴──────┘│
└──────────────────────────────────────────────────────┘
```

## Requirements

1. **Fetch data** on mount using `getOrderViews()`
2. **Filter Bar** (use `<FilterBar />`):
   - **Status filter**: Dropdown with all `ORDER_STATUS` values from `constants/orderStatus.js` + "All" option
   - **Account filter**: Dropdown populated from unique `accountID` values in the data
   - **Side filter**: Dropdown with "All", "BUY", "SELL"
   - **Search**: Text input to search by `orderID` (case-insensitive partial match)
3. **Client-side filtering**: Filter the full dataset based on selected filters + search
4. **Result count**: "Showing X of Y orders" text above the table
5. **Data Table** (use `<DataTable />`):
   - Columns:
     | Column | Key | Align | Render |
     |--------|-----|-------|--------|
     | Order ID | orderID | left | font-mono |
     | Account | accountID | left | font-mono |
     | Fund | fundName | left | truncate if long |
     | Side | orderSide | center | BUY (green text) / SELL (red text) |
     | Amount | amount | right | `formatCurrency()` |
     | Quantity | quantity | right | `formatQuantity()` |
     | NAV | nav | right | `formatCurrency()` |
     | Status | orderStatus | center | `<StatusBadge />` |
     | Bulk ID | bulkOrderID | left | font-mono, or "—" if null |
   - All columns sortable
   - Row click: (optional) expand to show full details OR navigate to order detail
6. **"New Order" button** in the header area: Link to `/orders/new`
7. **Loading state**: Spinner while fetching
8. **Empty state**: "No orders found" message

## Important Notes
- ALL filter options (statuses, sides) must come from constants — NOT hardcoded strings
- Use `formatCurrency` and `formatQuantity` from `utils/formatters.js` — don't inline formatting
- Fund name should truncate with ellipsis if too long (max-width in CSS)
- The table should handle 100+ rows performantly
