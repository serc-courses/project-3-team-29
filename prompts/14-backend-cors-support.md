# Prompt 14: Backend CORS Support

## Context
The React frontend runs on `http://localhost:5173` (Vite dev server), while the Java backend runs on `http://localhost:8080`. Browsers block cross-origin requests by default. While Vite's proxy handles this during development, CORS headers are needed for:
1. Production deployment where frontend and backend may be on different origins
2. Direct browser requests (e.g., SSE EventSource doesn't go through the Vite proxy in all scenarios)

## Task
Modify the backend Java file `src/main/java/com/iiit/oms/interfaces/OrderRestServer.java` to add CORS headers to all HTTP responses.

## Implementation

### Approach: Add a CORS filter to all handlers

In the `OrderRestServer` constructor, wrap each handler with a CORS-aware handler. Create a private utility method:

```java
/**
 * Wraps an HttpHandler with CORS headers for cross-origin requests.
 */
private HttpHandler withCors(HttpHandler handler) {
    return exchange -> {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        handler.handle(exchange);
    };
}
```

Then wrap all handler registrations:
```java
this.httpServer.createContext(PLAN_ORDERS_PATH, withCors(new PlanOrdersHandler()));
this.httpServer.createContext(LIST_ORDERS_PATH, withCors(new ListOrdersHandler()));
this.httpServer.createContext(ORDER_STATUS_PATH, withCors(new OrderStatusHandler()));
this.httpServer.createContext(CONFIRM_ORDERS_PATH, withCors(new ConfirmOrdersHandler()));
this.httpServer.createContext(BOOK_ORDERS_PATH, withCors(new BookOrdersHandler()));
this.httpServer.createContext(VIEW_ORDERS_PATH, withCors(new ViewOrdersHandler()));
this.httpServer.createContext(VIEW_BULK_ORDERS_PATH, withCors(new ViewBulkOrdersHandler()));
this.httpServer.createContext(VIEW_DASHBOARD_PATH, withCors(new ViewDashboardHandler()));
this.httpServer.createContext(VIEW_AGGREGATES_ACCOUNTS_PATH, withCors(new ViewAggregateAccountsHandler()));
this.httpServer.createContext(VIEW_AGGREGATES_FUNDS_PATH, withCors(new ViewAggregateFundsHandler()));
this.httpServer.createContext(VIEW_REPLAY_PATH, withCors(new ViewReplayHandler()));
this.httpServer.createContext(VIEW_STREAM_PATH, withCors(new ViewStreamHandler()));
this.httpServer.createContext(VIEW_UI_PATH, withCors(new ViewUiHandler()));
```

### For the SSE endpoint specifically
The `ViewStreamHandler` already sets response headers for `text/event-stream`. The CORS `Access-Control-Allow-Origin` header should still be added before the SSE headers. The `withCors` wrapper handles this correctly since it adds headers before delegating to the inner handler.

## Important Notes
- Use `"*"` for `Access-Control-Allow-Origin` in development. For production, this should be configurable via an environment variable (e.g., `OMS_CORS_ORIGIN`).
- Handle `OPTIONS` preflight requests by returning `204 No Content` immediately
- Do NOT break existing functionality — all existing tests must still pass
- The `withCors` pattern keeps CORS logic in one place, not scattered across every handler
- After making this change, verify with:
  ```bash
  mvn test
  ```
  Then test CORS manually:
  ```bash
  curl -i -X OPTIONS http://localhost:8080/orders \
    -H "Origin: http://localhost:5173" \
    -H "Access-Control-Request-Method: GET"
  ```
  Expected: `204` response with `Access-Control-Allow-Origin: *` header.

## Production Enhancement (Optional)
For production, make the allowed origin configurable:
```java
private static final String CORS_ORIGIN_ENV_VAR = "OMS_CORS_ORIGIN";

private String getAllowedOrigin() {
    String origin = System.getenv(CORS_ORIGIN_ENV_VAR);
    return (origin == null || origin.isBlank()) ? "*" : origin;
}
```
Then use `getAllowedOrigin()` instead of `"*"` in the `withCors` method.
