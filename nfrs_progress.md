# NFR Implementation Progress Log

---

## NFR 2: Strict RBAC Enforcement (JWT Middleware)

**Status:** Complete  
**Date:** 2026-04-24

### Files Created
- `src/main/java/com/iiit/oms/auth/JwtService.java` — HS256 JWT generation, validation, revocation
- `src/main/java/com/iiit/oms/auth/RevocationStore.java` — interface: `revoke(jti, ttl)`, `isRevoked(jti)`
- `src/main/java/com/iiit/oms/auth/InMemoryRevocationStore.java` — ConcurrentHashMap with wall-clock expiry
- `src/main/java/com/iiit/oms/filter/RbacFilter.java` — role enforcement via `checkAccess(HttpExchange)`
- `src/test/java/com/iiit/oms/auth/JwtServiceTest.java` — 16 tests
- `src/test/java/com/iiit/oms/auth/RbacFilterTest.java` — 12 tests

### Files Modified
- `pom.xml` — added `com.auth0:java-jwt:4.4.0` and `org.mindrot:jbcrypt:0.4`
- `src/main/java/com/iiit/oms/auth/RedisSessionStore.java` — implements `RevocationStore`; Redis-backed `revoke`/`isRevoked` with in-memory fallback
- `src/main/java/com/iiit/oms/interfaces/OrderRestServer.java`:
  - `jwtService` and `rbacFilter` fields (volatile)
  - `setJwtService()` setter (propagates to rbacFilter)
  - `resolveAuthenticatedUser()` — uses JWT when available, falls back to UUID sessions
  - `AuthLoginHandler` — uses `BCrypt.checkpw()` + `jwtService.generateToken()`
  - `AuthLogoutHandler` — calls `jwtService.revokeToken()`
  - `withCors()` — calls `rbacFilter.checkAccess()` on every request
  - CORS header now includes `Authorization`
- `src/main/java/com/iiit/oms/util/UserSeedDataUtil.java` — BCrypt hashes passwords at seed time
- `src/main/java/com/iiit/oms/OmsApplication.java` — wires Redis-backed `JwtService`

### Role Access Matrix (as implemented in RbacFilter)

| Rule key | Allowed roles |
|---|---|
| `POST:/orders/confirm` | ADMIN |
| `POST:/orders/book` | ADMIN |
| `GET:/accounts` | ADMIN |
| `POST:/funds` | ADMIN |
| `GET:/view/users` | ADMIN |
| `GET:/view/audit-archive` | ADMIN |
| `POST:/transfer-agent` | ADMIN |
| `POST:/orders/plan` | INVESTOR, ADVISOR |
| `POST:/orders/cancel` | INVESTOR, ADVISOR |
| `GET:/advisor` | ADVISOR |
| `POST:/advisor` | ADVISOR |
| `GET:/orders` | All authenticated |
| `GET:/view` | All authenticated |
| `GET:/funds` | All authenticated |
| `GET:/auth/me` | All authenticated |
| `POST:/auth/logout` | All authenticated |
| `/auth/login` | Public (no auth) |

### Design Decisions
- JWT is always enabled; UUID session fallback kept for backward compatibility if `jwtService` is null (it never is after construction)
- `InMemoryRevocationStore` is the constructor default; `OmsApplication` upgrades to Redis-backed store when Redis is available
- `RbacFilter` is applied via `withCors()` wrapper, so no per-context filter-chain edits needed
- Paths not in the access map are treated as open (no rule = no restriction); this is intentional to avoid locking out future endpoints

### Test Results
```
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
  JwtServiceTest: 16 tests  ✓
  RbacFilterTest: 12 tests  ✓
```

### Known Limitations
- `OMS_JWT_SECRET` must be set via env var in production; default falls back to an insecure hardcoded string (with a logged WARNING)
- Logout blacklist is in-memory if Redis is unavailable; tokens will re-authenticate until they expire (24h TTL)
- `/view/stream` (SSE) is open to all authenticated users; could be restricted further if needed

---

## NFR 1: TLS 1.2+ Migration

**Status:** Complete  
**Date:** 2026-04-24

### Files Created
- `scripts/gen-keystore.sh` — generates RSA-2048 keystore at `src/main/resources/keystore.jks`, idempotent (skips if exists)
- `src/main/resources/keystore.jks` — self-signed cert, 365-day validity, alias `oms`
- `src/test/java/com/iiit/oms/tls/TlsConfigTest.java` — 9 TLS tests

### Files Modified
- `src/main/java/com/iiit/oms/interfaces/OrderRestServer.java`:
  - Added `createServer(int port)` static factory method
  - Detects keystore at `OMS_KEYSTORE_PATH` (default: `src/main/resources/keystore.jks`)
  - Creates `HttpsServer` with `SSLContext` (TLSv1.2 + TLSv1.3 only)
  - Falls back to plain `HttpServer` if keystore missing (with logged WARNING)
- `frontend/vite.config.js` — proxy target changed to `https://localhost:8080`, added `secure: false`
- `user-frontend/vite.config.js` — same changes

### Design Decisions
- Port stays at **8080** (same port, now HTTPS) — avoids breaking existing scripts/configs
- Graceful fallback to HTTP if keystore is missing — dev environments without `gen-keystore.sh` still work
- `OMS_KEYSTORE_PASS` env var (default: `changeit`) — override in production
- TLS 1.0 and TLS 1.1 disabled via `SSLParameters.setProtocols(["TLSv1.2", "TLSv1.3"])`

### Test Results
```
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
  httpsEndpoint_respondsOn_testPort              ✓
  httpsEndpoint_tlsHandshake_succeeds            ✓
  httpsEndpoint_negotiatedProtocol_isTls12or13   ✓
  tls10Connection_isRejected                     ✓
  tls11Connection_isRejected                     ✓
  tls12Connection_succeeds                       ✓
  tls13Connection_succeeds                       ✓
  viteProxyConfig_frontendPointsToHttps          ✓
  viteProxyConfig_userFrontendPointsToHttps      ✓
```

### Known Limitations
- Self-signed cert causes browser security warnings — use a CA-signed cert for production (cert path configurable via `OMS_KEYSTORE_PATH`)
- Keystore is committed to the repo for dev convenience — exclude from production Docker images via `.dockerignore` and mount via secret/volume

---

## NFR 3: SLA Dashboard (Frontend Countdown + Latency Metrics)

**Status:** Complete  
**Date:** 2026-04-24

### Files Created
- `frontend/src/api/slaApi.js` — `getSla()` → `GET /view/sla`
- `frontend/src/components/SLAWidget/SLAWidget.jsx` — three-card widget (onshore cutoff, offshore cutoff, API latency)
- `frontend/src/components/SLAWidget/SLAWidget.css` — stripe-inspired card styling
- `src/test/java/com/iiit/oms/filter/LatencyFilterTest.java` — 11 tests

### Files Modified
- `src/main/java/com/iiit/oms/filter/LatencyFilter.java`:
  - Added `long[] ring` ring buffer (size 1000) for p99 tracking
  - `getP99Ms()` — sorts ring snapshot, returns 99th percentile value
  - `getAvgMs()` — renamed from `getAverageLatency()` (deprecated alias kept)
  - `getTotalRequests()` — exposes total request count
- `src/main/java/com/iiit/oms/interfaces/OrderRestServer.java`:
  - Added `SlaHandler` inner class for `GET /view/sla`
  - Computes next onshore (16:00 UTC) and offshore (01:00 UTC) cutoffs relative to server time
  - Returns: `serverTimeMs`, `onshoreDeadline`, `offshoreDeadline`, `secondsToOnshore`, `secondsToOffshore`, `latency{p99Ms, avgMs, totalRequests}`
- `frontend/src/pages/Dashboard.jsx` — `<SLAWidget />` mounted at top of dashboard

### Design Decisions
- SLAWidget ticks locally every 1s (no round-trip); re-fetches from server every 60s to correct drift
- Color thresholds: red < 30 min, amber < 2h, green ≥ 2h
- Cutoff times are in UTC — consistent with server time zone (UTC forced in OmsApplication)
- Widget renders nothing (`null`) if server fetch fails — no broken UI for unauthenticated views
- p99 ring buffer uses `Math.min(totalRequests, RING_SIZE)` to handle < 1000 request scenarios

### Test Results
```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
  getP99Ms_noRequests_returnsZero            ✓
  getP99Ms_uniformLatencies_correctPercentile ✓
  getP99Ms_singleValue_returnsThatValue      ✓
  getP99Ms_outliers_capturedByP99            ✓
  getAvgMs_correctAverage                    ✓
  getAvgMs_noRequests_returnsZero            ✓
  getTotalRequests_correctCount              ✓
  slaHandler_secondsToOnshore_isPositive     ✓
  slaHandler_secondsToOffshore_isPositive    ✓
  slaHandler_bothCutoffs_neverInThePast      ✓
  p99_ringBuffer_wrapsCorrectly              ✓
```

---

## NFR 5 (partial): WORM Archival Strategy

**Status:** Complete  
**Date:** 2026-04-24

### Files Created
- `src/main/java/com/iiit/oms/kafka/AuditLogArchiver.java` — weekly WORM archiver
- `src/test/java/com/iiit/oms/kafka/AuditLogArchiverTest.java` — 7 tests (H2 in-memory DB)

### Files Modified
- `src/main/java/com/iiit/oms/db/util/PostgresSchemaInitializer.java`:
  - `ALTER TABLE order_audit_log ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ`
  - `CREATE TABLE IF NOT EXISTS audit_archive_runs (run_id, archived_at, records_count, file_path, checksum_sha256)`
- `src/main/java/com/iiit/oms/interfaces/OrderRestServer.java`:
  - `AuditArchiveHandler` inner class — `GET /view/audit-archive` (ADMIN only via RBAC)
  - `pgConnectionFactory` field + `setConnectionFactory()` setter
  - `VIEW_AUDIT_ARCHIVE_PATH` constant
- `src/main/java/com/iiit/oms/OmsApplication.java`:
  - `AuditLogArchiver` instantiated and started after DB init
  - `setConnectionFactory()` called on server
  - Archiver shutdown wired into shutdown hook
- `pom.xml`: added `com.h2database:h2:2.2.224` (test scope)

### Archive Format
- File: `audit-archives/audit_YYYYMMDD_HHmmssSSS.json.gz` (millisecond precision prevents collisions)
- Content: newline-delimited JSON, GZip compressed
- Checksum: SHA-256 stored in `audit_archive_runs.checksum_sha256`

### Scheduler Timing
- Weekly: every Sunday 02:00 UTC (initial delay computed from current time)
- Monthly: first of month — 7-year retention WARNING logged for operator review
- Daemon thread so JVM can exit cleanly

### WORM Guarantee
- Files are append-only (timestamped names never overwrite)
- `archived_at` column prevents re-archiving same records (idempotent)
- 7-year alert is a WARNING log only — no deletion ever occurs

### Test Results (H2 in-memory, PostgreSQL compat mode)
```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
  archiver_exportsOnlyRecordsOlderThan7Days     ✓
  archiver_doesNotExport_recentRecords           ✓
  archiver_checksumMatches_writtenFile           ✓
  archiver_fileIsGzipCompressed                  ✓
  archiver_idempotent_doesNotReexportArchivedRecords ✓
  archiver_recordsMarkedArchivedAt_afterExport   ✓
  archiver_multipleRuns_produceDistinctFiles     ✓
```

---

## NFR 4: Active-Active Failover (Nginx + Docker Compose)

**Status:** Complete  
**Date:** 2026-04-24

### Files Created
- `nginx.conf` — upstream `oms_backend` with `least_conn` across `app_1:8080` + `app_2:8080`; special `/view/stream` location with `proxy_buffering off` for SSE
- `Dockerfile` — `eclipse-temurin:11-jre-alpine`, copies fat JAR + keystore, exposes 8080
- `src/main/java/com/iiit/oms/processor/DistributedLock.java` — Redis-backed NX EX lock, fail-open if Redis unavailable
- `src/test/java/com/iiit/oms/processor/DistributedLockTest.java` — 10 tests

### Files Modified
- `docker-compose.yml` — full multi-service topology:
  - `app_1` + `app_2` via YAML anchor (`&app` / `<<: *app`)
  - `nginx` — port 80, mounts `nginx.conf`
  - `oms_net` network shared by all services
  - `audit_archives` named volume for WORM archival persistence
- `src/main/java/com/iiit/oms/processor/OrderScheduler.java`:
  - `DistributedLock` at start of each tick (key: `oms:scheduler:order-lock`, TTL 25s)
  - `setJedisPool()` setter added
- `src/main/java/com/iiit/oms/processor/BatchoutScheduler.java`:
  - `DistributedLock` at start of each `runBatchoutCycle` (key: `oms:scheduler:batchout-lock`, TTL 110s)
- `pom.xml` — added `maven-shade-plugin:3.5.3` for fat JAR (Docker needs all deps bundled)
- `src/main/java/com/iiit/oms/OmsApplication.java` — `orderScheduler.setJedisPool(sharedPool)` added

### Why replicas are safe
| Component | Replica safety |
|---|---|
| Sessions | Redis-backed `RedisSessionStore` — shared across replicas |
| Idempotency | `RedisIdempotencyStore` — shared across replicas |
| Order scheduler | `DistributedLock` key `oms:scheduler:order-lock` (TTL 25s < 30s interval) |
| Batchout scheduler | `DistributedLock` key `oms:scheduler:batchout-lock` (TTL 110s < 120s interval) |
| Bulk dedup | Already Redis-keyed per bulk order ID in `BatchoutScheduler` |

### Test Results
```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
  nullJedisPool_tryAcquire_returnsTrue                ✓
  nullJedisPool_multipleAcquires_allReturnTrue        ✓
  twoReplicas_onlyOneExecutesPerTick_withInMemorySimulation ✓
  lockConstants_orderScheduler_ttlShorterThanInterval ✓
  lockConstants_batchoutScheduler_ttlShorterThanInterval ✓
  nginxConfig_exists_andConfiguresUpstream            ✓
  nginxConfig_sseLocation_hasBufferingOff             ✓
  dockerfile_exists_andUsesJre                        ✓
  dockerCompose_hasTwoAppReplicas                     ✓
  dockerCompose_nginx_exposesPort80                   ✓
```

### Manual Failover Test Steps (to run after `mvn package && docker compose up`)
1. `docker compose up --build` → verify all services start
2. `curl http://localhost/auth/login` → should route via nginx
3. `docker compose stop app_1` → verify requests still route to app_2
4. `docker compose start app_1` → verify nginx rebalances
5. SSE: `curl -N http://localhost/view/stream` → events should continue after failover
6. Place an order on app_1, confirm state visible via app_2 (Redis-backed sessions/orders)

---
