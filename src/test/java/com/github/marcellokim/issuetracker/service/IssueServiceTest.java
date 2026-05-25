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
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueHistoryRepository;
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
    private final User inactiveDev = User.fromPersistence("dev-disabled", "Inactive Dev", "hash", Role.DEV, false, now,
            now);
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

        IssueResult result = service.registerIssue(PROJECT_ID, "Login bug", "Cannot login", Priority.MAJOR,
                dev.getLoginId());

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
    @DisplayName("rejects duplicate issue title in same project including deleted issues")
    void registerIssueRejectsDuplicateTitleIncludingDeletedIssue() {
        Issue deletedIssue = persistedIssue(
                11L,
                "ISSUE-11",
                PROJECT_ID,
                "Login bug",
                IssueStatus.DELETED);
        var service = service(new InMemoryIssueRepository(deletedIssue));

        assertThrows(IllegalArgumentException.class,
                () -> service.registerIssue(PROJECT_ID, "Login bug", "Cannot login", Priority.MAJOR,
                        dev.getLoginId()));
    }

    @Test
    @DisplayName("allows same issue title in different projects")
    void registerIssueAllowsSameTitleInDifferentProject() {
        Issue otherProjectIssue = persistedIssue(
                21L,
                "ISSUE-21",
                OTHER_PROJECT_ID,
                "Login bug",
                IssueStatus.NEW);
        var service = service(new InMemoryIssueRepository(otherProjectIssue));

        IssueResult result = service.registerIssue(PROJECT_ID, "Login bug", "Cannot login", Priority.MAJOR,
                dev.getLoginId());

        assertEquals("Login bug", result.title());
        assertEquals(IssueStatus.NEW, result.status());
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
    @DisplayName("non-project members cannot register issues in that project")
    void registerIssueRejectsNonProjectMember() {
        var users = new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev)
                .withProjectMembers(PROJECT_ID, pl.getLoginId());
        var service = service(
                new InMemoryIssueRepository(),
                new FakeIssueDependencyRepository(),
                new FakeCommentRepository(),
                users);

        assertThrows(SecurityException.class,
                () -> service.registerIssue(PROJECT_ID, "Bug", "desc", Priority.MAJOR, dev.getLoginId()));
    }

    @Test
    @DisplayName("register issue availability follows project membership")
    void canRegisterIssueRequiresProjectMembership() {
        var users = new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev)
                .withProjectMembers(PROJECT_ID, pl.getLoginId(), tester.getLoginId());
        var service = service(
                new InMemoryIssueRepository(),
                new FakeIssueDependencyRepository(),
                new FakeCommentRepository(),
                users);

        assertTrue(service.canRegisterIssue(PROJECT_ID, tester.getLoginId()));
        assertFalse(service.canRegisterIssue(PROJECT_ID, dev.getLoginId()));
        assertFalse(service.canRegisterIssue(PROJECT_ID, admin.getLoginId()));
        assertFalse(service.canRegisterIssue(PROJECT_ID, inactiveDev.getLoginId()));
        assertFalse(service.canRegisterIssue(999L, tester.getLoginId()));
    }

    @Test
    @DisplayName("searches visible issues inside one project")
    void searchProjectIssuesFiltersByProjectAndKeyword() {
        Issue projectIssue = persistedIssue(11L, "ISSUE-11", PROJECT_ID, "Login bug", IssueStatus.NEW);
        Issue otherProjectIssue = persistedIssue(21L, "ISSUE-21", OTHER_PROJECT_ID, "Login bug", IssueStatus.NEW);
        var service = service(new InMemoryIssueRepository(projectIssue, otherProjectIssue));

        List<IssueSummary> results = service.searchIssues(PROJECT_ID, "login", null, null, dev.getLoginId());

        assertEquals(1, results.size());
        assertEquals(projectIssue.id(), results.getFirst().id());
        assertEquals(projectIssue.getIssueId(), results.getFirst().issueId());
        assertEquals(projectIssue.projectId(), results.getFirst().projectId());
        assertEquals(projectIssue.title(), results.getFirst().title());
        assertThrows(SecurityException.class,
                () -> service.searchIssues(PROJECT_ID, "login", null, null, admin.getLoginId()));
    }

    @Test
    @DisplayName("searches project issues by reporter, assignee, and verifier")
    void searchProjectIssuesFiltersByParticipants() {
        User tester2 = User.fromPersistence("tester2", "Tester Two", "hash", Role.TESTER, true, now, now);
        Issue matchingIssue = assignedIssue(
                11L,
                "ISSUE-11",
                PROJECT_ID,
                "Login participant match",
                tester,
                dev,
                tester);
        Issue differentReporter = assignedIssue(
                12L,
                "ISSUE-12",
                PROJECT_ID,
                "Login participant match",
                pl,
                dev,
                tester);
        Issue differentAssignee = assignedIssue(
                13L,
                "ISSUE-13",
                PROJECT_ID,
                "Login participant match",
                tester,
                inactiveDev,
                tester);
        Issue differentVerifier = assignedIssue(
                14L,
                "ISSUE-14",
                PROJECT_ID,
                "Login participant match",
                tester,
                dev,
                tester2);
        var service = service(new InMemoryIssueRepository(
                matchingIssue,
                differentReporter,
                differentAssignee,
                differentVerifier));

        List<IssueSummary> results = service.searchIssues(
                PROJECT_ID,
                "login",
                null,
                null,
                tester.getLoginId(),
                dev.getLoginId(),
                tester.getLoginId(),
                dev.getLoginId());

        assertEquals(List.of(matchingIssue.id()), results.stream().map(IssueSummary::id).toList());
    }

    @Test
    @DisplayName("shows only assigned participant issues for DEV and TESTER")
    void viewRelatedProjectIssuesReturnsOnlyActorRelatedIssues() {
        Issue reporterOnlyIssue = persistedIssue(
                11L,
                "ISSUE-11",
                PROJECT_ID,
                "Reporter only issue",
                IssueStatus.NEW,
                dev);
        Issue assignedDevIssue = assignedIssue(
                12L,
                "ISSUE-12",
                PROJECT_ID,
                "Assigned dev issue",
                tester,
                dev,
                tester);
        Issue otherProjectIssue = assignedIssue(
                21L,
                "ISSUE-21",
                OTHER_PROJECT_ID,
                "Other issue",
                dev,
                dev,
                tester);
        var users = new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev)
                .withProjectMembers(PROJECT_ID, dev.getLoginId(), tester.getLoginId(), pl.getLoginId())
                .withProjectMembers(OTHER_PROJECT_ID, dev.getLoginId());
        var service = service(
                new InMemoryIssueRepository(reporterOnlyIssue, assignedDevIssue, otherProjectIssue),
                new FakeIssueDependencyRepository(),
                new FakeCommentRepository(),
                users);

        List<IssueSummary> devResults = service.viewRelatedProjectIssues(PROJECT_ID, dev.getLoginId());
        List<IssueSummary> testerResults = service.viewRelatedProjectIssues(PROJECT_ID, tester.getLoginId());
        List<IssueSummary> plResults = service.viewRelatedProjectIssues(PROJECT_ID, pl.getLoginId());

        assertEquals(1, devResults.size());
        assertEquals(assignedDevIssue.id(), devResults.getFirst().id());
        assertEquals(1, testerResults.size());
        assertEquals(assignedDevIssue.id(), testerResults.getFirst().id());
        assertEquals(2, plResults.size());
        assertThrows(SecurityException.class,
                () -> service.viewRelatedProjectIssues(PROJECT_ID, admin.getLoginId()));
    }

    @Test
    @DisplayName("issue detail result includes comments histories and dependencies")
    void viewIssueDetailIncludesAssociatedData() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
        var histories = new FakeIssueHistoryRepository();
        histories.addFixture(IssueHistory.fromPersistence(
                200L,
                ISSUE_ID,
                dev.getLoginId(),
                ActionType.COMMENTED,
                null,
                "Outdated investigation note",
                "Outdated investigation note",
                now));
        var dependencies = new FakeIssueDependencyRepository();
        dependencies.addFixture(IssueDependency.fromPersistence(300L, "dep-1", 99L, ISSUE_ID, now));
        var service = service(
                new InMemoryIssueRepository(issue),
                dependencies,
                comments,
                histories,
                new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev));

        IssueDetailResult detail = service.viewIssueDetail(ISSUE_ID, dev.getLoginId());

        assertEquals(issue.id(), detail.id());
        assertEquals(issue.projectId(), detail.projectId());
        assertEquals(issue.getIssueId(), detail.issueId());
        assertEquals(1, detail.comments().size());
        assertEquals(String.valueOf(COMMENT_ID), detail.comments().getFirst().commentId());
        assertEquals(1, detail.histories().size());
        assertEquals(ActionType.COMMENTED, detail.histories().getFirst().actionType());
        assertEquals(1, detail.dependencies().size());
        assertEquals("dep-1", detail.dependencies().getFirst().dependencyId());
        assertTrue(detail.availableActions().isEmpty());
    }

    @Test
    @DisplayName("reporter updates title and description before assignment")
    void updateIssueSucceedsForReporterBeforeAssignment() {
        var issue = persistedIssue();
        var service = service(new InMemoryIssueRepository(issue));

        IssueResult result = service.updateIssue(ISSUE_ID, "Updated title", "Updated description", dev.getLoginId());

        assertEquals("Updated title", result.title());
        assertEquals("Updated description", result.description());
        assertEquals(ActionType.TITLE_DESCRIPTION_UPDATED, issue.getHistories().getLast().actionType());
    }

    @Test
    @DisplayName("non-reporter cannot update title and description")
    void updateIssueRejectsNonReporter() {
        var issue = persistedIssue();
        var service = service(new InMemoryIssueRepository(issue));

        assertThrows(SecurityException.class,
                () -> service.updateIssue(ISSUE_ID, "Updated title", "Updated description", tester.getLoginId()));
    }

    @Test
    @DisplayName("project PL changes issue priority")
    void changePrioritySucceedsForProjectPl() {
        var issue = persistedIssue();
        var service = service(new InMemoryIssueRepository(issue));

        IssueResult result = service.changePriority(ISSUE_ID, Priority.CRITICAL, pl.getLoginId());

        assertEquals(Priority.CRITICAL, result.priority());
        assertEquals(ActionType.PRIORITY_CHANGED, issue.getHistories().getLast().actionType());
        assertEquals(Priority.MAJOR.name(), issue.getHistories().getLast().previousValue());
        assertEquals(Priority.CRITICAL.name(), issue.getHistories().getLast().newValue());
    }

    @Test
    @DisplayName("non-PL cannot change issue priority")
    void changePriorityRejectsNonPl() {
        var issue = persistedIssue();
        var service = service(new InMemoryIssueRepository(issue));

        assertThrows(SecurityException.class,
                () -> service.changePriority(ISSUE_ID, Priority.CRITICAL, dev.getLoginId()));
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
        assertEquals(dev.getLoginId(), result.writer().getLoginId());
        assertNotNull(result.createdDate());
    }

    @Test
    @DisplayName("views comments for an issue")
    void viewCommentsReturnsIssueComments() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(
                comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL),
                comment(COMMENT_ID + 1L, 999L, tester, CommentPurpose.GENERAL));
        var service = service(new InMemoryIssueRepository(issue), comments);

        List<CommentResult> results = service.viewComments(ISSUE_ID, dev.getLoginId());

        assertEquals(1, results.size());
        assertEquals(String.valueOf(COMMENT_ID), results.getFirst().commentId());
        assertEquals(dev.getLoginId(), results.getFirst().writerLoginId());
        assertEquals("Outdated investigation note", results.getFirst().content());
    }

    @Test
    @DisplayName("general comment id is generated independently from hydrated comment count")
    void addCommentUsesGeneratedCommentId() {
        var issue = persistedIssue();
        var service = service(new InMemoryIssueRepository(issue));

        CommentResult result = service.addComment(ISSUE_ID, "Looks like a real bug", dev.getLoginId());

        assertEquals(CommentPurpose.GENERAL, result.purpose());
        assertTrue(
                result.commentId().startsWith("COMMENT-"),
                "commentId should use the shared generated comment id contract");
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
    @DisplayName("non-project members cannot add issue comments")
    void addCommentRejectsNonProjectMember() {
        var issue = persistedIssue();
        var users = new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev)
                .withProjectMembers(PROJECT_ID, tester.getLoginId(), pl.getLoginId());
        var service = service(
                new InMemoryIssueRepository(issue),
                new FakeIssueDependencyRepository(),
                new FakeCommentRepository(),
                users);

        assertThrows(SecurityException.class,
                () -> service.addComment(ISSUE_ID, "Cross-project comment", dev.getLoginId()));
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
    @DisplayName("writer updates own comment content")
    void updateCommentSucceedsForWriter() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
        var histories = new FakeIssueHistoryRepository();
        var service = service(new InMemoryIssueRepository(issue), new FakeIssueDependencyRepository(), comments,
                histories, new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev));

        CommentResult result = service.updateComment(
                ISSUE_ID,
                COMMENT_ID,
                "Updated investigation note",
                dev.getLoginId());

        assertEquals("Updated investigation note", result.content());
        assertEquals("Updated investigation note", comments.findById(COMMENT_ID).orElseThrow().content());
        assertNotNull(result.updatedDate());
        IssueHistory history = histories.findByIssueId(ISSUE_ID).getFirst();
        assertEquals(ActionType.COMMENTED, history.actionType());
        assertEquals("Outdated investigation note", history.previousValue());
        assertEquals("Updated investigation note", history.newValue());
        assertEquals("Updated investigation note", history.message());
        assertEquals(dev.getLoginId(), history.changedById());
    }

    @Test
    @DisplayName("comment update appends history without changing existing history")
    void updateCommentAppendsHistoryWithoutChangingExistingHistory() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
        var histories = new FakeIssueHistoryRepository();
        IssueHistory existingHistory = IssueHistory.fromPersistence(
                77L,
                ISSUE_ID,
                dev.getLoginId(),
                ActionType.COMMENTED,
                null,
                "Original comment",
                "Original comment",
                now.minusDays(1));
        histories.addFixture(existingHistory);
        var service = service(new InMemoryIssueRepository(issue), new FakeIssueDependencyRepository(), comments,
                histories, new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev));

        service.updateComment(ISSUE_ID, COMMENT_ID, "Updated investigation note", dev.getLoginId());

        List<IssueHistory> issueHistories = histories.findByIssueId(ISSUE_ID);
        assertEquals(2, issueHistories.size());
        IssueHistory unchangedHistory = histories.findById(existingHistory.id()).orElseThrow();
        assertEquals("Original comment", unchangedHistory.newValue());
        assertEquals("Original comment", unchangedHistory.message());
        IssueHistory appendedHistory = issueHistories.get(1);
        assertEquals(ActionType.COMMENTED, appendedHistory.actionType());
        assertEquals("Outdated investigation note", appendedHistory.previousValue());
        assertEquals("Updated investigation note", appendedHistory.newValue());
        assertEquals("Updated investigation note", appendedHistory.message());
    }

    @Test
    @DisplayName("writer updates own status-change comment content")
    void updateStatusChangeCommentSucceedsForWriter() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.STATUS_CHANGE));
        var histories = new FakeIssueHistoryRepository();
        var service = service(new InMemoryIssueRepository(issue), new FakeIssueDependencyRepository(), comments,
                histories, new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev));

        CommentResult result = service.updateComment(
                ISSUE_ID,
                COMMENT_ID,
                "Updated status transition reason",
                dev.getLoginId());

        assertEquals(CommentPurpose.STATUS_CHANGE, result.purpose());
        assertEquals("Updated status transition reason", result.content());
        assertEquals("Updated status transition reason", comments.findById(COMMENT_ID).orElseThrow().content());
        IssueHistory history = histories.findByIssueId(ISSUE_ID).getFirst();
        assertEquals(ActionType.COMMENTED, history.actionType());
        assertEquals("Outdated investigation note", history.previousValue());
        assertEquals("Updated status transition reason", history.newValue());
        assertEquals("Updated status transition reason", history.message());
    }

    @Test
    @DisplayName("rejects comment update by non-writer")
    void updateCommentRejectsNonWriter() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
        var service = service(new InMemoryIssueRepository(issue), comments);

        assertThrows(SecurityException.class,
                () -> service.updateComment(ISSUE_ID, COMMENT_ID, "Updated", tester.getLoginId()));
        assertEquals("Outdated investigation note", comments.findById(COMMENT_ID).orElseThrow().content());
    }

    @Test
    @DisplayName("comment writer must still belong to the issue project to update")
    void updateCommentRejectsWriterOutsideProject() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
        var histories = new FakeIssueHistoryRepository();
        var users = new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev)
                .withProjectMembers(PROJECT_ID, tester.getLoginId(), pl.getLoginId());
        var service = service(new InMemoryIssueRepository(issue), new FakeIssueDependencyRepository(), comments,
                histories, users);

        assertThrows(SecurityException.class,
                () -> service.updateComment(ISSUE_ID, COMMENT_ID, "Updated", dev.getLoginId()));
        assertEquals("Outdated investigation note", comments.findById(COMMENT_ID).orElseThrow().content());
        assertEquals(0, histories.findByIssueId(ISSUE_ID).size());
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
    @DisplayName("comment writer must still belong to the issue project to delete")
    void deleteCommentRejectsWriterOutsideProject() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
        var users = new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev)
                .withProjectMembers(PROJECT_ID, tester.getLoginId(), pl.getLoginId());
        var service = service(
                new InMemoryIssueRepository(issue),
                new FakeIssueDependencyRepository(),
                comments,
                users);

        assertThrows(SecurityException.class,
                () -> service.deleteComment(ISSUE_ID, COMMENT_ID, dev.getLoginId()));
        assertNotNull(comments.findById(COMMENT_ID).orElseThrow());
    }

    @Test
    @DisplayName("rejects status-change comment delete")
    void deleteCommentRejectsStatusChangeComment() {
        var issue = persistedIssue();
        var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.STATUS_CHANGE));
        var service = service(new InMemoryIssueRepository(issue), comments);

        assertThrows(SecurityException.class,
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

    private IssueService service(InMemoryIssueRepository issues, FakeIssueDependencyRepository dependencies,
            FakeCommentRepository comments) {
        return service(issues, dependencies, comments,
                new FakeIssueHistoryRepository(),
                new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev));
    }

    private IssueService service(
            InMemoryIssueRepository issues,
            FakeIssueDependencyRepository dependencies,
            FakeCommentRepository comments,
            InMemoryUserRepository users) {
        return service(issues, dependencies, comments, new FakeIssueHistoryRepository(), users);
    }

    private IssueService service(
            InMemoryIssueRepository issues,
            FakeIssueDependencyRepository dependencies,
            FakeCommentRepository comments,
            FakeIssueHistoryRepository histories,
            InMemoryUserRepository users) {
        return new IssueService(
                new FakeProjectRepository(project, otherProject),
                issues,
                dependencies,
                comments,
                histories,
                users,
                new PermissionPolicy(),
                new Clock());
    }

    private Issue persistedIssue() {
        return persistedIssue(ISSUE_ID, "ISSUE-1");
    }

    private Issue persistedIssue(long id, String issueId) {
        return persistedIssue(id, issueId, PROJECT_ID);
    }

    private Issue persistedIssue(long id, String issueId, long projectId) {
        return persistedIssue(id, issueId, projectId, "Issue " + id, IssueStatus.NEW);
    }

    private Issue persistedIssue(long id, String issueId, long projectId, String title, IssueStatus status) {
        return persistedIssue(id, issueId, projectId, title, status, dev);
    }

    private Issue persistedIssue(long id, String issueId, long projectId, String title, IssueStatus status,
            User reporter) {
        return Issue.fromPersistence(
                Issue.persistedState(projectId, title, "Description " + id, reporter)
                        .id(id)
                        .issueId(issueId)
                        .reportedDate(now)
                        .priority(Priority.MAJOR)
                        .status(status)
                        .updatedAt(now));
    }

    private Issue assignedIssue(
            long id,
            String issueId,
            long projectId,
            String title,
            User reporter,
            User assignee,
            User verifier) {
        return Issue.fromPersistence(
                Issue.persistedState(projectId, title, "Description " + id, reporter)
                        .id(id)
                        .issueId(issueId)
                        .reportedDate(now)
                        .priority(Priority.MAJOR)
                        .status(IssueStatus.ASSIGNED)
                        .assignee(assignee)
                        .verifier(verifier)
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
