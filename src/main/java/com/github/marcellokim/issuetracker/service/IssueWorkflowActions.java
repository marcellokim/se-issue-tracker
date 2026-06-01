package com.github.marcellokim.issuetracker.service;

import java.util.ArrayList;
import java.util.List;

public record IssueWorkflowActions(
        boolean canUpdateIssue,
        boolean canChangePriority,
        boolean canStartAssignment,
        boolean canAssign,
        boolean canReassign,
        boolean canChangeVerifier,
        boolean canMarkFixed,
        boolean canRejectFix,
        boolean canResolve,
        boolean canClose,
        boolean canReopen,
        boolean canAddDependency,
        boolean canRemoveDependency,
        boolean canAddComment,
        boolean canSoftDelete) {

    public List<String> availableActionNames() {
        ArrayList<String> names = new ArrayList<>();
        if (canUpdateIssue) {
            names.add("UPDATE_ISSUE");
        }
        if (canChangePriority) {
            names.add("CHANGE_PRIORITY");
        }
        if (canStartAssignment) {
            names.add("START_ASSIGNMENT");
        }
        if (canAssign) {
            names.add("ASSIGN");
        }
        if (canReassign) {
            names.add("REASSIGN_DEV");
        }
        if (canChangeVerifier) {
            names.add("CHANGE_TESTER");
        }
        if (canMarkFixed) {
            names.add("MARK_FIXED");
        }
        if (canRejectFix) {
            names.add("REJECT_FIX");
        }
        if (canResolve) {
            names.add("RESOLVE");
        }
        if (canClose) {
            names.add("CLOSE");
        }
        if (canReopen) {
            names.add("REOPEN");
        }
        if (canAddDependency) {
            names.add("ADD_DEPENDENCY");
        }
        if (canRemoveDependency) {
            names.add("REMOVE_DEPENDENCY");
        }
        if (canAddComment) {
            names.add("ADD_COMMENT");
        }
        if (canSoftDelete) {
            names.add("SOFT_DELETE");
        }
        return List.copyOf(names);
    }
}
