# Prompt 08: User Frontend — Orders Page (Order History)

## Context
The Orders page shows all of the user's orders in a scrollable card list. Unlike the admin frontend which uses a data table, this uses stacked OrderCards optimized for mobile.

## Task
Create `user-frontend/src/pages/Orders/Orders.jsx` and `Orders.css`.

## API Call
```js
import { getOrders } from '../../api/ordersApi'
```

`GET /view/orders` returns:
```json
[
  {
    "orderID": "ORD123",
    "accountID": "ACCT00001",
    "fundID": "FND001",
    "fundName": "Technology MF 1",
    "orderSide": "BUY",
    "amount": 1000.00,
    "quantity": 7.018,
    "nav": 142.50,
    "orderStatus": "BOOKED",
    "bulkOrderID": "BULK001"
  }
]
```

## Page Layout

```
┌─────────────────────────────┐
│  Orders                     │
│  12 total                   │
│                             │
│  ┌─ Filter Chips ─────────┐│
│  │ All (12) │ Active (5)  ││
│  │ Completed (6)│Failed(1)││
│  └─────────────────────────┘│
│                             │
│  ┌─ Search ───────────────┐│
│  │ 🔍 Search by Order ID  ││
│  └─────────────────────────┘│
│                             │
│  ┌─────────────────────────┐│
│  │ Technology MF 1    BUY  ││
│  │ ORD123          ₹1,000  ││
│  │ ACCT00001    ● Completed││
│  ├─────────────────────────┤│
│  │ Healthcare MF 2   SELL  ││
│  │ ORD124          ₹2,500  ││
│  │ ACCT00002   ● Processing││
│  ├─────────────────────────┤│
│  │ ...more cards...        ││
│  └─────────────────────────┘│
│                             │
│              [+ New Order]  │  ← FAB button (floating)
└─────────────────────────────┘
```

## Requirements

1. **Page header**: "Orders" title, subtitle showing total count ("12 total")

2. **Filter chips**: Horizontal scrollable row of filter chips:
   - "All" — shows all orders (default, with count)
   - "Active" — PLANNED + VALIDATED + ENRICHED + PLACED (processing statuses)
   - "Pending" — BULKED + CONFIRMED + CONTRACTED
   - "Completed" — BOOKED only
   - "Failed" — ERRORED only
   - Active chip: filled primary color. Inactive: outline/ghost style
   - Each chip shows count in parentheses
   - Use the `STATUS_GROUP` constant from `constants/orderStatus.js`

3. **Search bar**: 
   - Text input with inline SVG search icon on the left
   - Placeholder: "Search by Order ID"
   - Filters orders client-side by `orderID` (case-insensitive contains match)
   - Clear button (X icon) appears when text is entered

4. **Order list**: 
   - Stacked `OrderCard` components, separated by 8px gap
   - Each card is clickable → navigates to `/orders/{orderID}`
   - Sorted by orderID descending (most recent first)
   - Apply both filter chip AND search filters simultaneously

5. **Floating Action Button (FAB)**:
   - Fixed position, bottom-right (above bottom nav)
   - Primary emerald circle with a white "+" SVG icon
   - Navigates to `/orders/new`
   - Size: 56×56px, border-radius: 50%, box-shadow
   - Subtle scale animation on active/press

6. **Empty states**:
   - No orders at all: "No orders yet" with CTA "Start investing"
   - No orders matching filter: "No {filter} orders" without CTA
   - No search results: "No orders matching '{query}'"

7. **Loading state**: 4 skeleton OrderCards (shimmer placeholder)

8. **Counts**: Derive filter counts from the full order list client-side

## Filter Chip Styling
```css
.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 9999px;
  font-size: var(--text-sm);
  font-weight: 500;
  white-space: nowrap;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-500);
  cursor: pointer;
  transition: all var(--duration-fast);
  -webkit-user-select: none;
  min-height: 36px;
}

.filter-chip.active {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.filter-chips {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.filter-chips::-webkit-scrollbar {
  display: none;
}
```

## Important Notes
- Order list does NOT use a `<table>` — it uses stacked cards (mobile pattern)
- Filtering is client-side since data volume is small (hundreds, not millions)
- Search is debounced implicitly (filters on every keystroke, but the list is small)
- FAB positioned at `bottom: calc(var(--bottom-nav-height) + 16px)`, `right: 16px`
- On the search input, use `type="search"` for native mobile keyboard with search button
- OrderCard `onClick` should navigate to `/orders/${order.orderID}`
- Do NOT use the admin FilterBar component — build mobile-native filter chips
- Wire up the route in App.jsx
