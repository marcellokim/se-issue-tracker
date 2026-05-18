package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcIssueRepository implements IssueRepository {

    private final DatabaseConnectionProvider connectionProvider;

    public JdbcIssueRepository(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<Issue> findById(long issueId) {
        String sql = baseSelect() + " where id = ?";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapIssue(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find issue by id.", exception);
        }
    }

    @Override
    public List<Issue> findByProject(long projectId) {
        String sql = baseSelect() + " where project_id = ? and status <> 'DELETED' order by id";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            return executeIssueList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list issues by project.", exception);
        }
    }

    @Override
    public List<Issue> findDeletedByProject(long projectId) {
        String sql = baseSelect() + " where project_id = ? and status = 'DELETED' order by reported_date desc, id desc";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            return executeIssueList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list deleted issues.", exception);
        }
    }

    @Override
    public List<Issue> findByCriteria(IssueSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder(baseSelect());
        List<SqlBinder> binders = new ArrayList<>();
        sql.append(" where 1 = 1");

        if (criteria.projectId() != null) {
            sql.append(" and project_id = ?");
            binders.add((statement, index) -> statement.setLong(index, criteria.projectId()));
        }
        if (criteria.status() != null) {
            sql.append(" and status = ?");
            binders.add((statement, index) -> statement.setString(index, criteria.status().name()));
        } else if (!criteria.includeDeleted()) {
            sql.append(" and status <> 'DELETED'");
        }
        if (criteria.priority() != null) {
            sql.append(" and priority = ?");
            binders.add((statement, index) -> statement.setString(index, criteria.priority().name()));
        }
        if (criteria.reporterId() != null) {
            sql.append(" and reporter_login_id = ?");
            binders.add((statement, index) -> statement.setString(index, criteria.reporterId()));
        }
        if (criteria.assigneeId() != null) {
            sql.append(" and assignee_login_id = ?");
            binders.add((statement, index) -> statement.setString(index, criteria.assigneeId()));
        }
        if (criteria.verifierId() != null) {
            sql.append(" and verifier_login_id = ?");
            binders.add((statement, index) -> statement.setString(index, criteria.verifierId()));
        }
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            sql.append(" and (lower(title) like ? or lower(description) like ?)");
            String keyword = "%" + criteria.keyword().toLowerCase() + "%";
            binders.add((statement, index) -> statement.setString(index, keyword));
            binders.add((statement, index) -> statement.setString(index, keyword));
        }
        if (criteria.reportedFrom() != null) {
            sql.append(" and reported_date >= ?");
            binders.add(
                    (statement, index) -> JdbcSupport.setNullableTimestamp(statement, index, criteria.reportedFrom()));
        }
        if (criteria.reportedTo() != null) {
            sql.append(" and reported_date < ?");
            binders.add(
                    (statement, index) -> JdbcSupport.setNullableTimestamp(statement, index, criteria.reportedTo()));
        }

        sql.append(" order by reported_date desc, id desc");

        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int index = 0; index < binders.size(); index++) {
                binders.get(index).bind(statement, index + 1);
            }
            return executeIssueList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to search issues.", exception);
        }
    }

    @Override
    public Issue save(Issue issue) {
        if (issue.id() == 0L) {
            return insert(issue);
        }
        return update(issue);
    }

    @Override
    public Issue softDelete(long issueId, String changedById, String message, LocalDateTime changedDate) {
        requireText(changedById, "changedById");
        requireText(message, "message");

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Issue issue = findById(connection, issueId)
                        .orElseThrow(() -> new RepositoryException("Issue was not found.", null));
                if (issue.status() == IssueStatus.DELETED) {
                    throw new RepositoryException("Issue is already deleted.", null);
                }

                deleteDependencies(connection, issueId);
                updateIssueStatus(connection, issueId, IssueStatus.DELETED, changedDate);
                insertStatusHistory(
                        connection,
                        issueId,
                        changedById,
                        issue.status().name(),
                        IssueStatus.DELETED.name(),
                        message,
                        changedDate
                );
                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
                return findById(issueId)
                        .orElseThrow(() -> new RepositoryException("Deleted issue was not found.", null));
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                connection.setAutoCommit(originalAutoCommit);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to soft-delete issue.", exception);
        }
    }

    @Override
    public Issue restore(long issueId, String changedById, String message, LocalDateTime changedDate) {
        requireText(changedById, "changedById");
        requireText(message, "message");

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Issue issue = findById(connection, issueId)
                        .orElseThrow(() -> new RepositoryException("Issue was not found.", null));
                if (issue.status() != IssueStatus.DELETED) {
                    throw new RepositoryException("Only deleted issues can be restored.", null);
                }

                IssueStatus restoreStatus = latestPreDeleteStatus(connection, issueId)
                        .orElseThrow(() -> new RepositoryException("Restore requires pre-delete status history.", null));
                if (restoreStatus == IssueStatus.DELETED) {
                    throw new RepositoryException("Pre-delete status history must not be DELETED.", null);
                }
                updateIssueStatus(connection, issueId, restoreStatus, changedDate);
                insertStatusHistory(
                        connection,
                        issueId,
                        changedById,
                        IssueStatus.DELETED.name(),
                        restoreStatus.name(),
                        message,
                        changedDate
                );
                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
                return findById(issueId)
                        .orElseThrow(() -> new RepositoryException("Restored issue was not found.", null));
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                connection.setAutoCommit(originalAutoCommit);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to restore issue.", exception);
        }
    }

    @Override
    public int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
        if (maxDeletedIssues < 0) {
            throw new IllegalArgumentException("maxDeletedIssues must not be negative");
        }

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                List<Long> deletedIssueIds = currentDeletedIssueIdsByFifo(connection, projectId);
                int overflowCount = Math.max(0, deletedIssueIds.size() - maxDeletedIssues);
                for (int index = 0; index < overflowCount; index++) {
                    purge(connection, deletedIssueIds.get(index));
                }
                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
                return overflowCount;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                connection.setAutoCommit(originalAutoCommit);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to purge FIFO deleted issues.", exception);
        }
    }

    @Override
    public void purge(long issueId) {
        String sql = "delete from issues where id = ?";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to purge issue.", exception);
        }
    }

    private void purge(Connection connection, long issueId) throws SQLException {
        String sql = "delete from issues where id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            statement.executeUpdate();
        }
    }

    private Issue insert(Issue issue) {
        String sql = """
                insert into issues (
                    project_id, title, description, reported_date, priority, status,
                    reporter_login_id, assignee_login_id, verifier_login_id, fixer_login_id, resolver_login_id, updated_at
                )
                values (?, ?, ?, coalesce(?, current_timestamp), ?, ?, ?, ?, ?, ?, ?, coalesce(?, current_timestamp))
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = JdbcSupport.prepareInsertReturningId(connection, sql)) {
            bindIssueForInsert(statement, issue);
            statement.executeUpdate();
            return findById(JdbcSupport.generatedId(statement))
                    .orElseThrow(() -> new RepositoryException("Inserted issue was not found.", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to insert issue.", exception);
        }
    }

    private Issue update(Issue issue) {
        String sql = """
                update issues
                set project_id = ?,
                    title = ?,
                    description = ?,
                    priority = ?,
                    status = ?,
                    reporter_login_id = ?,
                    assignee_login_id = ?,
                    verifier_login_id = ?,
                    fixer_login_id = ?,
                    resolver_login_id = ?,
                    updated_at = coalesce(?, current_timestamp)
                where id = ?
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issue.projectId());
            statement.setString(2, issue.title());
            statement.setString(3, issue.description());
            statement.setString(4, issue.priority().name());
            statement.setString(5, issue.status().name());
            statement.setString(6, issue.reporterId());
            JdbcSupport.setNullableString(statement, 7, issue.assigneeId());
            JdbcSupport.setNullableString(statement, 8, issue.verifierId());
            JdbcSupport.setNullableString(statement, 9, issue.fixerId());
            JdbcSupport.setNullableString(statement, 10, issue.resolverId());
            JdbcSupport.setNullableTimestamp(statement, 11, issue.updatedAt());
            statement.setLong(12, issue.id());
            statement.executeUpdate();
            return findById(issue.id())
                    .orElseThrow(() -> new RepositoryException("Updated issue was not found.", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to update issue.", exception);
        }
    }

    private static void bindIssueForInsert(PreparedStatement statement, Issue issue) throws SQLException {
        statement.setLong(1, issue.projectId());
        statement.setString(2, issue.title());
        statement.setString(3, issue.description());
        JdbcSupport.setNullableTimestamp(statement, 4, issue.reportedDate());
        statement.setString(5, issue.priority().name());
        statement.setString(6, issue.status().name());
        statement.setString(7, issue.reporterId());
        JdbcSupport.setNullableString(statement, 8, issue.assigneeId());
        JdbcSupport.setNullableString(statement, 9, issue.verifierId());
        JdbcSupport.setNullableString(statement, 10, issue.fixerId());
        JdbcSupport.setNullableString(statement, 11, issue.resolverId());
        JdbcSupport.setNullableTimestamp(statement, 12, issue.updatedAt());
    }

    private static List<Issue> executeIssueList(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<Issue> issues = new ArrayList<>();
            while (resultSet.next()) {
                issues.add(mapIssue(resultSet));
            }
            return issues;
        }
    }

    private Optional<Issue> findById(Connection connection, long issueId) throws SQLException {
        String sql = baseSelect() + " where id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapIssue(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    private static void deleteDependencies(Connection connection, long issueId) throws SQLException {
        String sql = "delete from issue_dependencies where blocking_issue_id = ? or blocked_issue_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            statement.setLong(2, issueId);
            statement.executeUpdate();
        }
    }

    private static void updateIssueStatus(
            Connection connection,
            long issueId,
            IssueStatus status,
            LocalDateTime changedDate
    ) throws SQLException {
        String sql = "update issues set status = ?, updated_at = coalesce(?, current_timestamp) where id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            JdbcSupport.setNullableTimestamp(statement, 2, changedDate);
            statement.setLong(3, issueId);
            statement.executeUpdate();
        }
    }

    private static void insertStatusHistory(
            Connection connection,
            long issueId,
            String changedById,
            String previousValue,
            String newValue,
            String message,
            LocalDateTime changedDate
    ) throws SQLException {
        String sql = """
                insert into issue_history (
                    issue_id, changed_by_login_id, action_type, previous_value, new_value, message, changed_date
                )
                values (?, ?, 'STATUS_CHANGED', ?, ?, ?, coalesce(?, current_timestamp))
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            statement.setString(2, changedById);
            statement.setString(3, previousValue);
            statement.setString(4, newValue);
            statement.setString(5, message);
            JdbcSupport.setNullableTimestamp(statement, 6, changedDate);
            statement.executeUpdate();
        }
    }

    private static Optional<IssueStatus> latestPreDeleteStatus(Connection connection, long issueId) throws SQLException {
        String sql = """
                select previous_value
                from issue_history
                where issue_id = ?
                  and action_type = 'STATUS_CHANGED'
                  and new_value = 'DELETED'
                order by changed_date desc, id desc
                fetch first 1 rows only
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                String previousValue = resultSet.getString("previous_value");
                if (previousValue == null || previousValue.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(IssueStatus.valueOf(previousValue));
            }
        }
    }

    private static List<Long> currentDeletedIssueIdsByFifo(Connection connection, long projectId) throws SQLException {
        String sql = """
                select id
                from (
                    select i.id,
                           h.changed_date,
                           h.id as history_id,
                           row_number() over (
                               partition by i.id
                               order by h.changed_date desc, h.id desc
                           ) as rn
                    from issues i
                    join issue_history h on h.issue_id = i.id
                    where i.project_id = ?
                      and i.status = 'DELETED'
                      and h.action_type = 'STATUS_CHANGED'
                      and h.new_value = 'DELETED'
                )
                where rn = 1
                order by changed_date, history_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("id"));
                }
                return ids;
            }
        }
    }

    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Keep the original repository failure.
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    static Issue mapIssue(ResultSet resultSet) throws SQLException {
        return new Issue(
                resultSet.getLong("id"),
                resultSet.getLong("project_id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                JdbcSupport.nullableDateTime(resultSet, "reported_date"),
                Priority.valueOf(resultSet.getString("priority")),
                IssueStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("reporter_login_id"),
                resultSet.getString("assignee_login_id"),
                resultSet.getString("verifier_login_id"),
                resultSet.getString("fixer_login_id"),
                resultSet.getString("resolver_login_id"),
                JdbcSupport.nullableDateTime(resultSet, "updated_at"));
    }

    private static String baseSelect() {
        return """
                select id, project_id, title, description, reported_date, priority, status,
                       reporter_login_id, assignee_login_id, verifier_login_id, fixer_login_id, resolver_login_id, updated_at
                from issues
                """;
    }

    @FunctionalInterface
    private interface SqlBinder {

        void bind(PreparedStatement statement, int index) throws SQLException;
    }
}
