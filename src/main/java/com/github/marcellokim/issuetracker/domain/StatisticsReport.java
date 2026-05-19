package com.github.marcellokim.issuetracker.domain;

import java.util.List;
import java.util.Map;

public record StatisticsReport(
        Map<IssueStatus, Integer> statusCounts,
        Map<Priority, Integer> priorityCounts,
        List<DailyIssueCount> dailyCounts,
        List<MonthlyIssueCount> monthlyCounts
) {

    public StatisticsReport {
        statusCounts = Map.copyOf(statusCounts);
        priorityCounts = Map.copyOf(priorityCounts);
        dailyCounts = List.copyOf(dailyCounts);
        monthlyCounts = List.copyOf(monthlyCounts);
    }
}
