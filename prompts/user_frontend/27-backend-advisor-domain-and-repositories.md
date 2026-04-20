# Prompt 27: Backend — Advisor Domain Model & Repositories

## Context
The advisor UI can be built with frontend fallbacks, but Claude Code is allowed to add real backend support. Start by adding the core advisor domain model and persistence layer in the Java backend.

## Task
Add backend domain objects, repositories, and PostgreSQL persistence for advisor relationships and advisor-aware order audit metadata.

## Goals
1. Represent advisors in the backend
2. Represent advisor-to-client account relationships
3. Track when an order is placed by an advisor on behalf of a client
4. Keep the existing OMS flow intact for non-advisor order creation

## Suggested Domain Additions

### New entities
1. `Advisor`
   - `advisorID`
   - `name`
   - `email` or `code`

2. `AdvisorClientRelationship`
   - `advisorID`
   - `accountID`
   - `relationshipStatus`

### Order audit additions
Add optional fields to `Order` if practical:
- `createdByRole` (`INVESTOR` or `ADVISOR`)
- `createdByID`
- `placedForAccountID` if distinct from `accountID`

If modifying `Order` is too invasive, introduce a separate `OrderAuditMetadata` table keyed by `orderID`.

## Repository Work
Create interfaces and PostgreSQL implementations for:
- `AdvisorRepository`
- `AdvisorClientRelationshipRepository`

Required methods:
- `findByAdvisorId(String advisorID)`
- `findAll()`
- `save(...)`
- `findClientAccountIds(String advisorID)`
- `findAdvisorsByAccountId(String accountID)`

## Schema Work
Update PostgreSQL schema initialization to create tables such as:
- `advisors`
- `advisor_client_relationships`
- optional `order_audit_metadata`

Suggested columns:
```sql
advisors(advisor_id primary key, name, email)
advisor_client_relationships(advisor_id, account_id, relationship_status, primary key(advisor_id, account_id))
order_audit_metadata(order_id primary key, created_by_role, created_by_id, placed_for_account_id)
```

## Seed Data
Seed 2 advisors and map them to the 10 seeded accounts:
- `ADV001` -> `ACCT00001` to `ACCT00005`
- `ADV002` -> `ACCT00006` to `ACCT00010`

## Important Notes
- Keep changes minimal and consistent with existing repository patterns
- Do not break the current investor flow
- If you add audit fields to `Order`, update serialization and database persistence accordingly
- If a separate audit table is cleaner, prefer that over risky changes to the main order schema