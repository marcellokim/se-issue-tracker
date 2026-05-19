package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public final class StatisticsController {

    private final AuthenticationService authenticationService;
    private final PermissionPolicy permissionPolicy;
    private final StatisticsRepository statisticsRepository;

    public StatisticsController(
            AuthenticationService authenticationService,
            PermissionPolicy permissionPolicy,
            StatisticsRepository statisticsRepository
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.statisticsRepository = Objects.requireNonNull(statisticsRepository, "statisticsRepository");
    }

    public StatisticsReport viewStatistics(
            long projectId,
            LocalDate dailyFromInclusive,
            LocalDate dailyToInclusive,
            YearMonth monthlyFromInclusive,
            YearMonth monthlyToInclusive
    ) {
        User user = requireCurrentUser();
        permissionPolicy.assertCanViewStatistics(user, projectId);
        requireOrderedRange(dailyFromInclusive, dailyToInclusive, "dailyFromInclusive", "dailyToInclusive");
        requireOrderedRange(monthlyFromInclusive, monthlyToInclusive, "monthlyFromInclusive", "monthlyToInclusive");

        return statisticsRepository.buildReport(
                projectId,
                dailyFromInclusive,
                dailyToInclusive,
                monthlyFromInclusive,
                monthlyToInclusive
        );
    }

    public StatisticsReport viewStatistics(long projectId) {
        return viewStatistics(projectId, null, null, null, null);
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }

    private static <T extends Comparable<T>> void requireOrderedRange(
            T fromInclusive,
            T toInclusive,
            String fromName,
            String toName
    ) {
        if (fromInclusive != null && toInclusive != null && fromInclusive.compareTo(toInclusive) > 0) {
            throw new IllegalArgumentException(fromName + " must be <= " + toName);
        }
    }
}
