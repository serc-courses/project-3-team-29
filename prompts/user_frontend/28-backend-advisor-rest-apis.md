# Prompt 28: Backend — Advisor REST APIs

## Context
Once advisor repositories exist, expose dedicated APIs so the frontend does not need to fake advisor entitlements or compose everything client-side.

## Task
Extend the Java HTTP server with advisor-specific REST endpoints.

## Endpoints to Add

### `GET /advisor/me`
Return the current advisor identity.

For now, accept advisor identity via a lightweight mechanism such as:
- request header `X-Advisor-ID`, or
- query parameter `advisorID`

Response example:
```json
{
  "advisorID": "ADV001",
  "name": "Advisor 1",
  "clientCount": 5
}
```

### `GET /advisor/clients`
Return client accounts assigned to the advisor.

Response example:
```json
[
  {
    "accountID": "ACCT00001",
    "orderCount": 12,
    "totalAmount": 45000.0,
    "totalQuantity": 315.67,
    "statuses": { "BOOKED": 5, "ERRORED": 1 }
  }
]
```

### `GET /advisor/orders`
Return orders limited to the advisor's client accounts.

Supported query params:
- `accountID`
- `fundID`
- `status`

### `POST /advisor/orders/plan`
Plan orders on behalf of advisor clients.

Request body:
```json
[
  { "productID": "FND001", "amount": 1000, "accountID": "ACCT00001", "orderSide": "BUY" }
]
```

Behavior:
- validate that each `accountID` belongs to the current advisor
- assign order IDs as usual
- persist audit metadata indicating advisor creation
- return the same response shape as `/orders/plan`

### `GET /advisor/dashboard`
Return a server-side aggregated summary for the advisor book:
- `clientCount`
- `totalAmount`
- `activeOrders`
- `failedOrders`
- optional `attentionClients`

## Implementation Notes
- Add new contexts in `OrderRestServer`
- Reuse existing projection-store logic where possible
- Keep response shapes aligned with what the frontend prompts expect
- Return `403` when an advisor tries to access a client account not assigned to them
- Return `400` when advisor identity is missing

## Important Notes
- This is lightweight advisor identity, not full auth
- Prefer header-based identity propagation for cleaner frontend integration
- Do not remove or change the existing public OMS endpoints unless needed for refactoring safety