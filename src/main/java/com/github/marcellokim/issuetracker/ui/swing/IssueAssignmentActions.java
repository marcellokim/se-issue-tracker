package com.github.marcellokim.issuetracker.ui.swing;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class IssueAssignmentActions {

    private static final String START_ASSIGNMENT = "START_ASSIGNMENT";
    private static final List<String> CONCRETE_ACTIONS = List.of("ASSIGN", "REASSIGN_DEV", "CHANGE_TESTER");

    private IssueAssignmentActions() {
    }

    static Optional<IssueAssignmentMode> mode(String action) {
        return switch (action) {
            case "ASSIGN" -> Optional.of(IssueAssignmentMode.ASSIGN);
            case "REASSIGN_DEV" -> Optional.of(IssueAssignmentMode.REASSIGN_DEV);
            case "CHANGE_TESTER" -> Optional.of(IssueAssignmentMode.CHANGE_TESTER);
            default -> Optional.empty();
        };
    }

    static String effectiveAction(String action, Set<String> availableActions) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(availableActions, "availableActions");
        if (!START_ASSIGNMENT.equals(action)) {
            return action;
        }
        return CONCRETE_ACTIONS.stream()
                .filter(availableActions::contains)
                .findFirst()
                .orElse(action);
    }
}
