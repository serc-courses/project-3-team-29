package com.iiit.oms.db.postgres;

import com.iiit.oms.db.util.PostgresConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PostgresBulkOrderMappingDatabase {
    private final PostgresConnectionFactory connectionFactory;

    public PostgresBulkOrderMappingDatabase(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void upsert(String bulkOrderId, List<String> individualOrderIds) {
        String deleteSql = "DELETE FROM bulk_order_mappings WHERE bulk_order_id = ?";
        String insertSql = "INSERT INTO bulk_order_mappings(bulk_order_id, individual_order_id, sequence_no) VALUES (?, ?, ?)";

        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
                 PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                deleteStatement.setString(1, bulkOrderId);
                deleteStatement.executeUpdate();

                for (int i = 0; i < individualOrderIds.size(); i++) {
                    insertStatement.setString(1, bulkOrderId);
                    insertStatement.setString(2, individualOrderIds.get(i));
                    insertStatement.setInt(3, i);
                    insertStatement.addBatch();
                }
                insertStatement.executeBatch();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to upsert bulk order mapping " + bulkOrderId, ex);
        }
    }

    public Optional<List<String>> getById(String bulkOrderId) {
        String sql = "SELECT individual_order_id FROM bulk_order_mappings WHERE bulk_order_id = ? ORDER BY sequence_no";
        List<String> ids = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bulkOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString("individual_order_id"));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to find bulk order mapping " + bulkOrderId, ex);
        }
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids);
    }

    public Map<String, List<String>> getAll() {
        String sql = "SELECT bulk_order_id, individual_order_id FROM bulk_order_mappings ORDER BY bulk_order_id, sequence_no";
        Map<String, List<String>> mappings = new LinkedHashMap<>();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String bulkOrderId = rs.getString("bulk_order_id");
                String individualOrderId = rs.getString("individual_order_id");
                mappings.computeIfAbsent(bulkOrderId, ignored -> new ArrayList<>()).add(individualOrderId);
            }
            return mappings;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to list bulk order mappings", ex);
        }
    }

    public boolean exists(String bulkOrderId) {
        String sql = "SELECT 1 FROM bulk_order_mappings WHERE bulk_order_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bulkOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to check bulk order mapping existence " + bulkOrderId, ex);
        }
    }

    public void deleteById(String bulkOrderId) {
        String sql = "DELETE FROM bulk_order_mappings WHERE bulk_order_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bulkOrderId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete bulk order mapping " + bulkOrderId, ex);
        }
    }

    public void clear() {
        String sql = "DELETE FROM bulk_order_mappings";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to clear bulk order mappings", ex);
        }
    }
}
