package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.support.InMemoryAssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Assignment service")
class AssignmentServiceTest {

        private static final long PROJECT_ID = 10L;
        private static final long ISSUE_ID = 1L;
        private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true,
                        createdAt(),
                        createdAt());
        private final User assignee = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, createdAt(),
                        createdAt());
        private final User verifier = User.fromPersistence("tester2", "Tester Two", "hash", Role.TESTER, true,
                        createdAt(),
                        createdAt());
        private final User pl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, createdAt(), createdAt());
        private final User otherProjectPl = User.fromPersistence("pl2", "PL Two", "hash", Role.PL, true, createdAt(),
                        createdAt());
        private final User anotherAssignee = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true,
                        createdAt(),
                        createdAt());
        private final User anotherVerifier = User.fromPersistence("tester3", "Tester Three", "hash", Role.TESTER, true,
                        createdAt(), createdAt());

        @Test
        @DisplayName("PL opens assignment choices")
        void opensAssignmentChoices() {
                var issue = newIssue();
                var service = service(issue);

                var options = service.startAssignment(ISSUE_ID, pl.getLoginId());

                assertEquals(2, options.allDevAssignees().size());
                assertEquals(2, options.allTesterVerifiers().size());
        }

        @Test
        @DisplayName("PL assigns a new issue")
        void assignsNewIssue() {
                var issue = newIssue();
                var service = service(issue);

                var result = service.assignIssue(ISSUE_ID, assignee.getLoginId(), verifier.getLoginId(),
                                pl.getLoginId());

                assertEquals(IssueStatus.ASSIGNED, result.status());
                assertSame(assignee, issue.getAssignee());
                assertSame(verifier, issue.getVerifier());
                assertEquals(ActionType.STATUS_CHANGED, issue.getHistories().getLast().getAction());
        }

        @Test
        @DisplayName("reopened issue gets new owners")
        void assignsReopenedIssue() {
                var issue = reopenedIssue();
                var issueRepository = new InMemoryIssueRepository(issue);
                var service = service(issueRepository);

                var result = service.assignIssue(ISSUE_ID, anotherAssignee.getLoginId(), anotherVerifier.getLoginId(),
                                pl.getLoginId());
                var savedIssue = issueRepository.findById(ISSUE_ID).orElseThrow();

                assertEquals(IssueStatus.ASSIGNED, result.status());
                assertSame(anotherAssignee, savedIssue.getAssignee());
                assertSame(anotherVerifier, savedIssue.getVerifier());
                assertSame(assignee, savedIssue.getFixer());
                assertSame(verifier, savedIssue.getResolver());

                assertEquals(ActionType.STATUS_CHANGED, savedIssue.getHistories().getLast().getAction());
        }

        @Test
        @DisplayName("PL changes assignee")
        void changesAssignee() {
                var issue = assignedIssue();
                var service = service(issue);

                var result = service.reassignIssue(ISSUE_ID, anotherAssignee.getLoginId(), pl.getLoginId());

                assertEquals(IssueStatus.ASSIGNED, result.status());
                assertSame(anotherAssignee, issue.getAssignee());
                assertSame(verifier, issue.getVerifier());
                assertEquals(ActionType.ASSIGNMENT_CHANGED, issue.getHistories().getLast().getAction());
        }

        @Test
        @DisplayName("PL changes verifier")
        void changesVerifier() {
                var issue = fixedIssue();
                var service = service(issue);

                var result = service.changeVerifier(ISSUE_ID, anotherVerifier.getLoginId(), pl.getLoginId());

                assertEquals(IssueStatus.FIXED, result.status());
                assertSame(anotherVerifier, issue.getVerifier());
                assertSame(assignee, issue.getFixer());
                assertEquals(ActionType.ASSIGNMENT_CHANGED, issue.getHistories().getLast().getAction());
        }

        @Test
        @DisplayName("dev is blocked from assignment")
        void devIsBlocked() {
                var service = service(newIssue());

                assertThrows(SecurityException.class,
                                () -> service.assignIssue(ISSUE_ID, assignee.getLoginId(), verifier.getLoginId(),
                                                assignee.getLoginId()));
        }

        @Test
        @DisplayName("other project PL is blocked")
        void otherProjectPlIsBlocked() {
                var users = new InMemoryUserRepository(
                                reporter,
                                assignee,
                                verifier,
                                pl,
                                otherProjectPl,
                                anotherAssignee,
                                anotherVerifier)
                                .withProjectMembers(PROJECT_ID, pl.getLoginId());
                var service = service(new InMemoryIssueRepository(newIssue()), users);

                assertThrows(SecurityException.class,
                                () -> service.assignIssue(ISSUE_ID, assignee.getLoginId(), verifier.getLoginId(),
                                                otherProjectPl.getLoginId()));
        }

        @Test
        @DisplayName("non-member dev is rejected")
        void rejectsNonMemberDev() {
                var users = new InMemoryUserRepository(
                                reporter,
                                assignee,
                                verifier,
                                pl,
                                anotherAssignee,
                                anotherVerifier)
                                .withProjectMembers(PROJECT_ID, pl.getLoginId(), verifier.getLoginId());
                var service = service(new InMemoryIssueRepository(newIssue()), users);

                assertThrows(SecurityException.class,
                                () -> service.assignIssue(ISSUE_ID, assignee.getLoginId(), verifier.getLoginId(),
                                                pl.getLoginId()));
        }

        @Test
        @DisplayName("non-member tester is rejected")
        void rejectsNonMemberTester() {
                var users = new InMemoryUserRepository(
                                reporter,
                                assignee,
                                verifier,
                                pl,
                                anotherAssignee,
                                anotherVerifier)
                                .withProjectMembers(PROJECT_ID, pl.getLoginId(), assignee.getLoginId());
                var service = service(new InMemoryIssueRepository(newIssue()), users);

                assertThrows(SecurityException.class,
                                () -> service.assignIssue(ISSUE_ID, assignee.getLoginId(), verifier.getLoginId(),
                                                pl.getLoginId()));
        }

        @Test
        @DisplayName("outside dev cannot replace assignee")
        void rejectsOutsideReassign() {
                var users = new InMemoryUserRepository(
                                reporter,
                                assignee,
                                verifier,
                                pl,
                                anotherAssignee,
                                anotherVerifier)
                                .withProjectMembers(PROJECT_ID, pl.getLoginId(), assignee.getLoginId(),
                                                verifier.getLoginId());
                var service = service(new InMemoryIssueRepository(assignedIssue()), users);

                assertThrows(SecurityException.class,
                                () -> service.reassignIssue(ISSUE_ID, anotherAssignee.getLoginId(), pl.getLoginId()));
        }

        @Test
        @DisplayName("outside tester cannot replace verifier")
        void rejectsOutsideVerifierChange() {
                var users = new InMemoryUserRepository(
                                reporter,
                                assignee,
                                verifier,
                                pl,
                                anotherAssignee,
                                anotherVerifier)
                                .withProjectMembers(PROJECT_ID, pl.getLoginId(), assignee.getLoginId(),
                                                verifier.getLoginId());
                var service = service(new InMemoryIssueRepository(fixedIssue()), users);

                assertThrows(SecurityException.class,
                                () -> service.changeVerifier(ISSUE_ID, anotherVerifier.getLoginId(), pl.getLoginId()));
        }

        private AssignmentService service(Issue issue) {
                return service(new InMemoryIssueRepository(issue));
        }

        private AssignmentService service(InMemoryIssueRepository issueRepository) {
                return service(issueRepository, projectMemberUsers());
        }

        private AssignmentService service(InMemoryIssueRepository issueRepository,
                        InMemoryUserRepository userRepository) {
                var members = userRepository.findAll().stream()
                                .filter(user -> userRepository.existsActiveProjectMember(PROJECT_ID, user.getLoginId()))
                                .filter(user -> user.getRole() == Role.DEV || user.getRole() == Role.TESTER)
                                .toArray(User[]::new);
                return new AssignmentService(
                                issueRepository,
                                userRepository,
                                new PermissionPolicy(),
                                new AssignmentRecommendationService(
                                                new InMemoryAssignmentRecommendationRepository(members),
                                                new KNNAssignmentRecommendation()),
                                java.time.LocalDateTime::now);
        }

        private InMemoryUserRepository projectMemberUsers() {
                return new InMemoryUserRepository(
                                reporter,
                                assignee,
                                verifier,
                                pl,
                                otherProjectPl,
                                anotherAssignee,
                                anotherVerifier)
                                .withProjectMembers(PROJECT_ID,
                                                pl.getLoginId(),
                                                assignee.getLoginId(),
                                                verifier.getLoginId(),
                                                anotherAssignee.getLoginId(),
                                                anotherVerifier.getLoginId());
        }

        private Issue newIssue() {
                return issue(IssueStatus.NEW);
        }

        private Issue assignedIssue() {
                var issue = newIssue();
                issue.assignFromNew(assignee, verifier, pl, createdAt().plusMinutes(10));
                return issue;
        }

        private Issue fixedIssue() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt().plusMinutes(20));
                return issue;
        }

        private Issue reopenedIssue() {
                var issue = fixedIssue();
                issue.resolve(verifier, "Verified", createdAt().plusMinutes(30));
                issue.reopen(pl, "Needs more work", createdAt().plusMinutes(40));
                return issue;
        }

        private Issue issue(IssueStatus status) {
                return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", reporter)
                                .id(ISSUE_ID)
                                .issueId("ISSUE-1")
                                .reportedDate(createdAt())
                                .priority(Priority.MAJOR)
                                .status(status)
                                .updatedAt(createdAt()));
        }

        private static LocalDateTime createdAt() {
                return LocalDateTime.of(2026, 5, 18, 10, 0);
        }
}
