package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
    private final User dev = User.create("dev1", "Developer One", "hash", Role.DEV, true, null, null);

    @Test
    @DisplayName("assignment candidate builds default reasons")
    void assignmentCandidateBuildsDefaultReasons() {
        AssignmentCandidate experienced = AssignmentCandidate.create(dev, 3);
        AssignmentCandidate fallback = AssignmentCandidate.create(dev, 0);
        AssignmentCandidate explicit = AssignmentCandidate.create(dev, 1, "manual reason");

        assertEquals(dev, experienced.user());
        assertEquals(3, experienced.completedIssueCount());
        assertEquals("Resolved/closed issue history count: 3", experienced.reason());
        assertEquals("Fallback active project member with no resolved/closed history yet", fallback.reason());
        assertEquals("manual reason", explicit.reason());
    }

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
        assertTrue(criteria.includeDeleted());
        assertEquals(DailyIssueCount.create(date, 2), DailyIssueCount.create(date, 2));
        assertEquals(MonthlyIssueCount.create(month, 5), MonthlyIssueCount.create(month, 5));
        assertEquals(ProjectMember.create(1L, "dev1", NOW), ProjectMember.create(1L, "dev1", NOW));
        assertEquals(AssignmentContext.create(7L, IssueStatus.FIXED, "dev1", "tester1"),
                AssignmentContext.create(7L, IssueStatus.FIXED, "dev1", "tester1"));
    }

    @Test
    @DisplayName("all issue search criteria defaults to active issues")
    void allIssueSearchCriteriaDefaultsToActiveIssues() {
        IssueSearchCriteria criteria = IssueSearchCriteria.all();

        assertNull(criteria.projectId());
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

        StatisticsReport report = StatisticsReport.create(statusCounts, priorityCounts, dailyCounts, monthlyCounts);
        statusCounts.put(IssueStatus.CLOSED, 99);
        priorityCounts.put(Priority.CRITICAL, 99);
        dailyCounts.add(DailyIssueCount.create(LocalDate.of(2026, 5, 20), 99));
        monthlyCounts.add(MonthlyIssueCount.create(YearMonth.of(2026, 6), 99));

        assertEquals(Map.of(IssueStatus.NEW, 1), report.statusCounts());
        assertEquals(Map.of(Priority.MAJOR, 2), report.priorityCounts());
        assertEquals(List.of(DailyIssueCount.create(LocalDate.of(2026, 5, 19), 3)), report.dailyCounts());
        assertEquals(List.of(MonthlyIssueCount.create(YearMonth.of(2026, 5), 4)), report.monthlyCounts());
        assertThrows(UnsupportedOperationException.class,
                () -> report.dailyCounts().add(DailyIssueCount.create(LocalDate.now(), 1)));
    }

    @Test
    @DisplayName("validation result factories preserve status and messages")
    void validationResultFactoriesPreserveStatusAndMessages() {
        ValidationResult ok = ValidationResult.ok();
        ValidationResult failure = ValidationResult.failure("blocked by dependency");

        assertTrue(ok.valid());
        assertEquals("", ok.message());
        assertFalse(failure.valid());
        assertEquals("blocked by dependency", failure.message());
    }
}
