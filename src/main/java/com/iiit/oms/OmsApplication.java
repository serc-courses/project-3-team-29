package com.iiit.oms;

import com.iiit.oms.db.postgres.PostgresAccountDatabase;
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
import com.iiit.oms.repository.AccountRepository;
import com.iiit.oms.repository.BulkOrderRepository;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.repository.OrderRepository;
import com.iiit.oms.repository.postgres.PostgresAccountRepository;
import com.iiit.oms.repository.postgres.PostgresBulkOrderMappingRepository;
import com.iiit.oms.repository.postgres.PostgresBulkOrderRepository;
import com.iiit.oms.repository.postgres.PostgresFundRepository;
import com.iiit.oms.repository.postgres.PostgresOrderRepository;
import com.iiit.oms.util.AccountMockDataUtil;
import com.iiit.oms.util.FundMockDataUtil;

import java.io.IOException;

public class OmsApplication {
    private static final int SERVER_PORT = 8080;
    private static final String CLEAN_START_ENV_VAR = "OMS_DB_CLEAN_START";

    public static void main(String[] args) throws IOException {
        PostgresConnectionFactory connectionFactory = PostgresConnectionFactory.fromEnvironment();
        boolean cleanStart = isEnabled(CLEAN_START_ENV_VAR);
        PostgresSchemaInitializer.initialize(connectionFactory, cleanStart);

        AccountRepository accountRepository = new PostgresAccountRepository(new PostgresAccountDatabase(connectionFactory));
        seedAccountsIfMissing(accountRepository);

        FundRepository fundRepository = new PostgresFundRepository(new PostgresFundDatabase(connectionFactory));
        seedFundsIfMissing(fundRepository);

        OrderRepository orderRepository = new PostgresOrderRepository(new PostgresOrderDatabase(connectionFactory));
        OrderStateMachine orderStateMachine = new OrderStateMachine(new OrderManager(accountRepository, fundRepository));
        OrderScheduler orderScheduler = new OrderScheduler(orderRepository, orderStateMachine);
        PostgresBulkOrderMappingRepository bulkOrderMappingRepository = new PostgresBulkOrderMappingRepository(new PostgresBulkOrderMappingDatabase(connectionFactory));
        BulkOrderRepository bulkOrderRepository = new PostgresBulkOrderRepository(new PostgresBulkOrderDatabase(connectionFactory));
        BatchoutScheduler batchoutScheduler = new BatchoutScheduler(orderRepository, bulkOrderMappingRepository, bulkOrderRepository);
        OrderRestServer orderRestServer = new OrderRestServer(
            SERVER_PORT,
            orderRepository,
            bulkOrderRepository,
            bulkOrderMappingRepository,
            fundRepository,
            orderStateMachine
        );

        orderRestServer.start();
        orderScheduler.startPolling();
        batchoutScheduler.startBatchout();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            orderScheduler.shutdown();
            batchoutScheduler.shutdown();
            orderRestServer.stop(0);
        }));

        System.out.println("OmsApplication started on http://localhost:" + SERVER_PORT);
        System.out.println("OrderScheduler started with 30 second interval");
        System.out.println("BatchoutScheduler started with 120 second interval");
        System.out.println("Postgres DB bootstrap complete (set OMS_DB_URL/OMS_DB_USER/OMS_DB_PASSWORD to override defaults)");
        System.out.println("Postgres clean start: " + cleanStart + " (set " + CLEAN_START_ENV_VAR + "=true to wipe tables on startup)");
        System.out.println("Seeded default accounts count: " + accountRepository.findAll().size());
        System.out.println("Seeded default funds count: " + fundRepository.findAll().size());
        System.out.println("  POST /orders/plan - Plan new orders");
        System.out.println("  GET  /orders - List all orders");
        System.out.println("  GET  /orders/status?orderID=<ID> - Get status by order ID");
        System.out.println("  POST /orders/confirm - Confirm BULKED bulk orders and mapped individual orders");
        System.out.println("  POST /orders/book - Book CONFIRMED bulk orders and mapped individual orders");
        System.out.println("Order flow includes BULKED between PLACED and CONFIRMED");
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
}
