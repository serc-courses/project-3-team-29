# Non-Functional Requirements To Implement

## 1. Security Enhancements
- [x] **Database PII Encryption**: Implement AES-256 at-rest encryption for sensitive fields like `national_identity`.
- [ ] **TLS 1.2+ Migration**: Configure HTTPS endpoints and enforce TLS for all API communication.
- [ ] **Strict RBAC Enforcement**: Add token-based (JWT) validation middleware to securely separate Admin vs. User workloads on the backend.

## 2. Observability & Monitoring
- [x] **Real-Time Latency Metrics**: Inject filter traces to measure and expose p99 API ingestion latencies.
- [ ] **SLA Dashboards**: Expose countdowns and active tracking for onshore/offshore TA cutoffs (04:00 PM / 01:00 AM) to the frontend.

## 3. Availability & Auditing
- [ ] **Active-Active Failover**: Introduce an Nginx Load Balancer and scale internal `docker-compose` topology to multiple app replicas.
- [ ] **WORM Archival Strategy**: Implement immutable 7-year storage rotation dumps for the Kafka `AuditLog`.
