package com.github.marcellokim.issuetracker.persistence.jdbc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JDBC issue write support")
class JdbcIssueWriteSupportTest {

    @Test
    @DisplayName("failure cleanup preserves the original repository failure")
    void failureCleanupPreservesOriginalRepositoryFailure() {
        var writes = new JdbcIssueWriteSupport();
        Connection connection = failingCleanupConnection();

        assertDoesNotThrow(() -> writes.rollbackPreservingOriginalFailure(connection));
        assertDoesNotThrow(() -> writes.restoreAutoCommitPreservingOriginalFailure(connection, true));
        assertDoesNotThrow(() -> writes.restoreAutoCommitAfterTransaction(connection, true, false));
        assertThrows(SQLException.class, () -> writes.restoreAutoCommitAfterTransaction(connection, true, true));
    }

    private static Connection failingCleanupConnection() {
        return (Connection) Proxy.newProxyInstance(
                JdbcIssueWriteSupportTest.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    if ("rollback".equals(method.getName()) || "setAutoCommit".equals(method.getName())) {
                        throw new SQLException("cleanup failed");
                    }
                    if ("toString".equals(method.getName())) {
                        return "FailingCleanupConnection";
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
