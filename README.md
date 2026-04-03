# Mutual Fund OMS

## What This App Uses

- Persistence backend: PostgreSQL (default in OmsApplication)
- REST server: http://localhost:8080
- Main class: com.iiit.oms.OmsApplication

## PostgreSQL Prerequisites

The application is configured to use PostgreSQL by default.
Install PostgreSQL, start the service, and create the oms database before starting the app.

### macOS

1. Install PostgreSQL:

```bash
brew install postgresql@16
```

2. Start PostgreSQL service:

```bash
brew services start postgresql@16
```

3. Create the database once:

```bash
/opt/homebrew/opt/postgresql@16/bin/createdb oms
```

4. Verify PostgreSQL is running:

```bash
brew services list | grep postgresql
```

5. Verify connectivity and local PostgreSQL role:

```bash
/opt/homebrew/opt/postgresql@16/bin/psql -h localhost -d oms -c "SELECT current_user, current_database();"
```

Note:

- For Homebrew installs, your local macOS user is commonly the PostgreSQL role (for example: krishna).
- The app default is OMS_DB_USER=postgres, but that role may not exist in a local Homebrew setup.
- Set OMS_DB_USER to your actual local role when starting the app.

### Windows

1. Install PostgreSQL:
- Download and install from the official PostgreSQL installer for Windows.
- During setup, remember the superuser password for postgres.

2. Start PostgreSQL service:
- Open Services and ensure a service like postgresql-x64-16 is Running.

3. Create the database once (PowerShell example):

```powershell
& "C:\Program Files\PostgreSQL\16\bin\createdb.exe" -U postgres oms
```

4. Verify connectivity:

```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -d oms -c "SELECT version();"
```

If psql and createdb are not recognized, add C:\Program Files\PostgreSQL\16\bin to your PATH.

## Daily Operations (macOS)

### 1. Start PostgreSQL

```bash
brew services start postgresql@16
```

### 2. Ensure database exists (run once, safe to retry)

```bash
/opt/homebrew/opt/postgresql@16/bin/createdb oms
```

### 3. Start application

```bash
cd /Users/krishna/phd/pgssp/2026_project3/workspace/project-3-team-29
export OMS_DB_URL="jdbc:postgresql://localhost:5432/oms"
export OMS_DB_USER="$(whoami)"
export OMS_DB_PASSWORD=""
export OMS_DB_CLEAN_START="false"

mvn -q -DskipTests compile dependency:copy-dependencies -DincludeScope=runtime
java -cp "target/classes:target/dependency/*" com.iiit.oms.OmsApplication
```

### 4. Stop application

- Press Ctrl+C in the app terminal.

### 5. Stop PostgreSQL (optional)

```bash
brew services stop postgresql@16
```

## Restart Options

This application supports two restart modes when using PostgreSQL:

1. Normal restart (preserve data)
2. Clean restart (wipe data and start fresh)

Schema creation is always safe on restart because tables are created with `CREATE TABLE IF NOT EXISTS`.

## Environment Variables

Set these before starting the app:

- `OMS_DB_URL` (default: `jdbc:postgresql://localhost:5432/oms`)
- `OMS_DB_USER` (default: `postgres`; on Homebrew macOS this is often your local user)
- `OMS_DB_PASSWORD` (default: `postgres`)
- `OMS_DB_CLEAN_START` (default: not set / `false`)

## Option 1: Normal Restart (Preserve Existing Data)

Do not set `OMS_DB_CLEAN_START`, or set it to `false`.

```bash
cd project-3-team-29
export OMS_DB_URL="jdbc:postgresql://localhost:5432/oms"
export OMS_DB_USER="$(whoami)"
export OMS_DB_PASSWORD=""
unset OMS_DB_CLEAN_START

mvn -q -DskipTests compile
mvn -q dependency:copy-dependencies -DincludeScope=runtime
java -cp "target/classes:target/dependency/*" com.iiit.oms.OmsApplication
```

Expected behavior:

- Existing records remain in the database.
- Missing tables are created if needed.

## Option 2: Clean Restart (Wipe Data)

Set `OMS_DB_CLEAN_START=true` before starting.

```bash
cd project-3-team-29
export OMS_DB_URL="jdbc:postgresql://localhost:5432/oms"
export OMS_DB_USER="$(whoami)"
export OMS_DB_PASSWORD=""
export OMS_DB_CLEAN_START="true"

mvn -q -DskipTests compile
mvn -q dependency:copy-dependencies -DincludeScope=runtime
java -cp "target/classes:target/dependency/*" com.iiit.oms.OmsApplication
```

Expected behavior:

- Tables are retained.
- Data is cleared from these tables on startup:
  - `bulk_order_mappings`
  - `bulk_orders`
  - `orders`
  - `funds`
  - `accounts`

## Important Notes

- Clean restart is opt-in and only happens when `OMS_DB_CLEAN_START=true`.
- After a clean restart, set `OMS_DB_CLEAN_START=false` (or unset it) for regular restarts.
- You can also start from VS Code by running `OmsApplication.main`; the same environment variables apply.

## Manual DB Inspection (Command Line)

### Connect to DB (interactive)

```bash
/opt/homebrew/opt/postgresql@16/bin/psql -h localhost -d oms -U "$(whoami)"
```

### Inside psql: list and inspect tables

```sql
\dt
\d accounts
\d funds
\d orders
\d bulk_orders
\d bulk_order_mappings
```

### Inside psql: inspect data

```sql
SELECT * FROM accounts;
SELECT * FROM funds;
SELECT * FROM orders;
SELECT * FROM bulk_orders;
SELECT * FROM bulk_order_mappings;
```

### Inside psql: row counts

```sql
SELECT 'accounts' AS table_name, COUNT(*) FROM accounts
UNION ALL SELECT 'funds', COUNT(*) FROM funds
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'bulk_orders', COUNT(*) FROM bulk_orders
UNION ALL SELECT 'bulk_order_mappings', COUNT(*) FROM bulk_order_mappings;
```

### Exit psql

```sql
\q
```

### Non-interactive examples

```bash
/opt/homebrew/opt/postgresql@16/bin/psql -h localhost -d oms -U "$(whoami)" -c "\dt"
/opt/homebrew/opt/postgresql@16/bin/psql -h localhost -d oms -U "$(whoami)" -c "SELECT * FROM orders;"
```

## API Smoke Test

After app startup, run these commands in a separate terminal.

### Plan two orders

```bash
curl -i -X POST http://localhost:8080/orders/plan \
  -H "Content-Type: application/json" \
  -d '[
    {"productID":"FND001","amount":1000,"accountID":"ACCT00001","orderSide":"BUY"},
    {"productID":"FND001","amount":1500,"accountID":"ACCT00002","orderSide":"BUY"}
  ]'
```

The server assigns `orderID` automatically when it is omitted from the payload.

### List all orders

```bash
curl -s http://localhost:8080/orders
```

### Get order status by ID

```bash
curl -s "http://localhost:8080/orders/status?orderID=ORD900"
```

Example response:

```json
{"orderID":"ORD900","orderStatus":"BULKED","errorDescription":""}
```

Order status flow:

- PLANNED -> VALIDATED -> ENRICHED -> PLACED -> BULKED -> CONFIRMED -> CONTRACTED -> BOOKED
- Any failure transitions to ERRORED with errorDescription

## Quick Start on macOS (Verified)

If you installed PostgreSQL via Homebrew and created the oms database, use this exact flow:

```bash
cd project-3-team-29
export OMS_DB_URL="jdbc:postgresql://localhost:5432/oms"
export OMS_DB_USER="$(whoami)"
export OMS_DB_PASSWORD=""
export OMS_DB_CLEAN_START="false"

mvn -q -DskipTests compile dependency:copy-dependencies -DincludeScope=runtime
java -cp "target/classes:target/dependency/*" com.iiit.oms.OmsApplication
```

## Troubleshooting

Connection to localhost:5432 refused:

```bash
brew services start postgresql@16
/opt/homebrew/opt/postgresql@16/bin/pg_isready -h localhost -p 5432
```

FATAL: role postgres does not exist:

- Set OMS_DB_USER to your local role:

```bash
export OMS_DB_USER="$(whoami)"
```

## Windows Environment Variable Examples

Use these if you are starting the app from PowerShell.

Normal restart:

```powershell
cd project-3-team-29
$env:OMS_DB_URL = "jdbc:postgresql://localhost:5432/oms"
$env:OMS_DB_USER = "postgres"
$env:OMS_DB_PASSWORD = "postgres"
$env:OMS_DB_CLEAN_START = "false"

mvn -q -DskipTests compile
mvn -q dependency:copy-dependencies -DincludeScope=runtime
java -cp "target/classes;target/dependency/*" com.iiit.oms.OmsApplication
```

Clean restart:

```powershell
cd project-3-team-29
$env:OMS_DB_URL = "jdbc:postgresql://localhost:5432/oms"
$env:OMS_DB_USER = "postgres"
$env:OMS_DB_PASSWORD = "postgres"
$env:OMS_DB_CLEAN_START = "true"

mvn -q -DskipTests compile
mvn -q dependency:copy-dependencies -DincludeScope=runtime
java -cp "target/classes;target/dependency/*" com.iiit.oms.OmsApplication
```
