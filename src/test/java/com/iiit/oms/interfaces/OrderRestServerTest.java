package com.iiit.oms.interfaces;

import com.iiit.oms.db.inmemory.InMemoryOrderDatabase;
import com.iiit.oms.db.inmemory.InMemoryAccountDatabase;
import com.iiit.oms.db.inmemory.InMemoryBulkOrderDatabase;
import com.iiit.oms.db.inmemory.InMemoryBulkOrderMappingDatabase;
import com.iiit.oms.db.inmemory.InMemoryFundDatabase;
import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.model.BulkOrderStatus;
import com.iiit.oms.model.Fund;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.model.OrderStatus;
import com.iiit.oms.processor.OrderManager;
import com.iiit.oms.processor.OrderStateMachine;
import com.iiit.oms.readmodel.OrderProjectionListener;
import com.iiit.oms.readmodel.ProjectionStore;
import com.iiit.oms.readmodel.impl.DefaultOrderProjectionListener;
import com.iiit.oms.readmodel.impl.InMemoryProjectionStore;
import com.iiit.oms.repository.BulkOrderMappingRepository;
import com.iiit.oms.repository.BulkOrderRepository;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.repository.OrderRepository;
import com.iiit.oms.repository.inmemory.InMemoryAccountRepository;
import com.iiit.oms.repository.inmemory.InMemoryBulkOrderMappingRepository;
import com.iiit.oms.repository.inmemory.InMemoryBulkOrderRepository;
import com.iiit.oms.repository.inmemory.InMemoryFundRepository;
import com.iiit.oms.repository.inmemory.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderRestServerTest {

    static {
        // Force plain HTTP in tests — keystore path set to non-existent so createServer falls back
        System.setProperty("OMS_KEYSTORE_PATH", "nonexistent-for-test");
    }

    // Admin JWT pre-generated so all test requests pass RBAC
    private static final String ADMIN_TOKEN = buildAdminToken();

    private static String buildAdminToken() {
        com.iiit.oms.model.User admin = new com.iiit.oms.model.User();
        admin.setUserID("test-admin");
        admin.setUsername("test-admin");
        admin.setRole("ADMIN");
        return new com.iiit.oms.auth.JwtService(new com.iiit.oms.auth.InMemoryRevocationStore())
                .generateToken(admin);
    }

    private static HttpRequest auth(HttpRequest.Builder b) {
        return b.header("Authorization", "Bearer " + ADMIN_TOKEN).build();
    }

    @Test
    void shouldAcceptOrderListAndStoreOrders() throws Exception {
        InMemoryOrderDatabase database = new InMemoryOrderDatabase();
        OrderRepository repository = new InMemoryOrderRepository(database);
        OrderRestServer server = new OrderRestServer(0, repository);
        server.start();

        try {
            String payload = "["
                    + "{\"orderID\":\"ORD100\",\"productID\":\"FND001\",\"quantity\":10,\"amount\":1000,\"accountID\":\"ACCT00001\",\"orderSide\":\"BUY\"},"
                    + "{\"orderID\":\"ORD101\",\"productID\":\"FND002\",\"quantity\":5,\"amount\":600,\"accountID\":\"ACCT00002\",\"orderSide\":\"SELL\"}"
                    + "]";

            HttpResponse<String> response = postJson(server.getPort(), payload);
            List<com.iiit.oms.model.Order> storedOrders = repository.findAll();

            assertEquals(201, response.statusCode());
            assertEquals(2, storedOrders.size());
            assertTrue(storedOrders.stream().allMatch(order -> !order.isProcessed()));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        InMemoryOrderDatabase database = new InMemoryOrderDatabase();
        OrderRepository repository = new InMemoryOrderRepository(database);
        OrderRestServer server = new OrderRestServer(0, repository);
        server.start();

        try {
            HttpResponse<String> response = postJson(server.getPort(), "{\"bad\":\"payload\"}");

            assertEquals(400, response.statusCode());
            assertEquals(0, repository.findAll().size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldListAllOrdersViaGetEndpoint() throws Exception {
        InMemoryOrderDatabase database = new InMemoryOrderDatabase();
        OrderRepository repository = new InMemoryOrderRepository(database);
        OrderRestServer server = new OrderRestServer(0, repository);
        server.start();

        try {
            String payload = "["
                    + "{\"orderID\":\"ORD200\",\"productID\":\"FND001\",\"quantity\":10,\"amount\":1000,\"accountID\":\"ACCT00001\",\"orderSide\":\"BUY\"},"
                    + "{\"orderID\":\"ORD201\",\"productID\":\"FND002\",\"quantity\":5,\"amount\":600,\"accountID\":\"ACCT00002\",\"orderSide\":\"SELL\"}"
                    + "]";
            postJson(server.getPort(), payload);

            HttpResponse<String> getResponse = getOrders(server.getPort());

            assertEquals(200, getResponse.statusCode());
            assertTrue(getResponse.body().contains("ORD200"));
            assertTrue(getResponse.body().contains("ORD201"));
            assertTrue(getResponse.body().contains("\"orderSide\":\"BUY\""));
            assertTrue(getResponse.body().contains("\"orderSide\":\"SELL\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnOrderStatusByOrderId() throws Exception {
        InMemoryOrderDatabase database = new InMemoryOrderDatabase();
        OrderRepository repository = new InMemoryOrderRepository(database);
        OrderRestServer server = new OrderRestServer(0, repository);
        server.start();

        try {
            Order order = new Order("ORD300", "FND001", java.math.BigDecimal.TEN, java.math.BigDecimal.valueOf(100),
                    "ACCT00001", OrderSide.BUY, OrderStatus.ERRORED, false);
            order.setErrorDescription("Account ID ACCT99999 does not exist");
            repository.save(order);

            HttpResponse<String> response = getOrderStatus(server.getPort(), "ORD300");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"orderID\":\"ORD300\""));
            assertTrue(response.body().contains("\"orderStatus\":\"ERRORED\""));
            assertTrue(response.body().contains("\"errorDescription\":\"Account ID ACCT99999 does not exist\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnBulkedStatusByOrderId() throws Exception {
        InMemoryOrderDatabase database = new InMemoryOrderDatabase();
        OrderRepository repository = new InMemoryOrderRepository(database);
        OrderRestServer server = new OrderRestServer(0, repository);
        server.start();

        try {
            Order order = new Order("ORD301", "FND001", java.math.BigDecimal.ONE, java.math.BigDecimal.valueOf(100),
                    "ACCT00001", OrderSide.BUY, OrderStatus.BULKED, false);
            repository.save(order);

            HttpResponse<String> response = getOrderStatus(server.getPort(), "ORD301");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"orderID\":\"ORD301\""));
            assertTrue(response.body().contains("\"orderStatus\":\"BULKED\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnBadRequestWhenOrderIdIsMissingForStatusEndpoint() throws Exception {
        InMemoryOrderDatabase database = new InMemoryOrderDatabase();
        OrderRepository repository = new InMemoryOrderRepository(database);
        OrderRestServer server = new OrderRestServer(0, repository);
        server.start();

        try {
            HttpResponse<String> response = getOrderStatusWithoutOrderId(server.getPort());

            assertEquals(400, response.statusCode());
            assertTrue(response.body().contains("orderID query parameter is required"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnNotFoundWhenOrderIdDoesNotExistForStatusEndpoint() throws Exception {
        InMemoryOrderDatabase database = new InMemoryOrderDatabase();
        OrderRepository repository = new InMemoryOrderRepository(database);
        OrderRestServer server = new OrderRestServer(0, repository);
        server.start();

        try {
            HttpResponse<String> response = getOrderStatus(server.getPort(), "ORD404");

            assertEquals(404, response.statusCode());
            assertTrue(response.body().contains("Order not found"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldConfirmBulkedOrdersViaConfirmEndpoint() throws Exception {
        InMemoryOrderDatabase orderDatabase = new InMemoryOrderDatabase();
        InMemoryBulkOrderDatabase bulkOrderDatabase = new InMemoryBulkOrderDatabase();
        InMemoryBulkOrderMappingDatabase mappingDatabase = new InMemoryBulkOrderMappingDatabase();
        OrderRepository orderRepository = new InMemoryOrderRepository(orderDatabase);
        BulkOrderRepository bulkOrderRepository = new InMemoryBulkOrderRepository(bulkOrderDatabase);
        BulkOrderMappingRepository mappingRepository = new InMemoryBulkOrderMappingRepository(mappingDatabase);

        OrderStateMachine stateMachine = new OrderStateMachine(
                new OrderManager(
                        new InMemoryAccountRepository(new InMemoryAccountDatabase()),
                        new InMemoryFundRepository(new InMemoryFundDatabase()),
                        orderRepository));

        OrderRestServer server = new OrderRestServer(0, orderRepository, bulkOrderRepository, mappingRepository,
                stateMachine);
        server.start();

        try {
            Order order1 = new Order("ORD500", "FND001", java.math.BigDecimal.ONE, java.math.BigDecimal.TEN,
                    "ACCT00001", OrderSide.BUY, OrderStatus.BULKED, true);
            Order order2 = new Order("ORD501", "FND001", java.math.BigDecimal.ONE, java.math.BigDecimal.TEN,
                    "ACCT00002", OrderSide.BUY, OrderStatus.BULKED, true);
            orderRepository.save(order1);
            orderRepository.save(order2);

            BulkOrder bulkOrder = new BulkOrder(
                    "BLK500",
                    "FND001",
                    OrderSide.BUY,
                    BulkOrderStatus.BULKED,
                    java.math.BigDecimal.valueOf(2),
                    java.math.BigDecimal.valueOf(20),
                    "FIRMACCT");
            bulkOrderRepository.save(bulkOrder);
            mappingRepository.save("BLK500", java.util.List.of("ORD500", "ORD501"));

            HttpResponse<String> response = postConfirm(server.getPort());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"confirmedBulkOrders\":1"));
            assertTrue(response.body().contains("\"confirmedIndividualOrders\":2"));

            assertEquals(OrderStatus.CONFIRMED, orderRepository.findByOrderId("ORD500").orElseThrow().getOrderStatus());
            assertEquals(OrderStatus.CONFIRMED, orderRepository.findByOrderId("ORD501").orElseThrow().getOrderStatus());
            assertEquals(BulkOrderStatus.CONFIRMED,
                    bulkOrderRepository.findByOrderId("BLK500").orElseThrow().getBulkOrderStatus());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRejectGetOnConfirmEndpoint() throws Exception {
        InMemoryOrderDatabase orderDatabase = new InMemoryOrderDatabase();
        OrderRepository orderRepository = new InMemoryOrderRepository(orderDatabase);
        OrderRestServer server = new OrderRestServer(0, orderRepository);
        server.start();

        try {
            HttpResponse<String> response = getConfirm(server.getPort());
            assertEquals(405, response.statusCode());
            assertTrue(response.body().contains("Only POST is supported"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldBookConfirmedBulkOrdersViaBookEndpoint() throws Exception {
        InMemoryOrderDatabase orderDatabase = new InMemoryOrderDatabase();
        InMemoryBulkOrderDatabase bulkOrderDatabase = new InMemoryBulkOrderDatabase();
        InMemoryBulkOrderMappingDatabase mappingDatabase = new InMemoryBulkOrderMappingDatabase();
        InMemoryFundDatabase fundDatabase = new InMemoryFundDatabase();

        OrderRepository orderRepository = new InMemoryOrderRepository(orderDatabase);
        BulkOrderRepository bulkOrderRepository = new InMemoryBulkOrderRepository(bulkOrderDatabase);
        BulkOrderMappingRepository mappingRepository = new InMemoryBulkOrderMappingRepository(mappingDatabase);
        FundRepository fundRepository = new InMemoryFundRepository(fundDatabase);

        fundRepository.save(new Fund("FND001", "Fund 1", "Family", BigDecimal.TEN));

        OrderStateMachine stateMachine = new OrderStateMachine(
                new OrderManager(
                        new InMemoryAccountRepository(new InMemoryAccountDatabase()),
                        fundRepository,
                        orderRepository));

        OrderRestServer server = new OrderRestServer(0, orderRepository, bulkOrderRepository, mappingRepository,
                fundRepository, stateMachine);
        server.start();

        try {
            Order order1 = new Order("ORD700", "FND001", null, BigDecimal.valueOf(5), "ACCT00001", OrderSide.BUY,
                    OrderStatus.CONFIRMED, true);
            Order order2 = new Order("ORD701", "FND001", null, BigDecimal.valueOf(15), "ACCT00002", OrderSide.BUY,
                    OrderStatus.CONFIRMED, true);
            orderRepository.save(order1);
            orderRepository.save(order2);

            BulkOrder bulkOrder = new BulkOrder(
                    "BLK700",
                    "FND001",
                    OrderSide.BUY,
                    BulkOrderStatus.CONFIRMED,
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(20),
                    "FIRMACCT");
            bulkOrderRepository.save(bulkOrder);
            mappingRepository.save("BLK700", java.util.List.of("ORD700", "ORD701"));

            HttpResponse<String> response = postBook(server.getPort());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"bookedBulkOrders\":1"));
            assertTrue(response.body().contains("\"bookedIndividualOrders\":2"));

            BulkOrder savedBulkOrder = bulkOrderRepository.findByOrderId("BLK700").orElseThrow();
            assertEquals(BulkOrderStatus.BOOKED, savedBulkOrder.getBulkOrderStatus());
            assertEquals(0, savedBulkOrder.getQuantity().compareTo(BigDecimal.valueOf(2).setScale(8)));

            Order savedOrder1 = orderRepository.findByOrderId("ORD700").orElseThrow();
            Order savedOrder2 = orderRepository.findByOrderId("ORD701").orElseThrow();
            assertEquals(OrderStatus.BOOKED, savedOrder1.getOrderStatus());
            assertEquals(OrderStatus.BOOKED, savedOrder2.getOrderStatus());
            assertEquals(0, savedOrder1.getQuantity().compareTo(BigDecimal.valueOf(0.5).setScale(8)));
            assertEquals(0, savedOrder2.getQuantity().compareTo(BigDecimal.valueOf(1.5).setScale(8)));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRejectGetOnBookEndpoint() throws Exception {
        InMemoryOrderDatabase orderDatabase = new InMemoryOrderDatabase();
        OrderRepository orderRepository = new InMemoryOrderRepository(orderDatabase);
        OrderRestServer server = new OrderRestServer(0, orderRepository);
        server.start();

        try {
            HttpResponse<String> response = getBook(server.getPort());
            assertEquals(405, response.statusCode());
            assertTrue(response.body().contains("Only POST is supported"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldExposeProjectedOrdersViaReadEndpoint() throws Exception {
        InMemoryOrderDatabase orderDatabase = new InMemoryOrderDatabase();
        InMemoryFundDatabase fundDatabase = new InMemoryFundDatabase();

        OrderRepository orderRepository = new InMemoryOrderRepository(orderDatabase);
        FundRepository fundRepository = new InMemoryFundRepository(fundDatabase);
        fundRepository.save(new Fund("FND001", "Fund 1", "Family", BigDecimal.TEN));

        ProjectionStore projectionStore = new InMemoryProjectionStore();
        OrderProjectionListener projectionListener = new DefaultOrderProjectionListener(projectionStore);

        OrderRestServer server = new OrderRestServer(
                0,
                orderRepository,
                null,
                null,
                fundRepository,
                null,
                projectionStore,
                projectionListener);
        server.start();

        try {
            String payload = "["
                    + "{\"orderID\":\"ORD900\",\"productID\":\"FND001\",\"amount\":1000,\"accountID\":\"ACCT00001\",\"orderSide\":\"BUY\"}"
                    + "]";
            HttpResponse<String> postResponse = postJson(server.getPort(), payload);
            assertEquals(201, postResponse.statusCode());

            HttpResponse<String> readResponse = getViewOrders(server.getPort());
            assertEquals(200, readResponse.statusCode());
            assertTrue(readResponse.body().contains("ORD900"));
            assertTrue(readResponse.body().contains("PLANNED"));
            assertTrue(readResponse.body().contains("FND001"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldExposeDashboardAndUiEndpoints() throws Exception {
        InMemoryOrderDatabase orderDatabase = new InMemoryOrderDatabase();
        InMemoryFundDatabase fundDatabase = new InMemoryFundDatabase();

        OrderRepository orderRepository = new InMemoryOrderRepository(orderDatabase);
        FundRepository fundRepository = new InMemoryFundRepository(fundDatabase);
        fundRepository.save(new Fund("FND001", "Fund 1", "Family", BigDecimal.TEN));

        ProjectionStore projectionStore = new InMemoryProjectionStore();
        OrderProjectionListener projectionListener = new DefaultOrderProjectionListener(projectionStore);

        OrderRestServer server = new OrderRestServer(
                0,
                orderRepository,
                null,
                null,
                fundRepository,
                null,
                projectionStore,
                projectionListener);
        server.start();

        try {
            String payload = "["
                    + "{\"orderID\":\"ORD901\",\"productID\":\"FND001\",\"amount\":500,\"accountID\":\"ACCT00001\",\"orderSide\":\"BUY\"}"
                    + "]";
            HttpResponse<String> postResponse = postJson(server.getPort(), payload);
            assertEquals(201, postResponse.statusCode());

            HttpResponse<String> dashboardResponse = getViewDashboard(server.getPort());
            assertEquals(200, dashboardResponse.statusCode());
            assertTrue(dashboardResponse.body().contains("\"totalOrders\":1"));

            HttpResponse<String> uiResponse = getViewUi(server.getPort());
            assertEquals(200, uiResponse.statusCode());
            assertTrue(uiResponse.body().contains("OMS CQRS Dashboard"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldExposeAccountAndFundAggregates() throws Exception {
        InMemoryOrderDatabase orderDatabase = new InMemoryOrderDatabase();
        InMemoryFundDatabase fundDatabase = new InMemoryFundDatabase();

        OrderRepository orderRepository = new InMemoryOrderRepository(orderDatabase);
        FundRepository fundRepository = new InMemoryFundRepository(fundDatabase);
        fundRepository.save(new Fund("FND001", "Fund 1", "Family", BigDecimal.TEN));
        fundRepository.save(new Fund("FND002", "Fund 2", "Family", BigDecimal.valueOf(20)));

        ProjectionStore projectionStore = new InMemoryProjectionStore();
        OrderProjectionListener projectionListener = new DefaultOrderProjectionListener(projectionStore);

        OrderRestServer server = new OrderRestServer(
                0,
                orderRepository,
                null,
                null,
                fundRepository,
                null,
                projectionStore,
                projectionListener);
        server.start();

        try {
            String payload = "["
                    + "{\"orderID\":\"ORD920\",\"productID\":\"FND001\",\"amount\":100,\"accountID\":\"ACCT00001\",\"orderSide\":\"BUY\"},"
                    + "{\"orderID\":\"ORD921\",\"productID\":\"FND002\",\"amount\":200,\"accountID\":\"ACCT00002\",\"orderSide\":\"SELL\"}"
                    + "]";
            HttpResponse<String> postResponse = postJson(server.getPort(), payload);
            assertEquals(201, postResponse.statusCode());

            HttpResponse<String> accountAgg = getAccountAggregates(server.getPort());
            assertEquals(200, accountAgg.statusCode());
            assertTrue(accountAgg.body().contains("ACCT00001"));
            assertTrue(accountAgg.body().contains("ACCT00002"));

            HttpResponse<String> fundAgg = getFundAggregates(server.getPort());
            assertEquals(200, fundAgg.statusCode());
            assertTrue(fundAgg.body().contains("FND001"));
            assertTrue(fundAgg.body().contains("FND002"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldReplayViewsFromWriteSide() throws Exception {
        InMemoryOrderDatabase orderDatabase = new InMemoryOrderDatabase();
        InMemoryBulkOrderDatabase bulkOrderDatabase = new InMemoryBulkOrderDatabase();
        InMemoryBulkOrderMappingDatabase mappingDatabase = new InMemoryBulkOrderMappingDatabase();
        InMemoryFundDatabase fundDatabase = new InMemoryFundDatabase();

        OrderRepository orderRepository = new InMemoryOrderRepository(orderDatabase);
        BulkOrderRepository bulkOrderRepository = new InMemoryBulkOrderRepository(bulkOrderDatabase);
        BulkOrderMappingRepository mappingRepository = new InMemoryBulkOrderMappingRepository(mappingDatabase);
        FundRepository fundRepository = new InMemoryFundRepository(fundDatabase);
        fundRepository.save(new Fund("FND001", "Fund 1", "Family", BigDecimal.TEN));

        orderRepository.save(new Order("ORD930", "FND001", BigDecimal.ONE, BigDecimal.valueOf(10), "ACCT00001",
                OrderSide.BUY, OrderStatus.BOOKED, true));
        bulkOrderRepository.save(new BulkOrder("BLK930", "FND001", OrderSide.BUY, BulkOrderStatus.BOOKED,
                BigDecimal.ONE, BigDecimal.valueOf(10), "FIRMACCT"));
        mappingRepository.save("BLK930", List.of("ORD930"));

        ProjectionStore projectionStore = new InMemoryProjectionStore();
        OrderProjectionListener projectionListener = new DefaultOrderProjectionListener(projectionStore);

        OrderRestServer server = new OrderRestServer(
                0,
                orderRepository,
                bulkOrderRepository,
                mappingRepository,
                fundRepository,
                null,
                projectionStore,
                projectionListener);
        server.start();

        try {
            HttpResponse<String> replayResponse = postReplay(server.getPort());
            assertEquals(200, replayResponse.statusCode());
            assertTrue(replayResponse.body().contains("\"projectedOrders\":1"));
            assertTrue(replayResponse.body().contains("\"projectedBulkOrders\":1"));

            HttpResponse<String> viewOrders = getViewOrders(server.getPort());
            assertEquals(200, viewOrders.statusCode());
            assertTrue(viewOrders.body().contains("ORD930"));

            HttpResponse<String> viewBulkOrders = getViewBulkOrders(server.getPort());
            assertEquals(200, viewBulkOrders.statusCode());
            assertTrue(viewBulkOrders.body().contains("BLK930"));
        } finally {
            server.stop(0);
        }
    }

    private HttpResponse<String> postJson(int port, String payload) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders/plan"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload)));
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getOrders(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders")).GET());
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getOrderStatus(int port, String orderID) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders/status?orderID=" + orderID)).GET());
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getOrderStatusWithoutOrderId(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders/status")).GET());
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postConfirm(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders/confirm"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}")));
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getConfirm(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders/confirm")).GET());
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postBook(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders/book"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}")));
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getBook(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders/book")).GET());
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getViewOrders(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/view/orders")).GET());
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getViewDashboard(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/view/dashboard")).GET());
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getViewUi(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/view/ui")).GET());
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getViewBulkOrders(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/view/bulk-orders")).GET());
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getAccountAggregates(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/view/aggregates/accounts")).GET());
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getFundAggregates(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/view/aggregates/funds")).GET());
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postReplay(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = auth(HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/view/replay"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}")));
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
