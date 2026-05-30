package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.config.ApplicationContext;
import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

public final class SwingAppFrame extends JFrame implements SwingNavigator {

    private static final long serialVersionUID = 1L;
    private static final Dimension MINIMUM_SIZE = new Dimension(800, 600);
    private static final String LOGIN_CARD = "login";
    private static final String ADMIN_DASHBOARD_CARD = "adminDashboard";
    private static final String PROJECT_LIST_CARD = "projectList";

    private final AuthenticationController authenticationController;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final LoginPanel loginPanel = new LoginPanel();
    private final JPanel adminDashboardCard = new JPanel(new BorderLayout());
    private final JPanel projectListCard = new JPanel(new BorderLayout());
    private SwingWorker<Void, Void> loginWorker;

    public SwingAppFrame(ApplicationContext context) {
        this(Objects.requireNonNull(context, "context").authenticationController());
    }

    SwingAppFrame(AuthenticationController authenticationController) {
        super("Issue Tracker");
        this.authenticationController = Objects.requireNonNull(
                authenticationController,
                "authenticationController");

        LoginPresenter presenter = new LoginPresenter(
                this.authenticationController,
                new EdtLoginView(loginPanel),
                this);
        loginPanel.onLoginRequested(() -> submitLogin(presenter));

        cards.setName("appCards");
        adminDashboardCard.setName("adminDashboardCard");
        projectListCard.setName("projectListCard");
        cards.add(loginPanel, LOGIN_CARD);
        cards.add(adminDashboardCard, ADMIN_DASHBOARD_CARD);
        cards.add(projectListCard, PROJECT_LIST_CARD);

        setContentPane(cards);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(SwingStyles.WINDOW_SIZE);
        setMinimumSize(MINIMUM_SIZE);
        setLocationRelativeTo(null);
        showLogin();
    }

    @Override
    public void showLogin() {
        runOnEdtAndWait(() -> {
            setTitle("Issue Tracker");
            loginPanel.showMessage(" ", false);
            loginPanel.clearPassword();
            loginPanel.setLoginEnabled(true);
            cardLayout.show(cards, LOGIN_CARD);
        });
    }

    @Override
    public void showAdminDashboard(UserResult user) {
        Objects.requireNonNull(user, "user");
        runOnEdtAndWait(() -> {
            setTitle("Admin dashboard");
            showPlaceholder(adminDashboardCard, "Admin dashboard", user);
            cardLayout.show(cards, ADMIN_DASHBOARD_CARD);
        });
    }

    @Override
    public void showProjectList(UserResult user) {
        Objects.requireNonNull(user, "user");
        runOnEdtAndWait(() -> {
            setTitle("Project list");
            showPlaceholder(projectListCard, "Project list", user);
            cardLayout.show(cards, PROJECT_LIST_CARD);
        });
    }

    void logout() {
        authenticationController.logout();
        showLogin();
    }

    private void submitLogin(LoginPresenter presenter) {
        if (loginWorker != null && !loginWorker.isDone()) {
            return;
        }

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
                }
            }
        };
        loginWorker.execute();
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

    private static final class EdtLoginView implements LoginView {

        private final LoginView delegate;

        private EdtLoginView(LoginView delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public String loginId() {
            return callOnEdtAndWait(delegate::loginId);
        }

        @Override
        public String password() {
            return callOnEdtAndWait(delegate::password);
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
