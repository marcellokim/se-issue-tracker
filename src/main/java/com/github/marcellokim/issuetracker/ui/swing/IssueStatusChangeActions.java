package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import java.util.Optional;

final class IssueStatusChangeActions {

    private IssueStatusChangeActions() {
    }

    static Optional<IssueStatus> targetStatus(String action) {
        return switch (action) {
            case "MARK_FIXED" -> Optional.of(IssueStatus.FIXED);
            case "RESOLVE" -> Optional.of(IssueStatus.RESOLVED);
            case "REJECT_FIX" -> Optional.of(IssueStatus.ASSIGNED);
            case "CLOSE" -> Optional.of(IssueStatus.CLOSED);
            case "REOPEN" -> Optional.of(IssueStatus.REOPENED);
            default -> Optional.empty();
        };
    }

    static String label(String action) {
        return switch (action) {
            case "MARK_FIXED" -> "Mark fixed";
            case "RESOLVE" -> "Resolve";
            case "REJECT_FIX" -> "Reject fix";
            case "CLOSE" -> "Close";
            case "REOPEN" -> "Reopen";
            default -> action;
        };
    }
}
