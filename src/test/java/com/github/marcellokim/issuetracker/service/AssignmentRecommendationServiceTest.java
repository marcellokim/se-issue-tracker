package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.support.InMemoryAssignmentRecommendationRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Assignment recommendation service")
class AssignmentRecommendationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 19, 12, 0);

    private final User dev1 = user("dev1", Role.DEV);
    private final User tester1 = user("tester1", Role.TESTER);
    private final InMemoryAssignmentRecommendationRepository repository =
            new InMemoryAssignmentRecommendationRepository(dev1, tester1);
    private final AssignmentRecommendationService service =
            new AssignmentRecommendationService(repository, new KNNAssignmentRecommendation());

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

    @Test
    @DisplayName("throws for null issue input")
    void throwsForNullIssue() {
        assertThrows(NullPointerException.class,
                () -> service.recommendAssignmentCandidates(null));
    }

    @Test
    @DisplayName("recommended candidates limited to MAX_CANDIDATES (3)")
    void recommendedCandidatesLimitedToThree() {
        User dev2 = user("dev2", Role.DEV);
        User dev3 = user("dev3", Role.DEV);
        User dev4 = user("dev4", Role.DEV);
        User dev5 = user("dev5", Role.DEV);
        InMemoryAssignmentRecommendationRepository repo =
                new InMemoryAssignmentRecommendationRepository(dev1, dev2, dev3, dev4, dev5, tester1);
        AssignmentRecommendationService svc =
                new AssignmentRecommendationService(repo, new KNNAssignmentRecommendation());

        AssignmentOptionsResult options = svc.recommendAssignmentCandidates(issue(IssueStatus.NEW));

        assertTrue(options.devAssigneeCandidates().size() <= 3);
    }

    @Test
    @DisplayName("empty issue history yields empty KNN recommendations but returns all active users")
    void emptyHistoryYieldsEmptyRecommendationsButAllActive() {
        AssignmentOptionsResult options = service.recommendAssignmentCandidates(issue(IssueStatus.NEW));

        assertTrue(options.devAssigneeCandidates().isEmpty());
        assertTrue(options.testerVerifierCandidates().isEmpty());
        assertFalse(options.allDevAssignees().isEmpty());
        assertFalse(options.allTesterVerifiers().isEmpty());
    }

    @Test
    @DisplayName("inactive user is excluded from all active candidates")
    void inactiveUserExcludedFromAllActive() {
        User inactiveDev = User.fromPersistence("dev_off", "dev_off", "hash", Role.DEV, false, null, null);
        InMemoryAssignmentRecommendationRepository repo =
                new InMemoryAssignmentRecommendationRepository(dev1, inactiveDev, tester1);
        AssignmentRecommendationService svc =
                new AssignmentRecommendationService(repo, new KNNAssignmentRecommendation());

        AssignmentOptionsResult options = svc.recommendAssignmentCandidates(issue(IssueStatus.NEW));

        assertTrue(options.allDevAssignees().stream().noneMatch(c -> c.loginId().equals("dev_off")));
    }

    @Test
    @DisplayName("KNN recommends dev candidate when resolved issue history exists")
    void knnRecommendsDevWhenHistoryExists() {
        InMemoryAssignmentRecommendationRepository repo =
                new InMemoryAssignmentRecommendationRepository(dev1, tester1)
                        .withResolvedIssues(
                                new AssignmentRecommendationRepository.IssueRecommendationData(
                                        "login error", "login page crash", "dev1", "tester1"));
        AssignmentRecommendationService svc =
                new AssignmentRecommendationService(repo, new KNNAssignmentRecommendation());

        Issue target = Issue.fromPersistence(Issue.persistedState(1L, "login error page", "login crash", dev1)
                .id(2L).issueId("ISSUE-2").reportedDate(NOW).priority(Priority.MAJOR)
                .status(IssueStatus.NEW).updatedAt(NOW));
        AssignmentOptionsResult options = svc.recommendAssignmentCandidates(target);

        assertFalse(options.devAssigneeCandidates().isEmpty());
        assertEquals("dev1", options.devAssigneeCandidates().get(0).loginId());
    }

    @Test
    @DisplayName("KNN recommends tester candidate when resolved issue history exists")
    void knnRecommendsTesterWhenHistoryExists() {
        InMemoryAssignmentRecommendationRepository repo =
                new InMemoryAssignmentRecommendationRepository(dev1, tester1)
                        .withResolvedIssues(
                                new AssignmentRecommendationRepository.IssueRecommendationData(
                                        "login error", "login page crash", "dev1", "tester1"));
        AssignmentRecommendationService svc =
                new AssignmentRecommendationService(repo, new KNNAssignmentRecommendation());

        Issue target = Issue.fromPersistence(Issue.persistedState(1L, "login error page", "login crash", dev1)
                .id(2L).issueId("ISSUE-2").reportedDate(NOW).priority(Priority.MAJOR)
                .status(IssueStatus.NEW).updatedAt(NOW));
        AssignmentOptionsResult options = svc.recommendAssignmentCandidates(target);

        assertFalse(options.testerVerifierCandidates().isEmpty());
        assertEquals("tester1", options.testerVerifierCandidates().get(0).loginId());
    }

    @Test
    @DisplayName("KNN filters out inactive user from recommendation results")
    void knnFiltersInactiveFromRecommendations() {
        User inactiveDev = User.fromPersistence("dev_gone", "dev_gone", "hash", Role.DEV, false, null, null);
        InMemoryAssignmentRecommendationRepository repo =
                new InMemoryAssignmentRecommendationRepository(dev1, inactiveDev, tester1)
                        .withResolvedIssues(
                                new AssignmentRecommendationRepository.IssueRecommendationData(
                                        "login error", "crash", "dev_gone", "tester1"));
        AssignmentRecommendationService svc =
                new AssignmentRecommendationService(repo, new KNNAssignmentRecommendation());

        Issue target = Issue.fromPersistence(Issue.persistedState(1L, "login error", "crash", dev1)
                .id(2L).issueId("ISSUE-2").reportedDate(NOW).priority(Priority.MAJOR)
                .status(IssueStatus.NEW).updatedAt(NOW));
        AssignmentOptionsResult options = svc.recommendAssignmentCandidates(target);

        assertTrue(options.devAssigneeCandidates().stream().noneMatch(c -> c.loginId().equals("dev_gone")));
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
