package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.StatisticsReportResult;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.SwingWorker;

final class CurrentStatisticsView implements StatisticsView {

    private final StatisticsPanel delegate;
    private final CurrentViewGate gate;

    CurrentStatisticsView(
            StatisticsPanel delegate,
            SwingWorker<?, ?> worker,
            Supplier<? extends SwingWorker<?, ?>> currentWorker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.gate = new CurrentViewGate(worker, currentWorker);
    }

    @Override
    public void showReport(StatisticsReportResult report) {
        delegate.showReport(report, gate::isCurrent);
    }

    @Override
    public void showMessage(String message, boolean error) {
        gate.update(() -> delegate.showMessage(message, error));
    }
}
