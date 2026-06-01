package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.AssignmentOptionsResult;
import java.awt.Component;
import java.util.Optional;

@FunctionalInterface
interface IssueAssignmentPrompt {

    Optional<IssueAssignmentRequest> prompt(
            Component parent,
            IssueAssignmentMode mode,
            AssignmentOptionsResult options);
}
