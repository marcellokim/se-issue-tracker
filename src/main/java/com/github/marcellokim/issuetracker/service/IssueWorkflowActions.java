package com.github.marcellokim.issuetracker.service;

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
}
