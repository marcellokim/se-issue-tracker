package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.AssignmentCandidate;
import com.github.marcellokim.issuetracker.domain.AssignmentOptions;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import java.util.List;
import java.util.Objects;

public final class AssignmentRecommendationService {

    private static final int MAX_CANDIDATES = 3;
    private static final String ISSUE_REQUIRED = "issue";

    private final AssignmentRecommendationRepository recommendations;

    public AssignmentRecommendationService(AssignmentRecommendationRepository recommendations) {
        this.recommendations = Objects.requireNonNull(recommendations, "recommendations");
    }

    public AssignmentOptions recommendAssignmentCandidates(Issue issue) {
        Issue targetIssue = Objects.requireNonNull(issue, ISSUE_REQUIRED);
        return switch (targetIssue.status()) {
            case NEW, REOPENED -> new AssignmentOptions(
                    findDevAssigneeCandidateDetails(targetIssue),
                    findTesterVerifierCandidateDetails(targetIssue));
            case ASSIGNED -> new AssignmentOptions(
                    findDevAssigneeCandidateDetails(targetIssue),
                    List.of());
            case FIXED -> new AssignmentOptions(
                    List.of(),
                    findTesterVerifierCandidateDetails(targetIssue));
            case RESOLVED, CLOSED, DELETED -> new AssignmentOptions(List.of(), List.of());
        };
    }

    public List<User> findDevAssigneeCandidates(Issue issue) {
        return findDevAssigneeCandidateDetails(issue).stream()
                .map(AssignmentCandidate::user)
                .toList();
    }

    public List<User> findTesterVerifierCandidates(Issue issue) {
        return findTesterVerifierCandidateDetails(issue).stream()
                .map(AssignmentCandidate::user)
                .toList();
    }

    public List<AssignmentCandidate> findDevAssigneeCandidateDetails(Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        return topCandidates(recommendations.findDevAssigneeCandidates(issue.projectId()));
    }

    public List<AssignmentCandidate> findTesterVerifierCandidateDetails(Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        return topCandidates(recommendations.findTesterVerifierCandidates(issue.projectId()));
    }

    private static List<AssignmentCandidate> topCandidates(List<AssignmentCandidate> candidates) {
        return candidates.stream()
                .limit(MAX_CANDIDATES)
                .toList();
    }
}
