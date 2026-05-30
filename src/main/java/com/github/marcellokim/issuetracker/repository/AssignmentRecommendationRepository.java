package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.User;
import java.util.List;
import java.util.Optional;

public interface AssignmentRecommendationRepository {

    record IssueRecommendationData(String title, String description, String fixerLoginId, String resolverLoginId) {}

    List<IssueRecommendationData> findResolvedIssuesForRecommendation(long projectId);

    Optional<User> findCandidateByLoginId(String loginId);

    List<User> findActiveDevCandidates(long projectId);

    List<User> findActiveTesterCandidates(long projectId);
}
