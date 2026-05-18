package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.util.Objects;

public class PermissionPolicy {

    public void assertCanAssignIssue(User actor, Issue issue) {
        requireRole(actor, Role.PL);
        requireIssueStatus(issue, IssueStatus.NEW, IssueStatus.REOPENED);
    }

    public void assertCanReassignIssue(User actor, Issue issue) {
        requireRole(actor, Role.PL);
        requireIssueStatus(issue, IssueStatus.ASSIGNED);
    }

    public void assertCanChangeVerifier(User actor, Issue issue) {
        requireRole(actor, Role.PL);
        requireIssueStatus(issue, IssueStatus.FIXED);
    }

    public void assertCanMarkFixed(User actor, Issue issue) {
        requireRole(actor, Role.DEV);
        requireIssueStatus(issue, IssueStatus.ASSIGNED);
        requireSameUser(actor, issue.getAssignee(), "Only current assignee can mark issue fixed");
    }

    public void assertCanResolve(User actor, Issue issue) {
        requireRole(actor, Role.TESTER);
        requireIssueStatus(issue, IssueStatus.FIXED);
        requireSameUser(actor, issue.getVerifier(), "Only current verifier can resolve issue");
    }

    public void assertCanClose(User actor, Issue issue) {
        requireRole(actor, Role.PL);
        requireIssueStatus(issue, IssueStatus.RESOLVED);
    }

    private static void requireRole(User actor, Role expectedRole) {
        Objects.requireNonNull(actor, "actor must not be null");
        if (!actor.isActive()) {
            throw new SecurityException("Actor must be active");
        }
        if (!actor.hasRole(expectedRole)) {
            throw new SecurityException("Actor must have role " + expectedRole);
        }
    }

    private static void requireIssueStatus(Issue issue, IssueStatus... allowedStatuses) {
        Objects.requireNonNull(issue, "issue must not be null");
        for (var allowedStatus : allowedStatuses) {
            if (issue.getStatus() == allowedStatus) {
                return;
            }
        }
        throw new IllegalStateException("Issue status does not allow this operation");
    }

    private static void requireSameUser(User actual, User expected, String message) {
        if (actual == null || expected == null || !Objects.equals(actual.getUserId(), expected.getUserId())) {
            throw new SecurityException(message);
        }
    }
}
