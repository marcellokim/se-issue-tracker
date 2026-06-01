package com.github.marcellokim.issuetracker.ui.swing;

import java.awt.Component;
import java.util.Optional;

@FunctionalInterface
interface IssueEditPrompt {

    Optional<IssueEditRequest> prompt(Component parent, IssueEditMode mode, IssueEditContext context);
}
