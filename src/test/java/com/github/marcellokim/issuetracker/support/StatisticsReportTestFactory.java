package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public final class StatisticsReportTestFactory {

    private StatisticsReportTestFactory() {
    }

    public static StatisticsReport create(
            Map<IssueStatus, Integer> statusCounts,
            Map<Priority, Integer> priorityCounts,
            List<DailyIssueCount> dailyCounts,
            List<MonthlyIssueCount> monthlyCounts) {
        return create(statusCounts, priorityCounts, dailyCounts, monthlyCounts, Map.of(), Map.of());
    }

    public static StatisticsReport create(
            Map<IssueStatus, Integer> statusCounts,
            Map<Priority, Integer> priorityCounts,
            List<DailyIssueCount> dailyCounts,
            List<MonthlyIssueCount> monthlyCounts,
            Map<YearMonth, Map<IssueStatus, Integer>> monthlyStatusCounts,
            Map<YearMonth, Map<Priority, Integer>> monthlyPriorityCounts) {
        return StatisticsReport.create(
                statusCounts,
                priorityCounts,
                dailyCounts,
                monthlyCounts,
                monthlyStatusCounts,
                monthlyPriorityCounts,
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }
}
