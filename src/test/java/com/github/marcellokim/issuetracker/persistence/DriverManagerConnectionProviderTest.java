package com.github.marcellokim.issuetracker.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DriverManager connection provider")
class DriverManagerConnectionProviderTest {

    @Test
    @DisplayName("default constructor opens a plain driver manager connection without retry")
    void defaultConstructorOpensPlainDriverManagerConnectionWithoutRetry() {
        DriverManagerConnectionProvider provider = new DriverManagerConnectionProvider(
                "jdbc:unknown:issue-tracker",
                "ITS_TEST_USER",
                "secret");

        assertThrows(SQLException.class, provider::getConnection);
    }

    @Test
    @DisplayName("creates default provider from database environment")
    void createsDefaultProviderFromDatabaseEnvironment() {
        DriverManagerConnectionProvider provider = DriverManagerConnectionProvider.from(
                new DatabaseEnvironment("jdbc:unknown:issue-tracker", "ITS_TEST_USER", "secret"));

        assertThrows(SQLException.class, provider::getConnection);
        assertThrows(NullPointerException.class, () -> DriverManagerConnectionProvider.from(null));
    }

    @Test
    @DisplayName("parses integration-test environment and retries with configured settings")
    void parsesIntegrationTestEnvironmentAndRetriesWithConfiguredSettings() throws SQLException {
        AtomicInteger attempts = new AtomicInteger();
        Connection expectedConnection = connectionProxy();
        Map<String, String> environment = withIntegrationEnvironment(
                "ITS_TEST_DB_URL", "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
                "ITS_TEST_DB_CONNECT_RETRIES", "1",
                "ITS_TEST_DB_CONNECT_RETRY_DELAY_MS", "0");
        DriverManagerConnectionProvider provider = DriverManagerConnectionProvider.fromIntegrationTestEnvironment(
                environment,
                (url, user, password) -> {
                    assertEquals("jdbc:oracle:thin:@//localhost:1521/FREEPDB1", url);
                    assertEquals("ITS_TEST_USER", user);
                    assertEquals("secret", password);
                    if (attempts.incrementAndGet() == 1) {
                        throw new SQLException("listener does not currently know of service", "66000", 12514);
                    }
                    return expectedConnection;
                });

        assertSame(expectedConnection, provider.getConnection());
        assertEquals(2, attempts.get());
    }

    @Test
    @DisplayName("uses shared database URL and default retry settings when test overrides are blank")
    void usesSharedDatabaseUrlAndDefaultRetrySettingsWhenTestOverridesAreBlank() throws SQLException {
        AtomicInteger attempts = new AtomicInteger();
        Connection expectedConnection = connectionProxy();
        Map<String, String> environment = withIntegrationEnvironment(
                "ITS_TEST_DB_URL", " ",
                "ITS_DB_URL", "jdbc:oracle:thin:@//localhost:1521/XEPDB1",
                "ITS_TEST_DB_CONNECT_RETRIES", " ",
                "ITS_TEST_DB_CONNECT_RETRY_DELAY_MS", " ");
        DriverManagerConnectionProvider provider = DriverManagerConnectionProvider.fromIntegrationTestEnvironment(
                environment,
                (url, user, password) -> {
                    attempts.incrementAndGet();
                    assertEquals("jdbc:oracle:thin:@//localhost:1521/XEPDB1", url);
                    return expectedConnection;
                });

        assertSame(expectedConnection, provider.getConnection());
        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("rejects invalid integration-test retry environment settings")
    void rejectsInvalidIntegrationTestRetryEnvironmentSettings() {
        assertThrows(IllegalStateException.class, () -> DriverManagerConnectionProvider.fromIntegrationTestEnvironment(
                withIntegrationEnvironment("ITS_TEST_DB_CONNECT_RETRIES", "abc"),
                (url, user, password) -> connectionProxy()));
        assertThrows(IllegalArgumentException.class, () -> DriverManagerConnectionProvider.fromIntegrationTestEnvironment(
                withIntegrationEnvironment("ITS_TEST_DB_CONNECT_RETRIES", "-1"),
                (url, user, password) -> connectionProxy()));
        assertThrows(IllegalStateException.class, () -> DriverManagerConnectionProvider.fromIntegrationTestEnvironment(
                withIntegrationEnvironment("ITS_TEST_DB_CONNECT_RETRY_DELAY_MS", "abc"),
                (url, user, password) -> connectionProxy()));
        assertThrows(IllegalArgumentException.class, () -> DriverManagerConnectionProvider.fromIntegrationTestEnvironment(
                withIntegrationEnvironment("ITS_TEST_DB_CONNECT_RETRY_DELAY_MS", "-1"),
                (url, user, password) -> connectionProxy()));
    }

    @Test
    @DisplayName("requires integration-test database credentials")
    void requiresIntegrationTestDatabaseCredentials() {
        assertThrows(IllegalStateException.class, () -> DriverManagerConnectionProvider.fromIntegrationTestEnvironment(
                withIntegrationEnvironment("ITS_TEST_DB_USER", " "),
                (url, user, password) -> connectionProxy()));
        assertThrows(IllegalStateException.class, () -> DriverManagerConnectionProvider.fromIntegrationTestEnvironment(
                withoutIntegrationEnvironmentKey("ITS_TEST_DB_USER"),
                (url, user, password) -> connectionProxy()));
        assertThrows(IllegalStateException.class, () -> DriverManagerConnectionProvider.fromIntegrationTestEnvironment(
                withIntegrationEnvironment("ITS_TEST_DB_PASSWORD", ""),
                (url, user, password) -> connectionProxy()));
    }

    @Test
    @DisplayName("retries transient Oracle listener connection error codes")
    void retriesTransientOracleListenerConnectionErrorCodes() throws SQLException {
        AtomicInteger attempts = new AtomicInteger();
        Connection expectedConnection = connectionProxy();
        DriverManagerConnectionProvider provider = new DriverManagerConnectionProvider(
                "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
                "ITS_TEST_USER",
                "secret",
                2,
                0,
                (url, user, password) -> {
                    if (attempts.incrementAndGet() < 3) {
                        throw new SQLException("listener could not find available handler", "66000", 12516);
                    }
                    return expectedConnection;
                });

        assertSame(expectedConnection, provider.getConnection());
        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("retries transient Oracle listener messages without vendor codes")
    void retriesTransientOracleListenerMessagesWithoutVendorCodes() throws SQLException {
        AtomicInteger attempts = new AtomicInteger();
        Connection expectedConnection = connectionProxy();
        DriverManagerConnectionProvider provider = new DriverManagerConnectionProvider(
                "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
                "ITS_TEST_USER",
                "secret",
                1,
                0,
                (url, user, password) -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new SQLException("ORA-12516: listener could not find available handler");
                    }
                    return expectedConnection;
                });

        assertSame(expectedConnection, provider.getConnection());
        assertEquals(2, attempts.get());
    }

    @Test
    @DisplayName("retries transient Oracle listener messages case-insensitively")
    void retriesTransientOracleListenerMessagesCaseInsensitively() throws SQLException {
        AtomicInteger attempts = new AtomicInteger();
        Connection expectedConnection = connectionProxy();
        DriverManagerConnectionProvider provider = new DriverManagerConnectionProvider(
                "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
                "ITS_TEST_USER",
                "secret",
                1,
                0,
                (url, user, password) -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new SQLException("ora-12514: listener does not currently know of service requested");
                    }
                    return expectedConnection;
                });

        assertSame(expectedConnection, provider.getConnection());
        assertEquals(2, attempts.get());
    }

    @Test
    @DisplayName("walks exception causes to find retryable Oracle listener errors")
    void walksExceptionCausesToFindRetryableOracleListenerErrors() throws SQLException {
        AtomicInteger attempts = new AtomicInteger();
        Connection expectedConnection = connectionProxy();
        SQLException nestedRetryableException = new SQLException("ORA-12514: listener does not know service");
        SQLException wrapperException = new SQLException("connection failed");
        wrapperException.initCause(new RuntimeException(nestedRetryableException));
        DriverManagerConnectionProvider provider = new DriverManagerConnectionProvider(
                "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
                "ITS_TEST_USER",
                "secret",
                1,
                0,
                (url, user, password) -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw wrapperException;
                    }
                    return expectedConnection;
                });

        assertSame(expectedConnection, provider.getConnection());
        assertEquals(2, attempts.get());
    }

    @Test
    @DisplayName("does not retry retryable SQL exceptions after the configured budget")
    void doesNotRetryRetryableSqlExceptionsAfterConfiguredBudget() {
        AtomicInteger attempts = new AtomicInteger();
        DriverManagerConnectionProvider provider = new DriverManagerConnectionProvider(
                "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
                "ITS_TEST_USER",
                "secret",
                0,
                0,
                (url, user, password) -> {
                    attempts.incrementAndGet();
                    throw new SQLException("listener does not currently know of service", "66000", 12514);
                });

        assertThrows(SQLException.class, provider::getConnection);
        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("does not retry non-listener SQL exceptions")
    void doesNotRetryNonListenerSqlExceptions() {
        AtomicInteger attempts = new AtomicInteger();
        DriverManagerConnectionProvider provider = new DriverManagerConnectionProvider(
                "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
                "ITS_TEST_USER",
                "secret",
                3,
                0,
                (url, user, password) -> {
                    attempts.incrementAndGet();
                    throw new SQLException("ORA-01017: invalid username/password");
                });

        assertThrows(SQLException.class, provider::getConnection);
        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("does not retry SQL exceptions without retryable codes or messages")
    void doesNotRetrySqlExceptionsWithoutRetryableCodesOrMessages() {
        AtomicInteger attempts = new AtomicInteger();
        DriverManagerConnectionProvider provider = new DriverManagerConnectionProvider(
                "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
                "ITS_TEST_USER",
                "secret",
                3,
                0,
                (url, user, password) -> {
                    attempts.incrementAndGet();
                    throw new SQLException((String) null);
                });

        assertThrows(SQLException.class, provider::getConnection);
        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("waits before retry when retry delay is configured")
    void waitsBeforeRetryWhenRetryDelayIsConfigured() throws SQLException {
        AtomicInteger attempts = new AtomicInteger();
        Connection expectedConnection = connectionProxy();
        DriverManagerConnectionProvider provider = new DriverManagerConnectionProvider(
                "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
                "ITS_TEST_USER",
                "secret",
                1,
                1,
                (url, user, password) -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new SQLException("listener does not currently know of service", "66000", 12516);
                    }
                    return expectedConnection;
                });

        assertSame(expectedConnection, provider.getConnection());
        assertEquals(2, attempts.get());
    }

    @Test
    @DisplayName("wraps interruptions while waiting before retry")
    void wrapsInterruptionsWhileWaitingBeforeRetry() {
        AtomicInteger attempts = new AtomicInteger();
        DriverManagerConnectionProvider provider = new DriverManagerConnectionProvider(
                "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
                "ITS_TEST_USER",
                "secret",
                1,
                1_000,
                (url, user, password) -> {
                    attempts.incrementAndGet();
                    throw new SQLException("listener does not currently know of service", "66000", 12516);
                });

        Thread.currentThread().interrupt();
        try {
            SQLException exception = assertThrows(SQLException.class, provider::getConnection);

            assertEquals("Interrupted while waiting to retry Oracle connection.", exception.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, attempts.get());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("rejects negative retry settings")
    void rejectsNegativeRetrySettings() {
        assertThrows(IllegalArgumentException.class, () -> new DriverManagerConnectionProvider(
                "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
                "ITS_TEST_USER",
                "secret",
                -1,
                0,
                (url, user, password) -> connectionProxy()));
        assertThrows(IllegalArgumentException.class, () -> new DriverManagerConnectionProvider(
                "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
                "ITS_TEST_USER",
                "secret",
                0,
                -1,
                (url, user, password) -> connectionProxy()));
    }

    private static Map<String, String> withIntegrationEnvironment(String... entries) {
        Map<String, String> environment = new HashMap<>();
        environment.put("ITS_TEST_DB_USER", "ITS_TEST_USER");
        environment.put("ITS_TEST_DB_PASSWORD", "secret");
        for (int index = 0; index < entries.length; index += 2) {
            environment.put(entries[index], entries[index + 1]);
        }
        return environment;
    }

    private static Map<String, String> withoutIntegrationEnvironmentKey(String key) {
        Map<String, String> environment = withIntegrationEnvironment();
        environment.remove(key);
        return environment;
    }

    private static Connection connectionProxy() {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    if ("toString".equals(method.getName())) {
                        return "connection-proxy";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
