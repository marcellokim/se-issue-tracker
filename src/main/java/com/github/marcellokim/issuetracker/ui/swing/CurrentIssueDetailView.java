package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueWorkflowActions;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.SwingWorker;

final class CurrentIssueDetailView implements IssueDetailView {

    private final IssueDetailPanel delegate;
    private final CurrentViewGate gate;

    CurrentIssueDetailView(
            IssueDetailPanel delegate,
            SwingWorker<?, ?> worker,
            Supplier<? extends SwingWorker<?, ?>> currentWorker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.gate = new CurrentViewGate(worker, currentWorker);
    }

    @Override
    public void showDetail(IssueDetailResult detail, List<IssueCommentActionState> commentActions) {
        gate.update(() -> delegate.showDetail(detail, commentActions));
    }

    @Override
    public void showActions(IssueWorkflowActions actions) {
        gate.update(() -> delegate.showActions(actions));
    }

    @Override
    public void showMessage(String message, boolean error) {
        gate.update(() -> delegate.showMessage(message, error));
    }
}
