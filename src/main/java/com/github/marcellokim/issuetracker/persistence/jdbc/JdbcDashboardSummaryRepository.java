package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository.DashboardProjectSnapshot;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JdbcDashboardSummaryRepository implements DashboardSummaryRepository {

    private static final String ALL_PROJECT_SUMMARIES_SQL = """
            with member_counts as (
                select pm.project_id,
                       count(distinct pm.user_login_id) as member_count,
                       count(distinct case when u.active = 1 and u.role = 'PL' then u.login_id end) as pl_count,
                       count(distinct case when u.active = 1 and u.role = 'DEV' then u.login_id end) as dev_count,
                       count(distinct case when u.active = 1 and u.role = 'TESTER' then u.login_id end) as tester_count
                from project_members pm
                left join users u on u.login_id = pm.user_login_id
                group by pm.project_id
            ),
            issue_counts as (
                select i.project_id,
                       count(case when i.status <> 'DELETED' then 1 end) as visible_issue_count,
                       count(case when i.status = 'NEW' then 1 end) as new_count,
                       count(case when i.status = 'ASSIGNED' then 1 end) as assigned_count,
                       count(case when i.status = 'FIXED' then 1 end) as fixed_count,
                       count(case when i.status = 'RESOLVED' then 1 end) as resolved_count,
                       count(case when i.status = 'CLOSED' then 1 end) as closed_count,
                       count(case when i.status = 'REOPENED' then 1 end) as reopened_count
                from issues i
                group by i.project_id
            )
            select p.id as project_id,
                   p.name as project_name,
                   p.description as project_description,
                   coalesce(mc.member_count, 0) as member_count,
                   coalesce(mc.pl_count, 0) as pl_count,
                   coalesce(mc.dev_count, 0) as dev_count,
                   coalesce(mc.tester_count, 0) as tester_count,
                   coalesce(ic.visible_issue_count, 0) as visible_issue_count,
                   coalesce(ic.new_count, 0) as new_count,
                   coalesce(ic.assigned_count, 0) as assigned_count,
                   coalesce(ic.fixed_count, 0) as fixed_count,
                   coalesce(ic.resolved_count, 0) as resolved_count,
                   coalesce(ic.closed_count, 0) as closed_count,
                   coalesce(ic.reopened_count, 0) as reopened_count
            from projects p
            left join member_counts mc on mc.project_id = p.id
            left join issue_counts ic on ic.project_id = p.id
            order by p.id
            """;
    private static final String PARTICIPANT_PROJECT_SUMMARIES_SQL = """
            with selected_projects as (
                select distinct project_id
                from project_members
                where user_login_id = ?
            ),
            member_counts as (
                select pm.project_id,
                       count(distinct pm.user_login_id) as member_count,
                       count(distinct case when u.active = 1 and u.role = 'PL' then u.login_id end) as pl_count,
                       count(distinct case when u.active = 1 and u.role = 'DEV' then u.login_id end) as dev_count,
                       count(distinct case when u.active = 1 and u.role = 'TESTER' then u.login_id end) as tester_count
                from project_members pm
                left join users u on u.login_id = pm.user_login_id
                group by pm.project_id
            ),
            issue_counts as (
                select i.project_id,
                       count(case when i.status <> 'DELETED' then 1 end) as visible_issue_count,
                       count(case when i.status = 'NEW' then 1 end) as new_count,
                       count(case when i.status = 'ASSIGNED' then 1 end) as assigned_count,
                       count(case when i.status = 'FIXED' then 1 end) as fixed_count,
                       count(case when i.status = 'RESOLVED' then 1 end) as resolved_count,
                       count(case when i.status = 'CLOSED' then 1 end) as closed_count,
                       count(case when i.status = 'REOPENED' then 1 end) as reopened_count
                from issues i
                group by i.project_id
            )
            select p.id as project_id,
                   p.name as project_name,
                   p.description as project_description,
                   coalesce(mc.member_count, 0) as member_count,
                   coalesce(mc.pl_count, 0) as pl_count,
                   coalesce(mc.dev_count, 0) as dev_count,
                   coalesce(mc.tester_count, 0) as tester_count,
                   coalesce(ic.visible_issue_count, 0) as visible_issue_count,
                   coalesce(ic.new_count, 0) as new_count,
                   coalesce(ic.assigned_count, 0) as assigned_count,
                   coalesce(ic.fixed_count, 0) as fixed_count,
                   coalesce(ic.resolved_count, 0) as resolved_count,
                   coalesce(ic.closed_count, 0) as closed_count,
                   coalesce(ic.reopened_count, 0) as reopened_count
            from projects p
            join selected_projects sp on sp.project_id = p.id
            left join member_counts mc on mc.project_id = p.id
            left join issue_counts ic on ic.project_id = p.id
            order by p.id
            """;
    private final DatabaseConnectionProvider connectionProvider;

    public JdbcDashboardSummaryRepository(DatabaseConnectionProvider connectionProvider) {
        Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.connectionProvider = connectionProvider;
    }

    @Override
    public List<DashboardProjectSnapshot> findAllProjectSummaries() {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(ALL_PROJECT_SUMMARIES_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            List<DashboardProjectSnapshot> summaries = new ArrayList<>();
            while (resultSet.next()) {
                summaries.add(mapSnapshot(resultSet));
            }
            return summaries;
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to load dashboard project summaries.", exception);
        }
    }

    @Override
    public List<DashboardProjectSnapshot> findProjectSummariesByParticipant(String loginId) {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(PARTICIPANT_PROJECT_SUMMARIES_SQL)) {
            statement.setString(1, loginId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<DashboardProjectSnapshot> summaries = new ArrayList<>();
                while (resultSet.next()) {
                    summaries.add(mapSnapshot(resultSet));
                }
                return summaries;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to load dashboard project summaries by participant.", exception);
        }
    }

    private static DashboardProjectSnapshot mapSnapshot(ResultSet resultSet) throws SQLException {
        return new DashboardProjectSnapshot(
                resultSet.getLong("project_id"),
                resultSet.getString("project_name"),
                resultSet.getString("project_description"),
                resultSet.getInt("member_count"),
                resultSet.getInt("pl_count"),
                resultSet.getInt("dev_count"),
                resultSet.getInt("tester_count"),
                resultSet.getInt("visible_issue_count"),
                statusCounts(resultSet));
    }

    private static Map<IssueStatus, Integer> statusCounts(ResultSet resultSet) throws SQLException {
        Map<IssueStatus, Integer> counts = new EnumMap<>(IssueStatus.class);
        counts.put(IssueStatus.NEW, resultSet.getInt("new_count"));
        counts.put(IssueStatus.ASSIGNED, resultSet.getInt("assigned_count"));
        counts.put(IssueStatus.FIXED, resultSet.getInt("fixed_count"));
        counts.put(IssueStatus.RESOLVED, resultSet.getInt("resolved_count"));
        counts.put(IssueStatus.CLOSED, resultSet.getInt("closed_count"));
        counts.put(IssueStatus.REOPENED, resultSet.getInt("reopened_count"));
        return counts;
    }

}
