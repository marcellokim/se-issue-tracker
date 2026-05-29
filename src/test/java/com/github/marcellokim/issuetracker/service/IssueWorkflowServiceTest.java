package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue workflow service")
class IssueWorkflowServiceTest {

    private static final long PROJECT_ID = 10L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 28, 10, 0);

    private final User reporter = user("reporter", Role.DEV);
    private final User assignee = user("assignee", Role.DEV);
    private final User tester = user("tester", Role.TESTER);
    private final User pl = user("pl", Role.PL);

    @Test
    @DisplayName("reporter can update and comment on a new issue")
    void reporterCanUpdateAndCommentOnNewIssue() {
        Issue issue = issue(1L, IssueStatus.NEW, reporter, null, null);

        IssueWorkflowActions actions = service(issue).viewAvailableActions(issue.id(), reporter.getLoginId());

        assertTrue(actions.canUpdateIssue());
        assertTrue(actions.canAddComment());
        assertFalse(actions.canChangePriority());
        assertFalse(actions.canAssign());
        assertFalse(actions.canSoftDelete());
    }

    @Test
    @DisplayName("project lead sees management actions that match current issue status")
    void projectLeadSeesStatusAwareManagementActions() {
        Issue issue = issue(1L, IssueStatus.NEW, reporter, null, null);

        IssueWorkflowActions actions = service(issue).viewAvailableActions(issue.id(), pl.getLoginId());

        assertTrue(actions.canChangePriority());
        assertTrue(actions.canStartAssignment());
        assertTrue(actions.canAssign());
        assertTrue(actions.canAddDependency());
        assertTrue(actions.canRemoveDependency());
        assertTrue(actions.canAddComment());
        assertTrue(actions.canSoftDelete());
        assertFalse(actions.canReassign());
        assertFalse(actions.canChangeVerifier());
    }

    @Test
    @DisplayName("assignee can mark assigned issue as fixed")
    void assigneeCanMarkAssignedIssueFixed() {
        Issue issue = issue(1L, IssueStatus.ASSIGNED, reporter, assignee, tester);

        IssueWorkflowActions actions = service(issue).viewAvailableActions(issue.id(), assignee.getLoginId());

        assertTrue(actions.canMarkFixed());
        assertFalse(actions.canResolve());
        assertFalse(actions.canRejectFix());
    }

    @Test
    @DisplayName("tester resolves fixed issue only when blocking issues are complete")
    void testerResolveRequiresCompleteBlockingIssues() {
        Issue blocking = issue(1L, IssueStatus.NEW, reporter, assignee, tester);
        Issue fixed = issue(2L, IssueStatus.FIXED, reporter, assignee, tester);
        FakeIssueDependencyRepository dependencies = new FakeIssueDependencyRepository();
        dependencies.addFixture(IssueDependency.fromPersistence(1L, blocking.id(), fixed.id(), NOW));

        IssueWorkflowActions blockedActions = service(dependencies, blocking, fixed)
                .viewAvailableActions(fixed.id(), tester.getLoginId());

        assertTrue(blockedActions.canRejectFix());
        assertFalse(blockedActions.canResolve());

        Issue resolvedBlocking = issue(1L, IssueStatus.RESOLVED, reporter, assignee, tester);
        IssueWorkflowActions readyActions = service(dependencies, resolvedBlocking, fixed)
                .viewAvailableActions(fixed.id(), tester.getLoginId());

        assertTrue(readyActions.canResolve());
    }

    @Test
    @DisplayName("project lead can close resolved issues and reopen resolved or closed issues")
    void projectLeadCanCloseAndReopenCompletedIssues() {
        Issue resolved = issue(1L, IssueStatus.RESOLVED, reporter, assignee, tester);
        Issue closed = issue(2L, IssueStatus.CLOSED, reporter, assignee, tester);
        IssueWorkflowService service = service(resolved, closed);

        IssueWorkflowActions resolvedActions = service.viewAvailableActions(resolved.id(), pl.getLoginId());
        IssueWorkflowActions closedActions = service.viewAvailableActions(closed.id(), pl.getLoginId());

        assertTrue(resolvedActions.canClose());
        assertTrue(resolvedActions.canReopen());
        assertFalse(resolvedActions.canSoftDelete());
        assertFalse(resolvedActions.canAddDependency());
        assertTrue(closedActions.canReopen());
        assertTrue(closedActions.canSoftDelete());
        assertFalse(closedActions.canAddDependency());
    }

    @Test
    @DisplayName("deleted issue exposes no normal workflow actions")
    void deletedIssueExposesNoNormalWorkflowActions() {
        Issue deleted = issue(1L, IssueStatus.DELETED, reporter, null, null);

        IssueWorkflowActions actions = service(deleted).viewAvailableActions(deleted.id(), pl.getLoginId());

        assertFalse(actions.canUpdateIssue());
        assertFalse(actions.canStartAssignment());
        assertFalse(actions.canAddComment());
        assertFalse(actions.canSoftDelete());
    }

    @Test
    @DisplayName("comment actions require same issue, active membership, writer, and GENERAL purpose")
    void commentActionsRequireWriterAndGeneralPurpose() {
        Issue issue = issue(1L, IssueStatus.NEW, reporter, null, null);
        Issue otherIssue = issue(2L, IssueStatus.NEW, reporter, null, null);
        FakeCommentRepository comments = new FakeCommentRepository(
                comment(100L, issue.id(), reporter, CommentPurpose.GENERAL),
                comment(101L, issue.id(), reporter, CommentPurpose.STATUS_CHANGE));
        IssueWorkflowService service = service(new FakeIssueDependencyRepository(), comments, issue, otherIssue);

        assertTrue(service.canUpdateComment(issue.id(), 100L, reporter.getLoginId()));
        assertTrue(service.canDeleteComment(issue.id(), 100L, reporter.getLoginId()));
        assertFalse(service.canUpdateComment(otherIssue.id(), 100L, reporter.getLoginId()));
        assertFalse(service.canDeleteComment(issue.id(), 100L, assignee.getLoginId()));
        assertFalse(service.canUpdateComment(issue.id(), 101L, reporter.getLoginId()));
    }

    private IssueWorkflowService service(Issue... issues) {
        return service(new FakeIssueDependencyRepository(), new FakeCommentRepository(), issues);
    }

    private IssueWorkflowService service(FakeIssueDependencyRepository dependencies, Issue... issues) {
        return service(dependencies, new FakeCommentRepository(), issues);
    }

    private IssueWorkflowService service(
            FakeIssueDependencyRepository dependencies,
            FakeCommentRepository comments,
            Issue... issues) {
        return new IssueWorkflowService(
                new InMemoryIssueRepository(issues),
                dependencies,
                comments,
                users(),
                new PermissionPolicy());
    }

    private InMemoryUserRepository users() {
        return new InMemoryUserRepository(reporter, assignee, tester, pl)
                .withProjectMembers(
                        PROJECT_ID,
                        reporter.getLoginId(),
                        assignee.getLoginId(),
                        tester.getLoginId(),
                        pl.getLoginId());
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "hash", role, true, NOW, NOW);
    }

    private static Issue issue(long id, IssueStatus status, User reporter, User assignee, User verifier) {
        return Issue.fromPersistence(
                Issue.persistedState(PROJECT_ID, "Issue " + id, "Description " + id, reporter)
                        .id(id)
                        .issueId("ISSUE-" + id)
                        .reportedDate(NOW)
                        .priority(Priority.MAJOR)
                        .status(status)
                        .assignee(assignee)
                        .verifier(verifier)
                        .updatedAt(NOW));
    }

    private static Comment comment(long id, long issueId, User writer, CommentPurpose purpose) {
        return Comment.fromPersistence(
                id,
                issueId,
                writer.getLoginId(),
                "Comment " + id,
                purpose,
                NOW,
                NOW);
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
        public Comment saveCommentAndRecordHistory(Comment comment, IssueHistory history) {
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
