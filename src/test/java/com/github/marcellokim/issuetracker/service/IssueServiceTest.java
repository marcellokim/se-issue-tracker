package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.ActionType;
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
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue service")
class IssueServiceTest {

    private static final long PROJECT_ID = 10L;
    private static final long ISSUE_ID = 1L;
    private static final long COMMENT_ID = 100L;
    private final LocalDateTime now = LocalDateTime.of(2026, 5, 21, 10, 0);
    private final User dev = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, now, now);
    private final User tester = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, now, now);
    private final User pl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, now, now);
    private final User admin = User.fromPersistence("admin", "Admin", "hash", Role.ADMIN, true, now, now);
    private final User inactiveDev = User.fromPersistence("dev-disabled", "Inactive Dev", "hash", Role.DEV, false, now, now);
    private final Project project = Project.fromPersistence(PROJECT_ID, "ITS", "Issue Tracking", "admin", now, now);

    @Test
    @DisplayName("registers a new issue with project and reporter")
    void registerIssueSucceeds() {
        var service = service(new InMemoryIssueRepository());

        IssueResult result = service.registerIssue(PROJECT_ID, "Login bug", "Cannot login", Priority.MAJOR, dev.getLoginId());

        assertNotNull(result.issueId());
        assertEquals(IssueStatus.NEW, result.status());
        assertEquals(Priority.MAJOR, result.priority());
        assertEquals("Login bug", result.title());
        assertEquals("Cannot login", result.description());
    }

    @Test
    @DisplayName("registers issue with default priority when null")
    void registerIssueDefaultPriority() {
        var service = service(new InMemoryIssueRepository());

        IssueResult result = service.registerIssue(PROJECT_ID, "Bug", "desc", null, dev.getLoginId());

        assertEquals(Priority.MAJOR, result.priority());
    }

    @Test
    @DisplayName("rejects issue registration for nonexistent project")
    void registerIssueRejectsUnknownProject() {
        var service = service(new InMemoryIssueRepository());

        assertThrows(IllegalArgumentException.class,
                () -> service.registerIssue(999L, "Bug", "desc", Priority.MAJOR, dev.getLoginId()));
    }

    @Test
    @DisplayName("rejects issue registration for nonexistent user")
    void registerIssueRejectsUnknownUser() {
        var service = service(new InMemoryIssueRepository());

        assertThrows(IllegalArgumentException.class,
                () -> service.registerIssue(PROJECT_ID, "Bug", "desc", Priority.MAJOR, "unknown"));
    }

    @Test
    @DisplayName("adds comment to existing issue")
    void addCommentSucceeds() {
        var issue = persistedIssue();
        var service = service(new InMemoryIssueRepository(issue));

        CommentResult result = service.addComment(ISSUE_ID, "Looks like a real bug", dev.getLoginId());

        assertNotNull(result.commentId());
        assertEquals(CommentPurpose.GENERAL, result.purpose());
        assertEquals("Looks like a real bug", result.content());
        assertEquals(dev, result.writer());
        assertNotNull(result.createdDate());
    }

    @Test
    @DisplayName("general comment id is generated independently from hydrated comment count")
    void addCommentUsesGeneratedCommentId() {
        var issue = persistedIssue();
        var service = service(new InMemoryIssueRepository(issue));

        CommentResult result = service.addComment(ISSUE_ID, "Looks like a real bug", dev.getLoginId());

        assertEquals(CommentPurpose.GENERAL, result.purpose());
        org.junit.jupiter.api.Assertions.assertTrue(
                result.commentId().startsWith("COMMENT-"),
                "commentId should use the shared generated comment id contract"
        );
    }

    @Test
    @DisplayName("ADMIN and inactive users cannot add issue comments")
    void addCommentRejectsAdminAndInactiveUser() {
        var issue = persistedIssue();
        var service = service(new InMemoryIssueRepository(issue));

        assertThrows(SecurityException.class,
                () -> service.addComment(ISSUE_ID, "admin comment", admin.getLoginId()));
        assertThrows(SecurityException.class,
                () -> service.addComment(ISSUE_ID, "inactive comment", inactiveDev.getLoginId()));
    }

    @Test
    @DisplayName("rejects comment on nonexistent issue")
    void addCommentRejectsUnknownIssue() {
        var service = service(new InMemoryIssueRepository());

        assertThrows(IllegalArgumentException.class,
                () -> service.addComment(999L, "comment", dev.getLoginId()));
    }

    @Test
    @DisplayName("rejects comment with nonexistent user")
    void addCommentRejectsUnknownUser() {
        var issue = persistedIssue();
        var service = service(new InMemoryIssueRepository(issue));

        assertThrows(IllegalArgumentException.class,
                () -> service.addComment(ISSUE_ID, "comment", "unknown"));
    }

    @Test
    @DisplayName("rejects blank comment content")
    void addCommentRejectsBlankContent() {
        var issue = persistedIssue();
        var service = service(new InMemoryIssueRepository(issue));

        assertThrows(IllegalArgumentException.class,
                () -> service.addComment(ISSUE_ID, "", dev.getLoginId()));
    }

    @Test
    @DisplayName("deletes writer-owned general comment and records comment history")
    void deleteCommentSucceeds() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
        var service = service(new InMemoryIssueRepository(issue), comments);

        service.deleteComment(ISSUE_ID, COMMENT_ID, dev.getLoginId());

        assertFalse(comments.findById(COMMENT_ID).isPresent());
        var history = issue.getHistories().getLast();
        assertEquals(ActionType.COMMENTED, history.actionType());
        assertEquals("Outdated investigation note", history.previousValue());
        assertNull(history.newValue());
        assertNull(history.message());
        assertEquals(dev.getLoginId(), history.changedById());
    }

    @Test
    @DisplayName("rejects comment delete by non-writer")
    void deleteCommentRejectsNonWriter() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
        var service = service(new InMemoryIssueRepository(issue), comments);

        assertThrows(SecurityException.class,
                () -> service.deleteComment(ISSUE_ID, COMMENT_ID, tester.getLoginId()));
        assertNotNull(comments.findById(COMMENT_ID).orElseThrow());
    }

    @Test
    @DisplayName("rejects status-change comment delete")
    void deleteCommentRejectsStatusChangeComment() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.STATUS_CHANGE));
        var service = service(new InMemoryIssueRepository(issue), comments);

        assertThrows(IllegalArgumentException.class,
                () -> service.deleteComment(ISSUE_ID, COMMENT_ID, dev.getLoginId()));
        assertNotNull(comments.findById(COMMENT_ID).orElseThrow());
    }

    @Test
    @DisplayName("rejects comment delete for different issue")
    void deleteCommentRejectsDifferentIssue() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(comment(COMMENT_ID, 999L, dev, CommentPurpose.GENERAL));
        var service = service(new InMemoryIssueRepository(issue), comments);

        assertThrows(IllegalArgumentException.class,
                () -> service.deleteComment(ISSUE_ID, COMMENT_ID, dev.getLoginId()));
        assertNotNull(comments.findById(COMMENT_ID).orElseThrow());
    }

    private IssueService service(InMemoryIssueRepository issues) {
        return service(issues, new FakeCommentRepository());
    }

    private IssueService service(InMemoryIssueRepository issues, FakeCommentRepository comments) {
        return new IssueService(
                new FakeProjectRepository(project),
                issues,
                comments,
                new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev),
                new PermissionPolicy(),
                new Clock()
        );
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

    private Comment comment(long commentId, long issueId, User writer, CommentPurpose purpose) {
        return Comment.fromPersistence(
                commentId,
                issueId,
                writer.getLoginId(),
                "Outdated investigation note",
                purpose,
                now,
                now);
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
            return projects.values().stream()
                    .filter(p -> p.getName().equals(name))
                    .findFirst();
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
