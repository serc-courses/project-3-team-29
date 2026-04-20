# Prompt 30: Backend — Lightweight Advisor Auth & Audit Support

## Context
The UI needs at least minimal trust boundaries for advisor actions. Full auth is out of scope, but Claude Code can add lightweight request identity handling and audit persistence so advisor actions are distinguishable from investor actions.

## Task
Implement lightweight advisor identity resolution and audit support in the backend.

## Scope

### Identity resolution
Resolve advisor identity from request metadata such as:
- `X-Advisor-ID` header, or
- `advisorID` query param as fallback

Add a small helper utility so all advisor endpoints use the same resolution logic.

### Authorization checks
For advisor endpoints:
- reject missing advisor identity with `400`
- reject unknown advisor with `404` or `403`
- reject attempts to access unassigned client accounts with `403`

### Order audit
Whenever an advisor places orders through advisor APIs, persist audit metadata such as:
- `createdByRole = ADVISOR`
- `createdByID = advisorID`
- `placedForAccountID = accountID`

If investor orders continue through `/orders/plan`, optionally record:
- `createdByRole = INVESTOR`

### Readback support
Expose audit metadata where useful in advisor order responses, but keep current investor responses backward-compatible.

## Suggested Files to Update
- `OrderRestServer`
- `Order` model or a separate audit metadata model
- relevant repositories and postgres persistence classes
- schema initialization

## Important Notes
- Keep this lightweight and practical; do not build a full session/auth framework
- The goal is trustworthy advisor flows, not enterprise SSO
- Maintain compatibility with existing frontend/admin behavior wherever possible