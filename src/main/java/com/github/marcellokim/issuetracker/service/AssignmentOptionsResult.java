package com.github.marcellokim.issuetracker.service;

import java.util.List;
import java.util.Objects;

public record AssignmentOptionsResult(
                List<AssignmentCandidateResult> devAssigneeCandidates,
                List<AssignmentCandidateResult> testerVerifierCandidates,
                List<AssignmentCandidateResult> allDevAssignees,
                List<AssignmentCandidateResult> allTesterVerifiers) {

        public AssignmentOptionsResult {
                devAssigneeCandidates = List
                                .copyOf(Objects.requireNonNull(devAssigneeCandidates, "devAssigneeCandidates"));
                testerVerifierCandidates = List.copyOf(
                                Objects.requireNonNull(testerVerifierCandidates, "testerVerifierCandidates"));
                allDevAssignees = List.copyOf(Objects.requireNonNull(allDevAssignees, "allDevAssignees"));
                allTesterVerifiers = List.copyOf(Objects.requireNonNull(allTesterVerifiers, "allTesterVerifiers"));
        }
}
