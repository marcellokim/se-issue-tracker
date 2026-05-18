package com.github.marcellokim.issuetracker.persistence;

import com.github.marcellokim.issuetracker.technical.ConsoleOutput;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DatabaseInitializer {

    private static final List<String> ORACLE_INIT_SCRIPTS = List.of(
            "db/oracle/schema-oracle.sql",
            "db/oracle/seed-oracle.sql"
    );

    private DatabaseInitializer() {
    }

    public static void main(String[] args) {
        try {
            initialize(DriverManagerConnectionProvider.fromEnvironment());
            ConsoleOutput.out("Oracle schema init and seed completed.");
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
        Objects.requireNonNull(connectionProvider, "connectionProvider");

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (String script : ORACLE_INIT_SCRIPTS) {
                    runScript(connection, script);
                }
                connection.commit();
            } catch (SQLException | IOException exception) {
                connection.rollback();
                throw exception;
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
