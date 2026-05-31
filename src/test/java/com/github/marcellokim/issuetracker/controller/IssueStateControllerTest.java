package com.github.marcellokim.issuetracker.controller;

import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.NOW;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.PROJECT_ID;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.authenticated;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.issueWithAssigneeAndVerifier;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.project;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.IssueStateResult;
import com.github.marcellokim.issuetracker.service.IssueStateService;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue state controller")
class IssueStateControllerTest {

    @Test
    @DisplayName("assigned dev can mark issue fixed")
    void devMarksFixed() {
        ControllerTestSupport.AuthFixture auth = authenticated(Role.DEV);
        User tester = user("tester1", Role.TESTER);
        auth.users().save(tester);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID));
        projects.withParticipant(PROJECT_ID, auth.user().getLoginId());
        projects.withParticipant(PROJECT_ID, tester.getLoginId());
        auth.users().attachProjects(projects);
        Issue issue = issueWithAssigneeAndVerifier(301L, PROJECT_ID, IssueStatus.ASSIGNED, auth.user(), tester);
        var issues = new ControllerTestSupport.FakeIssueRepository(issue);
        IssueStateController controller = new IssueStateController(
                auth.service(),
                new IssueStateService(
                        issues,
                        new FakeIssueDependencyRepository(),
                        auth.users(),
                        new PermissionPolicy(),
                        () -> NOW,
                        ControllerTestSupport::nextCommentId));

        IssueStateResult result = controller.changeStatus(issue.id(), IssueStatus.FIXED, "fixed in controller test");

        assertEquals(IssueStatus.FIXED, result.status());
        assertEquals(auth.user().getLoginId(), result.fixer().loginId());
    }
}
