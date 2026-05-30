package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.support.AssignmentContext;
import com.github.marcellokim.issuetracker.support.IssueSearchCriteriaTestFactory;
import com.github.marcellokim.issuetracker.support.StatisticsReportTestFactory;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository.DashboardProjectSnapshot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Domain value objects")
class DomainValueObjectsTest {

        private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 19, 14, 0);
        private final User dev = User.fromPersistence("dev1", "Developer One", "hash", Role.DEV, true, null, null);

        @Test
        @DisplayName("simple query and count records preserve values")
        void simpleRecordsPreserveValues() {
                LocalDate date = LocalDate.of(2026, 5, 19);
                YearMonth month = YearMonth.of(2026, 5);
                IssueSearchCriteria criteria = IssueSearchCriteria.create(
                                1L,
                                IssueStatus.ASSIGNED,
                                Priority.MAJOR,
                                "tester1",
                                "dev1",
                                "tester2",
                                "login",
                                NOW.minusDays(1),
                                NOW,
                                true);

                assertEquals(1L, criteria.projectId());
                assertEquals(IssueStatus.ASSIGNED, criteria.status());
                assertEquals(Priority.MAJOR, criteria.priority());
                assertEquals("tester1", criteria.reporterId());
                assertEquals("dev1", criteria.assigneeId());
                assertEquals("tester2", criteria.verifierId());
                assertEquals("login", criteria.keyword());
                assertEquals(NOW.minusDays(1), criteria.reportedFrom());
                assertEquals(NOW, criteria.reportedTo());
                assertTrue(criteria.includeDeleted());
                DailyIssueCount dailyCount = DailyIssueCount.create(date, 2);
                DailyIssueCount sameDailyCount = DailyIssueCount.create(date, 2);
                MonthlyIssueCount monthlyCount = MonthlyIssueCount.create(month, 5);
                MonthlyIssueCount sameMonthlyCount = MonthlyIssueCount.create(month, 5);
                ProjectMember projectMember = ProjectMember.create(1L, "dev1", NOW);
                ProjectMember sameProjectMember = ProjectMember.create(1L, "dev1", NOW);
                AssignmentContext assignmentContext = AssignmentContext.create(7L, IssueStatus.FIXED, "dev1",
                                "tester1");
                AssignmentContext sameAssignmentContext = AssignmentContext.create(7L, IssueStatus.FIXED, "dev1",
                                "tester1");

                assertEquals(dailyCount, sameDailyCount);
                assertEquals(monthlyCount, sameMonthlyCount);
                assertEquals(projectMember, sameProjectMember);
                assertEquals(assignmentContext, sameAssignmentContext);
        }

        @Test
        @DisplayName("dashboard project snapshot has value semantics and copies status counts")
        void dashboardProjectSnapshotHasValueSemantics() {
                Map<IssueStatus, Integer> statusCounts = new EnumMap<>(IssueStatus.class);
                statusCounts.put(IssueStatus.NEW, 2);
                DashboardProjectSnapshot snapshot = new DashboardProjectSnapshot(
                                1L,
                                "project A",
                                null,
                                3,
                                1,
                                1,
                                1,
                                2,
                                statusCounts);
                DashboardProjectSnapshot sameSnapshot = new DashboardProjectSnapshot(
                                1L,
                                "project A",
                                "",
                                3,
                                1,
                                1,
                                1,
                                2,
                                Map.of(IssueStatus.NEW, 2));
                DashboardProjectSnapshot differentSnapshot = new DashboardProjectSnapshot(
                                2L,
                                "project B",
                                "Demo",
                                4,
                                1,
                                2,
                                1,
                                3,
                                Map.of(IssueStatus.ASSIGNED, 3));

                statusCounts.put(IssueStatus.CLOSED, 99);

                assertEquals("", snapshot.projectDescription());
                assertEquals(Map.of(IssueStatus.NEW, 2), snapshot.statusCounts());
                assertThrows(UnsupportedOperationException.class,
                                () -> snapshot.statusCounts().put(IssueStatus.CLOSED, 1));
                assertValueSemantics(
                                snapshot,
                                sameSnapshot,
                                differentSnapshot,
                                "DashboardProjectSnapshot[projectId=1");
                assertThrows(IllegalArgumentException.class,
                                () -> new DashboardProjectSnapshot(0L, "project", "", 0, 0, 0, 0, 0, Map.of()));
                assertThrows(IllegalArgumentException.class,
                                () -> new DashboardProjectSnapshot(1L, " ", "", 0, 0, 0, 0, 0, Map.of()));
                assertThrows(NullPointerException.class,
                                () -> new DashboardProjectSnapshot(1L, "project", "", 0, 0, 0, 0, 0, null));
        }

        @Test
        @DisplayName("issue search criteria has value semantics")
        void issueSearchCriteriaHasValueSemantics() {
                IssueSearchCriteria criteria = IssueSearchCriteria.create(
                                1L,
                                IssueStatus.ASSIGNED,
                                Priority.MAJOR,
                                "tester1",
                                "dev1",
                                "tester2",
                                "login",
                                NOW.minusDays(1),
                                NOW,
                                true);
                IssueSearchCriteria sameCriteria = IssueSearchCriteria.create(
                                1L,
                                IssueStatus.ASSIGNED,
                                Priority.MAJOR,
                                "tester1",
                                "dev1",
                                "tester2",
                                "login",
                                NOW.minusDays(1),
                                NOW,
                                true);
                IssueSearchCriteria differentCriteria = IssueSearchCriteria.create(
                                2L,
                                IssueStatus.NEW,
                                Priority.MINOR,
                                "tester3",
                                "dev4",
                                "tester5",
                                "logout",
                                NOW.minusDays(2),
                                NOW.plusDays(1),
                                false);

                assertValueSemantics(
                                criteria,
                                sameCriteria,
                                differentCriteria,
                                "IssueSearchCriteria[projectId=1");
                assertTrue(criteria.toString().contains("status=ASSIGNED"));
                assertTrue(criteria.toString().contains("includeDeleted=true"));
        }

        @Test
        @DisplayName("all issue search criteria defaults to active issues")
        void allIssueSearchCriteriaDefaultsToActiveIssues() {
                IssueSearchCriteria criteria = IssueSearchCriteriaTestFactory.all(1L);

                assertEquals(1L, criteria.projectId());
                assertNull(criteria.status());
                assertNull(criteria.priority());
                assertNull(criteria.reporterId());
                assertNull(criteria.assigneeId());
                assertNull(criteria.verifierId());
                assertNull(criteria.keyword());
                assertNull(criteria.reportedFrom());
                assertNull(criteria.reportedTo());
                assertFalse(criteria.includeDeleted());
        }

        @Test
        @DisplayName("statistics report defensively copies collection inputs")
        void statisticsReportCopiesInputs() {
                Map<IssueStatus, Integer> statusCounts = new EnumMap<>(IssueStatus.class);
                statusCounts.put(IssueStatus.NEW, 1);
                Map<Priority, Integer> priorityCounts = new EnumMap<>(Priority.class);
                priorityCounts.put(Priority.MAJOR, 2);
                List<DailyIssueCount> dailyCounts = new ArrayList<>(
                                List.of(DailyIssueCount.create(LocalDate.of(2026, 5, 19), 3)));
                List<MonthlyIssueCount> monthlyCounts = new ArrayList<>(
                                List.of(MonthlyIssueCount.create(YearMonth.of(2026, 5), 4)));
                Map<YearMonth, Map<IssueStatus, Integer>> monthlyStatusCounts = new java.util.LinkedHashMap<>();
                monthlyStatusCounts.put(YearMonth.of(2026, 5), Map.of(IssueStatus.NEW, 1));
                Map<YearMonth, Map<Priority, Integer>> monthlyPriorityCounts = new java.util.LinkedHashMap<>();
                monthlyPriorityCounts.put(YearMonth.of(2026, 5), Map.of(Priority.MAJOR, 2));

                StatisticsReport report = StatisticsReportTestFactory.create(
                                statusCounts,
                                priorityCounts,
                                dailyCounts,
                                monthlyCounts,
                                monthlyStatusCounts,
                                monthlyPriorityCounts);
                statusCounts.put(IssueStatus.CLOSED, 99);
                priorityCounts.put(Priority.CRITICAL, 99);
                dailyCounts.add(DailyIssueCount.create(LocalDate.of(2026, 5, 20), 99));
                monthlyCounts.add(MonthlyIssueCount.create(YearMonth.of(2026, 6), 99));
                monthlyStatusCounts.put(YearMonth.of(2026, 6), Map.of(IssueStatus.CLOSED, 99));
                monthlyPriorityCounts.put(YearMonth.of(2026, 6), Map.of(Priority.CRITICAL, 99));

                assertEquals(Map.of(IssueStatus.NEW, 1), report.statusCounts());
                assertEquals(Map.of(Priority.MAJOR, 2), report.priorityCounts());
                assertEquals(List.of(DailyIssueCount.create(LocalDate.of(2026, 5, 19), 3)), report.dailyCounts());
                assertEquals(List.of(MonthlyIssueCount.create(YearMonth.of(2026, 5), 4)), report.monthlyCounts());
                assertEquals(Map.of(IssueStatus.NEW, 1), report.monthlyStatusCounts().get(YearMonth.of(2026, 5)));
                assertEquals(Map.of(Priority.MAJOR, 2), report.monthlyPriorityCounts().get(YearMonth.of(2026, 5)));
                assertThrows(UnsupportedOperationException.class,
                                () -> report.dailyCounts().add(DailyIssueCount.create(LocalDate.now(), 1)));
                assertThrows(UnsupportedOperationException.class,
                                () -> report.monthlyStatusCounts().put(YearMonth.of(2026, 7), Map.of()));
        }

        @Test
        @DisplayName("statistics report has value semantics")
        void statisticsReportHasValueSemantics() {
                StatisticsReport report = report();
                StatisticsReport sameReport = report();
                StatisticsReport differentReport = StatisticsReportTestFactory.create(
                                Map.of(IssueStatus.CLOSED, 2),
                                Map.of(Priority.CRITICAL, 3),
                                List.of(DailyIssueCount.create(LocalDate.of(2026, 5, 20), 4)),
                                List.of(MonthlyIssueCount.create(YearMonth.of(2026, 6), 5)));

                assertValueSemantics(
                                report,
                                sameReport,
                                differentReport,
                                "StatisticsReport[statusCounts=");
                assertTrue(report.toString().contains("priorityCounts="));
                assertTrue(report.toString().contains("monthlyCounts="));
                assertTrue(report.toString().contains("monthlyStatusCounts="));
                assertThrows(NullPointerException.class,
                                () -> StatisticsReportTestFactory.create(null, Map.of(), List.of(), List.of()));
        }

        @Test
        @DisplayName("assignment context has value semantics")
        void assignmentContextHasValueSemantics() {
                AssignmentContext context = AssignmentContext.create(7L, IssueStatus.FIXED, "dev1", "tester1");
                AssignmentContext sameContext = AssignmentContext.create(7L, IssueStatus.FIXED, "dev1", "tester1");
                AssignmentContext differentContext = AssignmentContext.create(8L, IssueStatus.ASSIGNED, "dev2",
                                "tester2");

                assertEquals(7L, context.issueId());
                assertEquals(IssueStatus.FIXED, context.status());
                assertEquals("dev1", context.assigneeId());
                assertEquals("tester1", context.verifierId());
                assertValueSemantics(
                                context,
                                sameContext,
                                differentContext,
                                "AssignmentContext[issueId=7");
        }

        @Test
        @DisplayName("project member has value semantics")
        void projectMemberHasValueSemantics() {
                ProjectMember member = ProjectMember.create(1L, "dev1", NOW);
                ProjectMember sameMember = ProjectMember.create(1L, "dev1", NOW);
                ProjectMember differentMember = ProjectMember.create(2L, "dev2", NOW.plusDays(1));

                assertEquals(1L, member.projectId());
                assertEquals("dev1", member.userId());
                assertEquals(NOW, member.joinedAt());
                assertValueSemantics(
                                member,
                                sameMember,
                                differentMember,
                                "ProjectMember[projectId=1");
        }

        @Test
        @DisplayName("daily and monthly issue counts have value semantics")
        void issueCountsHaveValueSemantics() {
                DailyIssueCount daily = DailyIssueCount.create(LocalDate.of(2026, 5, 19), 3);
                DailyIssueCount sameDaily = DailyIssueCount.create(LocalDate.of(2026, 5, 19), 3);
                DailyIssueCount differentDaily = DailyIssueCount.create(LocalDate.of(2026, 5, 20), 4);
                MonthlyIssueCount monthly = MonthlyIssueCount.create(YearMonth.of(2026, 5), 5);
                MonthlyIssueCount sameMonthly = MonthlyIssueCount.create(YearMonth.of(2026, 5), 5);
                MonthlyIssueCount differentMonthly = MonthlyIssueCount.create(YearMonth.of(2026, 6), 6);

                assertEquals(LocalDate.of(2026, 5, 19), daily.date());
                assertEquals(3, daily.count());
                assertValueSemantics(daily, sameDaily, differentDaily, "DailyIssueCount[date=2026-05-19");

                assertEquals(YearMonth.of(2026, 5), monthly.month());
                assertEquals(5, monthly.count());
                assertValueSemantics(monthly, sameMonthly, differentMonthly, "MonthlyIssueCount[month=2026-05");
        }

        private static StatisticsReport report() {
                return StatisticsReportTestFactory.create(
                                Map.of(IssueStatus.NEW, 1),
                                Map.of(Priority.MAJOR, 2),
                                List.of(DailyIssueCount.create(LocalDate.of(2026, 5, 19), 3)),
                                List.of(MonthlyIssueCount.create(YearMonth.of(2026, 5), 4)),
                                Map.of(YearMonth.of(2026, 5), Map.of(IssueStatus.NEW, 1)),
                                Map.of(YearMonth.of(2026, 5), Map.of(Priority.MAJOR, 2)));
        }

        private static void assertValueSemantics(
                        Object value,
                        Object equalValue,
                        Object differentValue,
                        String expectedToStringPrefix) {
                Object sameReference = value;

                assertTrue(value.equals(sameReference));
                assertEquals(value, equalValue);
                assertEquals(value.hashCode(), equalValue.hashCode());
                assertNotEquals(value, differentValue);
                assertNotEquals(value, null);
                assertNotEquals(value, "different type");
                assertTrue(value.toString().startsWith(expectedToStringPrefix));
        }
}
