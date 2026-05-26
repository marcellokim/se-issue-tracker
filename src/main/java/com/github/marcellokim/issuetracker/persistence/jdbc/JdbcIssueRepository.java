package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
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
    private final JdbcIssueWriteSupport writes = new JdbcIssueWriteSupport();
    private final JdbcIssueDeleteOperations deleteOperations;

    public JdbcIssueRepository(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        deleteOperations = new JdbcIssueDeleteOperations(connectionProvider, rowMapper, writes);
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
    public List<Issue> findAllById(List<Long> issueIds) {
        if (issueIds.isEmpty()) {
            return List.of();
        }
        String sql = JdbcIssueQueries.findAllByIdSql(issueIds.size());
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < issueIds.size(); i++) {
                statement.setLong(i + 1, issueIds.get(i));
            }
            return executeIssueList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find issues by ids.", exception);
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
                PreparedStatement statement = connection
                        .prepareStatement(JdbcIssueQueries.FIND_DELETED_BY_PROJECT_SQL)) {
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
    public boolean existsByProjectIdAndTitle(long projectId, String title) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        JdbcIssueQueries.EXISTS_BY_PROJECT_ID_AND_TITLE_SQL)) {
            statement.setLong(1, projectId);
            statement.setString(2, title);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to check issue title duplication.", exception);
        }
    }

    @Override
    public boolean existsByProjectIdAndTitleExcludingIssueId(long projectId, String title, long excludedIssueId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        JdbcIssueQueries.EXISTS_BY_PROJECT_ID_AND_TITLE_EXCLUDING_ISSUE_ID_SQL)) {
            statement.setLong(1, projectId);
            statement.setString(2, title);
            statement.setLong(3, excludedIssueId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to check issue title duplication.", exception);
        }
    }

    @Override
    public boolean existsByResponsibleUser(String userLoginId) {
        String sql = """
                select 1
                from issues
                where status in ('ASSIGNED', 'FIXED')
                  and (
                      assignee_login_id = ?
                      or verifier_login_id = ?
                  )
                  and rownum = 1
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userLoginId);
            statement.setString(2, userLoginId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to check issue responsibility existence.", exception);
        }
    }

    @Override
    public boolean existsActiveAssignmentByProjectAndUser(long projectId, String loginId) {
        String sql = """
                select 1
                from issues
                where project_id = ?
                  and status in ('ASSIGNED', 'FIXED')
                  and (assignee_login_id = ? or verifier_login_id = ?)
                  and rownum = 1
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            statement.setString(2, loginId);
            statement.setString(3, loginId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to check active issue assignment.", exception);
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
        return deleteOperations.softDelete(issueId, changedById, message, changedDate);
    }

    @Override
    public Issue restore(long issueId, String changedById, String message, LocalDateTime changedDate) {
        return deleteOperations.restore(issueId, changedById, message, changedDate);
    }

    @Override
    public int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
        return deleteOperations.purgeDeletedBeyondLimit(projectId, maxDeletedIssues);
    }

    @Override
    public void purge(long issueId) {
        deleteOperations.purge(issueId);
    }

    private Issue insert(Issue issue) {
        String sql = """
                insert into issues (
                    project_id, issue_id, title, description, reported_at, priority, status,
                    reporter_login_id, assignee_login_id, verifier_login_id, fixer_login_id, resolver_login_id, updated_at
                )
                values (?, ?, ?, ?, coalesce(?, current_timestamp), ?, ?, ?, ?, ?, ?, ?, coalesce(?, current_timestamp))
                """;
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            boolean transactionSucceeded = false;
            connection.setAutoCommit(false);
            try (PreparedStatement statement = JdbcSupport.prepareInsertReturningId(connection, sql)) {
                bindIssueForInsert(statement, issue);
                statement.executeUpdate();
                long issueId = JdbcSupport.generatedId(statement);
                writes.insertTransientComments(connection, issueId, issue.getComments());
                writes.insertTransientHistories(connection, issueId, issue.getHistories());
                Issue inserted = findById(connection, issueId)
                        .orElseThrow(() -> new RepositoryException("Inserted issue was not found.", null));
                connection.commit();
                transactionSucceeded = true;
                return inserted;
            } catch (SQLException | RuntimeException exception) {
                writes.rollbackPreservingOriginalFailure(connection);
                throw exception;
            } finally {
                writes.restoreAutoCommitAfterTransaction(connection, originalAutoCommit, transactionSucceeded);
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
            boolean transactionSucceeded = false;
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
                writes.insertTransientComments(connection, issue.id(), issue.getComments());
                writes.insertTransientHistories(connection, issue.id(), issue.getHistories());
                Issue updated = findById(connection, issue.id())
                        .orElseThrow(() -> new RepositoryException("Updated issue was not found.", null));
                connection.commit();
                transactionSucceeded = true;
                return updated;
            } catch (SQLException | RuntimeException exception) {
                writes.rollbackPreservingOriginalFailure(connection);
                throw exception;
            } finally {
                writes.restoreAutoCommitAfterTransaction(connection, originalAutoCommit, transactionSucceeded);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to update issue.", exception);
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

}
