package com.github.marcellokim.issuetracker.controller;

import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.NOW;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.PROJECT_ID;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.issueController;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.persistedIssue;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.unauthenticatedIssueController;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeCommentRepository;
import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueResult;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue controller")
class IssueControllerTest {

        @Test
        @DisplayName("issue screen can register, read and edit")
        void issueReadAndEditPaths() {
                User dev = user("dev1", Role.DEV);
                Issue issue = persistedIssue(1L, "ISSUE-1", dev);
                IssueController devController = issueController(dev, issue);

                IssueResult created = devController.registerIssue(PROJECT_ID, "New issue", "Registration path",
                                Priority.MAJOR);
                assertNotNull(created.issueId());
                assertEquals(IssueStatus.NEW, created.status());
                assertEquals("New issue", created.title());
                assertTrue(devController.canRegisterIssue(PROJECT_ID));

                IssueDetailResult detail = devController.viewIssueDetail(issue.id());
                assertEquals(issue.id(), detail.id());
                assertTrue(detail.availableActions().contains("UPDATE_ISSUE"));

                List<IssueSummary> simpleSearch = devController.searchIssues(PROJECT_ID, null, null, null);
                assertEquals(List.of(issue.id(), created.id()), simpleSearch.stream().map(IssueSummary::id).toList());

                List<IssueSummary> projectIssues = devController.viewProjectIssues(PROJECT_ID);
                assertEquals(List.of(issue.id(), created.id()), projectIssues.stream().map(IssueSummary::id).toList());

                IssueResult updated = devController.updateIssue(issue.id(), "Updated title", "Updated description");
                assertEquals("Updated title", updated.title());
        }

        @Test
        @DisplayName("search keeps the selected filters")
        void searchUsesFilters() {
                User dev = user("dev1", Role.DEV);
                User reporter = user("reporter1", Role.TESTER);
                User assignee = user("dev-owner", Role.DEV);
                User verifier = user("tester-owner", Role.TESTER);
                Issue matchingIssue = searchableIssue(10L, "Login filter bug",
                                reporter, assignee, verifier);
                Issue wrongAssignee = searchableIssue(11L, "Login filter bug",
                                reporter, user("other-dev", Role.DEV), verifier);
                Issue wrongVerifier = searchableIssue(12L, "Login filter bug",
                                reporter, assignee, user("other-tester", Role.TESTER));
                IssueController controller = issueController(dev, matchingIssue, wrongAssignee, wrongVerifier);

                List<IssueSummary> result = controller.searchIssues(
                                PROJECT_ID,
                                "login",
                                IssueStatus.ASSIGNED,
                                Priority.MAJOR,
                                reporter.getLoginId(),
                                assignee.getLoginId(),
                                verifier.getLoginId(),
                                NOW.minusDays(1),
                                NOW.plusDays(1));

                assertEquals(List.of(matchingIssue.id()), result.stream().map(IssueSummary::id).toList());
        }

        @Test
        @DisplayName("PL can change priority from the issue screen")
        void plChangesPriority() {
                User pl = user("pl1", Role.PL);
                Issue issue = persistedIssue(1L, "ISSUE-1", user("reporter1", Role.DEV));
                IssueController controller = issueController(pl, issue);

                IssueResult result = controller.changePriority(issue.id(), Priority.CRITICAL);

                assertEquals(Priority.CRITICAL, result.priority());
        }

        @Test
        @DisplayName("comment buttons use the logged in writer")
        void commentPathsUseCurrentUser() {
                User dev = user("dev1", Role.DEV);
                Issue issue = persistedIssue(1L, "ISSUE-1", dev);
                var comments = new FakeCommentRepository(Comment.fromPersistence(
                                100L,
                                issue.id(),
                                dev.getLoginId(),
                                "Original comment",
                                CommentPurpose.GENERAL,
                                NOW,
                                NOW));
                IssueController controller = issueController(dev, comments, issue);

                CommentResult added = controller.addComment(issue.id(), "Confirmed this bug");
                assertEquals("Confirmed this bug", added.content());
                assertEquals(dev.getLoginId(), added.writerLoginId());

                List<CommentResult> viewed = controller.viewComments(issue.id());
                assertEquals(List.of("100", "101"), viewed.stream().map(CommentResult::commentId).toList());

                assertTrue(controller.canUpdateComment(issue.id(), 100L));
                assertTrue(controller.canDeleteComment(issue.id(), 100L));

                CommentResult updated = controller.updateComment(issue.id(), 100L, "Edited comment");
                assertEquals("Edited comment", updated.content());

                controller.deleteComment(issue.id(), 100L);
                assertFalse(comments.findById(100L).isPresent());
        }

        @Test
        @DisplayName("PL can manage dependencies from the issue screen")
        void dependencyPathsForPl() {
                User pl = user("pl1", Role.PL);
                Issue blockingIssue = persistedIssue(1L, "ISSUE-1", user("reporter1", Role.DEV));
                Issue blockedIssue = persistedIssue(2L, "ISSUE-2", user("reporter2", Role.DEV));
                IssueController controller = issueController(pl, blockingIssue, blockedIssue);

                DependencyResult dependency = controller.addDependency(blockingIssue.id(), blockedIssue.id());
                assertNotNull(dependency.dependencyId());
                assertEquals(blockingIssue.id(), dependency.blockingIssueId());
                assertEquals(blockedIssue.id(), dependency.blockedIssueId());

                List<DependencyResult> dependencies = controller.viewProjectDependencies(PROJECT_ID);
                assertEquals(List.of(dependency.dependencyId()),
                                dependencies.stream().map(DependencyResult::dependencyId).toList());

                assertTrue(controller.viewAvailableActions(blockedIssue.id()).canAddDependency());

                controller.removeDependency(blockingIssue.id(), blockedIssue.id());
                assertEquals(List.of(), controller.viewProjectDependencies(PROJECT_ID));
        }

        @Test
        @DisplayName("issue writes need a logged in user")
        void writeNeedsLogin() {
                IssueController controller = unauthenticatedIssueController(
                                persistedIssue(1L, "ISSUE-1", user("dev1", Role.DEV)));

                assertThrows(SecurityException.class,
                                () -> controller.registerIssue(PROJECT_ID, "Bug", "desc", Priority.MAJOR));
                assertThrows(SecurityException.class,
                                () -> controller.addComment(1L, "comment"));
                assertThrows(SecurityException.class,
                                () -> controller.deleteComment(1L, 100L));
        }

        private static Issue searchableIssue(
                        long id,
                        String title,
                        User reporter,
                        User assignee,
                        User verifier) {
                LocalDateTime reportedAt = NOW.plusMinutes(id);
                return Issue.fromPersistence(Issue.persistedState(
                                PROJECT_ID,
                                title,
                                "Search filter path",
                                reporter)
                                .id(id)
                                .issueId("ISSUE-" + id)
                                .reportedDate(reportedAt)
                                .updatedAt(reportedAt)
                                .priority(Priority.MAJOR)
                                .status(IssueStatus.ASSIGNED)
                                .assignee(assignee)
                                .verifier(verifier));
        }
}
