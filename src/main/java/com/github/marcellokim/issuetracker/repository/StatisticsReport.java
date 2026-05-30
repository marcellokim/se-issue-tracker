package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.DailyIssueCount;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.MonthlyIssueCount;
import java.time.YearMonth;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StatisticsReport {

    private final Map<IssueStatus, Integer> statusCounts;
    private final Map<Priority, Integer> priorityCounts;
    private final List<DailyIssueCount> dailyCounts;
    private final List<MonthlyIssueCount> monthlyCounts;
    private final Map<YearMonth, Map<IssueStatus, Integer>> monthlyStatusCounts;
    private final Map<YearMonth, Map<Priority, Integer>> monthlyPriorityCounts;
    private final List<DailyIssueCount> dailyStatusChangeCounts;
    private final List<MonthlyIssueCount> monthlyStatusChangeCounts;
    private final List<DailyIssueCount> dailyCommentCounts;
    private final List<MonthlyIssueCount> monthlyCommentCounts;

    public static StatisticsReport create(
            Map<IssueStatus, Integer> statusCounts,
            Map<Priority, Integer> priorityCounts,
            List<DailyIssueCount> dailyCounts,
            List<MonthlyIssueCount> monthlyCounts,
            Map<YearMonth, Map<IssueStatus, Integer>> monthlyStatusCounts,
            Map<YearMonth, Map<Priority, Integer>> monthlyPriorityCounts,
            List<DailyIssueCount> dailyStatusChangeCounts,
            List<MonthlyIssueCount> monthlyStatusChangeCounts,
            List<DailyIssueCount> dailyCommentCounts,
            List<MonthlyIssueCount> monthlyCommentCounts) {
        return new StatisticsReport(
                statusCounts,
                priorityCounts,
                dailyCounts,
                monthlyCounts,
                monthlyStatusCounts,
                monthlyPriorityCounts,
                dailyStatusChangeCounts,
                monthlyStatusChangeCounts,
                dailyCommentCounts,
                monthlyCommentCounts);
    }

    private StatisticsReport(
            Map<IssueStatus, Integer> statusCounts,
            Map<Priority, Integer> priorityCounts,
            List<DailyIssueCount> dailyCounts,
            List<MonthlyIssueCount> monthlyCounts,
            Map<YearMonth, Map<IssueStatus, Integer>> monthlyStatusCounts,
            Map<YearMonth, Map<Priority, Integer>> monthlyPriorityCounts,
            List<DailyIssueCount> dailyStatusChangeCounts,
            List<MonthlyIssueCount> monthlyStatusChangeCounts,
            List<DailyIssueCount> dailyCommentCounts,
            List<MonthlyIssueCount> monthlyCommentCounts) {
        this.statusCounts = Map.copyOf(statusCounts);
        this.priorityCounts = Map.copyOf(priorityCounts);
        this.dailyCounts = List.copyOf(dailyCounts);
        this.monthlyCounts = List.copyOf(monthlyCounts);
        this.monthlyStatusCounts = copyNestedCounts(monthlyStatusCounts);
        this.monthlyPriorityCounts = copyNestedCounts(monthlyPriorityCounts);
        this.dailyStatusChangeCounts = List.copyOf(dailyStatusChangeCounts);
        this.monthlyStatusChangeCounts = List.copyOf(monthlyStatusChangeCounts);
        this.dailyCommentCounts = List.copyOf(dailyCommentCounts);
        this.monthlyCommentCounts = List.copyOf(monthlyCommentCounts);
    }

    public Map<IssueStatus, Integer> statusCounts() {
        return statusCounts;
    }

    public Map<Priority, Integer> priorityCounts() {
        return priorityCounts;
    }

    public List<DailyIssueCount> dailyCounts() {
        return dailyCounts;
    }

    public List<MonthlyIssueCount> monthlyCounts() {
        return monthlyCounts;
    }

    public Map<YearMonth, Map<IssueStatus, Integer>> monthlyStatusCounts() {
        return monthlyStatusCounts;
    }

    public Map<YearMonth, Map<Priority, Integer>> monthlyPriorityCounts() {
        return monthlyPriorityCounts;
    }

    public List<DailyIssueCount> dailyStatusChangeCounts() {
        return dailyStatusChangeCounts;
    }

    public List<MonthlyIssueCount> monthlyStatusChangeCounts() {
        return monthlyStatusChangeCounts;
    }

    public List<DailyIssueCount> dailyCommentCounts() {
        return dailyCommentCounts;
    }

    public List<MonthlyIssueCount> monthlyCommentCounts() {
        return monthlyCommentCounts;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StatisticsReport that)) {
            return false;
        }
        return Objects.equals(statusCounts, that.statusCounts)
                && Objects.equals(priorityCounts, that.priorityCounts)
                && Objects.equals(dailyCounts, that.dailyCounts)
                && Objects.equals(monthlyCounts, that.monthlyCounts)
                && Objects.equals(monthlyStatusCounts, that.monthlyStatusCounts)
                && Objects.equals(monthlyPriorityCounts, that.monthlyPriorityCounts)
                && Objects.equals(dailyStatusChangeCounts, that.dailyStatusChangeCounts)
                && Objects.equals(monthlyStatusChangeCounts, that.monthlyStatusChangeCounts)
                && Objects.equals(dailyCommentCounts, that.dailyCommentCounts)
                && Objects.equals(monthlyCommentCounts, that.monthlyCommentCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                statusCounts,
                priorityCounts,
                dailyCounts,
                monthlyCounts,
                monthlyStatusCounts,
                monthlyPriorityCounts,
                dailyStatusChangeCounts,
                monthlyStatusChangeCounts,
                dailyCommentCounts,
                monthlyCommentCounts);
    }

    @Override
    public String toString() {
        return "StatisticsReport[statusCounts=" + statusCounts
                + ", priorityCounts=" + priorityCounts
                + ", dailyCounts=" + dailyCounts
                + ", monthlyCounts=" + monthlyCounts
                + ", monthlyStatusCounts=" + monthlyStatusCounts
                + ", monthlyPriorityCounts=" + monthlyPriorityCounts
                + ", dailyStatusChangeCounts=" + dailyStatusChangeCounts
                + ", monthlyStatusChangeCounts=" + monthlyStatusChangeCounts
                + ", dailyCommentCounts=" + dailyCommentCounts
                + ", monthlyCommentCounts=" + monthlyCommentCounts + "]";
    }

    private static <K> Map<YearMonth, Map<K, Integer>> copyNestedCounts(Map<YearMonth, Map<K, Integer>> counts) {
        Objects.requireNonNull(counts, "counts must not be null");
        Map<YearMonth, Map<K, Integer>> copy = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> copy.put(
                        Objects.requireNonNull(entry.getKey(), "month must not be null"),
                        Map.copyOf(Objects.requireNonNull(entry.getValue(), "monthly counts must not be null"))));
        return Collections.unmodifiableMap(copy);
    }
}
