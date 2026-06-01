package com.github.marcellokim.issuetracker.ui.swing;

import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.SwingWorker;

final class CurrentViewGate {

    private final SwingWorker<?, ?> worker;
    private final Supplier<? extends SwingWorker<?, ?>> currentWorker;

    CurrentViewGate(SwingWorker<?, ?> worker, Supplier<? extends SwingWorker<?, ?>> currentWorker) {
        this.worker = Objects.requireNonNull(worker, "worker");
        this.currentWorker = Objects.requireNonNull(currentWorker, "currentWorker");
    }

    void update(Runnable update) {
        Objects.requireNonNull(update, "update");
        SwingPanelSections.runOnEdt(() -> {
            if (isCurrent()) {
                update.run();
            }
        });
    }

    boolean isCurrent() {
        return currentWorker.get() == worker && !worker.isCancelled();
    }
}
