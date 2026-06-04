package com.github.marcellokim.issuetracker.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository.IssueRecommendationData;

public final class AssignmentRecommendationService {

    private static final int MAX_CANDIDATES = 3;

    private final AssignmentRecommendationRepository recmmendationRepository;
    private final KNNAssignmentRecommendation knnEngine;

    public AssignmentRecommendationService(AssignmentRecommendationRepository recmmendationRepository,
            KNNAssignmentRecommendation knnEngine) {
        this.recmmendationRepository = Objects.requireNonNull(recmmendationRepository, "recmmendationRepository");
        this.knnEngine = Objects.requireNonNull(knnEngine, "knnEngine");
    }

    public AssignmentOptionsResult recommendAssignmentCandidates(Issue issue) {
        Issue targetIssue = Objects.requireNonNull(issue, "issue");
        List<User> activeDevUsers = recmmendationRepository.findActiveDevCandidates(targetIssue.projectId());
        List<User> activeTesterUsers = recmmendationRepository.findActiveTesterCandidates(targetIssue.projectId());
        List<AssignmentCandidateResult> allActiveDevs = toActiveResults(activeDevUsers);
        List<AssignmentCandidateResult> allActiveTesters = toActiveResults(activeTesterUsers);
        return switch (targetIssue.status()) {
            case NEW -> options(
                    findAllDevAssigneeCandidateDetails(targetIssue, activeDevUsers),
                    findAllTesterVerifierCandidateDetails(targetIssue, activeTesterUsers),
                    allActiveDevs, allActiveTesters);
            case REOPENED -> {
                List<AssignmentCandidateResult> devCandidates = new ArrayList<>();
                User fixer = findActiveUser(activeDevUsers, targetIssue.fixerId());
                if (fixer != null) {
                    devCandidates.add(new AssignmentCandidateResult(fixer.getLoginId(),
                            fixer.getName(), fixer.getRole(), 0, "fixed lastly"));
                }
                for (AssignmentCandidateResult c : findAllDevAssigneeCandidateDetails(targetIssue, activeDevUsers)) {
                    if (c.loginId().equals(targetIssue.fixerId()) == false) {
                        devCandidates.add(c);
                    }
                }
                List<AssignmentCandidateResult> testerCandidates = new ArrayList<>();
                User resolver = findActiveUser(activeTesterUsers, targetIssue.resolverId());
                if (resolver != null) {
                    testerCandidates.add(new AssignmentCandidateResult(resolver.getLoginId(),
                            resolver.getName(), resolver.getRole(), 0, "resolved lastly"));
                }
                for (AssignmentCandidateResult c : findAllTesterVerifierCandidateDetails(targetIssue,
                        activeTesterUsers)) {
                    if (c.loginId().equals(targetIssue.resolverId()) == false) {
                        testerCandidates.add(c);
                    }
                }
                yield options(devCandidates, testerCandidates,
                        allActiveDevs, allActiveTesters);
            }
            case ASSIGNED -> {
                List<AssignmentCandidateResult> devCandidates = new ArrayList<>();
                User assignee = findActiveUser(activeDevUsers, targetIssue.assigneeId());
                if (assignee != null) {
                    devCandidates.add(new AssignmentCandidateResult(assignee.getLoginId(),
                            assignee.getName(), assignee.getRole(), 0, "current assignee"));
                }
                for (AssignmentCandidateResult c : findAllDevAssigneeCandidateDetails(targetIssue, activeDevUsers)) {
                    if (c.loginId().equals(targetIssue.assigneeId()) == false) {
                        devCandidates.add(c);
                    }
                }
                yield options(devCandidates, List.of(),
                        allActiveDevs, allActiveTesters);
            }
            case FIXED -> {
                List<AssignmentCandidateResult> testerCandidates = new ArrayList<>();
                User verifier = findActiveUser(activeTesterUsers, targetIssue.verifierId());
                if (verifier != null) {
                    testerCandidates.add(new AssignmentCandidateResult(verifier.getLoginId(),
                            verifier.getName(), verifier.getRole(), 0, "current verifier"));
                }
                for (AssignmentCandidateResult c : findAllTesterVerifierCandidateDetails(targetIssue,
                        activeTesterUsers)) {
                    if (c.loginId().equals(targetIssue.verifierId()) == false) {
                        testerCandidates.add(c);
                    }
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

    private List<AssignmentCandidateResult> findAllDevAssigneeCandidateDetails(Issue issue, List<User> activeDevUsers) {
        Objects.requireNonNull(issue, "issue");
        List<IssueRecommendationData> searchedField = recmmendationRepository
                .findResolvedIssuesForRecommendation(issue.projectId());
        List<KNNAssignmentRecommendation.UserRecord> recommendationRecordForCalculation = searchedField.stream()
                .filter(i -> i.fixerLoginId() != null)
                .map(j -> new KNNAssignmentRecommendation.UserRecord(j.title(), j.description(), j.fixerLoginId()))
                .toList();
        List<String> recommendedAssignees = knnEngine.calculateRecomendation(issue.title(), issue.description(),
                recommendationRecordForCalculation);
        return toCandidateResults(recommendedAssignees, recommendationRecordForCalculation, activeDevUsers,
                "recommended by similarity");
    }

    private List<AssignmentCandidateResult> findAllTesterVerifierCandidateDetails(Issue issue,
            List<User> activeTesterUsers) {
        Objects.requireNonNull(issue, "issue");
        List<IssueRecommendationData> searchedField = recmmendationRepository
                .findResolvedIssuesForRecommendation(issue.projectId());
        List<KNNAssignmentRecommendation.UserRecord> recommendationRecordForCalculation = searchedField.stream()
                .filter(i -> i.resolverLoginId() != null)
                .map(j -> new KNNAssignmentRecommendation.UserRecord(j.title(), j.description(), j.resolverLoginId()))
                .toList();
        List<String> recommendedVerifiers = knnEngine.calculateRecomendation(issue.title(), issue.description(),
                recommendationRecordForCalculation);
        return toCandidateResults(recommendedVerifiers, recommendationRecordForCalculation, activeTesterUsers,
                "recommended by similarity");
    }

    private static List<AssignmentCandidateResult> toCandidateResults(
            List<String> userIds,
            List<KNNAssignmentRecommendation.UserRecord> records,
            List<User> activeCandidateUsers,
            String reason) {
        List<AssignmentCandidateResult> results = new ArrayList<>();
        for (String userId : userIds) {
            User user = findActiveUser(activeCandidateUsers, userId);
            if (user != null) {
                int count = (int) records.stream().filter(r -> r.userId().equals(userId)).count();
                results.add(new AssignmentCandidateResult(
                        userId, user.getName(), user.getRole(), count, reason));
            }
        }
        return results;
    }

    private static User findActiveUser(List<User> activeUsers, String loginId) {
        if (loginId == null)
            return null;
        for (User user : activeUsers) {
            if (user.getLoginId().equals(loginId))
                return user;
        }
        return null;
    }

    private static List<AssignmentCandidateResult> toActiveResults(List<User> activeUsers) {
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
