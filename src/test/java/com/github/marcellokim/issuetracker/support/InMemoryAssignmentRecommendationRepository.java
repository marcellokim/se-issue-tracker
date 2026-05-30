package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryAssignmentRecommendationRepository implements AssignmentRecommendationRepository {

    private final Map<String, User> users = new LinkedHashMap<>();
    private List<IssueRecommendationData> resolvedIssues = List.of();

    public InMemoryAssignmentRecommendationRepository(User... users) {
        Arrays.stream(users).forEach(user -> this.users.put(user.getLoginId(), user));
    }

    public InMemoryAssignmentRecommendationRepository withResolvedIssues(IssueRecommendationData... issues) {
        this.resolvedIssues = List.of(issues);
        return this;
    }

    @Override
    public List<IssueRecommendationData> findResolvedIssuesForRecommendation(long projectId) {
        return resolvedIssues;
    }

    @Override
    public List<User> findActiveDevCandidates(long projectId) {
        return users.values().stream()
                .filter(User::isActive)
                .filter(user -> user.getRole() == Role.DEV)
                .toList();
    }

    @Override
    public List<User> findActiveTesterCandidates(long projectId) {
        return users.values().stream()
                .filter(User::isActive)
                .filter(user -> user.getRole() == Role.TESTER)
                .toList();
    }

}
