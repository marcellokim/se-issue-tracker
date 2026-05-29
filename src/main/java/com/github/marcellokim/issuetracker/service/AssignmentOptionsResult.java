package com.github.marcellokim.issuetracker.service;

//import com.github.marcellokim.issuetracker.domain.AssignmentOptions;
import java.util.List;

public record AssignmentOptionsResult(
                List<AssignmentCandidateResult> devAssigneeCandidates,
                List<AssignmentCandidateResult> testerVerifierCandidates,
                List<AssignmentCandidateResult> allDevAssignees,
                List<AssignmentCandidateResult> allTesterVerifiers) {

        public AssignmentOptionsResult (List<AssignmentCandidateResult> devAssigneeCandidates,
                List<AssignmentCandidateResult> testerVerifierCandidates,
                List<AssignmentCandidateResult> allDevAssignees,
                List<AssignmentCandidateResult> allTesterVerifiers){
                this.devAssigneeCandidates = devAssigneeCandidates;
                this.testerVerifierCandidates = testerVerifierCandidates;
                this.allDevAssignees = allDevAssignees;
                this.allTesterVerifiers = allTesterVerifiers;
        }
}