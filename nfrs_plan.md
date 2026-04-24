# NFR Implementation Plan — OMS Project

**Stack:** Java 11 + `com.sun.net.httpserver` backend, React/Vite dual frontend, PostgreSQL, Redis, Kafka, Docker Compose.

---

## Implementation Order

| Priority | NFR | Effort | Dependencies |
|---|---|---|---|
| 1 | RBAC + JWT | Medium | None — self-contained |
| 2 | TLS 1.2+ | Low | Needs keystore |
| 3 | SLA Dashboard | Low | LatencyFilter already exists |
| 4 | WORM Archival | Medium | Schema migration |
| 5 | Active-Active Failover | Medium | TLS + JWT done first |

---

## NFR 1: Strict RBAC Enforcement (JWT Middleware)

**Current state:** UUID session tokens in Redis, plain-text passwords, no per-endpoint role checks.

### Step 1 — Add dependencies to `pom.xml`

- `com.auth0:java-jwt:4.4.0` — JWT signing/verification
- `org.mindrot:jbcrypt:0.4` — BCrypt password hashing

### Step 2 — `JwtService.java` *(new — `com.iiit.oms.auth`)*

```
generateToken(User user) → signed JWT
  payload: sub=userID, role=role, jti=UUID, exp=now+24h
  algorithm: HS256, secret from env OMS_JWT_SECRET

validateAndExtract(String token) → UserSession or null
  verifies signature
  verifies expiry
  returns UserSession with userId + role
```

### Step 3 — Replace UUID sessions with JWT in `OrderRestServer.java`

- `AuthLoginHandler`: call `JwtService.generateToken()` instead of `UUID.randomUUID()`; stop writing to `SessionStore`
- `resolveAuthenticatedUser()`: call `JwtService.validateAndExtract()` instead of `sessionStore.get()`
- `AuthLogoutHandler`: write `oms:revoked:{jti}` key to Redis with TTL = remaining token lifetime (JWT blacklist)

### Step 4 — Role access matrix

| Endpoint prefix | Allowed roles |
|---|---|
| `POST /orders/plan`, `POST /orders/cancel` | INVESTOR, ADVISOR |
| `POST /orders/confirm`, `POST /orders/book` | ADMIN |
| `GET /orders`, `GET /view/*` | Any authenticated |
| `GET /funds`, `GET /accounts`, `POST /funds` | ADMIN |
| `GET/POST /advisor/*` | ADVISOR |
| `POST /auth/login` | Unauthenticated |

### Step 5 — `RbacFilter.java` *(new — `com.iiit.oms.filter`)*

```
handle(HttpExchange ex):
  1. Extract Bearer token from Authorization header
  2. Call resolveAuthenticatedUser() → UserSession
  3. Lookup required roles for (method, path prefix)
  4. If user role not in allowed set → respond 403 JSON
  5. Otherwise delegate to next handler
```

Wrap every handler registration in `OrderRestServer` with `RbacFilter`.

### Step 6 — Password hashing

- `AuthLoginHandler`: use `BCrypt.checkpw()` instead of `.equals()`
- `InMemoryUserRepository`: replace plain-text seed passwords with `BCrypt.hashpw()` values

### Files to change

- `pom.xml`
- `src/main/java/com/iiit/oms/interfaces/OrderRestServer.java`
- `src/main/java/com/iiit/oms/auth/JwtService.java` *(new)*
- `src/main/java/com/iiit/oms/auth/RedisSessionStore.java` (add JWT revocation list)
- `src/main/java/com/iiit/oms/filter/RbacFilter.java` *(new)*
- `src/main/java/com/iiit/oms/repository/inmemory/InMemoryUserRepository.java` (hashed seeds)

---

## NFR 2: TLS 1.2+ Migration

**Current state:** `HttpServer` on port 8080 (plaintext). Vite dev servers proxy to `http://localhost:8080`.

### Step 1 — Keystore generation script *(new — `scripts/gen-keystore.sh`)*

```sh
keytool -genkeypair -alias oms -keyalg RSA -keysize 2048 \
  -validity 365 -keystore src/main/resources/keystore.jks \
  -storepass changeit -keypass changeit \
  -dname "CN=localhost, OU=OMS, O=IIIT, L=Hyd, ST=TS, C=IN"
```

### Step 2 — Switch `OrderRestServer` to `HttpsServer`

- Replace `HttpServer.create(new InetSocketAddress(8080), 0)` with `HttpsServer.create(new InetSocketAddress(8443), 0)`
- Load `SSLContext` from keystore; restrict protocols to `["TLSv1.2", "TLSv1.3"]`; disable SSLv3/TLS1.0/TLS1.1
- Attach `HttpsConfigurator` with the resulting `SSLParameters`
- Read keystore path/password from env vars `OMS_KEYSTORE_PATH`, `OMS_KEYSTORE_PASS` (fall back to dev defaults)

### Step 3 — Vite proxy updates

Both `frontend/vite.config.js` and `user-frontend/vite.config.js`:
- Change proxy target to `https://localhost:8443`
- Add `secure: false` to accept self-signed cert in dev

### Step 4 — Docker Compose

- Expose port `8443` for the backend service
- Mount `keystore.jks` via a volume

### Files to change

- `src/main/java/com/iiit/oms/interfaces/OrderRestServer.java`
- `frontend/vite.config.js`
- `user-frontend/vite.config.js`
- `docker-compose.yml`
- `scripts/gen-keystore.sh` *(new)*

---

## NFR 3: SLA Dashboard (Frontend Countdowns)

**Current state:** `LatencyFilter` measures latency but data is not exposed. No cutoff countdowns in UI.

### Step 1 — Extend `LatencyFilter.java`

Add a p99 ring buffer:
- Keep a `long[]` ring buffer of last 1000 request latencies
- `getP99()`: copy buffer, sort, return element at 99th percentile index
- Expose `getAvg()`, `getTotalRequests()`, `getP99()` as public methods

### Step 2 — `SlaHandler` in `OrderRestServer.java`

New route `GET /view/sla` (authenticated, any role) returning:

```json
{
  "serverTimeMs": 1714000000000,
  "onshoreDeadline": "2026-04-24T16:00:00",
  "offshoreDeadline": "2026-04-25T01:00:00",
  "secondsToOnshore": 7234,
  "secondsToOffshore": 39234,
  "latency": {
    "p99Ms": 42,
    "avgMs": 18,
    "totalRequests": 4823
  }
}
```

Logic: compute next occurrence of 16:00 (onshore) and 01:00 (offshore) relative to server's current time.

### Step 3 — `slaApi.js` *(new — `frontend/src/api/`)*

```js
export const getSla = () => apiFetch('/view/sla')
```

### Step 4 — `SLAWidget.jsx` *(new — `frontend/src/components/`)*

- `useEffect` + `setInterval(1000)` to tick countdown locally after initial fetch
- Re-fetch from `/view/sla` every 60s to correct clock drift
- Two countdown cards: "Onshore Cutoff 16:00" and "Offshore Cutoff 01:00"
- Color coding: green > 2h remaining, amber < 2h, red < 30 min
- Third card: "API p99 Latency" with trend indicator

### Step 5 — Mount widget in admin dashboard

Add `<SLAWidget />` to `frontend/src/App.jsx` (dashboard route header).

### Files to change

- `src/main/java/com/iiit/oms/interfaces/OrderRestServer.java`
- `src/main/java/com/iiit/oms/filter/LatencyFilter.java`
- `frontend/src/api/slaApi.js` *(new)*
- `frontend/src/components/SLAWidget.jsx` *(new)*
- `frontend/src/App.jsx`

---

## NFR 4: WORM Archival Strategy (Kafka AuditLog)

**Current state:** `KafkaAuditLogConsumer` writes audit events to `order_audit_log` table. No archival or immutability policy.

### Step 1 — Schema migration in `PostgresSchemaInitializer.java`

```sql
CREATE TABLE IF NOT EXISTS audit_archive_runs (
    run_id          BIGSERIAL PRIMARY KEY,
    archived_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    records_count   INT NOT NULL,
    file_path       TEXT NOT NULL,
    checksum_sha256 TEXT NOT NULL
);

ALTER TABLE order_audit_log
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ;
```

### Step 2 — `AuditLogArchiver.java` *(new — `com.iiit.oms.kafka`)*

Scheduled weekly (every Sunday 02:00 via `ScheduledExecutorService`, initial delay computed from now to next Sunday 02:00):

```
1. SELECT * FROM order_audit_log
   WHERE occurred_at < NOW() - INTERVAL '7 days'
   AND archived_at IS NULL
   ORDER BY occurred_at

2. Serialize records to newline-delimited JSON (Jackson ObjectMapper)

3. GZip compress the output

4. Write to OMS_ARCHIVE_DIR (env var, default ./audit-archives/)
   Filename: audit_YYYYMMDD_HHMMSS.json.gz
   Files are never overwritten — each run produces a new timestamped file

5. Compute SHA-256 checksum of the written file

6. INSERT INTO audit_archive_runs (records_count, file_path, checksum_sha256)

7. UPDATE order_audit_log SET archived_at = NOW()
   WHERE id IN (<archived ids>)
```

**WORM guarantee:** Archive files are append-only by naming convention (timestamped, never overwritten). The `checksum_sha256` column in `audit_archive_runs` allows integrity verification of any archive file at any time.

**7-year retention enforcement:** A second monthly task checks `audit_archive_runs.archived_at < NOW() - INTERVAL '7 years'` and logs a WARNING — it does NOT delete (WORM prohibits deletion; the log flags records for operator review and compliance sign-off).

### Step 3 — `AuditArchiveHandler` in `OrderRestServer.java`

New route `GET /view/audit-archive` (ADMIN only via RBAC) returning:

```json
[
  {
    "runId": 1,
    "archivedAt": "2026-04-20T02:00:00Z",
    "recordsCount": 4200,
    "filePath": "audit_20260420_020000.json.gz",
    "checksumSha256": "abc123..."
  }
]
```

### Step 4 — Wire up in `OmsApplication.java`

Instantiate `AuditLogArchiver` and start its scheduler after DB is ready.

### Files to change

- `src/main/java/com/iiit/oms/db/util/PostgresSchemaInitializer.java`
- `src/main/java/com/iiit/oms/kafka/AuditLogArchiver.java` *(new)*
- `src/main/java/com/iiit/oms/interfaces/OrderRestServer.java`
- `src/main/java/com/iiit/oms/OmsApplication.java`

---

## NFR 5: Active-Active Failover (Nginx + Docker Compose Replicas)

**Current state:** Single backend instance, no load balancer, no nginx.

### Step 1 — Make backend scheduler replica-safe

`OrderScheduler.java` and `BatchoutScheduler.java` each tick independently per replica, causing double-firing. Fix with a Redis distributed lock at the top of each tick:

```java
// At start of each scheduler tick:
String lock = jedis.set("oms:scheduler:lock", instanceId, "NX", "EX", 25);
if (lock == null) return;  // another replica is handling this tick
// ... proceed with scheduler work
```

Use a unique `instanceId` per JVM (e.g., `UUID.randomUUID()` on startup). The 25s expiry is shorter than the 30s scheduler interval.

### Step 2 — `nginx.conf` *(new — project root)*

```nginx
upstream oms_backend {
    least_conn;
    server app_1:8080;
    server app_2:8080;
    keepalive 16;
}

server {
    listen 80;

    location / {
        proxy_pass         http://oms_backend;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_http_version 1.1;
        proxy_set_header   Connection "";
    }

    location /view/stream {
        proxy_pass         http://oms_backend;
        proxy_buffering    off;      # SSE requires no buffering
        proxy_read_timeout 3600s;
    }
}
```

### Step 3 — Update `docker-compose.yml`

```yaml
services:
  app_1: &app
    build: .
    environment:
      - OMS_REDIS_HOST=redis
      - OMS_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - OMS_DB_URL=jdbc:postgresql://postgres:5432/oms
    depends_on: [postgres, redis, kafka]
    networks: [oms_net]

  app_2:
    <<: *app

  nginx:
    image: nginx:1.25-alpine
    ports: ["80:80"]
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on: [app_1, app_2]
    networks: [oms_net]

networks:
  oms_net:
```

Remove direct port exposure from `app_1`/`app_2` — all traffic enters via nginx on port 80.

### Step 4 — `Dockerfile` *(new — project root)*

```dockerfile
FROM eclipse-temurin:11-jre-alpine
COPY target/oms-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

Build with `mvn package -DskipTests` before `docker compose up`.

### Why replicas are safe

- Sessions: stored in Redis (shared across replicas) — no stickiness required
- Idempotency: `RedisIdempotencyStore` uses Redis — dedup works across replicas
- Batchout dedup: already Redis-keyed per bulk order ID
- Scheduler: distributed lock added in Step 1

### Files to change

- `docker-compose.yml`
- `nginx.conf` *(new)*
- `Dockerfile` *(new)*
- `src/main/java/com/iiit/oms/processor/OrderScheduler.java`
- `src/main/java/com/iiit/oms/processor/BatchoutScheduler.java`
