package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.sql.ResultSet;
import java.sql.SQLException;

final class JdbcIssueRowMapper {

    Issue mapIssue(ResultSet resultSet) throws SQLException {
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

    private User mapRequiredUser(ResultSet resultSet, String loginIdColumn, String prefix) throws SQLException {
        User user = mapNullableUser(resultSet, loginIdColumn, prefix);
        if (user == null) {
            throw new SQLException("Required issue user was not joined: " + loginIdColumn);
        }
        return user;
    }

    private User mapNullableUser(ResultSet resultSet, String loginIdColumn, String prefix) throws SQLException {
        String loginId = resultSet.getString(loginIdColumn);
        if (loginId == null || loginId.isBlank()) {
            return null;
        }
        return User.fromPersistence(
                loginId,
                resultSet.getString(prefix + "_name"),
                resultSet.getString(prefix + "_password"),
                Role.valueOf(resultSet.getString(prefix + "_role")),
                resultSet.getInt(prefix + "_active") == 1,
                JdbcSupport.nullableDateTime(resultSet, prefix + "_created_at"),
                JdbcSupport.nullableDateTime(resultSet, prefix + "_updated_at")
        );
    }
}
