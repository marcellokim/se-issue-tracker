package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StatisticsController {

    private final AuthenticationService authenticationService;
    private final PermissionPolicy permissionPolicy;
    private final IssueRepository issueRepository;

    public StatisticsController(
            AuthenticationService authenticationService,
            PermissionPolicy permissionPolicy,
            IssueRepository issueRepository
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
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

        List<Issue> issues = issueRepository.findByProject(projectId);
        return new StatisticsReport(
                countByStatus(issues),
                countByPriority(issues),
                countByDay(issues, dailyFromInclusive, dailyToInclusive),
                countByMonth(issues, monthlyFromInclusive, monthlyToInclusive)
        );
    }

    public StatisticsReport viewStatistics(long projectId) {
        return viewStatistics(projectId, null, null, null, null);
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }

    private static Map<IssueStatus, Integer> countByStatus(List<Issue> issues) {
        Map<IssueStatus, Integer> counts = new EnumMap<>(IssueStatus.class);
        for (Issue issue : issues) {
            counts.merge(issue.status(), 1, Integer::sum);
        }
        return counts;
    }

    private static Map<Priority, Integer> countByPriority(List<Issue> issues) {
        Map<Priority, Integer> counts = new EnumMap<>(Priority.class);
        for (Issue issue : issues) {
            counts.merge(issue.priority(), 1, Integer::sum);
        }
        return counts;
    }

    private static List<DailyIssueCount> countByDay(
            List<Issue> issues,
            LocalDate fromInclusive,
            LocalDate toInclusive
    ) {
        validateDateRange(fromInclusive, toInclusive);
        Map<LocalDate, Integer> counts = new HashMap<>();
        for (Issue issue : issues) {
            if (issue.reportedDate() == null) {
                continue;
            }
            LocalDate date = issue.reportedDate().toLocalDate();
            if (isBeforeFrom(date, fromInclusive) || isAfterTo(date, toInclusive)) {
                continue;
            }
            counts.merge(date, 1, Integer::sum);
        }

        if (fromInclusive == null || toInclusive == null) {
            return counts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new DailyIssueCount(entry.getKey(), entry.getValue()))
                    .toList();
        }

        List<DailyIssueCount> result = new ArrayList<>();
        for (LocalDate date = fromInclusive; !date.isAfter(toInclusive); date = date.plusDays(1)) {
            result.add(new DailyIssueCount(date, counts.getOrDefault(date, 0)));
        }
        return result;
    }

    private static List<MonthlyIssueCount> countByMonth(
            List<Issue> issues,
            YearMonth fromInclusive,
            YearMonth toInclusive
    ) {
        validateMonthRange(fromInclusive, toInclusive);
        Map<YearMonth, Integer> counts = new HashMap<>();
        for (Issue issue : issues) {
            if (issue.reportedDate() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(issue.reportedDate());
            if (isBeforeFrom(month, fromInclusive) || isAfterTo(month, toInclusive)) {
                continue;
            }
            counts.merge(month, 1, Integer::sum);
        }

        if (fromInclusive == null || toInclusive == null) {
            return counts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new MonthlyIssueCount(entry.getKey(), entry.getValue()))
                    .toList();
        }

        List<MonthlyIssueCount> result = new ArrayList<>();
        for (YearMonth month = fromInclusive; !month.isAfter(toInclusive); month = month.plusMonths(1)) {
            result.add(new MonthlyIssueCount(month, counts.getOrDefault(month, 0)));
        }
        return result;
    }

    private static boolean isBeforeFrom(LocalDate date, LocalDate fromInclusive) {
        return fromInclusive != null && date.isBefore(fromInclusive);
    }

    private static boolean isAfterTo(LocalDate date, LocalDate toInclusive) {
        return toInclusive != null && date.isAfter(toInclusive);
    }

    private static boolean isBeforeFrom(YearMonth month, YearMonth fromInclusive) {
        return fromInclusive != null && month.isBefore(fromInclusive);
    }

    private static boolean isAfterTo(YearMonth month, YearMonth toInclusive) {
        return toInclusive != null && month.isAfter(toInclusive);
    }

    private static void validateDateRange(LocalDate fromInclusive, LocalDate toInclusive) {
        if (fromInclusive != null && toInclusive != null && fromInclusive.isAfter(toInclusive)) {
            throw new IllegalArgumentException("fromInclusive must not be after toInclusive");
        }
    }

    private static void validateMonthRange(YearMonth fromInclusive, YearMonth toInclusive) {
        if (fromInclusive != null && toInclusive != null && fromInclusive.isAfter(toInclusive)) {
            throw new IllegalArgumentException("fromInclusive must not be after toInclusive");
        }
    }
}
