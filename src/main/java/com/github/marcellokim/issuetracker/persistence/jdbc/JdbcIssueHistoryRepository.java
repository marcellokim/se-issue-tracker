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
import java.util.Optional;

public final class JdbcIssueHistoryRepository implements IssueHistoryRepository {

    private static final String INVALID_ACTION_TYPE = "Invalid issue history action type.";

    private final DatabaseConnectionProvider connectionProvider;

    public JdbcIssueHistoryRepository(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<IssueHistory> findById(long historyId) {
        String sql = baseSelect() + " where id = ?";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, historyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapHistory(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find issue history by id.", exception);
        }
    }

    @Override
    public List<IssueHistory> findByIssueId(long issueId) {
        String sql = baseSelect() + " where issue_id = ? order by changed_date, id";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            return executeHistoryList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list issue history.", exception);
        }
    }

    @Override
    public Optional<IssueHistory> findLatestStatusChangeToDeleted(long issueId) {
        String sql = baseSelect() + """
                 where issue_id = ?
                   and action_type = 'STATUS_CHANGED'
                   and new_value = 'DELETED'
                 order by changed_date desc, id desc
                 fetch first 1 rows only
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapHistory(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find latest delete history.", exception);
        }
    }

    @Override
    public List<IssueHistory> findDeletedTransitionsByProject(long projectId) {
        String sql = """
                select h.id, h.issue_id, h.changed_by_login_id, h.action_type, h.previous_value,
                       h.new_value, h.message, h.changed_date
                from issue_history h
                join issues i on i.id = h.issue_id
                where i.project_id = ?
                  and h.action_type = 'STATUS_CHANGED'
                  and h.new_value = 'DELETED'
                order by h.changed_date, h.id
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            return executeHistoryList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list deleted transitions.", exception);
        }
    }

    @Override
    public IssueHistory save(IssueHistory history) {
        if (history.id() == 0L) {
            return insert(history);
        }
        return update(history);
    }

    private IssueHistory insert(IssueHistory history) {
        String sql = """
                insert into issue_history (
                    issue_id, changed_by_login_id, action_type, previous_value, new_value, message, changed_date
                )
                values (?, ?, ?, ?, ?, ?, coalesce(?, current_timestamp))
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = JdbcSupport.prepareInsertReturningId(connection, sql)) {
            statement.setLong(1, history.issueId());
            statement.setString(2, history.changedById());
            statement.setString(3, history.actionType().name());
            JdbcSupport.setNullableString(statement, 4, history.previousValue());
            JdbcSupport.setNullableString(statement, 5, history.newValue());
            statement.setString(6, history.message());
            JdbcSupport.setNullableTimestamp(statement, 7, history.changedDate());
            statement.executeUpdate();
            return findById(JdbcSupport.generatedId(statement))
                    .orElseThrow(() -> new RepositoryException("Inserted issue history was not found.", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to insert issue history.", exception);
        }
    }

    private IssueHistory update(IssueHistory history) {
        String sql = """
                update issue_history
                set issue_id = ?,
                    changed_by_login_id = ?,
                    action_type = ?,
                    previous_value = ?,
                    new_value = ?,
                    message = ?,
                    changed_date = coalesce(?, changed_date)
                where id = ?
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, history.issueId());
            statement.setString(2, history.changedById());
            statement.setString(3, history.actionType().name());
            JdbcSupport.setNullableString(statement, 4, history.previousValue());
            JdbcSupport.setNullableString(statement, 5, history.newValue());
            statement.setString(6, history.message());
            JdbcSupport.setNullableTimestamp(statement, 7, history.changedDate());
            statement.setLong(8, history.id());
            statement.executeUpdate();
            return findById(history.id())
                    .orElseThrow(() -> new RepositoryException("Updated issue history was not found.", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to update issue history.", exception);
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
        return new IssueHistory(
                resultSet.getLong("id"),
                resultSet.getLong("issue_id"),
                resultSet.getString("changed_by_login_id"),
                actionType(resultSet.getString("action_type")),
                resultSet.getString("previous_value"),
                resultSet.getString("new_value"),
                resultSet.getString("message"),
                JdbcSupport.nullableDateTime(resultSet, "changed_date"));
    }

    private static ActionType actionType(String value) throws SQLException {
        try {
            return ActionType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new SQLException(INVALID_ACTION_TYPE, exception);
        }
    }

    private static String baseSelect() {
        return """
                select id, issue_id, changed_by_login_id, action_type, previous_value, new_value, message, changed_date
                from issue_history
                """;
    }
}
