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
                
        statements.add("ALTER TABLE accounts ADD COLUMN IF NOT EXISTS cash_balance NUMERIC(20, 8) DEFAULT 1000000.00");

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
        statements.add("ALTER TABLE orders ADD COLUMN IF NOT EXISTS transfer_agent VARCHAR(16)");
        statements.add("ALTER TABLE orders ADD COLUMN IF NOT EXISTS fund_family VARCHAR(255)");
        statements.add("ALTER TABLE orders ADD COLUMN IF NOT EXISTS trade_date VARCHAR(16)");
        statements.add("ALTER TABLE orders ADD COLUMN IF NOT EXISTS settlement_date VARCHAR(16)");
        statements.add("ALTER TABLE orders ADD COLUMN IF NOT EXISTS contract_ref VARCHAR(128)");
        statements.add("ALTER TABLE orders ADD COLUMN IF NOT EXISTS nav NUMERIC(20, 8)");
        statements.add("ALTER TABLE orders ADD COLUMN IF NOT EXISTS allocated_shares NUMERIC(20, 8)");

        statements.add("ALTER TABLE funds ADD COLUMN IF NOT EXISTS transfer_agent VARCHAR(16) NOT NULL DEFAULT 'NSCC'");
        statements.add("ALTER TABLE funds ADD COLUMN IF NOT EXISTS is_offshore BOOLEAN NOT NULL DEFAULT FALSE");

        statements.add("CREATE TABLE IF NOT EXISTS bulk_orders ("
                + "order_id VARCHAR(64) PRIMARY KEY,"
                + "product_id VARCHAR(64) NOT NULL,"
                + "order_side VARCHAR(16) NOT NULL,"
                + "bulk_order_status VARCHAR(32) NOT NULL DEFAULT 'BULKED',"
                + "quantity NUMERIC(20, 8) NOT NULL,"
                + "amount NUMERIC(20, 8) NOT NULL,"
                + "account_id VARCHAR(64) NOT NULL,"
                + "transfer_agent VARCHAR(16),"
                + "transmission_ref VARCHAR(128),"
                + "contract_ref VARCHAR(128),"
                + "bulk_nav NUMERIC(20, 8)"
                + ")");

        statements.add(
                "ALTER TABLE bulk_orders ADD COLUMN IF NOT EXISTS bulk_order_status VARCHAR(32) NOT NULL DEFAULT 'BULKED'");
        statements.add("ALTER TABLE bulk_orders ADD COLUMN IF NOT EXISTS transfer_agent VARCHAR(16)");
        statements.add("ALTER TABLE bulk_orders ADD COLUMN IF NOT EXISTS transmission_ref VARCHAR(128)");
        statements.add("ALTER TABLE bulk_orders ADD COLUMN IF NOT EXISTS contract_ref VARCHAR(128)");
        statements.add("ALTER TABLE bulk_orders ADD COLUMN IF NOT EXISTS bulk_nav NUMERIC(20, 8)");

        statements.add("CREATE TABLE IF NOT EXISTS order_audit_log ("
                + "id BIGSERIAL PRIMARY KEY,"
                + "order_id VARCHAR(64) NOT NULL,"
                + "from_status VARCHAR(32),"
                + "to_status VARCHAR(32) NOT NULL,"
                + "occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
                + "actor VARCHAR(100),"
                + "details TEXT"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS transmission_log ("
                + "id BIGSERIAL PRIMARY KEY,"
                + "bulk_order_id VARCHAR(64) NOT NULL,"
                + "transfer_agent VARCHAR(16) NOT NULL,"
                + "transmission_ref VARCHAR(128) NOT NULL,"
                + "transmitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
                + "status VARCHAR(32) NOT NULL DEFAULT 'SENT'"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS bulk_order_mappings ("
                + "bulk_order_id VARCHAR(64) NOT NULL,"
                + "individual_order_id VARCHAR(64) NOT NULL,"
                + "sequence_no INTEGER NOT NULL,"
                + "PRIMARY KEY (bulk_order_id, individual_order_id)"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS advisors ("
                + "advisor_id VARCHAR(64) PRIMARY KEY,"
                + "name VARCHAR(255) NOT NULL,"
                + "email VARCHAR(255)"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS advisor_client_relationships ("
                + "advisor_id VARCHAR(64) NOT NULL,"
                + "account_id VARCHAR(64) NOT NULL,"
                + "relationship_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',"
                + "PRIMARY KEY (advisor_id, account_id)"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS reconciliation_breaks ("
                + "break_id VARCHAR(64) PRIMARY KEY,"
                + "bulk_order_id VARCHAR(64),"
                + "break_type VARCHAR(32),"
                + "expected_value VARCHAR(255),"
                + "received_value VARCHAR(255),"
                + "detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
                + "resolved BOOLEAN DEFAULT FALSE,"
                + "escalated BOOLEAN DEFAULT FALSE"
                + ")");

        return statements;
    }

    private static List<String> cleanDataStatements() {
        List<String> statements = new ArrayList<>();
        // Use CASCADE to handle any FK dependencies safely
        statements.add("TRUNCATE TABLE transmission_log CASCADE");
        statements.add("TRUNCATE TABLE order_audit_log CASCADE");
        statements.add("TRUNCATE TABLE bulk_order_mappings CASCADE");
        statements.add("TRUNCATE TABLE bulk_orders CASCADE");
        statements.add("TRUNCATE TABLE orders CASCADE");
        statements.add("TRUNCATE TABLE funds CASCADE");
        statements.add("TRUNCATE TABLE advisor_client_relationships CASCADE");
        statements.add("TRUNCATE TABLE advisors CASCADE");
        statements.add("TRUNCATE TABLE accounts CASCADE");
        statements.add("TRUNCATE TABLE reconciliation_breaks CASCADE");
        return statements;
    }
}
