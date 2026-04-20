# Prompt 17: User Frontend — Role Switcher & Advisor App Shell

## Context
The app now supports two personas: Investor and Advisor. Add a lightweight role switcher and a role-aware app shell so the same codebase can render either experience cleanly.

## Task
Update the app shell to support:
- `investor` mode
- `advisor` mode
- role persistence in `localStorage`
- role-aware bottom navigation
- advisor-only routes

## Files to Create or Update

### `user-frontend/src/hooks/useRole.js`
Create a hook that persists the selected role.

```js
import { useState, useCallback } from 'react'

const STORAGE_KEY = 'oms_user_role'

export function useRole() {
  const [role, setRole] = useState(() => {
    return localStorage.getItem(STORAGE_KEY) || 'investor'
  })

  const selectRole = useCallback((nextRole) => {
    setRole(nextRole)
    localStorage.setItem(STORAGE_KEY, nextRole)
  }, [])

  return { role, selectRole, isAdvisor: role === 'advisor' }
}
```

### Update `user-frontend/src/components/BottomNav/BottomNav.jsx`
Make the bottom nav role-aware.

#### Investor tabs
- Home → `/`
- Orders → `/orders`
- Funds → `/funds`
- Account → `/account`

#### Advisor tabs
- Home → `/advisor`
- Clients → `/advisor/clients`
- Activity → `/advisor/activity`
- Account → `/account`

## App Layout

### Role Switcher Sheet
```
┌─────────────────────────────┐
│  Choose Experience          │
│                             │
│  ┌───────────────────────┐  │
│  │ Investor              │  │
│  │ Personal portfolio    │  │
│  └───────────────────────┘  │
│                             │
│  ┌───────────────────────┐  │
│  │ Advisor               │  │
│  │ Client book & baskets │  │
│  └───────────────────────┘  │
└─────────────────────────────┘
```

## Requirements
1. Add `useRole` hook and export it from `src/hooks/index.js`
2. Update `App.jsx` to initialize the role and render routes conditionally
3. Add a first-run role picker Sheet if no role is stored yet
4. The Account page should include a compact role switch card so users can switch later
5. Bottom nav labels and icons change based on role
6. Hidden route rules must remain intact:
   - investor detail pages still hide the bottom nav
   - advisor basket entry and review pages also hide the bottom nav
7. Advisor routes to wire in `App.jsx`:
   - `/advisor`
   - `/advisor/clients`
   - `/advisor/clients/:accountId`
   - `/advisor/orders/new`
   - `/advisor/orders/review`
   - `/advisor/activity`
8. If the user is in advisor mode and manually visits an investor-only route, allow it, but advisor nav remains active only on advisor root pages

## Visual Direction
- Reuse the same emerald/slate design system
- Advisor mode should feel slightly more operational: tighter spacing, stronger labels, more metadata
- Keep the same phone-sized container and bottom-nav shell

## Important Notes
- This is a front-end role switch, not real authentication
- Persist selected role in `localStorage`
- Do not create a fake login flow
- The role picker should appear only on first app launch or when the user explicitly switches roles