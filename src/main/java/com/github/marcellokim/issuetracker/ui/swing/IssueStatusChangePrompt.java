package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import java.awt.Component;
import java.util.Optional;

@FunctionalInterface
interface IssueStatusChangePrompt {

    Optional<IssueStatusChangeRequest> prompt(Component parent, String action, IssueStatus targetStatus);
}
