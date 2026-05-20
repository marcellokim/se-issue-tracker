package com.github.marcellokim.issuetracker.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StatisticsReport {

    private final Map<IssueStatus, Integer> statusCounts;
    private final Map<Priority, Integer> priorityCounts;
    private final List<DailyIssueCount> dailyCounts;
    private final List<MonthlyIssueCount> monthlyCounts;

    public static StatisticsReport create(
            Map<IssueStatus, Integer> statusCounts,
            Map<Priority, Integer> priorityCounts,
            List<DailyIssueCount> dailyCounts,
            List<MonthlyIssueCount> monthlyCounts
    ) {
        return new StatisticsReport(statusCounts, priorityCounts, dailyCounts, monthlyCounts);
    }

    private StatisticsReport(
            Map<IssueStatus, Integer> statusCounts,
            Map<Priority, Integer> priorityCounts,
            List<DailyIssueCount> dailyCounts,
            List<MonthlyIssueCount> monthlyCounts
    ) {
        this.statusCounts = Map.copyOf(statusCounts);
        this.priorityCounts = Map.copyOf(priorityCounts);
        this.dailyCounts = List.copyOf(dailyCounts);
        this.monthlyCounts = List.copyOf(monthlyCounts);
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
                && Objects.equals(monthlyCounts, that.monthlyCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCounts, priorityCounts, dailyCounts, monthlyCounts);
    }

    @Override
    public String toString() {
        return "StatisticsReport[statusCounts=" + statusCounts
                + ", priorityCounts=" + priorityCounts
                + ", dailyCounts=" + dailyCounts
                + ", monthlyCounts=" + monthlyCounts + "]";
    }
}
