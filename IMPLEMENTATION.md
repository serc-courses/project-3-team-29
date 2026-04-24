# MF-OMS Implementation Reference — Team 29

## Architecture Overview

| Layer | Technology | Purpose |
|---|---|---|
| Backend | Java 11 (`com.sun.net.httpserver` HTTPS) | REST API, order lifecycle, SSE |
| Admin Frontend | React 18 + Vite (port 5173) | Operations: orders, bulk, funds, accounts |
| User Frontend | React 18 + Vite (port 5174) | Investors/Advisors: portfolio, funds, orders |
| Primary DB | PostgreSQL 16 | Orders, accounts, funds, audit log |
| Read Model | MongoDB 7 (CQRS) | `order_views`, `bulk_order_views` — optimized read projection |
| Cache/Sessions | Redis 7 | JWT revocation, idempotency keys, bulk dedup, distributed locks |
| Event Streaming | Apache Kafka 3.7 | Async order processing, SSE bridge, audit log |
| Ingress | Nginx 1.25 (port 80) | Active-active load balancer (`app_1`, `app_2`) |

### Infrastructure Topology

```
User/Admin Browser
       │
    Nginx :80
    ├── app_1 :8080 (HTTPS)
    └── app_2 :8080 (HTTPS)
           │
    ┌──────┼──────────┐
  Postgres  Redis    Kafka
           │
         MongoDB
```

### Kafka Topics

| Topic | Consumer | Purpose |
|---|---|---|
| `oms.orders.received` | `KafkaOrderProcessingConsumer` | Async order intake |
| `oms.orders.state` | `KafkaNotificationConsumer` | SSE bridge: order state changes |
| `oms.orders.booked` | `KafkaNotificationConsumer` | SSE bridge: booking confirmation |
| `oms.nav.updated` | `KafkaNotificationConsumer` | SSE bridge: NAV changes → portfolio refresh |
| `oms.audit.log` | `KafkaAuditLogConsumer` | Immutable audit trail |

### SSE Event Types (frontend listens)

| Event | Trigger |
|---|---|
| `order-updated` | Order state change via Kafka |
| `bulk-order-updated` | Bulk order created/transmitted |
| `nav-updated` | Admin updates fund NAV |
| `replay-completed` | Read model replay finished |

### Replica Safety

| Component | Mechanism |
|---|---|
| Sessions | Redis `RedisSessionStore` (shared) |
| Idempotency | Redis `RedisIdempotencyStore` (shared) |
| Order Scheduler | `DistributedLock` key `oms:scheduler:order-lock` (TTL 25s) |
| Batchout Scheduler | `DistributedLock` key `oms:scheduler:batchout-lock` (TTL 110s) |
| Kafka SSE Consumer | Unique group ID per JVM (`oms-sse-<uuid8>`) — both replicas consume all events |

---

## How to Run

### Prerequisites

- Docker Desktop (runs Postgres, Redis, Kafka, Mongo, Nginx)
- Java 11+ (JDK — for backend build)
- Node.js 18+ + npm (for frontend builds)
- Maven 3.8+ (for Java build)

### Quick Start (Linux/Mac)

```bash
# 1. Generate TLS keystore (run once)
bash scripts/gen-keystore.sh

# 2. Build backend
mvn -q -DskipTests package

# 3. Build frontends
cd frontend && npm install && npm run build && cd ..
cd user-frontend && npm install && npm run build && cd ..

# 4. Start all services
docker compose up -d --build
```

Access:
- Admin UI: http://localhost/app/ (or http://localhost:5173 in dev)
- User UI: http://localhost/user/ (or http://localhost:5174 in dev)
- API: https://localhost:8080 (self-signed cert; browser may warn)

### Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `OMS_JWT_SECRET` | `changeit` (insecure) | JWT HMAC signing key — **set in production** |
| `OMS_KEYSTORE_PATH` | `src/main/resources/keystore.jks` | TLS keystore path |
| `OMS_KEYSTORE_PASS` | `changeit` | TLS keystore password |

### Useful Make Targets

```bash
make start        # docker compose up -d
make stop         # docker compose down
make logs         # tail all service logs
make rebuild      # mvn package + docker compose up --build
```

### Dev Mode (without Docker)

Start Postgres, Redis, Kafka, Mongo via Docker, then:
```bash
mvn spring-boot:run   # or java -jar target/app.jar
cd frontend && npm run dev        # admin on :5173
cd user-frontend && npm run dev   # user on :5174
```

---

## Non-Functional Requirements Status

### NFR 1 — TLS 1.2+
**Status: Complete**
- Backend uses `HttpsServer` with RSA-2048 self-signed cert
- TLS 1.0/1.1 disabled; TLS 1.2 and 1.3 only
- Graceful HTTP fallback if keystore missing
- Vite proxies updated to `https://localhost:8080`

### NFR 2 — RBAC / JWT Auth
**Status: Complete**
- HS256 JWT issued on login; 24h TTL
- Revocation via Redis (with in-memory fallback)
- `RbacFilter` enforced on every request via `withCors()` wrapper
- Role access: ADMIN, INVESTOR, ADVISOR with fine-grained path rules
- BCrypt password hashing

### NFR 3 — SLA Dashboard + Latency Metrics
**Status: Complete**
- `LatencyFilter` ring-buffer (size 1000) — p99 and avg latency
- `GET /view/sla` returns onshore (16:00 UTC) / offshore (01:00 UTC) countdowns
- `SLAWidget` on admin dashboard with color-coded alerts (red < 30 min, amber < 2h)

### NFR 4 — Active-Active Failover
**Status: Complete**
- Nginx `least_conn` upstream across `app_1` + `app_2`
- SSE path uses `proxy_buffering off`
- Distributed lock prevents double-processing by schedulers
- Unique Kafka consumer group per JVM for SSE bridge

### NFR 5 — WORM Audit Archival
**Status: Complete**
- Weekly archiver exports `order_audit_log` rows older than 7 days to gzip JSON files
- SHA-256 checksum stored in `audit_archive_runs` table
- `archived_at` column prevents re-export (idempotent)
- `GET /view/audit-archive` (ADMIN only) lists archive runs

### NFR 6 — AES-256 PII Encryption
**Status: Complete**
- `national_identity` field encrypted at rest using AES-256

---

## Key Design Decisions

### CQRS Read Model
Orders are written to PostgreSQL (write side) and projected into MongoDB (`order_views` collection) for fast UI reads. The projection is updated synchronously on every state change and can be replayed via `POST /view/replay`.

### Order Lifecycle
`PLANNED → VALIDATED → ENRICHED → PLACED → BULKED → TRANSMITTED → CONFIRMED → CONTRACTED → BOOKED` (or `ERRORED` / `CANCELLED`)

### Idempotency
Each order submission requires an `X-Idempotency-Key` header (UUID). Duplicate keys within 1 hour return the original result without re-processing.

### Bulk Orders
The `BatchoutScheduler` runs every 120s, groups PLACED orders by fund and side into bulk orders, acquires a Redis distributed lock, and transmits to the transfer agent simulation endpoint.

### Portfolio P/L
Calculated server-side from BOOKED orders. `currentNav` is fetched live from the fund repository; `buyNav` is stored at booking time. P/L = `(currentNav - buyNav) × allocatedShares`.

### Pre-order Cash Validation
BUY orders are blocked at the frontend if `amount > availableCash` (from `/view/portfolio`). SELL orders are blocked if the fund holding's current value is insufficient.

### Session Restore
`GET /auth/me` reloads the full `User` object from the repository after JWT validation, ensuring `displayName`, `accountID`, and `advisorID` are always present in the session.
