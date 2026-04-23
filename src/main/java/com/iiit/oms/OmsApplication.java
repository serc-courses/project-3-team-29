package com.iiit.oms;

import com.iiit.oms.db.postgres.PostgresAccountDatabase;
import com.iiit.oms.db.postgres.PostgresAdvisorClientRelationshipDatabase;
import com.iiit.oms.db.postgres.PostgresAdvisorDatabase;
import com.iiit.oms.db.postgres.PostgresAuditLogDatabase;
import com.iiit.oms.db.postgres.PostgresBulkOrderDatabase;
import com.iiit.oms.db.postgres.PostgresBulkOrderMappingDatabase;
import com.iiit.oms.db.postgres.PostgresFundDatabase;
import com.iiit.oms.db.postgres.PostgresOrderDatabase;
import com.iiit.oms.db.util.PostgresConnectionFactory;
import com.iiit.oms.db.util.PostgresSchemaInitializer;
import com.iiit.oms.idempotency.IdempotencyStore;
import com.iiit.oms.idempotency.InMemoryIdempotencyStore;
import com.iiit.oms.idempotency.RedisIdempotencyStore;
import com.iiit.oms.interfaces.OrderRestServer;
import com.iiit.oms.kafka.KafkaAuditLogConsumer;
import com.iiit.oms.kafka.KafkaNotificationConsumer;
import com.iiit.oms.kafka.KafkaOrderProcessingConsumer;
import com.iiit.oms.kafka.KafkaOrderEventPublisher;
import com.iiit.oms.processor.BatchoutScheduler;
import com.iiit.oms.processor.OrderManager;
import com.iiit.oms.processor.OrderScheduler;
import com.iiit.oms.processor.OrderStateMachine;
import com.iiit.oms.readmodel.OrderProjectionListener;
import com.iiit.oms.readmodel.ProjectionStore;
import com.iiit.oms.readmodel.impl.DefaultOrderProjectionListener;
import com.iiit.oms.readmodel.impl.InMemoryProjectionStore;
import com.iiit.oms.readmodel.impl.MongoDbProjectionStore;
import com.iiit.oms.repository.AccountRepository;
import com.iiit.oms.repository.AdvisorClientRelationshipRepository;
import com.iiit.oms.repository.AdvisorRepository;
import com.iiit.oms.repository.AuditLogRepository;
import com.iiit.oms.repository.BulkOrderRepository;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.repository.OrderRepository;
import com.iiit.oms.repository.UserRepository;
import com.iiit.oms.repository.inmemory.InMemoryUserRepository;
import com.iiit.oms.repository.postgres.PostgresAccountRepository;
import com.iiit.oms.repository.postgres.PostgresAdvisorClientRelationshipRepository;
import com.iiit.oms.repository.postgres.PostgresAdvisorRepository;
import com.iiit.oms.repository.postgres.PostgresAuditLogRepository;
import com.iiit.oms.repository.postgres.PostgresBulkOrderMappingRepository;
import com.iiit.oms.repository.postgres.PostgresBulkOrderRepository;
import com.iiit.oms.repository.postgres.PostgresFundRepository;
import com.iiit.oms.repository.postgres.PostgresOrderRepository;
import com.iiit.oms.transfer.TransferAgentRouter;
import com.iiit.oms.util.AccountMockDataUtil;
import com.iiit.oms.util.AdvisorMockDataUtil;
import com.iiit.oms.util.FundMockDataUtil;
import com.iiit.oms.util.UserSeedDataUtil;
import com.iiit.oms.db.postgres.PostgresReconciliationBreakDatabase;
import com.iiit.oms.repository.postgres.PostgresReconciliationBreakRepository;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class OmsApplication {
    private static final Logger LOGGER = Logger.getLogger(OmsApplication.class.getName());
    private static final int SERVER_PORT = 8080;
    private static final String CLEAN_START_ENV_VAR = "OMS_DB_CLEAN_START";
    private static final String CQRS_USE_MONGO_ENV_VAR = "OMS_CQRS_USE_MONGO";
    private static final String CQRS_MONGO_URI_ENV_VAR = "OMS_MONGO_URI";
    private static final String CQRS_MONGO_DB_ENV_VAR = "OMS_MONGO_DB";

    public static void main(String[] args) throws IOException {
        // Fix: PostgreSQL rejects legacy JVM timezone "Asia/Calcutta" — normalise to
        // UTC
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));

        PostgresConnectionFactory connectionFactory = PostgresConnectionFactory.fromEnvironment();
        boolean cleanStart = isEnabled(CLEAN_START_ENV_VAR);
        PostgresSchemaInitializer.initialize(connectionFactory, cleanStart);

        AccountRepository accountRepository = new PostgresAccountRepository(
                new PostgresAccountDatabase(connectionFactory));
        seedAccountsIfMissing(accountRepository);

        FundRepository fundRepository = new PostgresFundRepository(new PostgresFundDatabase(connectionFactory));
        seedFundsIfMissing(fundRepository);

        AdvisorRepository advisorRepository = new PostgresAdvisorRepository(
                new PostgresAdvisorDatabase(connectionFactory));
        AdvisorClientRelationshipRepository relationshipRepository = new PostgresAdvisorClientRelationshipRepository(
                new PostgresAdvisorClientRelationshipDatabase(connectionFactory));
        AdvisorMockDataUtil.seedIfMissing(advisorRepository, relationshipRepository);

        UserRepository userRepository = new InMemoryUserRepository();
        UserSeedDataUtil.seedIfMissing(userRepository);

        OrderRepository orderRepository = new PostgresOrderRepository(new PostgresOrderDatabase(connectionFactory));
        OrderStateMachine orderStateMachine = new OrderStateMachine(
                new OrderManager(accountRepository, fundRepository, orderRepository));
        OrderScheduler orderScheduler = new OrderScheduler(orderRepository, orderStateMachine);

        ProjectionStore projectionStore = createProjectionStore();
        OrderProjectionListener projectionListener = new DefaultOrderProjectionListener(projectionStore);

        PostgresBulkOrderMappingRepository bulkOrderMappingRepository = new PostgresBulkOrderMappingRepository(
                new PostgresBulkOrderMappingDatabase(connectionFactory));
        BulkOrderRepository bulkOrderRepository = new PostgresBulkOrderRepository(
                new PostgresBulkOrderDatabase(connectionFactory));
        PostgresAuditLogDatabase auditLogDatabase = new PostgresAuditLogDatabase(connectionFactory);
        PostgresAuditLogRepository auditLogRepository = new PostgresAuditLogRepository(auditLogDatabase);

        // Replay existing orders from DB into in-memory projection store on startup
        replayProjections(projectionStore, orderRepository, bulkOrderRepository, bulkOrderMappingRepository,
                fundRepository);

        // Wire audit log into state machine
        orderStateMachine.setAuditLogRepository(auditLogRepository);

        // Transfer agent router (NSCC + RBC simulators)
        TransferAgentRouter transferAgentRouter = new TransferAgentRouter();

        // Kafka publisher (silently disabled if Kafka unavailable)
        String kafkaBootstrap = getOrDefault("OMS_KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        KafkaOrderEventPublisher kafkaPublisher = new KafkaOrderEventPublisher(kafkaBootstrap);

        // Idempotency store (try Redis, fall back to in-memory)
        IdempotencyStore idempotencyStore;
        String redisHost = getOrDefault("OMS_REDIS_HOST", "localhost");
        int redisPort = Integer.parseInt(getOrDefault("OMS_REDIS_PORT", "6379"));
        try {
            idempotencyStore = new RedisIdempotencyStore(redisHost, redisPort);
            System.out.println("Idempotency store: Redis at " + redisHost + ":" + redisPort);
            // Flush Redis idempotency keys on clean start so they stay in sync with the DB
            if (cleanStart && idempotencyStore instanceof RedisIdempotencyStore) {
                ((RedisIdempotencyStore) idempotencyStore).flushAll();
                System.out.println("Idempotency store: flushed (clean start)");
            }
        } catch (Exception e) {
            idempotencyStore = new InMemoryIdempotencyStore();
            System.out.println("Idempotency store: InMemory (Redis unavailable)");
        }

        BatchoutScheduler batchoutScheduler = new BatchoutScheduler(orderRepository, bulkOrderMappingRepository,
                bulkOrderRepository, fundRepository, projectionListener);
        batchoutScheduler.setTransferAgentRouter(transferAgentRouter);
        batchoutScheduler.setAuditLogRepository(auditLogRepository);
        // Wire Redis pool for outbound bulk dedup
        try {
            redis.clients.jedis.JedisPoolConfig poolCfg = new redis.clients.jedis.JedisPoolConfig();
            poolCfg.setMaxTotal(4);
            poolCfg.setMaxIdle(1);
            redis.clients.jedis.JedisPool sharedPool = new redis.clients.jedis.JedisPool(poolCfg, redisHost, redisPort,
                    1000);
            try (redis.clients.jedis.Jedis j = sharedPool.getResource()) {
                j.ping();
            }
            batchoutScheduler.setJedisPool(sharedPool);
            System.out.println("BatchoutScheduler: Redis dedup pool connected");
        } catch (Exception e) {
            System.out.println("BatchoutScheduler: Redis dedup disabled (unavailable)");
        }

        OrderRestServer orderRestServer = new OrderRestServer(
                SERVER_PORT,
                orderRepository,
                bulkOrderRepository,
                bulkOrderMappingRepository,
                fundRepository,
                orderStateMachine,
                projectionStore,
                projectionListener,
                accountRepository,
                advisorRepository,
                relationshipRepository,
                userRepository);

        orderRestServer.start();
        orderRestServer.setIdempotencyStore(idempotencyStore);
        orderRestServer.setAuditLogRepository(auditLogRepository);
        orderRestServer.setKafkaPublisher(kafkaPublisher);

        // ---- Reconciliation Engine ----
        PostgresReconciliationBreakDatabase reconBreakDb = new PostgresReconciliationBreakDatabase(connectionFactory);
        PostgresReconciliationBreakRepository reconBreakRepository = new PostgresReconciliationBreakRepository(
                reconBreakDb);
        orderRestServer.setReconciliationBreakRepository(reconBreakRepository);

        // Scheduled job: escalate unresolved breaks older than 1 hour, runs every 60
        // seconds
        ScheduledExecutorService reconEscalationScheduler = Executors.newSingleThreadScheduledExecutor();
        reconEscalationScheduler.scheduleAtFixedRate(() -> {
            try {
                java.util.List<com.iiit.oms.model.ReconciliationBreak> staleBreaks = reconBreakRepository
                        .findUnresolvedOlderThan(3600); // 1 hour
                for (com.iiit.oms.model.ReconciliationBreak b : staleBreaks) {
                    reconBreakRepository.markEscalated(b.getBreakId());
                    LOGGER.warning("Reconciliation break " + b.getBreakId()
                            + " escalated (unresolved >1h, bulk=" + b.getBulkOrderId() + ")");
                }
                if (!staleBreaks.isEmpty()) {
                    LOGGER.info("Reconciliation escalation sweep: escalated " + staleBreaks.size() + " break(s)");
                }
            } catch (Exception ex) {
                LOGGER.severe("Reconciliation escalation sweep failed: " + ex.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);

        // Wire Redis session store so sessions survive restarts
        try {
            orderRestServer.setSessionStore(new com.iiit.oms.auth.RedisSessionStore(redisHost, redisPort));
            System.out.println("Session store: Redis at " + redisHost + ":" + redisPort);
        } catch (Exception e) {
            System.out.println("Session store: InMemory (Redis unavailable)");
        }

        // Wire SSE broadcaster, projection, and Kafka into OrderScheduler
        orderScheduler.setProjectionListener(projectionListener);
        orderScheduler.setFundRepository(fundRepository);
        orderScheduler.setSseBroadcaster(orderRestServer);
        orderScheduler.setKafkaPublisher(kafkaPublisher);
        orderScheduler.startPolling();

        // Wire SSE broadcaster and Kafka into BatchoutScheduler
        batchoutScheduler.setSseBroadcaster(orderRestServer);
        batchoutScheduler.setKafkaPublisher(kafkaPublisher);
        batchoutScheduler.startBatchout();

        // Kafka consumers (silently disabled if Kafka is unavailable)
        KafkaNotificationConsumer notificationConsumer = new KafkaNotificationConsumer(kafkaBootstrap, orderRestServer);
        notificationConsumer.start();
        KafkaAuditLogConsumer auditLogConsumer = new KafkaAuditLogConsumer(kafkaBootstrap, auditLogRepository);
        auditLogConsumer.start();
        // Kafka Problem 1: async order processing consumer
        KafkaOrderProcessingConsumer orderProcessingConsumer = new KafkaOrderProcessingConsumer(
                kafkaBootstrap, orderRepository, orderStateMachine, fundRepository,
                projectionListener, orderRestServer, kafkaPublisher);
        orderProcessingConsumer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            orderScheduler.shutdown();
            batchoutScheduler.shutdown();
            reconEscalationScheduler.shutdownNow();
            orderRestServer.stop(0);
            if (projectionStore instanceof MongoDbProjectionStore) {
                ((MongoDbProjectionStore) projectionStore).close();
            }
        }));

        System.out.println("OmsApplication started on http://localhost:" + SERVER_PORT);
        System.out.println("OrderScheduler started with 30 second interval");
        System.out.println("BatchoutScheduler started with 120 second interval");
        System.out.println(
                "Postgres DB bootstrap complete (set OMS_DB_URL/OMS_DB_USER/OMS_DB_PASSWORD to override defaults)");
        System.out.println("Postgres clean start: " + cleanStart + " (set " + CLEAN_START_ENV_VAR
                + "=true to wipe tables on startup)");
        System.out.println("Seeded default accounts count: " + accountRepository.findAll().size());
        System.out.println("Seeded default funds count: " + fundRepository.findAll().size());
        System.out.println("Seeded advisors count: " + advisorRepository.findAll().size());
        System.out.println("Seeded users count:    " + userRepository.findAll().size());
        System.out.println("--- OMS Endpoints ---");
        System.out.println("  POST /orders/plan             - Plan new orders");
        System.out.println("  GET  /orders                  - List all orders");
        System.out.println("  GET  /orders/status?orderID=  - Get status by order ID");
        System.out.println("  GET  /orders/audit?orderID=   - Get full audit trail for an order");
        System.out.println("  POST /orders/confirm          - Confirm BULKED/TRANSMITTED bulk orders");
        System.out.println("  POST /orders/book             - Book CONFIRMED bulk orders");
        System.out.println("  POST /transfer-agent/contract - Simulate TA contract callback (NAV + shares)");
        System.out.println("  GET  /view/reconciliation     - View reconciliation breaks");
        System.out.println("  POST /view/reconciliation/resolve - Resolve a reconciliation break (ACCEPT/REJECT)");
        System.out.println("  GET  /view/portfolio          - Portfolio P/L for BOOKED orders");
        System.out.println("  GET  /view/orders             - Query projected order read model");
        System.out.println("  GET  /view/bulk-orders        - Query projected bulk-order read model");
        System.out.println("  GET  /view/dashboard          - Dashboard summary");
        System.out.println("  GET  /view/stream             - SSE stream for real-time updates");
        System.out.println("  GET  /view/ui                 - Live dashboard UI");
        System.out.println("--- Auth Endpoints ---");
        System.out.println("  POST /auth/login              - Login (username + password → token)");
        System.out.println("  GET  /auth/me                 - Get current user from token");
        System.out.println("  POST /auth/logout             - Invalidate token");
        System.out.println("--- User Frontend Endpoints ---");
        System.out.println("  GET  /accounts                - List investor accounts");
        System.out.println("  POST /auth/login              - Investor login");
        System.out.println("--- Advisor Endpoints ---");
        System.out.println("  GET  /advisor/me              - Advisor identity (X-Advisor-ID header)");
        System.out.println("  GET  /advisor/clients         - Advisor's client list with aggregates");
        System.out.println("  GET  /advisor/orders          - All orders for advisor's clients");
        System.out.println("  POST /advisor/orders/plan     - Plan orders on behalf of clients");
        System.out.println("  GET  /advisor/dashboard       - Advisor book summary");
        System.out.println("Read model backend: " + projectionStore.getStatus());
    }

    private static void seedAccountsIfMissing(AccountRepository accountRepository) {
        if (accountRepository.findAll().isEmpty()) {
            AccountMockDataUtil.insertMockAccounts(accountRepository);
        }
    }

    private static void seedFundsIfMissing(FundRepository fundRepository) {
        if (fundRepository.findAll().isEmpty()) {
            FundMockDataUtil.insertMockFunds(fundRepository);
        }
    }

    private static boolean isEnabled(String envVarName) {
        String value = System.getenv(envVarName);
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    private static ProjectionStore createProjectionStore() {
        if (isEnabled(CQRS_USE_MONGO_ENV_VAR)) {
            String mongoUri = getOrDefault(CQRS_MONGO_URI_ENV_VAR, "mongodb://localhost:27017");
            String mongoDb = getOrDefault(CQRS_MONGO_DB_ENV_VAR, "oms");
            return new MongoDbProjectionStore(mongoUri, mongoDb);
        }
        return new InMemoryProjectionStore();
    }

    private static String getOrDefault(String envVarName, String defaultValue) {
        String value = System.getenv(envVarName);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /**
     * Replay all existing orders/bulk-orders from PostgreSQL into the in-memory
     * projection store.
     * This ensures the read model is populated after a server restart without a
     * clean-start wipe.
     * Only runs for InMemoryProjectionStore (MongoDB persists its own state).
     */
    private static void replayProjections(
            ProjectionStore projectionStore,
            OrderRepository orderRepository,
            BulkOrderRepository bulkOrderRepository,
            com.iiit.oms.repository.postgres.PostgresBulkOrderMappingRepository bulkOrderMappingRepository,
            FundRepository fundRepository) {
        if (!(projectionStore instanceof InMemoryProjectionStore)) {
            return; // MongoDB stores its own state
        }
        System.out.println("Replaying projections from DB into in-memory store...");
        int orderCount = 0;
        for (com.iiit.oms.model.Order order : orderRepository.findAll()) {
            try {
                java.util.Optional<com.iiit.oms.model.Fund> maybeFund = fundRepository
                        .findByFundId(order.getProductID());
                if (maybeFund.isEmpty())
                    continue;
                com.iiit.oms.model.Fund fund = maybeFund.get();
                String bulkOrderID = null;
                com.iiit.oms.model.BulkOrder bulkOrder = null;
                if (order.getOrderStatus().name().matches("BULKED|TRANSMITTED|CONFIRMED|CONTRACTED|BOOKED")) {
                    for (com.iiit.oms.model.BulkOrder bo : bulkOrderRepository.findAll()) {
                        java.util.List<String> ids = bulkOrderMappingRepository
                                .findIndividualOrderIds(bo.getOrderID()).orElse(java.util.List.of());
                        if (ids.contains(order.getOrderID())) {
                            bulkOrderID = bo.getOrderID();
                            bulkOrder = bo;
                            break;
                        }
                    }
                }
                projectionStore.projectOrder(order, bulkOrder, fund);
                orderCount++;
            } catch (Exception e) {
                System.err.println("Failed to replay order " + order.getOrderID() + ": " + e.getMessage());
            }
        }
        int bulkCount = 0;
        for (com.iiit.oms.model.BulkOrder bo : bulkOrderRepository.findAll()) {
            try {
                java.util.Optional<com.iiit.oms.model.Fund> maybeFund = fundRepository.findByFundId(bo.getProductID());
                if (maybeFund.isEmpty())
                    continue;
                java.util.List<String> ids = bulkOrderMappingRepository
                        .findIndividualOrderIds(bo.getOrderID()).orElse(java.util.List.of());
                projectionStore.projectBulkOrder(bo, maybeFund.get(), ids);
                bulkCount++;
            } catch (Exception e) {
                System.err.println("Failed to replay bulk order " + bo.getOrderID() + ": " + e.getMessage());
            }
        }
        System.out.println("Projection replay complete: " + orderCount + " orders, " + bulkCount + " bulk orders.");
    }
}
