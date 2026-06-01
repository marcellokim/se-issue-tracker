package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.SwingWorker;

final class CurrentDashboardView implements AdminDashboardView {

    private final AdminDashboardPanel delegate;
    private final CurrentViewGate gate;

    CurrentDashboardView(
            AdminDashboardPanel delegate,
            SwingWorker<?, ?> worker,
            Supplier<? extends SwingWorker<?, ?>> currentWorker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.gate = new CurrentViewGate(worker, currentWorker);
    }

    @Override
    public void showDashboard(List<DashboardProjectSummary> projects, List<UserResult> users) {
        gate.update(() -> delegate.showDashboard(projects, users));
    }

    @Override
    public void showError(String message) {
        gate.update(() -> delegate.showError(message));
    }
}
