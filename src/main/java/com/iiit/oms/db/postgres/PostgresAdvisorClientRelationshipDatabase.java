package com.iiit.oms.db.postgres;

import com.iiit.oms.db.util.PostgresConnectionFactory;
import com.iiit.oms.model.AdvisorClientRelationship;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostgresAdvisorClientRelationshipDatabase {
    private final PostgresConnectionFactory connectionFactory;

    public PostgresAdvisorClientRelationshipDatabase(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public AdvisorClientRelationship save(AdvisorClientRelationship rel) {
        String sql = "INSERT INTO advisor_client_relationships (advisor_id, account_id, relationship_status) "
                + "VALUES (?, ?, ?) ON CONFLICT (advisor_id, account_id) DO UPDATE SET relationship_status = EXCLUDED.relationship_status";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rel.getAdvisorID());
            ps.setString(2, rel.getAccountID());
            ps.setString(3, rel.getRelationshipStatus());
            ps.executeUpdate();
            return rel;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save advisor-client relationship", e);
        }
    }

    public List<String> findClientAccountIds(String advisorID) {
        String sql = "SELECT account_id FROM advisor_client_relationships WHERE advisor_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, advisorID);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getString("account_id"));
                return ids;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find client accounts for advisor", e);
        }
    }

    public List<String> findAdvisorsByAccountId(String accountID) {
        String sql = "SELECT advisor_id FROM advisor_client_relationships WHERE account_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountID);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getString("advisor_id"));
                return ids;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find advisors for account", e);
        }
    }

    public boolean isClientOfAdvisor(String advisorID, String accountID) {
        String sql = "SELECT 1 FROM advisor_client_relationships WHERE advisor_id = ? AND account_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, advisorID);
            ps.setString(2, accountID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check advisor-client relationship", e);
        }
    }

    public List<AdvisorClientRelationship> findAll() {
        String sql = "SELECT advisor_id, account_id, relationship_status FROM advisor_client_relationships ORDER BY advisor_id, account_id";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<AdvisorClientRelationship> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new AdvisorClientRelationship(
                        rs.getString("advisor_id"), rs.getString("account_id"), rs.getString("relationship_status")));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list advisor-client relationships", e);
        }
    }
}
