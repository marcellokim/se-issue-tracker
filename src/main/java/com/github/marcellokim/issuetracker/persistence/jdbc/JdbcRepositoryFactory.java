package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.persistence.DriverManagerConnectionProvider;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.IssueDependencyChangeRepository;
import com.github.marcellokim.issuetracker.repository.IssueDependencyRepository;
import com.github.marcellokim.issuetracker.repository.IssueHistoryRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;

public final class JdbcRepositoryFactory {

    private final DatabaseConnectionProvider connectionProvider;

    public JdbcRepositoryFactory(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public static JdbcRepositoryFactory fromEnvironment() {
        return new JdbcRepositoryFactory(DriverManagerConnectionProvider.fromEnvironment());
    }

    public UserRepository users() {
        return new JdbcUserRepository(connectionProvider);
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

    public IssueDependencyChangeRepository issueDependencyChanges() {
        return new JdbcIssueDependencyRepository(connectionProvider);
    }

    public StatisticsRepository statistics() {
        return new JdbcStatisticsRepository(connectionProvider);
    }

    public AssignmentRecommendationRepository assignmentRecommendations() {
        return new JdbcAssignmentRecommendationRepository(connectionProvider);
    }
}
