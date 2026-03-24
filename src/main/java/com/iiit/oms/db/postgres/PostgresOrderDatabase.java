package com.iiit.oms.db.postgres;

import com.iiit.oms.db.util.PostgresConnectionFactory;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.model.OrderStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresOrderDatabase {
    private final PostgresConnectionFactory connectionFactory;

    public PostgresOrderDatabase(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void upsert(Order order) {
        String sql = "INSERT INTO orders(order_id, product_id, quantity, amount, account_id, order_side, order_status, is_processed, error_description) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (order_id) DO UPDATE SET "
                + "product_id = EXCLUDED.product_id, quantity = EXCLUDED.quantity, amount = EXCLUDED.amount, "
                + "account_id = EXCLUDED.account_id, order_side = EXCLUDED.order_side, order_status = EXCLUDED.order_status, is_processed = EXCLUDED.is_processed, error_description = EXCLUDED.error_description";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, order.getOrderID());
            statement.setString(2, order.getProductID());
            statement.setBigDecimal(3, order.getQuantity());
            statement.setBigDecimal(4, order.getAmount());
            statement.setString(5, order.getAccountID());
            statement.setString(6, order.getOrderSide().name());
            statement.setString(7, order.getOrderStatus().name());
            statement.setBoolean(8, order.isProcessed());
            statement.setString(9, order.getErrorDescription());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to upsert order " + order.getOrderID(), ex);
        }
    }

    public Optional<Order> getById(String orderID) {
        String sql = "SELECT order_id, product_id, quantity, amount, account_id, order_side, order_status, is_processed, error_description FROM orders WHERE order_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderID);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to find order " + orderID, ex);
        }
    }

    public List<Order> getAll() {
        String sql = "SELECT order_id, product_id, quantity, amount, account_id, order_side, order_status, is_processed, error_description FROM orders";
        List<Order> results = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to list orders", ex);
        }
    }

    public boolean exists(String orderID) {
        String sql = "SELECT 1 FROM orders WHERE order_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderID);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to check order existence " + orderID, ex);
        }
    }

    public void deleteById(String orderID) {
        String sql = "DELETE FROM orders WHERE order_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderID);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete order " + orderID, ex);
        }
    }

    public void clear() {
        String sql = "DELETE FROM orders";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to clear orders", ex);
        }
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Order order = new Order(
                rs.getString("order_id"),
                rs.getString("product_id"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("amount"),
                rs.getString("account_id"),
                OrderSide.valueOf(rs.getString("order_side")),
                OrderStatus.valueOf(rs.getString("order_status")),
                rs.getBoolean("is_processed")
        );
        order.setErrorDescription(rs.getString("error_description"));
        return order;
    }
}
