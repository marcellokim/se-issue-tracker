package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.SwingWorker;

final class CurrentProjectSummaryView implements ProjectManagementView, ProjectListView {

    private final ProjectSummaryView delegate;
    private final CurrentViewGate gate;

    CurrentProjectSummaryView(
            ProjectSummaryView delegate,
            SwingWorker<?, ?> worker,
            Supplier<? extends SwingWorker<?, ?>> currentWorker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.gate = new CurrentViewGate(worker, currentWorker);
    }

    @Override
    public void showProjects(List<DashboardProjectSummary> projects) {
        gate.update(() -> delegate.showProjects(projects));
    }

    @Override
    public void showMessage(String message, boolean error) {
        gate.update(() -> delegate.showMessage(message, error));
    }
}
