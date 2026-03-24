package com.iiit.oms.db.postgres;

import com.iiit.oms.db.util.PostgresConnectionFactory;
import com.iiit.oms.model.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresAccountDatabase {
    private final PostgresConnectionFactory connectionFactory;

    public PostgresAccountDatabase(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void upsert(Account account) {
        String sql = "INSERT INTO accounts(account_id, account_name, national_identity) VALUES (?, ?, ?) "
                + "ON CONFLICT (account_id) DO UPDATE SET account_name = EXCLUDED.account_name, national_identity = EXCLUDED.national_identity";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getAccountID());
            statement.setString(2, account.getAccountName());
            statement.setString(3, account.getNationalIdentity());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to upsert account " + account.getAccountID(), ex);
        }
    }

    public Optional<Account> getById(String accountID) {
        String sql = "SELECT account_id, account_name, national_identity FROM accounts WHERE account_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountID);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Account(rs.getString("account_id"), rs.getString("account_name"), rs.getString("national_identity")));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to find account " + accountID, ex);
        }
    }

    public List<Account> getAll() {
        String sql = "SELECT account_id, account_name, national_identity FROM accounts";
        List<Account> results = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                results.add(new Account(rs.getString("account_id"), rs.getString("account_name"), rs.getString("national_identity")));
            }
            return results;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to list accounts", ex);
        }
    }

    public boolean exists(String accountID) {
        String sql = "SELECT 1 FROM accounts WHERE account_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountID);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to check account existence " + accountID, ex);
        }
    }

    public void deleteById(String accountID) {
        String sql = "DELETE FROM accounts WHERE account_id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountID);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete account " + accountID, ex);
        }
    }

    public void clear() {
        String sql = "DELETE FROM accounts";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to clear accounts", ex);
        }
    }
}
