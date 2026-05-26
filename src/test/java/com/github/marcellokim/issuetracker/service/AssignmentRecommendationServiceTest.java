package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.AssignmentCandidate;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Assignment recommendation service")
class AssignmentRecommendationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 19, 12, 0);

    private final AssignmentRecommendationService service =
            new AssignmentRecommendationService(new FakeAssignmentRecommendationRepository());

    @Test
    @DisplayName("returns both DEV and TESTER candidates for NEW and REOPENED issues")
    void recommendsBothCandidateTypesForNewAndReopenedIssues() {
        assertBothCandidateTypes(issue(IssueStatus.NEW));
        assertBothCandidateTypes(issue(IssueStatus.REOPENED));
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
    @DisplayName("returns no candidates for terminal or deleted issues")
    void recommendsNoCandidatesForTerminalOrDeletedIssues() {
        assertNoCandidates(issue(IssueStatus.RESOLVED));
        assertNoCandidates(issue(IssueStatus.CLOSED));
        assertNoCandidates(issue(IssueStatus.DELETED));
    }

    private void assertBothCandidateTypes(Issue issue) {
        AssignmentOptionsResult options = service.recommendAssignmentCandidates(issue);

        assertFalse(options.devAssigneeCandidates().isEmpty());
        assertFalse(options.testerVerifierCandidates().isEmpty());
    }

    private void assertNoCandidates(Issue issue) {
        AssignmentOptionsResult options = service.recommendAssignmentCandidates(issue);

        assertTrue(options.devAssigneeCandidates().isEmpty());
        assertTrue(options.testerVerifierCandidates().isEmpty());
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

    private static final class FakeAssignmentRecommendationRepository implements AssignmentRecommendationRepository {

        @Override
        public List<AssignmentCandidate> findDevAssigneeCandidates(long projectId) {
            return List.of(AssignmentCandidate.create(user("dev1", Role.DEV), 2));
        }

        @Override
        public List<AssignmentCandidate> findTesterVerifierCandidates(long projectId) {
            return List.of(AssignmentCandidate.create(user("tester1", Role.TESTER), 3));
        }
    }
}
