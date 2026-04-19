# Prompt 26: User Frontend — Advisor Backend Gaps & Future API Notes

## Context
The advisor prompts in this sequence may either:
1. use temporary frontend-managed fallbacks, or
2. add real backend APIs, repositories, and audit fields.

Document exactly which path was implemented so the generated frontend does not misrepresent production readiness.

## Task
Create a short implementation note file at `user-frontend/ADVISOR_MODE_NOTES.md` describing current behavior and future backend work.

## Contents to Include

### 1. What Was Implemented
Document one of these two states clearly:

#### State A — Frontend fallback mode
- Advisor mode exists in the frontend only
- Role switching is stored in localStorage
- Advisor-client relationships are mocked in frontend config
- Client directory, client detail, basket entry, and basket review are functional against current APIs
- Basket submission works because `/orders/plan` accepts an array
- Activity and dashboards are composed client-side using existing account/order endpoints

#### State B — Backend-supported advisor mode
- New advisor APIs were added to the Java backend
- Advisor-client relationships are stored server-side
- Advisor dashboard and order queries come from dedicated endpoints
- Advisor basket planning uses a dedicated advisor endpoint or enhanced audit-aware order planning flow
- Audit metadata such as `placedBy`, `advisorID`, or `createdByRole` is persisted

### 2. Remaining Limitations
- No real authentication or advisor sessions
- No backend authorization checks preventing an advisor from accessing unowned accounts
- No official advisor-to-client mapping endpoint
- No backend household or advisor summary endpoint
- No `placedByAdvisor` audit fields on orders
- No per-advisor SSE filtering

If any of the above were implemented, replace that limitation with the actual state.

### 3. Recommended or Implemented Endpoints
- `GET /advisor/me`
- `GET /advisor/clients`
- `GET /advisor/dashboard`
- `GET /advisor/orders`
- `POST /advisor/orders/plan`
- `GET /advisor/stream`

### 4. Recommended or Implemented Data Model Additions
- advisor entity
- advisor-client relationship table
- order audit metadata:
  - `placedBy`
  - `placedForAccountID`
  - `channel`
  - `createdByRole`

### 5. Product Caveat
Make it explicit whether the current advisor flow is:
- demo/prototype quality with frontend fallbacks, or
- partially productionized with backend APIs but still missing full auth and entitlements.

## Important Notes
- Keep the note direct and technical, not defensive
- This prompt exists to prevent future codegen sessions from hallucinating backend support that does not yet exist or from understating what was actually implemented