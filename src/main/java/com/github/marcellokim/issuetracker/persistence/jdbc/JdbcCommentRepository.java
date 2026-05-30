package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcCommentRepository implements CommentRepository {

    private static final String BASE_SELECT = "select id, issue_id, writer_login_id, content, purpose, created_at, updated_at from comments";
    private static final String FIND_BY_ID_SQL = BASE_SELECT + " where id = ?";
    private static final String FIND_BY_ISSUE_ID_SQL = BASE_SELECT + " where issue_id = ? order by created_at, id";

    private final DatabaseConnectionProvider connectionProvider;
    private final JdbcIssueWriteSupport writes = new JdbcIssueWriteSupport();

    public JdbcCommentRepository(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<Comment> findById(long commentId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, commentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapComment(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find comment by id.", exception);
        }
    }

    @Override
    public List<Comment> findByIssueId(long issueId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_BY_ISSUE_ID_SQL)) {
            statement.setLong(1, issueId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Comment> comments = new ArrayList<>();
                while (resultSet.next()) {
                    comments.add(mapComment(resultSet));
                }
                return comments;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list comments by issue.", exception);
        }
    }

    @Override
    public Comment saveCommentAndRecordHistory(Comment comment, IssueHistory history) {

        Objects.requireNonNull(history, "history");
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            boolean transactionSucceeded = false;
            connection.setAutoCommit(false);
            try {
                Comment saved = comment.id() == 0L ? insert(connection, comment) : update(connection, comment);
                writes.insertTransientHistories(connection, history.issueId(), List.of(history));
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
            throw new RepositoryException("Failed to update comment with issue history.", exception);
        }
    }

    @Override
    public void deleteGeneralByIdAndRecordIssueChange(
            long issueId,
            long commentId,
            String writerLoginId,
            IssueHistory history) {
        Objects.requireNonNull(history, "history");
        String sql = """
                delete from comments
                where id = ?
                  and issue_id = ?
                  and writer_login_id = ?
                  and purpose = 'GENERAL'
                """;
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            boolean transactionSucceeded = false;
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, commentId);
                statement.setLong(2, issueId);
                statement.setString(3, Objects.requireNonNull(writerLoginId, "writerLoginId"));
                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    throw new IllegalArgumentException(
                            "Comment was not deleted because it does not exist, is not owned by the writer, "
                                    + "or is not a GENERAL comment.");
                }
                writes.insertTransientHistories(connection, history.issueId(), List.of(history));
                connection.commit();
                transactionSucceeded = true;
            } catch (SQLException | RuntimeException exception) {
                writes.rollbackPreservingOriginalFailure(connection);
                throw exception;
            } finally {
                writes.restoreAutoCommitAfterTransaction(connection, originalAutoCommit, transactionSucceeded);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to delete comment with issue history.", exception);
        }
    }

    private Comment insert(Connection connection, Comment comment) throws SQLException {
        String sql = """
                insert into comments (issue_id, writer_login_id, content, purpose, created_at, updated_at)
                values (?, ?, ?, ?, coalesce(?, current_timestamp), coalesce(?, coalesce(?, current_timestamp)))
                """;
        try (PreparedStatement statement = JdbcSupport.prepareInsertReturningId(connection, sql)) {
            bindInsert(statement, comment);
            statement.executeUpdate();
            return findById(connection, JdbcSupport.generatedId(statement))
                    .orElseThrow(() -> new RepositoryException("Inserted comment was not found.", null));
        }
    }

    private static void bindInsert(PreparedStatement statement, Comment comment) throws SQLException {
        statement.setLong(1, comment.issueId());
        statement.setString(2, comment.writerId());
        statement.setString(3, comment.content());
        statement.setString(4, comment.purpose().name());
        JdbcSupport.setNullableTimestamp(statement, 5, comment.createdDate());
        JdbcSupport.setNullableTimestamp(statement, 6, comment.updatedDate());
        JdbcSupport.setNullableTimestamp(statement, 7, comment.createdDate());
    }

    private Comment update(Connection connection, Comment comment) throws SQLException {
        String sql = """
                update comments
                set content = ?,
                    updated_at = coalesce(?, current_timestamp)
                where id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, comment.content());
            JdbcSupport.setNullableTimestamp(statement, 2, comment.updatedDate());
            statement.setLong(3, comment.id());
            statement.executeUpdate();
            return findById(connection, comment.id())
                    .orElseThrow(() -> new RepositoryException("Updated comment was not found.", null));
        }
    }

    private Optional<Comment> findById(Connection connection, long commentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, commentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapComment(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    static Comment mapComment(ResultSet resultSet) throws SQLException {
        return Comment.fromPersistence(
                resultSet.getLong("id"),
                resultSet.getLong("issue_id"),
                resultSet.getString("writer_login_id"),
                resultSet.getString("content"),
                commentPurposeOf(resultSet.getString("purpose")),
                JdbcSupport.nullableDateTime(resultSet, "created_at"),
                JdbcSupport.nullableDateTime(resultSet, "updated_at"));
    }

    private static CommentPurpose commentPurposeOf(String value) {
        if (value == null || value.isBlank()) {
            return CommentPurpose.GENERAL;
        }
        return CommentPurpose.valueOf(value);
    }
}
