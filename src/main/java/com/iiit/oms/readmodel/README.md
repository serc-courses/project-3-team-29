# CQRS Read-Side Implementation Guide

This package contains the CQRS (Command Query Responsibility Segregation) read-side implementation for the OMS (Order Management System).

## Architecture Overview

```
Write-Side (Orders being processed)
        ↓
  OrderRestServer / OrderManager / OrderStateMachine
        ↓
OrderProjectionListener (bridges write → read)
        ↓
ProjectionStore (abstract interface)
        ↓
┌─────────────────────────────────────┐
│  Storage Implementation             │
├─────────────────────────────────────┤
│ InMemoryProjectionStore (POC)       │
│ MongoDbProjectionStore (Production) │
└─────────────────────────────────────┘
        ↓
Read Models (OrderView, BulkOrderView)
        ↓
REST Query Endpoints (GET /view/...)
        ↓
User Interface / Reporting
```

## Components

### 1. View Models
- `OrderView`: Flattened representation of Order optimized for queries
- `BulkOrderView`: Aggregated bulk order data with matched orders

### 2. ProjectionStore
Interface that defines operations for persisting/querying view models.

**Implementations:**
- `InMemoryProjectionStore`: For development/POC - stores views in ConcurrentHashMaps
- `MongoDbProjectionStore`: For production - persistent storage in MongoDB

### 3. OrderProjectionListener
Interface for reacting to write-side order events and updating read models.

**Implementation:**
- `DefaultOrderProjectionListener`: Default listener that delegates to ProjectionStore

## Integration Steps

### Step 1: Create ProjectionStore Instance
```java
// For POC/Development:
ProjectionStore store = new InMemoryProjectionStore();

// For Production with MongoDB:
ProjectionStore store = new MongoDbProjectionStore(
    "mongodb://localhost:27017",  // connection string
    "oms"                          // database name
);
```

### Step 2: Create Projection Listener
```java
OrderProjectionListener listener = new DefaultOrderProjectionListener(store);
```

### Step 3: Wire into Order Processing

#### In OrderRestServer.planOrdersHandler:
```java
Order order = // ... create order
listener.onOrderPlanned(order, fund, account);
```

#### In OrderStateMachine.advance() (for status changes):
```java
listener.onOrderStatusChanged(order, bulkOrder, fund);
```

#### In OrderManager (for bulk order creation):
```java
BulkOrder bulk = // ... create bulk order
List<String> orderIDs = // ... get matched order IDs
listener.onBulkOrderCreated(bulk, fund, orderIDs);
```

#### In OrderManager (for bulk order status changes):
```java
listener.onBulkOrderStatusChanged(bulkOrder, fund, matchedOrderIDs);
```

### Step 4: Query Read Models
```java
// Find all orders for an account
List<OrderView> orders = store.findOrdersByAccount("ACCT00001");

// Find orders for a fund
List<OrderView> fundOrders = store.findOrdersByFund("FND001");

// Find a specific order
Optional<OrderView> order = store.findOrderView("ORD123");

// Find bulk orders for a fund
List<BulkOrderView> bulkOrders = store.findBulkOrdersByFund("FND001");
```

## MongoDB Setup (for Production)

### Install MongoDB locally (macOS):
```bash
brew install mongodb-community
brew services start mongodb-community
```

### Verify MongoDB is running:
```bash
mongosh  # Connect to MongoDB shell
```

### Monitor collections:
```javascript
// In mongosh:
use oms;
db.order_views.findOne();
db.bulk_order_views.findOne();
db.order_views.countDocuments();
```

## Switching Between Implementations

The same `ProjectionStore` interface is used by both implementations. Switch by:

```java
// Option 1: Dependency injection pattern
public class OrderManager {
    private final ProjectionStore projectionStore;
    
    public OrderManager(ProjectionStore store) {
        this.projectionStore = store;
    }
}

// Create with InMemory for testing
OrderManager manager1 = new OrderManager(new InMemoryProjectionStore());

// Create with MongoDB for production
OrderManager manager2 = new OrderManager(
    new MongoDbProjectionStore("mongodb://localhost:27017", "oms")
);
```

## Event Flow Example

When an order is placed:
```
1. User sends POST /orders/plan with order data
2. OrderRestServer.planOrdersHandler creates Order
3. Calls listener.onOrderPlanned(order, fund, account)
4. DefaultOrderProjectionListener calls projectionStore.projectOrder()
5. ProjectionStore (MongoDB or InMemory) persists OrderView
6. REST client can now query GET /view/orders to see the new order
```

When order is confirmed:
```
1. Order moves through state machine (PLANNED → CONFIRMED)
2. OrderStateMachine calls listener.onOrderStatusChanged()
3. Listener updates existing OrderView in ProjectionStore
4. View now shows orderStatus="CONFIRMED"
```

## Testing Strategy

### 1. Unit Tests
- Test ProjectionStore implementations independently
- Mock OrderProjectionListener
- Verify document creation/updates in MongoDB

### 2. Integration Tests
- Use InMemoryProjectionStore for fast testing
- Process complete order workflows
- Verify read model consistency with write model

### 3. End-to-End Tests
- Deploy with MongoDB
- Process orders via REST
- Query views via read-side REST endpoints
- Verify eventual consistency

## Important Notes

### Eventual Consistency
In a true CQRS system with event sourcing, read models are eventually consistent with write models. Currently, this implementation updates read models synchronously, ensuring strong consistency.

For true eventual consistency (with resilience to failures):
- Implement an outbox table in PostgreSQL
- Create a separate projector service that polls outbox
- Decouple write-side from read-side projection via message queue

### Data Durability
- `InMemoryProjectionStore`: Data is lost on restart (suitable for demo/POC)
- `MongoDbProjectionStore`: Data persists across restarts (suitable for production)

### Scaling
- `InMemoryProjectionStore`: Single-node only, memory-limited
- `MongoDbProjectionStore`: Can distribute across MongoDB replica sets, use MongoDB sharding for scale

## Next Steps

1. **Wire listener into OrderRestServer and OrderManager**
   - Inject ProjectionStore/Listener into these classes
   - Call listener methods at appropriate order events

2. **Create read-side REST endpoints**
   - GET /view/orders (all orders)
   - GET /view/orders/account/{accountID}
   - GET /view/orders/fund/{fundID}
   - GET /view/bulk-orders
   - GET /view/bulk-orders/fund/{fundID}

3. **Implement Server-Sent Events (SSE)**
   - Allow clients to subscribe to order updates in real-time
   - Push new/updated views to connected clients

4. **Add web UI dashboard**
   - React components to display OrderView and BulkOrderView
   - Real-time updates via SSE
   - Query read-side endpoints instead of write-side
