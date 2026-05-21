package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.util.Objects;
import java.util.Optional;

public final class RepositoryDemoSummaryService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final StatisticsRepository statisticsRepository;
    private final AssignmentRecommendationRepository assignmentRecommendationRepository;

    public RepositoryDemoSummaryService(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            StatisticsRepository statisticsRepository,
            AssignmentRecommendationRepository assignmentRecommendationRepository
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.statisticsRepository = Objects.requireNonNull(statisticsRepository, "statisticsRepository");
        this.assignmentRecommendationRepository =
                Objects.requireNonNull(assignmentRecommendationRepository, "assignmentRecommendationRepository");
    }

    public RepositoryDemoSummary summarizeSeedDemo() {
        return summarize(RepositoryDemoRequest.seedDemo());
    }

    public RepositoryDemoSummary summarize(RepositoryDemoRequest request) {
        RepositoryDemoRequest requiredRequest = Objects.requireNonNull(request, "request");
        Optional<User> admin = userRepository.findByLoginId(requiredRequest.adminLoginId());
        Optional<Project> project = projectRepository.findByName(requiredRequest.projectName());
        return new RepositoryDemoSummary(
                admin.map(RepositoryDemoSummaryService::toAdminAccount),
                project.map(this::toProjectSummary)
        );
    }

    private static RepositoryDemoSummary.AdminAccount toAdminAccount(User user) {
        return new RepositoryDemoSummary.AdminAccount(user.getLoginId(), user.getRole(), user.isActive());
    }

    private RepositoryDemoSummary.ProjectSummary toProjectSummary(Project project) {
        long projectId = project.getId();
        return new RepositoryDemoSummary.ProjectSummary(
                project.getName(),
                projectRepository.findParticipants(projectId).size(),
                userRepository.findActiveByRole(projectId, Role.DEV).size(),
                userRepository.findActiveByRole(projectId, Role.TESTER).size(),
                issueRepository.findByProject(projectId).size(),
                statisticsRepository.countByStatus(projectId),
                statisticsRepository.countByPriority(projectId),
                assignmentRecommendationRepository.findDevAssigneeCandidates(projectId).size(),
                assignmentRecommendationRepository.findTesterVerifierCandidates(projectId).size());
    }
}
