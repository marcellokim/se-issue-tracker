package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.repository.StatisticsRepository.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.repository.StatisticsReport;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.support.StatisticsReportTestFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Statistics service")
class StatisticsServiceTest {

    private static final long PROJECT_ID = 10L;
    private static final long OTHER_PROJECT_ID = 20L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 1, 0, 0);

    @Test
    @DisplayName("project member opens stats")
    void memberOpensStats() {
        User dev = user("dev1", Role.DEV);
        InMemoryUserRepository users = new InMemoryUserRepository(dev)
                .withProjectMembers(PROJECT_ID, dev.getLoginId());
        FakeStatisticsRepository statistics = new FakeStatisticsRepository();
        StatisticsService service = new StatisticsService(new PermissionPolicy(), statistics, users);

        StatisticsReportResult result = service.viewStatistics(PROJECT_ID, null, null, null, null, dev);

        assertEquals(1, result.statusCounts().get(IssueStatus.NEW));
        assertEquals(PROJECT_ID, statistics.lastProjectId);
    }

    @Test
    @DisplayName("outsider cannot open stats")
    void outsiderCannotOpenStats() {
        User dev = user("dev1", Role.DEV);
        InMemoryUserRepository users = new InMemoryUserRepository(dev)
                .withProjectMembers(OTHER_PROJECT_ID, dev.getLoginId());
        StatisticsService service = new StatisticsService(new PermissionPolicy(), new FakeStatisticsRepository(), users);

        assertThrows(SecurityException.class,
                () -> service.viewStatistics(PROJECT_ID, null, null, null, null, dev));
    }

    @Test
    @DisplayName("admin cannot open project stats")
    void adminCannotOpenProjectStats() {
        User admin = user("admin", Role.ADMIN);
        InMemoryUserRepository users = new InMemoryUserRepository(admin)
                .withProjectMembers(PROJECT_ID, admin.getLoginId());
        StatisticsService service = new StatisticsService(new PermissionPolicy(), new FakeStatisticsRepository(), users);

        assertThrows(SecurityException.class,
                () -> service.viewStatistics(PROJECT_ID, null, null, null, null, admin));
    }

    @Test
    @DisplayName("bad date range stops early")
    void badDateRangeStopsEarly() {
        User dev = user("dev1", Role.DEV);
        InMemoryUserRepository users = new InMemoryUserRepository(dev)
                .withProjectMembers(PROJECT_ID, dev.getLoginId());
        FakeStatisticsRepository statistics = new FakeStatisticsRepository();
        StatisticsService service = new StatisticsService(new PermissionPolicy(), statistics, users);

        assertThrows(IllegalArgumentException.class,
                () -> service.viewStatistics(
                        PROJECT_ID,
                        LocalDate.of(2026, 5, 2),
                        LocalDate.of(2026, 5, 1),
                        null,
                        null,
                        dev));
        assertEquals(0, statistics.callCount);
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "hash", role, true, NOW, NOW);
    }

    private static StatisticsReport report() {
        Map<IssueStatus, Integer> statusCounts = new EnumMap<>(IssueStatus.class);
        statusCounts.put(IssueStatus.NEW, 1);
        Map<Priority, Integer> priorityCounts = new EnumMap<>(Priority.class);
        priorityCounts.put(Priority.MAJOR, 1);
        return StatisticsReportTestFactory.create(
                statusCounts,
                priorityCounts,
                List.of(new DailyIssueCount(LocalDate.of(2026, 5, 1), 1)),
                List.of(new MonthlyIssueCount(YearMonth.of(2026, 5), 1)));
    }

    private static final class FakeStatisticsRepository implements StatisticsRepository {

        private long lastProjectId;
        private int callCount;

        @Override
        public StatisticsReport calculateProjectStatistics(
                long projectId,
                LocalDate dailyFromInclusive,
                LocalDate dailyToInclusive,
                YearMonth monthlyFromInclusive,
                YearMonth monthlyToInclusive) {
            lastProjectId = projectId;
            callCount++;
            return report();
        }
    }
}
