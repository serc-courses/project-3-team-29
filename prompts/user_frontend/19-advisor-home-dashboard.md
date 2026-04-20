# Prompt 19: User Frontend — Advisor Home Dashboard

## Context
Advisor Home is the control center for the client book. It should summarize the advisor's managed accounts, highlight issues, and surface quick actions.

## Task
Create `user-frontend/src/pages/AdvisorHome/AdvisorHome.jsx` and `AdvisorHome.css`.

## API Calls
```js
import { getAdvisorBookOverview, getAdvisorClients } from '../../api/advisorApi'
import { useRole } from '../../hooks/useRole'
```

## Page Layout

```
┌─────────────────────────────┐
│  Advisor Book               │
│  ADV001 · 5 clients         │
│                             │
│  ┌─────────────────────────┐│
│  │  ₹ 3.45L               ││  ← total advised assets/orders amount
│  │  14 active · 2 failed   ││
│  └─────────────────────────┘│
│                             │
│  ┌──────┐ ┌──────┐ ┌──────┐ │
│  │  5   │ │ 14   │ │  2   │ │
│  │clients││active│ │failed│ │
│  └──────┘ └──────┘ └──────┘ │
│                             │
│  Quick Actions              │
│  [ New Basket ] [ Clients ] │
│                             │
│  Attention Needed           │
│  ┌─────────────────────────┐│
│  │ ACCT00003 · 1 errored   ││
│  ├─────────────────────────┤│
│  │ ACCT00005 · 2 pending   ││
│  └─────────────────────────┘│
│                             │
│  Top Clients                │
│  ┌─────────────────────────┐│
│  │ ClientCard              ││
│  └─────────────────────────┘│
└─────────────────────────────┘
```

## Requirements
1. Page title: `Advisor Book`
2. Hero summary card includes:
   - advisor ID
   - total client count
   - total invested / tracked amount
   - active order count
   - failed order count
3. Summary stats row with 3 compact metric cards
4. Quick actions:
   - New Basket → `/advisor/orders/new`
   - Browse Clients → `/advisor/clients`
5. `Attention Needed` section:
   - show only clients with failed or high pending counts
   - each row clickable to client detail
6. `Top Clients` section:
   - list top 3 to 5 clients by total amount or order count
   - reuse a compact client card pattern
7. Loading state:
   - skeleton hero
   - skeleton metric row
   - skeleton client list
8. Empty state:
   - if no clients assigned, show `No clients assigned to this advisor`

## Important Notes
- This dashboard is composed client-side using advisor API helpers
- Use the same consumer-fintech palette, but increase density slightly for advisor mode
- Show account IDs prominently because advisors care about which client needs attention