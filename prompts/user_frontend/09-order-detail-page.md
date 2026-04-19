# Prompt 09: User Frontend — Order Detail Page

## Context
When a user taps an order card, they see a full-screen detail view with order tracking, like a delivery tracker. This is the most "app-like" page.

## Task
Create `user-frontend/src/pages/OrderDetail/OrderDetail.jsx` and `OrderDetail.css`.

## API Calls
```js
import { getOrders } from '../../api/ordersApi'
import { getOrderStatus } from '../../api/ordersApi'
```

Use `useParams()` from react-router-dom to get `orderId` from the URL.
Fetch order details via `getOrders({ orderID: orderId })` — returns array, use first element.

## Page Layout

```
┌─────────────────────────────┐
│  ← Back         Order Detail│  ← top bar with back button
│                             │
│  ┌─────────────────────────┐│
│  │  Technology MF 1        ││
│  │  FND001                 ││
│  │                         ││
│  │     ₹ 1,000.00         ││  ← hero amount, large
│  │  7.018 units @ ₹142.50 ││  ← quantity × NAV
│  │                         ││
│  │     BUY     ACCT00001  ││  ← side badge + account
│  └─────────────────────────┘│
│                             │
│  Order Progress             │
│  ┌─────────────────────────┐│
│  │  ● Planned         ✓   ││  ← completed step (green)
│  │  │                      ││
│  │  ● Validated        ✓   ││
│  │  │                      ││
│  │  ● Enriched         ✓   ││
│  │  │                      ││
│  │  ● Placed           ✓   ││
│  │  │                      ││
│  │  ● Bulked           ✓   ││
│  │  │                      ││
│  │  ◉ Confirmed       ···  ││  ← current step (pulsing)
│  │  │                      ││
│  │  ○ Contracted           ││  ← upcoming step (gray)
│  │  │                      ││
│  │  ○ Booked               ││
│  └─────────────────────────┘│
│                             │
│  Order Details              │
│  ┌─────────────────────────┐│
│  │  Order ID    ORD123     ││  ← detail rows
│  │  Fund ID     FND001     ││
│  │  Account     ACCT00001  ││
│  │  Side        BUY        ││
│  │  Amount      ₹1,000.00  ││
│  │  Quantity    7.018       ││
│  │  NAV         ₹142.50    ││
│  │  Bulk ID     BULK001    ││
│  │  Status      ● Pending  ││
│  └─────────────────────────┘│
└─────────────────────────────┘
```

## Requirements

1. **Top navigation bar**:
   - Back arrow (inline SVG) on the left — calls `navigate(-1)` from react-router
   - "Order Detail" text centered
   - This page does NOT show the bottom nav (it's a detail screen)
   - Fixed at top, white background with bottom border

2. **Hero section** (in a card):
   - Fund name large and bold
   - Fund ID in monospace, muted
   - Amount displayed large center-aligned using AmountDisplay with `size="lg"`
   - If quantity and NAV are available: show "{quantity} units @ ₹{NAV}" below
   - Side badge (BUY in green, SELL in red) and account ID on the bottom row

3. **Order Progress Tracker** — a vertical stepper showing the order lifecycle:
   - Steps: PLANNED → VALIDATED → ENRICHED → PLACED → BULKED → CONFIRMED → CONTRACTED → BOOKED
   - For each step, determine state based on current `orderStatus`:
     - **Completed**: steps before the current status → green circle with checkmark, green connecting line
     - **Current**: the step matching `orderStatus` → emerald filled circle with pulse animation, label in primary color
     - **Upcoming**: steps after current → gray empty circle, gray dashed connecting line
   - If status is ERRORED: show all steps as gray, add a red "Error" step at the end with the error icon
   - Connecting lines between steps (vertical, 2px wide)
   - Step labels use user-friendly text (e.g., "Order Placed" instead of just "PLACED")
   - Current step should have a subtle pulse animation on the circle

4. **Order Details card**: Key-value rows in a clean list:
   - Order ID (monospace)
   - Fund ID (monospace)
   - Account ID (monospace)
   - Side (colored — green BUY, red SELL)
   - Amount (formatted currency)
   - Quantity (formatted, or "—" if null)
   - NAV (formatted currency, or "—" if null)
   - Bulk Order ID (monospace, or "—" if null)
   - Status (StatusPill component)
   - Each row: label on left (muted), value on right (dark)

5. **Loading state**: Skeleton for hero card + progress tracker

6. **Error/Not found**: If order not found, show EmptyState with "Order not found" message and back button

## Step Label Mapping
```js
const STEP_LABELS = {
  PLANNED: 'Order Planned',
  VALIDATED: 'Validated',
  ENRICHED: 'Enriched',
  PLACED: 'Order Placed',
  BULKED: 'Grouped',
  CONFIRMED: 'Confirmed',
  CONTRACTED: 'Contracted',
  BOOKED: 'Completed',
}
```

## Progress Tracker Styling

```css
.progress-tracker {
  padding: var(--sp-4) var(--sp-5);
}

.progress-step {
  display: flex;
  align-items: flex-start;
  gap: var(--sp-3);
  position: relative;
}

.progress-step-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.progress-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--color-border);
  background: var(--color-surface);
}

.progress-dot.completed {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.progress-dot.current {
  background: var(--color-primary);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px var(--color-primary-100);
  animation: pulse 2s ease-in-out infinite;
}

.progress-dot.errored {
  background: var(--color-error);
  border-color: var(--color-error);
  color: white;
}

.progress-line {
  width: 2px;
  height: 24px;
  background: var(--color-border);
  margin: 2px 0;
}

.progress-line.completed {
  background: var(--color-primary);
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 4px var(--color-primary-100); }
  50% { box-shadow: 0 0 0 8px rgba(5, 150, 105, 0.1); }
}
```

## Important Notes
- This page hides the bottom nav — detect route in BottomNav and hide on `/orders/:id`
- The progress tracker is the key visual — make it feel polished and app-like
- Use `useParams` to get `orderId`, then fetch via `getOrders({ orderID: orderId })`
- The API returns an array even for single order — use `data[0]`
- If `errorDescription` exists on the order, show it below the errored step in red text
- All IDs displayed in monospace font
- The back button should work with browser back (navigate(-1)), not hardcoded route
- Wire up the route in App.jsx: `/orders/:orderId`
