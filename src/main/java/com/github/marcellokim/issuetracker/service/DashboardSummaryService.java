package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Role;
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
        /*
         * The dashboard is a read model over several repositories. Keeping those reads
         * here lets the JavaFX presenter format data without knowing persistence ports.
         */
        return projectRepository.findAll().stream()
                .map(project -> new DashboardProjectSummary(
                        project.getName(),
                        projectRepository.findParticipants(project.getId()).size(),
                        userRepository.findActiveByRole(project.getId(), Role.PL).size(),
                        userRepository.findActiveByRole(project.getId(), Role.DEV).size(),
                        userRepository.findActiveByRole(project.getId(), Role.TESTER).size(),
                        issueRepository.findByProject(project.getId()).size(),
                        issueRepository.findDeletedByProject(project.getId()).size(),
                        statisticsRepository.countByStatus(project.getId())))
                .toList();
    }
}
