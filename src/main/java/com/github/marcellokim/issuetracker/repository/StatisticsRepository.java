package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public interface StatisticsRepository {

    Map<IssueStatus, Integer> countByStatus(long projectId);

    Map<Priority, Integer> countByPriority(long projectId);

    List<DailyIssueCount> countReportedIssuesByDay(long projectId);

    List<DailyIssueCount> countReportedIssuesByDay(long projectId, LocalDate fromInclusive, LocalDate toInclusive);

    List<MonthlyIssueCount> countReportedIssuesByMonth(long projectId);

    List<MonthlyIssueCount> countReportedIssuesByMonth(long projectId, YearMonth fromInclusive, YearMonth toInclusive);

    default List<DailyIssueCount> countStatusChangesByDay(long projectId) {
        return countStatusChangesByDay(projectId, null, null);
    }

    default List<DailyIssueCount> countStatusChangesByDay(
            long projectId,
            LocalDate fromInclusive,
            LocalDate toInclusive
    ) {
        return List.of();
    }

    default List<MonthlyIssueCount> countStatusChangesByMonth(long projectId) {
        return countStatusChangesByMonth(projectId, null, null);
    }

    default List<MonthlyIssueCount> countStatusChangesByMonth(
            long projectId,
            YearMonth fromInclusive,
            YearMonth toInclusive
    ) {
        return List.of();
    }

    default List<DailyIssueCount> countCommentsByDay(long projectId) {
        return countCommentsByDay(projectId, null, null);
    }

    default List<DailyIssueCount> countCommentsByDay(
            long projectId,
            LocalDate fromInclusive,
            LocalDate toInclusive
    ) {
        return List.of();
    }

    default List<MonthlyIssueCount> countCommentsByMonth(long projectId) {
        return countCommentsByMonth(projectId, null, null);
    }

    default List<MonthlyIssueCount> countCommentsByMonth(
            long projectId,
            YearMonth fromInclusive,
            YearMonth toInclusive
    ) {
        return List.of();
    }

    default Map<YearMonth, Map<IssueStatus, Integer>> countByStatusByMonth(long projectId) {
        return countByStatusByMonth(projectId, null, null);
    }

    default Map<YearMonth, Map<IssueStatus, Integer>> countByStatusByMonth(
            long projectId,
            YearMonth fromInclusive,
            YearMonth toInclusive
    ) {
        return Map.of();
    }

    default Map<YearMonth, Map<Priority, Integer>> countByPriorityByMonth(long projectId) {
        return countByPriorityByMonth(projectId, null, null);
    }

    default Map<YearMonth, Map<Priority, Integer>> countByPriorityByMonth(
            long projectId,
            YearMonth fromInclusive,
            YearMonth toInclusive
    ) {
        return Map.of();
    }

    StatisticsReport buildReport(
            long projectId,
            LocalDate dailyFromInclusive,
            LocalDate dailyToInclusive,
            YearMonth monthlyFromInclusive,
            YearMonth monthlyToInclusive
    );
}
