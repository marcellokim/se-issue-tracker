package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public final class StatisticsService {

    private final PermissionPolicy permissionPolicy;
    private final StatisticsRepository statisticsRepository;
    private final UserRepository userRepository;

    public StatisticsService(
            PermissionPolicy permissionPolicy,
            StatisticsRepository statisticsRepository,
            UserRepository userRepository) {
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.statisticsRepository = Objects.requireNonNull(statisticsRepository, "statisticsRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    }

    public StatisticsReportResult viewStatistics(
            long projectId,
            LocalDate dailyFromInclusive,
            LocalDate dailyToInclusive,
            YearMonth monthlyFromInclusive,
            YearMonth monthlyToInclusive,
            User actor) {
        if (projectId <= 0L) {
            throw new IllegalArgumentException(" project Id must be positive");
        }
        Objects.requireNonNull(actor, "actor");
        requireActiveProjectMember(actor, projectId);
        permissionPolicy.assertCanViewStatistics(actor);
        requireOrderedRange(dailyFromInclusive, dailyToInclusive, "dailyFromInclusive", "dailyToInclusive");
        requireOrderedRange(monthlyFromInclusive, monthlyToInclusive, "monthlyFromInclusive", "monthlyToInclusive");

        return StatisticsReportResult.from(statisticsRepository.calculateProjectStatistics(
                projectId,
                dailyFromInclusive,
                dailyToInclusive,
                monthlyFromInclusive,
                monthlyToInclusive));
    }

    private void requireActiveProjectMember(User actor, long projectId) {
        if (!isActiveProjectMember(actor, projectId)) {
            throw new SecurityException("Only project members can view statistics.");
        }
    }

    private boolean isActiveProjectMember(User actor, long projectId) {
        return userRepository.existsActiveProjectMember(projectId, actor.getLoginId());
    }

    private static <T extends Comparable<T>> void requireOrderedRange(
            T fromInclusive,
            T toInclusive,
            String fromName,
            String toName) {
        if (fromInclusive != null && toInclusive != null && fromInclusive.compareTo(toInclusive) > 0) {
            throw new IllegalArgumentException(fromName + " must be <= " + toName);
        }
    }
}
