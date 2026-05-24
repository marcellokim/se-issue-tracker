package com.github.marcellokim.issuetracker.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DriverManagerConnectionProvider implements DatabaseConnectionProvider {

    public static final String DEFAULT_ORACLE_URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1";
    private static final List<String> RETRYABLE_ORACLE_ERRORS = List.of(
            "ORA-12514",
            "ORA-12516",
            "ORA-12519",
            "ORA-12520");
    private static final Set<Integer> RETRYABLE_ORACLE_ERROR_CODES = Set.of(12514, 12516, 12519, 12520);

    private final String url;
    private final String user;
    private final String password;
    private final int connectionRetries;
    private final long retryDelayMillis;
    private final ConnectionOpener connectionOpener;

    public DriverManagerConnectionProvider(String url, String user, String password) {
        this(url, user, password, 0, 0, DriverManager::getConnection);
    }

    DriverManagerConnectionProvider(
            String url,
            String user,
            String password,
            int connectionRetries,
            long retryDelayMillis,
            ConnectionOpener connectionOpener) {
        this.url = Objects.requireNonNull(url, "url");
        this.user = Objects.requireNonNull(user, "user");
        this.password = Objects.requireNonNull(password, "password");
        this.connectionRetries = requireNonNegative(connectionRetries, "connectionRetries");
        this.retryDelayMillis = requireNonNegative(retryDelayMillis, "retryDelayMillis");
        this.connectionOpener = Objects.requireNonNull(connectionOpener, "connectionOpener");
    }

    public static DriverManagerConnectionProvider fromEnvironment() {
        return from(DatabaseEnvironment.fromSystem());
    }

    public static DriverManagerConnectionProvider from(DatabaseEnvironment environment) {
        Objects.requireNonNull(environment, "environment");
        return new DriverManagerConnectionProvider(environment.url(), environment.user(), environment.password());
    }

    public static DriverManagerConnectionProvider fromIntegrationTestEnvironment() {
        return fromIntegrationTestEnvironment(System.getenv(), DriverManager::getConnection);
    }

    static DriverManagerConnectionProvider fromIntegrationTestEnvironment(
            Map<String, String> environment,
            ConnectionOpener connectionOpener) {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(connectionOpener, "connectionOpener");
        String url = readEnvironment(environment, "ITS_TEST_DB_URL",
                readEnvironment(environment, "ITS_DB_URL", DEFAULT_ORACLE_URL));
        String user = readRequiredEnvironment(environment, "ITS_TEST_DB_USER");
        String password = readRequiredEnvironment(environment, "ITS_TEST_DB_PASSWORD");
        int connectionRetries = readNonNegativeIntegerEnvironment(environment, "ITS_TEST_DB_CONNECT_RETRIES", 0);
        long retryDelayMillis = readNonNegativeLongEnvironment(environment, "ITS_TEST_DB_CONNECT_RETRY_DELAY_MS", 0);
        return new DriverManagerConnectionProvider(
                url,
                user,
                password,
                connectionRetries,
                retryDelayMillis,
                connectionOpener);
    }

    @Override
    public Connection getConnection() throws SQLException {
        int attempt = 1;
        while (true) {
            try {
                return connectionOpener.open(url, user, password);
            } catch (SQLException exception) {
                if (attempt > connectionRetries || !isRetryableConnectionException(exception)) {
                    throw exception;
                }
                sleepBeforeRetry();
                attempt++;
            }
        }
    }

    private static String readEnvironment(Map<String, String> environment, String name, String defaultValue) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static String readRequiredEnvironment(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " environment variable is required.");
        }
        return value;
    }

    private static int readNonNegativeIntegerEnvironment(
            Map<String, String> environment,
            String name,
            int defaultValue) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return requireNonNegative(Integer.parseInt(value), name);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(name + " must be a non-negative integer.", exception);
        }
    }

    private static long readNonNegativeLongEnvironment(
            Map<String, String> environment,
            String name,
            long defaultValue) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return requireNonNegative(Long.parseLong(value), name);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(name + " must be a non-negative integer.", exception);
        }
    }

    private static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative.");
        }
        return value;
    }

    private static long requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative.");
        }
        return value;
    }

    private static boolean isRetryableConnectionException(SQLException exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException && isRetryableOracleError(sqlException)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRetryableOracleError(SQLException exception) {
        if (RETRYABLE_ORACLE_ERROR_CODES.contains(exception.getErrorCode())) {
            return true;
        }
        String message = exception.getMessage();
        return message != null && RETRYABLE_ORACLE_ERRORS.stream().anyMatch(message::contains);
    }

    private void sleepBeforeRetry() throws SQLException {
        if (retryDelayMillis == 0) {
            return;
        }
        try {
            Thread.sleep(retryDelayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting to retry Oracle connection.", exception);
        }
    }

    @FunctionalInterface
    interface ConnectionOpener {

        Connection open(String url, String user, String password) throws SQLException;
    }
}
