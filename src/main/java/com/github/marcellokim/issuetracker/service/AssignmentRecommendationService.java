package com.github.marcellokim.issuetracker.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;


public final class AssignmentRecommendationService {

    private static final int MAX_CANDIDATES = 3;
    private static final String ISSUE_REQUIRED = "issue";

    private final IssueRepository searchfield;
    private final UserRepository userRepository;
    private final KNNAssignmentRecommendation knnEngine;

    public AssignmentRecommendationService(IssueRepository searchfield, UserRepository userRepository, KNNAssignmentRecommendation knnEngine) {
        this.searchfield = Objects.requireNonNull(searchfield, "searchfield");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.knnEngine = Objects.requireNonNull(knnEngine, "knnEngine");
    }

    public AssignmentOptionsResult recommendAssignmentCandidates(Issue issue){
        Issue targetIssue = Objects.requireNonNull(issue, "issue");
        List<AssignmentCandidateResult> allActiveDevs = toAllActiveResults(targetIssue.projectId(), Role.DEV);
        List<AssignmentCandidateResult> allActiveTesters = toAllActiveResults(targetIssue.projectId(), Role.TESTER);
        return switch(targetIssue.status()) {
            case NEW -> options(
                findAllDevAssigneeCandidateDetails(targetIssue),
                findAllTesterVerifierCandidateDetails(targetIssue),
                allActiveDevs, allActiveTesters);
            case REOPENED -> {
                List<AssignmentCandidateResult> devCandidates = new ArrayList<>();
                User fixer = userRepository.findByLoginId(targetIssue.fixerId()).orElse(null);
                if (fixer != null && fixer.isActive()) {
                    devCandidates.add(new AssignmentCandidateResult(fixer.getLoginId(),
                    fixer.getName(), fixer.getRole(),0, "fixed lastly"));
                }
                for (AssignmentCandidateResult c : findAllDevAssigneeCandidateDetails(targetIssue)) {
                    if (c.loginId().equals(targetIssue.fixerId()) == false) { devCandidates.add(c); }
                }
                yield options(devCandidates, findAllTesterVerifierCandidateDetails(targetIssue),
                    allActiveDevs, allActiveTesters);
            }
            case ASSIGNED -> {
                List<AssignmentCandidateResult> devCandidates = new ArrayList<>();
                User assignee = userRepository.findByLoginId(targetIssue.assigneeId()).orElse(null);
                if (assignee != null && assignee.isActive()) {devCandidates.add(new AssignmentCandidateResult(assignee.getLoginId(),
                    assignee.getName(), assignee.getRole(),0, "current assignee"));
                }
                for (AssignmentCandidateResult c : findAllDevAssigneeCandidateDetails(targetIssue)) {
                    if (c.loginId().equals(targetIssue.assigneeId()) == false) { devCandidates.add(c); }
                }
                yield options(devCandidates, List.of(),
                    allActiveDevs, allActiveTesters);
            }
            case FIXED -> {
                List<AssignmentCandidateResult> testerCandidates = new ArrayList<>();
                User verifier = userRepository.findByLoginId(targetIssue.verifierId()).orElse(null);
                if (verifier != null && verifier.isActive()) {
                    testerCandidates.add(new AssignmentCandidateResult(verifier.getLoginId(),
                    verifier.getName(), verifier.getRole(),0, "current verifier"));
                }
                for (AssignmentCandidateResult c : findAllTesterVerifierCandidateDetails(targetIssue)) {
                    if (c.loginId().equals(targetIssue.verifierId()) == false) { testerCandidates.add(c); } 
                }
                yield options(List.of(), testerCandidates,
                    allActiveDevs, allActiveTesters);
            }
            default -> throw new IllegalStateException("Unsupported issue status");
        };
    }

    private static AssignmentOptionsResult options(
            List<AssignmentCandidateResult> recommendedDevCandidates,
            List<AssignmentCandidateResult> recommendedTesterCandidates,
            List<AssignmentCandidateResult> allActiveDevs,
            List<AssignmentCandidateResult> allActiveTesters) {
        return new AssignmentOptionsResult(
                topCandidateResults(recommendedDevCandidates),
                topCandidateResults(recommendedTesterCandidates),
                allActiveDevs,
                allActiveTesters);
    }

    private List<AssignmentCandidateResult> findAllDevAssigneeCandidateDetails(Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        List<Issue> searchedField = searchfield.findRecommendationForAssignment(issue.projectId());
        List<KNNAssignmentRecommendation.UserRecord> recommendationRecordForCalculation =
        searchedField.stream()
        .filter(i -> i.fixerId() != null) //예외사항, 중복 보안 처리
        .map(j -> new KNNAssignmentRecommendation.UserRecord(j.title(), j.description(), j.fixerId())).toList();
        List<String> recommendedAssignees = knnEngine.calculateRecomendation(issue.title(), issue.description(), recommendationRecordForCalculation);
        return toCandidateResults(recommendedAssignees, recommendationRecordForCalculation, "recommended by similarity");
    }

    private List<AssignmentCandidateResult> findAllTesterVerifierCandidateDetails(Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        List<Issue> searchedField = searchfield.findRecommendationForAssignment(issue.projectId());
        List<KNNAssignmentRecommendation.UserRecord> recommendationRecordForCalculation =
        searchedField.stream()
        .filter(i -> i.resolverId() != null)
        .map(j -> new KNNAssignmentRecommendation.UserRecord(j.title(), j.description(), j.resolverId())).toList();
        List<String> recommendedVerifiers = knnEngine.calculateRecomendation(issue.title(),issue.description(), recommendationRecordForCalculation);
        return toCandidateResults(recommendedVerifiers, recommendationRecordForCalculation, "recommended by similarity");
    }

    private List<AssignmentCandidateResult> toCandidateResults(
            List<String> userIds,
            List<KNNAssignmentRecommendation.UserRecord> records,
            String reason) {
        List<AssignmentCandidateResult> results = new ArrayList<>();
        for (String userId : userIds) {
            User user = userRepository.findByLoginId(userId).orElse(null);
            if (user != null && user.isActive()) {
                int count = (int) records.stream().filter(r -> r.userId().equals(userId)).count();
                results.add(new AssignmentCandidateResult(
                    userId, user.getName(), user.getRole(), count, reason));
            }
        }
        return results;
    }

    private List<AssignmentCandidateResult> toAllActiveResults(long projectId, Role role) {
        List<User> activeUsers = userRepository.findActiveByRole(projectId, role);
        List<AssignmentCandidateResult> results = new ArrayList<>();
        for (User user : activeUsers) {
            results.add(new AssignmentCandidateResult(
                user.getLoginId(), user.getName(), user.getRole(), 0, ""));
        }
        return results;
    }

    private static List<AssignmentCandidateResult> topCandidateResults(List<AssignmentCandidateResult> candidates) {
        return candidates.stream()
                .limit(MAX_CANDIDATES)
                .toList();
    }
}
