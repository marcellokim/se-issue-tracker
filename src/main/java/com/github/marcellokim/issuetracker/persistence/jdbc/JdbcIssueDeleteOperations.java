package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class JdbcIssueDeleteOperations {

    private static final int PURGE_BATCH_SIZE = 500;

    private final DatabaseConnectionProvider connectionProvider;
    private final JdbcIssueRowMapper rowMapper;
    private final JdbcIssueWriteSupport writes;

    JdbcIssueDeleteOperations(
            DatabaseConnectionProvider connectionProvider,
            JdbcIssueRowMapper rowMapper,
            JdbcIssueWriteSupport writes) {
        this.connectionProvider = connectionProvider;
        this.rowMapper = rowMapper;
        this.writes = writes;
    }

    // 삭제와 복구는 상태 전이와 의존성 제거를 함께 묶어야 하므로 repository facade에서 분리.
    Issue softDelete(long issueId, String changedById, String message, LocalDateTime changedDate) {
        requireText(changedById, "changedById");
        requireText(message, "message");

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            boolean transactionSucceeded = false;
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
                List<DependencyRemoval> dependencyRemovals = findDependencyRemovals(connection, issueId);
                recordDependencyRemovals(connection, dependencyRemovals, changedById, effectiveChangedDate);
                writes.deleteDependencies(connection, issueId);
                writes.updateIssueStatus(connection, issueId, IssueStatus.DELETED, effectiveChangedDate);
                writes.insertStatusHistory(
                        connection,
                        issueId,
                        changedById,
                        issue.status().name(),
                        IssueStatus.DELETED.name(),
                        message,
                        effectiveChangedDate);
                connection.commit();
                transactionSucceeded = true;
                return copyWithStatus(issue, IssueStatus.DELETED, effectiveChangedDate);
            } catch (SQLException | RuntimeException exception) {
                writes.rollbackPreservingOriginalFailure(connection);
                throw exception;
            } finally {
                writes.restoreAutoCommitAfterTransaction(connection, originalAutoCommit, transactionSucceeded);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to soft-delete issue.", exception);
        }
    }

    Issue restore(long issueId, String changedById, String message, LocalDateTime changedDate) {
        requireText(changedById, "changedById");
        requireText(message, "message");

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            boolean transactionSucceeded = false;
            connection.setAutoCommit(false);
            try {
                Issue issue = findById(connection, issueId)
                        .orElseThrow(() -> new RepositoryException("Issue was not found.", null));
                if (issue.status() != IssueStatus.DELETED) {
                    throw new RepositoryException("Only deleted issues can be restored.", null);
                }

                IssueStatus restoreStatus = latestPreDeleteStatus(connection, issueId)
                        .orElseThrow(
                                () -> new RepositoryException("Restore requires pre-delete status history.", null));
                if (!isDeletableStatus(restoreStatus)) {
                    throw new RepositoryException("Pre-delete status history must be NEW or CLOSED.", null);
                }
                LocalDateTime effectiveChangedDate = effectiveChangedDate(changedDate);
                writes.updateIssueStatus(connection, issueId, restoreStatus, effectiveChangedDate);
                writes.insertStatusHistory(
                        connection,
                        issueId,
                        changedById,
                        IssueStatus.DELETED.name(),
                        restoreStatus.name(),
                        message,
                        effectiveChangedDate);
                connection.commit();
                transactionSucceeded = true;
                return copyWithStatus(issue, restoreStatus, effectiveChangedDate);
            } catch (SQLException | RuntimeException exception) {
                writes.rollbackPreservingOriginalFailure(connection);
                throw exception;
            } finally {
                writes.restoreAutoCommitAfterTransaction(connection, originalAutoCommit, transactionSucceeded);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to restore issue.", exception);
        }
    }

    int purgeDeletedById(long issueId) {
        String sql = "delete from issues where id = ? and status = 'DELETED'";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to purge deleted issue.", exception);
        }
    }

    int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
        if (maxDeletedIssues < 0) {
            throw new IllegalArgumentException("maxDeletedIssues must not be negative");
        }

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            boolean transactionSucceeded = false;
            connection.setAutoCommit(false);
            try {
                List<Long> deletedIssueIds = currentDeletedIssueIdsByFifo(connection, projectId);
                int overflowCount = Math.max(0, deletedIssueIds.size() - maxDeletedIssues);
                purgeAll(connection, deletedIssueIds.subList(0, overflowCount));
                connection.commit();
                transactionSucceeded = true;
                return overflowCount;
            } catch (SQLException | RuntimeException exception) {
                writes.rollbackPreservingOriginalFailure(connection);
                throw exception;
            } finally {
                writes.restoreAutoCommitAfterTransaction(connection, originalAutoCommit, transactionSucceeded);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to purge FIFO deleted issues.", exception);
        }
    }

    private void purgeAll(Connection connection, List<Long> issueIds) throws SQLException {
        if (issueIds.isEmpty()) {
            return;
        }
        String sql = "delete from issues where id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int pendingBatchSize = 0;
            for (Long issueId : issueIds) {
                statement.setLong(1, issueId);
                statement.addBatch();
                pendingBatchSize++;
                if (pendingBatchSize == PURGE_BATCH_SIZE) {
                    statement.executeBatch();
                    pendingBatchSize = 0;
                }
            }
            if (pendingBatchSize > 0) {
                // 단건 DELETE 문을 JDBC batch로 묶어 SQL injection hotspot 없이 기존 FIFO 대상 집합만 제거함.
                statement.executeBatch();
            }
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

    private static List<DependencyRemoval> findDependencyRemovals(Connection connection, long issueId)
            throws SQLException {
        String sql = """
                select dependency_id, blocked_issue_id
                from issue_dependencies
                where blocking_issue_id = ? or blocked_issue_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            statement.setLong(2, issueId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<DependencyRemoval> removals = new ArrayList<>();
                while (resultSet.next()) {
                    removals.add(new DependencyRemoval(
                            resultSet.getString("dependency_id"),
                            resultSet.getLong("blocked_issue_id")));
                }
                return removals;
            }
        }
    }

    private static void recordDependencyRemovals(
            Connection connection,
            List<DependencyRemoval> removals,
            String changedById,
            LocalDateTime changedDate) throws SQLException {
        if (removals.isEmpty()) {
            return;
        }
        updateBlockedIssueTimestamps(connection, removals, changedDate);
        insertDependencyRemovalHistories(connection, removals, changedById, changedDate);
    }

    private static void updateBlockedIssueTimestamps(
            Connection connection,
            List<DependencyRemoval> removals,
            LocalDateTime changedDate) throws SQLException {
        String sql = "update issues set updated_at = coalesce(?, current_timestamp) where id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (DependencyRemoval removal : removals) {
                JdbcSupport.setNullableTimestamp(statement, 1, changedDate);
                statement.setLong(2, removal.blockedIssueId());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void insertDependencyRemovalHistories(
            Connection connection,
            List<DependencyRemoval> removals,
            String changedById,
            LocalDateTime changedDate) throws SQLException {
        String sql = """
                insert into issue_history (
                    issue_id, changed_by_login_id, action_type, previous_value, new_value, message, changed_at
                )
                values (?, ?, 'DEPENDENCY_CHANGED', ?, null, 'Dependency removed', coalesce(?, current_timestamp))
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (DependencyRemoval removal : removals) {
                statement.setLong(1, removal.blockedIssueId());
                statement.setString(2, changedById);
                statement.setString(3, removal.dependencyId());
                JdbcSupport.setNullableTimestamp(statement, 4, changedDate);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static Optional<IssueStatus> latestPreDeleteStatus(Connection connection, long issueId)
            throws SQLException {
        String sql = """
                select previous_value
                from issue_history
                where issue_id = ?
                  and action_type = 'STATUS_CHANGED'
                  and new_value = 'DELETED'
                order by changed_at desc, id desc
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

    private static List<Long> currentDeletedIssueIdsByFifo(Connection connection, long projectId)
            throws SQLException {
        String sql = """
                select id, changed_at, history_id
                from (
                    select i.id,
                           h.changed_at,
                           h.id as history_id,
                           row_number() over (
                               partition by i.id
                               order by h.changed_at desc, h.id desc
                           ) as rn
                    from issues i
                    join issue_history h on h.issue_id = i.id
                    where i.project_id = ?
                      and i.status = 'DELETED'
                      and h.action_type = 'STATUS_CHANGED'
                      and h.new_value = 'DELETED'
                )
                where rn = 1
                order by changed_at, history_id
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

    private record DependencyRemoval(String dependencyId, long blockedIssueId) {
    }
}
