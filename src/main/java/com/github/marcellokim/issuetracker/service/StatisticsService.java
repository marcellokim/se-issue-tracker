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
        /*
         * 기간 검증은 statistics use-case 경계 책임.
         * repository query input은 service 한 곳에서 보호.
         */
        permissionPolicy.assertCanViewStatistics(actor, projectId);
        requireActiveProjectMember(actor, projectId);
        requireOrderedRange(dailyFromInclusive, dailyToInclusive, "dailyFromInclusive", "dailyToInclusive");
        requireOrderedRange(monthlyFromInclusive, monthlyToInclusive, "monthlyFromInclusive", "monthlyToInclusive");

        return StatisticsReportResult.from(statisticsRepository.buildReport(
                projectId,
                dailyFromInclusive,
                dailyToInclusive,
                monthlyFromInclusive,
                monthlyToInclusive));
    }

    public boolean canViewStatistics(long projectId, User actor) {
        return permissionPolicy.canViewStatistics(actor, projectId) && isActiveProjectMember(actor, projectId);
    }

    private void requireActiveProjectMember(User actor, long projectId) {
        if (!isActiveProjectMember(actor, projectId)) {
            throw new SecurityException("Only project members can view statistics.");
        }
    }

    private boolean isActiveProjectMember(User actor, long projectId) {
        return userRepository.findActiveByRole(projectId, actor.getRole()).stream()
                .anyMatch(user -> user.getLoginId().equals(actor.getLoginId()));
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
