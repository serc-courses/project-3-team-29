package com.iiit.oms.db.postgres;

import com.iiit.oms.db.util.PostgresConnectionFactory;
import com.iiit.oms.model.AuditLogEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PostgresAuditLogDatabase {
    private final PostgresConnectionFactory connectionFactory;

    public PostgresAuditLogDatabase(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void insert(AuditLogEntry entry) {
        String sql = "INSERT INTO order_audit_log(order_id, from_status, to_status, occurred_at, actor, details) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entry.getOrderID());
            stmt.setString(2, entry.getFromStatus());
            stmt.setString(3, entry.getToStatus());
            stmt.setTimestamp(4, Timestamp.from(entry.getOccurredAt()));
            stmt.setString(5, entry.getActor());
            stmt.setString(6, entry.getDetails());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to insert audit log entry for order " + entry.getOrderID(), ex);
        }
    }

    public List<AuditLogEntry> findByOrderId(String orderID) {
        String sql = "SELECT order_id, from_status, to_status, occurred_at, actor, details "
                + "FROM order_audit_log WHERE order_id = ? ORDER BY occurred_at ASC";
        List<AuditLogEntry> results = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderID);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AuditLogEntry entry = new AuditLogEntry(
                            rs.getString("order_id"),
                            rs.getString("from_status"),
                            rs.getString("to_status"),
                            rs.getString("actor"),
                            rs.getString("details")
                    );
                    results.add(entry);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch audit log for order " + orderID, ex);
        }
        return results;
    }

    public void insertTransmissionLog(String bulkOrderId, String transferAgent, String transmissionRef) {
        String sql = "INSERT INTO transmission_log(bulk_order_id, transfer_agent, transmission_ref) VALUES (?, ?, ?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bulkOrderId);
            stmt.setString(2, transferAgent);
            stmt.setString(3, transmissionRef);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to insert transmission log for bulk order " + bulkOrderId, ex);
        }
    }
}
