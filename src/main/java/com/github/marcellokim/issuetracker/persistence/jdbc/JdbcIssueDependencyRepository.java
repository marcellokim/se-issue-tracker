package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.IssueDependencyChangeRepository;
import com.github.marcellokim.issuetracker.repository.IssueDependencyRepository;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcIssueDependencyRepository
        implements IssueDependencyRepository, IssueDependencyChangeRepository {

    private static final String BASE_SELECT =
            "select id, dependency_id, blocking_issue_id, blocked_issue_id, discovered_date from issue_dependencies";
    private static final String FIND_BY_ID_SQL = BASE_SELECT + " where id = ?";
    private static final String FIND_BY_ISSUE_ID_SQL =
            BASE_SELECT + " where blocking_issue_id = ? or blocked_issue_id = ? order by id";
    private static final String FIND_BY_BLOCKING_ISSUE_ID_SQL =
            BASE_SELECT + " where blocking_issue_id = ? order by id";
    private static final String FIND_BY_BLOCKED_ISSUE_ID_SQL =
            BASE_SELECT + " where blocked_issue_id = ? order by id";

    private final DatabaseConnectionProvider connectionProvider;

    public JdbcIssueDependencyRepository(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<IssueDependency> findById(long dependencyId) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, dependencyId);
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
    public List<IssueDependency> findByBlockingIssueId(long blockingIssueId) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_BLOCKING_ISSUE_ID_SQL)) {
            statement.setLong(1, blockingIssueId);
            return executeDependencyList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list dependencies by blocking issue.", exception);
        }
    }

    @Override
    public List<IssueDependency> findByBlockedIssueId(long blockedIssueId) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_BLOCKED_ISSUE_ID_SQL)) {
            statement.setLong(1, blockedIssueId);
            return executeDependencyList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list dependencies by blocked issue.", exception);
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
    public IssueDependency save(IssueDependency dependency) {
        try (Connection connection = connectionProvider.getConnection()) {
            return save(connection, dependency);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to save issue dependency.", exception);
        }
    }

    @Override
    public IssueDependency saveWithBlockedIssueHistory(IssueDependency dependency, Issue blockedIssue) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                IssueDependency saved = save(connection, dependency);
                // 의존성 row와 blocked issue 이력은 같은 트랜잭션에서만 일관성을 보장한다.
                insertTransientHistories(connection, blockedIssue.id(), blockedIssue.getHistories());
                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
                return saved;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                connection.setAutoCommit(originalAutoCommit);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to save dependency with blocked issue history.", exception);
        }
    }

    @Override
    public void deleteById(long dependencyId) {
        try (Connection connection = connectionProvider.getConnection()) {
            deleteById(connection, dependencyId);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to delete issue dependency.", exception);
        }
    }

    @Override
    public void deleteWithBlockedIssueHistory(IssueDependency dependency, Issue blockedIssue) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                deleteExactlyOneById(connection, dependency.id());
                // 의존성 삭제와 제거 이력 저장은 하나의 persistence operation으로 취급한다.
                insertTransientHistories(connection, blockedIssue.id(), blockedIssue.getHistories());
                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                connection.setAutoCommit(originalAutoCommit);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to delete dependency with blocked issue history.", exception);
        }
    }

    @Override
    public void deleteByIssueId(long issueId) {
        String sql = "delete from issue_dependencies where blocking_issue_id = ? or blocked_issue_id = ?";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            statement.setLong(2, issueId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to delete dependencies by issue.", exception);
        }
    }

    private IssueDependency save(Connection connection, IssueDependency dependency) throws SQLException {
        if (dependency.id() == 0L) {
            return insert(connection, dependency);
        }
        return update(connection, dependency);
    }

    private IssueDependency insert(Connection connection, IssueDependency dependency) throws SQLException {
        String sql = """
                insert into issue_dependencies (dependency_id, blocking_issue_id, blocked_issue_id, discovered_date)
                values (?, ?, ?, coalesce(?, current_timestamp))
                """;
        try (PreparedStatement statement = JdbcSupport.prepareInsertReturningId(connection, sql)) {
            statement.setString(1, dependency.getDependencyId());
            statement.setLong(2, dependency.blockingIssueId());
            statement.setLong(3, dependency.blockedIssueId());
            JdbcSupport.setNullableTimestamp(statement, 4, dependency.discoveredDate());
            statement.executeUpdate();
            return findById(connection, JdbcSupport.generatedId(statement))
                    .orElseThrow(() -> new RepositoryException("Inserted dependency was not found.", null));
        }
    }

    private IssueDependency update(Connection connection, IssueDependency dependency) throws SQLException {
        String sql = """
                update issue_dependencies
                set dependency_id = ?,
                    blocking_issue_id = ?,
                    blocked_issue_id = ?,
                    discovered_date = coalesce(?, discovered_date)
                where id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dependency.getDependencyId());
            statement.setLong(2, dependency.blockingIssueId());
            statement.setLong(3, dependency.blockedIssueId());
            JdbcSupport.setNullableTimestamp(statement, 4, dependency.discoveredDate());
            statement.setLong(5, dependency.id());
            statement.executeUpdate();
            return findById(connection, dependency.id())
                    .orElseThrow(() -> new RepositoryException("Updated dependency was not found.", null));
        }
    }

    private Optional<IssueDependency> findById(Connection connection, long dependencyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, dependencyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapDependency(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    private static void deleteExactlyOneById(Connection connection, long dependencyId) throws SQLException {
        int affectedRows = deleteById(connection, dependencyId);
        if (affectedRows != 1) {
            throw new SQLException("Expected to delete exactly one issue dependency but deleted " + affectedRows + ".");
        }
    }

    private static int deleteById(Connection connection, long dependencyId) throws SQLException {
        String sql = "delete from issue_dependencies where id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, dependencyId);
            return statement.executeUpdate();
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

    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 원래 repository 실패 원인을 유지한다.
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
                JdbcSupport.nullableDateTime(resultSet, "discovered_date")
        );
    }

}
