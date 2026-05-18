package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.User;
import java.util.List;

public record AssignmentOptions(
        String issueId,
        IssueStatus issueStatus,
        User currentAssignee,
        User currentVerifier,
        List<User> developers,
        List<User> testers,
        AssignmentCandidates candidates
) {

    public AssignmentOptions {
        developers = List.copyOf(developers);
        testers = List.copyOf(testers);
    }
}
