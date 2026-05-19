package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
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

    private static final String BASE_SELECT = """
            select i.id, i.issue_id as issue_key, i.project_id, i.title, i.description, i.reported_date, i.priority, i.status,
                   i.reporter_login_id, i.assignee_login_id, i.verifier_login_id, i.fixer_login_id,
                   i.resolver_login_id, i.updated_at,
                   reporter_credentials.password_salt || ':' || reporter_credentials.password_hash as reporter_password,
                   reporter.role as reporter_role,
                   reporter.active as reporter_active,
                   reporter.created_at as reporter_created_at,
                   reporter.updated_at as reporter_updated_at,
                   assignee_credentials.password_salt || ':' || assignee_credentials.password_hash as assignee_password,
                   assignee.role as assignee_role,
                   assignee.active as assignee_active,
                   assignee.created_at as assignee_created_at,
                   assignee.updated_at as assignee_updated_at,
                   verifier_credentials.password_salt || ':' || verifier_credentials.password_hash as verifier_password,
                   verifier.role as verifier_role,
                   verifier.active as verifier_active,
                   verifier.created_at as verifier_created_at,
                   verifier.updated_at as verifier_updated_at,
                   fixer_credentials.password_salt || ':' || fixer_credentials.password_hash as fixer_password,
                   fixer.role as fixer_role,
                   fixer.active as fixer_active,
                   fixer.created_at as fixer_created_at,
                   fixer.updated_at as fixer_updated_at,
                   resolver_credentials.password_salt || ':' || resolver_credentials.password_hash as resolver_password,
                   resolver.role as resolver_role,
                   resolver.active as resolver_active,
                   resolver.created_at as resolver_created_at,
                   resolver.updated_at as resolver_updated_at
            from issues i
            join users reporter on reporter.login_id = i.reporter_login_id
            join user_credentials reporter_credentials on reporter_credentials.login_id = reporter.login_id
            left join users assignee on assignee.login_id = i.assignee_login_id
            left join user_credentials assignee_credentials on assignee_credentials.login_id = assignee.login_id
            left join users verifier on verifier.login_id = i.verifier_login_id
            left join user_credentials verifier_credentials on verifier_credentials.login_id = verifier.login_id
            left join users fixer on fixer.login_id = i.fixer_login_id
            left join user_credentials fixer_credentials on fixer_credentials.login_id = fixer.login_id
            left join users resolver on resolver.login_id = i.resolver_login_id
            left join user_credentials resolver_credentials on resolver_credentials.login_id = resolver.login_id
            """;
    private static final String FIND_BY_ID_SQL = BASE_SELECT + " where i.id = ?";
    private static final String FIND_BY_PROJECT_SQL =
            BASE_SELECT + " where i.project_id = ? and i.status <> 'DELETED' order by i.id";
    private static final String FIND_DELETED_BY_PROJECT_SQL =
            BASE_SELECT + " where i.project_id = ? and i.status = 'DELETED' order by i.reported_date desc, i.id desc";

    private final DatabaseConnectionProvider connectionProvider;

    public JdbcIssueRepository(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<Issue> findById(long issueId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
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
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_BY_PROJECT_SQL)) {
            statement.setLong(1, projectId);
            return executeIssueList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list issues by project.", exception);
        }
    }

    @Override
    public List<Issue> findDeletedByProject(long projectId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_DELETED_BY_PROJECT_SQL)) {
            statement.setLong(1, projectId);
            return executeIssueList(statement);
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list deleted issues.", exception);
        }
    }

    @Override
    public List<Issue> findByCriteria(IssueSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<SqlBinder> binders = new ArrayList<>();
        sql.append(" where 1 = 1");

        if (criteria.projectId() != null) {
            sql.append(" and i.project_id = ?");
            binders.add((statement, index) -> statement.setLong(index, criteria.projectId()));
        }
        if (criteria.status() != null) {
            sql.append(" and i.status = ?");
            binders.add((statement, index) -> statement.setString(index, criteria.status().name()));
        } else if (!criteria.includeDeleted()) {
            sql.append(" and i.status <> 'DELETED'");
        }
        if (criteria.priority() != null) {
            sql.append(" and i.priority = ?");
            binders.add((statement, index) -> statement.setString(index, criteria.priority().name()));
        }
        if (criteria.reporterId() != null) {
            sql.append(" and i.reporter_login_id = ?");
            binders.add((statement, index) -> statement.setString(index, criteria.reporterId()));
        }
        if (criteria.assigneeId() != null) {
            sql.append(" and i.assignee_login_id = ?");
            binders.add((statement, index) -> statement.setString(index, criteria.assigneeId()));
        }
        if (criteria.verifierId() != null) {
            sql.append(" and i.verifier_login_id = ?");
            binders.add((statement, index) -> statement.setString(index, criteria.verifierId()));
        }
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            sql.append(" and (lower(i.title) like ? or dbms_lob.instr(lower(i.description), ?) > 0)");
            String keyword = criteria.keyword().toLowerCase();
            String likeKeyword = "%" + keyword + "%";
            binders.add((statement, index) -> statement.setString(index, likeKeyword));
            binders.add((statement, index) -> statement.setString(index, keyword));
        }
        if (criteria.reportedFrom() != null) {
            sql.append(" and i.reported_date >= ?");
            binders.add(
                    (statement, index) -> JdbcSupport.setNullableTimestamp(statement, index, criteria.reportedFrom()));
        }
        if (criteria.reportedTo() != null) {
            sql.append(" and i.reported_date < ?");
            binders.add(
                    (statement, index) -> JdbcSupport.setNullableTimestamp(statement, index, criteria.reportedTo()));
        }

        sql.append(" order by i.reported_date desc, i.id desc");

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
                if (restoreStatus == IssueStatus.DELETED) {
                    throw new RepositoryException("Pre-delete status history must not be DELETED.", null);
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
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
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
            return findById(issue.id())
                    .orElseThrow(() -> new RepositoryException("Updated issue was not found.", null));
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
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
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
            // Keep the original repository failure.
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

    static Issue mapIssue(ResultSet resultSet) throws SQLException {
        return Issue.fromPersistence(Issue.persistedState(
                resultSet.getLong("project_id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                mapRequiredUser(resultSet, "reporter_login_id", "reporter"))
                .id(resultSet.getLong("id"))
                .issueId(resultSet.getString("issue_key"))
                .reportedDate(JdbcSupport.nullableDateTime(resultSet, "reported_date"))
                .priority(Priority.valueOf(resultSet.getString("priority")))
                .status(IssueStatus.valueOf(resultSet.getString("status")))
                .assignee(mapNullableUser(resultSet, "assignee_login_id", "assignee"))
                .verifier(mapNullableUser(resultSet, "verifier_login_id", "verifier"))
                .fixer(mapNullableUser(resultSet, "fixer_login_id", "fixer"))
                .resolver(mapNullableUser(resultSet, "resolver_login_id", "resolver"))
                .updatedAt(JdbcSupport.nullableDateTime(resultSet, "updated_at")));
    }

    private static User mapRequiredUser(ResultSet resultSet, String loginIdColumn, String prefix) throws SQLException {
        User user = mapNullableUser(resultSet, loginIdColumn, prefix);
        if (user == null) {
            throw new SQLException("Required issue user was not joined: " + loginIdColumn);
        }
        return user;
    }

    private static User mapNullableUser(ResultSet resultSet, String loginIdColumn, String prefix) throws SQLException {
        String loginId = resultSet.getString(loginIdColumn);
        if (loginId == null || loginId.isBlank()) {
            return null;
        }
        return new User(
                loginId,
                resultSet.getString(prefix + "_password"),
                Role.valueOf(resultSet.getString(prefix + "_role")),
                resultSet.getInt(prefix + "_active") == 1,
                JdbcSupport.nullableDateTime(resultSet, prefix + "_created_at"),
                JdbcSupport.nullableDateTime(resultSet, prefix + "_updated_at")
        );
    }

    @FunctionalInterface
    private interface SqlBinder {

        void bind(PreparedStatement statement, int index) throws SQLException;
    }
}
