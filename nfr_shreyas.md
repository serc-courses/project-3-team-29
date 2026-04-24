# NFR1 to NFR5 in Simple Words

This document explains each NFR in plain language:
- what was implemented,
- why it helps,
- and how to show it during evaluation.

---

## NFR 1: Secure Login and Role Permissions

### What was implemented
- A login system using token-based authentication.
- Three roles: Admin, Advisor, Investor.
- Role checks on backend APIs so users can only access allowed actions/pages.
- Logout revokes access token.

### Why this helps
- Prevents unauthorized access to sensitive operations.
- Keeps admin operations protected.
- Supports auditability because access is controlled and traceable.

### Simple explanation
After login, the system gives a digital pass (token). Every request carries this pass. The backend checks role + permission before allowing the action.

### How to show TA
1. Login as admin and open admin-only pages (works).
2. Login as investor/advisor and try same admin page (should deny access/redirect).
3. Logout and try action again (token no longer valid).

---

## NFR 2: Encrypted Communication (TLS 1.2+)

### What was implemented
- Backend supports HTTPS using TLS 1.2/1.3.
- Old insecure protocols are disabled.
- Frontend proxy points to secure backend endpoint.

### Why this helps
- Login credentials and order data stay encrypted in transit.
- Reduces risk of packet sniffing and man-in-the-middle attacks.
- Meets expected security baseline for financial systems.

### Simple explanation
Data between browser and server is locked while traveling on network, so outsiders cannot read it.

### How to show TA
1. Open browser DevTools Network tab and inspect request security details.
2. Show that secure protocol is used.
3. Optional terminal check:

```bash
curl -v https://localhost:8080 2>&1 | grep -i tls
```

---

## NFR 3: SLA Dashboard (Deadline + Latency Visibility)

### What was implemented
- Dashboard widget showing:
  - Time remaining to onshore cutoff.
  - Time remaining to offshore cutoff.
  - API latency and request count.
- Widget updates countdown continuously and refreshes server metrics periodically.

### Why this helps
- Team can see deadline risk instantly.
- Supports operational awareness during order windows.
- Latency view helps detect performance degradation early.

### Simple explanation
The dashboard acts like a live timer plus health monitor so operators know how much time is left and if system is responding well.

### How to show TA
1. Open dashboard and show live ticking countdown.
2. Show color changes when deadline gets closer.
3. Make a few requests and show request counter/latency updates.

---

## NFR 4: Backup and Archive (WORM style)

### What was implemented
- Weekly archival job exports old audit logs to compressed files.
- Archive metadata is recorded in DB (run time, record count, file path, checksum).
- Exported records are marked archived to avoid duplicate archival.
- SHA-256 checksum is stored for integrity verification.

### Why this helps
- Supports compliance and long-term traceability.
- Proves records were not changed after archival.
- Saves storage via compression.
- Makes historical investigations possible.

### Simple explanation
Older audit records are moved into sealed archive files. Each file has a fingerprint (checksum). If file changes later, fingerprint mismatch reveals tampering.

## NFR 4: TA Evaluation Demo Script

### A. Generate/ensure audit data exists
1. Start system and perform actions (login, place/cancel orders) to create audit records.
2. Verify audit rows exist:

```bash
docker exec project3-postgres-1 psql -U oms_user -d oms -c "SELECT COUNT(*) FROM order_audit_log;"
```

### B. Show archive outputs
1. List archive files:

```bash
ls -lah audit-archives/
```

2. Show archive runs table:

```bash
docker exec project3-postgres-1 psql -U oms_user -d oms -c "SELECT run_id, archived_at, records_count, file_path, checksum_sha256 FROM audit_archive_runs ORDER BY archived_at DESC LIMIT 5;"
```

### C. Show archive content is readable
Use latest archive file name from Step B:

```bash
zcat audit-archives/<latest_file>.json.gz | head -5
```

### D. Prove integrity (very important for TA)
1. Compute file checksum:

```bash
sha256sum audit-archives/<latest_file>.json.gz
```

2. Compare with checksum_sha256 shown in DB table.
3. Explain: if both match, file is unchanged since archival.

### E. Prove idempotency (no duplicate re-archive)
1. Note current count of archive runs.
2. Re-run service cycle / wait next schedule.
3. Show only expected new runs appear, and same records are not repeatedly archived.

### What to say to TA in one line
We archive old audit logs to compressed immutable-style files, store checksum in DB, and verify checksum at evaluation time to prove no tampering.

---

## NFR 5: Active-Active Availability and Failover

### What was implemented
- Two backend instances run in parallel.
- Nginx load balances traffic between instances.
- Shared Redis state for sessions/idempotency/locks.
- Distributed locking prevents duplicate scheduler execution across replicas.

### Why this helps
- If one app instance fails, service continues through the other.
- Better uptime and reliability.
- Prevents duplicate background processing in multi-instance mode.

### Simple explanation
There are two servers behind a traffic manager. If one crashes, traffic automatically goes to the other with minimal user impact.

### How to show TA
1. Confirm both instances are running.
2. Open app and perform actions normally.
3. Stop one app instance:

```bash
docker compose stop app_1
```

4. Refresh app and show it still works.
5. Restart app_1 and show load balancing returns.

---

## Quick Summary Table

| NFR | What was implemented | Why it helps | Demo proof |
|---|---|---|---|
| NFR1 | Token login + role-based access | Security and controlled access | Role-based page/API allow/deny |
| NFR2 | TLS-secured communication | Data privacy in transit | DevTools/curl security check |
| NFR3 | SLA countdown + latency widget | Deadline awareness and health visibility | Live timer and metrics change |
| NFR4 | Weekly audit archival + checksum | Compliance, integrity, traceability | Archive files + DB metadata + checksum match |
| NFR5 | 2 app replicas + load balancer + locks | High availability and safe scaling | Kill one replica and app still serves |

---

If needed, this can be presented in 5 minutes by showing one proof per NFR (one screen/command each).
