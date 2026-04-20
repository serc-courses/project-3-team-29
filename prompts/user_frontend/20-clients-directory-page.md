# Prompt 20: User Frontend — Clients Directory Page

## Context
Advisors need a searchable roster of all managed clients. This is the main browse page for the advisor book.

## Task
Create `user-frontend/src/pages/AdvisorClients/AdvisorClients.jsx` and `AdvisorClients.css`.

## API Calls
```js
import { getAdvisorClients } from '../../api/advisorApi'
```

## Page Layout

```
┌─────────────────────────────┐
│  Clients                    │
│                             │
│  ┌─────────────────────────┐│
│  │ Search by account ID    ││
│  └─────────────────────────┘│
│                             │
│  Sort: [ Amount ▼ ]         │
│                             │
│  ┌─────────────────────────┐│
│  │ ACCT00001               ││
│  │ ₹45,000 · 12 orders     ││
│  │ 5 completed · 1 failed  ││
│  └─────────────────────────┘│
│  ┌─────────────────────────┐│
│  │ ACCT00002               ││
│  │ ₹31,500 · 8 orders      ││
│  │ 3 pending               ││
│  └─────────────────────────┘│
└─────────────────────────────┘
```

## Requirements
1. Page title: `Clients`
2. Add a search input that filters by account ID in real time
3. Add a compact sort control with options:
   - Highest amount
   - Most orders
   - Most active
   - Most issues
4. Each client card should show:
   - account ID
   - total amount
   - order count
   - quantity if available
   - status snapshot like completed / pending / failed counts
5. Tapping a client card navigates to `/advisor/clients/:accountId`
6. Add a sticky floating CTA for `New Basket`
7. Empty states:
   - no assigned clients
   - no search results
8. Loading state:
   - skeleton search bar
   - skeleton list of 5 cards

## Important Notes
- This is the advisor equivalent of the investor account selector, but optimized for scanning many accounts
- Reuse card styles from the investor flow when possible, but make metadata denser
- Keep the search and sort controls touch-friendly