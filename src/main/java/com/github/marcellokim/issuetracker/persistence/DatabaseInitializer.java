package com.github.marcellokim.issuetracker.persistence;

import com.github.marcellokim.issuetracker.technical.ConsoleOutput;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class DatabaseInitializer {

    private static final List<String> ORACLE_CORE_TABLES = List.of(
            "users",
            "user_credentials",
            "projects",
            "project_members",
            "issues",
            "comments",
            "issue_history",
            "issue_dependencies");
    private static final List<RequiredColumn> ORACLE_REQUIRED_COLUMNS = List.of(
            new RequiredColumn("users", "login_id"),
            new RequiredColumn("users", "name"),
            new RequiredColumn("users", "role"),
            new RequiredColumn("users", "active"),
            new RequiredColumn("users", "created_at"),
            new RequiredColumn("users", "updated_at"),
            new RequiredColumn("user_credentials", "login_id"),
            new RequiredColumn("user_credentials", "password_salt"),
            new RequiredColumn("user_credentials", "password_hash"),
            new RequiredColumn("user_credentials", "updated_at"),
            new RequiredColumn("projects", "id"),
            new RequiredColumn("projects", "name"),
            new RequiredColumn("projects", "description"),
            new RequiredColumn("projects", "managed_by_login_id"),
            new RequiredColumn("projects", "created_at"),
            new RequiredColumn("projects", "updated_at"),
            new RequiredColumn("project_members", "project_id"),
            new RequiredColumn("project_members", "user_login_id"),
            new RequiredColumn("project_members", "joined_at"),
            new RequiredColumn("issues", "id"),
            new RequiredColumn("issues", "project_id"),
            new RequiredColumn("issues", "issue_id"),
            new RequiredColumn("issues", "title"),
            new RequiredColumn("issues", "description"),
            new RequiredColumn("issues", "reported_at"),
            new RequiredColumn("issues", "priority"),
            new RequiredColumn("issues", "status"),
            new RequiredColumn("issues", "reporter_login_id"),
            new RequiredColumn("issues", "assignee_login_id"),
            new RequiredColumn("issues", "verifier_login_id"),
            new RequiredColumn("issues", "fixer_login_id"),
            new RequiredColumn("issues", "resolver_login_id"),
            new RequiredColumn("issues", "updated_at"),
            new RequiredColumn("comments", "id"),
            new RequiredColumn("comments", "issue_id"),
            new RequiredColumn("comments", "writer_login_id"),
            new RequiredColumn("comments", "content"),
            new RequiredColumn("comments", "purpose"),
            new RequiredColumn("comments", "created_at"),
            new RequiredColumn("comments", "updated_at"),
            new RequiredColumn("issue_history", "id"),
            new RequiredColumn("issue_history", "issue_id"),
            new RequiredColumn("issue_history", "changed_by_login_id"),
            new RequiredColumn("issue_history", "action_type"),
            new RequiredColumn("issue_history", "previous_value"),
            new RequiredColumn("issue_history", "new_value"),
            new RequiredColumn("issue_history", "message"),
            new RequiredColumn("issue_history", "changed_at"),
            new RequiredColumn("issue_dependencies", "id"),
            new RequiredColumn("issue_dependencies", "dependency_id"),
            new RequiredColumn("issue_dependencies", "blocking_issue_id"),
            new RequiredColumn("issue_dependencies", "blocked_issue_id"),
            new RequiredColumn("issue_dependencies", "discovered_at"));
    private static final List<String> ORACLE_DROP_STATEMENTS = List.of(
            "drop table issue_dependencies cascade constraints purge",
            "drop table issue_history cascade constraints purge",
            "drop table comments cascade constraints purge",
            "drop table issues cascade constraints purge",
            "drop table project_members cascade constraints purge",
            "drop table projects cascade constraints purge",
            "drop table user_credentials cascade constraints purge",
            "drop table users cascade constraints purge");
    private static final String ORACLE_SCHEMA_SCRIPT = "db/oracle/schema-oracle.sql";
    private static final String ORACLE_SEED_SCRIPT = "db/oracle/seed-oracle.sql";
    private static final String RESET_FIXED_SEED_ARGUMENT = "--reset-fixed-seed";

    private DatabaseInitializer() {
    }

    public static void main(String[] args) {
        try {
            if (usesFixedSeedReset(args)) {
                resetWithFixedSeed(DriverManagerConnectionProvider.fromEnvironment());
                ConsoleOutput.out("Oracle schema reset and fixed seed completed.");
            } else {
                initializeApplication(DriverManagerConnectionProvider.fromEnvironment());
                ConsoleOutput.out("Oracle application database check completed.");
            }
        } catch (IllegalStateException exception) {
            ConsoleOutput.err(exception.getMessage());
            ConsoleOutput.err("Set connection values before running init:");
            ConsoleOutput.err("  $env:ITS_DB_URL=\"jdbc:oracle:thin:@//localhost:1521/XEPDB1\"");
            ConsoleOutput.err("  $env:ITS_DB_USER=\"ITS_USER\"");
            ConsoleOutput.err("  $env:ITS_DB_PASSWORD=\"your_password\"");
            System.exit(2);
        } catch (SQLException | IOException exception) {
            ConsoleOutput.err("Oracle schema init and seed failed.");
            ConsoleOutput.err("Cause: " + exception.getMessage());
            System.exit(1);
        }
    }

    public static void initialize(DatabaseConnectionProvider connectionProvider) throws SQLException, IOException {
        initializeApplication(connectionProvider);
    }

    public static void initializeApplication(DatabaseConnectionProvider connectionProvider)
            throws SQLException, IOException {
        Objects.requireNonNull(connectionProvider, "connectionProvider");

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean firstRun = prepareApplicationSchema(connection);
                if (firstRun) {
                    runScript(connection, ORACLE_SEED_SCRIPT);
                } else {
                    ConsoleOutput.out("Oracle application data reused without seed reset.");
                }
                connection.commit();
            } catch (SQLException | IOException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public static void resetWithFixedSeed(DatabaseConnectionProvider connectionProvider)
            throws SQLException, IOException {
        Objects.requireNonNull(connectionProvider, "connectionProvider");

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                dropSchemaObjects(connection);
                runScript(connection, ORACLE_SCHEMA_SCRIPT);
                validateRequiredSchemaContract(connection);
                runScript(connection, ORACLE_SEED_SCRIPT);
                connection.commit();
            } catch (SQLException | IOException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static boolean usesFixedSeedReset(String[] args) {
        return Arrays.asList(args).contains(RESET_FIXED_SEED_ARGUMENT);
    }

    private static boolean prepareApplicationSchema(Connection connection) throws SQLException, IOException {
        boolean firstRun = !anyCoreTableExists(connection);
        if (!firstRun && !coreTablesExist(connection)) {
            throw new IllegalStateException(
                    "Oracle schema is incomplete. Run oracleResetFixedSeed to reset the development/test schema.");
        }

        runScript(connection, ORACLE_SCHEMA_SCRIPT);
        validateRequiredSchemaContract(connection);
        return firstRun;
    }

    private static void validateRequiredSchemaContract(Connection connection) throws SQLException {
        List<String> missingColumns = new ArrayList<>();
        for (RequiredColumn column : ORACLE_REQUIRED_COLUMNS) {
            if (!columnExists(connection, column.tableName(), column.columnName())) {
                missingColumns.add(column.tableName() + "." + column.columnName());
            }
        }

        if (!missingColumns.isEmpty()) {
            throw new IllegalStateException(
                    "Oracle schema is incompatible. Missing required columns: "
                            + String.join(", ", missingColumns)
                            + ". Run oracleResetFixedSeed to reset the development/test schema.");
        }
    }

    private static boolean coreTablesExist(Connection connection) throws SQLException {
        for (String tableName : ORACLE_CORE_TABLES) {
            if (!tableExists(connection, tableName)) {
                return false;
            }
        }
        return true;
    }

    private static boolean anyCoreTableExists(Connection connection) throws SQLException {
        for (String tableName : ORACLE_CORE_TABLES) {
            if (tableExists(connection, tableName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        String sql = """
                select count(*)
                from user_tables
                where table_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName.toUpperCase(Locale.ROOT));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        String sql = """
                select count(*)
                from user_tab_columns
                where table_name = ?
                  and column_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName.toUpperCase(Locale.ROOT));
            statement.setString(2, columnName.toUpperCase(Locale.ROOT));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static void dropSchemaObjects(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String dropStatement : ORACLE_DROP_STATEMENTS) {
                try {
                    statement.executeUpdate(dropStatement);
                } catch (SQLException exception) {
                    if (exception.getErrorCode() != 942) {
                        throw exception;
                    }
                }
            }
        }
    }

    private static void runScript(Connection connection, String resourcePath) throws IOException, SQLException {
        List<String> blocks = readDelimitedBlocks(resourcePath);
        int executedBlocks = 0;

        for (String block : blocks) {
            if (block.isBlank()) {
                continue;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(block);
                executedBlocks++;
            }
        }

        ConsoleOutput.out("Executed " + executedBlocks + " blocks from " + resourcePath);
    }

    private static List<String> readDelimitedBlocks(String resourcePath) throws IOException {
        var classLoader = DatabaseInitializer.class.getClassLoader();
        var resource = classLoader.getResourceAsStream(resourcePath);
        if (resource == null) {
            throw new IOException("Classpath resource not found: " + resourcePath);
        }

        List<String> blocks = new ArrayList<>();
        StringBuilder currentBlock = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("/")) {
                    blocks.add(currentBlock.toString().trim());
                    currentBlock.setLength(0);
                } else {
                    currentBlock.append(line).append(System.lineSeparator());
                }
            }
        }

        if (!currentBlock.toString().isBlank()) {
            blocks.add(currentBlock.toString().trim());
        }

        return blocks;
    }

    private record RequiredColumn(String tableName, String columnName) {
    }
}
