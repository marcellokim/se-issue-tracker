package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.repository.StatisticsReport;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record StatisticsReportResult(
        Map<IssueStatus, Integer> statusCounts,
        Map<Priority, Integer> priorityCounts,
        List<DailyCountResult> dailyCounts,
        List<MonthlyCountResult> monthlyCounts,
        Map<YearMonth, Map<IssueStatus, Integer>> monthlyStatusCounts,
        Map<YearMonth, Map<Priority, Integer>> monthlyPriorityCounts,
        List<DailyCountResult> dailyStatusChangeCounts,
        List<MonthlyCountResult> monthlyStatusChangeCounts,
        List<DailyCountResult> dailyCommentCounts,
        List<MonthlyCountResult> monthlyCommentCounts
) {

    public StatisticsReportResult {
        statusCounts = Map.copyOf(Objects.requireNonNull(statusCounts, "statusCounts"));
        priorityCounts = Map.copyOf(Objects.requireNonNull(priorityCounts, "priorityCounts"));
        dailyCounts = List.copyOf(Objects.requireNonNull(dailyCounts, "dailyCounts"));
        monthlyCounts = List.copyOf(Objects.requireNonNull(monthlyCounts, "monthlyCounts"));
        monthlyStatusCounts = copyNested(monthlyStatusCounts);
        monthlyPriorityCounts = copyNested(monthlyPriorityCounts);
        dailyStatusChangeCounts = List.copyOf(
                Objects.requireNonNull(dailyStatusChangeCounts, "dailyStatusChangeCounts"));
        monthlyStatusChangeCounts = List.copyOf(
                Objects.requireNonNull(monthlyStatusChangeCounts, "monthlyStatusChangeCounts"));
        dailyCommentCounts = List.copyOf(Objects.requireNonNull(dailyCommentCounts, "dailyCommentCounts"));
        monthlyCommentCounts = List.copyOf(Objects.requireNonNull(monthlyCommentCounts, "monthlyCommentCounts"));
    }

    public static StatisticsReportResult from(StatisticsReport report) {
        Objects.requireNonNull(report, "report");
        return new StatisticsReportResult(
                report.statusCounts(),
                report.priorityCounts(),
                report.dailyCounts().stream().map(DailyCountResult::from).toList(),
                report.monthlyCounts().stream().map(MonthlyCountResult::from).toList(),
                report.monthlyStatusCounts(),
                report.monthlyPriorityCounts(),
                report.dailyStatusChangeCounts().stream().map(DailyCountResult::from).toList(),
                report.monthlyStatusChangeCounts().stream().map(MonthlyCountResult::from).toList(),
                report.dailyCommentCounts().stream().map(DailyCountResult::from).toList(),
                report.monthlyCommentCounts().stream().map(MonthlyCountResult::from).toList());
    }

    private static <K> Map<YearMonth, Map<K, Integer>> copyNested(Map<YearMonth, Map<K, Integer>> source) {
        Objects.requireNonNull(source, "source");
        Map<YearMonth, Map<K, Integer>> copy = new LinkedHashMap<>();
        source.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> copy.put(entry.getKey(), Map.copyOf(entry.getValue())));
        return Map.copyOf(copy);
    }
}
