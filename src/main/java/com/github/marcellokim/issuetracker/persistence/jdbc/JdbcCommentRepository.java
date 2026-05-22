package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
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

    private static final String BASE_SELECT =
            "select id, issue_id, writer_login_id, content, purpose, created_at, updated_at from comments";
    private static final String FIND_BY_ID_SQL = BASE_SELECT + " where id = ?";
    private static final String FIND_BY_ISSUE_ID_SQL = BASE_SELECT + " where issue_id = ? order by created_at, id";

    private final DatabaseConnectionProvider connectionProvider;

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
    public Comment save(Comment comment) {
        if (comment.id() == 0L) {
            return insert(comment);
        }
        return update(comment);
    }

    @Override
    public void deleteGeneralById(long issueId, long commentId, String writerLoginId) {
        String sql = """
                delete from comments
                where id = ?
                  and issue_id = ?
                  and writer_login_id = ?
                  and purpose = 'GENERAL'
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, commentId);
            statement.setLong(2, issueId);
            statement.setString(3, Objects.requireNonNull(writerLoginId, "writerLoginId"));
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new IllegalArgumentException(
                        "Comment was not deleted because it does not exist, is not owned by the writer, "
                                + "or is not a GENERAL comment.");
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to delete comment.", exception);
        }
    }

    private Comment insert(Comment comment) {
        String sql = """
                insert into comments (issue_id, writer_login_id, content, purpose, created_at, updated_at)
                values (?, ?, ?, ?, coalesce(?, current_timestamp), coalesce(?, coalesce(?, current_timestamp)))
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = JdbcSupport.prepareInsertReturningId(connection, sql)) {
            statement.setLong(1, comment.issueId());
            statement.setString(2, comment.writerId());
            statement.setString(3, comment.content());
            statement.setString(4, comment.purpose().name());
            JdbcSupport.setNullableTimestamp(statement, 5, comment.createdDate());
            JdbcSupport.setNullableTimestamp(statement, 6, comment.updatedDate());
            JdbcSupport.setNullableTimestamp(statement, 7, comment.createdDate());
            statement.executeUpdate();
            return findById(JdbcSupport.generatedId(statement))
                    .orElseThrow(() -> new RepositoryException("Inserted comment was not found.", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to insert comment.", exception);
        }
    }

    private Comment update(Comment comment) {
        String sql = """
                update comments
                set issue_id = ?,
                    writer_login_id = ?,
                    content = ?,
                    purpose = ?,
                    created_at = coalesce(?, created_at),
                    updated_at = coalesce(?, current_timestamp)
                where id = ?
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, comment.issueId());
            statement.setString(2, comment.writerId());
            statement.setString(3, comment.content());
            statement.setString(4, comment.purpose().name());
            JdbcSupport.setNullableTimestamp(statement, 5, comment.createdDate());
            JdbcSupport.setNullableTimestamp(statement, 6, comment.updatedDate());
            statement.setLong(7, comment.id());
            statement.executeUpdate();
            return findById(comment.id())
                    .orElseThrow(() -> new RepositoryException("Updated comment was not found.", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to update comment.", exception);
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
