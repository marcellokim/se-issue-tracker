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
    private static final List<String> ORACLE_DROP_ORDER = List.of(
            "issue_dependencies",
            "issue_history",
            "comments",
            "issues",
            "project_members",
            "projects",
            "user_credentials",
            "users");
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
                boolean schemaCreated = ensureApplicationSchema(connection);
                if (schemaCreated) {
                    runScript(connection, ORACLE_SEED_SCRIPT);
                } else {
                    ConsoleOutput.out("Oracle application data reused without seed reset.");
                }
                connection.commit();
            } catch (SQLException | IOException exception) {
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
                runScript(connection, ORACLE_SEED_SCRIPT);
                connection.commit();
            } catch (SQLException | IOException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static boolean usesFixedSeedReset(String[] args) {
        return Arrays.asList(args).contains(RESET_FIXED_SEED_ARGUMENT);
    }

    private static boolean ensureApplicationSchema(Connection connection) throws SQLException, IOException {
        if (coreTablesExist(connection)) {
            ConsoleOutput.out("Oracle schema already exists; existing schema reused.");
            return false;
        }

        if (anyCoreTableExists(connection)) {
            throw new IllegalStateException(
                    "Oracle schema is incomplete. Run oracleInitializeDatabase to reset the development/test schema.");
        }

        runScript(connection, ORACLE_SCHEMA_SCRIPT);
        return true;
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

    private static void dropSchemaObjects(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String tableName : ORACLE_DROP_ORDER) {
                try {
                    statement.executeUpdate("drop table " + tableName + " cascade constraints purge");
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
}
