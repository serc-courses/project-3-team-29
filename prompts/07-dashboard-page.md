# Prompt 07: Dashboard Page

## Context
You are building the main Dashboard page for a Mutual Fund OMS. This is the landing page (`/`) that gives a high-level overview of the system. Use the reusable components created in prompts 05 and 06.

## Task
Create `frontend/src/pages/Dashboard.jsx` and `frontend/src/pages/Dashboard.css`.

## API Call
```js
import { getDashboard } from '../api/dashboardApi';
```

The `GET /view/dashboard` endpoint returns:
```json
{
  "totalOrders": 100,
  "totalBulkOrders": 10,
  "ordersByStatus": { "PLANNED": 5, "VALIDATED": 10, "BOOKED": 20, ... },
  "bulkOrdersByStatus": { "BULKED": 3, "CONFIRMED": 4, ... },
  "orders": [ /* array of OrderView objects */ ],
  "bulkOrders": [ /* array of BulkOrderView objects */ ]
}
```

## Page Layout

```
┌──────────────────────────────────────────────┐
│  Page Title: "Dashboard"                     │
├──────────────────────────────────────────────┤
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌───────┐ │
│  │ Total  │ │ Bulk   │ │ Booked │ │Errored│ │
│  │ Orders │ │ Orders │ │ Orders │ │ Count │ │
│  │  100   │ │   10   │ │   20   │ │   3   │ │
│  └────────┘ └────────┘ └────────┘ └───────┘ │
├──────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────────┐│
│  │ Order Status    │  │ Bulk Order Status   ││
│  │ Distribution    │  │ Distribution        ││
│  │ [Bar Chart]     │  │ [Donut Chart]       ││
│  └─────────────────┘  └─────────────────────┘│
├──────────────────────────────────────────────┤
│  Recent Orders (last 10)                     │
│  ┌──────────────────────────────────────────┐│
│  │ Order ID │ Account │ Fund │ Amount │ Sts ││
│  │ ORD900   │ ACCT001 │ FND1 │ $1,000 │ ●  ││
│  │ ...      │         │      │        │    ││
│  └──────────────────────────────────────────┘│
└──────────────────────────────────────────────┘
```

## Requirements

1. **Fetch data** on component mount using `useEffect` and `getDashboard()`
2. **Summary Cards** (use `<SummaryCard />`):
   - Total Orders (blue accent)
   - Total Bulk Orders (purple accent)
   - Booked Orders (green accent) — count from `ordersByStatus.BOOKED || 0`
   - Errored Orders (red accent) — count from `ordersByStatus.ERRORED || 0`
3. **Status Distribution Charts** (use `<StatusChart />`):
   - Left: Bar chart of `ordersByStatus`
   - Right: Donut chart of `bulkOrdersByStatus`
4. **Recent Orders Table** (use `<DataTable />`):
   - Show only the last 10 orders sorted by `orderID` descending
   - Columns: Order ID (font-mono), Account ID, Fund Name, Amount (formatted), Status (StatusBadge)
5. **Loading state**: Show spinner while fetching
6. **Error state**: Show error message if API call fails
7. **Auto-refresh**: Re-fetch data every 30 seconds OR integrate with SSE (prompt 13)
8. Cards grid should be responsive: 4 columns on desktop, 2 on tablet, 1 on mobile

## Important Notes
- Do NOT hardcode any data values
- Use `formatCurrency` from `utils/formatters.js` for amounts
- Import status colors from`constants/statusColors.js` — never define colors inline
- Keep the page component clean — extract logic into hooks if needed
