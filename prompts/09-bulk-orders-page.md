# Prompt 09: Bulk Orders Page

## Context
You are building the Bulk Orders page (`/bulk-orders`) for a Mutual Fund OMS. Bulk orders are aggregated orders grouped by fund and side. Each bulk order contains references to the individual orders that compose it.

## Task
Create `frontend/src/pages/BulkOrders.jsx` and `frontend/src/pages/BulkOrders.css`.

## API Call
```js
import { getBulkOrderViews } from '../api/bulkOrdersApi';
```

The `GET /view/bulk-orders` endpoint returns:
```json
[
  {
    "bulkOrderID": "BLK500",
    "fundID": "FND001",
    "fundName": "CharlesSchwabb Technology Mutual Fund 1",
    "orderSide": "BUY",
    "bulkOrderStatus": "BOOKED",
    "totalAmount": 25000.00,
    "totalQuantity": 2192.98245614,
    "nav": 11.40,
    "matchedOrderIDs": ["ORD900", "ORD902", "ORD904"],
    "matchedOrderCount": 3
  }
]
```

## Page Layout

```
┌──────────────────────────────────────────────────────┐
│  Page Title: "Bulk Orders"                           │
├──────────────────────────────────────────────────────┤
│  [Status ▼] [Side ▼] [🔍 Search by Bulk ID]         │
├──────────────────────────────────────────────────────┤
│  ┌────────┬─────────┬────┬──────────┬─────┬────┬───┐│
│  │Bulk ID │Fund Name│Side│Total Amt │ Qty │Sts │ # ││
│  ├────────┼─────────┼────┼──────────┼─────┼────┼───┤│
│  │BLK500  │Tech MF1 │BUY │$25,000   │2193 │ BK │ 3 ││
│  │  ↳ Matched Orders: ORD900, ORD902, ORD904       ││
│  │BLK501  │Tech MF2 │SELL│$15,000   │1100 │ CF │ 2 ││
│  │...     │         │    │          │     │    │   ││
│  └────────┴─────────┴────┴──────────┴─────┴────┴───┘│
└──────────────────────────────────────────────────────┘
```

## Requirements

1. **Fetch data** on mount using `getBulkOrderViews()`
2. **Filter Bar**:
   - Status: dropdown with `BULK_ORDER_STATUS` values + "All"
   - Side: BUY / SELL / All
   - Search: by `bulkOrderID`
3. **Data Table** (use `<DataTable />`):
   - Columns:
     | Column | Key | Render |
     |--------|-----|--------|
     | Bulk ID | bulkOrderID | font-mono |
     | Fund | fundName | — |
     | Fund ID | fundID | font-mono |
     | Side | orderSide | colored text |
     | Total Amount | totalAmount | `formatCurrency()` |
     | Total Qty | totalQuantity | `formatQuantity()` |
     | NAV | nav | `formatCurrency()` |
     | Status | bulkOrderStatus | `<StatusBadge variant="bulk" />` |
     | Orders | matchedOrderCount | numeric |
4. **Expandable rows**: When clicking a row, expand to show the list of `matchedOrderIDs` as clickable links (link to `/orders?bulkOrderID=X` or inline display)
5. **Loading / Empty states** handled

## Important Notes
- Import `BULK_ORDER_STATUS` from `constants/orderStatus.js` for filter options
- Import `BULK_STATUS_COLORS` from `constants/statusColors.js` for badge colors
- Expandable row can be a simple toggle showing/hiding a sub-row
- Matched order IDs should be rendered in `font-mono` and ideally link to the Orders page filtered by that bulk order
