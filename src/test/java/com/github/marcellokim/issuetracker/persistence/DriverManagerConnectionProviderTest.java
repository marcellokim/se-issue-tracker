package com.github.marcellokim.issuetracker.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DriverManager connection provider")
class DriverManagerConnectionProviderTest {

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
