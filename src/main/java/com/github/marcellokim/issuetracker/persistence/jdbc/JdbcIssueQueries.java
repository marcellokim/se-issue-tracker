package com.github.marcellokim.issuetracker.persistence.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;

final class JdbcIssueQueries {

    private static final String BASE_SELECT = """
            select i.id, i.issue_id as issue_key, i.project_id, i.title, i.description, i.reported_at, i.priority, i.status,
                   i.reporter_login_id, i.assignee_login_id, i.verifier_login_id, i.fixer_login_id,
                   i.resolver_login_id, i.updated_at,
                   reporter.name as reporter_name,
                   reporter_credentials.password_salt || ':' || reporter_credentials.password_hash as reporter_password,
                   reporter.role as reporter_role,
                   reporter.active as reporter_active,
                   reporter.created_at as reporter_created_at,
                   reporter.updated_at as reporter_updated_at,
                   assignee.name as assignee_name,
                   assignee_credentials.password_salt || ':' || assignee_credentials.password_hash as assignee_password,
                   assignee.role as assignee_role,
                   assignee.active as assignee_active,
                   assignee.created_at as assignee_created_at,
                   assignee.updated_at as assignee_updated_at,
                   verifier.name as verifier_name,
                   verifier_credentials.password_salt || ':' || verifier_credentials.password_hash as verifier_password,
                   verifier.role as verifier_role,
                   verifier.active as verifier_active,
                   verifier.created_at as verifier_created_at,
                   verifier.updated_at as verifier_updated_at,
                   fixer.name as fixer_name,
                   fixer_credentials.password_salt || ':' || fixer_credentials.password_hash as fixer_password,
                   fixer.role as fixer_role,
                   fixer.active as fixer_active,
                   fixer.created_at as fixer_created_at,
                   fixer.updated_at as fixer_updated_at,
                   resolver.name as resolver_name,
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
    static final String FIND_BY_ID_SQL = BASE_SELECT + " where i.id = ?";
    static final String FIND_BY_PROJECT_SQL =
            BASE_SELECT + " where i.project_id = ? and i.status <> 'DELETED' order by i.id";
    static final String FIND_DELETED_BY_PROJECT_SQL =
            BASE_SELECT + " where i.project_id = ? and i.status = 'DELETED' order by i.reported_at desc, i.id desc";
    static final String EXISTS_BY_PROJECT_ID_AND_TITLE_SQL = """
            select 1
            from issues
            where project_id = ?
              and title = ?
            fetch first 1 rows only
            """;
    static final String EXISTS_BY_PROJECT_ID_AND_TITLE_EXCLUDING_ISSUE_ID_SQL = """
            select 1
            from issues
            where project_id = ?
              and title = ?
              and id <> ?
            fetch first 1 rows only
            """;
            
    private JdbcIssueQueries() {
    }

    static String findAllByIdSql(int count) {
        return BASE_SELECT + " where i.id in (" + "?,".repeat(Math.max(0, count - 1)) + "?)";
    }

    static SearchQuery search(IssueSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<SqlBinder> binders = new ArrayList<>();
        sql.append(" where 1 = 1");

        sql.append(" and i.project_id = ?");
        binders.add((statement, index) -> statement.setLong(index, criteria.projectId()));
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
            sql.append(" and (lower(i.title) like ? escape '\\' or lower(i.description) like ? escape '\\')");
            String keyword = escapeLikeWildcards(criteria.keyword().toLowerCase());
            String likeKeyword = "%" + keyword + "%";
            binders.add((statement, index) -> statement.setString(index, likeKeyword));
            binders.add((statement, index) -> statement.setString(index, likeKeyword));
        }
        if (criteria.reportedFrom() != null) {
            sql.append(" and i.reported_at >= ?");
            binders.add(
                    (statement, index) -> JdbcSupport.setNullableTimestamp(statement, index, criteria.reportedFrom()));
        }
        if (criteria.reportedTo() != null) {
            sql.append(" and i.reported_at < ?");
            binders.add(
                    (statement, index) -> JdbcSupport.setNullableTimestamp(statement, index, criteria.reportedTo()));
        }

        sql.append(" order by i.reported_at desc, i.id desc");
        return new SearchQuery(sql.toString(), List.copyOf(binders));
    }

    record SearchQuery(String sql, List<SqlBinder> binders) {
    }

    private static String escapeLikeWildcards(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    @FunctionalInterface
    interface SqlBinder {

        void bind(PreparedStatement statement, int index) throws SQLException;
    }
}
