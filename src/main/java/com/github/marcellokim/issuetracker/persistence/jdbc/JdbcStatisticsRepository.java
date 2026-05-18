package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class JdbcStatisticsRepository implements StatisticsRepository {

    private final DatabaseConnectionProvider connectionProvider;

    public JdbcStatisticsRepository(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Map<IssueStatus, Integer> countByStatus(long projectId) {
        String sql = """
                select status, count(*) as issue_count
                from issues
                where project_id = ?
                  and status <> 'DELETED'
                group by status
                """;
        Map<IssueStatus, Integer> counts = new EnumMap<>(IssueStatus.class);
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.put(IssueStatus.valueOf(resultSet.getString("status")), resultSet.getInt("issue_count"));
                }
                return counts;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to count issues by status.", exception);
        }
    }

    @Override
    public Map<Priority, Integer> countByPriority(long projectId) {
        String sql = """
                select priority, count(*) as issue_count
                from issues
                where project_id = ?
                  and status <> 'DELETED'
                group by priority
                """;
        Map<Priority, Integer> counts = new EnumMap<>(Priority.class);
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.put(Priority.valueOf(resultSet.getString("priority")), resultSet.getInt("issue_count"));
                }
                return counts;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to count issues by priority.", exception);
        }
    }

    @Override
    public List<DailyIssueCount> countReportedIssuesByDay(long projectId) {
        String sql = """
                select trunc(reported_date) as reported_day, count(*) as issue_count
                from issues
                where project_id = ?
                  and status <> 'DELETED'
                group by trunc(reported_date)
                order by reported_day
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<DailyIssueCount> counts = new ArrayList<>();
                while (resultSet.next()) {
                    Date date = resultSet.getDate("reported_day");
                    LocalDate localDate = date.toLocalDate();
                    counts.add(new DailyIssueCount(localDate, resultSet.getInt("issue_count")));
                }
                return counts;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to count reported issues by day.", exception);
        }
    }

    @Override
    public List<MonthlyIssueCount> countReportedIssuesByMonth(long projectId) {
        String sql = """
                select to_char(reported_date, 'YYYY-MM') as reported_month, count(*) as issue_count
                from issues
                where project_id = ?
                  and status <> 'DELETED'
                group by to_char(reported_date, 'YYYY-MM')
                order by reported_month
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MonthlyIssueCount> counts = new ArrayList<>();
                while (resultSet.next()) {
                    YearMonth month = YearMonth.parse(resultSet.getString("reported_month"));
                    counts.add(new MonthlyIssueCount(month, resultSet.getInt("issue_count")));
                }
                return counts;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to count reported issues by month.", exception);
        }
    }
}
