package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.support.InMemoryAssignmentRecommendationRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Assignment recommendation service")
class AssignmentRecommendationServiceTest {

        private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 19, 12, 0);

        private final User dev1 = user("dev1", Role.DEV);
        private final User tester1 = user("tester1", Role.TESTER);
        private final InMemoryAssignmentRecommendationRepository repository = new InMemoryAssignmentRecommendationRepository(
                        dev1, tester1);
        private final AssignmentRecommendationService service = new AssignmentRecommendationService(repository,
                        new KNNAssignmentRecommendation());

        @Test
        @DisplayName("new issue shows dev and tester choices")
        void newIssueShowsChoices() {
                AssignmentOptionsResult options = service.recommendAssignmentCandidates(issue(IssueStatus.NEW));

                assertTrue(options.devAssigneeCandidates().isEmpty());
                assertTrue(options.testerVerifierCandidates().isEmpty());
                assertFalse(options.allDevAssignees().isEmpty());
                assertFalse(options.allTesterVerifiers().isEmpty());
        }

        @Test
        @DisplayName("reopened issue puts old fixer first")
        void oldFixerComesFirst() {
                AssignmentOptionsResult options = service.recommendAssignmentCandidates(issue(IssueStatus.REOPENED));

                assertEquals("dev1", options.devAssigneeCandidates().get(0).loginId());
                assertFalse(options.allTesterVerifiers().isEmpty());
        }

        @Test
        @DisplayName("assigned issue only needs a dev")
        void assignedIssueNeedsDev() {
                AssignmentOptionsResult options = service.recommendAssignmentCandidates(issue(IssueStatus.ASSIGNED));

                assertFalse(options.devAssigneeCandidates().isEmpty());
                assertTrue(options.testerVerifierCandidates().isEmpty());
        }

        @Test
        @DisplayName("fixed issue only needs a tester")
        void fixedIssueNeedsTester() {
                AssignmentOptionsResult options = service.recommendAssignmentCandidates(issue(IssueStatus.FIXED));

                assertTrue(options.devAssigneeCandidates().isEmpty());
                assertFalse(options.testerVerifierCandidates().isEmpty());
        }

        @Test
        @DisplayName("done issues are not assigned again")
        void doneIssuesAreRejected() {
                assertThrows(IllegalStateException.class,
                                () -> service.recommendAssignmentCandidates(issue(IssueStatus.RESOLVED)));
                assertThrows(IllegalStateException.class,
                                () -> service.recommendAssignmentCandidates(issue(IssueStatus.CLOSED)));
                assertThrows(IllegalStateException.class,
                                () -> service.recommendAssignmentCandidates(issue(IssueStatus.DELETED)));
        }

        @Test
        @DisplayName("only three recommendations are shown")
        void keepsThreeRecommendations() {
                User dev2 = user("dev2", Role.DEV);
                User dev3 = user("dev3", Role.DEV);
                User dev4 = user("dev4", Role.DEV);
                User dev5 = user("dev5", Role.DEV);
                InMemoryAssignmentRecommendationRepository repo = new InMemoryAssignmentRecommendationRepository(dev1,
                                dev2, dev3, dev4, dev5, tester1)
                                .withResolvedIssues(
                                                recommendationData("login error", "login page crash", "dev1",
                                                                "tester1"),
                                                recommendationData("login failure", "cannot login", "dev2", "tester1"),
                                                recommendationData("login timeout", "auth server timeout", "dev3",
                                                                "tester1"),
                                                recommendationData("login button", "button does not work", "dev4",
                                                                "tester1"));
                AssignmentRecommendationService svc = new AssignmentRecommendationService(repo,
                                new KNNAssignmentRecommendation());

                AssignmentOptionsResult options = svc.recommendAssignmentCandidates(issue(IssueStatus.NEW));

                assertEquals(3, options.devAssigneeCandidates().size());
        }

        @Test
        @DisplayName("past resolved issues suggest workers")
        void pastIssuesSuggestWorkers() {
                InMemoryAssignmentRecommendationRepository repo = new InMemoryAssignmentRecommendationRepository(dev1,
                                tester1)
                                .withResolvedIssues(
                                                recommendationData("login error", "login page crash", "dev1",
                                                                "tester1"));
                AssignmentRecommendationService svc = new AssignmentRecommendationService(repo,
                                new KNNAssignmentRecommendation());

                AssignmentOptionsResult options = svc.recommendAssignmentCandidates(loginIssue());

                assertEquals("dev1", options.devAssigneeCandidates().get(0).loginId());
                assertEquals("tester1", options.testerVerifierCandidates().get(0).loginId());
        }

        @Test
        @DisplayName("inactive user is left out")
        void leavesOutInactiveUser() {
                User inactiveDev = User.fromPersistence("dev_gone", "dev_gone", "hash", Role.DEV, false, null, null);
                InMemoryAssignmentRecommendationRepository repo = new InMemoryAssignmentRecommendationRepository(dev1,
                                inactiveDev, tester1)
                                .withResolvedIssues(
                                                recommendationData("login error", "crash", "dev_gone", "tester1"));
                AssignmentRecommendationService svc = new AssignmentRecommendationService(repo,
                                new KNNAssignmentRecommendation());

                AssignmentOptionsResult options = svc.recommendAssignmentCandidates(loginIssue());

                assertTrue(options.devAssigneeCandidates().stream().noneMatch(c -> c.loginId().equals("dev_gone")));
        }

        private static Issue loginIssue() {
                return Issue.fromPersistence(
                                Issue.persistedState(1L, "login error page", "login crash", user("dev1", Role.DEV))
                                                .id(2L)
                                                .issueId("ISSUE-2")
                                                .reportedDate(NOW)
                                                .priority(Priority.MAJOR)
                                                .status(IssueStatus.NEW)
                                                .updatedAt(NOW));
        }

        private static Issue issue(IssueStatus status) {
                var state = Issue.persistedState(1L, "Issue", "Issue description", user("tester1", Role.TESTER))
                                .id(1L)
                                .issueId("ISSUE-1")
                                .reportedDate(NOW)
                                .priority(Priority.MAJOR)
                                .status(status)
                                .updatedAt(NOW);

                return Issue.fromPersistence(switch (status) {
                        case ASSIGNED -> state.assignee(user("dev1", Role.DEV)).verifier(user("tester1", Role.TESTER));
                        case FIXED -> state.assignee(user("dev1", Role.DEV))
                                        .verifier(user("tester1", Role.TESTER))
                                        .fixer(user("dev1", Role.DEV));
                        case REOPENED, RESOLVED, CLOSED, DELETED -> state.fixer(user("dev1", Role.DEV))
                                        .resolver(user("tester1", Role.TESTER));
                        case NEW -> state;
                });
        }

        private static AssignmentRecommendationRepository.IssueRecommendationData recommendationData(
                        String title,
                        String description,
                        String fixerLoginId,
                        String resolverLoginId) {
                return new AssignmentRecommendationRepository.IssueRecommendationData(
                                title,
                                description,
                                fixerLoginId,
                                resolverLoginId);
        }

        private static User user(String loginId, Role role) {
                return User.fromPersistence(loginId, loginId, "hash", role, true, null, null);
        }
}
