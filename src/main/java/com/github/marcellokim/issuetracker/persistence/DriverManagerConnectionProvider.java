package com.github.marcellokim.issuetracker.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public final class DriverManagerConnectionProvider implements DatabaseConnectionProvider {

    public static final String DEFAULT_ORACLE_URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1";

    private final String url;
    private final String user;
    private final String password;

    public DriverManagerConnectionProvider(String url, String user, String password) {
        this.url = Objects.requireNonNull(url, "url");
        this.user = Objects.requireNonNull(user, "user");
        this.password = Objects.requireNonNull(password, "password");
    }

    public static DriverManagerConnectionProvider fromEnvironment() {
        String url = readEnvironment("ITS_DB_URL", DEFAULT_ORACLE_URL);
        String user = readRequiredEnvironment("ITS_DB_USER");
        String password = readRequiredEnvironment("ITS_DB_PASSWORD");
        return new DriverManagerConnectionProvider(url, user, password);
    }

    public static DriverManagerConnectionProvider fromIntegrationTestEnvironment() {
        String url = readEnvironment("ITS_TEST_DB_URL", readEnvironment("ITS_DB_URL", DEFAULT_ORACLE_URL));
        String user = readRequiredEnvironment("ITS_TEST_DB_USER");
        String password = readRequiredEnvironment("ITS_TEST_DB_PASSWORD");
        return new DriverManagerConnectionProvider(url, user, password);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private static String readEnvironment(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static String readRequiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " environment variable is required.");
        }
        return value;
    }
}
