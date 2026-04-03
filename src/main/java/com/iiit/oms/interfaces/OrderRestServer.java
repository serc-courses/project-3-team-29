package com.iiit.oms.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.model.BulkOrderStatus;
import com.iiit.oms.model.Fund;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.model.OrderStatus;
import com.iiit.oms.processor.OrderStateMachine;
import com.iiit.oms.repository.BulkOrderMappingRepository;
import com.iiit.oms.repository.BulkOrderRepository;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.repository.OrderRepository;
import com.iiit.oms.util.UniqueIdGenerator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OrderRestServer {
    private static final Logger LOGGER = Logger.getLogger(OrderRestServer.class.getName());
    private static final String PLAN_ORDERS_PATH = "/orders/plan";
    private static final String LIST_ORDERS_PATH = "/orders";
    private static final String ORDER_STATUS_PATH = "/orders/status";
    private static final String CONFIRM_ORDERS_PATH = "/orders/confirm";
    private static final String BOOK_ORDERS_PATH = "/orders/book";

    private final HttpServer httpServer;
    private final OrderRepository orderRepository;
    private final BulkOrderRepository bulkOrderRepository;
    private final BulkOrderMappingRepository bulkOrderMappingRepository;
    private final FundRepository fundRepository;
    private final OrderStateMachine orderStateMachine;
    private final ObjectMapper objectMapper;

    public OrderRestServer(int port, OrderRepository orderRepository) throws IOException {
        this(port, orderRepository, null, null, null, null);
    }

    public OrderRestServer(int port,
                           OrderRepository orderRepository,
                           BulkOrderRepository bulkOrderRepository,
                           BulkOrderMappingRepository bulkOrderMappingRepository,
                           OrderStateMachine orderStateMachine) throws IOException {
        this(port, orderRepository, bulkOrderRepository, bulkOrderMappingRepository, null, orderStateMachine);
    }

    public OrderRestServer(int port,
                           OrderRepository orderRepository,
                           BulkOrderRepository bulkOrderRepository,
                           BulkOrderMappingRepository bulkOrderMappingRepository,
                           FundRepository fundRepository,
                           OrderStateMachine orderStateMachine) throws IOException {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.bulkOrderRepository = bulkOrderRepository;
        this.bulkOrderMappingRepository = bulkOrderMappingRepository;
        this.fundRepository = fundRepository;
        this.orderStateMachine = orderStateMachine;
        this.objectMapper = new ObjectMapper();
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.httpServer.createContext(PLAN_ORDERS_PATH, new PlanOrdersHandler());
        this.httpServer.createContext(LIST_ORDERS_PATH, new ListOrdersHandler());
        this.httpServer.createContext(ORDER_STATUS_PATH, new OrderStatusHandler());
        this.httpServer.createContext(CONFIRM_ORDERS_PATH, new ConfirmOrdersHandler());
        this.httpServer.createContext(BOOK_ORDERS_PATH, new BookOrdersHandler());
        this.httpServer.setExecutor(Executors.newFixedThreadPool(4));
    }

    public void start() {
        httpServer.start();
    }

    public void stop(int delaySeconds) {
        httpServer.stop(delaySeconds);
    }

    public int getPort() {
        return httpServer.getAddress().getPort();
    }

    private final class PlanOrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeResponse(exchange, 405, "{\"message\":\"Only POST is supported\"}");
                return;
            }

            try {
                List<Order> orders = parseOrders(exchange.getRequestBody());
                LOGGER.info("Received " + orders.size() + " orders at endpoint: " + PLAN_ORDERS_PATH);
                List<String> assignedOrderIds = new ArrayList<>();
                for (Order order : orders) {
                    // Assign orderID if not present
                    if (order.getOrderID() == null || order.getOrderID().trim().isEmpty()) {
                        String uniqueId = UniqueIdGenerator.generate("ORD");
                        order.setOrderID(uniqueId);
                        LOGGER.info("Assigned unique ID " + uniqueId + " to order");
                    }
                    assignedOrderIds.add(order.getOrderID());
                    
                    if (order.getOrderSide() == null) {
                        order.setOrderSide(OrderSide.BUY);
                    }
                    if (order.getOrderStatus() == null) {
                        order.setOrderStatus(OrderStatus.PLANNED);
                    }
                    LOGGER.info("Processing received order: " + order.getOrderID());
                    orderRepository.save(order);
                }

                LOGGER.info("Successfully planned " + orders.size() + " orders");
                Map<String, Object> response = Map.of(
                    "message", "Orders planned",
                    "count", orders.size(),
                    "orderIDs", assignedOrderIds
                );
                writeResponse(exchange, 201, objectMapper.writeValueAsString(response));
            } catch (JsonProcessingException ex) {
                writeResponse(exchange, 400, "{\"message\":\"Invalid order payload\"}");
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                LOGGER.severe("Failed to plan orders: " + ex.getMessage());
                writeResponse(exchange, 500, "{\"message\":\"Failed to plan orders\"}");
            }
        }

        private List<Order> parseOrders(InputStream bodyStream) throws IOException {
            return objectMapper.readValue(bodyStream, new TypeReference<List<Order>>() {
            });
        }

        private void writeResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }

    private final class ListOrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeResponse(exchange, 405, "{\"message\":\"Only GET is supported\"}");
                return;
            }

            try {
                List<Order> orders = orderRepository.findAll();
                LOGGER.info("Listing " + orders.size() + " orders from repository");
                String jsonResponse = objectMapper.writeValueAsString(orders);
                writeResponse(exchange, 200, jsonResponse);
            } catch (RuntimeException ex) {
                LOGGER.severe("Failed to retrieve orders: " + ex.getMessage());
                writeResponse(exchange, 500, "{\"message\":\"Failed to retrieve orders\"}");
            }
        }

        private void writeResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }

    private final class OrderStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeResponse(exchange, 405, "{\"message\":\"Only GET is supported\"}");
                return;
            }

            String orderID = getQueryParam(exchange.getRequestURI().getQuery(), "orderID");
            if (orderID == null || orderID.isBlank()) {
                writeResponse(exchange, 400, "{\"message\":\"orderID query parameter is required\"}");
                return;
            }

            try {
                Optional<Order> order = orderRepository.findByOrderId(orderID);
                if (order.isEmpty()) {
                    writeResponse(exchange, 404, "{\"message\":\"Order not found\",\"orderID\":\"" + orderID + "\"}");
                    return;
                }

                Map<String, Object> statusResponse = Map.of(
                        "orderID", order.get().getOrderID(),
                        "orderStatus", order.get().getOrderStatus(),
                        "errorDescription", order.get().getErrorDescription() == null ? "" : order.get().getErrorDescription()
                );
                writeResponse(exchange, 200, objectMapper.writeValueAsString(statusResponse));
            } catch (RuntimeException ex) {
                LOGGER.severe("Failed to retrieve order status for " + orderID + ": " + ex.getMessage());
                writeResponse(exchange, 500, "{\"message\":\"Failed to retrieve order status\"}");
            }
        }

        private String getQueryParam(String query, String key) {
            if (query == null || query.isBlank()) {
                return null;
            }
            String prefix = key + "=";
            for (String pair : query.split("&")) {
                if (pair.startsWith(prefix)) {
                    return pair.substring(prefix.length());
                }
            }
            return null;
        }

        private void writeResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }

    private final class ConfirmOrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeResponse(exchange, 405, "{\"message\":\"Only POST is supported\"}");
                return;
            }

            if (bulkOrderRepository == null || bulkOrderMappingRepository == null || orderStateMachine == null) {
                writeResponse(exchange, 500, "{\"message\":\"Confirm endpoint is not configured\"}");
                return;
            }

            try {
                List<BulkOrder> bulkedOrders = bulkOrderRepository.findAll().stream()
                        .filter(bulkOrder -> bulkOrder.getBulkOrderStatus() == BulkOrderStatus.BULKED)
                        .collect(Collectors.toList());

                int confirmedBulkOrders = 0;
                int confirmedIndividualOrders = 0;
                int missingIndividualOrders = 0;

                for (BulkOrder bulkOrder : bulkedOrders) {
                    List<String> individualOrderIds = bulkOrderMappingRepository
                            .findIndividualOrderIds(bulkOrder.getOrderID())
                            .orElse(List.of());

                    for (String individualOrderId : individualOrderIds) {
                        Optional<Order> maybeOrder = orderRepository.findByOrderId(individualOrderId);
                        if (maybeOrder.isEmpty()) {
                            missingIndividualOrders++;
                            continue;
                        }

                        Order order = maybeOrder.get();
                        if (order.getOrderStatus() != OrderStatus.BULKED) {
                            order.setOrderStatus(OrderStatus.BULKED);
                        }

                        Order advancedOrder = orderStateMachine.process(order);
                        orderRepository.save(advancedOrder);
                        if (advancedOrder.getOrderStatus() == OrderStatus.CONFIRMED) {
                            confirmedIndividualOrders++;
                        }
                    }

                    bulkOrder.setBulkOrderStatus(BulkOrderStatus.CONFIRMED);
                    bulkOrderRepository.save(bulkOrder);
                    confirmedBulkOrders++;
                }

                Map<String, Object> response = Map.of(
                        "message", "Bulk confirmation completed",
                        "confirmedBulkOrders", confirmedBulkOrders,
                        "confirmedIndividualOrders", confirmedIndividualOrders,
                        "missingIndividualOrders", missingIndividualOrders
                );
                writeResponse(exchange, 200, objectMapper.writeValueAsString(response));
            } catch (RuntimeException ex) {
                LOGGER.severe("Failed to confirm bulk orders: " + ex.getMessage());
                writeResponse(exchange, 500, "{\"message\":\"Failed to confirm bulk orders\"}");
            }
        }

        private void writeResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }

    private final class BookOrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeResponse(exchange, 405, "{\"message\":\"Only POST is supported\"}");
                return;
            }

            if (bulkOrderRepository == null || bulkOrderMappingRepository == null || fundRepository == null || orderStateMachine == null) {
                writeResponse(exchange, 500, "{\"message\":\"Book endpoint is not configured\"}");
                return;
            }

            try {
                List<BulkOrder> confirmedBulkOrders = bulkOrderRepository.findAll().stream()
                        .filter(bulkOrder -> bulkOrder.getBulkOrderStatus() == BulkOrderStatus.CONFIRMED)
                        .collect(Collectors.toList());

                int bookedBulkOrders = 0;
                int bookedIndividualOrders = 0;
                int missingFunds = 0;
                int missingIndividualOrders = 0;

                for (BulkOrder bulkOrder : confirmedBulkOrders) {
                    Optional<Fund> maybeFund = fundRepository.findByFundId(bulkOrder.getProductID());
                    if (maybeFund.isEmpty()) {
                        missingFunds++;
                        continue;
                    }

                    BigDecimal nav = maybeFund.get().getNAV();
                    if (nav == null || nav.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalStateException("Invalid NAV for fund " + bulkOrder.getProductID() + ": " + nav);
                    }

                    bulkOrder.setQuantity(calculateQuantity(bulkOrder.getAmount(), nav));
                    bulkOrder.setBulkOrderStatus(BulkOrderStatus.BOOKED);
                    bulkOrderRepository.save(bulkOrder);
                    bookedBulkOrders++;

                    List<String> individualOrderIds = bulkOrderMappingRepository
                            .findIndividualOrderIds(bulkOrder.getOrderID())
                            .orElse(List.of());

                    for (String individualOrderId : individualOrderIds) {
                        Optional<Order> maybeOrder = orderRepository.findByOrderId(individualOrderId);
                        if (maybeOrder.isEmpty()) {
                            missingIndividualOrders++;
                            continue;
                        }

                        Order order = maybeOrder.get();
                        order.setQuantity(calculateQuantity(order.getAmount(), nav));

                        Order advancedOrder = orderStateMachine.processBooking(order);
                        orderRepository.save(advancedOrder);
                        if (advancedOrder.getOrderStatus() == OrderStatus.BOOKED) {
                            bookedIndividualOrders++;
                        }
                    }
                }

                Map<String, Object> response = Map.of(
                        "message", "Bulk booking completed",
                        "bookedBulkOrders", bookedBulkOrders,
                        "bookedIndividualOrders", bookedIndividualOrders,
                        "missingFunds", missingFunds,
                        "missingIndividualOrders", missingIndividualOrders
                );
                writeResponse(exchange, 200, objectMapper.writeValueAsString(response));
            } catch (RuntimeException ex) {
                LOGGER.severe("Failed to book bulk orders: " + ex.getMessage());
                writeResponse(exchange, 500, "{\"message\":\"Failed to book bulk orders\"}");
            }
        }

        private BigDecimal calculateQuantity(BigDecimal amount, BigDecimal nav) {
            if (amount == null) {
                throw new IllegalStateException("Amount is required for quantity calculation");
            }
            return amount.divide(nav, 8, RoundingMode.HALF_UP);
        }

        private void writeResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }
}
