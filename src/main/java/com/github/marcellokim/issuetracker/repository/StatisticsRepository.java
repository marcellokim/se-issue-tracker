package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import java.time.LocalDate;
import java.time.YearMonth;

public interface StatisticsRepository {

    StatisticsReport calculateProjectStatistics(
            long projectId,
            LocalDate dailyFromInclusive,
            LocalDate dailyToInclusive,
            YearMonth monthlyFromInclusive,
            YearMonth monthlyToInclusive);
}