package com.github.marcellokim.issuetracker.domain;

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

    public static StatisticsReport create(
            Map<IssueStatus, Integer> statusCounts,
            Map<Priority, Integer> priorityCounts,
            List<DailyIssueCount> dailyCounts,
            List<MonthlyIssueCount> monthlyCounts
    ) {
        return create(statusCounts, priorityCounts, dailyCounts, monthlyCounts, Map.of(), Map.of());
    }

    public static StatisticsReport create(
            Map<IssueStatus, Integer> statusCounts,
            Map<Priority, Integer> priorityCounts,
            List<DailyIssueCount> dailyCounts,
            List<MonthlyIssueCount> monthlyCounts,
            Map<YearMonth, Map<IssueStatus, Integer>> monthlyStatusCounts,
            Map<YearMonth, Map<Priority, Integer>> monthlyPriorityCounts
    ) {
        return new StatisticsReport(
                statusCounts,
                priorityCounts,
                dailyCounts,
                monthlyCounts,
                monthlyStatusCounts,
                monthlyPriorityCounts);
    }

    private StatisticsReport(
            Map<IssueStatus, Integer> statusCounts,
            Map<Priority, Integer> priorityCounts,
            List<DailyIssueCount> dailyCounts,
            List<MonthlyIssueCount> monthlyCounts,
            Map<YearMonth, Map<IssueStatus, Integer>> monthlyStatusCounts,
            Map<YearMonth, Map<Priority, Integer>> monthlyPriorityCounts
    ) {
        this.statusCounts = Map.copyOf(statusCounts);
        this.priorityCounts = Map.copyOf(priorityCounts);
        this.dailyCounts = List.copyOf(dailyCounts);
        this.monthlyCounts = List.copyOf(monthlyCounts);
        this.monthlyStatusCounts = copyNestedCounts(monthlyStatusCounts);
        this.monthlyPriorityCounts = copyNestedCounts(monthlyPriorityCounts);
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
                && Objects.equals(monthlyPriorityCounts, that.monthlyPriorityCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                statusCounts,
                priorityCounts,
                dailyCounts,
                monthlyCounts,
                monthlyStatusCounts,
                monthlyPriorityCounts);
    }

    @Override
    public String toString() {
        return "StatisticsReport[statusCounts=" + statusCounts
                + ", priorityCounts=" + priorityCounts
                + ", dailyCounts=" + dailyCounts
                + ", monthlyCounts=" + monthlyCounts
                + ", monthlyStatusCounts=" + monthlyStatusCounts
                + ", monthlyPriorityCounts=" + monthlyPriorityCounts + "]";
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
