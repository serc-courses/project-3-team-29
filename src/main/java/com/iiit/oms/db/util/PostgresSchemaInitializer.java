package com.iiit.oms.db.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class PostgresSchemaInitializer {
    private PostgresSchemaInitializer() {
    }

    public static void initialize(PostgresConnectionFactory connectionFactory) {
        initialize(connectionFactory, false);
    }

    public static void initialize(PostgresConnectionFactory connectionFactory, boolean cleanStart) {
        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement()) {
            for (String ddl : schemaStatements()) {
                statement.execute(ddl);
            }
            if (cleanStart) {
                for (String dml : cleanDataStatements()) {
                    statement.execute(dml);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to initialize PostgreSQL schema", ex);
        }
    }

    private static List<String> schemaStatements() {
        List<String> statements = new ArrayList<>();

        statements.add("CREATE TABLE IF NOT EXISTS accounts ("
                + "account_id VARCHAR(64) PRIMARY KEY,"
                + "account_name VARCHAR(255) NOT NULL,"
                + "national_identity VARCHAR(255) NOT NULL"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS funds ("
                + "fund_id VARCHAR(64) PRIMARY KEY,"
                + "fund_name VARCHAR(255) NOT NULL,"
                + "fund_family VARCHAR(255) NOT NULL,"
                + "nav NUMERIC(20, 8) NOT NULL"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS orders ("
                + "order_id VARCHAR(64) PRIMARY KEY,"
                + "product_id VARCHAR(64) NOT NULL,"
                + "quantity NUMERIC(20, 8),"
                + "amount NUMERIC(20, 8) NOT NULL,"
                + "account_id VARCHAR(64) NOT NULL,"
                + "order_side VARCHAR(16) NOT NULL,"
                + "order_status VARCHAR(32) NOT NULL,"
                + "is_processed BOOLEAN NOT NULL,"
                + "error_description TEXT"
                + ")");

        // Backward-compatible schema evolution for existing databases.
        statements.add("ALTER TABLE orders ADD COLUMN IF NOT EXISTS error_description TEXT");
        statements.add("ALTER TABLE orders ALTER COLUMN quantity DROP NOT NULL");

        statements.add("CREATE TABLE IF NOT EXISTS bulk_orders ("
                + "order_id VARCHAR(64) PRIMARY KEY,"
                + "product_id VARCHAR(64) NOT NULL,"
                + "order_side VARCHAR(16) NOT NULL,"
            + "bulk_order_status VARCHAR(32) NOT NULL,"
                + "quantity NUMERIC(20, 8) NOT NULL,"
                + "amount NUMERIC(20, 8) NOT NULL,"
                + "account_id VARCHAR(64) NOT NULL"
                + ")");

        statements.add("ALTER TABLE bulk_orders ADD COLUMN IF NOT EXISTS bulk_order_status VARCHAR(32) NOT NULL DEFAULT 'BULKED'");

        statements.add("CREATE TABLE IF NOT EXISTS bulk_order_mappings ("
                + "bulk_order_id VARCHAR(64) NOT NULL,"
                + "individual_order_id VARCHAR(64) NOT NULL,"
                + "sequence_no INTEGER NOT NULL,"
                + "PRIMARY KEY (bulk_order_id, individual_order_id)"
                + ")");

        return statements;
    }

    private static List<String> cleanDataStatements() {
        List<String> statements = new ArrayList<>();
        statements.add("TRUNCATE TABLE bulk_order_mappings");
        statements.add("TRUNCATE TABLE bulk_orders");
        statements.add("TRUNCATE TABLE orders");
        statements.add("TRUNCATE TABLE funds");
        statements.add("TRUNCATE TABLE accounts");
        return statements;
    }
}
