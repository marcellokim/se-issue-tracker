package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.StatisticsReportResult;
import com.github.marcellokim.issuetracker.service.StatisticsService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public final class StatisticsController {

    private final AuthenticationService authenticationService;
    private final StatisticsService statisticsService;

    public StatisticsController(
            AuthenticationService authenticationService,
            StatisticsService statisticsService) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.statisticsService = Objects.requireNonNull(statisticsService, "statisticsService");
    }

    public StatisticsReportResult viewStatistics(
            long projectId,
            LocalDate dailyFromInclusive,
            LocalDate dailyToInclusive,
            YearMonth monthlyFromInclusive,
            YearMonth monthlyToInclusive) {
        User user = requireCurrentUser();
        return statisticsService.viewStatistics(
                projectId,
                dailyFromInclusive,
                dailyToInclusive,
                monthlyFromInclusive,
                monthlyToInclusive,
                user);
    }

    public StatisticsReportResult viewStatistics(long projectId) {
        return viewStatistics(projectId, null, null, null, null);
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }
}
