package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.SwingWorker;

final class CurrentIssueListView implements IssueListView {

    private final IssueListPanel delegate;
    private final CurrentViewGate gate;

    CurrentIssueListView(
            IssueListPanel delegate,
            SwingWorker<?, ?> worker,
            Supplier<? extends SwingWorker<?, ?>> currentWorker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.gate = new CurrentViewGate(worker, currentWorker);
    }

    @Override
    public void showProject(ProjectResult project) {
        gate.update(() -> delegate.showProject(project));
    }

    @Override
    public void showIssues(List<IssueSummary> issues) {
        gate.update(() -> delegate.showIssues(issues));
    }

    @Override
    public void setRegisterEnabled(boolean enabled) {
        gate.update(() -> delegate.setRegisterEnabled(enabled));
    }

    @Override
    public void showMessage(String message, boolean error) {
        gate.update(() -> delegate.showMessage(message, error));
    }
}
