package com.iiit.oms.interfaces;

import com.iiit.oms.db.inmemory.InMemoryOrderDatabase;
import com.iiit.oms.db.inmemory.InMemoryAccountDatabase;
import com.iiit.oms.db.inmemory.InMemoryBulkOrderDatabase;
import com.iiit.oms.db.inmemory.InMemoryBulkOrderMappingDatabase;
import com.iiit.oms.db.inmemory.InMemoryFundDatabase;
import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.model.BulkOrderStatus;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.model.OrderStatus;
import com.iiit.oms.processor.OrderManager;
import com.iiit.oms.processor.OrderStateMachine;
import com.iiit.oms.repository.BulkOrderMappingRepository;
import com.iiit.oms.repository.BulkOrderRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderRestServerTest {

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
            Order order = new Order("ORD300", "FND001", java.math.BigDecimal.TEN, java.math.BigDecimal.valueOf(100), "ACCT00001", OrderSide.BUY, OrderStatus.ERRORED, false);
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
            Order order = new Order("ORD301", "FND001", java.math.BigDecimal.ONE, java.math.BigDecimal.valueOf(100), "ACCT00001", OrderSide.BUY, OrderStatus.BULKED, false);
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
                        new InMemoryFundRepository(new InMemoryFundDatabase())
                )
        );

        OrderRestServer server = new OrderRestServer(0, orderRepository, bulkOrderRepository, mappingRepository, stateMachine);
        server.start();

        try {
            Order order1 = new Order("ORD500", "FND001", java.math.BigDecimal.ONE, java.math.BigDecimal.TEN, "ACCT00001", OrderSide.BUY, OrderStatus.BULKED, true);
            Order order2 = new Order("ORD501", "FND001", java.math.BigDecimal.ONE, java.math.BigDecimal.TEN, "ACCT00002", OrderSide.BUY, OrderStatus.BULKED, true);
            orderRepository.save(order1);
            orderRepository.save(order2);

            BulkOrder bulkOrder = new BulkOrder(
                    "BLK500",
                    "FND001",
                    OrderSide.BUY,
                    BulkOrderStatus.BULKED,
                    java.math.BigDecimal.valueOf(2),
                    java.math.BigDecimal.valueOf(20),
                    "FIRMACCT"
            );
            bulkOrderRepository.save(bulkOrder);
            mappingRepository.save("BLK500", java.util.List.of("ORD500", "ORD501"));

            HttpResponse<String> response = postConfirm(server.getPort());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"confirmedBulkOrders\":1"));
            assertTrue(response.body().contains("\"confirmedIndividualOrders\":2"));

            assertEquals(OrderStatus.CONFIRMED, orderRepository.findByOrderId("ORD500").orElseThrow().getOrderStatus());
            assertEquals(OrderStatus.CONFIRMED, orderRepository.findByOrderId("ORD501").orElseThrow().getOrderStatus());
            assertEquals(BulkOrderStatus.CONFIRMED, bulkOrderRepository.findByOrderId("BLK500").orElseThrow().getBulkOrderStatus());
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

    private HttpResponse<String> postJson(int port, String payload) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders/plan"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getOrders(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders"))
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getOrderStatus(int port, String orderID) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders/status?orderID=" + orderID))
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getOrderStatusWithoutOrderId(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders/status"))
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postConfirm(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders/confirm"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getConfirm(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/orders/confirm"))
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
