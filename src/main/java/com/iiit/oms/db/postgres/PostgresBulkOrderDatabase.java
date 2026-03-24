package com.iiit.oms.db.postgres;

import com.iiit.oms.db.util.PostgresConnectionFactory;
import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.model.BulkOrderStatus;
import com.iiit.oms.model.OrderSide;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresBulkOrderDatabase {
    private final PostgresConnectionFactory connectionFactory;

    public PostgresBulkOrderDatabase(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void upsert(BulkOrder bulkOrder) {
        String sql = "INSERT INTO bulk_orders(order_id, product_id, order_side, bulk_order_status, quantity, amount, account_id) VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (order_id) DO UPDATE SET product_id = EXCLUDED.product_id, order_side = EXCLUDED.order_side, "
                + "bulk_order_status = EXCLUDED.bulk_order_status, quantity = EXCLUDED.quantity, amount = EXCLUDED.amount, account_id = EXCLUDED.account_id";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bulkOrder.getOrderID());
            statement.setString(2, bulkOrder.getProductID());
            statement.setString(3, bulkOrder.getOrderSide().name());
            statement.setString(4, bulkOrder.getBulkOrderStatus().name());
            statement.setBigDecimal(5, bulkOrder.getQuantity());
            statement.setBigDecimal(6, bulkOrder.getAmount());
            statement.setString(7, bulkOrder.getAccountID());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to upsert bulk order " + bulkOrder.getOrderID(), ex);
        }
    }

    public Optional<BulkOrder> getById(String orderId) {
        String sql = "SELECT order_id, product_id, order_side, bulk_order_status, quantity, amount, account_id FROM bulk_orders WHERE order_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to find bulk order " + orderId, ex);
        }
    }

    public List<BulkOrder> getAll() {
        String sql = "SELECT order_id, product_id, order_side, bulk_order_status, quantity, amount, account_id FROM bulk_orders";
        List<BulkOrder> results = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to list bulk orders", ex);
        }
    }

    public boolean exists(String orderId) {
        String sql = "SELECT 1 FROM bulk_orders WHERE order_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to check bulk order existence " + orderId, ex);
        }
    }

    public void deleteById(String orderId) {
        String sql = "DELETE FROM bulk_orders WHERE order_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete bulk order " + orderId, ex);
        }
    }

    public void clear() {
        String sql = "DELETE FROM bulk_orders";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to clear bulk orders", ex);
        }
    }

    private BulkOrder mapRow(ResultSet rs) throws SQLException {
        return new BulkOrder(
                rs.getString("order_id"),
                rs.getString("product_id"),
                OrderSide.valueOf(rs.getString("order_side")),
            BulkOrderStatus.valueOf(rs.getString("bulk_order_status")),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("amount"),
                rs.getString("account_id")
        );
    }
}
