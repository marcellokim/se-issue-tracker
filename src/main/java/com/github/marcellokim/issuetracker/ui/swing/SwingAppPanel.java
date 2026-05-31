package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

final class SwingAppPanel extends JPanel implements SwingNavigator {

    private static final long serialVersionUID = 1L;
    private static final String LOGIN_CARD = "login";
    private static final String ADMIN_DASHBOARD_CARD = "adminDashboard";
    private static final String PROJECT_LIST_CARD = "projectList";

    private final transient AuthenticationController authenticationController;
    private final transient DashboardController dashboardController;
    private final transient AccountController accountController;
    private final transient Consumer<String> titleUpdater;
    private final CardLayout cardLayout = new CardLayout();
    private final LoginPanel loginPanel = new LoginPanel();
    private final JPanel adminDashboardCard = new JPanel(new BorderLayout());
    private final JPanel projectListCard = new JPanel(new BorderLayout());
    private transient SwingWorker<Void, Void> loginWorker;
    private final transient AtomicReference<DashboardLoadWorker> dashboardWorker = new AtomicReference<>();
    private final transient AtomicReference<AccountManagementWorker> accountWorker = new AtomicReference<>();

    SwingAppPanel(
            AuthenticationController authenticationController,
            DashboardController dashboardController,
            AccountController accountController,
            Consumer<String> titleUpdater) {
        this.authenticationController = Objects.requireNonNull(authenticationController, "authenticationController");
        this.dashboardController = Objects.requireNonNull(dashboardController, "dashboardController");
        this.accountController = Objects.requireNonNull(accountController, "accountController");
        this.titleUpdater = Objects.requireNonNull(titleUpdater, "titleUpdater");

        setName("appCards");
        setLayout(cardLayout);
        adminDashboardCard.setName("adminDashboardCard");
        projectListCard.setName("projectListCard");
        loginPanel.onLoginRequested(this::submitLogin);

        add(loginPanel, LOGIN_CARD);
        add(adminDashboardCard, ADMIN_DASHBOARD_CARD);
        add(projectListCard, PROJECT_LIST_CARD);
        showLogin();
    }

    public void showLogin() {
        runOnEdtAndWait(() -> {
            cancelDashboardWorker();
            cancelAccountWorker();
            titleUpdater.accept("Issue Tracker");
            loginPanel.showMessage(" ", false);
            loginPanel.clearPassword();
            loginPanel.setLoginEnabled(true);
            cardLayout.show(this, LOGIN_CARD);
        });
    }

    @Override
    public void showAdminDashboard(UserResult user) {
        Objects.requireNonNull(user, "user");
        SwingUtilities.invokeLater(() -> {
            cancelDashboardWorker();
            cancelAccountWorker();
            titleUpdater.accept("Admin dashboard");
            AdminDashboardPanel panel = new AdminDashboardPanel(
                    user,
                    () -> showAccountManagement(user),
                    () -> showAdminPlaceholder("Project management", user),
                    this::logout);
            adminDashboardCard.removeAll();
            adminDashboardCard.add(panel, BorderLayout.CENTER);
            adminDashboardCard.revalidate();
            adminDashboardCard.repaint();
            cardLayout.show(this, ADMIN_DASHBOARD_CARD);
            loadAdminDashboard(panel);
        });
    }

    @Override
    public void showProjectList(UserResult user) {
        Objects.requireNonNull(user, "user");
        runOnEdtAndWait(() -> {
            cancelDashboardWorker();
            cancelAccountWorker();
            titleUpdater.accept("Project list");
            showPlaceholder(projectListCard, "Project list", user);
            cardLayout.show(this, PROJECT_LIST_CARD);
        });
    }

    void logout() {
        cancelDashboardWorker();
        cancelAccountWorker();
        authenticationController.logout();
        showLogin();
    }

    private void submitLogin() {
        if (loginWorker != null && !loginWorker.isDone()) {
            return;
        }

        LoginView capturedView = captureLoginRequest();
        LoginPresenter presenter = new LoginPresenter(authenticationController, capturedView, this);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                presenter.loginRequested();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showLoginFailure("Login was interrupted. Please try again.");
                } catch (ExecutionException exception) {
                    showLoginFailure("Login failed. Please try again.");
                } finally {
                    clearCompletedLoginWorker(this);
                }
            }
        };
        loginWorker = worker;
        worker.execute();
    }

    private void clearCompletedLoginWorker(SwingWorker<?, ?> worker) {
        if (loginWorker == worker) {
            loginWorker = null;
        }
    }

    private LoginView captureLoginRequest() {
        return callOnEdtAndWait(() -> {
            loginPanel.setLoginEnabled(false);
            return new CapturedLoginView(loginPanel.loginId(), loginPanel.password(), loginPanel);
        });
    }

    private void showPlaceholder(JPanel card, String destination, UserResult user) {
        showPlaceholder(card, destination, user, null);
    }

    private void showPlaceholder(JPanel card, String destination, UserResult user, Runnable onBack) {
        card.removeAll();
        card.add(new PlaceholderPanel(destination, user, onBack, this::logout), BorderLayout.CENTER);
        card.revalidate();
        card.repaint();
    }

    private void showAdminPlaceholder(String destination, UserResult user) {
        SwingUtilities.invokeLater(() -> {
            cancelDashboardWorker();
            cancelAccountWorker();
            titleUpdater.accept(destination);
            showPlaceholder(adminDashboardCard, destination, user, () -> showAdminDashboard(user));
            cardLayout.show(this, ADMIN_DASHBOARD_CARD);
        });
    }

    private void showAccountManagement(UserResult user) {
        SwingUtilities.invokeLater(() -> {
            cancelDashboardWorker();
            cancelAccountWorker();
            titleUpdater.accept("Account management");
            AccountManagementPanel panel = new AccountManagementPanel(
                    user,
                    new AccountManagementPanel.JOptionPaneAccountDialogs(),
                    (panelRef, request) -> startAccountTask(panelRef, presenter -> presenter.createAccount(request)),
                    (panelRef, loginId, name) ->
                            startAccountTask(panelRef, presenter -> presenter.renameAccount(loginId, name)),
                    (panelRef, loginId, role) ->
                            startAccountTask(panelRef, presenter -> presenter.changeAccountRole(loginId, role)),
                    (panelRef, loginId) -> startAccountTask(panelRef, presenter -> presenter.activateAccount(loginId)),
                    (panelRef, loginId) -> startAccountTask(panelRef, presenter -> presenter.deactivateAccount(loginId)),
                    () -> showAdminDashboard(user),
                    this::logout);
            adminDashboardCard.removeAll();
            adminDashboardCard.add(panel, BorderLayout.CENTER);
            adminDashboardCard.revalidate();
            adminDashboardCard.repaint();
            cardLayout.show(this, ADMIN_DASHBOARD_CARD);
            startAccountTask(panel, AccountManagementPresenter::loadUsers);
        });
    }

    private void loadAdminDashboard(AdminDashboardPanel panel) {
        DashboardLoadWorker worker = new DashboardLoadWorker(panel);
        dashboardWorker.set(worker);
        worker.execute();
    }

    private void cancelDashboardWorker() {
        DashboardLoadWorker worker = dashboardWorker.getAndSet(null);
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
    }

    private void startAccountTask(AccountManagementPanel panel, AccountTask task) {
        Objects.requireNonNull(panel, "panel");
        Objects.requireNonNull(task, "task");
        cancelAccountWorker();
        panel.setBusy(true);
        AccountManagementWorker worker = new AccountManagementWorker(panel, task);
        accountWorker.set(worker);
        worker.execute();
    }

    private void cancelAccountWorker() {
        AccountManagementWorker worker = accountWorker.getAndSet(null);
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
    }

    private void showLoginFailure(String message) {
        runOnEdtAndWait(() -> {
            loginPanel.showMessage(message, true);
            loginPanel.clearPassword();
            loginPanel.setLoginEnabled(true);
        });
    }

    private static void runOnEdtAndWait(Runnable action) {
        Objects.requireNonNull(action, "action");
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while updating Swing UI.", exception);
        } catch (InvocationTargetException exception) {
            rethrow(exception.getCause());
        }
    }

    private static <T> T callOnEdtAndWait(Supplier<T> action) {
        AtomicReference<T> result = new AtomicReference<>();
        runOnEdtAndWait(() -> result.set(action.get()));
        return result.get();
    }

    private static void rethrow(Throwable cause) {
        if (cause instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException("Swing UI update failed.", cause);
    }

    private final class DashboardLoadWorker extends SwingWorker<Void, Void> {

        private final AdminDashboardPanel panel;

        private DashboardLoadWorker(AdminDashboardPanel panel) {
            this.panel = Objects.requireNonNull(panel, "panel");
        }

        @Override
        protected Void doInBackground() {
            AdminDashboardPresenter presenter = new AdminDashboardPresenter(
                    dashboardController,
                    new CurrentDashboardView(panel, this));
            presenter.load();
            return null;
        }

        @Override
        protected void done() {
            dashboardWorker.compareAndSet(this, null);
        }
    }

    private final class AccountManagementWorker extends SwingWorker<Void, Void> {

        private final AccountManagementPanel panel;
        private final AccountTask task;

        private AccountManagementWorker(AccountManagementPanel panel, AccountTask task) {
            this.panel = Objects.requireNonNull(panel, "panel");
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        protected Void doInBackground() {
            AccountManagementPresenter presenter = new AccountManagementPresenter(
                    dashboardController,
                    accountController,
                    new CurrentAccountManagementView(panel, this));
            task.run(presenter);
            return null;
        }

        @Override
        protected void done() {
            if (!accountWorker.compareAndSet(this, null)) {
                return;
            }
            panel.setBusy(false);
            if (isCancelled()) {
                return;
            }
            try {
                get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                panel.showMessage("Account management was interrupted. Please try again.", true);
            } catch (ExecutionException exception) {
                panel.showMessage("Account management failed. Please try again.", true);
            }
        }
    }

    private final class CurrentAccountManagementView implements AccountManagementView {

        private final AccountManagementPanel delegate;
        private final AccountManagementWorker worker;

        private CurrentAccountManagementView(AccountManagementPanel delegate, AccountManagementWorker worker) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.worker = Objects.requireNonNull(worker, "worker");
        }

        @Override
        public void showUsers(List<UserResult> users) {
            if (isCurrent()) {
                delegate.showUsers(users);
            }
        }

        @Override
        public void showMessage(String message, boolean error) {
            if (isCurrent()) {
                delegate.showMessage(message, error);
            }
        }

        private boolean isCurrent() {
            return accountWorker.get() == worker && !worker.isCancelled();
        }
    }

    @FunctionalInterface
    private interface AccountTask {

        void run(AccountManagementPresenter presenter);
    }

    private final class CurrentDashboardView implements AdminDashboardView {

        private final AdminDashboardPanel delegate;
        private final DashboardLoadWorker worker;

        private CurrentDashboardView(AdminDashboardPanel delegate, DashboardLoadWorker worker) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.worker = Objects.requireNonNull(worker, "worker");
        }

        @Override
        public void showDashboard(List<DashboardProjectSummary> projects, List<UserResult> users) {
            if (isCurrent()) {
                delegate.showDashboard(projects, users);
            }
        }

        @Override
        public void showError(String message) {
            if (isCurrent()) {
                delegate.showError(message);
            }
        }

        private boolean isCurrent() {
            return dashboardWorker.get() == worker && !worker.isCancelled();
        }
    }

    private static final class CapturedLoginView implements LoginView {

        private final String loginId;
        private final String password;
        private final LoginView delegate;

        private CapturedLoginView(String loginId, String password, LoginView delegate) {
            this.loginId = loginId;
            this.password = password;
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public String loginId() {
            return loginId;
        }

        @Override
        public String password() {
            return password;
        }

        @Override
        public void setLoginEnabled(boolean enabled) {
            runOnEdtAndWait(() -> delegate.setLoginEnabled(enabled));
        }

        @Override
        public void showMessage(String message, boolean error) {
            runOnEdtAndWait(() -> delegate.showMessage(message, error));
        }

        @Override
        public void clearPassword() {
            runOnEdtAndWait(delegate::clearPassword);
        }
    }
}
