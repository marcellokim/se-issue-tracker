package com.github.marcellokim.issuetracker.persistence.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

final class JdbcSupport {

    private JdbcSupport() {
    }

    static Long nullableLong(ResultSet resultSet, String columnLabel) throws SQLException {
        long value = resultSet.getLong(columnLabel);
        if (resultSet.wasNull()) {
            return null;
        }
        return value;
    }

    static LocalDateTime nullableDateTime(ResultSet resultSet, String columnLabel) throws SQLException {
        Timestamp value = resultSet.getTimestamp(columnLabel);
        if (value == null) {
            return null;
        }
        return value.toLocalDateTime();
    }

    static void setNullableLong(PreparedStatement statement, int parameterIndex, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.NUMERIC);
        } else {
            statement.setLong(parameterIndex, value);
        }
    }

    static void setNullableString(PreparedStatement statement, int parameterIndex, String value) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.VARCHAR);
        } else {
            statement.setString(parameterIndex, value);
        }
    }

    static void setNullableTimestamp(
            PreparedStatement statement,
            int parameterIndex,
            LocalDateTime value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(parameterIndex, Timestamp.valueOf(value));
        }
    }

    static long generatedId(Statement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("No generated id returned.");
        }
    }

    static PreparedStatement prepareInsertReturningId(Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql, new String[] {"ID"});
    }
}
