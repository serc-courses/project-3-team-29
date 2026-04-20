package com.iiit.oms.db.postgres;

import com.iiit.oms.db.util.PostgresConnectionFactory;
import com.iiit.oms.model.Advisor;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresAdvisorDatabase {
    private final PostgresConnectionFactory connectionFactory;

    public PostgresAdvisorDatabase(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public Advisor save(Advisor advisor) {
        String sql = "INSERT INTO advisors (advisor_id, name, email) VALUES (?, ?, ?) "
                + "ON CONFLICT (advisor_id) DO UPDATE SET name = EXCLUDED.name, email = EXCLUDED.email";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, advisor.getAdvisorID());
            ps.setString(2, advisor.getName());
            ps.setString(3, advisor.getEmail());
            ps.executeUpdate();
            return advisor;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save advisor", e);
        }
    }

    public Optional<Advisor> findByAdvisorId(String advisorID) {
        String sql = "SELECT advisor_id, name, email FROM advisors WHERE advisor_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, advisorID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find advisor", e);
        }
    }

    public List<Advisor> findAll() {
        String sql = "SELECT advisor_id, name, email FROM advisors ORDER BY advisor_id";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Advisor> result = new ArrayList<>();
            while (rs.next()) result.add(map(rs));
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list advisors", e);
        }
    }

    private Advisor map(ResultSet rs) throws SQLException {
        return new Advisor(rs.getString("advisor_id"), rs.getString("name"), rs.getString("email"));
    }
}
