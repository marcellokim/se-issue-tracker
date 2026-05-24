package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        return countReportedIssuesByDay(projectId, null, null);
    }

    @Override
    public List<DailyIssueCount> countReportedIssuesByDay(
            long projectId,
            LocalDate fromInclusive,
            LocalDate toInclusive
    ) {
        validateDateRange(fromInclusive, toInclusive);

        String sql = """
                select trunc(reported_at) as reported_day, count(*) as issue_count
                from issues
                where project_id = ?
                  and status <> 'DELETED'
                  and (? is null or reported_at >= ?)
                  and (? is null or reported_at < ?)
                group by trunc(reported_at)
                order by reported_day
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            LocalDate toExclusive = toInclusive == null ? null : toInclusive.plusDays(1);
            setNullableDate(statement, 2, fromInclusive);
            setNullableDate(statement, 3, fromInclusive);
            setNullableDate(statement, 4, toExclusive);
            setNullableDate(statement, 5, toExclusive);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<LocalDate, Integer> countByDate = new HashMap<>();
                while (resultSet.next()) {
                    Date date = resultSet.getDate("reported_day");
                    LocalDate localDate = date.toLocalDate();
                    countByDate.put(localDate, resultSet.getInt("issue_count"));
                }
                return fillDailyRange(countByDate, fromInclusive, toInclusive);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to count reported issues by day.", exception);
        }
    }

    @Override
    public List<MonthlyIssueCount> countReportedIssuesByMonth(long projectId) {
        return countReportedIssuesByMonth(projectId, null, null);
    }

    @Override
    public List<MonthlyIssueCount> countReportedIssuesByMonth(
            long projectId,
            YearMonth fromInclusive,
            YearMonth toInclusive
    ) {
        validateMonthRange(fromInclusive, toInclusive);

        String sql = """
                select to_char(reported_at, 'YYYY-MM') as reported_month, count(*) as issue_count
                from issues
                where project_id = ?
                  and status <> 'DELETED'
                  and (? is null or reported_at >= ?)
                  and (? is null or reported_at < ?)
                group by to_char(reported_at, 'YYYY-MM')
                order by reported_month
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            LocalDate fromDate = fromInclusive == null ? null : fromInclusive.atDay(1);
            LocalDate toExclusive = toInclusive == null ? null : toInclusive.plusMonths(1).atDay(1);
            setNullableDate(statement, 2, fromDate);
            setNullableDate(statement, 3, fromDate);
            setNullableDate(statement, 4, toExclusive);
            setNullableDate(statement, 5, toExclusive);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<YearMonth, Integer> countByMonth = new HashMap<>();
                while (resultSet.next()) {
                    YearMonth month = YearMonth.parse(resultSet.getString("reported_month"));
                    countByMonth.put(month, resultSet.getInt("issue_count"));
                }
                return fillMonthlyRange(countByMonth, fromInclusive, toInclusive);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to count reported issues by month.", exception);
        }
    }

    @Override
    public Map<YearMonth, Map<IssueStatus, Integer>> countByStatusByMonth(
            long projectId,
            YearMonth fromInclusive,
            YearMonth toInclusive
    ) {
        validateMonthRange(fromInclusive, toInclusive);

        String sql = """
                select to_char(reported_at, 'YYYY-MM') as reported_month,
                       status,
                       count(*) as issue_count
                from issues
                where project_id = ?
                  and status <> 'DELETED'
                  and (? is null or reported_at >= ?)
                  and (? is null or reported_at < ?)
                group by to_char(reported_at, 'YYYY-MM'), status
                order by reported_month, status
                """;
        Map<YearMonth, Map<IssueStatus, Integer>> counts = emptyStatusMonthRange(fromInclusive, toInclusive);
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            LocalDate fromDate = fromInclusive == null ? null : fromInclusive.atDay(1);
            LocalDate toExclusive = toInclusive == null ? null : toInclusive.plusMonths(1).atDay(1);
            setNullableDate(statement, 2, fromDate);
            setNullableDate(statement, 3, fromDate);
            setNullableDate(statement, 4, toExclusive);
            setNullableDate(statement, 5, toExclusive);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    YearMonth month = YearMonth.parse(resultSet.getString("reported_month"));
                    counts.computeIfAbsent(month, ignored -> new EnumMap<>(IssueStatus.class))
                            .put(IssueStatus.valueOf(resultSet.getString("status")), resultSet.getInt("issue_count"));
                }
                return freezeNestedCounts(counts);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to count issues by status and month.", exception);
        }
    }

    @Override
    public Map<YearMonth, Map<Priority, Integer>> countByPriorityByMonth(
            long projectId,
            YearMonth fromInclusive,
            YearMonth toInclusive
    ) {
        validateMonthRange(fromInclusive, toInclusive);

        String sql = """
                select to_char(reported_at, 'YYYY-MM') as reported_month,
                       priority,
                       count(*) as issue_count
                from issues
                where project_id = ?
                  and status <> 'DELETED'
                  and (? is null or reported_at >= ?)
                  and (? is null or reported_at < ?)
                group by to_char(reported_at, 'YYYY-MM'), priority
                order by reported_month, priority
                """;
        Map<YearMonth, Map<Priority, Integer>> counts = emptyPriorityMonthRange(fromInclusive, toInclusive);
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            LocalDate fromDate = fromInclusive == null ? null : fromInclusive.atDay(1);
            LocalDate toExclusive = toInclusive == null ? null : toInclusive.plusMonths(1).atDay(1);
            setNullableDate(statement, 2, fromDate);
            setNullableDate(statement, 3, fromDate);
            setNullableDate(statement, 4, toExclusive);
            setNullableDate(statement, 5, toExclusive);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    YearMonth month = YearMonth.parse(resultSet.getString("reported_month"));
                    counts.computeIfAbsent(month, ignored -> new EnumMap<>(Priority.class))
                            .put(Priority.valueOf(resultSet.getString("priority")), resultSet.getInt("issue_count"));
                }
                return freezeNestedCounts(counts);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to count issues by priority and month.", exception);
        }
    }

    @Override
    public StatisticsReport buildReport(
            long projectId,
            LocalDate dailyFromInclusive,
            LocalDate dailyToInclusive,
            YearMonth monthlyFromInclusive,
            YearMonth monthlyToInclusive
    ) {
        return StatisticsReport.create(
                countByStatus(projectId),
                countByPriority(projectId),
                countReportedIssuesByDay(projectId, dailyFromInclusive, dailyToInclusive),
                countReportedIssuesByMonth(projectId, monthlyFromInclusive, monthlyToInclusive),
                countByStatusByMonth(projectId, monthlyFromInclusive, monthlyToInclusive),
                countByPriorityByMonth(projectId, monthlyFromInclusive, monthlyToInclusive)
        );
    }

    private static List<DailyIssueCount> fillDailyRange(
            Map<LocalDate, Integer> countByDate,
            LocalDate fromInclusive,
            LocalDate toInclusive
    ) {
        if (fromInclusive == null || toInclusive == null) {
            return countByDate.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> DailyIssueCount.create(entry.getKey(), entry.getValue()))
                    .toList();
        }

        List<DailyIssueCount> counts = new ArrayList<>();
        for (LocalDate date = fromInclusive; !date.isAfter(toInclusive); date = date.plusDays(1)) {
            counts.add(DailyIssueCount.create(date, countByDate.getOrDefault(date, 0)));
        }
        return counts;
    }

    private static List<MonthlyIssueCount> fillMonthlyRange(
            Map<YearMonth, Integer> countByMonth,
            YearMonth fromInclusive,
            YearMonth toInclusive
    ) {
        if (fromInclusive == null || toInclusive == null) {
            return countByMonth.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> MonthlyIssueCount.create(entry.getKey(), entry.getValue()))
                    .toList();
        }

        List<MonthlyIssueCount> counts = new ArrayList<>();
        for (YearMonth month = fromInclusive; !month.isAfter(toInclusive); month = month.plusMonths(1)) {
            counts.add(MonthlyIssueCount.create(month, countByMonth.getOrDefault(month, 0)));
        }
        return counts;
    }

    private static void validateDateRange(LocalDate fromInclusive, LocalDate toInclusive) {
        if (fromInclusive != null && toInclusive != null && fromInclusive.isAfter(toInclusive)) {
            throw new IllegalArgumentException("fromInclusive must not be after toInclusive");
        }
    }

    private static void validateMonthRange(YearMonth fromInclusive, YearMonth toInclusive) {
        if (fromInclusive != null && toInclusive != null && fromInclusive.isAfter(toInclusive)) {
            throw new IllegalArgumentException("fromInclusive must not be after toInclusive");
        }
    }

    private static Map<YearMonth, Map<IssueStatus, Integer>> emptyStatusMonthRange(
            YearMonth fromInclusive,
            YearMonth toInclusive
    ) {
        Map<YearMonth, Map<IssueStatus, Integer>> counts = new LinkedHashMap<>();
        if (fromInclusive == null || toInclusive == null) {
            return counts;
        }
        for (YearMonth month = fromInclusive; !month.isAfter(toInclusive); month = month.plusMonths(1)) {
            counts.put(month, new EnumMap<>(IssueStatus.class));
        }
        return counts;
    }

    private static Map<YearMonth, Map<Priority, Integer>> emptyPriorityMonthRange(
            YearMonth fromInclusive,
            YearMonth toInclusive
    ) {
        Map<YearMonth, Map<Priority, Integer>> counts = new LinkedHashMap<>();
        if (fromInclusive == null || toInclusive == null) {
            return counts;
        }
        for (YearMonth month = fromInclusive; !month.isAfter(toInclusive); month = month.plusMonths(1)) {
            counts.put(month, new EnumMap<>(Priority.class));
        }
        return counts;
    }

    private static <K> Map<YearMonth, Map<K, Integer>> freezeNestedCounts(
            Map<YearMonth, Map<K, Integer>> counts
    ) {
        Map<YearMonth, Map<K, Integer>> frozen = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> frozen.put(entry.getKey(), Map.copyOf(entry.getValue())));
        return Map.copyOf(frozen);
    }

    private static void setNullableDate(PreparedStatement statement, int index, LocalDate date) throws SQLException {
        if (date == null) {
            statement.setNull(index, Types.DATE);
            return;
        }
        statement.setDate(index, Date.valueOf(date));
    }
}
