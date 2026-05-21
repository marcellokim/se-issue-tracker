package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public final class StatisticsService {

    private final PermissionPolicy permissionPolicy;
    private final StatisticsRepository statisticsRepository;

    public StatisticsService(
            PermissionPolicy permissionPolicy,
            StatisticsRepository statisticsRepository
    ) {
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.statisticsRepository = Objects.requireNonNull(statisticsRepository, "statisticsRepository");
    }

    public StatisticsReport viewStatistics(
            long projectId,
            LocalDate dailyFromInclusive,
            LocalDate dailyToInclusive,
            YearMonth monthlyFromInclusive,
            YearMonth monthlyToInclusive,
            User actor
    ) {
        /*
         * Range validation is part of the statistics use-case boundary, not controller
         * plumbing, so repository query inputs are guarded in one place.
         */
        permissionPolicy.assertCanViewStatistics(actor, projectId);
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
