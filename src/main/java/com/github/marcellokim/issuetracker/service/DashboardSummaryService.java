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

    public DashboardSummaryService(
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            StatisticsRepository statisticsRepository,
            UserRepository userRepository) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.statisticsRepository = Objects.requireNonNull(statisticsRepository, "statisticsRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    }

    public List<DashboardProjectSummary> projectSummaries() {
        return projectRepository.findAll().stream()
                .map(this::summarizeProject)
                .toList();
    }

    public List<DashboardProjectSummary> projectSummariesFor(User user) {
        /*
         * 대시보드는 여러 repository를 합친 read model.
         * 조회 조합을 service에 두어 JavaFX presenter가 persistence port 없이 화면 데이터 포맷 가능.
         */
        Objects.requireNonNull(user, "user");
        return projectRepository.findAll().stream()
                .filter(project -> user.getRole() == Role.ADMIN || isParticipant(project.getId(), user.getLoginId()))
                .map(this::summarizeProject)
                .toList();
    }

    public List<UserResult> usersFor(User user) {
        Objects.requireNonNull(user, "user");
        if (user.getRole() != Role.ADMIN) {
            return List.of();
        }
        return userRepository.findAll().stream()
                .map(UserResult::from)
                .toList();
    }

    public List<IssueSummary> relatedIssuesFor(User user) {
        Objects.requireNonNull(user, "user");
        return projectRepository.findAll().stream()
                .filter(project -> user.getRole() == Role.ADMIN || isParticipant(project.getId(), user.getLoginId()))
                .flatMap(project -> issueRepository.findByProject(project.getId()).stream())
                .filter(issue -> user.getRole() == Role.ADMIN
                        || user.getRole() == Role.PL
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
                || loginId.equals(issue.verifierId())
                || loginId.equals(issue.fixerId())
                || loginId.equals(issue.resolverId());
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
