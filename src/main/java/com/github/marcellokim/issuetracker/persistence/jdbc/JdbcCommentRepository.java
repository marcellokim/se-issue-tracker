package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcCommentRepository implements CommentRepository {

    private static final String BASE_SELECT = "select id, issue_id, writer_login_id, content, created_date from comments";
    private static final String FIND_BY_ID_SQL = BASE_SELECT + " where id = ?";
    private static final String FIND_BY_ISSUE_ID_SQL = BASE_SELECT + " where issue_id = ? order by created_date, id";

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
    public void deleteById(long commentId) {
        String sql = "delete from comments where id = ?";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, commentId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to delete comment.", exception);
        }
    }

    private Comment insert(Comment comment) {
        String sql = """
                insert into comments (issue_id, writer_login_id, content, created_date)
                values (?, ?, ?, coalesce(?, current_timestamp))
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = JdbcSupport.prepareInsertReturningId(connection, sql)) {
            statement.setLong(1, comment.issueId());
            statement.setString(2, comment.writerId());
            statement.setString(3, comment.content());
            JdbcSupport.setNullableTimestamp(statement, 4, comment.createdDate());
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
                    created_date = coalesce(?, created_date)
                where id = ?
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, comment.issueId());
            statement.setString(2, comment.writerId());
            statement.setString(3, comment.content());
            JdbcSupport.setNullableTimestamp(statement, 4, comment.createdDate());
            statement.setLong(5, comment.id());
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
                JdbcSupport.nullableDateTime(resultSet, "created_date"));
    }
}
