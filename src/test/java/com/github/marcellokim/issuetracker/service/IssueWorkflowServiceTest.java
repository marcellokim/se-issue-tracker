package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue assignment and status workflow")
class IssueWorkflowServiceTest {

    private static final long PROJECT_ID = 10L;
    private static final long ISSUE_ID = 1L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 5, 18, 10, 0);
    private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, CREATED_AT,
            CREATED_AT);
    private final User assignee = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, CREATED_AT, CREATED_AT);
    private final User verifier = User.fromPersistence("tester2", "Tester Two", "hash", Role.TESTER, true, CREATED_AT,
            CREATED_AT);
    private final User pl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, CREATED_AT, CREATED_AT);
    private final User otherDev = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true, CREATED_AT,
            CREATED_AT);

    @Test
    @DisplayName("main demo workflow completes from assignment to close")
    void completeMainDemoWorkflow() {
        var issue = Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(CREATED_AT)
                .priority(Priority.MAJOR)
                .status(IssueStatus.NEW)
                .updatedAt(CREATED_AT));
        var issueRepository = new InMemoryIssueRepository(issue);
        var userRepository = new InMemoryUserRepository(reporter, assignee, verifier, pl);
        var policy = new PermissionPolicy();
        var assignmentService = new AssignmentService(
                issueRepository,
                userRepository,
                policy,
                new AssignmentRecommendationService(issueRepository, userRepository, new KNNAssignmentRecommendation()),
                new Clock()
        );
        var stateService = new IssueStateService(issueRepository, new FakeIssueDependencyRepository(), userRepository, policy, new Clock());

        assignmentService.assignIssue(ISSUE_ID, assignee.getLoginId(), verifier.getLoginId(), pl.getLoginId());
        stateService.changeStatus(ISSUE_ID, IssueStatus.FIXED, "Fix completed", assignee.getLoginId());
        stateService.changeStatus(ISSUE_ID, IssueStatus.RESOLVED, "Verified", verifier.getLoginId());
        stateService.changeStatus(ISSUE_ID, IssueStatus.CLOSED, "Release completed", pl.getLoginId());

        var completedIssue = issueRepository.findById(ISSUE_ID).orElseThrow();
        assertEquals(IssueStatus.CLOSED, completedIssue.getStatus());
        assertNull(completedIssue.getAssignee());
        assertNull(completedIssue.getVerifier());
        assertSame(assignee, completedIssue.getFixer());
        assertSame(verifier, completedIssue.getResolver());
        assertEquals(3, completedIssue.getComments().size());
        assertEquals(4, completedIssue.getHistories().stream()
                .filter(history -> history.getAction() == ActionType.STATUS_CHANGED)
                .count());
        assertEquals(1, completedIssue.getHistories().stream()
                .filter(history -> history.getAction() == ActionType.ASSIGNMENT_CHANGED)
                .count());
        assertEquals(3, completedIssue.getHistories().stream()
                .filter(history -> history.getAction() == ActionType.COMMENTED)
                .count());
    }

    @Test
    @DisplayName("workflow actions are disabled for non-project members")
    void workflowActionsDisableMutationsForNonProjectMember() {
        var issue = Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(CREATED_AT)
                .priority(Priority.MAJOR)
                .status(IssueStatus.ASSIGNED)
                .assignee(otherDev)
                .verifier(verifier)
                .updatedAt(CREATED_AT));
        var service = workflowService(
                new InMemoryIssueRepository(issue),
                new InMemoryUserRepository(reporter, assignee, verifier, pl, otherDev)
                        .withProjectMembers(PROJECT_ID, reporter.getLoginId(), verifier.getLoginId(), pl.getLoginId()));

        IssueWorkflowActions actions = service.viewAvailableActions(ISSUE_ID, otherDev.getLoginId());

        assertFalse(actions.canMarkFixed());
        assertFalse(actions.canAddComment());
    }

    @Test
    @DisplayName("comment action flags are disabled for writers outside the issue project")
    void commentActionsDisableForWriterOutsideProject() {
        var issue = Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(CREATED_AT)
                .priority(Priority.MAJOR)
                .status(IssueStatus.NEW)
                .updatedAt(CREATED_AT));
        var comments = new FakeCommentRepository(Comment.fromPersistence(
                100L,
                ISSUE_ID,
                otherDev.getLoginId(),
                "Investigation note",
                CommentPurpose.GENERAL,
                CREATED_AT,
                CREATED_AT));
        var service = workflowService(
                new InMemoryIssueRepository(issue),
                comments,
                new InMemoryUserRepository(reporter, assignee, verifier, pl, otherDev)
                        .withProjectMembers(PROJECT_ID, reporter.getLoginId(), verifier.getLoginId(), pl.getLoginId()));

        assertFalse(service.canUpdateComment(ISSUE_ID, 100L, otherDev.getLoginId()));
        assertFalse(service.canDeleteComment(ISSUE_ID, 100L, otherDev.getLoginId()));
    }

    @Test
    @DisplayName("workflow actions stay enabled for assigned project members")
    void workflowActionsEnableForAssignedProjectMember() {
        var issue = Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(CREATED_AT)
                .priority(Priority.MAJOR)
                .status(IssueStatus.ASSIGNED)
                .assignee(assignee)
                .verifier(verifier)
                .updatedAt(CREATED_AT));
        var service = workflowService(
                new InMemoryIssueRepository(issue),
                new InMemoryUserRepository(reporter, assignee, verifier, pl)
                        .withProjectMembers(PROJECT_ID, reporter.getLoginId(), assignee.getLoginId(),
                                verifier.getLoginId(), pl.getLoginId()));

        IssueWorkflowActions actions = service.viewAvailableActions(ISSUE_ID, assignee.getLoginId());

        assertTrue(actions.canMarkFixed());
        assertTrue(actions.canAddComment());
    }

    @Test
    @DisplayName("PL can start assignment, assign, manage dependency on NEW issue")
    void plActionsOnNewIssue() {
        var issue = Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "New issue", "desc", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(CREATED_AT)
                .priority(Priority.MAJOR)
                .status(IssueStatus.NEW)
                .updatedAt(CREATED_AT));
        var service = workflowService(
                new InMemoryIssueRepository(issue),
                new InMemoryUserRepository(reporter, assignee, verifier, pl)
                        .withProjectMembers(PROJECT_ID, reporter.getLoginId(), assignee.getLoginId(),
                                verifier.getLoginId(), pl.getLoginId()));

        IssueWorkflowActions actions = service.viewAvailableActions(ISSUE_ID, pl.getLoginId());

        assertTrue(actions.canStartAssignment());
        assertTrue(actions.canAssign());
        assertTrue(actions.canAddDependency());
        assertTrue(actions.canRemoveDependency());
        assertTrue(actions.canChangePriority());
        assertTrue(actions.canSoftDelete());
        assertFalse(actions.canReassign());
        assertFalse(actions.canClose());
        assertFalse(actions.canReopen());
    }

    @Test
    @DisplayName("PL can close resolved issue and reopen closed issue")
    void plCloseAndReopenActions() {
        var resolvedIssue = Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Resolved", "desc", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(CREATED_AT)
                .priority(Priority.MAJOR)
                .status(IssueStatus.RESOLVED)
                .assignee(assignee)
                .verifier(verifier)
                .fixer(assignee)
                .resolver(verifier)
                .updatedAt(CREATED_AT));
        var service = workflowService(
                new InMemoryIssueRepository(resolvedIssue),
                new InMemoryUserRepository(reporter, assignee, verifier, pl)
                        .withProjectMembers(PROJECT_ID, reporter.getLoginId(), assignee.getLoginId(),
                                verifier.getLoginId(), pl.getLoginId()));

        IssueWorkflowActions resolvedActions = service.viewAvailableActions(ISSUE_ID, pl.getLoginId());

        assertTrue(resolvedActions.canClose());
        assertTrue(resolvedActions.canReopen());
    }

    @Test
    @DisplayName("verifier can resolve and reject fix on FIXED issue")
    void verifierActionsOnFixedIssue() {
        var issue = Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Fixed issue", "desc", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(CREATED_AT)
                .priority(Priority.MAJOR)
                .status(IssueStatus.FIXED)
                .assignee(assignee)
                .verifier(verifier)
                .fixer(assignee)
                .updatedAt(CREATED_AT));
        var service = workflowService(
                new InMemoryIssueRepository(issue),
                new InMemoryUserRepository(reporter, assignee, verifier, pl)
                        .withProjectMembers(PROJECT_ID, reporter.getLoginId(), assignee.getLoginId(),
                                verifier.getLoginId(), pl.getLoginId()));

        IssueWorkflowActions actions = service.viewAvailableActions(ISSUE_ID, verifier.getLoginId());

        assertTrue(actions.canResolve());
        assertTrue(actions.canRejectFix());
    }

    @Test
    @DisplayName("comment update and delete allowed for writer who is project member")
    void commentActionsEnabledForProjectMemberWriter() {
        var issue = Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "desc", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(CREATED_AT)
                .priority(Priority.MAJOR)
                .status(IssueStatus.NEW)
                .updatedAt(CREATED_AT));
        var comments = new FakeCommentRepository(Comment.fromPersistence(
                100L,
                ISSUE_ID,
                reporter.getLoginId(),
                "My note",
                CommentPurpose.GENERAL,
                CREATED_AT,
                CREATED_AT));
        var service = workflowService(
                new InMemoryIssueRepository(issue),
                comments,
                new InMemoryUserRepository(reporter, assignee, verifier, pl)
                        .withProjectMembers(PROJECT_ID, reporter.getLoginId(), verifier.getLoginId(), pl.getLoginId()));

        assertTrue(service.canUpdateComment(ISSUE_ID, 100L, reporter.getLoginId()));
        assertTrue(service.canDeleteComment(ISSUE_ID, 100L, reporter.getLoginId()));
    }

    @Test
    @DisplayName("status-change comments are read-only in workflow actions")
    void statusChangeCommentActionsAreReadOnly() {
        var issue = Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "desc", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(CREATED_AT)
                .priority(Priority.MAJOR)
                .status(IssueStatus.NEW)
                .updatedAt(CREATED_AT));
        var comments = new FakeCommentRepository(Comment.fromPersistence(
                100L,
                ISSUE_ID,
                reporter.getLoginId(),
                "Status transition reason",
                CommentPurpose.STATUS_CHANGE,
                CREATED_AT,
                CREATED_AT));
        var service = workflowService(
                new InMemoryIssueRepository(issue),
                comments,
                new InMemoryUserRepository(reporter, assignee, verifier, pl)
                        .withProjectMembers(PROJECT_ID, reporter.getLoginId(), verifier.getLoginId(), pl.getLoginId()));

        assertFalse(service.canUpdateComment(ISSUE_ID, 100L, reporter.getLoginId()));
        assertFalse(service.canDeleteComment(ISSUE_ID, 100L, reporter.getLoginId()));
    }

    @Test
    @DisplayName("mark fixed action is disabled after issue is already fixed")
    void markFixedActionDisablesAfterIssueIsFixed() {
        var issue = Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(CREATED_AT)
                .priority(Priority.MAJOR)
                .status(IssueStatus.FIXED)
                .assignee(assignee)
                .verifier(verifier)
                .fixer(assignee)
                .updatedAt(CREATED_AT));
        var service = workflowService(
                new InMemoryIssueRepository(issue),
                new InMemoryUserRepository(reporter, assignee, verifier, pl)
                        .withProjectMembers(PROJECT_ID, reporter.getLoginId(), assignee.getLoginId(),
                                verifier.getLoginId(), pl.getLoginId()));

        IssueWorkflowActions actions = service.viewAvailableActions(ISSUE_ID, assignee.getLoginId());

        assertFalse(actions.canMarkFixed());
        assertFalse(actions.canResolve());
    }

    private IssueWorkflowService workflowService(
            InMemoryIssueRepository issueRepository,
            InMemoryUserRepository userRepository
    ) {
        return workflowService(issueRepository, new FakeCommentRepository(), userRepository);
    }

    private IssueWorkflowService workflowService(
            InMemoryIssueRepository issueRepository,
            FakeCommentRepository commentRepository,
            InMemoryUserRepository userRepository
    ) {
        return new IssueWorkflowService(
                issueRepository,
                new FakeIssueDependencyRepository(),
                commentRepository,
                userRepository,
                new PermissionPolicy());
    }

    private static final class FakeCommentRepository implements CommentRepository {

        private final Map<Long, Comment> comments = new LinkedHashMap<>();

        private FakeCommentRepository(Comment... comments) {
            for (Comment comment : comments) {
                this.comments.put(comment.id(), comment);
            }
        }

        @Override
        public Optional<Comment> findById(long commentId) {
            return Optional.ofNullable(comments.get(commentId));
        }

        @Override
        public List<Comment> findByIssueId(long issueId) {
            return comments.values().stream()
                    .filter(comment -> comment.issueId() == issueId)
                    .toList();
        }

        @Override
        public Comment save(Comment comment) {
            comments.put(comment.id(), comment);
            return comment;
        }

        @Override
        public Comment saveAndRecordIssueChange(Comment comment, IssueHistory history) {
            return save(comment);
        }

        @Override
        public void deleteGeneralById(long issueId, long commentId, String writerLoginId) {
            comments.remove(commentId);
        }

        @Override
        public void deleteGeneralByIdAndRecordIssueChange(
                long issueId,
                long commentId,
                String writerLoginId,
                IssueHistory history) {
            deleteGeneralById(issueId, commentId, writerLoginId);
        }
    }
}
