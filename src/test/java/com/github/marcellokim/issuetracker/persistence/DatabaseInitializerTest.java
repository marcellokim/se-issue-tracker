package com.github.marcellokim.issuetracker.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Database initializer")
class DatabaseInitializerTest {

    private static final Set<String> ALL_CORE_TABLES = Set.of(
            "USERS",
            "USER_CREDENTIALS",
            "PROJECTS",
            "PROJECT_MEMBERS",
            "ISSUES",
            "COMMENTS",
            "ISSUE_HISTORY",
            "ISSUE_DEPENDENCIES");

    @Test
    @DisplayName("application initialization runs schema migrations and reuses existing data")
    void applicationInitializationRunsSchemaAndSkipsSeedForExistingSchema() throws SQLException, IOException {
        RecordingConnectionProvider provider = new RecordingConnectionProvider(ALL_CORE_TABLES);

        DatabaseInitializer.initializeApplication(provider);

        assertTrue(provider.executedSqlContaining("create table users"));
        assertFalse(provider.executedSqlContaining("merge into users"));
        assertFalse(provider.updatedSqlContaining("drop table"));
    }

    @Test
    @DisplayName("application initialization seeds only an empty schema")
    void applicationInitializationSeedsOnlyFirstRun() throws SQLException, IOException {
        RecordingConnectionProvider provider = new RecordingConnectionProvider(Set.of());

        DatabaseInitializer.initializeApplication(provider);

        assertTrue(provider.executedSqlContaining("create table users"));
        assertTrue(provider.executedSqlContaining("merge into users"));
        assertFalse(provider.updatedSqlContaining("drop table"));
    }

    @Test
    @DisplayName("application initialization rejects incomplete schemas before scripts run")
    void applicationInitializationRejectsIncompleteSchema() {
        RecordingConnectionProvider provider = new RecordingConnectionProvider(Set.of("USERS"));

        assertThrows(IllegalStateException.class, () -> DatabaseInitializer.initializeApplication(provider));
        assertFalse(provider.executedSqlContaining("create table users"));
        assertFalse(provider.updatedSqlContaining("drop table"));
    }

    @Test
    @DisplayName("fixed seed reset drops objects before schema and seed scripts")
    void fixedSeedResetDropsObjectsBeforeSchemaAndSeed() throws SQLException, IOException {
        RecordingConnectionProvider provider = new RecordingConnectionProvider(ALL_CORE_TABLES);

        DatabaseInitializer.resetWithFixedSeed(provider);

        assertTrue(provider.firstEventContains("update:drop table issue_dependencies"));
        assertTrue(provider.updatedSqlContaining("drop table users"));
        assertTrue(provider.executedSqlContaining("create table users"));
        assertTrue(provider.executedSqlContaining("merge into users"));
    }

    private static final class RecordingConnectionProvider implements DatabaseConnectionProvider {

        private final Set<String> existingTables;
        private final List<String> events = new ArrayList<>();

        private RecordingConnectionProvider(Set<String> existingTables) {
            this.existingTables = existingTables;
        }

        @Override
        public Connection getConnection() {
            InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
                case "createStatement" -> statement();
                case "prepareStatement" -> preparedStatement((String) args[0]);
                case "setAutoCommit", "commit", "rollback", "close" -> null;
                default -> defaultValue(method);
            };
            return proxy(Connection.class, handler);
        }

        private boolean executedSqlContaining(String fragment) {
            return eventContaining("execute:", fragment);
        }

        private boolean updatedSqlContaining(String fragment) {
            return eventContaining("update:", fragment);
        }

        private boolean firstEventContains(String fragment) {
            return !events.isEmpty() && events.getFirst().contains(fragment);
        }

        private boolean eventContaining(String prefix, String fragment) {
            String expected = fragment.toLowerCase(Locale.ROOT);
            return events.stream()
                    .filter(event -> event.startsWith(prefix))
                    .anyMatch(event -> event.contains(expected));
        }

        private Statement statement() {
            InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
                case "execute" -> {
                    events.add("execute:" + normalize((String) args[0]));
                    yield false;
                }
                case "executeUpdate" -> {
                    events.add("update:" + normalize((String) args[0]));
                    yield 0;
                }
                case "close" -> null;
                default -> defaultValue(method);
            };
            return proxy(Statement.class, handler);
        }

        private PreparedStatement preparedStatement(String sql) {
            class PreparedStatementState {
                String tableName;
            }
            PreparedStatementState state = new PreparedStatementState();
            InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
                case "setString" -> {
                    state.tableName = ((String) args[1]).toUpperCase(Locale.ROOT);
                    yield null;
                }
                case "executeQuery" -> resultSet(existingTables.contains(state.tableName) ? 1 : 0);
                case "close" -> null;
                default -> defaultValue(method);
            };
            return proxy(PreparedStatement.class, handler);
        }

        private static ResultSet resultSet(int count) {
            class ResultSetState {
                boolean next = true;
            }
            ResultSetState state = new ResultSetState();
            InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
                case "next" -> {
                    boolean result = state.next;
                    state.next = false;
                    yield result;
                }
                case "getInt" -> count;
                case "close" -> null;
                default -> defaultValue(method);
            };
            return proxy(ResultSet.class, handler);
        }

        private static String normalize(String sql) {
            return sql.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        }

        private static Object defaultValue(Method method) {
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            return null;
        }

        private static <T> T proxy(Class<T> type, InvocationHandler handler) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
        }
    }
}
