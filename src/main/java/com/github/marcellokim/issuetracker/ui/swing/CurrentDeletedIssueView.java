package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.SwingWorker;

final class CurrentDeletedIssueView implements DeletedIssueView {

    private final DeletedIssuePanel delegate;
    private final CurrentViewGate gate;

    CurrentDeletedIssueView(
            DeletedIssuePanel delegate,
            SwingWorker<?, ?> worker,
            Supplier<? extends SwingWorker<?, ?>> currentWorker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.gate = new CurrentViewGate(worker, currentWorker);
    }

    @Override
    public void showDeletedIssues(int maxRetentionLimit, List<IssueSummary> issues) {
        gate.update(() -> delegate.showDeletedIssues(maxRetentionLimit, issues));
    }

    @Override
    public void showMessage(String message, boolean error) {
        gate.update(() -> delegate.showMessage(message, error));
    }
}
