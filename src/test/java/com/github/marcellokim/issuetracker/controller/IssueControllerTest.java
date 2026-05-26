package com.github.marcellokim.issuetracker.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueHistoryRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueResult;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.IssueWorkflowActions;
import com.github.marcellokim.issuetracker.service.IssueWorkflowService;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue controller")
class IssueControllerTest {

    private static final long PROJECT_ID = 10L;
    private static final long ISSUE_ID = 1L;
    private static final long COMMENT_ID = 100L;
    private static final String PASSWORD = "pass123";
    private final LocalDateTime now = LocalDateTime.of(2026, 5, 21, 10, 0);
    private final PasswordHasher hasher = new PasswordHasher();
    private final User dev = User.fromPersistence("dev1", "Dev One", hasher.hash(PASSWORD), Role.DEV, true, now, now);
    private final User pl = User.fromPersistence("pl1", "PL One", hasher.hash(PASSWORD), Role.PL, true, now, now);
    private final Project project = Project.fromPersistence(PROJECT_ID, "ITS", "Issue Tracking", "admin", now, now);

    @Test
    @DisplayName("authenticated user registers issue")
    void registerIssue() {
        var controller = authenticatedController(dev);

        IssueResult result = controller.registerIssue(PROJECT_ID, "Bug", "Login fails", Priority.MAJOR);

        assertNotNull(result.issueId());
        assertEquals(IssueStatus.NEW, result.status());
        assertEquals("Bug", result.title());
    }

    @Test
    @DisplayName("authenticated user adds comment")
    void addComment() {
        var issue = persistedIssue();
        var controller = authenticatedController(dev, issue);

        CommentResult result = controller.addComment(ISSUE_ID, "Confirmed this bug");

        assertEquals("Confirmed this bug", result.content());
        assertEquals(dev.getLoginId(), result.writerLoginId());
    }

    @Test
    @DisplayName("authenticated user views issue comments")
    void viewComments() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(Comment.fromPersistence(
                COMMENT_ID,
                ISSUE_ID,
                dev.getLoginId(),
                "Visible comment",
                CommentPurpose.GENERAL,
                now,
                now));
        var controller = authenticatedController(dev, comments, issue);

        List<CommentResult> results = controller.viewComments(ISSUE_ID);

        assertEquals(1, results.size());
        assertEquals(String.valueOf(COMMENT_ID), results.getFirst().commentId());
        assertEquals(dev.getLoginId(), results.getFirst().writerLoginId());
        assertEquals("Visible comment", results.getFirst().content());
    }

    @Test
    @DisplayName("authenticated PL adds dependency")
    void addDependency() {
        var issueA = persistedIssue(1L, "ISSUE-1");
        var issueB = persistedIssue(2L, "ISSUE-2");
        var controller = authenticatedController(pl, issueA, issueB);

        DependencyResult result = controller.addDependency(1L, 2L);

        assertNotNull(result.dependencyId());
        assertEquals(1L, result.blockingIssueId());
        assertEquals("ISSUE-1", result.blockingIssueKey());
        assertEquals(2L, result.blockedIssueId());
        assertEquals("ISSUE-2", result.blockedIssueKey());
    }

    @Test
    @DisplayName("authenticated writer deletes general comment")
    void deleteComment() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(Comment.fromPersistence(
                COMMENT_ID,
                ISSUE_ID,
                dev.getLoginId(),
                "Remove this note",
                CommentPurpose.GENERAL,
                now,
                now));
        var controller = authenticatedController(dev, comments, issue);

        controller.deleteComment(ISSUE_ID, COMMENT_ID);

        assertFalse(comments.findById(COMMENT_ID).isPresent());
    }

    @Test
    @DisplayName("authenticated user checks canRegisterIssue")
    void canRegisterIssue() {
        var controller = authenticatedController(dev);

        assertTrue(controller.canRegisterIssue(PROJECT_ID));
    }

    @Test
    @DisplayName("authenticated user views issue detail")
    void viewIssueDetail() {
        var issue = persistedIssue();
        var controller = authenticatedController(dev, issue);

        IssueDetailResult detail = controller.viewIssueDetail(ISSUE_ID);

        assertEquals(ISSUE_ID, detail.id());
        assertEquals("Issue 1", detail.title());
        assertEquals(IssueStatus.NEW, detail.status());
    }

    @Test
    @DisplayName("authenticated user searches issues")
    void searchIssues() {
        var issue = persistedIssue();
        var controller = authenticatedController(dev, issue);

        List<IssueSummary> results = controller.searchIssues(PROJECT_ID, null, null, null);

        assertEquals(1, results.size());
        assertEquals(ISSUE_ID, results.getFirst().id());
    }

    @Test
    @DisplayName("authenticated user searches issues with extended filters")
    void searchIssuesWithFilters() {
        var issue = persistedIssue();
        var controller = authenticatedController(dev, issue);

        List<IssueSummary> results = controller.searchIssues(
                PROJECT_ID, "Issue", IssueStatus.NEW, Priority.MAJOR, null, null, null, null, null);

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("authenticated user views related project issues")
    void viewRelatedProjectIssues() {
        var issue = persistedIssue();
        var controller = authenticatedController(dev, issue);

        List<IssueSummary> results = controller.viewRelatedProjectIssues(PROJECT_ID);

        assertNotNull(results);
    }

    @Test
    @DisplayName("authenticated reporter updates issue")
    void updateIssue() {
        var issue = persistedIssue();
        var controller = authenticatedController(dev, issue);

        IssueResult result = controller.updateIssue(ISSUE_ID, "Updated Title", "Updated desc");

        assertEquals("Updated Title", result.title());
    }

    @Test
    @DisplayName("authenticated PL changes priority")
    void changePriority() {
        var issue = persistedIssue();
        var controller = authenticatedController(pl, issue);

        IssueResult result = controller.changePriority(ISSUE_ID, Priority.CRITICAL);

        assertEquals(Priority.CRITICAL, result.priority());
    }

    @Test
    @DisplayName("authenticated user views project dependencies")
    void viewProjectDependencies() {
        var issueA = persistedIssue(1L, "ISSUE-1");
        var issueB = persistedIssue(2L, "ISSUE-2");
        var controller = authenticatedController(dev, issueA, issueB);

        List<DependencyResult> results = controller.viewProjectDependencies(PROJECT_ID);

        assertNotNull(results);
    }

    @Test
    @DisplayName("authenticated PL removes dependency")
    void removeDependency() {
        var issueA = persistedIssue(1L, "ISSUE-1");
        var issueB = persistedIssue(2L, "ISSUE-2");
        var controller = authenticatedController(pl, issueA, issueB);
        controller.addDependency(1L, 2L);

        controller.removeDependency(1L, 2L);

        assertEquals(0, controller.viewProjectDependencies(PROJECT_ID).size());
    }

    @Test
    @DisplayName("authenticated writer updates comment")
    void updateComment() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(Comment.fromPersistence(
                COMMENT_ID,
                ISSUE_ID,
                dev.getLoginId(),
                "Original text",
                CommentPurpose.GENERAL,
                now,
                now));
        var controller = authenticatedController(dev, comments, issue);

        CommentResult result = controller.updateComment(ISSUE_ID, COMMENT_ID, "Edited text");

        assertEquals("Edited text", result.content());
    }

    @Test
    @DisplayName("viewIssueDetail with workflow service includes available actions")
    void viewIssueDetailWithWorkflow() {
        var issue = persistedIssue();
        var controller = authenticatedControllerWithWorkflow(pl, issue);

        IssueDetailResult detail = controller.viewIssueDetail(ISSUE_ID);

        assertNotNull(detail.availableActions());
    }

    @Test
    @DisplayName("viewAvailableActions delegates to workflow service")
    void viewAvailableActions() {
        var issue = persistedIssue();
        var controller = authenticatedControllerWithWorkflow(pl, issue);

        IssueWorkflowActions actions = controller.viewAvailableActions(ISSUE_ID);

        assertNotNull(actions);
    }

    @Test
    @DisplayName("canUpdateComment delegates to workflow service")
    void canUpdateComment() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(Comment.fromPersistence(
                COMMENT_ID,
                ISSUE_ID,
                dev.getLoginId(),
                "Note",
                CommentPurpose.GENERAL,
                now,
                now));
        var controller = authenticatedControllerWithWorkflow(dev, comments, issue);

        boolean result = controller.canUpdateComment(ISSUE_ID, COMMENT_ID);

        assertTrue(result);
    }

    @Test
    @DisplayName("canDeleteComment delegates to workflow service")
    void canDeleteComment() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(Comment.fromPersistence(
                COMMENT_ID,
                ISSUE_ID,
                dev.getLoginId(),
                "Note",
                CommentPurpose.GENERAL,
                now,
                now));
        var controller = authenticatedControllerWithWorkflow(dev, comments, issue);

        boolean result = controller.canDeleteComment(ISSUE_ID, COMMENT_ID);

        assertTrue(result);
    }

    @Test
    @DisplayName("viewAvailableActions throws without workflow service")
    void viewAvailableActionsThrowsWithoutWorkflow() {
        var issue = persistedIssue();
        var controller = authenticatedController(dev, issue);

        assertThrows(IllegalStateException.class, () -> controller.viewAvailableActions(ISSUE_ID));
    }

    @Test
    @DisplayName("unauthenticated user is rejected")
    void rejectUnauthenticated() {
        var controller = unauthenticatedController();

        assertThrows(SecurityException.class,
                () -> controller.registerIssue(PROJECT_ID, "Bug", "desc", Priority.MAJOR));
        assertThrows(SecurityException.class,
                () -> controller.addComment(ISSUE_ID, "comment"));
        assertThrows(SecurityException.class,
                () -> controller.deleteComment(ISSUE_ID, COMMENT_ID));
    }

    private IssueController authenticatedController(User user, Issue... issues) {
        return authenticatedController(user, new FakeCommentRepository(), issues);
    }

    private IssueController authenticatedController(User user, FakeCommentRepository comments, Issue... issues) {
        var users = new InMemoryUserRepository(user);
        var sessionStore = new SessionStore();
        var authService = new AuthenticationService(users, hasher, sessionStore);
        authService.login(user.getLoginId(), PASSWORD);
        var issueService = new IssueService(
                new FakeProjectRepository(project),
                new InMemoryIssueRepository(issues),
                new FakeIssueDependencyRepository(),
                comments,
                new FakeIssueHistoryRepository(),
                users,
                new PermissionPolicy(),
                new Clock());
        return new IssueController(authService, issueService);
    }

    private IssueController authenticatedControllerWithWorkflow(User user, Issue... issues) {
        return authenticatedControllerWithWorkflow(user, new FakeCommentRepository(), issues);
    }

    private IssueController authenticatedControllerWithWorkflow(User user, FakeCommentRepository comments, Issue... issues) {
        var users = new InMemoryUserRepository(user)
                .withProjectMembers(PROJECT_ID, user.getLoginId());
        var sessionStore = new SessionStore();
        var authService = new AuthenticationService(users, hasher, sessionStore);
        authService.login(user.getLoginId(), PASSWORD);
        var issueRepo = new InMemoryIssueRepository(issues);
        var depRepo = new FakeIssueDependencyRepository();
        var policy = new PermissionPolicy();
        var issueService = new IssueService(
                new FakeProjectRepository(project),
                issueRepo,
                depRepo,
                comments,
                new FakeIssueHistoryRepository(),
                users,
                policy,
                new Clock());
        var workflowService = new IssueWorkflowService(
                issueRepo, depRepo, comments, users, policy);
        return new IssueController(authService, issueService, workflowService);
    }

    private IssueController unauthenticatedController() {
        var users = new InMemoryUserRepository(dev);
        var authService = new AuthenticationService(users, hasher, new SessionStore());
        var issueService = new IssueService(
                new FakeProjectRepository(project),
                new InMemoryIssueRepository(),
                new FakeIssueDependencyRepository(),
                new FakeCommentRepository(),
                new FakeIssueHistoryRepository(),
                users,
                new PermissionPolicy(),
                new Clock());
        return new IssueController(authService, issueService);
    }

    private Issue persistedIssue() {
        return persistedIssue(ISSUE_ID, "ISSUE-1");
    }

    private Issue persistedIssue(long id, String issueId) {
        return Issue.fromPersistence(
                Issue.persistedState(PROJECT_ID, "Issue " + id, "Desc " + id, dev)
                        .id(id)
                        .issueId(issueId)
                        .reportedDate(now)
                        .priority(Priority.MAJOR)
                        .status(IssueStatus.NEW)
                        .updatedAt(now));
    }

    private static final class FakeProjectRepository implements ProjectRepository {

        private final Map<Long, Project> projects = new LinkedHashMap<>();

        private FakeProjectRepository(Project... projects) {
            for (Project p : projects) {
                this.projects.put(p.getId(), p);
            }
        }

        @Override
        public Optional<Project> findById(long projectId) {
            return Optional.ofNullable(projects.get(projectId));
        }

        @Override
        public Optional<Project> findByName(String name) {
            return Optional.empty();
        }

        @Override
        public List<Project> findAll() {
            return new ArrayList<>(projects.values());
        }

        @Override
        public Project save(Project project) {
            projects.put(project.getId(), project);
            return project;
        }

        @Override
        public void deleteById(long projectId) {
            projects.remove(projectId);
        }

        @Override
        public void addParticipant(long projectId, String userLoginId) {
        }

        @Override
        public void removeParticipant(long projectId, String userLoginId) {
        }

        @Override
        public List<ProjectMember> findParticipants(long projectId) {
            return List.of();
        }

        @Override
        public boolean existsByParticipant(String userLoginId) {
            return false;
        }
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
        public void deleteGeneralById(long issueId, long commentId, String writerLoginId) {
            Comment comment = comments.get(commentId);
            if (comment == null
                    || comment.issueId() != issueId
                    || !comment.writerId().equals(writerLoginId)
                    || comment.purpose() != CommentPurpose.GENERAL) {
                throw new IllegalArgumentException(
                        "Comment was not deleted because it does not exist, is not owned by the writer, "
                                + "or is not a GENERAL comment.");
            }
            comments.remove(commentId);
        }
    }
}
