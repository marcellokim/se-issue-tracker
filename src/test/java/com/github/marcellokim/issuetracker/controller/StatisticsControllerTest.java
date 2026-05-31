package com.github.marcellokim.issuetracker.controller;

import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.PROJECT_ID;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.anonymousAuth;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.authenticated;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.project;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.report;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.repository.StatisticsReport;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.StatisticsReportResult;
import com.github.marcellokim.issuetracker.service.StatisticsService;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Statistics controller")
class StatisticsControllerTest {

    @Test
    @DisplayName("statistics query keeps the selected range")
    void readsReportForRange() {
        ControllerTestSupport.AuthFixture auth = authenticated(Role.DEV);
        auth.users().attachProjects(new InMemoryProjectRepository(project(PROJECT_ID))
                .withParticipant(PROJECT_ID, auth.user().getLoginId()));
        var statistics = new ControllerTestSupport.FakeStatisticsRepository();
        StatisticsReport expectedReport = report();
        statistics.report = expectedReport;

        StatisticsController controller = new StatisticsController(
                auth.service(),
                new StatisticsService(new PermissionPolicy(), statistics, auth.users()));

        StatisticsReportResult actualReport = controller.viewStatistics(
                PROJECT_ID,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                YearMonth.of(2026, 5),
                YearMonth.of(2026, 6));

        assertEquals(expectedReport.statusCounts(), actualReport.statusCounts());
        assertEquals(expectedReport.priorityCounts(), actualReport.priorityCounts());
        assertEquals(PROJECT_ID, statistics.reportProjectId);
        assertEquals(LocalDate.of(2026, 5, 1), statistics.dailyFrom);
        assertEquals(YearMonth.of(2026, 6), statistics.monthlyTo);

        StatisticsReportResult defaultRangeReport = controller.viewStatistics(PROJECT_ID);
        assertEquals(expectedReport.statusCounts(), defaultRangeReport.statusCounts());
        assertEquals(PROJECT_ID, statistics.reportProjectId);
        assertNull(statistics.dailyFrom);
        assertNull(statistics.monthlyTo);
    }

    @Test
    @DisplayName("statistics rejects missing login and reversed ranges")
    void rejectsBadAccessOrRange() {
        StatisticsController anonymousController = new StatisticsController(
                anonymousAuth(),
                new StatisticsService(new PermissionPolicy(),
                        new ControllerTestSupport.FakeStatisticsRepository(),
                        new ControllerTestSupport.FakeUserRepository()));
        assertThrows(SecurityException.class, () -> anonymousController.viewStatistics(PROJECT_ID));

        ControllerTestSupport.AuthFixture pl = authenticated(Role.PL);
        pl.users().attachProjects(new InMemoryProjectRepository(project(PROJECT_ID))
                .withParticipant(PROJECT_ID, pl.user().getLoginId()));
        StatisticsController controller = new StatisticsController(
                pl.service(),
                new StatisticsService(new PermissionPolicy(),
                        new ControllerTestSupport.FakeStatisticsRepository(),
                        pl.users()));
        assertThrows(
                IllegalArgumentException.class,
                () -> controller.viewStatistics(
                        PROJECT_ID,
                        LocalDate.of(2026, 5, 2),
                        LocalDate.of(2026, 5, 1),
                        null,
                        null));
        assertThrows(
                IllegalArgumentException.class,
                () -> controller.viewStatistics(
                        PROJECT_ID,
                        null,
                        null,
                        YearMonth.of(2026, 6),
                        YearMonth.of(2026, 5)));
    }
}
