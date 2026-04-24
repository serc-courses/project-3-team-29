# Code Fixes Log (Swam)

*This file acts as a continuous log for identifying and recording code fixes manually pushed to this project codebase.*

## Fix #1: SELL Flow Validations & Rendering
- **OrderManager.java (`validate`)**: Implemented pending SELL tracking. By iterating through and accumulating the cash value of pending SELL orders, we actively deduct this off a user's holdings to prevent **double-spending** issues before orders book.
- **OrderManager.java (`validate`)**: Fixed the 101% buffer logic that allowed short-selling. Clamped limits strictly at **99%** of available portfolio value to guarantee safety limits during NAV fluctuation.
- **OrderRestServer.java (`PortfolioHandler`)**: Refactored the dashboard's processing stream to chronologically process calculations by day (`TradeDate`), explicitly forcing `BUY` processing before `SELL` actions to permanently secure Average Cost and P&L math.
- **OrderRestServer.java (`PortfolioHandler`)**: Modified the portfolio UI renderer to deduct pending cash values dynamically from the `allocatedShares` object so users have real-time visibility that limits their current portfolio correctly.

## Fix #2: Admin UI Aggregate Funds routing
- **frontend/src/constants/routes.js**: Added missing `AGGREGATE_FUNDS: '/aggregate-funds'` inside the `ROUTES` object. The sidebar button previously had an `undefined` path which silently failed when clicked.

## Fix #3: Cash Management Simulation
- **Account Schema & Models**: Injected a `cash_balance` field across `Account.java`, `PostgresSchemaInitializer.java`, and `PostgresAccountDatabase.java`. Every account safely originates with a default $1,000,000 in sandbox cash.
- **OrderManager.java**: Enforced strict validation checks. During `BUY` operations, the system mathematically tracks uncompleted orders and securely subtracts them from the total available balance. If the new order bridges the `cashBalance`, it throws a safe standard Exception!
- **OrderRestServer.java (`ContractCallback`)**: Integrated transaction finality! When any transaction settles (converts to `BOOKED`), the system safely deposits the proceeds equivalent (from SELL payouts) directly back into the user's `cashBalance`, or physically deducts the purchase power (from BUY drafts).

## Fix #4: UI Cash Visibility
- **OrderRestServer.java (`PortfolioHandler`)**: Appended an explicit `availableCash` variable to the `/view/portfolio` route. It correctly accounts for active un-booked pending BUY orders across the application.
- **PlaceOrder.jsx**: Intercepted the new `availableCash` hook into the "Place Order" view, allowing investors to visually review their liquid cash boundary immediately within the amount text box when `BUY` is selected.

## Fix #5: Currency Standardization
- **Global Interfaces**: Audited both the Admin and Investor interfaces for currency indicator overlaps, eradicating any localized usage of the Dollar (`$`) sign. Replaced them symmetrically across `Portfolio.jsx`, `Reconciliation.jsx`, and UI charts so the platform universally scales and enforces Indian Rupees (`₹`) correctly.

## Fix #6: Aligning UI SELL Limits
- **PlaceOrder.jsx (`validate`)**: Modified the client-side SELL limit block that was mistakenly allowing orders out to `101%` (1.01x). Hardened the frontend UI to throw validation errors at exactly **99%** (`0.99x`) to strictly synchronize and match the backend `OrderManager.java`'s updated safety threshold.

## Fix #7: Non-Functional Requirement Tracking & AES-256 Encryption
- **NFR Tracking**: Established `nfrs_to_implement.md` per proposal directives to actively monitor compliance across High Availability, TLS scaling, RBAC, and Audit capabilities.
- **Data-at-Rest Security**: Upgraded `PostgresAccountDatabase.java` with AES-256 cryptographic routines (`EncryptionUtil.java`) to proactively secure client `national_identity` fields at the persistence layer, satisfying the core FINRA/compliance mandate.

## Fix #8: API Latency Observability
- **Performance Tracing**: Authored `LatencyFilter.java` and attached it universally to all endpoints mounted on `OrderRestServer.java` to track API response delays. If operations violate the maximum `500ms` SLA boundary, it actively outputs strict performance warnings to the server logs.

## Fix #9: UI Total Invested Integrity & Home Page Cash Tracking
- **Home.jsx (`stats `)**: Modified the `useMemo` block that computes `totalInvested` to forcibly ignore orders executing a `SELL` side or possessing statuses mapped inside the `STATUS_GROUP.FAILED` cluster (such as `ERRORED`/`FAILED`). Errored transaction amounts are immediately omitted from "Invested" capital displays.
- **Home.jsx (`Cash Injection`)**: Embedded `/portfolio` API fetches into the home dashboard render tree, exposing authentic `Available Cash` directly on the investor start page below total invested, drastically increasing visibility.

### Fix #10: Universal Order Cancellation Support
- Files Changed: `OrderStatus.java`, `OrderStateMachine.java`, `OrderRestServer.java`, `OrderManager.java`, `OrderCard.jsx`, `ordersApi.js`
- Change Made: Implemented the `cancelOrder` operation securely via REST mappings and injected its state-checks straight into the backend validation engine.
- Why: Investors need strict agency around order lifecycles; enables abandoning un-batched Limit or Market transactions cleanly natively restoring pending cash.

### Fix #11: Historical Transaction Ledger
- Files Changed: `Transactions.jsx`, `Account.jsx`, `App.jsx`, `ordersApi.js`, `OrderRestServer.java`
- Change Made: Erected a dedicated `/view/transactions` REST stream retrieving chronically booked orders natively without merging into trading tables, rendering them securely on a generic ledger portal.
- Why: Vital functional requirement establishing transparent tracing logic enabling clients to audit previous successfully cleared portfolio cash flows accurately.

## Fix #11: Universal Backend Cash Integrity Locks
- **OrderRestServer.java**: Discovered that Admin/Advisor APIs (`ViewAggregateAccounts`, `AdvisorClientsHandler`, `ViewDashboard`) were calculating macro-level PnL and `totalAmount` metrics without rejecting errored executions. Implemented strict `stream().filter(o -> !"ERRORED")` barriers preventing SELL items or failed transfers from inflating Top Clients and global portfolio metrics. As requested, all checks are now **seriously implemented** across both React UI logic AND Java JVM APIs.

## Fix #12: Unlocking Real-World Decimal Inputs
- **PlaceOrder.jsx & AdvisorBasketOrder.jsx**: Fixed an overly strict HTML5 validation boundary. Modified `<input type="number" step="100">` down to `step="0.01"` to support native input of fractional cents/paise values directly in the React components, eliminating the browser's aggressive integer rounding warnings.
