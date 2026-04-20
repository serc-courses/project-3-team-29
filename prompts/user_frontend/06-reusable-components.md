# Prompt 06: User Frontend — Reusable Components

## Context
Build the shared components used across multiple pages in the user mobile app. These are mobile-optimized, touch-friendly, and card-based.

## Task
Create all reusable components in `user-frontend/src/components/`.

## Components to Create

### 1. `StatusPill` — Order status indicator

File: `user-frontend/src/components/StatusPill/StatusPill.jsx` and `StatusPill.css`

A small colored pill that shows the user-friendly status label.

```
┌──────────────────┐
│ ● Processing     │   ← colored dot + label, rounded pill
└──────────────────┘
```

Requirements:
- Import `STATUS_LABEL` from constants to map internal status to user-friendly label
- Import `STATUS_COLORS` to get `bg`, `text`, `border` for each status
- Show a small colored dot (6px circle) before the label text
- Pill shape: `border-radius: 9999px`, inline-flex
- Props: `status` (string — the internal status like "BULKED")
- The pill should display the simplified label: "Processing", "Pending", "Completed", "Failed"

### 2. `OrderCard` — Order summary card

File: `user-frontend/src/components/OrderCard/OrderCard.jsx` and `OrderCard.css`

A card showing a single order's key info. Used in order lists.

```
┌───────────────────────────────────────┐
│  Technology MF 1               BUY   │  ← fund name + side badge
│  ORD1234                     ₹1,000  │  ← order ID (mono) + amount
│  ACCT00001            ● Processing   │  ← account + status pill
└───────────────────────────────────────┘
```

Requirements:
- Props: `order` (object with orderID, fundName, fundID, accountID, amount, quantity, orderSide, orderStatus, bulkOrderID)
- Clickable — wraps in a `<Link>` or accepts `onClick`
- Fund name as primary text (semibold), order side as a small colored text (green BUY, red SELL)
- Order ID in monospace, amount in monospace formatted with `formatCurrency`
- Status shown via StatusPill component
- Card has active press effect (`scale(0.98)`)

### 3. `FundCard` — Fund info card

File: `user-frontend/src/components/FundCard/FundCard.jsx` and `FundCard.css`

```
┌───────────────────────────────────────┐
│  📈 Technology MF 1          FND001  │  ← SVG icon + name + ID
│  NAV ₹142.50                         │  ← current NAV
│  ┌──────┐ ┌──────┐ ┌──────────────┐  │
│  │ 15   │ │₹25K  │ │ BUY:12 SELL:3│  │  ← stats row
│  │orders│ │total │ │  side split  │  │
│  └──────┘ └──────┘ └──────────────┘  │
└───────────────────────────────────────┘
```

Requirements:
- Props: `fund` (object with fundID, fundName, nav, orderCount, totalAmount, totalQuantity, orderSides)
- Clickable — navigates to fund detail or orders filtered by fund
- Fund name prominent, fundID in monospace muted text
- NAV displayed with ₹ formatting
- Mini stats row: order count, total amount (compact format), buy/sell split
- No emojis — use a small inline SVG chart icon for the fund

### 4. `AmountDisplay` — Formatted currency display

File: `user-frontend/src/components/AmountDisplay/AmountDisplay.jsx` and `AmountDisplay.css`

A styled amount display with currency symbol and monospace numbers.

Requirements:
- Props: `amount` (number), `size` ('sm' | 'md' | 'lg'), `showSign` (boolean)
- Renders in monospace font with `font-feature-settings: 'tnum'`
- Currency symbol (₹) in slightly smaller, lighter weight
- Large variant for hero amounts on home page
- Optional green/red color based on positive/negative or buy/sell context

### 5. `EmptyState` — Friendly empty state

File: `user-frontend/src/components/EmptyState/EmptyState.jsx`

Requirements:
- Props: `icon` (SVG element), `title`, `message`, `action` (optional button label), `onAction` (callback)
- Centered vertically, with the SVG icon, title, message, and optional CTA button
- Uses the `.empty-state` classes from components.css
- Icon should be a CSS-drawn simple illustration (e.g., empty box, magnifying glass)

### 6. `Sheet` — Bottom sheet / modal

File: `user-frontend/src/components/Sheet/Sheet.jsx` and `Sheet.css`

A bottom sheet overlay for confirmations, filters, and detail views.

```
┌─────────────────────────────┐
│  (dimmed backdrop)          │
│                             │
│  ┌─────────────────────────┐│
│  │  ─── (drag handle)      ││
│  │  Sheet Title      [✕]   ││
│  │  ─────────────────────  ││
│  │                         ││
│  │  Content goes here      ││
│  │                         ││
│  └─────────────────────────┘│
└─────────────────────────────┘
```

Requirements:
- Props: `isOpen`, `onClose`, `title`, `children`
- Backdrop: semi-transparent dark overlay, clicking it closes the sheet
- Sheet slides up from the bottom with CSS animation
- Drag handle bar at top (decorative, 36px wide, 4px tall, rounded, centered)
- Close button (X) in top-right using inline SVG
- Max height: 85vh, scrollable content
- Border-radius on top corners only
- Trap focus inside the sheet when open (basic: focus the close button on open)

### 7. `Toast` — Notification toast

File: `user-frontend/src/components/Toast/Toast.jsx` and `Toast.css`

Requirements:
- Props: `message`, `type` ('success' | 'error' | 'warning' | 'info'), `onClose`
- Shows at the TOP of the screen (mobile pattern)
- Slides down from top with animation
- Auto-dismisses after 3 seconds
- Shows a small inline SVG icon matching the type (checkmark, X, warning triangle, info circle)
- Uses `.toast` classes from components.css

### Barrel Exports

Create `index.js` in each component directory exporting the default component.

## Important Notes
- All components use inline SVG icons — no emojis, no icon libraries
- All interactive elements have `min-height: 44px` tap targets
- Use `:active` pseudo-class for touch feedback, not `:hover`
- OrderCard and FundCard use the `card-interactive` class from components.css
- Sheet uses `position: fixed` and `z-index: 200` (above bottom nav)
- Toast uses `z-index: 9999` (above everything)
- Import formatters from `utils/formatters.js` — do not inline formatting logic
- Import status constants from `constants/` — do not hardcode status strings
