package com.github.marcellokim.issuetracker.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class OracleConnectionCheck {

    private OracleConnectionCheck() {
    }

    public static void main(String[] args) {
        try {
            checkConnection(DriverManagerConnectionProvider.fromEnvironment());
        } catch (IllegalStateException exception) {
            System.err.println(exception.getMessage());
            System.err.println("Set connection values before running the check:");
            System.err.println("  $env:ITS_DB_URL=\"jdbc:oracle:thin:@//localhost:1521/XEPDB1\"");
            System.err.println("  $env:ITS_DB_USER=\"ITS_USER\"");
            System.err.println("  $env:ITS_DB_PASSWORD=\"your_password\"");
            System.exit(2);
        } catch (SQLException exception) {
            System.err.println("Oracle Database connection failed.");
            System.err.println("Cause: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void checkConnection(DatabaseConnectionProvider connectionProvider) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
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
}
