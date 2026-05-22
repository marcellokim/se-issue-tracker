package com.github.marcellokim.issuetracker.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.IssueResult;
import com.github.marcellokim.issuetracker.service.IssueService;
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
        assertEquals(dev, result.writer());
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
                comments,
                users,
                new PermissionPolicy(),
                new Clock()
        );
        return new IssueController(authService, issueService);
    }

    private IssueController unauthenticatedController() {
        var users = new InMemoryUserRepository(dev);
        var authService = new AuthenticationService(users, hasher, new SessionStore());
        var issueService = new IssueService(
                new FakeProjectRepository(project),
                new InMemoryIssueRepository(),
                new FakeCommentRepository(),
                users,
                new PermissionPolicy(),
                new Clock()
        );
        return new IssueController(authService, issueService);
    }

    private Issue persistedIssue() {
        return Issue.fromPersistence(
                Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", dev)
                        .id(ISSUE_ID)
                        .issueId("ISSUE-1")
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
