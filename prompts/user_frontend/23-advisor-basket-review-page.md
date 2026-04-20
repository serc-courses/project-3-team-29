# Prompt 23: User Frontend — Advisor Basket Review & Submit Page

## Context
Advisors should review a basket before submission. This is where they confirm each client/fund/amount row and submit the final array to the OMS backend.

## Task
Create `user-frontend/src/pages/AdvisorBasketReview/AdvisorBasketReview.jsx` and `AdvisorBasketReview.css`.

## API Calls
```js
import { planAdvisorBasket } from '../../api/advisorApi'
import { useNavigate } from 'react-router-dom'
```

## Page Layout

```
┌─────────────────────────────┐
│ ← Back    Review Basket     │
│                             │
│  4 orders · 3 clients       │
│  ₹18,000 total              │
│                             │
│  Client: ACCT00001          │
│  ┌─────────────────────────┐│
│  │ FND001 · BUY · ₹1,000   ││
│  │ FND002 · SELL · ₹2,000  ││
│  └─────────────────────────┘│
│                             │
│  Client: ACCT00003          │
│  ┌─────────────────────────┐│
│  │ ...                     ││
│  └─────────────────────────┘│
│                             │
│  [ Submit Basket ]          │
└─────────────────────────────┘
```

## Requirements
1. Read basket rows from localStorage or navigation state
2. Group rows by `accountID`
3. Show per-client groupings with each order row summarized clearly
4. Top summary area shows:
   - number of rows
   - total amount
   - client count
   - BUY vs SELL split
5. Provide `Edit Basket` action to go back to `/advisor/orders/new`
6. Submit CTA behavior:
   - disabled if no valid rows exist
   - loading state while request is in flight
   - call `planAdvisorBasket(rows)`
   - success state must show all returned order IDs
   - clear basket draft from localStorage on success
7. Success UI:
   - Sheet or full-page success state
   - `Basket Submitted`
   - show order IDs in a scrollable mono list
   - CTA to `View Activity`
   - CTA to `Start New Basket`
8. Error handling:
   - show a toast if submit fails
   - keep the basket intact so advisor can retry

## Important Notes
- This page is critical for operational safety; clarity matters more than visual flair
- Do not auto-submit directly from the basket entry form
- The backend returns `orderIDs`, so surface them explicitly after success