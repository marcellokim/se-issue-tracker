package com.github.marcellokim.issuetracker.controller;

import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.PROJECT_ID;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.anonymousAuth;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.assignmentController;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.authenticated;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.issue;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.issueWithAssigneeAndVerifier;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.issueWithCompletedOwners;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.project;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.AuthFixture;
import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeAssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeIssueRepository;
import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeUserRepository;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.service.AssignmentOptionsResult;
import com.github.marcellokim.issuetracker.service.AssignmentRecommendationService;
import com.github.marcellokim.issuetracker.service.AssignmentResult;
import com.github.marcellokim.issuetracker.service.AssignmentService;
import com.github.marcellokim.issuetracker.service.KNNAssignmentRecommendation;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Assignment controller")
class AssignmentControllerTest {

        @Test
        @DisplayName("assignment start returns the right candidate lists")
        void startsAssignment() {
                AuthFixture auth = authenticated(Role.PL);
                User dev = user("dev1", Role.DEV);
                User tester = user("tester1", Role.TESTER);
                auth.users().save(dev);
                auth.users().save(tester);
                InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID))
                                .withParticipant(PROJECT_ID, auth.user().getLoginId())
                                .withParticipant(PROJECT_ID, dev.getLoginId())
                                .withParticipant(PROJECT_ID, tester.getLoginId());
                auth.users().attachProjects(projects);
                Issue issue = issue(201L, PROJECT_ID, IssueStatus.NEW);
                Issue completedIssue = issueWithCompletedOwners(202L, PROJECT_ID, dev, tester);
                var issues = new FakeIssueRepository(issue, completedIssue);
                var recommendations = new FakeAssignmentRecommendationRepository();
                recommendations.addResolvedIssue(new AssignmentRecommendationRepository.IssueRecommendationData(
                                completedIssue.title(), completedIssue.description(), dev.getLoginId(),
                                tester.getLoginId()));
                recommendations.addCandidate(dev);
                recommendations.addCandidate(tester);
                AssignmentRecommendationService recommendationService = new AssignmentRecommendationService(
                                recommendations, new KNNAssignmentRecommendation());
                AssignmentController controller = new AssignmentController(
                                auth.service(),
                                new AssignmentService(
                                                issues,
                                                auth.users(),
                                                new PermissionPolicy(),
                                                recommendationService,
                                                LocalDateTime::now));

                AssignmentOptionsResult options = controller.startAssignment(issue.id());

                assertEquals(List.of(dev.getLoginId()), options.devAssigneeCandidates().stream()
                                .map(candidate -> candidate.loginId())
                                .toList());
                assertEquals(List.of(tester.getLoginId()), options.testerVerifierCandidates().stream()
                                .map(candidate -> candidate.loginId())
                                .toList());
                assertEquals(List.of(dev.getLoginId()), options.allDevAssignees().stream()
                                .map(candidate -> candidate.loginId())
                                .toList());
                assertEquals(List.of(tester.getLoginId()), options.allTesterVerifiers().stream()
                                .map(candidate -> candidate.loginId())
                                .toList());
        }

        @Test
        @DisplayName("PL can assign and later swap owners")
        void assignmentChanges() {
                AuthFixture auth = authenticated(Role.PL);
                User dev1 = user("dev1", Role.DEV);
                User dev2 = user("dev2", Role.DEV);
                User tester1 = user("tester1", Role.TESTER);
                User tester2 = user("tester2", Role.TESTER);
                auth.users().save(dev1);
                auth.users().save(dev2);
                auth.users().save(tester1);
                auth.users().save(tester2);
                InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID));
                projects.withParticipant(PROJECT_ID, auth.user().getLoginId());
                projects.withParticipant(PROJECT_ID, dev1.getLoginId());
                projects.withParticipant(PROJECT_ID, dev2.getLoginId());
                projects.withParticipant(PROJECT_ID, tester1.getLoginId());
                projects.withParticipant(PROJECT_ID, tester2.getLoginId());
                auth.users().attachProjects(projects);
                Issue issue = issue(201L, PROJECT_ID, IssueStatus.NEW);
                var issues = new FakeIssueRepository(issue);
                AssignmentController controller = assignmentController(auth, issues);

                AssignmentResult assigned = controller.assignIssue(issue.id(), dev1.getLoginId(), tester1.getLoginId());
                assertEquals(IssueStatus.ASSIGNED, assigned.status());
                assertEquals(dev1.getLoginId(), assigned.assignee().loginId());
                assertEquals(tester1.getLoginId(), assigned.verifier().loginId());

                AssignmentResult reassigned = controller.reassignIssue(issue.id(), dev2.getLoginId());
                assertEquals(dev2.getLoginId(), reassigned.assignee().loginId());

                issues.save(issueWithAssigneeAndVerifier(issue.id(), PROJECT_ID, IssueStatus.FIXED, dev2, tester1));
                AssignmentResult verifierChanged = controller.changeVerifier(issue.id(), tester2.getLoginId());
                assertEquals(tester2.getLoginId(), verifierChanged.verifier().loginId());
        }

        @Test
        @DisplayName("assignment start needs login and a real issue")
        void startNeedsLoginAndIssue() {
                Issue issue = issue(201L, PROJECT_ID, IssueStatus.NEW);
                var issues = new FakeIssueRepository(issue);
                AssignmentRecommendationService recommendations = new AssignmentRecommendationService(
                                new FakeAssignmentRecommendationRepository(),
                                new KNNAssignmentRecommendation());
                PermissionPolicy policy = new PermissionPolicy();

                AssignmentController anonymousController = new AssignmentController(
                                anonymousAuth(),
                                new AssignmentService(
                                                issues,
                                                new FakeUserRepository(),
                                                policy,
                                                recommendations,
                                                LocalDateTime::now));
                assertThrows(SecurityException.class, () -> anonymousController.startAssignment(issue.id()));

                AssignmentController plController = new AssignmentController(
                                authenticated(Role.PL).service(),
                                new AssignmentService(
                                                new FakeIssueRepository(),
                                                new FakeUserRepository(),
                                                policy,
                                                recommendations,
                                                LocalDateTime::now));
                assertThrows(IllegalArgumentException.class, () -> plController.startAssignment(issue.id()));
        }
}
