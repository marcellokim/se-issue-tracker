package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Assignment recommendation service")
class AssignmentRecommendationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 19, 12, 0);

    private final User dev1 = user("dev1", Role.DEV);
    private final User tester1 = user("tester1", Role.TESTER);
    private final InMemoryIssueRepository issueRepository = new InMemoryIssueRepository();
    private final InMemoryUserRepository userRepository = new InMemoryUserRepository(dev1, tester1);
    private final AssignmentRecommendationService service =
            new AssignmentRecommendationService(issueRepository, userRepository, new KNNAssignmentRecommendation());

    @Test
    @DisplayName("returns all active DEV and TESTER for NEW issue")
    void returnsAllActiveCandidatesForNewIssue() {
        AssignmentOptionsResult options = service.recommendAssignmentCandidates(issue(IssueStatus.NEW));

        assertFalse(options.allDevAssignees().isEmpty());
        assertFalse(options.allTesterVerifiers().isEmpty());
    }

    @Test
    @DisplayName("returns fixer as priority DEV candidate for REOPENED issue")
    void returnsFixerAsPriorityCandidateForReopenedIssue() {
        AssignmentOptionsResult options = service.recommendAssignmentCandidates(issue(IssueStatus.REOPENED));

        assertFalse(options.devAssigneeCandidates().isEmpty());
        assertFalse(options.allDevAssignees().isEmpty());
        assertFalse(options.allTesterVerifiers().isEmpty());
    }

    @Test
    @DisplayName("returns only DEV candidates for ASSIGNED issues")
    void recommendsOnlyDevCandidatesForAssignedIssues() {
        AssignmentOptionsResult options = service.recommendAssignmentCandidates(issue(IssueStatus.ASSIGNED));

        assertFalse(options.devAssigneeCandidates().isEmpty());
        assertTrue(options.testerVerifierCandidates().isEmpty());
    }

    @Test
    @DisplayName("returns only TESTER candidates for FIXED issues")
    void recommendsOnlyTesterCandidatesForFixedIssues() {
        AssignmentOptionsResult options = service.recommendAssignmentCandidates(issue(IssueStatus.FIXED));

        assertTrue(options.devAssigneeCandidates().isEmpty());
        assertFalse(options.testerVerifierCandidates().isEmpty());
    }

    @Test
    @DisplayName("throws for terminal or deleted issues")
    void throwsForTerminalOrDeletedIssues() {
        assertThrows(IllegalStateException.class,
                () -> service.recommendAssignmentCandidates(issue(IssueStatus.RESOLVED)));
        assertThrows(IllegalStateException.class,
                () -> service.recommendAssignmentCandidates(issue(IssueStatus.CLOSED)));
        assertThrows(IllegalStateException.class,
                () -> service.recommendAssignmentCandidates(issue(IssueStatus.DELETED)));
    }

    private static Issue issue(IssueStatus status) {
        return Issue.fromPersistence(Issue.persistedState(1L, "Issue", "Issue description", user("dev1", Role.DEV))
                .id(1L)
                .issueId("ISSUE-1")
                .reportedDate(NOW)
                .priority(Priority.MAJOR)
                .status(status)
                .assignee(user("dev1", Role.DEV))
                .verifier(user("tester1", Role.TESTER))
                .fixer(user("dev1", Role.DEV))
                .resolver(user("tester1", Role.TESTER))
                .updatedAt(NOW));
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "hash", role, true, null, null);
    }
}
