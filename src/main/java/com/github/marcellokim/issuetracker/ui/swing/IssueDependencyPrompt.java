package com.github.marcellokim.issuetracker.ui.swing;

import java.awt.Component;
import java.util.Optional;

@FunctionalInterface
interface IssueDependencyPrompt {

    Optional<IssueDependencyRequest> prompt(
            Component parent,
            IssueDependencyMode mode,
            IssueDependencySelection selection,
            long defaultBlockedIssueId);
}
