package com.github.marcellokim.issuetracker.domain;

import java.util.List;
import java.util.Objects;

public final class AssignmentOptions {

    private final List<AssignmentCandidate> devAssigneeCandidates;
    private final List<AssignmentCandidate> testerVerifierCandidates;

    public static AssignmentOptions create(
            List<AssignmentCandidate> devAssigneeCandidates,
            List<AssignmentCandidate> testerVerifierCandidates
    ) {
        return new AssignmentOptions(devAssigneeCandidates, testerVerifierCandidates);
    }

    private AssignmentOptions(
            List<AssignmentCandidate> devAssigneeCandidates,
            List<AssignmentCandidate> testerVerifierCandidates
    ) {
        this.devAssigneeCandidates = List.copyOf(devAssigneeCandidates);
        this.testerVerifierCandidates = List.copyOf(testerVerifierCandidates);
    }

    public List<AssignmentCandidate> devAssigneeCandidates() {
        return devAssigneeCandidates;
    }

    public List<AssignmentCandidate> testerVerifierCandidates() {
        return testerVerifierCandidates;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AssignmentOptions that)) {
            return false;
        }
        return Objects.equals(devAssigneeCandidates, that.devAssigneeCandidates)
                && Objects.equals(testerVerifierCandidates, that.testerVerifierCandidates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(devAssigneeCandidates, testerVerifierCandidates);
    }

    @Override
    public String toString() {
        return "AssignmentOptions[devAssigneeCandidates=" + devAssigneeCandidates
                + ", testerVerifierCandidates=" + testerVerifierCandidates + "]";
    }
}
