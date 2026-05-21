package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
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
    private final JdbcIssueRowMapper rowMapper = new JdbcIssueRowMapper();

    public JdbcIssueRepository(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<Issue> findById(long issueId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(JdbcIssueQueries.FIND_BY_ID_SQL)) {
            statement.setLong(1, issueId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(rowMapper.mapIssue(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find issue by id.", exception);
        }
    }

    @Override
    public List<Issue> findByProject(long projectId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(JdbcIssueQueries.FIND_BY_PROJECT_SQL)) {
            statement.setLong(1, projectId);
            return executeIssueList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list issues by project.", exception);
        }
    }

    @Override
    public List<Issue> findDeletedByProject(long projectId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(JdbcIssueQueries.FIND_DELETED_BY_PROJECT_SQL)) {
            statement.setLong(1, projectId);
            return executeIssueList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list deleted issues.", exception);
        }
    }

    @Override
    public List<Issue> findByCriteria(IssueSearchCriteria criteria) {
        JdbcIssueQueries.SearchQuery query = JdbcIssueQueries.search(criteria);
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(query.sql())) {
            for (int index = 0; index < query.binders().size(); index++) {
                query.binders().get(index).bind(statement, index + 1);
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
                if (!isDeletableStatus(issue.status())) {
                    throw new RepositoryException("Only NEW or CLOSED issues can be deleted.", null);
                }

                LocalDateTime effectiveChangedDate = effectiveChangedDate(changedDate);
                deleteDependencies(connection, issueId);
                updateIssueStatus(connection, issueId, IssueStatus.DELETED, effectiveChangedDate);
                insertStatusHistory(
                        connection,
                        issueId,
                        changedById,
                        issue.status().name(),
                        IssueStatus.DELETED.name(),
                        message,
                        effectiveChangedDate
                );
                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
                return copyWithStatus(issue, IssueStatus.DELETED, effectiveChangedDate);
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
                if (!isDeletableStatus(restoreStatus)) {
                    throw new RepositoryException("Pre-delete status history must be NEW or CLOSED.", null);
                }
                LocalDateTime effectiveChangedDate = effectiveChangedDate(changedDate);
                updateIssueStatus(connection, issueId, restoreStatus, effectiveChangedDate);
                insertStatusHistory(
                        connection,
                        issueId,
                        changedById,
                        IssueStatus.DELETED.name(),
                        restoreStatus.name(),
                        message,
                        effectiveChangedDate
                );
                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
                return copyWithStatus(issue, restoreStatus, effectiveChangedDate);
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
                    project_id, issue_id, title, description, reported_date, priority, status,
                    reporter_login_id, assignee_login_id, verifier_login_id, fixer_login_id, resolver_login_id, updated_at
                )
                values (?, ?, ?, ?, coalesce(?, current_timestamp), ?, ?, ?, ?, ?, ?, ?, coalesce(?, current_timestamp))
                """;
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = JdbcSupport.prepareInsertReturningId(connection, sql)) {
                bindIssueForInsert(statement, issue);
                statement.executeUpdate();
                long issueId = JdbcSupport.generatedId(statement);
                insertTransientComments(connection, issueId, issue.getComments());
                insertTransientHistories(connection, issueId, issue.getHistories());
                Issue inserted = findById(connection, issueId)
                        .orElseThrow(() -> new RepositoryException("Inserted issue was not found.", null));
                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
                return inserted;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                connection.setAutoCommit(originalAutoCommit);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to insert issue.", exception);
        }
    }

    private Issue update(Issue issue) {
        String sql = """
                update issues
                set project_id = ?,
                    issue_id = ?,
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
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, issue.projectId());
                statement.setString(2, issue.getIssueId());
                statement.setString(3, issue.title());
                statement.setString(4, issue.description());
                statement.setString(5, issue.priority().name());
                statement.setString(6, issue.status().name());
                statement.setString(7, issue.reporterId());
                JdbcSupport.setNullableString(statement, 8, issue.assigneeId());
                JdbcSupport.setNullableString(statement, 9, issue.verifierId());
                JdbcSupport.setNullableString(statement, 10, issue.fixerId());
                JdbcSupport.setNullableString(statement, 11, issue.resolverId());
                JdbcSupport.setNullableTimestamp(statement, 12, issue.updatedAt());
                statement.setLong(13, issue.id());
                statement.executeUpdate();
                insertTransientComments(connection, issue.id(), issue.getComments());
                insertTransientHistories(connection, issue.id(), issue.getHistories());
                Issue updated = findById(connection, issue.id())
                        .orElseThrow(() -> new RepositoryException("Updated issue was not found.", null));
                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
                return updated;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                connection.setAutoCommit(originalAutoCommit);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to update issue.", exception);
        }
    }

    private static void insertTransientComments(Connection connection, long issueId, List<Comment> comments)
            throws SQLException {
        String sql = """
                insert into comments (issue_id, writer_login_id, content, purpose, created_date)
                values (?, ?, ?, ?, coalesce(?, current_timestamp))
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Comment comment : comments) {
                if (comment.id() != 0L) {
                    continue;
                }
                statement.setLong(1, issueId);
                statement.setString(2, comment.writerId());
                statement.setString(3, comment.content());
                statement.setString(4, comment.purpose().name());
                JdbcSupport.setNullableTimestamp(statement, 5, comment.createdDate());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void insertTransientHistories(Connection connection, long issueId, List<IssueHistory> histories)
            throws SQLException {
        String sql = """
                insert into issue_history (
                    issue_id, changed_by_login_id, action_type, previous_value, new_value, message, changed_date
                )
                values (?, ?, ?, ?, ?, ?, coalesce(?, current_timestamp))
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (IssueHistory history : histories) {
                if (history.id() != 0L) {
                    continue;
                }
                statement.setLong(1, issueId);
                statement.setString(2, history.changedById());
                statement.setString(3, history.actionType().name());
                JdbcSupport.setNullableString(statement, 4, history.previousValue());
                JdbcSupport.setNullableString(statement, 5, history.newValue());
                statement.setString(6, history.message());
                JdbcSupport.setNullableTimestamp(statement, 7, history.changedDate());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void bindIssueForInsert(PreparedStatement statement, Issue issue) throws SQLException {
        statement.setLong(1, issue.projectId());
        statement.setString(2, issue.getIssueId());
        statement.setString(3, issue.title());
        statement.setString(4, issue.description());
        JdbcSupport.setNullableTimestamp(statement, 5, issue.reportedDate());
        statement.setString(6, issue.priority().name());
        statement.setString(7, issue.status().name());
        statement.setString(8, issue.reporterId());
        JdbcSupport.setNullableString(statement, 9, issue.assigneeId());
        JdbcSupport.setNullableString(statement, 10, issue.verifierId());
        JdbcSupport.setNullableString(statement, 11, issue.fixerId());
        JdbcSupport.setNullableString(statement, 12, issue.resolverId());
        JdbcSupport.setNullableTimestamp(statement, 13, issue.updatedAt());
    }

    private List<Issue> executeIssueList(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<Issue> issues = new ArrayList<>();
            while (resultSet.next()) {
                issues.add(rowMapper.mapIssue(resultSet));
            }
            return issues;
        }
    }

    private Optional<Issue> findById(Connection connection, long issueId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(JdbcIssueQueries.FIND_BY_ID_SQL)) {
            statement.setLong(1, issueId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(rowMapper.mapIssue(resultSet));
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
                select id, changed_date, history_id
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
            // 원래 repository 실패 원인을 유지한다.
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static LocalDateTime effectiveChangedDate(LocalDateTime changedDate) {
        return changedDate == null ? LocalDateTime.now() : changedDate;
    }

    private static boolean isDeletableStatus(IssueStatus status) {
        return status == IssueStatus.NEW || status == IssueStatus.CLOSED;
    }

    private static Issue copyWithStatus(Issue issue, IssueStatus status, LocalDateTime updatedAt) {
        return Issue.fromPersistence(Issue.persistedState(
                issue.projectId(),
                issue.title(),
                issue.description(),
                issue.getReporter())
                .id(issue.id())
                .issueId(issue.getIssueId())
                .reportedDate(issue.reportedDate())
                .priority(issue.priority())
                .status(status)
                .assignee(issue.getAssignee())
                .verifier(issue.getVerifier())
                .fixer(issue.getFixer())
                .resolver(issue.getResolver())
                .updatedAt(updatedAt));
    }

}
