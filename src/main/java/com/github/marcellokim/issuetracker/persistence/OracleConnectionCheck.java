package com.github.marcellokim.issuetracker.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class OracleConnectionCheck {

    private static final String DEFAULT_URL = "jdbc:oracle:thin:@//localhost:1521/FREEPDB1";

    private OracleConnectionCheck() {
    }

    public static void main(String[] args) {
        try {
            DatabaseSettings settings = DatabaseSettings.fromEnvironment();
            checkConnection(settings);
        } catch (IllegalStateException exception) {
            System.err.println(exception.getMessage());
            System.err.println("Set connection values before running the check:");
            System.err.println("  $env:ITS_DB_URL=\"jdbc:oracle:thin:@//localhost:1521/FREEPDB1\"");
            System.err.println("  $env:ITS_DB_USER=\"its_user\"");
            System.err.println("  $env:ITS_DB_PASSWORD=\"your_password\"");
            System.exit(2);
        } catch (SQLException exception) {
            System.err.println("Oracle Database connection failed.");
            System.err.println("Cause: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void checkConnection(DatabaseSettings settings) throws SQLException {
        try (Connection connection = DriverManager.getConnection(settings.url(), settings.user(),
                settings.password())) {
            printConnectionMetadata(connection);
            printDualQueryResult(connection);
        }
    }

    private static void printConnectionMetadata(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();

        System.out.println("Oracle Database connection succeeded.");
        System.out
                .println("Database: " + metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion());
        System.out.println("Driver: " + metadata.getDriverName() + " " + metadata.getDriverVersion());
    }

    private static void printDualQueryResult(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection
                .prepareStatement("select 'ITS Oracle connection OK' as message from dual");
                ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                System.out.println(resultSet.getString("message"));
            }
        }
    }

    private record DatabaseSettings(String url, String user, String password) {

        static DatabaseSettings fromEnvironment() {
            String url = readEnvironment("ITS_DB_URL", DEFAULT_URL);
            String user = readRequiredEnvironment("ITS_DB_USER");
            String password = readRequiredEnvironment("ITS_DB_PASSWORD");
            return new DatabaseSettings(url, user, password);
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
}
