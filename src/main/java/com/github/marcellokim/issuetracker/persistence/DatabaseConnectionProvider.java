package com.github.marcellokim.issuetracker.persistence;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface DatabaseConnectionProvider {

    Connection getConnection() throws SQLException;
}
