package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.repository.StatisticsReport;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public final class FakeStatisticsRepository implements StatisticsRepository {

    private StatisticsReport report;
    private RuntimeException failure;
    private long lastProjectId;
    private LocalDate lastDailyFromInclusive;
    private LocalDate lastDailyToInclusive;
    private YearMonth lastMonthlyFromInclusive;
    private YearMonth lastMonthlyToInclusive;

    public FakeStatisticsRepository(StatisticsReport report) {
        this.report = Objects.requireNonNull(report, "report");
    }

    public void failWith(RuntimeException failure) {
        this.failure = Objects.requireNonNull(failure, "failure");
    }

    public long lastProjectId() {
        return lastProjectId;
    }

    public LocalDate lastDailyFromInclusive() {
        return lastDailyFromInclusive;
    }

    public LocalDate lastDailyToInclusive() {
        return lastDailyToInclusive;
    }

    public YearMonth lastMonthlyFromInclusive() {
        return lastMonthlyFromInclusive;
    }

    public YearMonth lastMonthlyToInclusive() {
        return lastMonthlyToInclusive;
    }

    @Override
    public StatisticsReport calculateProjectStatistics(
            long projectId,
            LocalDate dailyFromInclusive,
            LocalDate dailyToInclusive,
            YearMonth monthlyFromInclusive,
            YearMonth monthlyToInclusive) {
        if (failure != null) {
            throw failure;
        }
        lastProjectId = projectId;
        lastDailyFromInclusive = dailyFromInclusive;
        lastDailyToInclusive = dailyToInclusive;
        lastMonthlyFromInclusive = monthlyFromInclusive;
        lastMonthlyToInclusive = monthlyToInclusive;
        return report;
    }
}
