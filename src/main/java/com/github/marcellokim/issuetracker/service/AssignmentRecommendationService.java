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

    public AssignmentRecommendationService(AssignmentRecommendationRepository recmmendationRepository, KNNAssignmentRecommendation knnEngine) {
        this.recmmendationRepository = Objects.requireNonNull(recmmendationRepository, "recmmendationRepository");
        this.knnEngine = Objects.requireNonNull(knnEngine, "knnEngine");
    }

    public AssignmentOptionsResult recommendAssignmentCandidates(Issue issue){
        Issue targetIssue = Objects.requireNonNull(issue, "issue");
        List<AssignmentCandidateResult> allActiveDevs = toAllActiveDevResults(targetIssue.projectId());
        List<AssignmentCandidateResult> allActiveTesters = toAllActiveTesterResults(targetIssue.projectId());
        return switch(targetIssue.status()) {
            case NEW -> options(
                findAllDevAssigneeCandidateDetails(targetIssue),
                findAllTesterVerifierCandidateDetails(targetIssue),
                allActiveDevs, allActiveTesters);
            case REOPENED -> {
                List<AssignmentCandidateResult> devCandidates = new ArrayList<>();
                User fixer = recmmendationRepository.findCandidateByLoginId(targetIssue.fixerId()).orElse(null);
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
                User assignee = recmmendationRepository.findCandidateByLoginId(targetIssue.assigneeId()).orElse(null);
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
                User verifier = recmmendationRepository.findCandidateByLoginId(targetIssue.verifierId()).orElse(null);
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
        Objects.requireNonNull(issue, "issue");
        List<IssueRecommendationData> searchedField = recmmendationRepository.findResolvedIssuesForRecommendation(issue.projectId());
        List<KNNAssignmentRecommendation.UserRecord> recommendationRecordForCalculation =
        searchedField.stream()
        .filter(i -> i.fixerLoginId() != null)
        .map(j -> new KNNAssignmentRecommendation.UserRecord(j.title(), j.description(), j.fixerLoginId())).toList();
        List<String> recommendedAssignees = knnEngine.calculateRecomendation(issue.title(), issue.description(), recommendationRecordForCalculation);
        return toCandidateResults(recommendedAssignees, recommendationRecordForCalculation, "recommended by similarity");
    }

    private List<AssignmentCandidateResult> findAllTesterVerifierCandidateDetails(Issue issue) {
        Objects.requireNonNull(issue, "issue");
        List<IssueRecommendationData> searchedField = recmmendationRepository.findResolvedIssuesForRecommendation(issue.projectId());
        List<KNNAssignmentRecommendation.UserRecord> recommendationRecordForCalculation =
        searchedField.stream()
        .filter(i -> i.resolverLoginId() != null)
        .map(j -> new KNNAssignmentRecommendation.UserRecord(j.title(), j.description(), j.resolverLoginId())).toList();
        List<String> recommendedVerifiers = knnEngine.calculateRecomendation(issue.title(),issue.description(), recommendationRecordForCalculation);
        return toCandidateResults(recommendedVerifiers, recommendationRecordForCalculation, "recommended by similarity");
    }

    private List<AssignmentCandidateResult> toCandidateResults(
            List<String> userIds,
            List<KNNAssignmentRecommendation.UserRecord> records,
            String reason) {
        List<AssignmentCandidateResult> results = new ArrayList<>();
        for (String userId : userIds) {
            User user = recmmendationRepository.findCandidateByLoginId(userId).orElse(null);
            if (user != null && user.isActive()) {
                int count = (int) records.stream().filter(r -> r.userId().equals(userId)).count();
                results.add(new AssignmentCandidateResult(
                    userId, user.getName(), user.getRole(), count, reason));
            }
        }
        return results;
    }

    private List<AssignmentCandidateResult> toAllActiveDevResults(long projectId) {
        return toActiveResults(recmmendationRepository.findActiveDevCandidates(projectId));
    }

    private List<AssignmentCandidateResult> toAllActiveTesterResults(long projectId) {
        return toActiveResults(recmmendationRepository.findActiveTesterCandidates(projectId));
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
