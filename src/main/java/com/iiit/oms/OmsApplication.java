package com.iiit.oms;

import com.iiit.oms.db.postgres.PostgresAccountDatabase;
import com.iiit.oms.db.postgres.PostgresAdvisorClientRelationshipDatabase;
import com.iiit.oms.db.postgres.PostgresAdvisorDatabase;
import com.iiit.oms.db.postgres.PostgresBulkOrderDatabase;
import com.iiit.oms.db.postgres.PostgresBulkOrderMappingDatabase;
import com.iiit.oms.db.postgres.PostgresFundDatabase;
import com.iiit.oms.db.postgres.PostgresOrderDatabase;
import com.iiit.oms.db.util.PostgresConnectionFactory;
import com.iiit.oms.db.util.PostgresSchemaInitializer;
import com.iiit.oms.interfaces.OrderRestServer;
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
import com.iiit.oms.repository.BulkOrderRepository;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.repository.OrderRepository;
import com.iiit.oms.repository.UserRepository;
import com.iiit.oms.repository.inmemory.InMemoryUserRepository;
import com.iiit.oms.repository.postgres.PostgresAccountRepository;
import com.iiit.oms.repository.postgres.PostgresAdvisorClientRelationshipRepository;
import com.iiit.oms.repository.postgres.PostgresAdvisorRepository;
import com.iiit.oms.repository.postgres.PostgresBulkOrderMappingRepository;
import com.iiit.oms.repository.postgres.PostgresBulkOrderRepository;
import com.iiit.oms.repository.postgres.PostgresFundRepository;
import com.iiit.oms.repository.postgres.PostgresOrderRepository;
import com.iiit.oms.util.AccountMockDataUtil;
import com.iiit.oms.util.AdvisorMockDataUtil;
import com.iiit.oms.util.FundMockDataUtil;
import com.iiit.oms.util.UserSeedDataUtil;

import java.io.IOException;

public class OmsApplication {
    private static final int SERVER_PORT = 8080;
    private static final String CLEAN_START_ENV_VAR = "OMS_DB_CLEAN_START";
    private static final String CQRS_USE_MONGO_ENV_VAR = "OMS_CQRS_USE_MONGO";
    private static final String CQRS_MONGO_URI_ENV_VAR = "OMS_MONGO_URI";
    private static final String CQRS_MONGO_DB_ENV_VAR = "OMS_MONGO_DB";

    public static void main(String[] args) throws IOException {
        PostgresConnectionFactory connectionFactory = PostgresConnectionFactory.fromEnvironment();
        boolean cleanStart = isEnabled(CLEAN_START_ENV_VAR);
        PostgresSchemaInitializer.initialize(connectionFactory, cleanStart);

        AccountRepository accountRepository = new PostgresAccountRepository(new PostgresAccountDatabase(connectionFactory));
        seedAccountsIfMissing(accountRepository);

        FundRepository fundRepository = new PostgresFundRepository(new PostgresFundDatabase(connectionFactory));
        seedFundsIfMissing(fundRepository);

        AdvisorRepository advisorRepository = new PostgresAdvisorRepository(new PostgresAdvisorDatabase(connectionFactory));
        AdvisorClientRelationshipRepository relationshipRepository =
                new PostgresAdvisorClientRelationshipRepository(new PostgresAdvisorClientRelationshipDatabase(connectionFactory));
        AdvisorMockDataUtil.seedIfMissing(advisorRepository, relationshipRepository);

        UserRepository userRepository = new InMemoryUserRepository();
        UserSeedDataUtil.seedIfMissing(userRepository);

        OrderRepository orderRepository = new PostgresOrderRepository(new PostgresOrderDatabase(connectionFactory));
        OrderStateMachine orderStateMachine = new OrderStateMachine(new OrderManager(accountRepository, fundRepository));
        OrderScheduler orderScheduler = new OrderScheduler(orderRepository, orderStateMachine);

        ProjectionStore projectionStore = createProjectionStore();
        OrderProjectionListener projectionListener = new DefaultOrderProjectionListener(projectionStore);

        PostgresBulkOrderMappingRepository bulkOrderMappingRepository = new PostgresBulkOrderMappingRepository(new PostgresBulkOrderMappingDatabase(connectionFactory));
        BulkOrderRepository bulkOrderRepository = new PostgresBulkOrderRepository(new PostgresBulkOrderDatabase(connectionFactory));
        BatchoutScheduler batchoutScheduler = new BatchoutScheduler(orderRepository, bulkOrderMappingRepository, bulkOrderRepository, fundRepository, projectionListener);

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
            userRepository
        );

        orderRestServer.start();
        orderScheduler.startPolling();
        batchoutScheduler.startBatchout();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            orderScheduler.shutdown();
            batchoutScheduler.shutdown();
            orderRestServer.stop(0);
            if (projectionStore instanceof MongoDbProjectionStore) {
                ((MongoDbProjectionStore) projectionStore).close();
            }
        }));

        System.out.println("OmsApplication started on http://localhost:" + SERVER_PORT);
        System.out.println("OrderScheduler started with 30 second interval");
        System.out.println("BatchoutScheduler started with 120 second interval");
        System.out.println("Postgres DB bootstrap complete (set OMS_DB_URL/OMS_DB_USER/OMS_DB_PASSWORD to override defaults)");
        System.out.println("Postgres clean start: " + cleanStart + " (set " + CLEAN_START_ENV_VAR + "=true to wipe tables on startup)");
        System.out.println("Seeded default accounts count: " + accountRepository.findAll().size());
        System.out.println("Seeded default funds count: " + fundRepository.findAll().size());
        System.out.println("Seeded advisors count: " + advisorRepository.findAll().size());
        System.out.println("Seeded users count:    " + userRepository.findAll().size());
        System.out.println("--- OMS Endpoints ---");
        System.out.println("  POST /orders/plan             - Plan new orders");
        System.out.println("  GET  /orders                  - List all orders");
        System.out.println("  GET  /orders/status?orderID=  - Get status by order ID");
        System.out.println("  POST /orders/confirm          - Confirm BULKED bulk orders");
        System.out.println("  POST /orders/book             - Book CONFIRMED bulk orders");
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
}
