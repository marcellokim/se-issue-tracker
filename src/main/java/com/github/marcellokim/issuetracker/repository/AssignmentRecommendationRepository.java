package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.AssignmentCandidate;
import java.util.List;

public interface AssignmentRecommendationRepository {

    List<AssignmentCandidate> findDevAssigneeCandidates(long projectId);

    List<AssignmentCandidate> findTesterVerifierCandidates(long projectId);
}
