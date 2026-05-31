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
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueHistoryRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import java.time.LocalDateTime;
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
        private final User inactiveDev = User.fromPersistence("dev-disabled", "Inactive Dev", "hash", Role.DEV, false,
                        now,
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
        @DisplayName("project member creates an issue")
        void memberCreatesIssue() {
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
        @DisplayName("missing priority becomes major")
        void missingPriorityBecomesMajor() {
                var service = service(new InMemoryIssueRepository());

                IssueResult result = service.registerIssue(PROJECT_ID, "Bug", "desc", null, dev.getLoginId());

                assertEquals(Priority.MAJOR, result.priority());
        }

        @Test
        @DisplayName("new issue needs basic input")
        void newIssueNeedsBasicInput() {
                var service = service(new InMemoryIssueRepository());
                String devLoginId = dev.getLoginId();

                assertThrows(IllegalArgumentException.class,
                                () -> service.registerIssue(0L, "Bug", "desc", Priority.MAJOR, devLoginId));
                assertThrows(IllegalArgumentException.class,
                                () -> service.registerIssue(PROJECT_ID, " ", "desc", Priority.MAJOR, devLoginId));
                assertThrows(IllegalArgumentException.class,
                                () -> service.registerIssue(PROJECT_ID, "Bug", null, Priority.MAJOR, devLoginId));
                assertThrows(IllegalArgumentException.class,
                                () -> service.registerIssue(PROJECT_ID, "Bug", "desc", Priority.MAJOR, " "));
        }

        @Test
        @DisplayName("project cannot reuse an issue title")
        void projectCannotReuseIssueTitle() {
                Issue deletedIssue = persistedIssue(
                                11L,
                                "ISSUE-11",
                                PROJECT_ID,
                                "Login bug",
                                IssueStatus.DELETED);
                var service = service(new InMemoryIssueRepository(deletedIssue));
                String devLoginId = dev.getLoginId();

                assertThrows(IllegalArgumentException.class,
                                () -> service.registerIssue(PROJECT_ID, "Login bug", "Cannot login", Priority.MAJOR,
                                                devLoginId));
        }

        @Test
        @DisplayName("another project may use the same title")
        void otherProjectMayUseSameTitle() {
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
        @DisplayName("missing project stops registration")
        void missingProjectStopsRegistration() {
                var service = service(new InMemoryIssueRepository());

                assertThrows(IllegalArgumentException.class,
                                () -> service.registerIssue(999L, "Bug", "desc", Priority.MAJOR, dev.getLoginId()));
        }

        @Test
        @DisplayName("missing reporter stops registration")
        void missingReporterStopsRegistration() {
                var service = service(new InMemoryIssueRepository());

                assertThrows(IllegalArgumentException.class,
                                () -> service.registerIssue(PROJECT_ID, "Bug", "desc", Priority.MAJOR, "unknown"));
        }

        @Test
        @DisplayName("outside member is not a reporter")
        void outsideMemberIsNotReporter() {
                var users = new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev)
                                .withProjectMembers(PROJECT_ID, pl.getLoginId());
                var service = service(
                                new InMemoryIssueRepository(),
                                new FakeIssueDependencyRepository(),
                                new FakeCommentRepository(),
                                users);

                assertThrows(SecurityException.class,
                                () -> service.registerIssue(PROJECT_ID, "Bug", "desc", Priority.MAJOR,
                                                dev.getLoginId()));
        }

        @Test
        @DisplayName("search stays inside one project")
        void searchStaysInsideProject() {
                Issue projectIssue = persistedIssue(11L, "ISSUE-11", PROJECT_ID, "Login bug", IssueStatus.NEW);
                Issue otherProjectIssue = persistedIssue(21L, "ISSUE-21", OTHER_PROJECT_ID, "Login bug",
                                IssueStatus.NEW);
                var service = service(new InMemoryIssueRepository(projectIssue, otherProjectIssue));

                List<IssueSummary> results = service.searchIssues(
                                PROJECT_ID,
                                "login",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                dev.getLoginId());

                assertEquals(1, results.size());
                assertEquals(projectIssue.id(), results.getFirst().id());
                assertEquals(projectIssue.getIssueId(), results.getFirst().issueId());
                assertEquals(projectIssue.projectId(), results.getFirst().projectId());
                assertEquals(projectIssue.title(), results.getFirst().title());
                assertThrows(SecurityException.class,
                                () -> service.searchIssues(
                                                PROJECT_ID,
                                                "login",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                admin.getLoginId()));
        }

        @Test
        @DisplayName("search can narrow by people")
        void searchNarrowsByPeople() {
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
                                null,
                                null,
                                dev.getLoginId());

                assertEquals(List.of(matchingIssue.id()), results.stream().map(IssueSummary::id).toList());
        }

        @Test
        @DisplayName("normal search skips deleted issues")
        void normalSearchSkipsDeletedIssues() {
                var issue = persistedIssue();
                var service = service(new InMemoryIssueRepository(issue));
                String devLoginId = dev.getLoginId();

                assertThrows(SecurityException.class,
                                () -> service.searchIssues(
                                                PROJECT_ID,
                                                null,
                                                IssueStatus.DELETED,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                devLoginId));
        }

        @Test
        @DisplayName("search range must be ordered")
        void searchRangeMustBeOrdered() {
                var issue = persistedIssue();
                var service = service(new InMemoryIssueRepository(issue));
                String devLoginId = dev.getLoginId();
                LocalDateTime reportedFrom = now.plusDays(1);

                assertThrows(IllegalArgumentException.class,
                                () -> service.searchIssues(
                                                PROJECT_ID,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                reportedFrom,
                                                now,
                                                devLoginId));
        }

        @Test
        @DisplayName("outside member cannot search the project")
        void outsideMemberCannotSearchProject() {
                var issue = persistedIssue();
                var users = new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev)
                                .withProjectMembers(PROJECT_ID, tester.getLoginId(), pl.getLoginId());
                var service = service(
                                new InMemoryIssueRepository(issue),
                                new FakeIssueDependencyRepository(),
                                new FakeCommentRepository(),
                                users);
                String devLoginId = dev.getLoginId();

                assertThrows(SecurityException.class,
                                () -> service.searchIssues(
                                                PROJECT_ID,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                devLoginId));
        }

        @Test
        @DisplayName("project roles see their issue list")
        void projectRolesSeeIssueList() {
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
                Issue completedHistoryOnlyIssue = Issue.fromPersistence(
                                Issue.persistedState(PROJECT_ID, "Completed history only issue", "Description 13", pl)
                                                .id(13L)
                                                .issueId("ISSUE-13")
                                                .reportedDate(now)
                                                .priority(Priority.MAJOR)
                                                .status(IssueStatus.CLOSED)
                                                .fixer(dev)
                                                .resolver(tester)
                                                .updatedAt(now));
                var users = new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev)
                                .withProjectMembers(PROJECT_ID, dev.getLoginId(), tester.getLoginId(), pl.getLoginId())
                                .withProjectMembers(OTHER_PROJECT_ID, dev.getLoginId());
                var service = service(
                                new InMemoryIssueRepository(
                                                reporterOnlyIssue,
                                                assignedDevIssue,
                                                completedHistoryOnlyIssue,
                                                otherProjectIssue),
                                new FakeIssueDependencyRepository(),
                                new FakeCommentRepository(),
                                users);

                List<IssueSummary> devResults = service.viewRelatedProjectIssues(PROJECT_ID, dev.getLoginId());
                List<IssueSummary> testerResults = service.viewRelatedProjectIssues(PROJECT_ID, tester.getLoginId());
                List<IssueSummary> plResults = service.viewRelatedProjectIssues(PROJECT_ID, pl.getLoginId());
                String adminLoginId = admin.getLoginId();

                assertEquals(List.of(reporterOnlyIssue.id(), assignedDevIssue.id()), devResults.stream()
                                .map(IssueSummary::id)
                                .toList());
                assertEquals(List.of(assignedDevIssue.id()), testerResults.stream()
                                .map(IssueSummary::id)
                                .toList());
                assertEquals(List.of(reporterOnlyIssue.id(), assignedDevIssue.id(), completedHistoryOnlyIssue.id()),
                                plResults.stream()
                                                .map(IssueSummary::id)
                                                .toList());
                assertThrows(SecurityException.class,
                                () -> service.viewRelatedProjectIssues(PROJECT_ID, adminLoginId));
        }

        @Test
        @DisplayName("outside member gets no project issues")
        void outsideMemberGetsNoProjectIssues() {
                var issue = persistedIssue();
                var users = new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev)
                                .withProjectMembers(PROJECT_ID, tester.getLoginId(), pl.getLoginId());
                var service = service(
                                new InMemoryIssueRepository(issue),
                                new FakeIssueDependencyRepository(),
                                new FakeCommentRepository(),
                                users);
                String devLoginId = dev.getLoginId();

                assertThrows(SecurityException.class,
                                () -> service.viewRelatedProjectIssues(PROJECT_ID, devLoginId));
        }

        @Test
        @DisplayName("issue detail loads related records")
        void issueDetailLoadsRelatedRecords() {
                var issue = persistedIssue();
                var blockingIssue = persistedIssue(99L, "ISSUE-99");
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
                                new InMemoryIssueRepository(issue, blockingIssue),
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
        }

        @Test
        @DisplayName("deleted issue takes deleted workflow")
        void deletedIssueTakesDeletedWorkflow() {
                var deletedIssue = persistedIssue(ISSUE_ID, "ISSUE-1", PROJECT_ID, "Deleted issue",
                                IssueStatus.DELETED);
                var service = service(new InMemoryIssueRepository(deletedIssue));

                SecurityException exception = assertThrows(SecurityException.class,
                                () -> service.viewIssueDetail(ISSUE_ID, dev.getLoginId()));

                assertEquals("Deleted issues must be managed through deleted issue workflow.", exception.getMessage());
        }

        @Test
        @DisplayName("reporter edits a new issue")
        void reporterEditsNewIssue() {
                var issue = persistedIssue();
                var service = service(new InMemoryIssueRepository(issue));

                IssueResult result = service.updateIssue(ISSUE_ID, "Updated title", "Updated description",
                                dev.getLoginId());

                assertEquals("Updated title", result.title());
                assertEquals("Updated description", result.description());
                assertEquals(ActionType.TITLE_DESCRIPTION_UPDATED, issue.getHistories().getLast().actionType());
        }

        @Test
        @DisplayName("reporter can keep the same title")
        void reporterKeepsSameTitle() {
                var issue = persistedIssue(ISSUE_ID, "ISSUE-1", PROJECT_ID, "Issue 1", IssueStatus.NEW);
                var service = service(new InMemoryIssueRepository(issue));

                IssueResult result = service.updateIssue(ISSUE_ID, "Issue 1", "Updated description", dev.getLoginId());

                assertEquals("Issue 1", result.title());
                assertEquals("Updated description", result.description());
        }

        @Test
        @DisplayName("edit cannot take another issue title")
        void editCannotTakeUsedTitle() {
                var issue = persistedIssue(ISSUE_ID, "ISSUE-1", PROJECT_ID, "Editable title", IssueStatus.NEW);
                var deletedIssue = persistedIssue(11L, "ISSUE-11", PROJECT_ID, "Duplicated title", IssueStatus.DELETED);
                var service = service(new InMemoryIssueRepository(issue, deletedIssue));

                assertThrows(IllegalArgumentException.class,
                                () -> service.updateIssue(ISSUE_ID, "Duplicated title", "Updated description",
                                                dev.getLoginId()));
        }

        @Test
        @DisplayName("same title is fine in another project")
        void sameTitleIsFineElsewhere() {
                var issue = persistedIssue(ISSUE_ID, "ISSUE-1", PROJECT_ID, "Editable title", IssueStatus.NEW);
                var otherProjectIssue = persistedIssue(
                                21L,
                                "ISSUE-21",
                                OTHER_PROJECT_ID,
                                "Shared title",
                                IssueStatus.NEW);
                var service = service(new InMemoryIssueRepository(issue, otherProjectIssue));

                IssueResult result = service.updateIssue(ISSUE_ID, "Shared title", "Updated description",
                                dev.getLoginId());

                assertEquals("Shared title", result.title());
        }

        @Test
        @DisplayName("deleted issue is not edited here")
        void deletedIssueIsNotEditedHere() {
                var deletedIssue = persistedIssue(ISSUE_ID, "ISSUE-1", PROJECT_ID, "Deleted issue",
                                IssueStatus.DELETED);
                var service = service(new InMemoryIssueRepository(deletedIssue));

                assertThrows(SecurityException.class,
                                () -> service.updateIssue(ISSUE_ID, "Updated title", "Updated description",
                                                dev.getLoginId()));
        }

        @Test
        @DisplayName("only reporter edits title and description")
        void onlyReporterEditsIssueText() {
                var issue = persistedIssue();
                var service = service(new InMemoryIssueRepository(issue));

                assertThrows(SecurityException.class,
                                () -> service.updateIssue(ISSUE_ID, "Updated title", "Updated description",
                                                tester.getLoginId()));
        }

        @Test
        @DisplayName("editing stays with open issues")
        void editingStaysWithOpenIssues() {
                for (IssueStatus status : List.of(
                                IssueStatus.ASSIGNED,
                                IssueStatus.FIXED,
                                IssueStatus.RESOLVED,
                                IssueStatus.CLOSED)) {
                        var issue = persistedIssue(ISSUE_ID, "ISSUE-1", PROJECT_ID, "Issue " + status, status);
                        var service = service(new InMemoryIssueRepository(issue));
                        String title = "Updated " + status;
                        String devLoginId = dev.getLoginId();

                        assertThrows(SecurityException.class,
                                        () -> service.updateIssue(
                                                        ISSUE_ID,
                                                        title,
                                                        "Updated description",
                                                        devLoginId));
                }
        }

        @Test
        @DisplayName("project PL changes priority")
        void plChangesPriority() {
                var issue = persistedIssue();
                var service = service(new InMemoryIssueRepository(issue));

                IssueResult result = service.changePriority(ISSUE_ID, Priority.CRITICAL, pl.getLoginId());

                assertEquals(Priority.CRITICAL, result.priority());
                assertEquals(ActionType.PRIORITY_CHANGED, issue.getHistories().getLast().actionType());
                assertEquals(Priority.MAJOR.name(), issue.getHistories().getLast().previousValue());
                assertEquals(Priority.CRITICAL.name(), issue.getHistories().getLast().newValue());
        }

        @Test
        @DisplayName("deleted issue priority is left alone")
        void deletedPriorityLeftAlone() {
                var deletedIssue = persistedIssue(ISSUE_ID, "ISSUE-1", PROJECT_ID, "Deleted issue",
                                IssueStatus.DELETED);
                var service = service(new InMemoryIssueRepository(deletedIssue));

                assertThrows(SecurityException.class,
                                () -> service.changePriority(ISSUE_ID, Priority.CRITICAL, pl.getLoginId()));
        }

        @Test
        @DisplayName("non-PL cannot change priority")
        void nonPlCannotChangePriority() {
                var issue = persistedIssue();
                var service = service(new InMemoryIssueRepository(issue));

                assertThrows(SecurityException.class,
                                () -> service.changePriority(ISSUE_ID, Priority.CRITICAL, dev.getLoginId()));
        }

        @Test
        @DisplayName("other project PL cannot touch priority")
        void otherPlCannotTouchPriority() {
                var issue = persistedIssue();
                var users = new InMemoryUserRepository(dev, tester, pl, otherProjectPl, admin, inactiveDev)
                                .withProjectMembers(PROJECT_ID, pl.getLoginId())
                                .withProjectMembers(OTHER_PROJECT_ID, otherProjectPl.getLoginId());
                var service = service(
                                new InMemoryIssueRepository(issue),
                                new FakeIssueDependencyRepository(),
                                new FakeCommentRepository(),
                                users);
                String otherProjectPlLoginId = otherProjectPl.getLoginId();

                assertThrows(SecurityException.class,
                                () -> service.changePriority(ISSUE_ID, Priority.CRITICAL,
                                                otherProjectPlLoginId));
        }

        @Test
        @DisplayName("same priority is not a change")
        void samePriorityIsNotChange() {
                var issue = persistedIssue();
                var service = service(new InMemoryIssueRepository(issue));
                String plLoginId = pl.getLoginId();

                assertThrows(IllegalArgumentException.class,
                                () -> service.changePriority(ISSUE_ID, Priority.MAJOR, plLoginId));
        }

        @Test
        @DisplayName("project member writes a comment")
        void memberWritesComment() {
                var issue = persistedIssue();
                var comments = new FakeCommentRepository();
                var histories = new FakeIssueHistoryRepository();
                var service = service(
                                new InMemoryIssueRepository(issue),
                                new FakeIssueDependencyRepository(),
                                comments,
                                histories,
                                new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev));

                CommentResult result = service.addComment(ISSUE_ID, "Looks like a real bug", dev.getLoginId());

                assertEquals(String.valueOf(COMMENT_ID), result.commentId());
                assertEquals(CommentPurpose.GENERAL, result.purpose());
                assertEquals("Looks like a real bug", result.content());
                assertEquals(dev.getLoginId(), result.writer().loginId());
                assertNotNull(result.createdDate());
                assertEquals("Looks like a real bug", comments.findById(COMMENT_ID).orElseThrow().content());
                IssueHistory history = histories.findByIssueId(ISSUE_ID).getFirst();
                assertEquals(ActionType.COMMENTED, history.actionType());
                assertNull(history.previousValue());
                assertEquals("Looks like a real bug", history.newValue());
                assertEquals("comment added", history.message());
        }

        @Test
        @DisplayName("comment work does not touch issue updatedAt")
        void commentDoesNotTouchIssueUpdatedAt() {
                var issue = persistedIssue();
                var issues = new InMemoryIssueRepository(issue);
                var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
                var service = service(issues, comments);
                LocalDateTime originalUpdatedAt = issue.updatedAt();

                service.addComment(ISSUE_ID, "Additional investigation note", dev.getLoginId());
                service.updateComment(ISSUE_ID, COMMENT_ID, "Updated investigation note", dev.getLoginId());
                service.deleteComment(ISSUE_ID, COMMENT_ID, dev.getLoginId());

                assertEquals(originalUpdatedAt, issues.findById(ISSUE_ID).orElseThrow().updatedAt());
        }

        @Test
        @DisplayName("deleted issue comments are locked")
        void deletedIssueCommentsAreLockedEarly() {
                var deletedIssue = persistedIssue(ISSUE_ID, "ISSUE-1", PROJECT_ID, "Deleted issue",
                                IssueStatus.DELETED);
                var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
                var service = service(new InMemoryIssueRepository(deletedIssue), comments);

                assertThrows(SecurityException.class,
                                () -> service.addComment(ISSUE_ID, "comment", dev.getLoginId()));
                assertThrows(SecurityException.class,
                                () -> service.viewComments(ISSUE_ID, dev.getLoginId()));
        }

        @Test
        @DisplayName("project member reads comments")
        void memberReadsComments() {
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
        @DisplayName("saved comment id is returned")
        void savedCommentIdIsReturned() {
                var issue = persistedIssue();
                var comments = new FakeCommentRepository();
                var service = service(new InMemoryIssueRepository(issue), comments);

                CommentResult result = service.addComment(ISSUE_ID, "Looks like a real bug", dev.getLoginId());

                assertEquals(CommentPurpose.GENERAL, result.purpose());
                assertEquals(String.valueOf(COMMENT_ID), result.commentId());
        }

        @Test
        @DisplayName("admin and inactive users cannot comment")
        void adminAndInactiveUsersCannotComment() {
                var issue = persistedIssue();
                var service = service(new InMemoryIssueRepository(issue));

                assertThrows(SecurityException.class,
                                () -> service.addComment(ISSUE_ID, "admin comment", admin.getLoginId()));
                assertThrows(SecurityException.class,
                                () -> service.addComment(ISSUE_ID, "inactive comment", inactiveDev.getLoginId()));
        }

        @Test
        @DisplayName("missing issue stops comment")
        void missingIssueStopsComment() {
                var service = service(new InMemoryIssueRepository());

                assertThrows(IllegalArgumentException.class,
                                () -> service.addComment(999L, "comment", dev.getLoginId()));
        }

        @Test
        @DisplayName("unknown writer stops comment")
        void unknownWriterStopsComment() {
                var issue = persistedIssue();
                var service = service(new InMemoryIssueRepository(issue));

                assertThrows(IllegalArgumentException.class,
                                () -> service.addComment(ISSUE_ID, "comment", "unknown"));
        }

        @Test
        @DisplayName("comment needs text")
        void commentNeedsText() {
                var issue = persistedIssue();
                var service = service(new InMemoryIssueRepository(issue));

                assertThrows(IllegalArgumentException.class,
                                () -> service.addComment(ISSUE_ID, "", dev.getLoginId()));
        }

        @Test
        @DisplayName("outside member cannot comment")
        void outsideMemberCannotComment() {
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
        @DisplayName("PL adds a dependency")
        void plAddsDependency() {
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
        @DisplayName("deleted issues stay out of dependencies")
        void deletedIssuesStayOutOfDependencies() {
                var deletedBlockingIssue = persistedIssue(1L, "ISSUE-1", PROJECT_ID, "Deleted blocking",
                                IssueStatus.DELETED);
                var blockedIssue = persistedIssue(2L, "ISSUE-2");
                var service = service(new InMemoryIssueRepository(deletedBlockingIssue, blockedIssue));

                assertThrows(SecurityException.class,
                                () -> service.addDependency(1L, 2L, pl.getLoginId()));
        }

        @Test
        @DisplayName("completed issue cannot be blocked")
        void completedIssueCannotBeBlocked() {
                var blockingIssue = persistedIssue(1L, "ISSUE-1");
                var resolvedBlockedIssue = persistedIssue(2L, "ISSUE-2", PROJECT_ID, "Resolved blocked",
                                IssueStatus.RESOLVED);
                var closedBlockedIssue = persistedIssue(3L, "ISSUE-3", PROJECT_ID, "Closed blocked",
                                IssueStatus.CLOSED);
                var service = service(new InMemoryIssueRepository(
                                blockingIssue,
                                resolvedBlockedIssue,
                                closedBlockedIssue));
                String plLoginId = pl.getLoginId();

                assertThrows(IllegalStateException.class,
                                () -> service.addDependency(1L, 2L, plLoginId));
                assertThrows(IllegalStateException.class,
                                () -> service.addDependency(1L, 3L, plLoginId));
        }

        @Test
        @DisplayName("issue cannot block itself")
        void issueCannotBlockItself() {
                var issue = persistedIssue(1L, "ISSUE-1");
                var service = service(new InMemoryIssueRepository(issue));

                assertThrows(IllegalArgumentException.class,
                                () -> service.addDependency(1L, 1L, pl.getLoginId()));
        }

        @Test
        @DisplayName("same dependency is not added twice")
        void sameDependencyIsNotAddedTwice() {
                var issueA = persistedIssue(1L, "ISSUE-1");
                var issueB = persistedIssue(2L, "ISSUE-2");
                var deps = new FakeIssueDependencyRepository();
                var service = service(new InMemoryIssueRepository(issueA, issueB), deps);

                service.addDependency(1L, 2L, pl.getLoginId());

                assertThrows(IllegalArgumentException.class,
                                () -> service.addDependency(1L, 2L, pl.getLoginId()));
        }

        @Test
        @DisplayName("two-issue cycle is blocked")
        void twoIssueCycleIsBlocked() {
                var issueA = persistedIssue(1L, "ISSUE-1");
                var issueB = persistedIssue(2L, "ISSUE-2");
                var deps = new FakeIssueDependencyRepository();
                var service = service(new InMemoryIssueRepository(issueA, issueB), deps);

                service.addDependency(1L, 2L, pl.getLoginId());

                assertThrows(IllegalArgumentException.class,
                                () -> service.addDependency(2L, 1L, pl.getLoginId()));
        }

        @Test
        @DisplayName("longer cycle is blocked")
        void longerCycleIsBlocked() {
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
        @DisplayName("PL removes a dependency")
        void plRemovesDependency() {
                var issueA = persistedIssue(1L, "ISSUE-1");
                var issueB = persistedIssue(2L, "ISSUE-2");
                var deps = new FakeIssueDependencyRepository();
                var service = service(new InMemoryIssueRepository(issueA, issueB), deps);

                DependencyResult result = service.addDependency(1L, 2L, pl.getLoginId());

                service.removeDependency(result.blockingIssueId(), result.blockedIssueId(), pl.getLoginId());

                assertFalse(deps.findByDependencyId(result.dependencyId()).isPresent());
                var history = issueB.getHistories().getLast();
                assertEquals(ActionType.DEPENDENCY_CHANGED, history.actionType());
                assertEquals(result.dependencyId(), history.previousValue());
                assertNull(history.newValue());
                assertEquals("Dependency removed", history.message());
        }

        @Test
        @DisplayName("other project PL cannot remove dependency")
        void otherProjectPlCannotRemoveDependency() {
                var issueA = persistedIssue(1L, "ISSUE-1");
                var issueB = persistedIssue(2L, "ISSUE-2");
                var deps = new FakeIssueDependencyRepository();
                var users = new InMemoryUserRepository(dev, tester, pl, otherProjectPl, admin, inactiveDev)
                                .withProjectMembers(PROJECT_ID, pl.getLoginId())
                                .withProjectMembers(OTHER_PROJECT_ID, otherProjectPl.getLoginId());
                var service = service(new InMemoryIssueRepository(issueA, issueB), deps,
                                new FakeCommentRepository(), users);
                DependencyResult result = service.addDependency(1L, 2L, pl.getLoginId());
                long blockingIssueId = result.blockingIssueId();
                long blockedIssueId = result.blockedIssueId();
                String otherProjectPlLoginId = otherProjectPl.getLoginId();

                assertThrows(SecurityException.class,
                                () -> service.removeDependency(
                                                blockingIssueId,
                                                blockedIssueId,
                                                otherProjectPlLoginId));
                assertTrue(deps.findByDependencyId(result.dependencyId()).isPresent());
        }

        @Test
        @DisplayName("deleted issue dependency stays untouched")
        void deletedIssueDependencyStaysUntouched() {
                var issueA = persistedIssue(1L, "ISSUE-1");
                var deletedIssueB = persistedIssue(2L, "ISSUE-2", PROJECT_ID, "Deleted blocked", IssueStatus.DELETED);
                var deps = new FakeIssueDependencyRepository();
                deps.addFixture(IssueDependency.fromPersistence(1L, issueA.id(), deletedIssueB.id(), now));
                var service = service(new InMemoryIssueRepository(issueA, deletedIssueB), deps);

                assertThrows(SecurityException.class,
                                () -> service.removeDependency(issueA.id(), deletedIssueB.id(), pl.getLoginId()));
        }

        @Test
        @DisplayName("missing dependency is not removed")
        void missingDependencyIsNotRemoved() {
                var service = service(new InMemoryIssueRepository());

                assertThrows(IllegalArgumentException.class,
                                () -> service.removeDependency(999L, 998L, pl.getLoginId()));
        }

        @Test
        @DisplayName("non-PL cannot add dependency")
        void nonPlCannotAddDependency() {
                var issueA = persistedIssue(1L, "ISSUE-1");
                var issueB = persistedIssue(2L, "ISSUE-2");
                var service = service(new InMemoryIssueRepository(issueA, issueB));

                assertThrows(SecurityException.class,
                                () -> service.addDependency(1L, 2L, dev.getLoginId()));
        }

        @Test
        @DisplayName("PL must be on the blocked issue project")
        void plMustBeOnBlockedProject() {
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
        @DisplayName("dependency stays inside one project")
        void dependencyStaysInsideOneProject() {
                var blockingIssue = persistedIssue(1L, "ISSUE-1", OTHER_PROJECT_ID);
                var blockedIssue = persistedIssue(2L, "ISSUE-2", PROJECT_ID);
                var deps = new FakeIssueDependencyRepository();
                var users = new InMemoryUserRepository(dev, tester, pl, otherProjectPl, admin, inactiveDev)
                                .withProjectMembers(PROJECT_ID, pl.getLoginId())
                                .withProjectMembers(OTHER_PROJECT_ID, otherProjectPl.getLoginId());
                var service = service(new InMemoryIssueRepository(blockingIssue, blockedIssue), deps,
                                new FakeCommentRepository(), users);

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                () -> service.addDependency(1L, 2L, pl.getLoginId()));
                assertEquals("Dependencies are allowed only within the same project.", exception.getMessage());
                assertFalse(deps.existsByPair(1L, 2L));
                assertEquals(0, blockingIssue.getHistories().size());
                assertEquals(0, blockedIssue.getHistories().size());
        }

        @Test
        @DisplayName("project member views dependencies")
        void memberViewsDependencies() {
                var issueA = persistedIssue(1L, "ISSUE-1");
                var issueB = persistedIssue(2L, "ISSUE-2");
                var deps = new FakeIssueDependencyRepository();
                deps.addFixture(IssueDependency.fromPersistence(1L, "dep-1", issueA.id(), issueB.id(), now));
                var users = new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev)
                                .withProjectMembers(PROJECT_ID, dev.getLoginId());
                var service = service(new InMemoryIssueRepository(issueA, issueB), deps, new FakeCommentRepository(),
                                users);

                List<DependencyResult> results = service.viewProjectDependencies(PROJECT_ID, dev.getLoginId());

                assertEquals(1, results.size());
                assertEquals("dep-1", results.getFirst().dependencyId());
        }

        @Test
        @DisplayName("outside member cannot view dependencies")
        void outsideMemberCannotViewDependencies() {
                var issueA = persistedIssue(1L, "ISSUE-1");
                var issueB = persistedIssue(2L, "ISSUE-2");
                var deps = new FakeIssueDependencyRepository();
                deps.addFixture(IssueDependency.fromPersistence(1L, "dep-1", issueA.id(), issueB.id(), now));
                var users = new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev)
                                .withProjectMembers(OTHER_PROJECT_ID, dev.getLoginId());
                var service = service(new InMemoryIssueRepository(issueA, issueB), deps, new FakeCommentRepository(),
                                users);

                assertThrows(SecurityException.class,
                                () -> service.viewProjectDependencies(PROJECT_ID, dev.getLoginId()));
        }

        @Test
        @DisplayName("writer deletes a general comment")
        void writerDeletesGeneralComment() {
                var issue = persistedIssue();
                var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
                var service = service(new InMemoryIssueRepository(issue), comments);

                service.deleteComment(ISSUE_ID, COMMENT_ID, dev.getLoginId());

                assertFalse(comments.findById(COMMENT_ID).isPresent());
                var history = issue.getHistories().getLast();
                assertEquals(ActionType.COMMENTED, history.actionType());
                assertEquals("Outdated investigation note", history.previousValue());
                assertNull(history.newValue());
                assertEquals("comment deleted", history.message());
                assertEquals(dev.getLoginId(), history.changedById());
        }

        @Test
        @DisplayName("deleted issue comments are locked")
        void deletedIssueCommentsAreLocked() {
                var deletedIssue = persistedIssue(ISSUE_ID, "ISSUE-1", PROJECT_ID, "Deleted issue",
                                IssueStatus.DELETED);
                var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
                var service = service(new InMemoryIssueRepository(deletedIssue), comments);

                assertThrows(SecurityException.class,
                                () -> service.updateComment(ISSUE_ID, COMMENT_ID, "Updated", dev.getLoginId()));
                assertThrows(SecurityException.class,
                                () -> service.deleteComment(ISSUE_ID, COMMENT_ID, dev.getLoginId()));
                assertTrue(comments.findById(COMMENT_ID).isPresent());
        }

        @Test
        @DisplayName("writer updates own comment")
        void writerUpdatesOwnComment() {
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
                assertEquals("comment updated", history.message());
                assertEquals(dev.getLoginId(), history.changedById());
        }

        @Test
        @DisplayName("comment update adds a new history row")
        void commentUpdateAddsHistory() {
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
                IssueHistory appendedHistory = issueHistories.get(1);
                assertEquals(ActionType.COMMENTED, appendedHistory.actionType());
                assertEquals("Outdated investigation note", appendedHistory.previousValue());
                assertEquals("Updated investigation note", appendedHistory.newValue());
                assertEquals("comment updated", appendedHistory.message());
        }

        @Test
        @DisplayName("same comment text is not an update")
        void sameCommentTextIsIgnored() {
                var issue = persistedIssue();
                var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
                var histories = new FakeIssueHistoryRepository();
                var service = service(new InMemoryIssueRepository(issue), new FakeIssueDependencyRepository(), comments,
                                histories, new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev));
                String devLoginId = dev.getLoginId();

                assertThrows(IllegalArgumentException.class,
                                () -> service.updateComment(
                                                ISSUE_ID,
                                                COMMENT_ID,
                                                "Outdated investigation note",
                                                devLoginId));
                assertEquals("Outdated investigation note", comments.findById(COMMENT_ID).orElseThrow().content());
                assertTrue(histories.findByIssueId(ISSUE_ID).isEmpty());
        }

        @Test
        @DisplayName("status-change comment is read-only")
        void statusChangeCommentIsReadOnly() {
                var issue = persistedIssue();
                var comments = new FakeCommentRepository(
                                comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.STATUS_CHANGE));
                var histories = new FakeIssueHistoryRepository();
                var service = service(new InMemoryIssueRepository(issue), new FakeIssueDependencyRepository(), comments,
                                histories, new InMemoryUserRepository(dev, tester, pl, admin, inactiveDev));

                assertThrows(SecurityException.class, () -> service.updateComment(
                                ISSUE_ID,
                                COMMENT_ID,
                                "Updated status transition reason",
                                dev.getLoginId()));

                assertEquals("Outdated investigation note", comments.findById(COMMENT_ID).orElseThrow().content());
                assertTrue(histories.findByIssueId(ISSUE_ID).isEmpty());
        }

        @Test
        @DisplayName("non-writer cannot update comment")
        void nonWriterCannotUpdateComment() {
                var issue = persistedIssue();
                var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
                var service = service(new InMemoryIssueRepository(issue), comments);

                assertThrows(SecurityException.class,
                                () -> service.updateComment(ISSUE_ID, COMMENT_ID, "Updated", tester.getLoginId()));
                assertEquals("Outdated investigation note", comments.findById(COMMENT_ID).orElseThrow().content());
        }

        @Test
        @DisplayName("writer outside the project cannot update")
        void writerOutsideProjectCannotUpdate() {
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
        @DisplayName("non-writer cannot delete comment")
        void nonWriterCannotDeleteComment() {
                var issue = persistedIssue();
                var comments = new FakeCommentRepository(comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.GENERAL));
                var service = service(new InMemoryIssueRepository(issue), comments);

                assertThrows(SecurityException.class,
                                () -> service.deleteComment(ISSUE_ID, COMMENT_ID, tester.getLoginId()));
                assertNotNull(comments.findById(COMMENT_ID).orElseThrow());
        }

        @Test
        @DisplayName("writer outside the project cannot delete")
        void writerOutsideProjectCannotDelete() {
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
        @DisplayName("status-change comment is not deleted")
        void statusChangeCommentIsNotDeleted() {
                var issue = persistedIssue();
                var comments = new FakeCommentRepository(
                                comment(COMMENT_ID, ISSUE_ID, dev, CommentPurpose.STATUS_CHANGE));
                var service = service(new InMemoryIssueRepository(issue), comments);

                assertThrows(SecurityException.class,
                                () -> service.deleteComment(ISSUE_ID, COMMENT_ID, dev.getLoginId()));
                assertNotNull(comments.findById(COMMENT_ID).orElseThrow());
        }

        @Test
        @DisplayName("comment must belong to the issue")
        void commentMustBelongToIssue() {
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
                comments.recordHistoriesIn(histories);
                return new IssueService(
                                new InMemoryProjectRepository(project, otherProject),
                                issues,
                                dependencies,
                                comments,
                                histories,
                                users,
                                new PermissionPolicy(),
                                java.time.LocalDateTime::now);
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

        private static final class FakeCommentRepository implements CommentRepository {

                private final Map<Long, Comment> comments = new LinkedHashMap<>();
                private FakeIssueHistoryRepository histories;
                private long nextId = COMMENT_ID;

                private FakeCommentRepository(Comment... comments) {
                        for (Comment comment : comments) {
                                this.comments.put(comment.id(), comment);
                        }
                }

                private void recordHistoriesIn(FakeIssueHistoryRepository histories) {
                        this.histories = histories;
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

                private Comment saveInternal(Comment comment) {
                        if (comment.id() != 0L) {
                                comments.put(comment.id(), comment);
                                nextId = Math.max(nextId, comment.id() + 1L);
                                return comment;
                        }
                        Comment saved = Comment.fromPersistence(
                                        nextId++,
                                        comment.issueId(),
                                        comment.writerId(),
                                        comment.content(),
                                        comment.purpose(),
                                        comment.createdDate(),
                                        comment.updatedDate());
                        comments.put(saved.id(), saved);
                        return saved;
                }

                @Override
                public Comment saveCommentAndRecordHistory(Comment comment, IssueHistory history) {
                        Comment saved = saveInternal(comment);
                        if (histories != null) {
                                histories.save(history);
                        }
                        return saved;
                }

                private void deleteGeneralInternal(long issueId, long commentId, String writerLoginId) {
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

                @Override
                public void deleteGeneralByIdAndRecordIssueChange(
                                long issueId,
                                long commentId,
                                String writerLoginId,
                                IssueHistory history) {
                        deleteGeneralInternal(issueId, commentId, writerLoginId);
                        if (histories != null) {
                                histories.save(history);
                        }
                }
        }
}
