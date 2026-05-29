package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.util.List;
import java.util.Objects;

public final class DashboardSummaryService {
        private final ProjectRepository projectRepository;
        private final IssueRepository issueRepository;
        private final StatisticsRepository statisticsRepository;
        private final UserRepository userRepository;
        private final PermissionPolicy permissionPolicy;

        public DashboardSummaryService(
                        ProjectRepository projectRepository,
                        IssueRepository issueRepository,
                        StatisticsRepository statisticsRepository,
                        UserRepository userRepository,
                        PermissionPolicy permissionPolicy) {
                this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
                this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
                this.statisticsRepository = Objects.requireNonNull(statisticsRepository, "statisticsRepository");
                this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
                this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        }

        public List<DashboardProjectSummary> projectSummariesFor(User user) {
                Objects.requireNonNull(user, "user");
                if (!user.isActive()) {
                        throw new SecurityException("Only active users can view dashboard projects.");
                }
                return projectRepository.findAll().stream()
                                .filter(project -> permissionPolicy.canViewAllProjects(user)
                                                || isParticipant(project.getId(), user.getLoginId()))
                                .map(this::summarizeProject)
                                .toList();
        }

        public List<UserResult> usersFor(User user) {
                Objects.requireNonNull(user, "user");
                if (!permissionPolicy.canViewAllUsers(user)) {
                        return List.of();
                }
                return userRepository.findAll().stream()
                                .map(UserResult::from)
                                .toList();
        }

        public List<IssueSummary> relatedIssuesFor(User user) {
                Objects.requireNonNull(user, "user");
                if (user.getRole() == Role.ADMIN) {
                        return List.of();
                }
                if (!user.isActive()) {
                        throw new SecurityException("Only active users can view dashboard issues.");
                }
                return projectRepository.findAll().stream()
                                .filter(project -> isParticipant(project.getId(), user.getLoginId()))
                                .flatMap(project -> issueRepository.findByProject(project.getId()).stream())
                                .filter(issue -> permissionPolicy.canViewAllProjectIssues(user)
                                                || isRelatedIssue(issue, user.getLoginId()))
                                .map(DashboardSummaryService::toIssueSummary)
                                .toList();
        }

        private DashboardProjectSummary summarizeProject(Project project) {
                return new DashboardProjectSummary(
                                project.getId(),
                                project.getName(),
                                project.getDescription(),
                                projectRepository.findParticipants(project.getId()).size(),
                                userRepository.findActiveByRole(project.getId(), Role.PL).size(),
                                userRepository.findActiveByRole(project.getId(), Role.DEV).size(),
                                userRepository.findActiveByRole(project.getId(), Role.TESTER).size(),
                                issueRepository.findByProject(project.getId()).size(),
                                issueRepository.findDeletedByProject(project.getId()).size(),
                                statisticsRepository.countByStatus(project.getId()));
        }

        private boolean isParticipant(long projectId, String loginId) {
                return projectRepository.findParticipants(projectId).stream()
                                .anyMatch(member -> member.userId().equals(loginId));
        }

        private static boolean isRelatedIssue(Issue issue, String loginId) {
                return loginId.equals(issue.reporterId())
                                || loginId.equals(issue.assigneeId())
                                || loginId.equals(issue.verifierId());
        }

        private static IssueSummary toIssueSummary(Issue issue) {
                return new IssueSummary(
                                issue.id(),
                                issue.getIssueId(),
                                issue.projectId(),
                                issue.status(),
                                issue.priority(),
                                issue.title(),
                                issue.reporterId(),
                                issue.assigneeId(),
                                issue.verifierId(),
                                issue.reportedDate(),
                                issue.updatedAt());
        }
}
