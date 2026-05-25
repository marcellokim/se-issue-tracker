package com.github.marcellokim.issuetracker.service;

//import com.github.marcellokim.issuetracker.domain.AssignmentOptions;
import java.util.List;
import java.util.Objects;

public record AssignmentOptionsResult(
                List<AssignmentCandidateResult> devAssigneeCandidates,
                List<AssignmentCandidateResult> testerVerifierCandidates,
                List<AssignmentCandidateResult> allDevAssignees,
                List<AssignmentCandidateResult> allTesterVerifiers) {

        public AssignmentOptionsResult(
                        List<AssignmentCandidateResult> devAssigneeCandidates,
                        List<AssignmentCandidateResult> testerVerifierCandidates) {
                this(devAssigneeCandidates, testerVerifierCandidates, devAssigneeCandidates, testerVerifierCandidates);
        }

        public AssignmentOptionsResult {
                devAssigneeCandidates = List
                                .copyOf(Objects.requireNonNull(devAssigneeCandidates, "devAssigneeCandidates"));
                testerVerifierCandidates = List.copyOf(
                                Objects.requireNonNull(testerVerifierCandidates, "testerVerifierCandidates"));
                allDevAssignees = List.copyOf(Objects.requireNonNull(allDevAssignees, "allDevAssignees"));
                allTesterVerifiers = List.copyOf(Objects.requireNonNull(allTesterVerifiers, "allTesterVerifiers"));
        }

        // public static AssignmentOptionsResult from(AssignmentOptions options) {
        // Objects.requireNonNull(options, "options");
        // return new AssignmentOptionsResult(
        // options.devAssigneeCandidates().stream()
        // .map(AssignmentCandidateResult::from)
        // .toList(),
        // options.testerVerifierCandidates().stream()
        // .map(AssignmentCandidateResult::from)
        // .toList());
        // }
}
