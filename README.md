# Mutual Fund OMS

## What This App Uses

- Persistence backend: PostgreSQL (default in OmsApplication)
- REST server: http://localhost:8080
- Main class: com.iiit.oms.OmsApplication

## PostgreSQL Prerequisites

The application is configured to use PostgreSQL by default.
Install PostgreSQL, start the service, and create the oms database before starting the app.

---

## macOS

### 1. Install PostgreSQL

```bash
brew install postgresql@16
````

### 2. Start PostgreSQL service

```bash
brew services start postgresql@16
```

### 3. Create the database once

```bash
/opt/homebrew/opt/postgresql@16/bin/createdb oms
```

### 4. Verify PostgreSQL is running

```bash
brew services list | grep postgresql
```

### 5. Verify connectivity

```bash
/opt/homebrew/opt/postgresql@16/bin/psql -h localhost -d oms -c "SELECT current_user, current_database();"
```

---

## Linux (Ubuntu / Debian)

### 1. Install PostgreSQL

```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

### 2. Start PostgreSQL service

```bash
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### 3. Create the database once

```bash
sudo -i -u postgres
createdb oms
exit
```

### 4. Verify connectivity

```bash
psql -U postgres -d oms -c "SELECT current_user, current_database();"
```

If that fails:

```bash
sudo -u postgres psql -d oms -c "SELECT current_user, current_database();"
```

---

## Windows

### 1. Install PostgreSQL

* Download from official installer
* Remember postgres password

### 2. Start PostgreSQL

* Ensure service is running in Services panel

### 3. Create database

```powershell
& "C:\Program Files\PostgreSQL\16\bin\createdb.exe" -U postgres oms
```

### 4. Verify

```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -d oms -c "SELECT version();"
```

---

## Daily Operations (macOS / Linux)

### 1. Start PostgreSQL

**macOS**

```bash
brew services start postgresql@16
```

**Linux**

```bash
sudo systemctl start postgresql
```

---

### 2. Ensure database exists

**macOS**

```bash
/opt/homebrew/opt/postgresql@16/bin/createdb oms
```

**Linux**

```bash
sudo -u postgres createdb oms
```

---

### 3. Start application

```bash
cd project-3-team-29

export OMS_DB_URL="jdbc:postgresql://localhost:5432/oms"
export OMS_DB_USER="$(whoami)"   # use "postgres" on Linux if needed
export OMS_DB_PASSWORD=""
export OMS_DB_CLEAN_START="false"

mvn -q -DskipTests compile dependency:copy-dependencies -DincludeScope=runtime
java -cp "target/classes:target/dependency/*" com.iiit.oms.OmsApplication
```

---

### 4. Stop application

```
Ctrl + C
```

---

### 5. Stop PostgreSQL (optional)

**macOS**

```bash
brew services stop postgresql@16
```

**Linux**

```bash
sudo systemctl stop postgresql
```

---

## Restart Options

### Option 1: Normal Restart

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

---

### Option 2: Clean Restart

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

---

## Manual DB Inspection

### Connect

```bash
psql -U postgres -d oms
```

or

```bash
sudo -u postgres psql -d oms
```

---

### Inspect tables

```sql
\dt
\d orders
\d accounts
\d funds
\d bulk_orders
\d bulk_order_mappings
```

---

### Query data

```sql
SELECT * FROM orders;
```

---

### Exit

```sql
\q
```

---

## API Smoke Test

### Plan orders

```bash
curl -i -X POST http://localhost:8080/orders/plan \
  -H "Content-Type: application/json" \
  -d '[
    {"productID":"FND001","amount":1000,"accountID":"ACCT00001","orderSide":"BUY"},
    {"productID":"FND001","amount":1500,"accountID":"ACCT00002","orderSide":"BUY"}
  ]'
```

---

### List orders

```bash
curl -s http://localhost:8080/orders
```

---

### Order status

```bash
curl -s "http://localhost:8080/orders/status?orderID=ORD900"
```

---

## Troubleshooting

### PostgreSQL not running

```bash
# macOS
brew services start postgresql@16

# Linux
sudo systemctl start postgresql
```

---

### Port issue

```bash
sudo lsof -i :5432
```

---

### Role does not exist

```bash
export OMS_DB_USER="$(whoami)"
```

---

### Linux auth issue (peer vs password)

```bash
sudo nano /etc/postgresql/*/main/pg_hba.conf
```

Change:

```
local   all   all   peer
```

to:

```
local   all   all   md5
```

Restart:

```bash
sudo systemctl restart postgresql
```

---

## Windows Environment Variables

### Normal restart

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

---

### Clean restart

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


