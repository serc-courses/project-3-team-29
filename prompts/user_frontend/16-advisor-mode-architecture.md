# Prompt 16: User Frontend — Advisor Mode Architecture Overview

## Context
The current `user-frontend/` prompt set models a single-investor mobile app. Add a second persona: **Advisor**. An advisor represents multiple client accounts and can review multiple client portfolios, monitor cross-client activity, and place orders on behalf of several clients.

This must be built as an **extension of the same mobile app**, not a third standalone frontend.

## Backend Strategy
The backend currently has:
- `POST /orders/plan` accepting an array of orders
- `GET /view/orders?accountID=...` for account-specific order views
- `GET /view/aggregates/accounts`
- `GET /view/aggregates/funds`
- `GET /view/dashboard`
- `GET /view/stream`

The backend does **not yet clearly expose**:
- advisor authentication
- advisor-to-client mapping APIs
- household/advisor aggregate APIs
- audit metadata such as `placedByAdvisor`

Claude Code is allowed to add missing backend APIs, models, repositories, read-model support, and lightweight authorization flows if needed to make advisor mode real. Only use frontend-only fallbacks when a backend addition would be too large for the task or clearly out of scope.

## Task
Create advisor-mode prompts that extend the current user app with:
1. Role-aware app shell
2. Advisor dashboard
3. Client directory
4. Client detail page
5. Advisor basket order entry for multiple clients
6. Basket review page before submit
7. Cross-client activity page
8. Advisor SSE refresh behavior
9. Role and entitlement config
10. Explicit documentation of backend gaps
11. Backend implementation prompts when advisor APIs are missing

## Product Definition

### Personas
1. **Investor**
   - Views one selected account
   - Places one order at a time
   - Tracks own order history

2. **Advisor**
   - Views many client accounts
   - Places one or many orders across multiple client accounts
   - Monitors cross-client operational status
   - Needs strong filtering and clear client identity everywhere

## Advisor UX Principles
1. Mobile-first, but denser than investor mode
2. Every advisor order card must prominently show `accountID`
3. Multi-step review is required before basket submission
4. Client context must persist across navigation
5. Prefer real backend support over pretending in the UI
6. Use frontend mocks only as a fallback path

## Advisor Information Architecture

### Advisor Bottom Tabs
| Tab | Route | Purpose |
|-----|-------|---------|
| Home | `/advisor` | Book overview and alerts |
| Clients | `/advisor/clients` | Client list and search |
| Activity | `/advisor/activity` | Cross-client order history |
| Account | `/account` | Role switcher and app settings |

### Additional Advisor Routes
| Route | Page |
|-------|------|
| `/advisor/clients/:accountId` | Client detail |
| `/advisor/orders/new` | Basket order entry |
| `/advisor/orders/review` | Basket review and submit |

## Proposed Prompt Sequence
```text
16 — Advisor mode architecture overview
17 — Role switcher and advisor shell
18 — Advisor config and service layer
19 — Advisor home dashboard
20 — Clients directory page
21 — Client detail page
22 — Advisor basket order page
23 — Basket review page
24 — Advisor activity page
25 — Advisor SSE integration and live alerts
26 — Backend gaps and future APIs
27 — Backend advisor domain and repositories
28 — Backend advisor REST APIs
29 — Backend advisor read model and SSE
30 — Backend advisor auth and audit support
```

## Important Notes
- Advisor mode must coexist with investor mode in the same app shell
- Prefer implementing missing backend endpoints instead of hardcoding frontend-only workarounds
- Use seeded account IDs as the temporary client roster fallback only when backend relationship APIs are still absent
- `POST /orders/plan` already supports basket submission because it accepts an array
- The generated app should make it obvious where behavior is production-ready vs mocked locally