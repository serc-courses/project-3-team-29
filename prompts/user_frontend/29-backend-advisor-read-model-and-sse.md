# Prompt 29: Backend — Advisor Read Model & SSE Support

## Context
Advisor mode becomes much cleaner if the backend can serve advisor-scoped views and optionally advisor-scoped SSE events.

## Task
Add advisor-aware read-model helpers and SSE support to the Java backend.

## Goals
1. Allow advisor dashboards to be built server-side
2. Filter order and aggregate views by advisor ownership
3. Optionally expose advisor-scoped SSE payloads

## Work Items

### 1. Advisor-scoped projection queries
Add helper methods or service-layer functions that can:
- collect all client account IDs for an advisor
- fetch read-model orders across those accounts
- aggregate statuses and totals server-side

If modifying `ProjectionStore` is reasonable, add methods like:
- `findOrdersByAccounts(List<String> accountIDs)`
- `summarizeAdvisorDashboard(String advisorID)`

If not, compose this in a service class above the store.

### 2. Advisor dashboard response
Ensure `GET /advisor/dashboard` can compute:
- client count
- total amount
- total quantity
- active order count
- failed order count
- top clients by amount

### 3. Advisor SSE
Add one of these options:

#### Preferred
`GET /advisor/stream`
- accepts advisor identity
- emits only events relevant to that advisor's clients

#### Acceptable fallback
Continue using `/view/stream`, but enrich events enough for the frontend to filter by `accountID` or `orderID`

### 4. Event payload enrichment
Ensure order-related SSE payloads include enough data for advisor filtering:
- `orderID`
- `accountID`
- `fundID`
- `orderStatus`
- `bulkOrderID` if present

## Important Notes
- Keep investor SSE behavior unchanged
- Do not overcomplicate the projection store if a thin advisor aggregation service is simpler
- Favor server-side filtering over pushing all advisor composition into the mobile app