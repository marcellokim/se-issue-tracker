package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.SwingWorker;

final class CurrentAccountManagementView implements AccountManagementView {

    private final AccountManagementPanel delegate;
    private final CurrentViewGate gate;

    CurrentAccountManagementView(
            AccountManagementPanel delegate,
            SwingWorker<?, ?> worker,
            Supplier<? extends SwingWorker<?, ?>> currentWorker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.gate = new CurrentViewGate(worker, currentWorker);
    }

    @Override
    public void showUsers(List<UserResult> users) {
        gate.update(() -> delegate.showUsers(users));
    }

    @Override
    public void showMessage(String message, boolean error) {
        gate.update(() -> delegate.showMessage(message, error));
    }
}
