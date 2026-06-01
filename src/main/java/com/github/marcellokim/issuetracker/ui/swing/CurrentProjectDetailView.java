package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.ProjectAdminDetail;
import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.SwingWorker;

final class CurrentProjectDetailView implements ProjectDetailView {

    private final ProjectDetailPanel delegate;
    private final CurrentViewGate gate;

    CurrentProjectDetailView(
            ProjectDetailPanel delegate,
            SwingWorker<?, ?> worker,
            Supplier<? extends SwingWorker<?, ?>> currentWorker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.gate = new CurrentViewGate(worker, currentWorker);
    }

    @Override
    public void showDetail(ProjectAdminDetail detail) {
        gate.update(() -> delegate.showDetail(detail));
    }

    @Override
    public void showParticipants(List<ProjectMemberResult> participants) {
        gate.update(() -> delegate.showParticipants(participants));
    }

    @Override
    public void showMessage(String message, boolean error) {
        gate.update(() -> delegate.showMessage(message, error));
    }
}
