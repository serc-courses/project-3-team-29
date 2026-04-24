package com.iiit.oms.readmodel.impl;

import com.iiit.oms.readmodel.*;
import com.iiit.oms.model.*;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MongoDB-backed implementation of ProjectionStore.
 * Provides persistent storage for CQRS read-side views using MongoDB.
 * Lightweight alternative to relational databases for read models.
 */
public class MongoDbProjectionStore implements ProjectionStore {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> orderViewsCollection;
    private final MongoCollection<Document> bulkOrderViewsCollection;
    private static final ReplaceOptions UPSERT = new ReplaceOptions().upsert(true);

    /**
     * Initialize MongoDB connection.
     * @param connectionString MongoDB connection string (e.g., "mongodb://localhost:27017")
     * @param databaseName Database name (e.g., "oms")
     */
    public MongoDbProjectionStore(String connectionString, String databaseName) {
        this.mongoClient = MongoClients.create(connectionString);
        this.database = mongoClient.getDatabase(databaseName);
        this.orderViewsCollection = database.getCollection("order_views");
        this.bulkOrderViewsCollection = database.getCollection("bulk_order_views");
        createIndexes();
    }

    /**
     * Create indexes for efficient querying
     */
    private void createIndexes() {
        try {
            // Indexes on order_views collection
            orderViewsCollection.createIndex(new Document("accountID", 1));
            orderViewsCollection.createIndex(new Document("fundID", 1));
            orderViewsCollection.createIndex(new Document("bulkOrderID", 1));
            orderViewsCollection.createIndex(new Document("orderStatus", 1));

            // Indexes on bulk_order_views collection
            bulkOrderViewsCollection.createIndex(new Document("fundID", 1));
            bulkOrderViewsCollection.createIndex(new Document("bulkOrderStatus", 1));
        } catch (Exception e) {
            System.err.println("Warning: Could not create indexes: " + e.getMessage());
        }
    }

    @Override
    public void projectOrder(Order order, BulkOrder bulkOrder, Fund fund) {
        OrderView view = new OrderView(
                order.getOrderID(),
                order.getAccountID(),
                fund.getFundID(),
                fund.getFundName(),
                order.getOrderSide().toString(),
                order.getAmount(),
                order.getQuantity(),
                fund.getNAV(),
                order.getOrderStatus().toString(),
                bulkOrder != null ? bulkOrder.getOrderID() : null
        );
        view.setFundFamily(order.getFundFamily() != null ? order.getFundFamily() : fund.getFundFamily());
        view.setTransferAgent(order.getTransferAgent());
        view.setTradeDate(order.getTradeDate());
        view.setSettlementDate(order.getSettlementDate());
        view.setContractRef(order.getContractRef());
        view.setAllocatedShares(order.getAllocatedShares());
        saveOrderView(view);
    }

    @Override
    public void updateOrderView(String orderID, Order order, Fund fund, BulkOrder bulkOrder) {
        Optional<OrderView> existing = findOrderView(orderID);
        if (existing.isPresent()) {
            OrderView view = existing.get();
            view.setOrderStatus(order.getOrderStatus().toString());
            view.setQuantity(order.getQuantity());
            view.setAmount(order.getAmount());
            view.setNav(fund.getNAV());
            view.setBulkOrderID(bulkOrder != null ? bulkOrder.getOrderID() : null);
            view.setFundFamily(order.getFundFamily() != null ? order.getFundFamily() : fund.getFundFamily());
            view.setTransferAgent(order.getTransferAgent());
            view.setTradeDate(order.getTradeDate());
            view.setSettlementDate(order.getSettlementDate());
            view.setContractRef(order.getContractRef());
            view.setAllocatedShares(order.getAllocatedShares());
            saveOrderView(view);
        }
    }

    @Override
    public Optional<OrderView> findOrderView(String orderID) {
        Document doc = orderViewsCollection.find(Filters.eq("orderID", orderID)).first();
        return doc != null ? Optional.of(documentToOrderView(doc)) : Optional.empty();
    }

    @Override
    public List<OrderView> findOrdersByAccount(String accountID) {
        return orderViewsCollection.find(Filters.eq("accountID", accountID))
                .into(new ArrayList<>())
                .stream()
                .map(this::documentToOrderView)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderView> findOrdersByFund(String fundID) {
        return orderViewsCollection.find(Filters.eq("fundID", fundID))
                .into(new ArrayList<>())
                .stream()
                .map(this::documentToOrderView)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderView> findOrdersByBulkOrder(String bulkOrderID) {
        return orderViewsCollection.find(Filters.eq("bulkOrderID", bulkOrderID))
                .into(new ArrayList<>())
                .stream()
                .map(this::documentToOrderView)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderView> findAllOrderViews() {
        return orderViewsCollection.find()
                .into(new ArrayList<>())
                .stream()
                .map(this::documentToOrderView)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteOrderView(String orderID) {
        orderViewsCollection.deleteOne(Filters.eq("orderID", orderID));
    }

    @Override
    public void projectBulkOrder(BulkOrder bulkOrder, Fund fund, List<String> matchedOrderIDs) {
        BulkOrderView view = new BulkOrderView(
                bulkOrder.getOrderID(),
                fund.getFundID(),
                fund.getFundName(),
                bulkOrder.getOrderSide().toString(),
                bulkOrder.getBulkOrderStatus().toString(),
                bulkOrder.getAmount(),
                bulkOrder.getQuantity(),
                fund.getNAV(),
                matchedOrderIDs,
                matchedOrderIDs != null ? matchedOrderIDs.size() : 0
        );
        saveBulkOrderView(view);
    }

    @Override
    public void updateBulkOrderView(String bulkOrderID, BulkOrder bulkOrder, Fund fund, List<String> matchedOrderIDs) {
        Optional<BulkOrderView> existing = findBulkOrderView(bulkOrderID);
        if (existing.isPresent()) {
            BulkOrderView view = existing.get();
            view.setBulkOrderStatus(bulkOrder.getBulkOrderStatus().toString());
            view.setTotalAmount(bulkOrder.getAmount());
            view.setTotalQuantity(bulkOrder.getQuantity());
            view.setNAV(fund.getNAV());
            view.setMatchedOrderIDs(matchedOrderIDs);
            view.setMatchedOrderCount(matchedOrderIDs != null ? matchedOrderIDs.size() : 0);
            saveBulkOrderView(view);
        }
    }

    @Override
    public Optional<BulkOrderView> findBulkOrderView(String bulkOrderID) {
        Document doc = bulkOrderViewsCollection.find(Filters.eq("bulkOrderID", bulkOrderID)).first();
        return doc != null ? Optional.of(documentToBulkOrderView(doc)) : Optional.empty();
    }

    @Override
    public List<BulkOrderView> findBulkOrdersByFund(String fundID) {
        return bulkOrderViewsCollection.find(Filters.eq("fundID", fundID))
                .into(new ArrayList<>())
                .stream()
                .map(this::documentToBulkOrderView)
                .collect(Collectors.toList());
    }

    @Override
    public List<BulkOrderView> findAllBulkOrderViews() {
        return bulkOrderViewsCollection.find()
                .into(new ArrayList<>())
                .stream()
                .map(this::documentToBulkOrderView)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteBulkOrderView(String bulkOrderID) {
        bulkOrderViewsCollection.deleteOne(Filters.eq("bulkOrderID", bulkOrderID));
    }

    @Override
    public void clearAll() {
        orderViewsCollection.deleteMany(new Document());
        bulkOrderViewsCollection.deleteMany(new Document());
    }

    @Override
    public String getStatus() {
        long orderCount = orderViewsCollection.countDocuments();
        long bulkOrderCount = bulkOrderViewsCollection.countDocuments();
        return String.format("MongoDbProjectionStore{orders=%d, bulkOrders=%d, database=%s}",
                orderCount, bulkOrderCount, database.getName());
    }

    // Helper methods
    private void saveOrderView(OrderView view) {
        Document doc = orderViewToDocument(view);
        orderViewsCollection.replaceOne(
                Filters.eq("orderID", view.getOrderID()),
                doc,
                UPSERT
        );
    }

    private void saveBulkOrderView(BulkOrderView view) {
        Document doc = bulkOrderViewToDocument(view);
        bulkOrderViewsCollection.replaceOne(
                Filters.eq("bulkOrderID", view.getBulkOrderID()),
                doc,
                UPSERT
        );
    }

    private Document orderViewToDocument(OrderView view) {
        return new Document()
                .append("orderID", view.getOrderID())
                .append("accountID", view.getAccountID())
                .append("fundID", view.getFundID())
                .append("fundName", view.getFundName())
                .append("orderSide", view.getOrderSide())
                .append("orderStatus", view.getOrderStatus())
                .append("amount", view.getAmount())
                .append("quantity", view.getQuantity())
                .append("nav", view.getNav())
                .append("bulkOrderID", view.getBulkOrderID())
                .append("fundFamily", view.getFundFamily())
                .append("transferAgent", view.getTransferAgent())
                .append("tradeDate", view.getTradeDate())
                .append("settlementDate", view.getSettlementDate())
                .append("contractRef", view.getContractRef())
                .append("allocatedShares", view.getAllocatedShares());
    }

    private OrderView documentToOrderView(Document doc) {
        OrderView view = new OrderView(
                doc.getString("orderID"),
                doc.getString("accountID"),
                doc.getString("fundID"),
                doc.getString("fundName"),
                doc.getString("orderSide"),
                doc.get("amount", org.bson.types.Decimal128.class) != null ?
                        doc.get("amount", org.bson.types.Decimal128.class).bigDecimalValue() : null,
                doc.get("quantity", org.bson.types.Decimal128.class) != null ?
                        doc.get("quantity", org.bson.types.Decimal128.class).bigDecimalValue() : null,
                doc.get("nav", org.bson.types.Decimal128.class) != null ?
                        doc.get("nav", org.bson.types.Decimal128.class).bigDecimalValue() : null,
                doc.getString("orderStatus"),
                doc.getString("bulkOrderID")
        );
        view.setFundFamily(doc.getString("fundFamily"));
        view.setTransferAgent(doc.getString("transferAgent"));
        view.setTradeDate(doc.getString("tradeDate"));
        view.setSettlementDate(doc.getString("settlementDate"));
        view.setContractRef(doc.getString("contractRef"));
        if (doc.get("allocatedShares", org.bson.types.Decimal128.class) != null) {
            view.setAllocatedShares(doc.get("allocatedShares", org.bson.types.Decimal128.class).bigDecimalValue());
        }
        return view;
    }

    private Document bulkOrderViewToDocument(BulkOrderView view) {
        return new Document()
                .append("bulkOrderID", view.getBulkOrderID())
                .append("fundID", view.getFundID())
                .append("fundName", view.getFundName())
                .append("orderSide", view.getOrderSide())
                .append("bulkOrderStatus", view.getBulkOrderStatus())
                .append("totalAmount", view.getTotalAmount())
                .append("totalQuantity", view.getTotalQuantity())
                .append("nav", view.getNAV())
                .append("matchedOrderIDs", view.getMatchedOrderIDs())
                .append("matchedOrderCount", view.getMatchedOrderCount());
    }

    private BulkOrderView documentToBulkOrderView(Document doc) {
        @SuppressWarnings("unchecked")
        List<String> matchedOrderIDs = (List<String>) doc.get("matchedOrderIDs", List.class);
        return new BulkOrderView(
                doc.getString("bulkOrderID"),
                doc.getString("fundID"),
                doc.getString("fundName"),
                doc.getString("orderSide"),
                doc.getString("bulkOrderStatus"),
                doc.get("totalAmount", org.bson.types.Decimal128.class) != null ?
                        doc.get("totalAmount", org.bson.types.Decimal128.class).bigDecimalValue() : null,
                doc.get("totalQuantity", org.bson.types.Decimal128.class) != null ?
                        doc.get("totalQuantity", org.bson.types.Decimal128.class).bigDecimalValue() : null,
                doc.get("nav", org.bson.types.Decimal128.class) != null ?
                        doc.get("nav", org.bson.types.Decimal128.class).bigDecimalValue() : null,
                matchedOrderIDs,
                doc.getInteger("matchedOrderCount")
        );
    }

    /**
     * Close the MongoDB connection (call on shutdown)
     */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
