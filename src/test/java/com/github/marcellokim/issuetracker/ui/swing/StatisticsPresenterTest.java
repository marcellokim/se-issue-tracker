package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.StatisticsController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.DailyIssueCount;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.StatisticsReportResult;
import com.github.marcellokim.issuetracker.service.StatisticsService;
import com.github.marcellokim.issuetracker.support.FakeStatisticsRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.support.StatisticsReportTestFactory;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing statistics presenter")
class StatisticsPresenterTest {

    private static final long PROJECT_ID = 7L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("loads default and filtered statistics through the controller")
    void loadsDefaultAndFilteredStatistics() {
        User dev = user("dev1", Role.DEV);
        FakeStatisticsRepository statistics = new FakeStatisticsRepository(report());
        StatisticsPresenter presenter = new StatisticsPresenter(controller(statistics, dev), new RecordingView());

        presenter.loadStatistics(PROJECT_ID);

        assertEquals(PROJECT_ID, statistics.lastProjectId());
        assertNull(statistics.lastDailyFromInclusive());

        presenter.loadStatistics(PROJECT_ID, new StatisticsRangeRequest(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                YearMonth.of(2026, 5),
                YearMonth.of(2026, 6)));

        assertEquals(LocalDate.of(2026, 5, 1), statistics.lastDailyFromInclusive());
        assertEquals(LocalDate.of(2026, 5, 31), statistics.lastDailyToInclusive());
        assertEquals(YearMonth.of(2026, 5), statistics.lastMonthlyFromInclusive());
        assertEquals(YearMonth.of(2026, 6), statistics.lastMonthlyToInclusive());
    }

    @Test
    @DisplayName("keeps statistics failures in the view message")
    void keepsStatisticsFailuresInViewMessage() {
        User dev = user("dev1", Role.DEV);
        FakeStatisticsRepository statistics = new FakeStatisticsRepository(report());
        statistics.failWith(new IllegalArgumentException("dailyFromInclusive must be <= dailyToInclusive"));
        RecordingView view = new RecordingView();
        StatisticsPresenter presenter = new StatisticsPresenter(controller(statistics, dev), view);

        presenter.loadStatistics(PROJECT_ID);

        assertEquals("dailyFromInclusive must be <= dailyToInclusive", view.message);
        assertEquals(true, view.error);
        assertNull(view.report);
    }

    private static StatisticsController controller(FakeStatisticsRepository statistics, User actor) {
        InMemoryUserRepository users = new InMemoryUserRepository(actor)
                .withProjectMembers(PROJECT_ID, actor.getLoginId());
        AuthenticationService authentication = new AuthenticationService(users, new PlainPasswordHashing(), new SessionStore());
        new AuthenticationController(authentication).login(actor.getLoginId(), "password");
        return new StatisticsController(
                authentication,
                new StatisticsService(new PermissionPolicy(), statistics, users));
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "password", role, true, NOW, NOW);
    }

    private static com.github.marcellokim.issuetracker.repository.StatisticsReport report() {
        Map<IssueStatus, Integer> statusCounts = new EnumMap<>(IssueStatus.class);
        statusCounts.put(IssueStatus.NEW, 2);
        statusCounts.put(IssueStatus.CLOSED, 1);
        Map<Priority, Integer> priorityCounts = new EnumMap<>(Priority.class);
        priorityCounts.put(Priority.CRITICAL, 1);
        priorityCounts.put(Priority.MAJOR, 2);
        return StatisticsReportTestFactory.create(
                statusCounts,
                priorityCounts,
                List.of(new DailyIssueCount(LocalDate.of(2026, 5, 31), 3)),
                List.of(new MonthlyIssueCount(YearMonth.of(2026, 5), 3)));
    }

    private static final class RecordingView implements StatisticsView {

        private StatisticsReportResult report;
        private String message;
        private boolean error;

        @Override
        public void showReport(StatisticsReportResult report) {
            this.report = report;
        }

        @Override
        public void showMessage(String message, boolean error) {
            this.message = message;
            this.error = error;
        }
    }

    private static final class PlainPasswordHashing implements PasswordHashing {

        @Override
        public String hash(String password) {
            return password;
        }

        @Override
        public boolean matches(String password, String storedCredential) {
            return storedCredential.equals(password);
        }

        @Override
        public boolean isHashed(String storedCredential) {
            return true;
        }

        @Override
        public String saltOf(String storedCredential) {
            return "salt";
        }

        @Override
        public String hashOf(String storedCredential) {
            return storedCredential;
        }
    }
}
