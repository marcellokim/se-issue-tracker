package com.github.marcellokim.issuetracker.repository;

import java.time.LocalDate;
import java.time.YearMonth;

public interface StatisticsRepository {

    record DailyIssueCount(LocalDate date, int count) {}
    record MonthlyIssueCount(YearMonth month, int count) {}

    StatisticsReport calculateProjectStatistics(
            long projectId,
            LocalDate dailyFromInclusive,
            LocalDate dailyToInclusive,
            YearMonth monthlyFromInclusive,
            YearMonth monthlyToInclusive);
}