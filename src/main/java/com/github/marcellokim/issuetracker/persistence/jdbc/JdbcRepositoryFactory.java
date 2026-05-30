package com.github.marcellokim.issuetracker.persistence.jdbc;

import java.util.Objects;

import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.repository.DeletedIssueRepository;
import com.github.marcellokim.issuetracker.repository.IssueDependencyRepository;
import com.github.marcellokim.issuetracker.repository.IssueHistoryRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.PasswordHashing;

public final class JdbcRepositoryFactory {

    private final DatabaseConnectionProvider connectionProvider;
    private final PasswordHashing passwordHashing;

    public JdbcRepositoryFactory(DatabaseConnectionProvider connectionProvider, PasswordHashing passwordHashing) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.passwordHashing = Objects.requireNonNull(passwordHashing, "passwordHashing");
    }

    public UserRepository users() {
        return new JdbcUserRepository(connectionProvider, passwordHashing);
    }

    public ProjectRepository projects() {
        return new JdbcProjectRepository(connectionProvider);
    }

    public IssueRepository issues() {
        return new JdbcIssueRepository(connectionProvider);
    }

    public CommentRepository comments() {
        return new JdbcCommentRepository(connectionProvider);
    }

    public IssueHistoryRepository issueHistory() {
        return new JdbcIssueHistoryRepository(connectionProvider);
    }

    public IssueDependencyRepository issueDependencies() {
        return new JdbcIssueDependencyRepository(connectionProvider);
    }

    public StatisticsRepository statistics() {
        return new JdbcStatisticsRepository(connectionProvider);
    }

    public AssignmentRecommendationRepository assignmentRecommendations() {
        return new JdbcAssignmentRecommendationRepository(connectionProvider);
    }

    public DeletedIssueRepository deletedIssues() {
        return new JdbcDeletedIssueRepository(connectionProvider);
    }

    public DashboardSummaryRepository dashboardSummaries() {
        return new JdbcDashboardSummaryRepository(connectionProvider);
    }
}
