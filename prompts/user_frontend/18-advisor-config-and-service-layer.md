# Prompt 18: User Frontend — Advisor Config & Service Layer

## Context
Advisor mode needs a client roster, helper APIs, and role-aware constants. If backend prompts 27-30 are implemented, the frontend should consume real advisor APIs. If they are not implemented yet, use frontend config as a fallback.

## Task
Create advisor-specific constants and a service layer that prefers real advisor APIs, with a graceful fallback to composed existing OMS APIs.

## Files to Create or Update

### `user-frontend/src/constants/advisorConfig.js`
Create advisor-mode configuration.

```js
export const ADVISOR_CONFIG = {
  DEFAULT_ADVISOR_ID: 'ADV001',
  ADVISOR_CLIENT_MAP: {
    ADV001: ['ACCT00001', 'ACCT00002', 'ACCT00003', 'ACCT00004', 'ACCT00005'],
    ADV002: ['ACCT00006', 'ACCT00007', 'ACCT00008', 'ACCT00009', 'ACCT00010']
  },
  STORAGE_KEYS: {
    role: 'oms_user_role',
    advisorId: 'oms_advisor_id',
    selectedClientId: 'oms_selected_client_id',
    advisorDraftBasket: 'oms_advisor_draft_basket'
  }
}
```

### Update `user-frontend/src/constants/routes.js`
Add advisor routes.

```js
export const ROUTES = {
  home: '/',
  orders: '/orders',
  funds: '/funds',
  account: '/account',
  advisorHome: '/advisor',
  advisorClients: '/advisor/clients',
  advisorClientDetail: '/advisor/clients/:accountId',
  advisorNewOrders: '/advisor/orders/new',
  advisorOrderReview: '/advisor/orders/review',
  advisorActivity: '/advisor/activity'
}
```

### `user-frontend/src/api/advisorApi.js`
Create a service layer that first checks for real advisor endpoints, then falls back to current APIs if needed.

## API Calls
```js
import { getAccounts, getFunds } from './portfolioApi'
import { getOrders, planOrders } from './ordersApi'
import { ADVISOR_CONFIG } from '../constants/advisorConfig'
```

If backend support exists, also allow these endpoints:
```js
// Preferred backend-backed endpoints
GET /advisor/me
GET /advisor/clients
GET /advisor/dashboard
GET /advisor/orders
POST /advisor/orders/plan
GET /advisor/stream
```

### Functions to implement
1. `getAdvisorClients(advisorId)`
   - Prefer `GET /advisor/clients`
   - Fallback: load all account aggregates from `getAccounts()` and filter to `ADVISOR_CLIENT_MAP`
   - If aggregates are empty, synthesize client cards from the allowlist

2. `getAdvisorClientOrders(accountID)`
   - Prefer `GET /advisor/orders?accountID=...`
   - Fallback: call `getOrders({ accountID })`

3. `planAdvisorBasket(orderRows)`
   - Accept an array of advisor draft rows
   - Map each row to `{ productID, amount, accountID, orderSide }`
   - Prefer `POST /advisor/orders/plan`
   - Fallback: submit one `planOrders(payload)` request

4. `getAdvisorBookOverview(advisorId)`
   - Prefer `GET /advisor/dashboard`
   - Fallback: compose a front-end aggregate using `getAdvisorClients(advisorId)`
   - Optionally query each client's orders in parallel for richer counts
   - Return totals like:
     - clientCount
     - totalInvested
     - activeOrders
     - failedOrders

### `user-frontend/src/api/index.js`
Export the advisor API.

## Requirements
1. Keep the service layer honest: prefer real advisor endpoints if created, otherwise use explicit fallback logic
2. The client roster must be filterable by advisor ID
3. Each advisor draft basket row must preserve:
   - `accountID`
   - `fundID`
   - `amount`
   - `orderSide`
4. Provide helper functions for:
   - grouping basket rows by client
   - computing basket totals
   - sorting clients by invested amount or activity count
5. Store advisor draft basket rows in localStorage so in-progress basket work survives refresh
6. Make the fallback path explicit in code comments or function names where useful

## Important Notes
- If backend prompts 27-30 are implemented, the frontend should stop claiming entitlements are mocked-only
- Entitlements may remain mocked in frontend constants only as a fallback path
- Keep the file names and export pattern consistent with the existing API layer