# Prompt 24: User Frontend — Advisor Activity Page

## Context
Advisor Activity is the cross-client equivalent of investor order history. It must help the advisor scan all managed client orders with strong filtering and visible client identity.

## Task
Create `user-frontend/src/pages/AdvisorActivity/AdvisorActivity.jsx` and `AdvisorActivity.css`.

## API Calls
```js
import { getAdvisorClients, getAdvisorClientOrders } from '../../api/advisorApi'
```

## Page Layout

```
┌─────────────────────────────┐
│  Activity                   │
│                             │
│  [All Clients ▼] [Status ▼] │
│  [Fund search___________]   │
│                             │
│  ┌─────────────────────────┐│
│  │ ACCT00001               ││
│  │ FND001 · BUY · ₹1,000   ││
│  │ PLACED                  ││
│  └─────────────────────────┘│
│  ┌─────────────────────────┐│
│  │ ACCT00003               ││
│  │ FND004 · SELL · ₹2,500  ││
│  │ ERRORED                 ││
│  └─────────────────────────┘│
└─────────────────────────────┘
```

## Requirements
1. Page title: `Activity`
2. Aggregate orders across all advisor-owned clients
3. Add filter controls for:
   - client account
   - status group (All / Active / Pending / Completed / Failed)
   - fund ID or fund name search
   - side (BUY / SELL / All)
4. Each activity card must include:
   - account ID as the top label
   - fund name or fund ID
   - amount
   - side
   - status
   - optional bulk order ID if available
5. Cards should navigate to the existing order detail page if order IDs are available
6. Support query-param based prefiltering from client detail or success flows
7. Loading state shows 5 skeleton cards
8. Empty state when no matching activity remains after filters

## Important Notes
- This page should feel more operational than the investor orders page
- Keep filters sticky near the top on mobile for quick scan-and-refine behavior
- Use the same status color mappings already defined in the investor constants