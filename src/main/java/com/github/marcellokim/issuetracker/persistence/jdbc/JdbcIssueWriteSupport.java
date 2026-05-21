package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

final class JdbcIssueWriteSupport {

    void insertTransientComments(Connection connection, long issueId, List<Comment> comments)
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

    void insertTransientHistories(Connection connection, long issueId, List<IssueHistory> histories)
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

    void deleteDependencies(Connection connection, long issueId) throws SQLException {
        String sql = "delete from issue_dependencies where blocking_issue_id = ? or blocked_issue_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            statement.setLong(2, issueId);
            statement.executeUpdate();
        }
    }

    void updateIssueStatus(
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

    void insertStatusHistory(
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

    void rollbackPreservingOriginalFailure(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 롤백 실패가 원래 repository 실패 원인을 가리면 호출자가 실제 원인 잃음.
        }
    }

    void restoreAutoCommitPreservingOriginalFailure(Connection connection, boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException ignored) {
            // 실패 경로 cleanup 실패가 원래 repository 실패 원인을 가리면 호출자가 실제 원인 잃음.
        }
    }
}
