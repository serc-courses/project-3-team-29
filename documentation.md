# Order Management System (OMS) Documentation

## 1. Overview
The **Mutual Fund Order Management System (OMS)** is a Java-based RESTful application designed to manage the lifecycle of mutual fund orders. It connects to a PostgreSQL database for the primary persistence of operational data and implements an optional CQRS pattern with MongoDB for scalable read models. The system exposes a REST API via `com.sun.net.httpserver` to handle order planning, lifecycle transitions, and status checks.

## 2. Architecture & Design Patterns
The application architecture is structured into clear layers to maintain a robust separation of concerns, heavily applying Domain-Driven Design (DDD) concepts and event-driven patterns.

### CQRS (Command Query Responsibility Segregation)
- **Write-Side**: Rest APIs that process transactions and mutate domain state (e.g., `OrderManager`, `OrderStateMachine`). Data is written primarily to the PostgreSQL database.
- **Read-Side** (`com.iiit.oms.readmodel`): Exposes highly available representation of data optimized for queries. It contains view models (`OrderView`, `BulkOrderView`) persisting optionally in MongoDB for scalable querying, powered by the `OrderProjectionListener` bridging write-side events to updates in the read-side layer.

### State Machine Pattern
The system leverages a state machine (`OrderStateMachine`) to enforce correct order transitions rigorously. Status progresses sequentially:
`PLANNED -> VALIDATED -> ENRICHED -> PLACED -> BULKED -> CONFIRMED -> BOOKED`
Orders also have an `ERRORED` state for graceful failure handling.

### Scheduled Batching
- Uses a background scheduler (`BatchoutScheduler`) to aggregate valid `PLACED` orders into aggregated `BulkOrder` batches at a fixed interval (120 secs default). Bulk orders are sorted and dispatched to executing parties (simulated) collectively to reduce transaction overhead.

## 3. Package Structure

- **`com.iiit.oms.model`**: Core domain resources – `Order`, `BulkOrder`, `Account`, `Fund`, and essential enums defining states (`OrderStatus`, `OrderSide`, `BulkOrderStatus`).
- **`com.iiit.oms.interfaces`**: Boundary layer containing the REST controller `OrderRestServer`. Maps routes for planning, listing, viewing, status checking, and manual confirmation endpoints.
- **`com.iiit.oms.processor`**: Core domain logic controllers. Contains the `OrderStateMachine`, `OrderManager`, and asynchronous task runners like `BatchoutScheduler` and `OrderScheduler`.
- **`com.iiit.oms.repository`**: Defines repository abstractions. It includes multiple storage engine implementations spanning `inmemory` and `postgres`.
- **`com.iiit.oms.readmodel`**: Read-model specifics for the CQRS setup. Includes listener implementations mapping `Order` state changes to `View` events saved into `ProjectionStore`.
- **`com.iiit.oms.db`**: Common connectivity configurations to construct database instances and query mapping templates interfaces. 

## 4. Workflows

### Order Lifecycle Event Flow
1. **Plan Orders**: User submits POST `/orders/plan` containing basic raw instruction (`productID`, `amount`, `accountID`, `BUY/SELL`). The `OrderRestServer` validates payload, maps to internal `Order`, sets state to `PLANNED` and invokes processor logic.
2. **State Advancement**: Handled by `OrderStateMachine` which moves orders rapidly through internal states (`VALIDATED` with rules, `ENRICHED` with related fund metadata) until arriving at `PLACED`.
3. **Batching Phase**: `BatchoutScheduler` periodic tick processes unbatched `PLACED` orders, generating mapped `BulkOrder` entries tracking them. Source orders transition to `BULKED`.
4. **Fulfillment / Booking**: Bulk confirmations trigger underlying orders to `CONFIRMED`. Subsequent logic checks finalize the quantities considering NAV (Net Asset Value) execution rates shifting order to terminal `BOOKED` state.
5. **View Re-Projection**: Every mutation throughout this cycle asynchronously triggers `OrderProjectionListener` bridging logic ensuring `OrderView` query representation stays eventually (or synchronously) consistent for user reading.

## 5. Startup & Environment Configuration
Refer to the native `README.md` for full startup requirements and configuration options. The application primarily respects:
- **`OMS_DB_URL`**: Postgres/DB JDBC connect URL.
- **`OMS_DB_CLEAN_START`**: Set to `true` to purge prior DB schema/data on start.
- **`OMS_DB_USER`** / **`OMS_DB_PASSWORD`**: DB role credentials.

Run application instances through the main Java entry-point `com.iiit.oms.OmsApplication`.
