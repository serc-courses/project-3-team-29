# How to Run — MF-OMS Project (Team 29)

This guide explains how to set up and run the entire Mutual Fund Order Management System from scratch on a Windows machine where nothing is installed. Read everything once before starting.

---

## What You Will Be Running

| Service | What it is | Port |
|---------|-----------|------|
| **Java Backend** | REST API handling all business logic | 8080 |
| **Admin Frontend** | React web app for operations team | 5173 |
| **User/Advisor Frontend** | React PWA for investors and advisors | 5174 |
| **PostgreSQL** | Primary database (orders, accounts, funds) | 5432 |
| **Redis** | Idempotency key store + session token store + bulk dedup | 6379 |
| **Kafka** | Event streaming (order lifecycle events, SSE bridge, audit) | 9092 |

PostgreSQL, Redis, and Kafka all run inside Docker containers — you do not install them manually.

**What Redis is used for (3 things):**
- **Inbound idempotency**: prevents duplicate order submissions (same `X-Idempotency-Key` header gets 409)
- **Session tokens**: user login tokens stored in Redis with 24h TTL; survive backend restarts
- **Outbound bulk dedup**: prevents a bulk order from being transmitted to the transfer agent twice (SETNX per bulk order ID)

**What Kafka is used for (3 consumers):**
- `KafkaOrderProcessingConsumer` — async processing of new orders from `oms.orders.received` topic
- `KafkaNotificationConsumer` — bridges `ORDER_STATE_CHANGED` events to Server-Sent Events (SSE) for real-time UI updates
- `KafkaAuditLogConsumer` — records all OMS events into the audit log table

---

## Step 0 — Install Prerequisites

You need four things installed before running anything. Do them in this order.

### 0A — Install Docker Desktop

Docker runs PostgreSQL, Redis, and Kafka in containers so you do not have to install them individually.

1. Go to: https://www.docker.com/products/docker-desktop/
2. Download **Docker Desktop for Windows**
3. Run the installer (it will ask to enable WSL2 — say Yes)
4. After install, restart your computer
5. Open Docker Desktop from the Start Menu and wait until the whale icon in the taskbar shows green / "Engine running"
6. Verify: open PowerShell and run:
   ```powershell
   docker ps
   ```
   Expected output: a table with column headers (even if empty). If you get an error, Docker is not running yet.

### 0B — Install Java 11 (or newer)

The backend requires Java 11+.

1. Go to: https://adoptium.net
2. Click **Latest LTS** — download the `.msi` Windows installer for Java 21 (or 11)
3. Run the installer, check the box "Add to PATH" during install
4. Verify:
   ```powershell
   java -version
   ```
   Expected: `openjdk version "21.x.x"` or `"11.x.x"`

### 0C — Install Maven

Maven builds and runs the Java backend.

1. Go to: https://maven.apache.org/download.cgi
2. Download the **Binary zip archive** (e.g., `apache-maven-3.9.x-bin.zip`)
3. Extract it to somewhere like `C:\maven`
4. Add `C:\maven\bin` to your Windows PATH:
   - Search "Environment Variables" in Start Menu
   - Edit System Variables → Path → New → `C:\maven\bin`
   - Click OK, restart PowerShell
5. Verify:
   ```powershell
   mvn -version
   ```
   Expected: `Apache Maven 3.9.x`

### 0D — Install Node.js (LTS)

Node.js runs the React frontends.

1. Go to: https://nodejs.org
2. Download the **LTS** version Windows installer
3. Run the installer (all defaults are fine)
4. Verify:
   ```powershell
   node --version
   npm --version
   ```
   Expected: `v20.x.x` and `10.x.x`

---

## Step 1 — Get the Code

If you are cloning fresh from GitHub:
```powershell
git clone https://github.com/serc-courses/project-3-team-29.git
cd project-3-team-29
```

If you already have the folder, navigate to it:
```powershell
cd "C:\path\to\project3"
```

---

## Option A — Automated Setup (Recommended)

Run the setup script. It does everything automatically: starts Docker containers, compiles Java, installs npm deps, starts all three services.

```powershell
# Allow script execution (run once on a new machine)
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned

# Run the setup script from the project root
.\setup.ps1
```

Wait ~2 minutes. When done, you will see:
```
======================================================
  Services running:
  - Backend API   : http://localhost:8080
  - Admin UI      : http://localhost:5173
  - User/Advisor  : http://localhost:5174
  ...
======================================================
```

Open your browser and go to http://localhost:5174 to use the app.

---

## Quick Start — One Command (After Prerequisites)

If you have already done Step 0 (installed Docker, Java, Maven, Node) and your dependencies are installed, run everything with a single command:

### On Linux / macOS / WSL:

```bash
# Start Docker infrastructure in background
docker compose up -d

# Run all services (backend + 2 frontends in one terminal with color-coded logs)
make run
```

Or manually:
```bash
bash scripts/run.sh
```

This starts all services with prefixed logs so you can see what's happening. Press `Ctrl+C` to stop everything.

### On Windows (PowerShell):

If you don't have `make` or bash, use this alternative:

```powershell
# 1. Start Docker infrastructure
docker compose up -d

# 2. Install frontend deps (if not already done)
cd frontend; npm install; cd ..
cd user-frontend; npm install; cd ..

# 3. Start all three services in separate windows or tabs:
#    Window 1 — Backend:
mvn exec:java -Dexec.mainClass=com.iiit.oms.OmsApplication

#    Window 2 — Admin UI:
cd frontend; npm run dev

#    Window 3 — User/Advisor UI:
cd user-frontend; npm run dev
```

---

## Option B — Manual Setup (Step by Step)

Use this if the automated script fails or you want to understand each step.

### Step 1 — Start infrastructure (PostgreSQL + Redis + Kafka)

Open PowerShell in the project root folder and run:

```powershell
docker compose up -d
```

This downloads and starts three containers in the background. First run takes 2–5 minutes to download images.

Check they are running:
```powershell
docker compose ps
```
Expected output — all three should show `running` or `Up`:
```
NAME                   STATUS
project3-postgres-1    Up (healthy)
project3-redis-1       Up
project3-kafka-1       Up
```

Wait until PostgreSQL shows `(healthy)` before continuing. Run `docker compose ps` again after 30 seconds if it shows `starting`.

### Step 2 — Compile the backend

```powershell
mvn compile
```

This downloads all Java dependencies (first run takes 1–2 minutes) and compiles all Java files.  
Expected: `BUILD SUCCESS` at the end.

### Step 3 — Start the backend server

Open a **new PowerShell window** and run:

```powershell
$env:OMS_DB_URL = "jdbc:postgresql://localhost:5432/oms"
$env:OMS_DB_USER = "oms_user"
$env:OMS_DB_PASSWORD = "oms_pass"
$env:OMS_DB_CLEAN_START = "true"
$env:OMS_REDIS_HOST = "localhost"
$env:OMS_REDIS_PORT = "6379"
$env:OMS_KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"

mvn exec:java -Dexec.mainClass=com.iiit.oms.OmsApplication
```

**Environment variables explained:**

| Variable | Purpose |
|----------|---------|
| `OMS_DB_URL` | PostgreSQL connection URL |
| `OMS_DB_USER` | DB username |
| `OMS_DB_PASSWORD` | DB password |
| `OMS_DB_CLEAN_START` | Set `true` to wipe all data on startup (fresh demo). Set `false` to keep existing orders/sessions. |
| `OMS_REDIS_HOST` / `OMS_REDIS_PORT` | Redis for idempotency keys, session tokens, and bulk dedup |
| `OMS_KAFKA_BOOTSTRAP_SERVERS` | Kafka broker for event streaming (order processing, SSE bridge, audit log) |

Wait until you see **all** of these lines in the output:
```
OMS HTTP server started on port 8080
Seeded default accounts count: 10
Projection replay complete: N orders, N bulk orders.
BatchoutScheduler: Redis dedup pool connected
Session store: Redis at localhost:6379
KafkaOrderProcessingConsumer started, subscribed to [oms.orders.received]
KafkaAuditLogConsumer started, subscribed to all OMS topics
```

Then the backend is ready. **Leave this window open** — closing it stops the server.

### Step 4 — Install and start the admin frontend

Open a **new PowerShell window**:

```powershell
cd frontend
npm install    # only needed the first time
npm run dev
```

Wait until you see:
```
  VITE v8.x.x  ready in xxx ms
  ➜  Local:   http://localhost:5173/
```

### Step 5 — Install and start the user/advisor frontend

Open **another new PowerShell window**:

```powershell
cd user-frontend
npm install    # only needed the first time
npm run dev
```

Wait until you see:
```
  VITE v5.x.x  ready in xxx ms
  ➜  Local:   http://localhost:5174/
```

---

## Login Credentials

### User/Advisor Frontend (http://localhost:5174)

The login page shows test credentials. These are the main ones:

| Role | Username | Password | Account |
|------|----------|----------|---------|
| Investor | `john.miller` | `invest123` | ACCT00001 |
| Investor | `emma.johnson` | `invest123` | ACCT00002 |
| Investor | `alice.wong` | `invest123` | ACCT00003 |
| Advisor | `advisor1` | `advise123` | ADV001 |
| Advisor | `advisor2` | `advise123` | ADV002 |

### Admin Frontend (http://localhost:5173)

Admin login is required.

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |

Important: pages like Reconciliation and Portfolio P/L are admin-focused; if you log in with advisor/investor credentials, those pages can show "Failed to load ..." due to role restrictions.

Quick API check for admin login:
```powershell
curl -s -o NUL -w "%{http_code}" -X POST http://localhost/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'
```
Expected status: `200`

Quick role check for admin-only pages (PowerShell):
```powershell
$token = (Invoke-RestMethod -Method POST -Uri http://localhost/auth/login -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}').token
Invoke-RestMethod -Method GET -Uri http://localhost/view/reconciliation -Headers @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Method GET -Uri http://localhost/view/portfolio -Headers @{ Authorization = "Bearer $token" }
```

---

## Daily Use (After First Setup)

Once everything is installed, your daily workflow is:

1. **Start Docker Desktop** (open the app from Start Menu, wait for green status)
2. **Start infrastructure:**
   ```powershell
   docker compose up -d
   ```
3. **Start backend** (in one PowerShell window):
   ```powershell
   $env:OMS_DB_URL="jdbc:postgresql://localhost:5432/oms"
   $env:OMS_DB_USER="oms_user"
   $env:OMS_DB_PASSWORD="oms_pass"
   $env:OMS_DB_CLEAN_START="false"
   $env:OMS_REDIS_HOST="localhost"
   $env:OMS_REDIS_PORT="6379"
   $env:OMS_KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
   mvn exec:java -Dexec.mainClass=com.iiit.oms.OmsApplication
   ```
   Note: use `OMS_DB_CLEAN_START=false` to keep your existing data (orders, sessions).
   Use `OMS_DB_CLEAN_START=true` for a fresh demo with clean tables.
4. **Start admin frontend** (another window):
   ```powershell
   cd frontend; npm run dev
   ```
5. **Start user frontend** (another window):
   ```powershell
   cd user-frontend; npm run dev
   ```

---

## Stopping Everything

To stop all services:

1. Press `Ctrl+C` in the backend PowerShell window
2. Press `Ctrl+C` in the frontend windows
3. Stop Docker containers:
   ```powershell
   docker compose down
   ```

---

## Troubleshooting

### "docker ps" gives an error about the pipe
Docker Desktop is not running. Open it from the Start Menu and wait 30–60 seconds for the engine to start, then try again.

### Backend fails to start with "timezone" error
This should be fixed, but if it happens again, set your system timezone to "UTC" in Windows Settings → Time & Language → Date & Time.

### "Port 8080 already in use"
Another process is using port 8080. Find and kill it:
```powershell
Get-Process -Name java | Stop-Process -Force
```
Then start the backend again.

### "Port 5173 already in use" or "Port 5174 already in use"
```powershell
# Find what's using the port (replace 5173 with 5174 if needed)
netstat -ano | findstr :5173
# Kill it (replace PID with the number from the output)
Stop-Process -Id <PID> -Force
```

### Backend compiled but orders fail with "Account ID does not exist"
Make sure you used account IDs in the format `ACCT00001`, `ACCT00002`, etc. (not `ACC001`).

### PostgreSQL shows "starting" for too long
```powershell
docker logs project3-postgres-1
```
Look for errors. Usually it resolves in 15–30 seconds.

### Frontend shows blank page or 404
Make sure the backend stack is running. The frontend proxies API calls to `http://localhost` (nginx on port 80). If backend/nginx is down, all API calls fail.

### Admin login works but Reconciliation/Portfolio shows "Failed to load ..."
This usually means you are not logged in as admin. Sign out from `http://localhost:5173`, then log in with:
- Username: `admin`
- Password: `admin123`

### npm install fails
Make sure you are in the right folder (`frontend` for admin, `user-frontend` for user). Delete the `node_modules` folder and try again:
```powershell
Remove-Item -Recurse -Force node_modules
npm install
```

---

## Architecture Quick Reference

```
Browser (User)       Browser (Admin)
      |                     |
 :5174 React           :5173 React
      |                     |
      └────────┬────────────┘
               ↓
         :8080 Java Backend
               |
    ┌──────────┼──────────────┐
    ↓          ↓              ↓
PostgreSQL    Redis          Kafka
 :5432        :6379          :9092
(main DB)  (3 uses*)     (3 consumers*)
```

**Redis (3 uses):**
1. Inbound idempotency — blocks duplicate `POST /orders/plan` with same `X-Idempotency-Key`
2. Session tokens — user login JWTs stored 24h, survive backend restarts
3. Outbound bulk dedup — prevents re-transmitting the same bulk order to Transfer Agent

**Kafka (3 consumers):**
1. `KafkaOrderProcessingConsumer` — asynchronously processes orders from `oms.orders.received`
2. `KafkaNotificationConsumer` — bridges `ORDER_STATE_CHANGED` events → SSE for real-time browser updates
3. `KafkaAuditLogConsumer` — records all events to `oms_audit_log` table

- SSE (`/view/stream`) pushes real-time updates to the browser — no manual refresh needed

---

## Complete Order Flow (End to End)

This is the full lifecycle of a mutual fund order in the system:

| Step | Who triggers it | State transition | What happens |
|------|----------------|-----------------|--------------|
| 1 | User places order in PWA | → PLANNED | `POST /orders/plan` saves to DB, publishes to Kafka |
| 2 | `OrderStateMachine` (async) | PLANNED → VALIDATED | Checks account exists and has enough balance |
| 3 | `OrderStateMachine` | VALIDATED → ENRICHED | Attaches NAV, fund family, transfer agent, trade date, settlement date |
| 4 | `OrderStateMachine` | ENRICHED → PLACED | Order ready for batching |
| 5 | `BatchoutScheduler` (every 60s) | PLACED → BULKED | Groups orders for the same fund into one bulk order |
| 6 | `BatchoutScheduler` | BULKED → TRANSMITTED | Sends bulk order to Transfer Agent (NSCC or RBC) |
| 7 | Admin clicks **Confirm Orders** | TRANSMITTED → CONFIRMED | TA has acknowledged the trade |
| 8 | Admin clicks **Simulate TA Contract** (Bulk Orders page) | CONFIRMED → CONTRACTED → BOOKED | TA sends final NAV, allocated shares, contract reference; units credited |

**Testing the flow quickly:**
1. Log into user PWA at http://localhost:5174 → Place Order
2. Watch the admin dashboard at http://localhost:5173 for real-time updates (no refresh needed)
3. Orders page: see orders progress through states automatically
4. Bulk Orders page: wait ~60s for TRANSMITTED, then click "Simulate TA Contract" to enter final NAV and shares
5. Orders should reach BOOKED state
