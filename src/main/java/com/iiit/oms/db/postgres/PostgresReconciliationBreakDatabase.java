package com.iiit.oms.db.postgres;

import com.iiit.oms.db.util.PostgresConnectionFactory;
import com.iiit.oms.model.ReconciliationBreak;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PostgresReconciliationBreakDatabase {
    private final PostgresConnectionFactory connectionFactory;

    public PostgresReconciliationBreakDatabase(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void insert(ReconciliationBreak record) {
        String sql = "INSERT INTO reconciliation_breaks(break_id, bulk_order_id, break_type, "
                + "expected_value, received_value, detected_at, resolved, escalated) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, record.getBreakId());
            stmt.setString(2, record.getBulkOrderId());
            stmt.setString(3, record.getBreakType());
            stmt.setString(4, record.getExpectedValue());
            stmt.setString(5, record.getReceivedValue());
            stmt.setTimestamp(6, Timestamp.from(record.getDetectedAt()));
            stmt.setBoolean(7, record.isResolved());
            stmt.setBoolean(8, record.isEscalated());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to insert reconciliation break " + record.getBreakId(), ex);
        }
    }

    public List<ReconciliationBreak> findAll() {
        return query("SELECT * FROM reconciliation_breaks ORDER BY detected_at DESC");
    }

    public List<ReconciliationBreak> findUnresolved() {
        return query("SELECT * FROM reconciliation_breaks WHERE resolved = FALSE ORDER BY detected_at DESC");
    }

    public void markEscalated(String breakId) {
        String sql = "UPDATE reconciliation_breaks SET escalated = TRUE WHERE break_id = ?";
        try (Connection conn = connectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, breakId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to escalate reconciliation break " + breakId, ex);
        }
    }

    public void markResolved(String breakId) {
        String sql = "UPDATE reconciliation_breaks SET resolved = TRUE WHERE break_id = ?";
        try (Connection conn = connectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, breakId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to resolve reconciliation break " + breakId, ex);
        }
    }

    /**
     * Find all unresolved breaks older than 1 hour that are not yet escalated.
     */
    public List<ReconciliationBreak> findUnresolvedOlderThan(long seconds) {
        String sql = "SELECT * FROM reconciliation_breaks "
                + "WHERE resolved = FALSE AND escalated = FALSE "
                + "AND detected_at < ? ORDER BY detected_at ASC";
        Instant cutoff = Instant.now().minusSeconds(seconds);
        List<ReconciliationBreak> results = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.from(cutoff));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch stale reconciliation breaks", ex);
        }
        return results;
    }

    private List<ReconciliationBreak> query(String sql) {
        List<ReconciliationBreak> results = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch reconciliation breaks", ex);
        }
        return results;
    }

    private ReconciliationBreak mapRow(ResultSet rs) throws SQLException {
        ReconciliationBreak r = new ReconciliationBreak();
        r.setBreakId(rs.getString("break_id"));
        r.setBulkOrderId(rs.getString("bulk_order_id"));
        r.setBreakType(rs.getString("break_type"));
        r.setExpectedValue(rs.getString("expected_value"));
        r.setReceivedValue(rs.getString("received_value"));
        Timestamp ts = rs.getTimestamp("detected_at");
        if (ts != null)
            r.setDetectedAt(ts.toInstant());
        r.setResolved(rs.getBoolean("resolved"));
        r.setEscalated(rs.getBoolean("escalated"));
        return r;
    }
}
