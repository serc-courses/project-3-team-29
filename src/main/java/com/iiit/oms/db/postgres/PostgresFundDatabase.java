package com.iiit.oms.db.postgres;

import com.iiit.oms.db.util.PostgresConnectionFactory;
import com.iiit.oms.model.Fund;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresFundDatabase {
    private final PostgresConnectionFactory connectionFactory;

    public PostgresFundDatabase(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void upsert(Fund fund) {
        String sql = "INSERT INTO funds(fund_id, fund_name, fund_family, nav) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (fund_id) DO UPDATE SET fund_name = EXCLUDED.fund_name, fund_family = EXCLUDED.fund_family, nav = EXCLUDED.nav";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fund.getFundID());
            statement.setString(2, fund.getFundName());
            statement.setString(3, fund.getFundFamily());
            statement.setBigDecimal(4, fund.getNAV());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to upsert fund " + fund.getFundID(), ex);
        }
    }

    public Optional<Fund> getById(String fundID) {
        String sql = "SELECT fund_id, fund_name, fund_family, nav FROM funds WHERE fund_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fundID);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Fund(rs.getString("fund_id"), rs.getString("fund_name"), rs.getString("fund_family"), rs.getBigDecimal("nav")));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to find fund " + fundID, ex);
        }
    }

    public List<Fund> getAll() {
        String sql = "SELECT fund_id, fund_name, fund_family, nav FROM funds";
        List<Fund> results = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                results.add(new Fund(rs.getString("fund_id"), rs.getString("fund_name"), rs.getString("fund_family"), rs.getBigDecimal("nav")));
            }
            return results;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to list funds", ex);
        }
    }

    public boolean exists(String fundID) {
        String sql = "SELECT 1 FROM funds WHERE fund_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fundID);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to check fund existence " + fundID, ex);
        }
    }

    public void deleteById(String fundID) {
        String sql = "DELETE FROM funds WHERE fund_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fundID);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete fund " + fundID, ex);
        }
    }

    public void clear() {
        String sql = "DELETE FROM funds";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to clear funds", ex);
        }
    }
}
