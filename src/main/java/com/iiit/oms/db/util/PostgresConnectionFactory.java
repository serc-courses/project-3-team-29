package com.iiit.oms.db.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgresConnectionFactory {
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/oms";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "postgres";

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public PostgresConnectionFactory(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public static PostgresConnectionFactory fromEnvironment() {
        String url = getOrDefault("OMS_DB_URL", DEFAULT_URL);
        String user = getOrDefault("OMS_DB_USER", DEFAULT_USER);
        String pass = getOrDefault("OMS_DB_PASSWORD", DEFAULT_PASSWORD);
        return new PostgresConnectionFactory(url, user, pass);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private static String getOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
