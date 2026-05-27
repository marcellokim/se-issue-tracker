package com.github.marcellokim.issuetracker.service;

// import com.github.marcellokim.issuetracker.domain.AssignmentCandidate;
import com.github.marcellokim.issuetracker.domain.Issue;
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

    public AssignmentOptionsResult recommendAssignmentCandidates(Issue issue) {
        Issue targetIssue = Objects.requireNonNull(issue, ISSUE_REQUIRED);
        return switch (targetIssue.status()) {
            case NEW, REOPENED -> options(
                    findAllDevAssigneeCandidateDetails(targetIssue),
                    findAllTesterVerifierCandidateDetails(targetIssue));
            case ASSIGNED -> options(
                    findAllDevAssigneeCandidateDetails(targetIssue),
                    List.of());
            case FIXED -> options(
                    List.of(),
                    findAllTesterVerifierCandidateDetails(targetIssue));
            case RESOLVED, CLOSED, DELETED -> new AssignmentOptionsResult(List.of(), List.of());
        };
    }

    private static AssignmentOptionsResult options(
            List<AssignmentCandidateResult> allDevCandidates,
            List<AssignmentCandidateResult> allTesterCandidates) {
        return new AssignmentOptionsResult(
                topCandidateResults(allDevCandidates),
                topCandidateResults(allTesterCandidates),
                allDevCandidates,
                allTesterCandidates);
    }

    private List<AssignmentCandidateResult> findAllDevAssigneeCandidateDetails(Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        return recommendations.findDevAssigneeCandidates(issue.projectId()).stream()
                .map(AssignmentCandidateResult::from)
                .toList();
    }

    private List<AssignmentCandidateResult> findAllTesterVerifierCandidateDetails(Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        return recommendations.findTesterVerifierCandidates(issue.projectId()).stream()
                .map(AssignmentCandidateResult::from)
                .toList();
    }

    private static List<AssignmentCandidateResult> topCandidateResults(List<AssignmentCandidateResult> candidates) {
        return candidates.stream()
                .limit(MAX_CANDIDATES)
                .toList();
    }

}
