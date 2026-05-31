package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

@SuppressWarnings("java:S6539")
final class SwingAppPanel extends JPanel implements SwingNavigator {

    private static final long serialVersionUID = 1L;
    private static final String LOGIN_CARD = "login";
    private static final String ADMIN_DASHBOARD_CARD = "adminDashboard";
    private static final String PROJECT_LIST_CARD = "projectList";

    private final transient AuthenticationController authenticationController;
    private final transient DashboardController dashboardController;
    private final transient AccountController accountController;
    private final transient ProjectController projectController;
    private final transient IssueController issueController;
    private final transient IssueStatusChangeSupport statusChangeSupport;
    private final transient Consumer<String> titleUpdater;
    private final CardLayout cardLayout = new CardLayout();
    private final LoginPanel loginPanel = new LoginPanel();
    private final JPanel adminDashboardCard = new JPanel(new BorderLayout());
    private final JPanel projectListCard = new JPanel(new BorderLayout());
    private transient SwingWorker<Void, Void> loginWorker;
    private final transient AtomicReference<SwingWorker<Void, Void>> dashboardWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> accountWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> projectWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> projectDetailWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> projectListWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> issueListWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> issueDetailWorker = new AtomicReference<>();

    SwingAppPanel(
            AuthenticationController authenticationController,
            DashboardController dashboardController,
            AccountController accountController,
            ProjectController projectController,
            IssueController issueController,
            Consumer<String> titleUpdater) {
        this(
                authenticationController,
                dashboardController,
                accountController,
                projectController,
                issueController,
                IssueStatusChangeSupport.disabled(),
                titleUpdater);
    }

    SwingAppPanel(
            AuthenticationController authenticationController,
            DashboardController dashboardController,
            AccountController accountController,
            ProjectController projectController,
            IssueController issueController,
            IssueStatusChangeSupport statusChangeSupport,
            Consumer<String> titleUpdater) {
        this.authenticationController = Objects.requireNonNull(authenticationController, "authenticationController");
        this.dashboardController = Objects.requireNonNull(dashboardController, "dashboardController");
        this.accountController = Objects.requireNonNull(accountController, "accountController");
        this.projectController = Objects.requireNonNull(projectController, "projectController");
        this.issueController = Objects.requireNonNull(issueController, "issueController");
        this.statusChangeSupport = Objects.requireNonNull(statusChangeSupport, "statusChangeSupport");
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
            cancelProjectWorker();
            cancelProjectDetailWorker();
            cancelProjectListWorker();
            cancelIssueListWorker();
            cancelIssueDetailWorker();
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
            cancelProjectWorker();
            cancelProjectDetailWorker();
            cancelProjectListWorker();
            cancelIssueListWorker();
            cancelIssueDetailWorker();
            titleUpdater.accept("Admin dashboard");
            AdminDashboardPanel panel = new AdminDashboardPanel(
                    user,
                    () -> showAccountManagement(user),
                    () -> showProjectManagement(user),
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
            cancelProjectWorker();
            cancelProjectDetailWorker();
            cancelProjectListWorker();
            cancelIssueListWorker();
            cancelIssueDetailWorker();
            titleUpdater.accept("Project list");
            ProjectListPanel panel = new ProjectListPanel(
                    user,
                    new ProjectListPanel.ProjectListActions(
                            projectId -> showIssueList(user, projectId),
                            this::logout));
            projectListCard.removeAll();
            projectListCard.add(panel, BorderLayout.CENTER);
            projectListCard.revalidate();
            projectListCard.repaint();
            cardLayout.show(this, PROJECT_LIST_CARD);
            startProjectListTask(panel, ProjectListPresenter::loadProjects);
        });
    }

    void logout() {
        cancelDashboardWorker();
        cancelAccountWorker();
        cancelProjectWorker();
        cancelProjectDetailWorker();
        cancelProjectListWorker();
        cancelIssueListWorker();
        cancelIssueDetailWorker();
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

    private void showPlaceholder(JPanel card, String destination, UserResult user, Runnable onBack) {
        card.removeAll();
        card.add(new PlaceholderPanel(destination, user, onBack, this::logout), BorderLayout.CENTER);
        card.revalidate();
        card.repaint();
    }

    private void showAccountManagement(UserResult user) {
        SwingUtilities.invokeLater(() -> {
            cancelDashboardWorker();
            cancelAccountWorker();
            cancelProjectWorker();
            cancelProjectDetailWorker();
            cancelProjectListWorker();
            cancelIssueListWorker();
            cancelIssueDetailWorker();
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

    private void showProjectManagement(UserResult user) {
        SwingUtilities.invokeLater(() -> {
            cancelDashboardWorker();
            cancelAccountWorker();
            cancelProjectWorker();
            cancelProjectDetailWorker();
            cancelProjectListWorker();
            cancelIssueListWorker();
            cancelIssueDetailWorker();
            titleUpdater.accept("Project management");
            ProjectManagementPanel panel = new ProjectManagementPanel(
                    user,
                    new ProjectManagementPanel.JOptionPaneProjectDialogs(),
                    new ProjectManagementPanel.ProjectManagementActions(
                            (panelRef, request) ->
                                    startProjectTask(panelRef, presenter -> presenter.createProject(request)),
                            (panelRef, projectId) -> showProjectDetail(user, projectId),
                            (panelRef, projectId, name) ->
                                    startProjectTask(panelRef, presenter -> presenter.renameProject(projectId, name)),
                            (panelRef, projectId, description) ->
                                    startProjectTask(
                                            panelRef,
                                            presenter -> presenter.changeProjectDescription(projectId, description)),
                            (panelRef, projectId, projectName) ->
                                    startProjectTask(
                                            panelRef,
                                            presenter -> presenter.deleteProject(projectId, projectName)),
                            () -> showAdminDashboard(user),
                            this::logout));
            adminDashboardCard.removeAll();
            adminDashboardCard.add(panel, BorderLayout.CENTER);
            adminDashboardCard.revalidate();
            adminDashboardCard.repaint();
            cardLayout.show(this, ADMIN_DASHBOARD_CARD);
            startProjectTask(panel, ProjectManagementPresenter::loadProjects);
        });
    }

    private void showProjectDetail(UserResult user, long projectId) {
        SwingUtilities.invokeLater(() -> {
            cancelDashboardWorker();
            cancelAccountWorker();
            cancelProjectWorker();
            cancelProjectDetailWorker();
            cancelProjectListWorker();
            cancelIssueListWorker();
            cancelIssueDetailWorker();
            titleUpdater.accept("Project detail");
            ProjectDetailPanel panel = new ProjectDetailPanel(
                    user,
                    projectId,
                    new ProjectDetailPanel.JOptionPaneProjectDetailDialogs(),
                    new ProjectDetailPanel.ProjectDetailActions(
                            (panelRef, ignoredProjectId, name) ->
                                    startProjectDetailTask(
                                            panelRef,
                                            presenter -> presenter.renameProject(projectId, name)),
                            (panelRef, ignoredProjectId, description) ->
                                    startProjectDetailTask(
                                            panelRef,
                                            presenter -> presenter.changeProjectDescription(projectId, description)),
                            (panelRef, ignoredProjectId, loginId) ->
                                    startProjectDetailTask(
                                            panelRef,
                                            presenter -> presenter.addProjectParticipant(projectId, loginId)),
                            (panelRef, ignoredProjectId, loginId) ->
                                    startProjectDetailTask(
                                            panelRef,
                                            presenter -> presenter.removeProjectParticipant(projectId, loginId)),
                            () -> showProjectManagement(user),
                            this::logout));
            adminDashboardCard.removeAll();
            adminDashboardCard.add(panel, BorderLayout.CENTER);
            adminDashboardCard.revalidate();
            adminDashboardCard.repaint();
            cardLayout.show(this, ADMIN_DASHBOARD_CARD);
            startProjectDetailTask(panel, presenter -> presenter.loadProject(projectId));
        });
    }

    private void showIssueList(UserResult user, long projectId) {
        SwingUtilities.invokeLater(() -> {
            cancelDashboardWorker();
            cancelAccountWorker();
            cancelProjectWorker();
            cancelProjectDetailWorker();
            cancelProjectListWorker();
            cancelIssueListWorker();
            cancelIssueDetailWorker();
            titleUpdater.accept("Issue list");
            IssueListPanel panel = new IssueListPanel(
                    user,
                    new IssueListPanel.JOptionPaneIssueDialogs(),
                    new IssueListPanel.IssueListActions(
                            (panelRef, request) ->
                                    startIssueListTask(
                                            panelRef,
                                            presenter -> presenter.searchIssues(projectId, request)),
                            (panelRef, request) ->
                                    startIssueListTask(
                                            panelRef,
                                            presenter -> presenter.registerIssue(projectId, request)),
                            issueId -> showIssueDetail(user, projectId, issueId),
                            () -> showProjectList(user),
                            this::logout));
            projectListCard.removeAll();
            projectListCard.add(panel, BorderLayout.CENTER);
            projectListCard.revalidate();
            projectListCard.repaint();
            cardLayout.show(this, PROJECT_LIST_CARD);
            startIssueListTask(panel, presenter -> presenter.loadProjectAndIssues(projectId));
        });
    }

    private void showIssueDetail(UserResult user, long projectId, long issueId) {
        SwingUtilities.invokeLater(() -> {
            cancelDashboardWorker();
            cancelAccountWorker();
            cancelProjectWorker();
            cancelProjectDetailWorker();
            cancelProjectListWorker();
            cancelIssueListWorker();
            cancelIssueDetailWorker();
            titleUpdater.accept("Issue detail");
            IssueDetailPanel panel = new IssueDetailPanel(
                    user,
                    new IssueDetailPanel.IssueDetailActions(
                            (panelRef, action) -> showIssueAction(user, projectId, issueId, panelRef, action),
                            () -> showIssueList(user, projectId),
                            this::logout));
            projectListCard.removeAll();
            projectListCard.add(panel, BorderLayout.CENTER);
            projectListCard.revalidate();
            projectListCard.repaint();
            cardLayout.show(this, PROJECT_LIST_CARD);
            startIssueDetailTask(panel, presenter -> presenter.loadIssue(issueId));
        });
    }

    private void showIssueAction(
            UserResult user,
            long projectId,
            long issueId,
            IssueDetailPanel panel,
            String action) {
        Optional<IssueStatus> targetStatus = IssueStatusChangeActions.targetStatus(action);
        if (targetStatus.isPresent()) {
            statusChangeSupport.prompt().prompt(panel, action, targetStatus.get())
                    .ifPresent(request -> startIssueDetailTask(
                            panel,
                            presenter -> presenter.changeStatus(
                                    issueId,
                                    request.targetStatus(),
                                    request.comment())));
            return;
        }
        SwingUtilities.invokeLater(() -> {
            cancelDashboardWorker();
            cancelAccountWorker();
            cancelProjectWorker();
            cancelProjectDetailWorker();
            cancelProjectListWorker();
            cancelIssueListWorker();
            cancelIssueDetailWorker();
            titleUpdater.accept("Issue action");
            showPlaceholder(
                    projectListCard,
                    "Issue action " + action,
                    user,
                    () -> showIssueDetail(user, projectId, issueId));
            cardLayout.show(this, PROJECT_LIST_CARD);
        });
    }

    private void loadAdminDashboard(AdminDashboardPanel panel) {
        DashboardLoadWorker worker = new DashboardLoadWorker(panel);
        dashboardWorker.set(worker);
        worker.execute();
    }

    private void cancelDashboardWorker() {
        SwingWorker<Void, Void> worker = dashboardWorker.getAndSet(null);
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
        SwingWorker<Void, Void> worker = accountWorker.getAndSet(null);
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
    }

    private void startProjectTask(ProjectManagementPanel panel, ProjectTask task) {
        Objects.requireNonNull(panel, "panel");
        Objects.requireNonNull(task, "task");
        cancelProjectWorker();
        panel.setBusy(true);
        ProjectManagementWorker worker = new ProjectManagementWorker(panel, task);
        projectWorker.set(worker);
        worker.execute();
    }

    private void cancelProjectWorker() {
        SwingWorker<Void, Void> worker = projectWorker.getAndSet(null);
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
    }

    private void startProjectDetailTask(ProjectDetailPanel panel, ProjectDetailTask task) {
        Objects.requireNonNull(panel, "panel");
        Objects.requireNonNull(task, "task");
        cancelProjectDetailWorker();
        panel.setBusy(true);
        ProjectDetailWorker worker = new ProjectDetailWorker(panel, task);
        projectDetailWorker.set(worker);
        worker.execute();
    }

    private void cancelProjectDetailWorker() {
        SwingWorker<Void, Void> worker = projectDetailWorker.getAndSet(null);
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
    }

    private void startProjectListTask(ProjectListPanel panel, ProjectListTask task) {
        Objects.requireNonNull(panel, "panel");
        Objects.requireNonNull(task, "task");
        cancelProjectListWorker();
        panel.setBusy(true);
        ProjectListWorker worker = new ProjectListWorker(panel, task);
        projectListWorker.set(worker);
        worker.execute();
    }

    private void cancelProjectListWorker() {
        SwingWorker<Void, Void> worker = projectListWorker.getAndSet(null);
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
    }

    private void startIssueListTask(IssueListPanel panel, IssueListTask task) {
        Objects.requireNonNull(panel, "panel");
        Objects.requireNonNull(task, "task");
        cancelIssueListWorker();
        panel.setBusy(true);
        IssueListWorker worker = new IssueListWorker(panel, task);
        issueListWorker.set(worker);
        worker.execute();
    }

    private void cancelIssueListWorker() {
        SwingWorker<Void, Void> worker = issueListWorker.getAndSet(null);
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
    }

    private void startIssueDetailTask(IssueDetailPanel panel, IssueDetailTask task) {
        Objects.requireNonNull(panel, "panel");
        Objects.requireNonNull(task, "task");
        cancelIssueDetailWorker();
        panel.setBusy(true);
        IssueDetailWorker worker = new IssueDetailWorker(panel, task);
        issueDetailWorker.set(worker);
        worker.execute();
    }

    private void cancelIssueDetailWorker() {
        SwingWorker<Void, Void> worker = issueDetailWorker.getAndSet(null);
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
                    new CurrentDashboardView(panel, this, dashboardWorker::get));
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
                    new CurrentAccountManagementView(panel, this, accountWorker::get));
            task.run(presenter);
            return null;
        }

        @Override
        protected void done() {
            finishWorker(accountWorker, this, () -> panel.setBusy(false), panel::showMessage, "Account management");
        }
    }

    private final class ProjectManagementWorker extends SwingWorker<Void, Void> {

        private final ProjectManagementPanel panel;
        private final ProjectTask task;

        private ProjectManagementWorker(ProjectManagementPanel panel, ProjectTask task) {
            this.panel = Objects.requireNonNull(panel, "panel");
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        protected Void doInBackground() {
            ProjectManagementPresenter presenter = new ProjectManagementPresenter(
                    dashboardController,
                    projectController,
                    new CurrentProjectSummaryView(panel, this, projectWorker::get));
            task.run(presenter);
            return null;
        }

        @Override
        protected void done() {
            finishWorker(projectWorker, this, () -> panel.setBusy(false), panel::showMessage, "Project management");
        }
    }

    private final class ProjectDetailWorker extends SwingWorker<Void, Void> {

        private final ProjectDetailPanel panel;
        private final ProjectDetailTask task;

        private ProjectDetailWorker(ProjectDetailPanel panel, ProjectDetailTask task) {
            this.panel = Objects.requireNonNull(panel, "panel");
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        protected Void doInBackground() {
            ProjectDetailPresenter presenter = new ProjectDetailPresenter(
                    projectController,
                    new CurrentProjectDetailView(panel, this, projectDetailWorker::get));
            task.run(presenter);
            return null;
        }

        @Override
        protected void done() {
            finishWorker(projectDetailWorker, this, () -> panel.setBusy(false), panel::showMessage, "Project detail");
        }
    }

    private final class ProjectListWorker extends SwingWorker<Void, Void> {

        private final ProjectListPanel panel;
        private final ProjectListTask task;

        private ProjectListWorker(ProjectListPanel panel, ProjectListTask task) {
            this.panel = Objects.requireNonNull(panel, "panel");
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        protected Void doInBackground() {
            ProjectListPresenter presenter = new ProjectListPresenter(
                    dashboardController,
                    new CurrentProjectSummaryView(panel, this, projectListWorker::get));
            task.run(presenter);
            return null;
        }

        @Override
        protected void done() {
            finishWorker(projectListWorker, this, () -> panel.setBusy(false), panel::showMessage, "Project list");
        }
    }

    private final class IssueListWorker extends SwingWorker<Void, Void> {

        private final IssueListPanel panel;
        private final IssueListTask task;

        private IssueListWorker(IssueListPanel panel, IssueListTask task) {
            this.panel = Objects.requireNonNull(panel, "panel");
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        protected Void doInBackground() {
            IssueListPresenter presenter = new IssueListPresenter(
                    projectController,
                    issueController,
                    new CurrentIssueListView(panel, this, issueListWorker::get));
            task.run(presenter);
            return null;
        }

        @Override
        protected void done() {
            finishWorker(issueListWorker, this, () -> panel.setBusy(false), panel::showMessage, "Issue list");
        }
    }

    private final class IssueDetailWorker extends SwingWorker<Void, Void> {

        private final IssueDetailPanel panel;
        private final IssueDetailTask task;

        private IssueDetailWorker(IssueDetailPanel panel, IssueDetailTask task) {
            this.panel = Objects.requireNonNull(panel, "panel");
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        protected Void doInBackground() {
            IssueDetailPresenter presenter = new IssueDetailPresenter(
                    issueController,
                    statusChangeSupport.issueStateController(),
                    new CurrentIssueDetailView(panel, this, issueDetailWorker::get));
            task.run(presenter);
            return null;
        }

        @Override
        protected void done() {
            finishWorker(issueDetailWorker, this, () -> panel.setBusy(false), panel::showMessage, "Issue detail");
        }
    }

    private static void finishWorker(
            AtomicReference<SwingWorker<Void, Void>> workerRef,
            SwingWorker<Void, Void> worker,
            Runnable clearBusy,
            MessageSink showMessage,
            String screenName) {
        if (!workerRef.compareAndSet(worker, null)) {
            return;
        }
        clearBusy.run();
        if (worker.isCancelled()) {
            return;
        }
        try {
            worker.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            showMessage.showMessage(screenName + " was interrupted. Please try again.", true);
        } catch (ExecutionException exception) {
            showMessage.showMessage(screenName + " failed. Please try again.", true);
        }
    }

    @FunctionalInterface
    private interface AccountTask {

        void run(AccountManagementPresenter presenter);
    }

    @FunctionalInterface
    private interface ProjectTask {

        void run(ProjectManagementPresenter presenter);
    }

    @FunctionalInterface
    private interface ProjectDetailTask {

        void run(ProjectDetailPresenter presenter);
    }

    @FunctionalInterface
    private interface ProjectListTask {

        void run(ProjectListPresenter presenter);
    }

    @FunctionalInterface
    private interface IssueListTask {

        void run(IssueListPresenter presenter);
    }

    @FunctionalInterface
    private interface IssueDetailTask {

        void run(IssueDetailPresenter presenter);
    }

    @FunctionalInterface
    private interface MessageSink {

        void showMessage(String message, boolean error);
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
