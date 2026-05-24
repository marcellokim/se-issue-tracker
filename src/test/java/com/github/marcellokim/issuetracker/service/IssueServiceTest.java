package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
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
    private static final long OTHER_PROJECT_ID = 20L;
    private static final long ISSUE_ID = 1L;
    private static final long COMMENT_ID = 100L;
    private final LocalDateTime now = LocalDateTime.of(2026, 5, 21, 10, 0);
    private final User dev = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, now, now);
    private final User tester = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, now, now);
    private final User pl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, now, now);
    private final User otherProjectPl = User.fromPersistence("pl2", "PL Two", "hash", Role.PL, true, now, now);
    private final User admin = User.fromPersistence("admin", "Admin", "hash", Role.ADMIN, true, now, now);
    private final User inactiveDev = User.fromPersistence("dev-disabled", "Inactive Dev", "hash", Role.DEV, false, now, now);
    private final Project project = Project.fromPersistence(PROJECT_ID, "ITS", "Issue Tracking", "admin", now, now);
    private final Project otherProject = Project.fromPersistence(
            OTHER_PROJECT_ID,
            "External ITS",
            "External dependency project",
            "admin",
            now,
            now);

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
    @DisplayName("adds dependency between two issues")
    void addDependencySucceeds() {
        var issueA = persistedIssue(1L, "ISSUE-1");
        var issueB = persistedIssue(2L, "ISSUE-2");
        var deps = new FakeIssueDependencyRepository();
        var service = service(new InMemoryIssueRepository(issueA, issueB), deps);

        DependencyResult result = service.addDependency(1L, 2L, pl.getLoginId());

        assertNotNull(result.dependencyId());
        assertEquals(1L, result.blockingIssueId());
        assertEquals("ISSUE-1", result.blockingIssueKey());
        assertEquals(2L, result.blockedIssueId());
        assertEquals("ISSUE-2", result.blockedIssueKey());
        assertNotNull(result.discoveredDate());
    }

    @Test
    @DisplayName("rejects self-dependency at service level")
    void addDependencyRejectsSelf() {
        var issue = persistedIssue(1L, "ISSUE-1");
        var service = service(new InMemoryIssueRepository(issue));

        assertThrows(IllegalArgumentException.class,
                () -> service.addDependency(1L, 1L, pl.getLoginId()));
    }

    @Test
    @DisplayName("rejects duplicate dependency at service level")
    void addDependencyRejectsDuplicate() {
        var issueA = persistedIssue(1L, "ISSUE-1");
        var issueB = persistedIssue(2L, "ISSUE-2");
        var deps = new FakeIssueDependencyRepository();
        var service = service(new InMemoryIssueRepository(issueA, issueB), deps);

        service.addDependency(1L, 2L, pl.getLoginId());

        assertThrows(IllegalArgumentException.class,
                () -> service.addDependency(1L, 2L, pl.getLoginId()));
    }

    @Test
    @DisplayName("rejects cyclic dependency between two issues")
    void addDependencyRejectsTwoNodeCycle() {
        var issueA = persistedIssue(1L, "ISSUE-1");
        var issueB = persistedIssue(2L, "ISSUE-2");
        var deps = new FakeIssueDependencyRepository();
        var service = service(new InMemoryIssueRepository(issueA, issueB), deps);

        service.addDependency(1L, 2L, pl.getLoginId());

        assertThrows(IllegalArgumentException.class,
                () -> service.addDependency(2L, 1L, pl.getLoginId()));
    }

    @Test
    @DisplayName("rejects cyclic dependency across three issues")
    void addDependencyRejectsThreeNodeCycle() {
        var issueA = persistedIssue(1L, "ISSUE-1");
        var issueB = persistedIssue(2L, "ISSUE-2");
        var issueC = persistedIssue(3L, "ISSUE-3");
        var deps = new FakeIssueDependencyRepository();
        var service = service(new InMemoryIssueRepository(issueA, issueB, issueC), deps);

        service.addDependency(1L, 2L, pl.getLoginId());
        service.addDependency(2L, 3L, pl.getLoginId());

        assertThrows(IllegalArgumentException.class,
                () -> service.addDependency(3L, 1L, pl.getLoginId()));
    }

    @Test
    @DisplayName("removes existing dependency")
    void removeDependencySucceeds() {
        var issueA = persistedIssue(1L, "ISSUE-1");
        var issueB = persistedIssue(2L, "ISSUE-2");
        var deps = new FakeIssueDependencyRepository();
        var service = service(new InMemoryIssueRepository(issueA, issueB), deps);

        DependencyResult result = service.addDependency(1L, 2L, pl.getLoginId());

        service.removeDependency(result.dependencyId(), pl.getLoginId());

        assertFalse(deps.findById(result.id()).isPresent());
        assertFalse(deps.findByDependencyId(result.dependencyId()).isPresent());
        var history = issueB.getHistories().getLast();
        assertEquals(ActionType.DEPENDENCY_CHANGED, history.actionType());
        assertEquals(result.dependencyId(), history.previousValue());
        assertNull(history.newValue());
        assertEquals("Dependency removed", history.message());
    }

    @Test
    @DisplayName("rejects removing nonexistent dependency")
    void removeDependencyRejectsUnknown() {
        var service = service(new InMemoryIssueRepository());

        assertThrows(IllegalArgumentException.class,
                () -> service.removeDependency("missing-dependency-id", pl.getLoginId()));
    }

    @Test
    @DisplayName("non-PL user cannot add dependency")
    void addDependencyRejectsNonPl() {
        var issueA = persistedIssue(1L, "ISSUE-1");
        var issueB = persistedIssue(2L, "ISSUE-2");
        var service = service(new InMemoryIssueRepository(issueA, issueB));

        assertThrows(SecurityException.class,
                () -> service.addDependency(1L, 2L, dev.getLoginId()));
    }

    @Test
    @DisplayName("PL must belong to the blocked issue project to manage dependencies")
    void addDependencyRejectsPlFromOtherProject() {
        var issueA = persistedIssue(1L, "ISSUE-1");
        var issueB = persistedIssue(2L, "ISSUE-2");
        var users = new InMemoryUserRepository(dev, tester, pl, otherProjectPl, admin, inactiveDev)
                .withProjectMembers(PROJECT_ID, pl.getLoginId());
        var service = service(new InMemoryIssueRepository(issueA, issueB), new FakeIssueDependencyRepository(),
                new FakeCommentRepository(), users);

        assertThrows(SecurityException.class,
                () -> service.addDependency(1L, 2L, otherProjectPl.getLoginId()));
    }

    @Test
    @DisplayName("blocked issue project PL can add cross-project dependency")
    void addDependencyAllowsBlockedProjectLeadForCrossProjectDependency() {
        var blockingIssue = persistedIssue(1L, "ISSUE-1", OTHER_PROJECT_ID);
        var blockedIssue = persistedIssue(2L, "ISSUE-2", PROJECT_ID);
        var deps = new FakeIssueDependencyRepository();
        var users = new InMemoryUserRepository(dev, tester, pl, otherProjectPl, admin, inactiveDev)
                .withProjectMembers(PROJECT_ID, pl.getLoginId())
                .withProjectMembers(OTHER_PROJECT_ID, otherProjectPl.getLoginId());
        var service = service(new InMemoryIssueRepository(blockingIssue, blockedIssue), deps,
                new FakeCommentRepository(), users);

        DependencyResult result = service.addDependency(1L, 2L, pl.getLoginId());

        assertEquals(1L, result.blockingIssueId());
        assertEquals("ISSUE-1", result.blockingIssueKey());
        assertEquals(2L, result.blockedIssueId());
        assertEquals("ISSUE-2", result.blockedIssueKey());
        assertEquals(ActionType.DEPENDENCY_CHANGED, blockedIssue.getHistories().getLast().actionType());
        assertEquals(0, blockingIssue.getHistories().size());
    }

    @Test
    @DisplayName("searches issues and returns IssueSummary list")
    void searchIssuesReturnsSummaries() {
        var issue1 = persistedIssue(1L, "ISSUE-1");
        var issue2 = persistedIssue(2L, "ISSUE-2");
        var service = service(new InMemoryIssueRepository(issue1, issue2));

        List<IssueSummary> results = service.searchIssues(
                PROJECT_ID, null, null, null, null, null, null, dev.getLoginId());

        assertEquals(2, results.size());
        assertEquals("ISSUE-1", results.get(0).issueId());
        assertEquals(IssueStatus.NEW, results.get(0).status());
        assertEquals(Priority.MAJOR, results.get(0).priority());
    }

    @Test
    @DisplayName("search rejects ADMIN user")
    void searchIssuesRejectsAdmin() {
        var service = service(new InMemoryIssueRepository());

        assertThrows(SecurityException.class,
                () -> service.searchIssues(PROJECT_ID, null, null, null, null, null, null, admin.getLoginId()));
    }

    @Test
    @DisplayName("views issue detail with comments and histories")
    void viewIssueDetailReturnsFullResult() {
        var issue = persistedIssue();
        issue.addComment("COMMENT-1", "Test comment", dev, now.plusMinutes(5));
        var service = service(new InMemoryIssueRepository(issue));

        IssueDetailResult result = service.viewIssueDetail(ISSUE_ID, dev.getLoginId());

        assertEquals("ISSUE-1", result.issueId());
        assertEquals(IssueStatus.NEW, result.status());
        assertEquals("Issue 1", result.title());
        assertEquals(1, result.comments().size());
        assertEquals("Test comment", result.comments().get(0).content());
        assertFalse(result.histories().isEmpty());
    }

    @Test
    @DisplayName("detail includes dependency with blocking issue key")
    void viewIssueDetailIncludesDependencyWithBlockingIssueKey() {
        var issueA = persistedIssue(1L, "ISSUE-1");
        var issueB = persistedIssue(2L, "ISSUE-2");
        var deps = new FakeIssueDependencyRepository();
        var service = service(new InMemoryIssueRepository(issueA, issueB), deps);

        service.addDependency(1L, 2L, pl.getLoginId());

        IssueDetailResult result = service.viewIssueDetail(2L, dev.getLoginId());

        assertEquals(1, result.dependencies().size());
        DependencyResult dep = result.dependencies().get(0);
        assertEquals(1L, dep.blockingIssueId());
        assertEquals("ISSUE-1", dep.blockingIssueKey());
        assertEquals(2L, dep.blockedIssueId());
        assertEquals("ISSUE-2", dep.blockedIssueKey());
    }

    @Test
    @DisplayName("detail rejects nonexistent issue")
    void viewIssueDetailRejectsUnknownIssue() {
        var service = service(new InMemoryIssueRepository());

        assertThrows(IllegalArgumentException.class,
                () -> service.viewIssueDetail(999L, dev.getLoginId()));
    }

    @Test
    @DisplayName("detail rejects ADMIN user")
    void viewIssueDetailRejectsAdmin() {
        var issue = persistedIssue();
        var service = service(new InMemoryIssueRepository(issue));

        assertThrows(SecurityException.class,
                () -> service.viewIssueDetail(ISSUE_ID, admin.getLoginId()));
    }

    @Test
    @DisplayName("PL on NEW issue gets ASSIGN, DELETE, ADD_COMMENT, MANAGE_DEPENDENCY actions")
    void availableActionsForPlOnNewIssue() {
        var issue = persistedIssue();
        var service = service(new InMemoryIssueRepository(issue));

        IssueDetailResult result = service.viewIssueDetail(ISSUE_ID, pl.getLoginId());

        assertTrue(result.availableActions().contains("ASSIGN"));
        assertTrue(result.availableActions().contains("DELETE"));
        assertTrue(result.availableActions().contains("ADD_COMMENT"));
        assertTrue(result.availableActions().contains("MANAGE_DEPENDENCY"));
    }

    @Test
    @DisplayName("DEV assignee on ASSIGNED issue gets FIX action")
    void availableActionsForDevAssigneeOnAssignedIssue() {
        var issue = assignedIssue(ISSUE_ID, "ISSUE-1");
        var service = service(new InMemoryIssueRepository(issue));

        IssueDetailResult result = service.viewIssueDetail(ISSUE_ID, dev.getLoginId());

        assertTrue(result.availableActions().contains("FIX"));
        assertTrue(result.availableActions().contains("ADD_COMMENT"));
    }

    @Test
    @DisplayName("TESTER verifier on FIXED issue gets RESOLVE and REJECT_FIX actions")
    void availableActionsForTesterOnFixedIssue() {
        var issue = assignedIssue(ISSUE_ID, "ISSUE-1");
        issue.markFixed(dev, "Fixed", now.plusMinutes(20));
        var service = service(new InMemoryIssueRepository(issue));

        IssueDetailResult result = service.viewIssueDetail(ISSUE_ID, tester.getLoginId());

        assertTrue(result.availableActions().contains("RESOLVE"));
        assertTrue(result.availableActions().contains("REJECT_FIX"));
        assertTrue(result.availableActions().contains("ADD_COMMENT"));
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
        return service(issues, new FakeIssueDependencyRepository(), new FakeCommentRepository());
    }

    private IssueService service(InMemoryIssueRepository issues, FakeIssueDependencyRepository dependencies) {
        return service(issues, dependencies, new FakeCommentRepository());
    }

    private IssueService service(InMemoryIssueRepository issues, FakeCommentRepository comments) {
        return service(issues, new FakeIssueDependencyRepository(), comments);
    }

    private IssueService service(InMemoryIssueRepository issues, FakeIssueDependencyRepository dependencies, FakeCommentRepository comments) {
        return service(issues, dependencies, comments,
                new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev));
    }

    private IssueService service(
            InMemoryIssueRepository issues,
            FakeIssueDependencyRepository dependencies,
            FakeCommentRepository comments,
            InMemoryUserRepository users
    ) {
        return new IssueService(
                new FakeProjectRepository(project, otherProject),
                issues,
                dependencies,
                comments,
                users,
                new PermissionPolicy(),
                new Clock()
        );
    }

    private Issue persistedIssue() {
        return persistedIssue(ISSUE_ID, "ISSUE-1");
    }

    private Issue persistedIssue(long id, String issueId) {
        return persistedIssue(id, issueId, PROJECT_ID);
    }

    private Issue persistedIssue(long id, String issueId, long projectId) {
        return Issue.fromPersistence(
                Issue.persistedState(projectId, "Issue " + id, "Description " + id, dev)
                        .id(id)
                        .issueId(issueId)
                        .reportedDate(now)
                        .priority(Priority.MAJOR)
                        .status(IssueStatus.NEW)
                        .updatedAt(now));
    }

    private Issue assignedIssue(long id, String issueId) {
        var issue = persistedIssue(id, issueId);
        issue.assignFromNew(dev, tester, pl, now.plusMinutes(10));
        return issue;
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
