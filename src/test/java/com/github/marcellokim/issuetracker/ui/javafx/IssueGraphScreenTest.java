package com.github.marcellokim.issuetracker.ui.javafx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JavaFX issue graph screen")
class IssueGraphScreenTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 1, 12, 0);

    @Test
    @DisplayName("uses only dependencies whose endpoints are visible")
    void graphDependenciesUseVisibleEndpointIssues() {
        List<IssueSummary> visibleIssues = List.of(issue(1L), issue(2L), issue(4L));
        List<DependencyResult> projectDependencies = List.of(
                dependency(1L, 1L, 2L),
                dependency(2L, 2L, 3L),
                dependency(3L, 5L, 1L));

        List<DependencyResult> visibleDependencies = IssueGraphScreen.visibleDependencies(
                visibleIssues,
                projectDependencies);
        List<IssueSummary> graphIssues = IssueGraphScreen.dependencyIssues(visibleIssues, visibleDependencies);

        assertEquals(List.of("DEP-1-2"), visibleDependencies.stream()
                .map(DependencyResult::dependencyId)
                .toList());
        assertEquals(List.of(1L, 2L), graphIssues.stream()
                .map(IssueSummary::id)
                .toList());
    }

    private static IssueSummary issue(long id) {
        return new IssueSummary(
                id,
                "ISSUE-" + id,
                1L,
                IssueStatus.NEW,
                Priority.MAJOR,
                "Issue " + id,
                "reporter",
                null,
                null,
                NOW,
                NOW);
    }

    private static DependencyResult dependency(long id, long blockingIssueId, long blockedIssueId) {
        return new DependencyResult(
                id,
                "DEP-" + blockingIssueId + "-" + blockedIssueId,
                blockingIssueId,
                "ISSUE-" + blockingIssueId,
                blockedIssueId,
                "ISSUE-" + blockedIssueId,
                NOW);
    }
}
