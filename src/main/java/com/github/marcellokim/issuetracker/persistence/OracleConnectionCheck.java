package com.github.marcellokim.issuetracker.persistence;

import com.github.marcellokim.issuetracker.technical.ConsoleOutput;
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
            ConsoleOutput.err(exception.getMessage());
            ConsoleOutput.err("Set connection values before running the check:");
            ConsoleOutput.err("  $env:ITS_DB_URL=\"jdbc:oracle:thin:@//localhost:1521/XEPDB1\"");
            ConsoleOutput.err("  $env:ITS_DB_USER=\"ITS_USER\"");
            ConsoleOutput.err("  $env:ITS_DB_PASSWORD=\"your_password\"");
            System.exit(2);
        } catch (SQLException exception) {
            ConsoleOutput.err("Oracle Database connection failed.");
            ConsoleOutput.err("Cause: " + exception.getMessage());
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

        ConsoleOutput.out("Oracle Database connection succeeded.");
        ConsoleOutput.out("Database: " + metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion());
        ConsoleOutput.out("Driver: " + metadata.getDriverName() + " " + metadata.getDriverVersion());
    }

    private static void printDualQueryResult(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection
                .prepareStatement("select 'ITS Oracle connection OK' as message from dual");
                ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                ConsoleOutput.out(resultSet.getString("message"));
            }
        }
    }
}
