package com.github.marcellokim.issuetracker.domain;

import java.util.List;

public record AssignmentOptions(
        List<AssignmentCandidate> devAssigneeCandidates,
        List<AssignmentCandidate> testerVerifierCandidates
) {

    public AssignmentOptions {
        devAssigneeCandidates = List.copyOf(devAssigneeCandidates);
        testerVerifierCandidates = List.copyOf(testerVerifierCandidates);
    }
}
