package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.IssueDependencyRepository;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcIssueDependencyRepository implements IssueDependencyRepository {

    private static final String BASE_SELECT = "select id, dependency_id, blocking_issue_id, blocked_issue_id, discovered_at from issue_dependencies";
    private static final String FIND_BY_ID_SQL = BASE_SELECT + " where id = ?";
    private static final String FIND_BY_DEPENDENCY_ID_SQL = BASE_SELECT + " where dependency_id = ?";
    private static final String FIND_BY_ISSUE_ID_SQL = BASE_SELECT
            + " where blocking_issue_id = ? or blocked_issue_id = ? order by id";
    private static final String FIND_DEPENDENCIES_BLOCKED_BY_ISSUE_SQL = BASE_SELECT
            + " where blocking_issue_id = ? order by id";
    private static final String FIND_DEPENDENCIES_BLOCKING_ISSUE_SQL = BASE_SELECT
            + " where blocked_issue_id = ? order by id";

    private final DatabaseConnectionProvider connectionProvider;
    private final JdbcIssueWriteSupport writes = new JdbcIssueWriteSupport();

    public JdbcIssueDependencyRepository(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<IssueDependency> findById(long id) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapDependency(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find issue dependency by id.", exception);
        }
    }

    @Override
    public Optional<IssueDependency> findByDependencyId(String dependencyId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_BY_DEPENDENCY_ID_SQL)) {
            statement.setString(1, dependencyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapDependency(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find issue dependency by dependency id.", exception);
        }
    }

    @Override
    public List<IssueDependency> findByIssueId(long issueId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_BY_ISSUE_ID_SQL)) {
            statement.setLong(1, issueId);
            statement.setLong(2, issueId);
            return executeDependencyList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list dependencies by issue.", exception);
        }
    }

    @Override
    public List<IssueDependency> findDependenciesBlockedByIssue(long blockingIssueId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_DEPENDENCIES_BLOCKED_BY_ISSUE_SQL)) {
            statement.setLong(1, blockingIssueId);
            return executeDependencyList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list dependencies by blocking issue.", exception);
        }
    }

    @Override
    public List<IssueDependency> findDependenciesBlockingIssue(long blockedIssueId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_DEPENDENCIES_BLOCKING_ISSUE_SQL)) {
            statement.setLong(1, blockedIssueId);
            return executeDependencyList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list dependencies by blocked issue.", exception);
        }
    }

    @Override
    public List<IssueDependency> findByProjectId(long projectId) {
        String sql = """
                select id, dependency_id, blocking_issue_id, blocked_issue_id, discovered_at
                from issue_dependencies
                where blocking_issue_id in (select id from issues where project_id = ?)
                    and blocked_issue_id in (select id from issues where project_id = ?)
                order by id
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            statement.setLong(2, projectId);
            return executeDependencyList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list dependencies by project.", exception);
        }
    }

    @Override
    public boolean existsByPair(long blockingIssueId, long blockedIssueId) {
        String sql = """
                select count(*)
                from issue_dependencies
                where blocking_issue_id = ? and blocked_issue_id = ?
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, blockingIssueId);
            statement.setLong(2, blockedIssueId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to check dependency pair.", exception);
        }
    }

    @Override
    public IssueDependency recordDependencyAdded(IssueDependency dependency, Issue issue) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            boolean transactionSucceeded = false;
            connection.setAutoCommit(false);
            try {
                IssueHistory history = latestDependencyHistory(issue, dependency.getDependencyId(), false);
                IssueDependency saved = insert(connection, dependency);
                updateIssueTimestamp(connection, issue.id(), history.changedDate());
                writes.insertTransientHistories(connection, issue.id(), List.of(history));
                connection.commit();
                transactionSucceeded = true;
                return saved;
            } catch (SQLException | RuntimeException exception) {
                writes.rollbackPreservingOriginalFailure(connection);
                throw exception;
            } finally {
                writes.restoreAutoCommitAfterTransaction(connection, originalAutoCommit, transactionSucceeded);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to insert issue dependency with issue history.", exception);
        }
    }

    @Override
    public void recordDependencyRemoved(String dependencyId, Issue issue) {
        String sql = "delete from issue_dependencies where dependency_id = ?";
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            boolean transactionSucceeded = false;
            connection.setAutoCommit(false);
            try {
                IssueHistory history = latestDependencyHistory(issue, dependencyId, true);
                updateIssueTimestamp(connection, issue.id(), history.changedDate());
                writes.insertTransientHistories(connection, issue.id(), List.of(history));
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, dependencyId);
                    int affectedRows = statement.executeUpdate();
                    if (affectedRows == 0) {
                        throw new RepositoryException("Issue dependency was not found.", null);
                    }
                }
                connection.commit();
                transactionSucceeded = true;
            } catch (SQLException | RuntimeException exception) {
                writes.rollbackPreservingOriginalFailure(connection);
                throw exception;
            } finally {
                writes.restoreAutoCommitAfterTransaction(connection, originalAutoCommit, transactionSucceeded);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to delete issue dependency with issue history.", exception);
        }
    }

    private static IssueHistory latestDependencyHistory(Issue issue, String dependencyId, boolean removal) {
        List<IssueHistory> histories = issue.getHistories();
        if (histories.isEmpty()) {
            throw new RepositoryException("Issue dependency history was not recorded.", null);
        }
        IssueHistory history = histories.getLast();
        if (history.actionType() != ActionType.DEPENDENCY_CHANGED) {
            throw new RepositoryException("Latest issue history is not a dependency change.", null);
        }
        if (removal && history.newValue() != null) {
            throw new RepositoryException("Latest issue history is not a dependency removal.", null);
        }
        if (removal && !dependencyId.equals(history.previousValue())) {
            throw new RepositoryException("Latest issue history does not match dependency removal.", null);
        }
        if (!removal && history.previousValue() != null) {
            throw new RepositoryException("Latest issue history is not a dependency addition.", null);
        }
        if (!removal && !dependencyId.equals(history.newValue())) {
            throw new RepositoryException("Latest issue history does not match dependency addition.", null);
        }
        return history;
    }

    private static void updateIssueTimestamp(Connection connection, long issueId, LocalDateTime changedAt)
            throws SQLException {
        String sql = "update issues set updated_at = coalesce(?, current_timestamp) where id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setNullableTimestamp(statement, 1, changedAt);
            statement.setLong(2, issueId);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new RepositoryException("Issue was not found while updating dependency timestamp.", null);
            }
        }
    }

    private IssueDependency insert(Connection connection, IssueDependency dependency) throws SQLException {
        String sql = """
                insert into issue_dependencies (dependency_id, blocking_issue_id, blocked_issue_id, discovered_at)
                values (?, ?, ?, coalesce(?, current_timestamp))
                """;
        try (PreparedStatement statement = JdbcSupport.prepareInsertReturningId(connection, sql)) {
            statement.setString(1, dependency.getDependencyId());
            statement.setLong(2, dependency.blockingIssueId());
            statement.setLong(3, dependency.blockedIssueId());
            JdbcSupport.setNullableTimestamp(statement, 4, dependency.discoveredDate());
            statement.executeUpdate();
            return IssueDependency.fromPersistence(
                    JdbcSupport.generatedId(statement),
                    dependency.getDependencyId(),
                    dependency.blockingIssueId(),
                    dependency.blockedIssueId(),
                    dependency.discoveredDate());
        }
    }

    private static List<IssueDependency> executeDependencyList(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<IssueDependency> dependencies = new ArrayList<>();
            while (resultSet.next()) {
                dependencies.add(mapDependency(resultSet));
            }
            return dependencies;
        }
    }

    static IssueDependency mapDependency(ResultSet resultSet) throws SQLException {
        return IssueDependency.fromPersistence(
                resultSet.getLong("id"),
                resultSet.getString("dependency_id"),
                resultSet.getLong("blocking_issue_id"),
                resultSet.getLong("blocked_issue_id"),
                JdbcSupport.nullableDateTime(resultSet, "discovered_at"));
    }

}
