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
import com.iiit.oms.model.Account;
import com.iiit.oms.model.Advisor;
import com.iiit.oms.model.AdvisorClientRelationship;
import com.iiit.oms.model.User;
import com.iiit.oms.model.UserSession;
import com.iiit.oms.auth.SessionStore;
import com.iiit.oms.auth.InMemorySessionStore;
import com.iiit.oms.auth.JwtService;
import com.iiit.oms.auth.InMemoryRevocationStore;
import com.iiit.oms.filter.RbacFilter;
import com.iiit.oms.repository.AuditLogRepository;
import com.iiit.oms.repository.ReconciliationBreakRepository;
import com.iiit.oms.model.ReconciliationBreak;
import com.iiit.oms.idempotency.IdempotencyStore;
import com.iiit.oms.repository.AccountRepository;
import com.iiit.oms.repository.AdvisorClientRelationshipRepository;
import com.iiit.oms.repository.AdvisorRepository;
import com.iiit.oms.repository.UserRepository;
import com.iiit.oms.repository.BulkOrderMappingRepository;
import com.iiit.oms.repository.BulkOrderRepository;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.repository.OrderRepository;
import com.iiit.oms.util.UniqueIdGenerator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OrderRestServer implements SseBroadcaster {
    private static final Logger LOGGER = Logger.getLogger(OrderRestServer.class.getName());
    private static final String PLAN_ORDERS_PATH = "/orders/plan";
    private static final String CANCEL_ORDERS_PATH = "/orders/cancel";
    private static final String LIST_ORDERS_PATH = "/orders";
    private static final String ORDER_STATUS_PATH = "/orders/status";
    private static final String CONFIRM_ORDERS_PATH = "/orders/confirm";
    private static final String BOOK_ORDERS_PATH = "/orders/book";
    private static final String VIEW_ORDERS_PATH = "/view/orders";
    private static final String VIEW_BULK_ORDERS_PATH = "/view/bulk-orders";
    private static final String VIEW_DASHBOARD_PATH = "/view/dashboard";
    private static final String VIEW_TRANSACTIONS_PATH = "/view/transactions";
    private static final String VIEW_AGGREGATES_ACCOUNTS_PATH = "/view/aggregates/accounts";
    private static final String VIEW_AGGREGATES_FUNDS_PATH = "/view/aggregates/funds";
    private static final String VIEW_REPLAY_PATH = "/view/replay";
    private static final String VIEW_STREAM_PATH = "/view/stream";
    private static final String VIEW_UI_PATH = "/view/ui";
    private static final String FUNDS_PATH = "/funds";
    private static final String ACCOUNTS_PATH = "/accounts";
    private static final String AUTH_LOGIN_PATH = "/auth/login";
    private static final String AUTH_ME_PATH = "/auth/me";
    private static final String AUTH_LOGOUT_PATH = "/auth/logout";

    private static final String VIEW_USERS_PATH = "/view/users";

    private static final String ADVISOR_ME_PATH = "/advisor/me";
    private static final String ADVISOR_CLIENTS_PATH = "/advisor/clients";
    private static final String ADVISOR_ORDERS_PATH = "/advisor/orders";
    private static final String ADVISOR_ORDERS_PLAN_PATH = "/advisor/orders/plan";
    private static final String ADVISOR_DASHBOARD_PATH = "/advisor/dashboard";
    private static final String TRANSFER_AGENT_CONTRACT_PATH = "/transfer-agent/contract";
    private static final String TRANSFER_AGENT_EOD_PATH = "/transfer-agent/eod";
    private static final String ORDER_AUDIT_PATH = "/orders/audit";
    private static final String VIEW_RECONCILIATION_PATH = "/view/reconciliation";
    private static final String RECONCILIATION_RESOLVE_PATH = "/view/reconciliation/resolve";
    private static final String PORTFOLIO_PATH = "/view/portfolio";
    private static final String FUNDS_NAV_PATH = "/funds/nav";
    private static final String VIEW_SLA_PATH = "/view/sla";
    private static final String VIEW_AUDIT_ARCHIVE_PATH = "/view/audit-archive";

    private final HttpServer httpServer;
    private final OrderRepository orderRepository;
    private final BulkOrderRepository bulkOrderRepository;
    private final BulkOrderMappingRepository bulkOrderMappingRepository;
    private final FundRepository fundRepository;
    private final AccountRepository accountRepository;
    private final AdvisorRepository advisorRepository;
    private final AdvisorClientRelationshipRepository advisorClientRelationshipRepository;
    private final UserRepository userRepository;
    private volatile SessionStore tokenStore;
    private volatile JwtService jwtService;
    private final RbacFilter rbacFilter;
    private volatile com.iiit.oms.db.util.PostgresConnectionFactory pgConnectionFactory;
    private final OrderStateMachine orderStateMachine;
    private final ProjectionStore projectionStore;
    private final OrderProjectionListener projectionListener;
    private final ObjectMapper objectMapper;
    private final List<OutputStream> sseClients;
    private IdempotencyStore idempotencyStore;
    private com.iiit.oms.kafka.KafkaOrderEventPublisher kafkaPublisher;
    private AuditLogRepository auditLogRepository;
    private ReconciliationBreakRepository reconciliationBreakRepository;

    public OrderRestServer(int port, OrderRepository orderRepository) throws IOException {
        this(port, orderRepository, null, null, null, null, null, null, null, null, null, null);
    }

    public OrderRestServer(int port,
            OrderRepository orderRepository,
            BulkOrderRepository bulkOrderRepository,
            BulkOrderMappingRepository bulkOrderMappingRepository,
            OrderStateMachine orderStateMachine) throws IOException {
        this(port, orderRepository, bulkOrderRepository, bulkOrderMappingRepository, null, orderStateMachine, null,
                null, null, null, null, null);
    }

    public OrderRestServer(int port,
            OrderRepository orderRepository,
            BulkOrderRepository bulkOrderRepository,
            BulkOrderMappingRepository bulkOrderMappingRepository,
            FundRepository fundRepository,
            OrderStateMachine orderStateMachine) throws IOException {
        this(port, orderRepository, bulkOrderRepository, bulkOrderMappingRepository, fundRepository, orderStateMachine,
                null, null, null, null, null, null);
    }

    public OrderRestServer(int port,
            OrderRepository orderRepository,
            BulkOrderRepository bulkOrderRepository,
            BulkOrderMappingRepository bulkOrderMappingRepository,
            FundRepository fundRepository,
            OrderStateMachine orderStateMachine,
            ProjectionStore projectionStore,
            OrderProjectionListener projectionListener) throws IOException {
        this(port, orderRepository, bulkOrderRepository, bulkOrderMappingRepository, fundRepository, orderStateMachine,
                projectionStore, projectionListener, null, null, null, null);
    }

    public OrderRestServer(int port,
            OrderRepository orderRepository,
            BulkOrderRepository bulkOrderRepository,
            BulkOrderMappingRepository bulkOrderMappingRepository,
            FundRepository fundRepository,
            OrderStateMachine orderStateMachine,
            ProjectionStore projectionStore,
            OrderProjectionListener projectionListener,
            AccountRepository accountRepository,
            AdvisorRepository advisorRepository,
            AdvisorClientRelationshipRepository advisorClientRelationshipRepository,
            UserRepository userRepository) throws IOException {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.bulkOrderRepository = bulkOrderRepository;
        this.bulkOrderMappingRepository = bulkOrderMappingRepository;
        this.fundRepository = fundRepository;
        this.accountRepository = accountRepository;
        this.advisorRepository = advisorRepository;
        this.advisorClientRelationshipRepository = advisorClientRelationshipRepository;
        this.userRepository = userRepository;
        this.orderStateMachine = orderStateMachine;
        this.projectionStore = projectionStore;
        this.projectionListener = projectionListener;
        this.objectMapper = new ObjectMapper();
        this.sseClients = new CopyOnWriteArrayList<>();
        this.tokenStore = new InMemorySessionStore();
        this.jwtService = new JwtService(new InMemoryRevocationStore());
        this.rbacFilter = new RbacFilter(this.jwtService);
        this.httpServer = createServer(port);
        com.iiit.oms.filter.LatencyFilter latencyFilter = new com.iiit.oms.filter.LatencyFilter();
        this.httpServer.createContext(ADVISOR_ORDERS_PLAN_PATH, withCors(new AdvisorPlanOrdersHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(ADVISOR_ORDERS_PATH, withCors(new AdvisorOrdersHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(ADVISOR_DASHBOARD_PATH, withCors(new AdvisorDashboardHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(ADVISOR_CLIENTS_PATH, withCors(new AdvisorClientsHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(ADVISOR_ME_PATH, withCors(new AdvisorMeHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(PLAN_ORDERS_PATH, withCors(new PlanOrdersHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(CANCEL_ORDERS_PATH, withCors(new CancelOrderHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(LIST_ORDERS_PATH, withCors(new ListOrdersHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(ORDER_STATUS_PATH, withCors(new OrderStatusHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(CONFIRM_ORDERS_PATH, withCors(new ConfirmOrdersHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(BOOK_ORDERS_PATH, withCors(new BookOrdersHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(VIEW_ORDERS_PATH, withCors(new ViewOrdersHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(VIEW_BULK_ORDERS_PATH, withCors(new ViewBulkOrdersHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(VIEW_TRANSACTIONS_PATH, withCors(new TransactionsHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(VIEW_DASHBOARD_PATH, withCors(new ViewDashboardHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(VIEW_AGGREGATES_ACCOUNTS_PATH, withCors(new ViewAggregateAccountsHandler()))
                .getFilters().add(latencyFilter);
        this.httpServer.createContext(VIEW_AGGREGATES_FUNDS_PATH, withCors(new ViewAggregateFundsHandler()))
                .getFilters().add(latencyFilter);
        this.httpServer.createContext(VIEW_REPLAY_PATH, withCors(new ViewReplayHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(VIEW_STREAM_PATH, withCors(new ViewStreamHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(VIEW_UI_PATH, withCors(new ViewUiHandler())).getFilters().add(latencyFilter);
        this.httpServer.createContext(FUNDS_PATH, withCors(new ListFundsHandler())).getFilters().add(latencyFilter);
        this.httpServer.createContext(ACCOUNTS_PATH, withCors(new ListAccountsHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(AUTH_LOGIN_PATH, withCors(new AuthLoginHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(AUTH_ME_PATH, withCors(new AuthMeHandler())).getFilters().add(latencyFilter);
        this.httpServer.createContext(AUTH_LOGOUT_PATH, withCors(new AuthLogoutHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(VIEW_USERS_PATH, withCors(new ViewUsersHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(TRANSFER_AGENT_CONTRACT_PATH, withCors(new ContractCallbackHandler()))
                .getFilters().add(latencyFilter);
        this.httpServer.createContext(TRANSFER_AGENT_EOD_PATH, withCors(new EndOfDayHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(ORDER_AUDIT_PATH, withCors(new AuditLogHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(RECONCILIATION_RESOLVE_PATH, withCors(new ResolveReconciliationHandler()))
                .getFilters().add(latencyFilter);
        this.httpServer.createContext(VIEW_RECONCILIATION_PATH, withCors(new ViewReconciliationHandler())).getFilters()
                .add(latencyFilter);
        this.httpServer.createContext(PORTFOLIO_PATH, withCors(new PortfolioHandler())).getFilters().add(latencyFilter);
        this.httpServer.createContext(FUNDS_NAV_PATH, withCors(new UpdateNavHandler())).getFilters().add(latencyFilter);
        this.httpServer.createContext(VIEW_SLA_PATH, withCors(new SlaHandler())).getFilters().add(latencyFilter);
        this.httpServer.createContext(VIEW_AUDIT_ARCHIVE_PATH, withCors(new AuditArchiveHandler())).getFilters().add(latencyFilter);
        this.httpServer.setExecutor(Executors.newFixedThreadPool(16));
    }

    private static final String CORS_ORIGIN_ENV_VAR = "OMS_CORS_ORIGIN";

    private String getAllowedOrigin() {
        String origin = System.getenv(CORS_ORIGIN_ENV_VAR);
        return (origin == null || origin.isBlank()) ? "*" : origin;
    }

    private HttpHandler withCors(HttpHandler handler) {
        return exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", getAllowedOrigin());
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (!rbacFilter.checkAccess(exchange)) return;

            handler.handle(exchange);
        };
    }

    /**
     * Creates an HttpsServer if a keystore is available, otherwise falls back to plain HttpServer.
     * Keystore path: OMS_KEYSTORE_PATH env var (default: src/main/resources/keystore.jks).
     * Keystore pass: OMS_KEYSTORE_PASS env var (default: changeit).
     * TLS protocols restricted to TLSv1.2 and TLSv1.3.
     */
    private static HttpServer createServer(int port) throws IOException {
        // Check system property first (allows test override), then env var, then default
        String keystorePath = System.getProperty("OMS_KEYSTORE_PATH");
        if (keystorePath == null || keystorePath.isBlank()) keystorePath = System.getenv("OMS_KEYSTORE_PATH");
        if (keystorePath == null || keystorePath.isBlank()) keystorePath = "src/main/resources/keystore.jks";

        String keystorePass = System.getProperty("OMS_KEYSTORE_PASS");
        if (keystorePass == null || keystorePass.isBlank()) keystorePass = System.getenv("OMS_KEYSTORE_PASS");
        if (keystorePass == null || keystorePass.isBlank()) keystorePass = "changeit";

        java.io.File ksFile = new java.io.File(keystorePath);
        if (!ksFile.exists()) {
            LOGGER.warning("TLS keystore not found at " + keystorePath + " — falling back to plain HTTP on port " + port
                    + ". Run scripts/gen-keystore.sh to enable HTTPS.");
            return HttpServer.create(new InetSocketAddress(port), 0);
        }

        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] pass = keystorePass.toCharArray();
            try (java.io.FileInputStream fis = new java.io.FileInputStream(ksFile)) {
                ks.load(fis, pass);
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, pass);

            SSLContext sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(kmf.getKeyManagers(), null, null);

            HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslCtx) {
                @Override
                public void configure(HttpsParameters params) {
                    SSLParameters sslParams = sslCtx.getDefaultSSLParameters();
                    sslParams.setProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
                    params.setSSLParameters(sslParams);
                }
            });
            LOGGER.info("TLS enabled on port " + port + " using keystore: " + keystorePath);
            return httpsServer;
        } catch (Exception ex) {
            LOGGER.warning("Failed to initialise TLS (" + ex.getMessage() + ") — falling back to plain HTTP on port " + port);
            return HttpServer.create(new InetSocketAddress(port), 0);
        }
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

    public void setIdempotencyStore(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    public void setKafkaPublisher(com.iiit.oms.kafka.KafkaOrderEventPublisher kafkaPublisher) {
        this.kafkaPublisher = kafkaPublisher;
    }

    public void setSessionStore(SessionStore sessionStore) {
        if (sessionStore != null) {
            this.tokenStore = sessionStore;
        }
    }

    public void setJwtService(JwtService jwtService) {
        if (jwtService != null) {
            this.jwtService = jwtService;
            this.rbacFilter.setJwtService(jwtService);
        }
    }

    public void setAuditLogRepository(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void setReconciliationBreakRepository(ReconciliationBreakRepository reconciliationBreakRepository) {
        this.reconciliationBreakRepository = reconciliationBreakRepository;
    }

    public void setConnectionFactory(com.iiit.oms.db.util.PostgresConnectionFactory factory) {
        this.pgConnectionFactory = factory;
    }

    private final class CancelOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, Map.of("status", "ok"));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only POST is supported"));
                return;
            }
            UserSession session = resolveAuthenticatedUser(exchange);
            if (session == null) {
                sendJsonResponse(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            User user = session.getUser();

            String query = exchange.getRequestURI().getQuery();
            String orderId = null;
            if (query != null && query.contains("orderID=")) {
                for (String param : query.split("&")) {
                    if (param.startsWith("orderID=")) {
                        orderId = param.split("=")[1];
                        break;
                    }
                }
            }
            if (orderId == null) {
                try {
                    InputStream is = exchange.getRequestBody();
                    Map<String, String> body = objectMapper.readValue(is, new TypeReference<Map<String, String>>() {
                    });
                    orderId = body.get("orderID");
                } catch (Exception e) {
                }
            }

            if (orderId == null) {
                sendJsonResponse(exchange, 400, Map.of("message", "Missing orderID"));
                return;
            }

            Optional<Order> orderOpt = orderRepository.findByOrderId(orderId);
            if (orderOpt.isEmpty()) {
                sendJsonResponse(exchange, 404, Map.of("message", "Order not found"));
                return;
            }
            Order order = orderOpt.get();
            if (!"ADMIN".equals(user.getRole()) && !"ADVISOR".equals(user.getRole())
                    && (user.getAccountID() != null && !user.getAccountID().equals(order.getAccountID()))) {
                sendJsonResponse(exchange, 403, Map.of("message", "Unauthorized to cancel this order"));
                return;
            }

            try {
                Order canceled = orderStateMachine.cancel(order);
                orderRepository.save(canceled);

                // Update the read-model projection store so view/orders returns the new CANCELLED status.
                // Without this, the projection store serves stale data after cancel.
                if (projectionListener != null && fundRepository != null) {
                    Optional<com.iiit.oms.model.Fund> maybeFund = resolveFund(canceled.getProductID());
                    if (maybeFund.isPresent()) {
                        projectionListener.onOrderStatusChanged(canceled, null, maybeFund.get());
                    }
                }

                broadcastOrderUpdate(canceled, null);
                sendJsonResponse(exchange, 200, canceled);
            } catch (IllegalStateException e) {
                sendJsonResponse(exchange, 400, Map.of("message", e.getMessage()));
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, Map.of("message", "Error cancelling order"));
            }
        }
    }

    private final class TransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, Map.of("status", "ok"));
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            UserSession session = resolveAuthenticatedUser(exchange);
            if (session == null) {
                sendJsonResponse(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            User user = session.getUser();
            String accountId = user.getAccountID();

            if (accountId == null) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.contains("accountID=")) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("accountID="))
                            accountId = param.split("=")[1];
                    }
                }
            }

            final String targetAccountId = accountId;
            java.util.List<Order> transactions = orderRepository.findAll().stream()
                    .filter(o -> o.getOrderStatus() == OrderStatus.BOOKED)
                    .filter(o -> targetAccountId == null || targetAccountId.equals(o.getAccountID()))
                    .collect(java.util.stream.Collectors.toList());

            transactions.sort(Comparator.comparing((Order o) -> o.getTradeDate() != null ? o.getTradeDate() : "").reversed());
            sendJsonResponse(exchange, 200, transactions);
        }
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

                // Read client-provided idempotency key (one per request batch)
                String clientIdempotencyKey = exchange.getRequestHeaders().getFirst("X-Idempotency-Key");

                List<String> assignedOrderIds = new ArrayList<>();
                for (Order order : orders) {
                    // Assign orderID if not present
                    if (order.getOrderID() == null || order.getOrderID().trim().isEmpty()) {
                        String uniqueId = UniqueIdGenerator.generate("ORD");
                        order.setOrderID(uniqueId);
                        LOGGER.info("Assigned unique ID " + uniqueId + " to order");
                    }

                    // Idempotency check: only use client-supplied key (X-Idempotency-Key header).
                    // The natural-key fallback (accountID:productID:amount:side) was removed
                    // because
                    // it blocked legitimate repeat orders for the same fund/amount combination.
                    if (idempotencyStore != null && clientIdempotencyKey != null && !clientIdempotencyKey.isBlank()) {
                        int idx = orders.indexOf(order);
                        String idemKey = "req:" + clientIdempotencyKey + (orders.size() > 1 ? ":" + idx : "");
                        boolean isNew = idempotencyStore.registerIfAbsent(idemKey, order.getOrderID(), 3600);
                        if (!isNew) {
                            String existingId = idempotencyStore.getOrderId(idemKey);
                            idempotencyStore.incrementDedupHits();
                            LOGGER.warning("Duplicate order submission detected for key=" + idemKey
                                    + " existing orderID=" + existingId);
                            writeResponse(exchange, 409,
                                    "{\"message\":\"Duplicate order submission\",\"existingOrderID\":\"" + existingId
                                            + "\"}");
                            return;
                        }
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
                        "orderIDs", assignedOrderIds);
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

    private final class ListFundsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeResponse(exchange, 405, "{\"message\":\"Only GET is supported\"}");
                return;
            }

            try {
                if (fundRepository == null) {
                    writeResponse(exchange, 500, "{\"message\":\"Fund repository not configured\"}");
                    return;
                }
                List<Fund> funds = fundRepository.findAll();
                LOGGER.info("Listing " + funds.size() + " funds from repository");
                String jsonResponse = objectMapper.writeValueAsString(funds);
                writeResponse(exchange, 200, jsonResponse);
            } catch (RuntimeException ex) {
                LOGGER.severe("Failed to retrieve funds: " + ex.getMessage());
                writeResponse(exchange, 500, "{\"message\":\"Failed to retrieve funds\"}");
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
                        "errorDescription",
                        order.get().getErrorDescription() == null ? "" : order.get().getErrorDescription());
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
                        .filter(bulkOrder -> bulkOrder.getBulkOrderStatus() == BulkOrderStatus.BULKED
                                || bulkOrder.getBulkOrderStatus() == BulkOrderStatus.TRANSMITTED)
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
                        if (order.getOrderStatus() != OrderStatus.BULKED
                                && order.getOrderStatus() != OrderStatus.TRANSMITTED) {
                            continue; // already moved on
                        }

                        OrderStatus prevStatus = order.getOrderStatus();
                        order.setOrderStatus(OrderStatus.CONFIRMED);
                        orderRepository.save(order);

                        Optional<Fund> maybeFund = resolveFund(order.getProductID());
                        if (projectionListener != null && maybeFund.isPresent()) {
                            projectionListener.onOrderStatusChanged(order, bulkOrder, maybeFund.get());
                            publishViewEvent("order-updated",
                                    toOrderEventPayload(order, bulkOrder.getOrderID()));
                        }

                        confirmedIndividualOrders++;
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
                        "missingIndividualOrders", missingIndividualOrders);
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

            if (bulkOrderRepository == null || bulkOrderMappingRepository == null || fundRepository == null
                    || orderStateMachine == null) {
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
                        throw new IllegalStateException(
                                "Invalid NAV for fund " + bulkOrder.getProductID() + ": " + nav);
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
                        OrderStatus prevStatus = order.getOrderStatus();
                        order.setQuantity(calculateQuantity(order.getAmount(), nav));

                        Order advancedOrder = orderStateMachine.processBooking(order);
                        orderRepository.save(advancedOrder);

                        if (kafkaPublisher != null && kafkaPublisher.isEnabled()) {
                            kafkaPublisher.publishOrderStateChanged(advancedOrder, prevStatus.name());
                        }

                        if (projectionListener != null) {
                            projectionListener.onOrderStatusChanged(advancedOrder, bulkOrder, maybeFund.get());
                            publishViewEvent("order-updated",
                                    toOrderEventPayload(advancedOrder, bulkOrder.getOrderID()));
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
                        "missingIndividualOrders", missingIndividualOrders);
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

                Map<String, Long> ordersByStatus = orders.stream()
                    .collect(Collectors.groupingBy(
                        o -> o.getOrderStatus() != null ? o.getOrderStatus() : "UNKNOWN",
                        Collectors.counting()));
                Map<String, Long> bulkOrdersByStatus = bulkOrders.stream()
                    .collect(Collectors.groupingBy(
                        b -> b.getBulkOrderStatus() != null ? b.getBulkOrderStatus() : "UNKNOWN",
                        Collectors.counting()));

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("totalOrders", orders.size());
            dashboard.put("totalBulkOrders", bulkOrders.size());
            dashboard.put("ordersByStatus", ordersByStatus);
            dashboard.put("bulkOrdersByStatus", bulkOrdersByStatus);
            dashboard.put("orders", orders);
            dashboard.put("bulkOrders", bulkOrders);
            // Dedup counter from Redis/in-memory idempotency store
            if (idempotencyStore != null) {
                dashboard.put("dedupHits", idempotencyStore.getDedupHits());
                dashboard.put("idempotencyStatus", idempotencyStore.getStatus());
            }

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
                    .collect(Collectors.groupingBy(o -> o.getAccountID() != null ? o.getAccountID() : "UNKNOWN_ACCOUNT"));

            List<Map<String, Object>> response = grouped.entrySet().stream()
                    .map(entry -> {
                        String accountID = entry.getKey();
                        List<OrderView> accountOrders = entry.getValue();
                        BigDecimal totalAmount = accountOrders.stream()
                                .filter(o -> !"ERRORED".equals(o.getOrderStatus())
                                        && !"FAILED".equals(o.getOrderStatus()) && !"SELL".equals(o.getOrderSide()))
                                .map(OrderView::getAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal totalQuantity = accountOrders.stream()
                                .map(OrderView::getQuantity)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        Map<String, Long> statusCounts = accountOrders.stream()
                            .collect(Collectors.groupingBy(
                                o -> o.getOrderStatus() != null ? o.getOrderStatus() : "UNKNOWN",
                                Collectors.counting()));

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
                    .collect(Collectors.groupingBy(o -> o.getFundID() != null ? o.getFundID() : "UNKNOWN_FUND"));

            List<Map<String, Object>> response = grouped.entrySet().stream()
                    .map(entry -> {
                        String fundID = entry.getKey();
                        List<OrderView> fundOrders = entry.getValue();
                        BigDecimal totalAmount = fundOrders.stream()
                                .filter(o -> !"ERRORED".equals(o.getOrderStatus())
                                        && !"FAILED".equals(o.getOrderStatus()) && !"SELL".equals(o.getOrderSide()))
                                .map(OrderView::getAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal totalQuantity = fundOrders.stream()
                                .map(OrderView::getQuantity)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        Map<String, Long> sideCounts = fundOrders.stream()
                            .filter(o -> !"ERRORED".equals(o.getOrderStatus())
                                    && !"FAILED".equals(o.getOrderStatus())
                                    && !"CANCELLED".equals(o.getOrderStatus()))
                            .collect(Collectors.groupingBy(
                                o -> o.getOrderSide() != null ? o.getOrderSide() : "UNKNOWN",
                                Collectors.counting()));
                        String fundName = fundOrders.stream().map(OrderView::getFundName).filter(Objects::nonNull)
                                .findFirst().orElse("");
                        BigDecimal nav = fundOrders.stream().map(OrderView::getNav).filter(Objects::nonNull).findFirst()
                                .orElse(null);

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
                    outputStream.write(":keepalive\n\n".getBytes(StandardCharsets.UTF_8));
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

    private UserSession resolveAuthenticatedUser(HttpExchange exchange) {
        String token = extractAccessToken(exchange);
        if (token == null || token.isBlank()) return null;
        if (jwtService != null) {
            return jwtService.validateAndExtract(token);
        }
        // Legacy UUID-token fallback
        UserSession session = tokenStore.get(token);
        if (session == null)
            return null;
        if (session.isExpired()) {
            tokenStore.remove(token);
            return null;
        }
        return session;
    }

    private String extractAccessToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isBlank()) return null;

        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && "access_token".equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String resolveAdvisorId(HttpExchange exchange) {
        // Token-first: resolve from authenticated session
        UserSession session = resolveAuthenticatedUser(exchange);
        if (session != null && "ADVISOR".equals(session.getUser().getRole())) {
            return session.getUser().getAdvisorID();
        }
        // Legacy fallback for X-Advisor-ID header or query param
        String fromHeader = exchange.getRequestHeaders().getFirst("X-Advisor-ID");
        if (fromHeader != null && !fromHeader.isBlank())
            return fromHeader.trim();
        return getQueryParam(exchange.getRequestURI().getQuery(), "advisorID");
    }

    private Map<String, Object> userToResponse(User user) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("userID", user.getUserID());
        resp.put("username", user.getUsername());
        resp.put("displayName", user.getDisplayName());
        resp.put("role", user.getRole());
        resp.put("accountID", user.getAccountID());
        resp.put("advisorID", user.getAdvisorID());
        return resp;
    }

    private final class AdvisorMeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            String advisorId = resolveAdvisorId(exchange);
            if (advisorId == null || advisorId.isBlank()) {
                sendJsonResponse(exchange, 400,
                        Map.of("message", "X-Advisor-ID header or advisorID query param required"));
                return;
            }
            if (advisorRepository == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Advisor repository not configured"));
                return;
            }
            Optional<com.iiit.oms.model.Advisor> advisor = advisorRepository.findByAdvisorId(advisorId);
            if (advisor.isEmpty()) {
                sendJsonResponse(exchange, 404, Map.of("message", "Advisor not found: " + advisorId));
                return;
            }
            List<String> clients = advisorClientRelationshipRepository != null
                    ? advisorClientRelationshipRepository.findClientAccountIds(advisorId)
                    : List.of();
            Map<String, Object> resp = new HashMap<>();
            resp.put("advisorID", advisor.get().getAdvisorID());
            resp.put("name", advisor.get().getName());
            resp.put("email", advisor.get().getEmail());
            resp.put("clientCount", clients.size());
            sendJsonResponse(exchange, 200, resp);
        }
    }

    private final class AdvisorClientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            String advisorId = resolveAdvisorId(exchange);
            if (advisorId == null || advisorId.isBlank()) {
                sendJsonResponse(exchange, 400,
                        Map.of("message", "X-Advisor-ID header or advisorID query param required"));
                return;
            }
            if (advisorRepository == null || advisorClientRelationshipRepository == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Advisor repositories not configured"));
                return;
            }
            if (advisorRepository.findByAdvisorId(advisorId).isEmpty()) {
                sendJsonResponse(exchange, 404, Map.of("message", "Advisor not found: " + advisorId));
                return;
            }
            List<String> clientIds = advisorClientRelationshipRepository.findClientAccountIds(advisorId);
            List<Map<String, Object>> response = new ArrayList<>();
            for (String accountID : clientIds) {
                Map<String, Object> row = new HashMap<>();
                row.put("accountID", accountID);
                if (projectionStore != null) {
                    List<OrderView> orders = projectionStore.findOrdersByAccount(accountID);
                    BigDecimal totalAmount = orders.stream()
                            .filter(o -> !"ERRORED".equals(o.getOrderStatus()) && !"FAILED".equals(o.getOrderStatus())
                                    && !"SELL".equals(o.getOrderSide()))
                            .map(OrderView::getAmount).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalQuantity = orders.stream().map(OrderView::getQuantity).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Map<String, Long> statuses = orders.stream()
                            .collect(Collectors.groupingBy(OrderView::getOrderStatus, Collectors.counting()));
                    row.put("orderCount", orders.size());
                    row.put("totalAmount", totalAmount);
                    row.put("totalQuantity", totalQuantity);
                    row.put("statuses", statuses);
                } else {
                    row.put("orderCount", 0);
                    row.put("totalAmount", BigDecimal.ZERO);
                    row.put("totalQuantity", BigDecimal.ZERO);
                    row.put("statuses", Map.of());
                }
                response.add(row);
            }
            sendJsonResponse(exchange, 200, response);
        }
    }

    private final class AdvisorOrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            String advisorId = resolveAdvisorId(exchange);
            if (advisorId == null || advisorId.isBlank()) {
                sendJsonResponse(exchange, 400, Map.of("message", "X-Advisor-ID header required"));
                return;
            }
            if (advisorRepository == null || advisorClientRelationshipRepository == null || projectionStore == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Advisor or projection repositories not configured"));
                return;
            }
            if (advisorRepository.findByAdvisorId(advisorId).isEmpty()) {
                sendJsonResponse(exchange, 404, Map.of("message", "Advisor not found"));
                return;
            }
            List<String> clientIds = advisorClientRelationshipRepository.findClientAccountIds(advisorId);
            String filterAccount = getQueryParam(exchange.getRequestURI().getQuery(), "accountID");
            String filterFund = getQueryParam(exchange.getRequestURI().getQuery(), "fundID");
            String filterStatus = getQueryParam(exchange.getRequestURI().getQuery(), "status");

            List<OrderView> orders = clientIds.stream()
                    .filter(id -> filterAccount == null || filterAccount.isBlank() || filterAccount.equals(id))
                    .flatMap(id -> projectionStore.findOrdersByAccount(id).stream())
                    .filter(o -> filterFund == null || filterFund.isBlank() || filterFund.equals(o.getFundID()))
                    .filter(o -> filterStatus == null || filterStatus.isBlank()
                            || filterStatus.equalsIgnoreCase(o.getOrderStatus()))
                    .sorted(Comparator.comparing(OrderView::getOrderID))
                    .collect(Collectors.toList());

            sendJsonResponse(exchange, 200, orders);
        }
    }

    private final class AdvisorPlanOrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only POST is supported"));
                return;
            }
            String advisorId = resolveAdvisorId(exchange);
            if (advisorId == null || advisorId.isBlank()) {
                sendJsonResponse(exchange, 400, Map.of("message", "X-Advisor-ID header required"));
                return;
            }
            if (advisorRepository == null || advisorClientRelationshipRepository == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Advisor repositories not configured"));
                return;
            }
            if (advisorRepository.findByAdvisorId(advisorId).isEmpty()) {
                sendJsonResponse(exchange, 404, Map.of("message", "Advisor not found: " + advisorId));
                return;
            }
            try {
                List<Order> orders = objectMapper.readValue(exchange.getRequestBody(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Order>>() {
                        });
                List<String> assignedIds = new ArrayList<>();
                for (Order order : orders) {
                    if (!advisorClientRelationshipRepository.isClientOfAdvisor(advisorId, order.getAccountID())) {
                        sendJsonResponse(exchange, 403, Map.of("message",
                                "Account " + order.getAccountID() + " is not a client of advisor " + advisorId));
                        return;
                    }
                    if (order.getOrderID() == null || order.getOrderID().isBlank()) {
                        order.setOrderID(UniqueIdGenerator.generate("ORD"));
                    }
                    if (order.getOrderSide() == null)
                        order.setOrderSide(OrderSide.BUY);
                    if (order.getOrderStatus() == null)
                        order.setOrderStatus(OrderStatus.PLANNED);
                    orderRepository.save(order);
                    assignedIds.add(order.getOrderID());
                    Optional<Fund> maybeFund = resolveFund(order.getProductID());
                    if (projectionListener != null && maybeFund.isPresent()) {
                        projectionListener.onOrderPlanned(order, maybeFund.get());
                        publishViewEvent("order-updated", toOrderEventPayload(order, null));
                    }
                }
                Map<String, Object> resp = Map.of("message", "Orders planned", "count", orders.size(), "orderIDs",
                        assignedIds);
                sendJsonResponse(exchange, 201, resp);
            } catch (Exception ex) {
                LOGGER.severe("Advisor plan orders failed: " + ex.getMessage());
                sendJsonResponse(exchange, 400, Map.of("message", "Invalid request: " + ex.getMessage()));
            }
        }
    }

    private final class AdvisorDashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            String advisorId = resolveAdvisorId(exchange);
            if (advisorId == null || advisorId.isBlank()) {
                sendJsonResponse(exchange, 400, Map.of("message", "X-Advisor-ID header required"));
                return;
            }
            if (advisorRepository == null || advisorClientRelationshipRepository == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Advisor repositories not configured"));
                return;
            }
            if (advisorRepository.findByAdvisorId(advisorId).isEmpty()) {
                sendJsonResponse(exchange, 404, Map.of("message", "Advisor not found"));
                return;
            }
            List<String> clientIds = advisorClientRelationshipRepository.findClientAccountIds(advisorId);
            List<OrderView> allOrders = new ArrayList<>();
            if (projectionStore != null) {
                for (String id : clientIds)
                    allOrders.addAll(projectionStore.findOrdersByAccount(id));
            }
            long activeOrders = allOrders.stream()
                    .filter(o -> List
                            .of("PLANNED", "VALIDATED", "ENRICHED", "PLACED", "BULKED", "CONFIRMED", "CONTRACTED")
                            .contains(o.getOrderStatus()))
                    .count();
            long failedOrders = allOrders.stream().filter(o -> "ERRORED".equals(o.getOrderStatus())).count();
            BigDecimal totalAmount = allOrders.stream()
                    .filter(o -> !"ERRORED".equals(o.getOrderStatus()) && !"FAILED".equals(o.getOrderStatus())
                            && !"SELL".equals(o.getOrderSide()))
                    .map(OrderView::getAmount).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("advisorID", advisorId);
            dashboard.put("clientCount", clientIds.size());
            dashboard.put("totalAmount", totalAmount);
            dashboard.put("activeOrders", activeOrders);
            dashboard.put("failedOrders", failedOrders);
            dashboard.put("totalOrders", allOrders.size());
            sendJsonResponse(exchange, 200, dashboard);
        }
    }

    private final class ListAccountsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            if (accountRepository == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Account repository not configured"));
                return;
            }
            List<Map<String, String>> response = accountRepository.findAll().stream()
                    .map(a -> {
                        Map<String, String> row = new HashMap<>();
                        row.put("accountID", a.getAccountID());
                        row.put("accountName", a.getAccountName());
                        return row;
                    })
                    .sorted(Comparator.comparing(m -> m.get("accountID")))
                    .collect(Collectors.toList());
            sendJsonResponse(exchange, 200, response);
        }
    }

    private final class AuthLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only POST is supported"));
                return;
            }
            if (userRepository == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "User repository not configured"));
                return;
            }
            try {
                Map<String, String> body = objectMapper.readValue(
                        exchange.getRequestBody(), new TypeReference<Map<String, String>>() {
                        });
                String username = body.get("username");
                String password = body.get("password");
                if (username == null || username.isBlank() || password == null || password.isBlank()) {
                    sendJsonResponse(exchange, 400, Map.of("message", "username and password are required"));
                    return;
                }
                Optional<User> maybeUser = userRepository.findByUsername(username.trim());
                if (maybeUser.isEmpty()) {
                    sendJsonResponse(exchange, 401, Map.of("message", "Invalid username or password"));
                    return;
                }
                User user = maybeUser.get();
                boolean passwordOk = org.mindrot.jbcrypt.BCrypt.checkpw(password, user.getPassword());
                if (!passwordOk) {
                    sendJsonResponse(exchange, 401, Map.of("message", "Invalid username or password"));
                    return;
                }
                String token;
                if (jwtService != null) {
                    token = jwtService.generateToken(user);
                } else {
                    token = java.util.UUID.randomUUID().toString();
                    tokenStore.put(token, new UserSession(token, user));
                }
                Map<String, Object> response = userToResponse(user);
                response.put("token", token);
                sendJsonResponse(exchange, 200, response);
            } catch (Exception ex) {
                sendJsonResponse(exchange, 400, Map.of("message", "Invalid request body"));
            }
        }
    }

    private final class AuthMeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            UserSession session = resolveAuthenticatedUser(exchange);
            if (session == null) {
                sendJsonResponse(exchange, 401, Map.of("message", "Not authenticated"));
                return;
            }
            // JWT only encodes userID/role/username; reload the full user from the DB
            // so that displayName, accountID, and advisorID are always present.
            User user = session.getUser();
            if (userRepository != null) {
                Optional<User> fullUser = userRepository.findByUserID(user.getUserID());
                if (fullUser.isPresent()) {
                    user = fullUser.get();
                }
            }
            sendJsonResponse(exchange, 200, userToResponse(user));
        }
    }

    private final class ViewUsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            if (userRepository == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "User repository is not configured"));
                return;
            }

            List<User> users = userRepository.findAll();
            List<Advisor> advisors = advisorRepository != null ? advisorRepository.findAll() : List.of();
            Map<String, Advisor> advisorMap = advisors.stream()
                    .collect(Collectors.toMap(Advisor::getAdvisorID, a -> a));

            // Build advisor -> client accounts mapping
            Map<String, List<String>> advisorClients = new HashMap<>();
            if (advisorClientRelationshipRepository != null) {
                advisorClientRelationshipRepository.findAll().forEach(rel -> advisorClients
                        .computeIfAbsent(rel.getAdvisorID(), k -> new ArrayList<>()).add(rel.getAccountID()));
            }

            List<Map<String, Object>> userList = users.stream().map(u -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("userID", u.getUserID());
                row.put("username", u.getUsername());
                row.put("role", u.getRole());
                row.put("displayName", u.getDisplayName());
                row.put("accountID", u.getAccountID());
                row.put("advisorID", u.getAdvisorID());
                if ("ADVISOR".equals(u.getRole()) && u.getAdvisorID() != null) {
                    Advisor adv = advisorMap.get(u.getAdvisorID());
                    if (adv != null) {
                        row.put("advisorEmail", adv.getEmail());
                    }
                    row.put("clientAccounts", advisorClients.getOrDefault(u.getAdvisorID(), List.of()));
                }
                return row;
            }).sorted(Comparator.comparing(m -> (String) m.get("userID")))
                    .collect(Collectors.toList());

            sendJsonResponse(exchange, 200, userList);
        }
    }

    private final class AuthLogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only POST is supported"));
                return;
            }
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                if (jwtService != null) {
                    jwtService.revokeToken(token);
                } else {
                    tokenStore.remove(token);
                }
            }
            sendJsonResponse(exchange, 200, Map.of("message", "Logged out"));
        }
    }

    private final class AuditArchiveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            if (pgConnectionFactory == null) {
                sendJsonResponse(exchange, 503, Map.of("message", "Database not configured"));
                return;
            }
            try (java.sql.Connection conn = pgConnectionFactory.getConnection();
                 java.sql.PreparedStatement stmt = conn.prepareStatement(
                         "SELECT run_id, archived_at, records_count, file_path, checksum_sha256 " +
                         "FROM audit_archive_runs ORDER BY archived_at DESC")) {
                java.sql.ResultSet rs = stmt.executeQuery();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("runId",          rs.getLong("run_id"));
                    row.put("archivedAt",     rs.getTimestamp("archived_at").toInstant().toString());
                    row.put("recordsCount",   rs.getInt("records_count"));
                    row.put("filePath",       rs.getString("file_path"));
                    row.put("checksumSha256", rs.getString("checksum_sha256"));
                    rows.add(row);
                }
                sendJsonResponse(exchange, 200, rows);
            } catch (java.sql.SQLException ex) {
                sendJsonResponse(exchange, 500, Map.of("message", "DB error: " + ex.getMessage()));
            }
        }
    }

    private final class SlaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            UserSession session = resolveAuthenticatedUser(exchange);
            if (session == null) {
                sendJsonResponse(exchange, 401, Map.of("message", "Authentication required"));
                return;
            }

            java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);

            // Onshore cutoff: 16:00 UTC today (or tomorrow if already past)
            java.time.ZonedDateTime onshore = now.toLocalDate()
                    .atTime(16, 0).atZone(java.time.ZoneOffset.UTC);
            if (!now.isBefore(onshore)) onshore = onshore.plusDays(1);

            // Offshore cutoff: 01:00 UTC today (or tomorrow if already past)
            java.time.ZonedDateTime offshore = now.toLocalDate()
                    .atTime(1, 0).atZone(java.time.ZoneOffset.UTC);
            if (!now.isBefore(offshore)) offshore = offshore.plusDays(1);

            long secToOnshore  = java.time.Duration.between(now, onshore).getSeconds();
            long secToOffshore = java.time.Duration.between(now, offshore).getSeconds();

            Map<String, Object> latency = new LinkedHashMap<>();
            latency.put("p99Ms",          com.iiit.oms.filter.LatencyFilter.getP99Ms());
            latency.put("avgMs",           com.iiit.oms.filter.LatencyFilter.getAvgMs());
            latency.put("totalRequests",   com.iiit.oms.filter.LatencyFilter.getTotalRequests());

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("serverTimeMs",       now.toInstant().toEpochMilli());
            resp.put("onshoreDeadline",    onshore.toString());
            resp.put("offshoreDeadline",   offshore.toString());
            resp.put("secondsToOnshore",   secToOnshore);
            resp.put("secondsToOffshore",  secToOffshore);
            resp.put("latency",            latency);
            sendJsonResponse(exchange, 200, resp);
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
        publishViewEventRaw(eventType, data);
    }

    private void publishViewEventRaw(String eventType, String data) {
        if (sseClients.isEmpty()) {
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
        String payload = "event: " + eventType + "\n" + "data: " + data + "\n\n";
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

    private String buildIdempotencyKey(Order order) {
        return order.getAccountID() + ":" + order.getProductID() + ":" + order.getAmount() + ":" + order.getOrderSide();
    }

    // --- SseBroadcaster implementation ---
    @Override
    public void broadcastOrderUpdate(Order order, String bulkOrderID) {
        publishViewEvent("order-updated", toOrderEventPayload(order, bulkOrderID));
    }

    @Override
    public void broadcastBulkOrderUpdate(BulkOrder bulkOrder, java.util.List<String> mappedOrderIDs) {
        publishViewEvent("bulk-order-updated", toBulkOrderEventPayload(bulkOrder, mappedOrderIDs));
    }

    @Override
    public void broadcastRawEvent(String eventType, String jsonPayload) {
        publishViewEventRaw(eventType, jsonPayload);
    }

    /**
     * POST /transfer-agent/contract
     * Simulates the Transfer Agent calling back with contract details (NAV + total
     * shares).
     * Body: { "bulkOrderId": "BLK-xxx", "nav": 47.23, "totalShares": 4447.28,
     * "contractRef": "CTR-001" }
     * Advances all constituent orders: TRANSMITTED/CONFIRMED → CONTRACTED → BOOKED
     */
    private final class ContractCallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only POST is supported"));
                return;
            }
            if (bulkOrderRepository == null || bulkOrderMappingRepository == null || orderStateMachine == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "Contract callback endpoint not configured"));
                return;
            }
            try {
                Map<String, Object> body = objectMapper.readValue(exchange.getRequestBody(),
                        new TypeReference<Map<String, Object>>() {
                        });
                String bulkOrderId = (String) body.get("bulkOrderId");
                if (bulkOrderId == null || bulkOrderId.isBlank()) {
                    sendJsonResponse(exchange, 400, Map.of("message", "bulkOrderId is required"));
                    return;
                }
                Object navObj = body.get("nav");
                Object sharesObj = body.get("totalShares");
                Object contractRefObj = body.get("contractRef");
                if (navObj == null || sharesObj == null) {
                    sendJsonResponse(exchange, 400, Map.of("message", "nav and totalShares are required"));
                    return;
                }
                BigDecimal nav = new BigDecimal(navObj.toString());
                BigDecimal totalShares = new BigDecimal(sharesObj.toString());
                String contractRef = contractRefObj != null ? contractRefObj.toString()
                        : "CTR-" + System.currentTimeMillis();

                Optional<BulkOrder> maybeBulk = bulkOrderRepository.findByOrderId(bulkOrderId);
                if (maybeBulk.isEmpty()) {
                    sendJsonResponse(exchange, 404, Map.of("message", "Bulk order not found: " + bulkOrderId));
                    return;
                }
                BulkOrder bulkOrder = maybeBulk.get();
                if (bulkOrder.getBulkOrderStatus() != BulkOrderStatus.TRANSMITTED) {
                    sendJsonResponse(exchange, 409, Map.of(
                            "message", "Bulk order is not in TRANSMITTED status",
                            "currentStatus", bulkOrder.getBulkOrderStatus().name()));
                    return;
                }

                List<String> individualOrderIds = bulkOrderMappingRepository
                        .findIndividualOrderIds(bulkOrderId).orElse(List.of());

                BigDecimal bulkAmount = bulkOrder.getAmount();

                // --- Reconciliation break detection ---
                boolean hasBreak = false;
                if (reconciliationBreakRepository != null && bulkAmount != null
                        && bulkAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal actualValue = nav.multiply(totalShares)
                            .setScale(8, java.math.RoundingMode.HALF_UP);
                    BigDecimal difference = bulkAmount.subtract(actualValue).abs();
                    BigDecimal threshold = bulkAmount.multiply(new BigDecimal("0.0001"))
                            .setScale(8, java.math.RoundingMode.HALF_UP); // 0.01%
                    if (difference.compareTo(threshold) > 0) {
                        hasBreak = true;
                        ReconciliationBreak breakRecord = new ReconciliationBreak(
                                com.iiit.oms.util.UniqueIdGenerator.generate("BRK"),
                                bulkOrderId,
                                "AMOUNT_MISMATCH",
                                bulkAmount.toPlainString(),
                                actualValue.toPlainString(),
                                java.time.Instant.now());
                        reconciliationBreakRepository.save(breakRecord);
                        LOGGER.warning("Reconciliation break detected for bulk order " + bulkOrderId
                                + ": expected=" + bulkAmount.toPlainString()
                                + " received=" + actualValue.toPlainString()
                                + " — orders will be FROZEN at CONTRACTED status");
                    }
                }

                int contracted = 0;
                int booked = 0;

                for (String orderId : individualOrderIds) {
                    Optional<Order> maybeOrder = orderRepository.findByOrderId(orderId);
                    if (maybeOrder.isEmpty())
                        continue;
                    Order order = maybeOrder.get();
                    OrderStatus statusBeforeContracted = order.getOrderStatus();

                    // Proportional share allocation: clientShares = (clientAmount / bulkAmount) *
                    // totalShares
                    BigDecimal allocatedShares = BigDecimal.ZERO;
                    if (bulkAmount != null && bulkAmount.compareTo(BigDecimal.ZERO) > 0 && order.getAmount() != null) {
                        allocatedShares = order.getAmount()
                                .divide(bulkAmount, 10, java.math.RoundingMode.HALF_UP)
                                .multiply(totalShares)
                                .setScale(8, java.math.RoundingMode.HALF_UP);
                    }

                    // CONTRACTED
                    orderStateMachine.advanceToContracted(order, contractRef, nav, allocatedShares);
                    orderRepository.save(order);
                    if (kafkaPublisher != null && kafkaPublisher.isEnabled()) {
                        kafkaPublisher.publishOrderStateChanged(order, statusBeforeContracted.name());
                    }
                    contracted++;

                    // Only advance to BOOKED if there is NO reconciliation break.
                    // When a break is detected, orders freeze at CONTRACTED pending ops review.
                    if (!hasBreak) {
                        OrderStatus statusBeforeBooked = order.getOrderStatus();
                        orderStateMachine.advanceToBooked(order);
                        orderRepository.save(order);
                        if (kafkaPublisher != null && kafkaPublisher.isEnabled()) {
                            kafkaPublisher.publishOrderStateChanged(order, statusBeforeBooked.name());
                        }

                        // CASH MANAGEMENT: Debit or Credit upon finalized booking
                        // For BUY: deduct the original order amount (what the user paid), not
                        // allocatedShares × nav (which may differ if TA allocated at a discounted NAV).
                        // For SELL: credit the actual proceeds = allocatedShares × nav.
                        if (accountRepository != null && order.getAllocatedShares() != null && nav != null) {
                            Optional<com.iiit.oms.model.Account> accOpt = accountRepository
                                    .findByAccountId(order.getAccountID());
                            if (accOpt.isPresent()) {
                                com.iiit.oms.model.Account acc = accOpt.get();
                                if (order.getOrderSide() == null
                                        || order.getOrderSide() == com.iiit.oms.model.OrderSide.BUY) {
                                    // Deduct what the user actually placed the order for
                                    BigDecimal debitAmount = order.getAmount() != null
                                            ? order.getAmount().setScale(4, java.math.RoundingMode.HALF_UP)
                                            : order.getAllocatedShares().multiply(nav).setScale(4, java.math.RoundingMode.HALF_UP);
                                    acc.setCashBalance(acc.getCashBalance().subtract(debitAmount));
                                } else if (order.getOrderSide() == com.iiit.oms.model.OrderSide.SELL) {
                                    // Credit actual proceeds received from TA
                                    BigDecimal executedAmount = order.getAllocatedShares().multiply(nav).setScale(4,
                                            java.math.RoundingMode.HALF_UP);
                                    acc.setCashBalance(acc.getCashBalance().add(executedAmount));
                                }
                                accountRepository.save(acc);
                            }
                        }

                        booked++;
                    }

                    Optional<Fund> maybeFund = resolveFund(order.getProductID());
                    if (projectionListener != null && maybeFund.isPresent()) {
                        projectionListener.onOrderStatusChanged(order, bulkOrder, maybeFund.get());
                        publishViewEvent("order-updated", toOrderEventPayload(order, bulkOrderId));
                    }
                }

                // Update bulk order status
                bulkOrder.setContractRef(contractRef);
                bulkOrder.setBulkNav(nav);
                bulkOrder.setBulkOrderStatus(BulkOrderStatus.CONTRACTED);
                bulkOrderRepository.save(bulkOrder);

                // Only advance bulk order to BOOKED if there is no break
                if (!hasBreak) {
                    bulkOrder.setBulkOrderStatus(BulkOrderStatus.BOOKED);
                    bulkOrderRepository.save(bulkOrder);
                }

                Optional<Fund> maybeFund = resolveFund(bulkOrder.getProductID());
                if (projectionListener != null && maybeFund.isPresent()) {
                    projectionListener.onBulkOrderStatusChanged(bulkOrder, maybeFund.get(), individualOrderIds);
                    publishViewEvent("bulk-order-updated", toBulkOrderEventPayload(bulkOrder, individualOrderIds));
                }

                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("message", hasBreak
                        ? "Contract received — RECONCILIATION BREAK detected. Orders frozen at CONTRACTED."
                        : "Contract callback processed");
                responseMap.put("bulkOrderId", bulkOrderId);
                responseMap.put("contractRef", contractRef);
                responseMap.put("nav", nav);
                responseMap.put("totalShares", totalShares);
                responseMap.put("contractedOrders", contracted);
                responseMap.put("bookedOrders", booked);
                responseMap.put("reconciliationBreak", hasBreak);
                sendJsonResponse(exchange, hasBreak ? 200 : 200, responseMap);
            } catch (Exception ex) {
                LOGGER.severe("Contract callback failed: " + ex.getMessage());
                sendJsonResponse(exchange, 500,
                        Map.of("message", "Failed to process contract callback: " + ex.getMessage()));
            }
        }
    }

    private final class EndOfDayHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only POST is supported"));
                return;
            }
            if (bulkOrderRepository == null || bulkOrderMappingRepository == null || orderStateMachine == null) {
                sendJsonResponse(exchange, 500, Map.of("message", "EOD endpoint not configured"));
                return;
            }
            try {
                List<BulkOrder> pendingBulkOrders = bulkOrderRepository.findAll().stream()
                        .filter(b -> b.getBulkOrderStatus() == BulkOrderStatus.TRANSMITTED
                                || b.getBulkOrderStatus() == BulkOrderStatus.CONFIRMED)
                        .collect(Collectors.toList());

                int processedBulks = 0;
                int totalContracted = 0;
                int totalBooked = 0;

                for (BulkOrder bulkOrder : pendingBulkOrders) {
                    Optional<Fund> maybeFund = resolveFund(bulkOrder.getProductID());
                    if (maybeFund.isEmpty() || maybeFund.get().getNAV() == null
                            || maybeFund.get().getNAV().compareTo(BigDecimal.ZERO) <= 0) {
                        continue; // Cannot process without valid NAV
                    }

                    BigDecimal nav = maybeFund.get().getNAV();
                    BigDecimal bulkAmount = bulkOrder.getAmount() != null ? bulkOrder.getAmount() : BigDecimal.ZERO;
                    if (bulkAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    BigDecimal totalShares = bulkAmount.divide(nav, 8, java.math.RoundingMode.HALF_UP);
                    String contractRef = "EOD-" + System.currentTimeMillis() + "-" + bulkOrder.getOrderID();

                    List<String> individualOrderIds = bulkOrderMappingRepository
                            .findIndividualOrderIds(bulkOrder.getOrderID()).orElse(List.of());

                    boolean hasBreak = false; // For EOD, assume no break since we trust our own NAV, but we could do
                                              // check. Let's assume false.

                    int contracted = 0;
                    int booked = 0;

                    for (String orderId : individualOrderIds) {
                        Optional<Order> maybeOrder = orderRepository.findByOrderId(orderId);
                        if (maybeOrder.isEmpty())
                            continue;
                        Order order = maybeOrder.get();
                        OrderStatus statusBeforeContracted = order.getOrderStatus();

                        BigDecimal allocatedShares = BigDecimal.ZERO;
                        if (order.getAmount() != null) {
                            allocatedShares = order.getAmount()
                                    .divide(bulkAmount, 10, java.math.RoundingMode.HALF_UP)
                                    .multiply(totalShares)
                                    .setScale(8, java.math.RoundingMode.HALF_UP);
                        }

                        orderStateMachine.advanceToContracted(order, contractRef, nav, allocatedShares);
                        orderRepository.save(order);
                        if (kafkaPublisher != null && kafkaPublisher.isEnabled()) {
                            kafkaPublisher.publishOrderStateChanged(order, statusBeforeContracted.name());
                        }
                        contracted++;

                        if (!hasBreak) {
                            OrderStatus statusBeforeBooked = order.getOrderStatus();
                            orderStateMachine.advanceToBooked(order);
                            orderRepository.save(order);
                            if (kafkaPublisher != null && kafkaPublisher.isEnabled()) {
                                kafkaPublisher.publishOrderStateChanged(order, statusBeforeBooked.name());
                            }

                            if (accountRepository != null && order.getAllocatedShares() != null && nav != null) {
                                Optional<com.iiit.oms.model.Account> accOpt = accountRepository
                                        .findByAccountId(order.getAccountID());
                                if (accOpt.isPresent()) {
                                    com.iiit.oms.model.Account acc = accOpt.get();
                                    if (order.getOrderSide() == null
                                            || order.getOrderSide() == com.iiit.oms.model.OrderSide.BUY) {
                                        // Deduct what the user actually placed the order for
                                        BigDecimal debitAmount = order.getAmount() != null
                                                ? order.getAmount().setScale(4, java.math.RoundingMode.HALF_UP)
                                                : order.getAllocatedShares().multiply(nav).setScale(4, java.math.RoundingMode.HALF_UP);
                                        acc.setCashBalance(acc.getCashBalance().subtract(debitAmount));
                                    } else if (order.getOrderSide() == com.iiit.oms.model.OrderSide.SELL) {
                                        // Credit actual proceeds received from TA
                                        BigDecimal executedAmount = order.getAllocatedShares().multiply(nav).setScale(4,
                                                java.math.RoundingMode.HALF_UP);
                                        acc.setCashBalance(acc.getCashBalance().add(executedAmount));
                                    }
                                    accountRepository.save(acc);
                                }
                            }
                            booked++;
                        }

                        if (projectionListener != null && maybeFund.isPresent()) {
                            projectionListener.onOrderStatusChanged(order, bulkOrder, maybeFund.get());
                            publishViewEvent("order-updated", toOrderEventPayload(order, bulkOrder.getOrderID()));
                        }
                    }

                    bulkOrder.setContractRef(contractRef);
                    bulkOrder.setBulkNav(nav);
                    bulkOrder.setBulkOrderStatus(BulkOrderStatus.CONTRACTED);
                    bulkOrderRepository.save(bulkOrder);

                    if (!hasBreak) {
                        bulkOrder.setBulkOrderStatus(BulkOrderStatus.BOOKED);
                        bulkOrderRepository.save(bulkOrder);
                    }

                    if (projectionListener != null && maybeFund.isPresent()) {
                        projectionListener.onBulkOrderStatusChanged(bulkOrder, maybeFund.get(), individualOrderIds);
                        publishViewEvent("bulk-order-updated", toBulkOrderEventPayload(bulkOrder, individualOrderIds));
                    }

                    processedBulks++;
                    totalContracted += contracted;
                    totalBooked += booked;
                }

                Map<String, Object> response = Map.of(
                        "message", "EOD processing completed",
                        "processedBulks", processedBulks,
                        "contractedOrders", totalContracted,
                        "bookedOrders", totalBooked);
                sendJsonResponse(exchange, 200, response);
            } catch (Exception ex) {
                LOGGER.severe("Failed to process EOD: " + ex.getMessage());
                sendJsonResponse(exchange, 500, Map.of("message", "Failed to process EOD"));
            }
        }
    }

    /**
     * GET /view/reconciliation
     * Returns all unresolved reconciliation breaks.
     */
    private final class ViewReconciliationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            if (reconciliationBreakRepository == null) {
                sendJsonResponse(exchange, 503, Map.of("message", "Reconciliation engine not configured"));
                return;
            }
            try {
                String query = exchange.getRequestURI().getQuery();
                String showAll = getQueryParam(query, "all");
                List<ReconciliationBreak> breaks;
                if ("true".equalsIgnoreCase(showAll)) {
                    breaks = reconciliationBreakRepository.findAll();
                } else {
                    breaks = reconciliationBreakRepository.findUnresolved();
                }
                List<Map<String, Object>> response = breaks.stream().map(b -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("breakId", b.getBreakId());
                    row.put("bulkOrderId", b.getBulkOrderId());
                    row.put("breakType", b.getBreakType());
                    row.put("expectedValue", b.getExpectedValue());
                    row.put("receivedValue", b.getReceivedValue());
                    row.put("detectedAt", b.getDetectedAt() != null ? b.getDetectedAt().toString() : null);
                    row.put("resolved", b.isResolved());
                    row.put("escalated", b.isEscalated());
                    return row;
                }).collect(Collectors.toList());
                sendJsonResponse(exchange, 200, response);
            } catch (Exception ex) {
                LOGGER.severe("Failed to fetch reconciliation breaks: " + ex.getMessage());
                sendJsonResponse(exchange, 500, Map.of("message", "Failed to fetch reconciliation breaks"));
            }
        }
    }

    /**
     * GET /view/portfolio
     * Computes P/L for all BOOKED orders that have NAV and allocatedShares.
     * Optional query params: ?accountID=xxx to filter by investor.
     * Returns per-holding P/L plus a portfolio summary.
     */
    private final class PortfolioHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            try {
                String query = exchange.getRequestURI().getQuery();
                String accountFilter = getQueryParam(query, "accountID");

                // Get all orders to calculate pending sells
                List<Order> allOrders = orderRepository.findAll();

                // Track pending sell amounts per account+fund
                Map<String, BigDecimal> pendingSellCash = new HashMap<>();
                BigDecimal pendingBuyCash = BigDecimal.ZERO;

                for (Order o : allOrders) {
                    if (o.getOrderStatus() != OrderStatus.BOOKED && o.getOrderStatus() != OrderStatus.ERRORED && o.getOrderStatus() != OrderStatus.CANCELLED) {
                        if (o.getOrderSide() == OrderSide.SELL && o.getAmount() != null) {
                            String key = o.getAccountID() + ":" + o.getProductID();
                            pendingSellCash.put(key,
                                    pendingSellCash.getOrDefault(key, BigDecimal.ZERO).add(o.getAmount()));
                        } else if ((o.getOrderSide() == null || o.getOrderSide() == OrderSide.BUY)
                                && o.getAmount() != null) {
                            if (accountFilter != null && accountFilter.equals(o.getAccountID())) {
                                pendingBuyCash = pendingBuyCash.add(o.getAmount());
                            }
                        }
                    }
                }

                // Get BOOKED orders and sort them by tradeDate, pushing BUYs before SELLs
                List<Order> bookedOrders = allOrders.stream()
                        .filter(o -> o.getOrderStatus() == OrderStatus.BOOKED)
                        .filter(o -> o.getNav() != null && o.getAllocatedShares() != null)
                        .filter(o -> accountFilter == null || accountFilter.isBlank()
                                || accountFilter.equals(o.getAccountID()))
                        .sorted(Comparator.comparing((Order o) -> o.getTradeDate() == null ? "" : o.getTradeDate())
                                .thenComparingInt(
                                        o -> (o.getOrderSide() == null || o.getOrderSide() == OrderSide.BUY) ? 0 : 1))
                        .collect(Collectors.toList());

                // Build a fund NAV lookup (current live NAVs)
                Map<String, BigDecimal> currentNavs = new HashMap<>();
                for (Fund f : fundRepository.findAll()) {
                    currentNavs.put(f.getFundID(), f.getNAV());
                }

                // Build fund name lookup
                Map<String, String> fundNames = new HashMap<>();
                for (Fund f : fundRepository.findAll()) {
                    fundNames.put(f.getFundID(), f.getFundName());
                }

                // Aggregate P/L per holding using average cost basis
                Map<String, Map<String, Object>> aggregated = new HashMap<>();
                for (Order order : bookedOrders) {
                    String fundId = order.getProductID();
                    String accountId = order.getAccountID();
                    String key = accountId + ":" + fundId;

                    Map<String, Object> h = aggregated.computeIfAbsent(key, k -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("accountID", accountId);
                        map.put("fundID", fundId);
                        map.put("fundName", fundNames.getOrDefault(fundId, fundId));
                        map.put("shares", BigDecimal.ZERO);
                        map.put("investedAmount", BigDecimal.ZERO);
                        return map;
                    });

                    BigDecimal shares = order.getAllocatedShares();
                    BigDecimal amount = order.getAmount();

                    BigDecimal currShares = (BigDecimal) h.get("shares");
                    BigDecimal currInvested = (BigDecimal) h.get("investedAmount");

                    if (order.getOrderSide() == null || order.getOrderSide() == com.iiit.oms.model.OrderSide.BUY) {
                        h.put("shares", currShares.add(shares));
                        h.put("investedAmount", currInvested.add(amount));
                    } else if (order.getOrderSide() == com.iiit.oms.model.OrderSide.SELL) {
                        if (currShares.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal avgCost = currInvested.divide(currShares, 8, java.math.RoundingMode.HALF_UP);
                            currShares = currShares.subtract(shares);
                            if (currShares.compareTo(BigDecimal.ZERO) < 0) {
                                currShares = BigDecimal.ZERO;
                            }
                            h.put("shares", currShares);
                            h.put("investedAmount",
                                    currShares.multiply(avgCost).setScale(2, java.math.RoundingMode.HALF_UP));
                        }
                    }
                }

                List<Map<String, Object>> holdings = new ArrayList<>();
                BigDecimal totalInvested = BigDecimal.ZERO;
                BigDecimal totalCurrentValue = BigDecimal.ZERO;

                for (Map<String, Object> h : aggregated.values()) {
                    BigDecimal shares = (BigDecimal) h.get("shares");
                    if (shares.compareTo(BigDecimal.ZERO) <= 0)
                        continue; // Skip emptied positions

                    BigDecimal invested = (BigDecimal) h.get("investedAmount");
                    String fundId = (String) h.get("fundID");
                    BigDecimal liveNav = currentNavs.getOrDefault(fundId, BigDecimal.ZERO);

                    // Average buy nav for display
                    BigDecimal avgBuyNav = invested.divide(shares, 4, java.math.RoundingMode.HALF_UP);

                    BigDecimal currentValue = liveNav.multiply(shares).setScale(2, java.math.RoundingMode.HALF_UP);
                    BigDecimal pnl = currentValue.subtract(invested);
                    BigDecimal pnlPct = invested.compareTo(BigDecimal.ZERO) > 0
                            ? pnl.divide(invested, 6, java.math.RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100")).setScale(2, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    // Calculate pending shares equivalent
                    BigDecimal currentPendingCash = pendingSellCash.getOrDefault(h.get("accountID") + ":" + fundId,
                            BigDecimal.ZERO);
                    BigDecimal pendingShares = liveNav.compareTo(BigDecimal.ZERO) > 0
                            ? currentPendingCash.divide(liveNav, 6, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    BigDecimal availableShares = shares.subtract(pendingShares);
                    if (availableShares.compareTo(BigDecimal.ZERO) < 0) {
                        availableShares = BigDecimal.ZERO;
                    }

                    totalInvested = totalInvested.add(invested);
                    totalCurrentValue = totalCurrentValue.add(currentValue);

                    Map<String, Object> out = new HashMap<>();
                    out.put("orderID", fundId + "-agg"); // Unique enough for React keys when aggregated
                    out.put("accountID", h.get("accountID"));
                    out.put("fundID", fundId);
                    out.put("fundName", h.get("fundName"));
                    out.put("investedAmount", invested);
                    out.put("buyNav", avgBuyNav);
                    out.put("allocatedShares", availableShares);
                    out.put("currentNav", liveNav);
                    out.put("currentValue", currentValue);
                    out.put("pnl", pnl);
                    out.put("pnlPercent", pnlPct);
                    holdings.add(out);
                }

                BigDecimal totalPnl = totalCurrentValue.subtract(totalInvested);
                BigDecimal totalPnlPct = totalInvested.compareTo(BigDecimal.ZERO) > 0
                        ? totalPnl.divide(totalInvested, 6, java.math.RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100")).setScale(2, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                Map<String, Object> response = new HashMap<>();
                response.put("holdings", holdings);
                response.put("totalInvested", totalInvested);
                response.put("totalCurrentValue", totalCurrentValue);
                response.put("totalPnl", totalPnl);
                response.put("totalPnlPercent", totalPnlPct);
                response.put("holdingCount", holdings.size());

                if (accountFilter != null && !accountFilter.isBlank() && accountRepository != null) {
                    Optional<com.iiit.oms.model.Account> accOpt = accountRepository.findByAccountId(accountFilter);
                    if (accOpt.isPresent() && accOpt.get().getCashBalance() != null) {
                        BigDecimal availableCash = accOpt.get().getCashBalance().subtract(pendingBuyCash);
                        if (availableCash.compareTo(BigDecimal.ZERO) < 0)
                            availableCash = BigDecimal.ZERO;
                        response.put("availableCash", availableCash);
                    }
                }

                sendJsonResponse(exchange, 200, response);
            } catch (Exception ex) {
                LOGGER.severe("Portfolio P/L computation failed: " + ex.getMessage());
                sendJsonResponse(exchange, 500, Map.of("message", "Failed to compute portfolio: " + ex.getMessage()));
            }
        }
    }

    /**
     * POST /view/reconciliation/resolve
     * Allows ops to resolve a reconciliation break.
     * Body: { "breakId": "BRK-xxx", "action": "ACCEPT" | "RETRANSMIT" | "CANCEL" }
     *
     * ACCEPT = Acknowledge the TA's values. Absorb the difference and advance
     * all frozen CONTRACTED orders → BOOKED. This is the most common
     * resolution (e.g., TA deducted a legitimate fee).
     *
     * RETRANSMIT = Dispute the TA's values. Roll all CONTRACTED orders back to
     * TRANSMITTED so a new corrected contract callback can be received.
     * The ops team contacts the TA to re-issue a corrected contract.
     *
     * CANCEL = Abandon the orders entirely. Mark all CONTRACTED orders as
     * ERRORED and bulk order as ERRORED. Used when the break is
     * unresolvable (e.g., fraud, severe communication failure).
     */
    private final class ResolveReconciliationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only POST is supported"));
                return;
            }
            if (reconciliationBreakRepository == null || bulkOrderRepository == null
                    || bulkOrderMappingRepository == null || orderStateMachine == null) {
                sendJsonResponse(exchange, 503, Map.of("message", "Reconciliation resolve not configured"));
                return;
            }
            try {
                Map<String, Object> body = objectMapper.readValue(exchange.getRequestBody(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });
                String breakId = (String) body.get("breakId");
                String action = (String) body.get("action");
                if (breakId == null || breakId.isBlank()) {
                    sendJsonResponse(exchange, 400, Map.of("message", "breakId is required"));
                    return;
                }
                if (action == null || (!"ACCEPT".equalsIgnoreCase(action)
                        && !"RETRANSMIT".equalsIgnoreCase(action)
                        && !"CANCEL".equalsIgnoreCase(action))) {
                    sendJsonResponse(exchange, 400, Map.of("message", "action must be ACCEPT, RETRANSMIT, or CANCEL"));
                    return;
                }

                // Find the break
                List<ReconciliationBreak> allBreaks = reconciliationBreakRepository.findAll();
                ReconciliationBreak targetBreak = allBreaks.stream()
                        .filter(b -> b.getBreakId().equals(breakId))
                        .findFirst().orElse(null);
                if (targetBreak == null) {
                    sendJsonResponse(exchange, 404, Map.of("message", "Break not found: " + breakId));
                    return;
                }
                if (targetBreak.isResolved()) {
                    sendJsonResponse(exchange, 409, Map.of("message", "Break already resolved"));
                    return;
                }

                String bulkOrderId = targetBreak.getBulkOrderId();
                Optional<BulkOrder> maybeBulk = bulkOrderRepository.findByOrderId(bulkOrderId);
                if (maybeBulk.isEmpty()) {
                    sendJsonResponse(exchange, 404, Map.of("message", "Bulk order not found: " + bulkOrderId));
                    return;
                }
                BulkOrder bulkOrder = maybeBulk.get();
                List<String> individualOrderIds = bulkOrderMappingRepository
                        .findIndividualOrderIds(bulkOrderId).orElse(List.of());

                int affectedOrders = 0;
                String resultMessage;

                if ("ACCEPT".equalsIgnoreCase(action)) {
                    // ---- ACCEPT: Advance frozen CONTRACTED orders → BOOKED ----
                    for (String orderId : individualOrderIds) {
                        Optional<Order> maybeOrder = orderRepository.findByOrderId(orderId);
                        if (maybeOrder.isEmpty())
                            continue;
                        Order order = maybeOrder.get();
                        if (order.getOrderStatus() != OrderStatus.CONTRACTED)
                            continue;

                        orderStateMachine.advanceToBooked(order);
                        orderRepository.save(order);
                        affectedOrders++;

                        // CASH MANAGEMENT: Debit or Credit upon ACCEPT-forced booking
                        // For BUY: deduct the original order amount (what the user paid), not
                        // allocatedShares × nav (which may differ if TA allocated at a discounted NAV).
                        // For SELL: credit the actual proceeds = allocatedShares × nav.
                        if (accountRepository != null && order.getAllocatedShares() != null && order.getNav() != null) {
                            Optional<com.iiit.oms.model.Account> accOpt = accountRepository.findByAccountId(order.getAccountID());
                            if (accOpt.isPresent()) {
                                com.iiit.oms.model.Account acc = accOpt.get();
                                if (order.getOrderSide() == null || order.getOrderSide() == com.iiit.oms.model.OrderSide.BUY) {
                                    BigDecimal debitAmount = order.getAmount() != null
                                            ? order.getAmount().setScale(4, java.math.RoundingMode.HALF_UP)
                                            : order.getAllocatedShares().multiply(order.getNav()).setScale(4, java.math.RoundingMode.HALF_UP);
                                    acc.setCashBalance(acc.getCashBalance().subtract(debitAmount));
                                } else if (order.getOrderSide() == com.iiit.oms.model.OrderSide.SELL) {
                                    BigDecimal executedAmount = order.getAllocatedShares().multiply(order.getNav()).setScale(4, java.math.RoundingMode.HALF_UP);
                                    acc.setCashBalance(acc.getCashBalance().add(executedAmount));
                                }
                                accountRepository.save(acc);
                            }
                        }

                        Optional<Fund> maybeFund = resolveFund(order.getProductID());
                        if (projectionListener != null && maybeFund.isPresent()) {
                            projectionListener.onOrderStatusChanged(order, bulkOrder, maybeFund.get());
                            publishViewEvent("order-updated", toOrderEventPayload(order, bulkOrderId));
                        }
                    }
                    if (bulkOrder.getBulkOrderStatus() == BulkOrderStatus.CONTRACTED) {
                        bulkOrder.setBulkOrderStatus(BulkOrderStatus.BOOKED);
                        bulkOrderRepository.save(bulkOrder);
                    }
                    resultMessage = "Break accepted — " + affectedOrders + " orders advanced to BOOKED";

                } else if ("RETRANSMIT".equalsIgnoreCase(action)) {
                    // ---- RETRANSMIT: Roll back CONTRACTED → TRANSMITTED ----
                    // Clear the contract data so a fresh contract callback can be received
                    for (String orderId : individualOrderIds) {
                        Optional<Order> maybeOrder = orderRepository.findByOrderId(orderId);
                        if (maybeOrder.isEmpty())
                            continue;
                        Order order = maybeOrder.get();
                        if (order.getOrderStatus() != OrderStatus.CONTRACTED)
                            continue;

                        order.setOrderStatus(OrderStatus.TRANSMITTED);
                        order.setContractRef(null);
                        order.setNav(null);
                        order.setAllocatedShares(null);
                        orderRepository.save(order);
                        affectedOrders++;

                        Optional<Fund> maybeFund = resolveFund(order.getProductID());
                        if (projectionListener != null && maybeFund.isPresent()) {
                            projectionListener.onOrderStatusChanged(order, bulkOrder, maybeFund.get());
                            publishViewEvent("order-updated", toOrderEventPayload(order, bulkOrderId));
                        }
                    }
                    // Roll bulk order back to TRANSMITTED
                    bulkOrder.setBulkOrderStatus(BulkOrderStatus.TRANSMITTED);
                    bulkOrder.setContractRef(null);
                    bulkOrder.setBulkNav(null);
                    bulkOrderRepository.save(bulkOrder);
                    resultMessage = "Break rejected — " + affectedOrders
                            + " orders rolled back to TRANSMITTED. Submit a new corrected contract callback.";

                } else {
                    // ---- CANCEL: Mark all orders as ERRORED ----
                    for (String orderId : individualOrderIds) {
                        Optional<Order> maybeOrder = orderRepository.findByOrderId(orderId);
                        if (maybeOrder.isEmpty())
                            continue;
                        Order order = maybeOrder.get();
                        if (order.getOrderStatus() != OrderStatus.CONTRACTED)
                            continue;

                        order.setOrderStatus(OrderStatus.ERRORED);
                        order.setErrorDescription("Cancelled due to unresolvable reconciliation break " + breakId);
                        orderRepository.save(order);
                        affectedOrders++;

                        Optional<Fund> maybeFund = resolveFund(order.getProductID());
                        if (projectionListener != null && maybeFund.isPresent()) {
                            projectionListener.onOrderStatusChanged(order, bulkOrder, maybeFund.get());
                            publishViewEvent("order-updated", toOrderEventPayload(order, bulkOrderId));
                        }
                    }
                    // There is no ERRORED on BulkOrderStatus, so keep at CONTRACTED
                    // but the individual orders are all ERRORED which effectively kills the batch
                    resultMessage = "Break cancelled — " + affectedOrders
                            + " orders marked as ERRORED. Bulk order abandoned.";
                }

                // Publish bulk order update
                Optional<Fund> maybeFund = resolveFund(bulkOrder.getProductID());
                if (projectionListener != null && maybeFund.isPresent()) {
                    projectionListener.onBulkOrderStatusChanged(bulkOrder, maybeFund.get(), individualOrderIds);
                    publishViewEvent("bulk-order-updated", toBulkOrderEventPayload(bulkOrder, individualOrderIds));
                }

                // Mark break as resolved
                reconciliationBreakRepository.markResolved(breakId);

                Map<String, Object> response = new HashMap<>();
                response.put("message", resultMessage);
                response.put("breakId", breakId);
                response.put("action", action.toUpperCase());
                response.put("affectedOrders", affectedOrders);
                sendJsonResponse(exchange, 200, response);
            } catch (Exception ex) {
                LOGGER.severe("Failed to resolve reconciliation break: " + ex.getMessage());
                sendJsonResponse(exchange, 500, Map.of("message", "Failed to resolve break: " + ex.getMessage()));
            }
        }
    }

    /**
     * GET /orders/audit?orderID=xxx
     * Returns the full audit trail for a specific order.
     */
    private final class AuditLogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only GET is supported"));
                return;
            }
            if (auditLogRepository == null) {
                sendJsonResponse(exchange, 503, Map.of("message", "Audit log not configured"));
                return;
            }
            String orderID = getQueryParam(exchange.getRequestURI().getQuery(), "orderID");
            if (orderID == null || orderID.isBlank()) {
                sendJsonResponse(exchange, 400, Map.of("message", "orderID query parameter is required"));
                return;
            }
            try {
                List<com.iiit.oms.model.AuditLogEntry> entries = auditLogRepository.findByOrderId(orderID);
                List<Map<String, Object>> response = entries.stream().map(e -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("orderID", e.getOrderID());
                    row.put("fromStatus", e.getFromStatus());
                    row.put("toStatus", e.getToStatus());
                    row.put("occurredAt", e.getOccurredAt().toString());
                    row.put("actor", e.getActor());
                    row.put("details", e.getDetails());
                    return row;
                }).collect(Collectors.toList());
                sendJsonResponse(exchange, 200, response);
            } catch (Exception ex) {
                LOGGER.severe("Failed to fetch audit log for order " + orderID + ": " + ex.getMessage());
                sendJsonResponse(exchange, 500, Map.of("message", "Failed to fetch audit log"));
            }
        }
    }

    /**
     * POST /funds/nav
     * Body: { "fundID": "FND001", "nav": 52.30 }
     * Updates the fund's NAV in DB, publishes to Kafka oms.nav.updated, and
     * broadcasts SSE.
     */
    private final class UpdateNavHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("message", "Only POST is supported"));
                return;
            }
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = objectMapper.readValue(exchange.getRequestBody(), Map.class);
                String fundID = (String) body.get("fundID");
                Number navNum = (Number) body.get("nav");

                if (fundID == null || fundID.isBlank() || navNum == null) {
                    sendJsonResponse(exchange, 400, Map.of("message", "fundID and nav are required"));
                    return;
                }

                BigDecimal newNav = new BigDecimal(navNum.toString()).setScale(2, java.math.RoundingMode.HALF_UP);
                if (newNav.compareTo(BigDecimal.ZERO) <= 0) {
                    sendJsonResponse(exchange, 400, Map.of("message", "NAV must be greater than 0"));
                    return;
                }

                Optional<Fund> maybeFund = fundRepository.findByFundId(fundID);
                if (maybeFund.isEmpty()) {
                    sendJsonResponse(exchange, 404, Map.of("message", "Fund not found: " + fundID));
                    return;
                }

                Fund fund = maybeFund.get();
                BigDecimal oldNav = fund.getNAV();
                fund.setNAV(newNav);
                fundRepository.save(fund);

                LOGGER.info("NAV updated for " + fundID + ": " + oldNav + " -> " + newNav);

                // Publish to Kafka
                if (kafkaPublisher != null) {
                    kafkaPublisher.publishNavUpdated(fundID, fund.getFundName(), oldNav, newNav);
                }

                // Broadcast SSE event so all connected UIs refresh
                publishViewEvent("nav-updated", Map.of(
                        "fundID", fundID,
                        "fundName", fund.getFundName(),
                        "oldNav", oldNav,
                        "newNav", newNav));

                sendJsonResponse(exchange, 200, Map.of(
                        "message", "NAV updated",
                        "fundID", fundID,
                        "fundName", fund.getFundName(),
                        "oldNav", oldNav,
                        "newNav", newNav));
            } catch (Exception ex) {
                LOGGER.severe("Failed to update NAV: " + ex.getMessage());
                sendJsonResponse(exchange, 500, Map.of("message", "Failed to update NAV: " + ex.getMessage()));
            }
        }
    }
}
