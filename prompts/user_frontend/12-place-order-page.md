# Prompt 12: User Frontend — Place Order Page (Mobile Order Form)

## Context
This is the order placement form — where investors actually put money into mutual funds. It must be mobile-optimized, feel like a fintech order ticket, and have clear validation.

## Task
Create `user-frontend/src/pages/PlaceOrder/PlaceOrder.jsx` and `PlaceOrder.css`.

## API Calls
```js
import { planOrders } from '../../api/ordersApi'
import { getFunds } from '../../api/portfolioApi'
import { getAccounts } from '../../api/portfolioApi'
import { CONFIG } from '../../constants/config'
```

`POST /orders/plan` accepts:
```json
[
  { "productID": "FND001", "amount": 1000, "accountID": "ACCT00001", "orderSide": "BUY" }
]
```

Response (201):
```json
{ "message": "Orders planned", "count": 1, "orderIDs": ["ORD1234"] }
```

## Page Layout

```
┌─────────────────────────────┐
│  ← Back         Place Order │  ← top bar with back
│                             │
│  Select Fund                │
│  ┌─────────────────────────┐│
│  │ ▼ Choose a fund...      ││  ← fund dropdown (full width)
│  └─────────────────────────┘│
│                             │
│  Select Account             │
│  ┌─────────────────────────┐│
│  │ ▼ Choose account...     ││  ← account dropdown
│  └─────────────────────────┘│
│                             │
│  Amount (₹)                 │
│  ┌─────────────────────────┐│
│  │ ₹ 1,000                ││  ← amount input
│  └─────────────────────────┘│
│                             │
│  ┌──────────┐ ┌──────────┐ │
│  │   BUY    │ │   SELL   │ │  ← toggle buttons (not dropdown)
│  │ (active) │ │          │ │
│  └──────────┘ └──────────┘ │
│                             │
│  ─────────────────────────  │
│  Order Summary              │
│  Fund: Technology MF 1      │
│  Amount: ₹1,000.00          │
│  Side: BUY                  │
│  ─────────────────────────  │
│                             │
│  ┌─────────────────────────┐│
│  │    Place Order           ││  ← sticky bottom CTA
│  └─────────────────────────┘│
└─────────────────────────────┘
```

## Requirements

1. **Top bar**: Back arrow + "Place Order" title. Bottom nav hidden on this route.

2. **Fund selection**:
   - Full-width `<select>` styled as a custom dropdown
   - Options from `getFunds()` response: show `fundName` as label, `fundID` as value
   - Format: "Technology MF 1 (FND001)"
   - If fund aggregates return empty (no orders exist), use known seed data: funds FND001–FND050 with generic names "Fund {n}"
   - Default: "Choose a fund..." placeholder option
   - Required field with error message if empty on submit

3. **Account selection**:
   - Full-width `<select>` dropdown
   - Options from `getAccounts()` response: show `accountID`
   - If account aggregates return empty, fall back to `CONFIG.ACCOUNTS_SEED` (ACCT00001–ACCT00010)
   - Default: "Choose account..." placeholder
   - Required field

4. **Amount input**:
   - Numeric input with ₹ prefix label
   - `type="number"`, `inputMode="decimal"` for mobile numeric keyboard
   - `min="1"`, `step="100"`
   - Required, must be > 0
   - Show error message if invalid: "Amount must be greater than 0"
   - Style the input larger than default for easy tapping

5. **Side toggle**:
   - Two toggle buttons side by side: BUY and SELL
   - NOT a `<select>` dropdown — use button toggle for mobile UX
   - BUY: green/emerald when active, SELL: red when active
   - BUY selected by default
   - Only one can be active at a time

6. **Order summary** (appears once all fields are filled):
   - Shows below a divider
   - Lists: Fund name, Amount (formatted), Side
   - Subtle card or light background

7. **Sticky CTA button**:
   - "Place Order" primary button fixed at bottom
   - Disabled until all fields are valid
   - On tap:
     - Show loading state (spinner + "Placing order...")
     - Call `planOrders([{ productID, amount, accountID, orderSide }])`
     - Note: `productID` = the selected `fundID`
     - On success: show success Sheet/modal with:
       - Checkmark SVG animation
       - "Order Placed!" title
       - Order ID(s) displayed in monospace
       - "View Order" button → navigate to `/orders/{orderID}`
       - Auto-redirect to `/orders` after 3 seconds
     - On error: show error toast, keep form data

8. **Validation**:
   - Run on submit attempt
   - Show inline error text below each invalid field (in red, `--text-xs`)
   - Fields: fund (required), account (required), amount (required, > 0), side (always valid since default is BUY)

## Success State

```
┌─────────────────────────────┐
│                             │
│          ✓                  │  ← animated checkmark (CSS)
│    Order Placed!            │
│                             │
│    Order ID: ORD1234        │
│                             │
│    [View Order]             │
│                             │
│    Redirecting to orders... │
└─────────────────────────────┘
```

The success state should be a Sheet (bottom sheet) that covers the form.

## Checkmark Animation CSS
```css
.success-check {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--sp-4);
  animation: scaleIn 0.3s var(--ease-out);
}

@keyframes scaleIn {
  from { transform: scale(0); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
```

## Important Notes
- Bottom nav is hidden on this route — already configured in prompt 05
- `productID` in the API = `fundID` in the UI — map when building the request payload
- Do NOT send `orderID`, `quantity`, `orderStatus` — backend generates these
- The form sends exactly ONE order (not multi-row like admin) — users place one order at a time
- Use `inputMode="decimal"` on amount input for mobile keyboard optimization
- Side toggle buttons should have proper `aria-pressed` attribute for accessibility
- Do NOT allow form submission while request is in-flight
- The success flow (Sheet → auto-redirect) should clear any timeouts if user manually navigates away
- Wire up route in App.jsx: `/orders/new`
