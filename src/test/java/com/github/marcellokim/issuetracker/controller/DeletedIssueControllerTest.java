package com.github.marcellokim.issuetracker.controller;

import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.PROJECT_ID;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.anonymousAuth;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.authenticated;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.issue;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.project;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.AuthFixture;
import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeIssueRepository;
import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeUserRepository;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.service.DeletedIssueService;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Deleted issue controller")
class DeletedIssueControllerTest {

        @Test
        @DisplayName("PL can move issues in and out of deleted list")
        void plManagesDeletedIssues() {
                AuthFixture auth = authenticated(Role.PL);
                auth.users().attachProjects(new InMemoryProjectRepository(project(PROJECT_ID))
                                .withParticipant(PROJECT_ID, auth.user().getLoginId()));
                Issue activeIssue = issue(101L, PROJECT_ID, IssueStatus.NEW);
                Issue deletedIssue = issue(102L, PROJECT_ID, IssueStatus.DELETED);
                Issue purgeTarget = issue(103L, PROJECT_ID, IssueStatus.DELETED);
                var issues = new FakeIssueRepository(activeIssue, deletedIssue, purgeTarget);
                DeletedIssueController controller = new DeletedIssueController(
                                auth.service(),
                                new DeletedIssueService(issues, issues, auth.users(), new PermissionPolicy(),
                                                LocalDateTime::now));

                List<IssueSummary> deletedIssues = controller.viewDeletedIssues(PROJECT_ID);
                IssueSummary softDeleted = controller.deleteIssue(activeIssue.id(), "remove from demo");
                IssueSummary restored = controller.restoreIssue(deletedIssue.id(), "restore for demo");
                int purged = controller.purgeOverflow(PROJECT_ID);
                controller.purgeDeletedIssue(purgeTarget.id());

                assertEquals(List.of(deletedIssue.id(), purgeTarget.id()),
                                deletedIssues.stream().map(IssueSummary::id).toList());
                assertEquals(IssueStatus.DELETED, softDeleted.status());
                assertEquals(IssueStatus.NEW, restored.status());
                assertEquals("pl", issues.lastChangedBy);
                assertEquals("restore for demo", issues.lastRestoreMessage);
                assertEquals(2, purged);
                assertEquals(30, issues.lastPurgeLimit);
                assertEquals(purgeTarget.id(), issues.lastPurgedIssueId);
                assertTrue(issues.findById(purgeTarget.id()).isEmpty());
        }

        @Test
        @DisplayName("deleted issue paths reject the wrong user")
        void rejectsWrongUser() {
                var issues = new FakeIssueRepository(issue(101L, PROJECT_ID, IssueStatus.NEW));
                DeletedIssueController anonymousController = new DeletedIssueController(
                                anonymousAuth(),
                                new DeletedIssueService(issues, issues, new FakeUserRepository(),
                                                new PermissionPolicy(), LocalDateTime::now));
                assertThrows(SecurityException.class, () -> anonymousController.viewDeletedIssues(PROJECT_ID));

                DeletedIssueController adminController = new DeletedIssueController(
                                authenticated(Role.ADMIN).service(),
                                new DeletedIssueService(issues, issues, new FakeUserRepository(),
                                                new PermissionPolicy(), LocalDateTime::now));
                SecurityException adminFailure = assertThrows(SecurityException.class,
                                () -> adminController.deleteIssue(101L, "admin cannot delete"));
                assertEquals("Only PL can manage deleted issues.", adminFailure.getMessage());

                DeletedIssueController plController = new DeletedIssueController(
                                authenticated(Role.PL).service(),
                                new DeletedIssueService(issues, issues, new FakeUserRepository(),
                                                new PermissionPolicy(), LocalDateTime::now));
                assertThrows(IllegalArgumentException.class, () -> plController.restoreIssue(999L, "missing"));
        }
}
