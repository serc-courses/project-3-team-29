# Advisor Mode — Implementation Notes

## What Was Implemented

**State B — Backend-supported advisor mode** (with lightweight fallbacks)

The advisor flow has full backend support:

- Advisor domain entities (`Advisor`, `AdvisorClientRelationship`) and PostgreSQL persistence exist in the Java backend.
- Advisor-to-client relationships are seeded server-side (`ADV001` → `ACCT00001–ACCT00005`, `ADV002` → `ACCT00006–ACCT00010`).
- Dedicated advisor REST endpoints are live at `/advisor/me`, `/advisor/clients`, `/advisor/orders`, `/advisor/orders/plan`, and `/advisor/dashboard`.
- Advisor basket planning goes through `/advisor/orders/plan`, which validates that each target `accountID` belongs to the requesting advisor before delegating to the standard order planning flow.
- Advisor identity is resolved from the `X-Advisor-ID` request header (or `advisorID` query param as fallback).
- Role-based user model (`User.role = INVESTOR | ADVISOR`) and session management are implemented; demo users are seeded (`advisor1 / advise123`, `advisor2 / advise123`).
- The frontend prefers backend endpoints and falls back to client-side composition only when the backend returns an error.

## Remaining Limitations

- No cryptographic sessions or JWT for advisors — identity is trusted via header/param (suitable for internal/demo use only).
- No `/advisor/stream` SSE endpoint; advisor pages share the global `/view/stream` and refresh on any event.
- No per-order audit columns (`placedByAdvisor`, `createdByRole`) on the `orders` table — advisor order attribution is at the API validation layer only, not persisted with individual orders.
- Role switching is persisted in `localStorage`; there is no server-side role enforcement preventing an investor token from calling advisor endpoints.

## Implemented Endpoints

| Method | Path | Notes |
|--------|------|-------|
| GET | `/advisor/me` | Returns advisor identity and client count |
| GET | `/advisor/clients` | Returns client accounts with order aggregates |
| GET | `/advisor/orders` | Orders for advisor's clients (filterable by accountID, fundID, status) |
| POST | `/advisor/orders/plan` | Plans basket; validates accountID ownership; returns same shape as `/orders/plan` |
| GET | `/advisor/dashboard` | Book-level summary (clientCount, totalAmount, activeOrders, failedOrders) |

## Recommended Future Work

- Add `/advisor/stream` SSE endpoint filtered to advisor's client accounts.
- Persist `created_by_role` and `created_by_advisor_id` columns on the `orders` table.
- Replace header-based identity with a proper signed session for advisor endpoints.
- Add a `GET /advisor/clients/:accountId` endpoint for per-client portfolio detail (currently composed on the frontend).

## Product Status

**Partially productionized**: backend APIs and data model are real, but auth is lightweight (header-based identity, no signed tokens for advisor flows). Suitable for internal tooling or a controlled demo environment — not for multi-tenant production without adding proper advisor authentication.
