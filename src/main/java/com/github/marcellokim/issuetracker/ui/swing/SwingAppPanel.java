package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.lang.reflect.InvocationTargetException;
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
    private final transient Consumer<String> titleUpdater;
    private final CardLayout cardLayout = new CardLayout();
    private final LoginPanel loginPanel = new LoginPanel();
    private final JPanel adminDashboardCard = new JPanel(new BorderLayout());
    private final JPanel projectListCard = new JPanel(new BorderLayout());
    private transient SwingWorker<Void, Void> loginWorker;

    SwingAppPanel(AuthenticationController authenticationController, Consumer<String> titleUpdater) {
        this.authenticationController = Objects.requireNonNull(authenticationController, "authenticationController");
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

    @Override
    public void showLogin() {
        runOnEdtAndWait(() -> {
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
        runOnEdtAndWait(() -> {
            titleUpdater.accept("Admin dashboard");
            showPlaceholder(adminDashboardCard, "Admin dashboard", user);
            cardLayout.show(this, ADMIN_DASHBOARD_CARD);
        });
    }

    @Override
    public void showProjectList(UserResult user) {
        Objects.requireNonNull(user, "user");
        runOnEdtAndWait(() -> {
            titleUpdater.accept("Project list");
            showPlaceholder(projectListCard, "Project list", user);
            cardLayout.show(this, PROJECT_LIST_CARD);
        });
    }

    void logout() {
        authenticationController.logout();
        showLogin();
    }

    private void submitLogin() {
        if (loginWorker != null && !loginWorker.isDone()) {
            return;
        }

        LoginView capturedView = captureLoginRequest();
        LoginPresenter presenter = new LoginPresenter(authenticationController, capturedView, this);
        loginWorker = new SwingWorker<>() {
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
                    loginWorker = null;
                }
            }
        };
        loginWorker.execute();
    }

    private LoginView captureLoginRequest() {
        return callOnEdtAndWait(() -> {
            loginPanel.setLoginEnabled(false);
            return new CapturedLoginView(loginPanel.loginId(), loginPanel.password(), loginPanel);
        });
    }

    private void showPlaceholder(JPanel card, String destination, UserResult user) {
        card.removeAll();
        card.add(new PlaceholderPanel(destination, user, this::logout), BorderLayout.CENTER);
        card.revalidate();
        card.repaint();
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
