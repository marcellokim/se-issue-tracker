package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.User;
import java.util.List;

public record AssignmentCandidates(
        List<User> assigneeCandidates,
        List<User> verifierCandidates
) {

    public AssignmentCandidates {
        assigneeCandidates = List.copyOf(assigneeCandidates);
        verifierCandidates = List.copyOf(verifierCandidates);
    }

    public static AssignmentCandidates empty() {
        return new AssignmentCandidates(List.of(), List.of());
    }
}
