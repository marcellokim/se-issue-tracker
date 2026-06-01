package com.github.marcellokim.issuetracker.ui.swing;

import java.awt.Component;
import java.util.Optional;

@FunctionalInterface
interface IssueCommentPrompt {

    Optional<IssueCommentRequest> prompt(
            Component parent,
            IssueCommentMode mode,
            IssueCommentSelection selection);
}
