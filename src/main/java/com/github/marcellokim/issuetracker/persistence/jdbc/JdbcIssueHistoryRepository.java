package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.IssueHistoryRepository;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcIssueHistoryRepository implements IssueHistoryRepository {

    private static final String INVALID_ACTION_TYPE = "Invalid issue history action type.";
    private static final String BASE_SELECT = """
            select id, issue_id, changed_by_login_id, action_type, previous_value, new_value, message, changed_at
            from issue_history
            """;
    private static final String FIND_BY_ISSUE_ID_SQL = BASE_SELECT + " where issue_id = ? order by changed_at, id";

    private final DatabaseConnectionProvider connectionProvider;

    public JdbcIssueHistoryRepository(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public List<IssueHistory> findByIssueId(long issueId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_BY_ISSUE_ID_SQL)) {
            statement.setLong(1, issueId);
            return executeHistoryList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list issue history.", exception);
        }
    }

    private static List<IssueHistory> executeHistoryList(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<IssueHistory> histories = new ArrayList<>();
            while (resultSet.next()) {
                histories.add(mapHistory(resultSet));
            }
            return histories;
        }
    }

    static IssueHistory mapHistory(ResultSet resultSet) throws SQLException {
        return IssueHistory.fromPersistence(
                resultSet.getLong("id"),
                resultSet.getLong("issue_id"),
                resultSet.getString("changed_by_login_id"),
                actionType(resultSet.getString("action_type")),
                resultSet.getString("previous_value"),
                resultSet.getString("new_value"),
                resultSet.getString("message"),
                JdbcSupport.nullableDateTime(resultSet, "changed_at"));
    }

    private static ActionType actionType(String value) throws SQLException {
        try {
            return ActionType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new SQLException(INVALID_ACTION_TYPE, exception);
        }
    }

}
