package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue")
class IssueTest {

    private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, null, null);
    private final User assignee = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, null, null);
    private final User otherDeveloper = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
    private final User verifier = User.fromPersistence("tester2", "Tester Two", "hash", Role.TESTER, true, null,
            null);
    private final User otherTester = User.fromPersistence("tester3", "Tester Three", "hash", Role.TESTER, true, null,
            null);
    private final User pl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, null, null);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("new issue starts as NEW and records creation history")
    void createIssueWithReporterDefaultStatusAndCreationHistory() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);

        assertEquals("ISSUE-1", issue.getIssueId());
        assertEquals("Login fails", issue.getTitle());
        assertEquals("Cannot log in", issue.getDescription());
        assertSame(reporter, issue.getReporter());
        assertEquals(createdAt, issue.getReportedDate());
        assertEquals(Priority.MAJOR, issue.getPriority());
        assertEquals(IssueStatus.NEW, issue.getStatus());
        assertEquals(1, issue.getHistories().size());

        var history = issue.getHistories().getFirst();
        assertEquals(ActionType.CREATED, history.getAction());
        assertNull(history.getPreviousValue());
        assertEquals(IssueStatus.NEW.name(), history.getNewValue());
        assertSame(reporter, history.getChangedBy());
        assertEquals(createdAt, history.getChangedDate());
    }

    @Test
    @DisplayName("explicit priority is kept")
    void createIssueWithExplicitPriority() {
        var issue = IssueFixtures.create("ISSUE-1", "Crash", "App crashes", Priority.CRITICAL, reporter, createdAt);

        assertEquals(Priority.CRITICAL, issue.getPriority());
    }

    @Test
    @DisplayName("persisted issue uses saved issue id")
    void persistedIssueRequiresStableIssueId() {
        var persisted = Issue.fromPersistence(Issue.persistedState(
                1L,
                "Persisted issue",
                "Loaded from DB.",
                reporter)
                .id(10L)
                .issueId("ISSUE-STABLE-1")
                .reportedDate(createdAt)
                .updatedAt(createdAt));

        assertEquals(10L, persisted.id());
        assertEquals("ISSUE-STABLE-1", persisted.getIssueId());
        assertThrows(IllegalArgumentException.class, () -> Issue.fromPersistence(Issue.persistedState(
                1L,
                "Missing issue id",
                "Persisted state must carry DB issue_id.",
                reporter)
                .id(11L)
                .reportedDate(createdAt)
                .updatedAt(createdAt)));
    }

    @Test
    @DisplayName("new issue can generate an issue id before save")
    void createIssueCanGenerateIssueIdBeforeSave() {
        var issue = Issue.create(Issue.persistedState(
                1L,
                "New repository issue",
                "Before DB identity is assigned.",
                reporter)
                .reportedDate(createdAt)
                .updatedAt(createdAt));

        assertEquals(0L, issue.id());
        assertFalse(issue.getIssueId().isBlank());
        assertEquals(1, issue.getHistories().size());
        var history = issue.getHistories().getFirst();
        assertEquals(ActionType.CREATED, history.getAction());
        assertNull(history.getPreviousValue());
        assertEquals(IssueStatus.NEW.name(), history.getNewValue());
        assertEquals("Issue created", history.getMessage());
        assertSame(reporter, history.getChangedBy());
        assertEquals(createdAt, history.getChangedDate());
    }

    @Test
    @DisplayName("explicit issue id is kept before save")
    void createWithExplicitIssueId() {
        var issue = Issue.create(Issue.persistedState(
                1L, "Bug", "desc", reporter)
                .issueId("CUSTOM-ID")
                .reportedDate(createdAt)
                .updatedAt(createdAt));

        assertEquals("CUSTOM-ID", issue.getIssueId());
    }

    @Test
    @DisplayName("persisted issue id must be positive")
    void fromPersistenceRejectsNonPositiveId() {
        assertThrows(IllegalArgumentException.class, () -> Issue.fromPersistence(
                Issue.persistedState(1L, "Bug", "desc", reporter)
                        .id(0L)
                        .issueId("ISSUE-1")
                        .reportedDate(createdAt)
                        .updatedAt(createdAt)));
        assertThrows(IllegalArgumentException.class, () -> Issue.fromPersistence(
                Issue.persistedState(1L, "Bug", "desc", reporter)
                        .id(-1L)
                        .issueId("ISSUE-1")
                        .reportedDate(createdAt)
                        .updatedAt(createdAt)));
    }

    @Test
    @DisplayName("new issue cannot start with a database id")
    void createRejectsNonZeroId() {
        assertThrows(IllegalArgumentException.class, () -> Issue.create(
                Issue.persistedState(1L, "Bug", "desc", reporter)
                        .id(5L)
                        .reportedDate(createdAt)
                        .updatedAt(createdAt)));
    }

    @Test
    @DisplayName("persisted issue exposes stored values")
    void persistenceGettersReturnExpectedValues() {
        var issue = Issue.fromPersistence(Issue.persistedState(
                1L, "Bug", "desc", reporter)
                .id(10L)
                .issueId("ISSUE-1")
                .reportedDate(createdAt)
                .updatedAt(createdAt));

        assertEquals("Bug", issue.title());
        assertEquals("desc", issue.description());
        assertEquals(createdAt, issue.reportedDate());
        assertEquals(Priority.MAJOR, issue.priority());
        assertEquals(IssueStatus.NEW, issue.status());
        assertEquals(reporter.getLoginId(), issue.reporterId());
        assertNull(issue.assigneeId());
        assertNull(issue.verifierId());
        assertNull(issue.fixerId());
        assertNull(issue.resolverId());
        assertEquals(createdAt, issue.updatedAt());
    }

    @Test
    @DisplayName("issue creation needs required fields")
    void rejectInvalidIssueCreationArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> IssueFixtures.create("", "Title", "Description", null, reporter, createdAt));
        assertThrows(IllegalArgumentException.class,
                () -> IssueFixtures.create("ISSUE-1", "", "Description", null, reporter, createdAt));
        assertThrows(IllegalArgumentException.class,
                () -> IssueFixtures.create("ISSUE-1", "Title", "", null, reporter, createdAt));
        assertThrows(NullPointerException.class,
                () -> IssueFixtures.create("ISSUE-1", "Title", "Description", null, null, createdAt));
        assertThrows(NullPointerException.class,
                () -> IssueFixtures.create("ISSUE-1", "Title", "Description", null, reporter, null));
    }

    @Test
    @DisplayName("NEW issue assignment sets assignee and verifier")
    void assignFromNewIssue() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var assignedAt = createdAt.plusMinutes(10);

        issue.assignFromNew(assignee, verifier, pl, assignedAt);

        assertSame(assignee, issue.getAssignee());
        assertSame(verifier, issue.getVerifier());
        assertEquals(IssueStatus.ASSIGNED, issue.getStatus());
        assertEquals(3, issue.getHistories().size());

        var assignmentHistory = findHistory(issue, ActionType.ASSIGNMENT_CHANGED);
        assertEquals(ActionType.ASSIGNMENT_CHANGED, assignmentHistory.getAction());

        var statusHistory = findHistory(issue, ActionType.STATUS_CHANGED);
        assertEquals(IssueStatus.NEW.name(), statusHistory.getPreviousValue());
        assertEquals(IssueStatus.ASSIGNED.name(), statusHistory.getNewValue());
    }

    @Test
    @DisplayName("REOPENED issue assignment moves to ASSIGNED")
    void assignReopenedIssue() {
        var issue = reopenedIssue();

        issue.assignReopened(assignee, verifier, pl, createdAt.plusMinutes(40));

        assertSame(assignee, issue.getAssignee());
        assertSame(verifier, issue.getVerifier());
        assertEquals(IssueStatus.ASSIGNED, issue.getStatus());
    }

    @Test
    @DisplayName("assigned issue changes assignee only")
    void reassignAssigneeOnly() {
        var issue = assignedIssue();
        var reassignedAt = createdAt.plusMinutes(20);

        issue.reassignAssignee(otherDeveloper, pl, reassignedAt);

        assertSame(otherDeveloper, issue.getAssignee());
        assertSame(verifier, issue.getVerifier());
        assertEquals(IssueStatus.ASSIGNED, issue.getStatus());

        var history = issue.getHistories().getLast();
        assertEquals(ActionType.ASSIGNMENT_CHANGED, history.getAction());
        assertEquals(assignee.getLoginId(), history.getPreviousValue());
        assertEquals(otherDeveloper.getLoginId(), history.getNewValue());
        assertEquals(reassignedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("fixed issue changes verifier only")
    void changeVerifierOnly() {
        var issue = assignedIssue();
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
        var changedAt = createdAt.plusMinutes(30);

        issue.changeVerifier(otherTester, pl, changedAt);

        assertSame(assignee, issue.getAssignee());
        assertSame(otherTester, issue.getVerifier());
        assertSame(assignee, issue.getFixer());
        assertEquals(IssueStatus.FIXED, issue.getStatus());

        var history = issue.getHistories().getLast();
        assertEquals(ActionType.ASSIGNMENT_CHANGED, history.getAction());
        assertEquals(verifier.getLoginId(), history.getPreviousValue());
        assertEquals(otherTester.getLoginId(), history.getNewValue());
        assertEquals(changedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("assignee must be DEV and verifier must be TESTER")
    void rejectInvalidAssignmentRoles() {
        var invalidAssigneeIssue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter,
                createdAt);
        var invalidVerifierIssue = IssueFixtures.create("ISSUE-2", "Signup fails", "Cannot sign up", null, reporter,
                createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> invalidAssigneeIssue.assignFromNew(verifier, verifier, pl, createdAt));
        assertThrows(IllegalArgumentException.class,
                () -> invalidVerifierIssue.assignFromNew(assignee, assignee, pl, createdAt));
    }

    @Test
    @DisplayName("inactive users cannot be assignee or verifier")
    void rejectInactiveAssignmentParticipants() {
        var inactiveAssignee = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
        var inactiveVerifier = User.fromPersistence("tester3", "Tester Three", "hash", Role.TESTER, true, null, null);
        inactiveAssignee.deactivate(createdAt.plusMinutes(1));
        inactiveVerifier.deactivate(createdAt.plusMinutes(1));

        var issueForAssignee = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter,
                createdAt);
        assertThrows(IllegalArgumentException.class,
                () -> issueForAssignee.assignFromNew(inactiveAssignee, verifier, pl,
                        createdAt.plusMinutes(10)));

        var issueForVerifier = IssueFixtures.create("ISSUE-2", "Signup fails", "Cannot sign up", null, reporter,
                createdAt);
        assertThrows(IllegalArgumentException.class,
                () -> issueForVerifier.assignFromNew(assignee, inactiveVerifier, pl,
                        createdAt.plusMinutes(10)));
    }

    @Test
    @DisplayName("assignFromNew requires NEW status")
    void rejectAssignFromNewWhenStatusIsNotNew() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));

        assertThrows(IllegalStateException.class,
                () -> issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(20)));
    }

    @Test
    @DisplayName("assignment changes require matching status branch")
    void rejectAssignmentChangesForInvalidStatus() {
        var newIssue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var issue = assignedIssue();

        assertThrows(IllegalStateException.class,
                () -> newIssue.reassignAssignee(otherDeveloper, pl, createdAt.plusMinutes(10)));
        assertThrows(IllegalStateException.class,
                () -> issue.changeVerifier(otherTester, pl, createdAt.plusMinutes(20)));
    }

    @Test
    @DisplayName("same assignee reassignment is rejected")
    void rejectSameAssigneeReassignment() {
        var issue = assignedIssue();

        assertThrows(IllegalArgumentException.class,
                () -> issue.reassignAssignee(assignee, pl, createdAt.plusMinutes(20)));
    }

    @Test
    @DisplayName("same verifier change is rejected")
    void rejectSameVerifierChange() {
        var issue = assignedIssue();
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

        assertThrows(IllegalArgumentException.class,
                () -> issue.changeVerifier(verifier, pl, createdAt.plusMinutes(30)));
    }

    @Test
    @DisplayName("assignment changes require valid active participants")
    void rejectInvalidReassignmentParticipants() {
        var assignedIssue = assignedIssue();
        var fixedIssue = assignedIssue();
        fixedIssue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
        var inactiveDev = User.fromPersistence("dev3", "Dev Three", "hash", Role.DEV, true, null, null);
        inactiveDev.deactivate(createdAt.plusMinutes(1));

        assertThrows(IllegalArgumentException.class,
                () -> assignedIssue.reassignAssignee(verifier, pl, createdAt.plusMinutes(20)));
        assertThrows(IllegalArgumentException.class,
                () -> assignedIssue.reassignAssignee(inactiveDev, pl, createdAt.plusMinutes(20)));
        assertThrows(IllegalArgumentException.class,
                () -> fixedIssue.changeVerifier(assignee, pl, createdAt.plusMinutes(30)));
    }

    @Test
    @DisplayName("priority change records history")
    void changePriorityAndRecordHistory() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var changedAt = createdAt.plusMinutes(10);

        issue.changePriority(Priority.CRITICAL, pl, changedAt);

        assertEquals(Priority.CRITICAL, issue.getPriority());
        assertEquals(2, issue.getHistories().size());
        var history = issue.getHistories().get(1);
        assertEquals(ActionType.PRIORITY_CHANGED, history.getAction());
        assertEquals(Priority.MAJOR.name(), history.getPreviousValue());
        assertEquals(Priority.CRITICAL.name(), history.getNewValue());
        assertEquals(changedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("reopening resolved issue clears active assignment")
    void reopenResolvedIssueAndRecordHistory() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
        issue.resolve(verifier, "Verified", createdAt.plusMinutes(30));
        var changedAt = createdAt.plusMinutes(40);

        issue.reopen(pl, "Needs more work", changedAt);

        assertEquals(IssueStatus.REOPENED, issue.getStatus());
        assertNull(issue.getAssignee());
        assertNull(issue.getVerifier());
        assertSame(assignee, issue.getFixer());
        assertSame(verifier, issue.getResolver());
        assertEquals(6, issue.getHistories().size());
        var history = issue.getHistories().getLast();
        assertEquals(ActionType.STATUS_CHANGED, history.getAction());
        assertEquals(IssueStatus.RESOLVED.name(), history.getPreviousValue());
        assertEquals(IssueStatus.REOPENED.name(), history.getNewValue());
        assertEquals("Needs more work", history.getMessage());
        assertEquals(changedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("closing resolved issue clears active assignment")
    void closeResolvedIssueAndRecordHistory() {
        var issue = resolvedIssue();
        var closedAt = createdAt.plusMinutes(40);

        issue.close(pl, "Release completed", closedAt);

        assertEquals(IssueStatus.CLOSED, issue.getStatus());
        assertNull(issue.getAssignee());
        assertNull(issue.getVerifier());
        assertSame(assignee, issue.getFixer());
        assertSame(verifier, issue.getResolver());

        var history = issue.getHistories().getLast();
        assertEquals(ActionType.STATUS_CHANGED, history.getAction());
        assertEquals(IssueStatus.RESOLVED.name(), history.getPreviousValue());
        assertEquals(IssueStatus.CLOSED.name(), history.getNewValue());
        assertEquals("Release completed", history.getMessage());
        assertEquals(closedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("close needs resolved status, PL, and comment")
    void rejectInvalidClose() {
        var fixedIssue = assignedIssue();
        fixedIssue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

        assertThrows(IllegalStateException.class,
                () -> fixedIssue.close(pl, "Release completed", createdAt.plusMinutes(30)));
        assertThrows(IllegalArgumentException.class,
                () -> resolvedIssue().close(reporter, "Release completed", createdAt.plusMinutes(40)));
        assertThrows(IllegalArgumentException.class,
                () -> resolvedIssue().close(pl, "", createdAt.plusMinutes(40)));
    }

    @Test
    @DisplayName("reporter can edit title and description before assignment")
    void updateTitleAndDescriptionByReporter() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var changedAt = createdAt.plusMinutes(10);

        issue.updateTitleAndDescription("Login fixed", "Updated description", reporter, changedAt);

        assertEquals("Login fixed", issue.getTitle());
        assertEquals("Updated description", issue.getDescription());
        var history = issue.getHistories().getLast();
        assertEquals(ActionType.TITLE_DESCRIPTION_UPDATED, history.getAction());
        assertEquals("Login fails\nCannot log in", history.getPreviousValue());
        assertEquals("Login fixed\nUpdated description", history.getNewValue());
        assertEquals(changedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("reporter can edit reopened issue")
    void updateTitleAndDescriptionOnReopenedIssue() {
        var issue = resolvedIssue();
        issue.reopen(pl, "Needs more work", createdAt.plusMinutes(40));

        issue.updateTitleAndDescription("Revised title", "Revised desc", reporter, createdAt.plusMinutes(50));

        assertEquals("Revised title", issue.getTitle());
        assertEquals("Revised desc", issue.getDescription());
    }

    @Test
    @DisplayName("only reporter can edit title and description")
    void rejectUpdateTitleByNonReporter() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> issue.updateTitleAndDescription("New", "Desc", pl, createdAt.plusMinutes(10)));
    }

    @Test
    @DisplayName("assigned issue cannot be edited")
    void rejectUpdateTitleOnAssignedIssue() {
        var issue = assignedIssue();

        assertThrows(IllegalStateException.class,
                () -> issue.updateTitleAndDescription("New", "Desc", reporter, createdAt.plusMinutes(20)));
    }

    @Test
    @DisplayName("title and description cannot be blank")
    void rejectUpdateTitleWithBlankValues() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> issue.updateTitleAndDescription("", "Desc", reporter, createdAt.plusMinutes(10)));
        assertThrows(IllegalArgumentException.class,
                () -> issue.updateTitleAndDescription("Title", "", reporter, createdAt.plusMinutes(10)));
    }

    @Test
    @DisplayName("closed issue can be reopened")
    void reopenClosedIssue() {
        var issue = resolvedIssue();
        issue.close(pl, "Release completed", createdAt.plusMinutes(40));

        issue.reopen(pl, "Found regression", createdAt.plusMinutes(50));

        assertEquals(IssueStatus.REOPENED, issue.getStatus());
    }

    @Test
    @DisplayName("no-op priority and invalid reopen are rejected")
    void rejectNoOpChanges() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> issue.changePriority(Priority.MAJOR, pl, createdAt));
        assertThrows(IllegalStateException.class,
                () -> issue.reopen(pl, "Needs more work", createdAt));
    }

    @Test
    @DisplayName("adding a comment records COMMENTED history")
    void addCommentAndCommentedHistory() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var commentedAt = createdAt.plusMinutes(5);

        var comment = issue.addComment("C-1", "I will check it.", assignee, commentedAt);

        assertEquals("C-1", comment.getCommentId());
        assertEquals("I will check it.", comment.getContent());
        assertEquals(CommentPurpose.GENERAL, comment.getPurpose());
        assertSame(assignee, comment.getWriter());
        assertEquals(commentedAt, comment.getCreatedDate());
        assertEquals(commentedAt, comment.getUpdatedDate());
        assertEquals(1, issue.getComments().size());
        assertSame(comment, issue.getComments().getFirst());

        assertEquals(2, issue.getHistories().size());
        var history = issue.getHistories().get(1);
        assertEquals(ActionType.COMMENTED, history.getAction());
        assertNull(history.getPreviousValue());
        assertEquals("I will check it.", history.getNewValue());
        assertEquals("comment added", history.getMessage());
        assertSame(assignee, history.getChangedBy());
        assertEquals(commentedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("status-change comment keeps its purpose")
    void addStatusChangeReasonComment() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var commentedAt = createdAt.plusMinutes(5);

        var comment = issue.addComment(
                "C-1",
                "Fixed with regression tests.",
                assignee,
                commentedAt,
                CommentPurpose.STATUS_CHANGE
        );

        assertEquals(CommentPurpose.STATUS_CHANGE, comment.getPurpose());
        assertEquals(ActionType.COMMENTED, issue.getHistories().getLast().getAction());
        assertNull(issue.getHistories().getLast().getPreviousValue());
        assertEquals("Fixed with regression tests.", issue.getHistories().getLast().getNewValue());
        assertEquals("Fixed with regression tests.", issue.getHistories().getLast().getMessage());
    }

    @Test
    @DisplayName("comment deletion records previous content")
    void recordCommentDeletionHistory() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var comment = Comment.fromPersistence(
                11L,
                100L,
                assignee.getLoginId(),
                "Outdated investigation note",
                CommentPurpose.GENERAL,
                createdAt.plusMinutes(5),
                createdAt.plusMinutes(5));
        var deletedAt = createdAt.plusMinutes(30);

        issue.recordCommentDeletion(comment, assignee, deletedAt);

        var history = issue.getHistories().getLast();
        assertEquals(ActionType.COMMENTED, history.getAction());
        assertEquals("Outdated investigation note", history.getPreviousValue());
        assertNull(history.getNewValue());
        assertEquals("comment deleted", history.getMessage());
        assertSame(assignee, history.getChangedBy());
        assertEquals(deletedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("assignee marks an issue fixed")
    void markAssignedIssueFixed() {
        var issue = assignedIssue();
        var fixedAt = createdAt.plusMinutes(20);

        issue.markFixed(assignee, "Fix completed", fixedAt);

        assertSame(assignee, issue.getFixer());
        assertEquals(IssueStatus.FIXED, issue.getStatus());
        var history = issue.getHistories().getLast();
        assertEquals(ActionType.STATUS_CHANGED, history.getAction());
        assertEquals(IssueStatus.ASSIGNED.name(), history.getPreviousValue());
        assertEquals(IssueStatus.FIXED.name(), history.getNewValue());
        assertEquals("Fix completed", history.getMessage());
        assertSame(assignee, history.getChangedBy());
    }

    @Test
    @DisplayName("verifier resolves a fixed issue")
    void resolveFixedIssue() {
        var issue = assignedIssue();
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
        var resolvedAt = createdAt.plusMinutes(30);

        issue.resolve(verifier, "Verified", resolvedAt);

        assertSame(assignee, issue.getFixer());
        assertSame(verifier, issue.getResolver());
        assertNull(issue.getAssignee());
        assertNull(issue.getVerifier());
        assertNull(issue.assigneeId());
        assertNull(issue.verifierId());
        assertEquals(IssueStatus.RESOLVED, issue.getStatus());
        var history = issue.getHistories().getLast();
        assertEquals(ActionType.STATUS_CHANGED, history.getAction());
        assertEquals(IssueStatus.FIXED.name(), history.getPreviousValue());
        assertEquals(IssueStatus.RESOLVED.name(), history.getNewValue());
        assertEquals("Verified", history.getMessage());
        assertSame(verifier, history.getChangedBy());
    }

    @Test
    @DisplayName("only current assignee can mark fixed")
    void onlyCurrentAssigneeCanMarkFixed() {
        var issue = assignedIssue();

        assertThrows(IllegalArgumentException.class,
                () -> issue.markFixed(otherDeveloper, "Fix completed", createdAt.plusMinutes(20)));
    }

    @Test
    @DisplayName("only current verifier can resolve")
    void onlyCurrentVerifierCanResolve() {
        var issue = assignedIssue();
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

        assertThrows(IllegalArgumentException.class,
                () -> issue.resolve(otherTester, "Verified", createdAt.plusMinutes(30)));
    }

    @Test
    @DisplayName("fixer and resolver have the right roles")
    void rejectsWrongFixerAndResolverRoles() {
        var issue = assignedIssue();

        assertThrows(IllegalArgumentException.class,
                () -> issue.markFixed(verifier, "Fix completed", createdAt.plusMinutes(20)));
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
        assertThrows(IllegalArgumentException.class,
                () -> issue.resolve(assignee, "Verified", createdAt.plusMinutes(30)));
    }

    @Test
    @DisplayName("wrong status cannot move forward")
    void rejectsWrongSourceStatus() {
        var newIssue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter,
                createdAt);
        var issue = assignedIssue();

        assertThrows(IllegalStateException.class,
                () -> newIssue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20)));
        assertThrows(IllegalStateException.class,
                () -> issue.resolve(verifier, "Verified", createdAt.plusMinutes(20)));
    }

    @Test
    @DisplayName("status changes need a comment")
    void rejectsBlankStatusComment() {
        var issue = assignedIssue();

        assertThrows(IllegalArgumentException.class,
                () -> issue.markFixed(assignee, "", createdAt.plusMinutes(20)));
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
        assertThrows(IllegalArgumentException.class,
                () -> issue.resolve(verifier, " ", createdAt.plusMinutes(30)));
    }

    @Test
    @DisplayName("inactive users cannot fix or resolve")
    void rejectsInactiveFixerAndResolver() {
        var inactiveFixer = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
        var inactiveResolver = User.fromPersistence("tester3", "Tester Three", "hash", Role.TESTER, true, null, null);
        inactiveFixer.deactivate(createdAt.plusMinutes(1));
        inactiveResolver.deactivate(createdAt.plusMinutes(1));

        var issueForFixer = assignedIssue();
        assertThrows(IllegalArgumentException.class,
                () -> issueForFixer.markFixed(inactiveFixer, "Fix completed", createdAt.plusMinutes(20)));

        var issueForResolver = assignedIssue();
        issueForResolver.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
        assertThrows(IllegalArgumentException.class,
                () -> issueForResolver.resolve(inactiveResolver, "Verified", createdAt.plusMinutes(30)));
    }

    @Test
    @DisplayName("verifier can send a fixed issue back")
    void verifierSendsFixedIssueBack() {
        var issue = assignedIssue();
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

        issue.rejectFix(verifier, "Not fixed properly", createdAt.plusMinutes(30));

        assertEquals(IssueStatus.ASSIGNED, issue.getStatus());
        var history = issue.getHistories().getLast();
        assertEquals(ActionType.STATUS_CHANGED, history.getAction());
        assertEquals(IssueStatus.FIXED.name(), history.getPreviousValue());
        assertEquals(IssueStatus.ASSIGNED.name(), history.getNewValue());
    }

    @Test
    @DisplayName("only current verifier can reject a fix")
    void onlyCurrentVerifierCanRejectFix() {
        var issue = assignedIssue();
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

        assertThrows(IllegalArgumentException.class,
                () -> issue.rejectFix(otherTester, "Not fixed", createdAt.plusMinutes(30)));
    }

    @Test
    @DisplayName("only fixed issues can be sent back")
    void onlyFixedIssuesCanBeSentBack() {
        var issue = assignedIssue();

        assertThrows(IllegalStateException.class,
                () -> issue.rejectFix(verifier, "Not fixed", createdAt.plusMinutes(20)));
    }

    private Issue assignedIssue() {
        return assignedIssue("ISSUE-1");
    }

    private Issue assignedIssue(String issueId) {
        var issue = IssueFixtures.create(issueId, "Login fails", "Cannot log in", null, reporter, createdAt);
        issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));
        return issue;
    }

    private Issue resolvedIssue() {
        var issue = assignedIssue();
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
        issue.resolve(verifier, "Verified", createdAt.plusMinutes(30));
        return issue;
    }

    private Issue reopenedIssue() {
        var issue = resolvedIssue();
        issue.reopen(pl, "Needs more work", createdAt.plusMinutes(35));
        return issue;
    }

    private static IssueHistory findHistory(Issue issue, ActionType action) {
        return issue.getHistories().stream()
                .filter(history -> history.getAction() == action)
                .findFirst()
                .orElseThrow(() -> new AssertionError("History not found for action " + action));
    }
}
