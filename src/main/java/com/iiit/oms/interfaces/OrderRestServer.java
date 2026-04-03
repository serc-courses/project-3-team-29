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
import com.iiit.oms.readmodel.BulkOrderView;
import com.iiit.oms.readmodel.OrderProjectionListener;
import com.iiit.oms.readmodel.OrderView;
import com.iiit.oms.readmodel.ProjectionStore;
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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private static final String VIEW_ORDERS_PATH = "/view/orders";
    private static final String VIEW_BULK_ORDERS_PATH = "/view/bulk-orders";
    private static final String VIEW_DASHBOARD_PATH = "/view/dashboard";
    private static final String VIEW_AGGREGATES_ACCOUNTS_PATH = "/view/aggregates/accounts";
    private static final String VIEW_AGGREGATES_FUNDS_PATH = "/view/aggregates/funds";
    private static final String VIEW_REPLAY_PATH = "/view/replay";
    private static final String VIEW_STREAM_PATH = "/view/stream";
    private static final String VIEW_UI_PATH = "/view/ui";

    private final HttpServer httpServer;
    private final OrderRepository orderRepository;
    private final BulkOrderRepository bulkOrderRepository;
    private final BulkOrderMappingRepository bulkOrderMappingRepository;
    private final FundRepository fundRepository;
    private final OrderStateMachine orderStateMachine;
    private final ProjectionStore projectionStore;
    private final OrderProjectionListener projectionListener;
    private final ObjectMapper objectMapper;
    private final List<OutputStream> sseClients;

    public OrderRestServer(int port, OrderRepository orderRepository) throws IOException {
        this(port, orderRepository, null, null, null, null);
    }

    public OrderRestServer(int port,
                           OrderRepository orderRepository,
                           BulkOrderRepository bulkOrderRepository,
                           BulkOrderMappingRepository bulkOrderMappingRepository,
                           OrderStateMachine orderStateMachine) throws IOException {
        this(port, orderRepository, bulkOrderRepository, bulkOrderMappingRepository, null, orderStateMachine, null, null);
    }

    public OrderRestServer(int port,
                           OrderRepository orderRepository,
                           BulkOrderRepository bulkOrderRepository,
                           BulkOrderMappingRepository bulkOrderMappingRepository,
                           FundRepository fundRepository,
                           OrderStateMachine orderStateMachine) throws IOException {
        this(port, orderRepository, bulkOrderRepository, bulkOrderMappingRepository, fundRepository, orderStateMachine, null, null);
    }

    public OrderRestServer(int port,
                           OrderRepository orderRepository,
                           BulkOrderRepository bulkOrderRepository,
                           BulkOrderMappingRepository bulkOrderMappingRepository,
                           FundRepository fundRepository,
                           OrderStateMachine orderStateMachine,
                           ProjectionStore projectionStore,
                           OrderProjectionListener projectionListener) throws IOException {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.bulkOrderRepository = bulkOrderRepository;
        this.bulkOrderMappingRepository = bulkOrderMappingRepository;
        this.fundRepository = fundRepository;
        this.orderStateMachine = orderStateMachine;
        this.projectionStore = projectionStore;
        this.projectionListener = projectionListener;
        this.objectMapper = new ObjectMapper();
        this.sseClients = new CopyOnWriteArrayList<>();
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.httpServer.createContext(PLAN_ORDERS_PATH, new PlanOrdersHandler());
        this.httpServer.createContext(LIST_ORDERS_PATH, new ListOrdersHandler());
        this.httpServer.createContext(ORDER_STATUS_PATH, new OrderStatusHandler());
        this.httpServer.createContext(CONFIRM_ORDERS_PATH, new ConfirmOrdersHandler());
        this.httpServer.createContext(BOOK_ORDERS_PATH, new BookOrdersHandler());
        this.httpServer.createContext(VIEW_ORDERS_PATH, new ViewOrdersHandler());
        this.httpServer.createContext(VIEW_BULK_ORDERS_PATH, new ViewBulkOrdersHandler());
        this.httpServer.createContext(VIEW_DASHBOARD_PATH, new ViewDashboardHandler());
        this.httpServer.createContext(VIEW_AGGREGATES_ACCOUNTS_PATH, new ViewAggregateAccountsHandler());
        this.httpServer.createContext(VIEW_AGGREGATES_FUNDS_PATH, new ViewAggregateFundsHandler());
        this.httpServer.createContext(VIEW_REPLAY_PATH, new ViewReplayHandler());
        this.httpServer.createContext(VIEW_STREAM_PATH, new ViewStreamHandler());
        this.httpServer.createContext(VIEW_UI_PATH, new ViewUiHandler());
        this.httpServer.setExecutor(Executors.newFixedThreadPool(12));
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

                    Optional<Fund> maybeFund = resolveFund(order.getProductID());
                    if (projectionListener != null && maybeFund.isPresent()) {
                        projectionListener.onOrderPlanned(order, maybeFund.get());
                        publishViewEvent("order-updated", toOrderEventPayload(order, null));
                    }
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

                        Optional<Fund> maybeFund = resolveFund(order.getProductID());
                        if (projectionListener != null && maybeFund.isPresent()) {
                            projectionListener.onOrderStatusChanged(advancedOrder, bulkOrder, maybeFund.get());
                            publishViewEvent("order-updated", toOrderEventPayload(advancedOrder, bulkOrder.getOrderID()));
                        }

                        if (advancedOrder.getOrderStatus() == OrderStatus.CONFIRMED) {
                            confirmedIndividualOrders++;
                        }
                    }

                    bulkOrder.setBulkOrderStatus(BulkOrderStatus.CONFIRMED);
                    bulkOrderRepository.save(bulkOrder);

                    Optional<Fund> maybeFund = resolveFund(bulkOrder.getProductID());
                    if (projectionListener != null && maybeFund.isPresent()) {
                        projectionListener.onBulkOrderStatusChanged(bulkOrder, maybeFund.get(), individualOrderIds);
                        publishViewEvent("bulk-order-updated", toBulkOrderEventPayload(bulkOrder, individualOrderIds));
                    }

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

                        if (projectionListener != null) {
                            projectionListener.onOrderStatusChanged(advancedOrder, bulkOrder, maybeFund.get());
                            publishViewEvent("order-updated", toOrderEventPayload(advancedOrder, bulkOrder.getOrderID()));
                        }

                        if (advancedOrder.getOrderStatus() == OrderStatus.BOOKED) {
                            bookedIndividualOrders++;
                        }
                    }

                    if (projectionListener != null) {
                        projectionListener.onBulkOrderStatusChanged(bulkOrder, maybeFund.get(), individualOrderIds);
                        publishViewEvent("bulk-order-updated", toBulkOrderEventPayload(bulkOrder, individualOrderIds));
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

    private final class ViewOrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            if (projectionStore == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Read model is not configured"));
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String orderID = getQueryParam(query, "orderID");
            String accountID = getQueryParam(query, "accountID");
            String fundID = getQueryParam(query, "fundID");
            String bulkOrderID = getQueryParam(query, "bulkOrderID");

            List<OrderView> response;
            if (orderID != null && !orderID.isBlank()) {
                response = projectionStore.findOrderView(orderID).map(List::of).orElse(List.of());
            } else if (accountID != null && !accountID.isBlank()) {
                response = projectionStore.findOrdersByAccount(accountID);
            } else if (fundID != null && !fundID.isBlank()) {
                response = projectionStore.findOrdersByFund(fundID);
            } else if (bulkOrderID != null && !bulkOrderID.isBlank()) {
                response = projectionStore.findOrdersByBulkOrder(bulkOrderID);
            } else {
                response = projectionStore.findAllOrderViews();
            }

            response = response.stream()
                    .sorted(Comparator.comparing(OrderView::getOrderID))
                    .collect(Collectors.toList());
            sendJsonResponse(exchange, 200, response);
        }
    }

    private final class ViewBulkOrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            if (projectionStore == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Read model is not configured"));
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String bulkOrderID = getQueryParam(query, "bulkOrderID");
            String fundID = getQueryParam(query, "fundID");

            List<BulkOrderView> response;
            if (bulkOrderID != null && !bulkOrderID.isBlank()) {
                response = projectionStore.findBulkOrderView(bulkOrderID).map(List::of).orElse(List.of());
            } else if (fundID != null && !fundID.isBlank()) {
                response = projectionStore.findBulkOrdersByFund(fundID);
            } else {
                response = projectionStore.findAllBulkOrderViews();
            }

            response = response.stream()
                    .sorted(Comparator.comparing(BulkOrderView::getBulkOrderID))
                    .collect(Collectors.toList());
            sendJsonResponse(exchange, 200, response);
        }
    }

    private final class ViewDashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            if (projectionStore == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Read model is not configured"));
                return;
            }

            List<OrderView> orders = projectionStore.findAllOrderViews();
            List<BulkOrderView> bulkOrders = projectionStore.findAllBulkOrderViews();

            Map<String, Long> ordersByStatus = orders.stream().collect(Collectors.groupingBy(OrderView::getOrderStatus, Collectors.counting()));
            Map<String, Long> bulkOrdersByStatus = bulkOrders.stream().collect(Collectors.groupingBy(BulkOrderView::getBulkOrderStatus, Collectors.counting()));

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("totalOrders", orders.size());
            dashboard.put("totalBulkOrders", bulkOrders.size());
            dashboard.put("ordersByStatus", ordersByStatus);
            dashboard.put("bulkOrdersByStatus", bulkOrdersByStatus);
            dashboard.put("orders", orders);
            dashboard.put("bulkOrders", bulkOrders);

            sendJsonResponse(exchange, 200, dashboard);
        }
    }

    private final class ViewAggregateAccountsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            if (projectionStore == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Read model is not configured"));
                return;
            }

            List<OrderView> orders = projectionStore.findAllOrderViews();
            Map<String, List<OrderView>> grouped = orders.stream()
                    .collect(Collectors.groupingBy(OrderView::getAccountID));

            List<Map<String, Object>> response = grouped.entrySet().stream()
                    .map(entry -> {
                        String accountID = entry.getKey();
                        List<OrderView> accountOrders = entry.getValue();
                        BigDecimal totalAmount = accountOrders.stream()
                                .map(OrderView::getAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal totalQuantity = accountOrders.stream()
                                .map(OrderView::getQuantity)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        Map<String, Long> statusCounts = accountOrders.stream()
                                .collect(Collectors.groupingBy(OrderView::getOrderStatus, Collectors.counting()));

                        Map<String, Object> row = new HashMap<>();
                        row.put("accountID", accountID);
                        row.put("orderCount", accountOrders.size());
                        row.put("totalAmount", totalAmount);
                        row.put("totalQuantity", totalQuantity);
                        row.put("statuses", statusCounts);
                        return row;
                    })
                    .sorted(Comparator.comparing(m -> (String) m.get("accountID")))
                    .collect(Collectors.toList());

            sendJsonResponse(exchange, 200, response);
        }
    }

    private final class ViewAggregateFundsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            if (projectionStore == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Read model is not configured"));
                return;
            }

            List<OrderView> orders = projectionStore.findAllOrderViews();
            Map<String, List<OrderView>> grouped = orders.stream()
                    .collect(Collectors.groupingBy(OrderView::getFundID));

            List<Map<String, Object>> response = grouped.entrySet().stream()
                    .map(entry -> {
                        String fundID = entry.getKey();
                        List<OrderView> fundOrders = entry.getValue();
                        BigDecimal totalAmount = fundOrders.stream()
                                .map(OrderView::getAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal totalQuantity = fundOrders.stream()
                                .map(OrderView::getQuantity)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        Map<String, Long> sideCounts = fundOrders.stream()
                                .collect(Collectors.groupingBy(OrderView::getOrderSide, Collectors.counting()));
                        String fundName = fundOrders.stream().map(OrderView::getFundName).filter(Objects::nonNull).findFirst().orElse("");
                        BigDecimal nav = fundOrders.stream().map(OrderView::getNAV).filter(Objects::nonNull).findFirst().orElse(null);

                        Map<String, Object> row = new HashMap<>();
                        row.put("fundID", fundID);
                        row.put("fundName", fundName);
                        row.put("orderCount", fundOrders.size());
                        row.put("totalAmount", totalAmount);
                        row.put("totalQuantity", totalQuantity);
                        row.put("orderSides", sideCounts);
                        row.put("nav", nav);
                        return row;
                    })
                    .sorted(Comparator.comparing(m -> (String) m.get("fundID")))
                    .collect(Collectors.toList());

            sendJsonResponse(exchange, 200, response);
        }
    }

    private final class ViewReplayHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only POST is supported"));
                return;
            }
            if (projectionStore == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Read model is not configured"));
                return;
            }
            if (bulkOrderRepository == null || bulkOrderMappingRepository == null || fundRepository == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Replay endpoint is not configured"));
                return;
            }

            int projectedOrders = 0;
            int projectedBulkOrders = 0;
            int missingOrderFunds = 0;
            int missingBulkFunds = 0;

            projectionStore.clearAll();

            Map<String, List<String>> mappings = bulkOrderMappingRepository.findAll();
            Map<String, String> orderToBulkId = new HashMap<>();
            for (Map.Entry<String, List<String>> mapping : mappings.entrySet()) {
                for (String orderID : mapping.getValue()) {
                    orderToBulkId.put(orderID, mapping.getKey());
                }
            }

            Map<String, BulkOrder> bulkById = bulkOrderRepository.findAll().stream()
                    .collect(Collectors.toMap(BulkOrder::getOrderID, bulk -> bulk, (a, b) -> a));

            for (Order order : orderRepository.findAll()) {
                Optional<Fund> maybeFund = resolveFund(order.getProductID());
                if (maybeFund.isEmpty()) {
                    missingOrderFunds++;
                    continue;
                }
                String bulkOrderID = orderToBulkId.get(order.getOrderID());
                BulkOrder bulkOrder = bulkOrderID == null ? null : bulkById.get(bulkOrderID);
                projectionStore.projectOrder(order, bulkOrder, maybeFund.get());
                projectedOrders++;
            }

            for (BulkOrder bulkOrder : bulkById.values()) {
                Optional<Fund> maybeFund = resolveFund(bulkOrder.getProductID());
                if (maybeFund.isEmpty()) {
                    missingBulkFunds++;
                    continue;
                }
                List<String> mappedOrderIDs = mappings.getOrDefault(bulkOrder.getOrderID(), List.of());
                projectionStore.projectBulkOrder(bulkOrder, maybeFund.get(), mappedOrderIDs);
                projectedBulkOrders++;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("message", "Replay completed");
            payload.put("projectedOrders", projectedOrders);
            payload.put("projectedBulkOrders", projectedBulkOrders);
            payload.put("missingOrderFunds", missingOrderFunds);
            payload.put("missingBulkFunds", missingBulkFunds);
            publishViewEvent("replay-completed", payload);
            sendJsonResponse(exchange, 200, payload);
        }
    }

    private final class ViewStreamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            if (projectionStore == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Read model is not configured"));
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            sseClients.add(outputStream);

            writeSseEvent(outputStream, "connected", "{\"message\":\"SSE connected\"}");

            try {
                while (true) {
                    outputStream.write(":keepalive\\n\\n".getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    Thread.sleep(15000);
                }
            } catch (Exception ex) {
                sseClients.remove(outputStream);
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private final class ViewUiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }

            String html = "<!doctype html>"
                    + "<html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                    + "<title>OMS CQRS Dashboard</title>"
                    + "<style>"
                    + "body{font-family:ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,sans-serif;margin:0;background:#f4f7fb;color:#1d2939;}"
                    + ".wrap{max-width:1100px;margin:0 auto;padding:20px;}"
                    + ".card{background:#fff;border:1px solid #d0d5dd;border-radius:12px;padding:16px;margin-bottom:16px;}"
                    + ".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;}"
                    + ".metric{font-size:26px;font-weight:700;}"
                    + "table{width:100%;border-collapse:collapse;font-size:14px;}"
                    + "th,td{border-bottom:1px solid #eaecf0;padding:8px;text-align:left;}"
                    + "h1{margin:0 0 12px;}"
                    + "</style></head><body><div class='wrap'>"
                    + "<h1>OMS CQRS Dashboard</h1>"
                    + "<div class='card'><div class='grid'><div><div>Total Orders</div><div id='mOrders' class='metric'>0</div></div>"
                    + "<div><div>Total Bulk Orders</div><div id='mBulk' class='metric'>0</div></div></div></div>"
                    + "<div class='card'><h3>Orders</h3><table><thead><tr><th>Order ID</th><th>Account</th><th>Fund</th><th>Amount</th><th>Qty</th><th>Status</th></tr></thead><tbody id='ordersBody'></tbody></table></div>"
                    + "<div class='card'><h3>Bulk Orders</h3><table><thead><tr><th>Bulk ID</th><th>Fund</th><th>Amount</th><th>Qty</th><th>Status</th><th>Count</th></tr></thead><tbody id='bulkBody'></tbody></table></div>"
                    + "</div><script>"
                    + "async function reload(){const r=await fetch('/view/dashboard');const d=await r.json();"
                    + "document.getElementById('mOrders').textContent=d.totalOrders||0;"
                    + "document.getElementById('mBulk').textContent=d.totalBulkOrders||0;"
                    + "document.getElementById('ordersBody').innerHTML=(d.orders||[]).map(o=>`<tr><td>${o.orderID||''}</td><td>${o.accountID||''}</td><td>${o.fundID||''}</td><td>${o.amount||''}</td><td>${o.quantity||''}</td><td>${o.orderStatus||''}</td></tr>`).join('');"
                    + "document.getElementById('bulkBody').innerHTML=(d.bulkOrders||[]).map(b=>`<tr><td>${b.bulkOrderID||''}</td><td>${b.fundID||''}</td><td>${b.totalAmount||''}</td><td>${b.totalQuantity||''}</td><td>${b.bulkOrderStatus||''}</td><td>${b.matchedOrderCount||0}</td></tr>`).join('');}"
                    + "reload();const es=new EventSource('/view/stream');es.onmessage=reload;es.addEventListener('order-updated',reload);es.addEventListener('bulk-order-updated',reload);"
                    + "</script></body></html>";

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private Optional<Fund> resolveFund(String fundID) {
        if (fundRepository == null || fundID == null || fundID.isBlank()) {
            return Optional.empty();
        }
        return fundRepository.findByFundId(fundID);
    }

    private void publishViewEvent(String eventType, Object payload) {
        if (sseClients.isEmpty()) {
            return;
        }
        String data;
        try {
            data = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return;
        }
        for (OutputStream client : sseClients) {
            try {
                writeSseEvent(client, eventType, data);
            } catch (IOException ex) {
                sseClients.remove(client);
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void writeSseEvent(OutputStream outputStream, String eventType, String data) throws IOException {
        String payload = "event: " + eventType + "\\n" + "data: " + data + "\\n\\n";
        outputStream.write(payload.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private Map<String, Object> toOrderEventPayload(Order order, String bulkOrderID) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderID", order.getOrderID());
        payload.put("accountID", order.getAccountID());
        payload.put("fundID", order.getProductID());
        payload.put("orderStatus", order.getOrderStatus().name());
        payload.put("amount", order.getAmount());
        payload.put("quantity", order.getQuantity());
        payload.put("bulkOrderID", bulkOrderID);
        return payload;
    }

    private Map<String, Object> toBulkOrderEventPayload(BulkOrder bulkOrder, List<String> mappedOrderIDs) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bulkOrderID", bulkOrder.getOrderID());
        payload.put("fundID", bulkOrder.getProductID());
        payload.put("bulkOrderStatus", bulkOrder.getBulkOrderStatus().name());
        payload.put("amount", bulkOrder.getAmount());
        payload.put("quantity", bulkOrder.getQuantity());
        payload.put("mappedOrderIDs", mappedOrderIDs);
        return payload;
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
}
