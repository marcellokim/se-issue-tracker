package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.User;
import java.util.List;

@FunctionalInterface
public interface AssignmentRecommendationService {

    AssignmentCandidates recommendAssignmentCandidates(Issue issue, List<User> developers, List<User> testers);

    static AssignmentRecommendationService noRecommendations() {
        return (issue, developers, testers) -> AssignmentCandidates.empty();
    }
}
