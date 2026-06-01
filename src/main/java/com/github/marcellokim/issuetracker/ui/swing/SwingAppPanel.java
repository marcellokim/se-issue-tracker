package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.controller.StatisticsController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.service.AssignmentOptionsResult;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
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

    private final transient SwingControllers controllers;
    private final transient IssueActionSupport issueActionSupport;
    private final transient DeletedIssuePrompt deletedIssuePrompt;
    private final transient Consumer<String> titleUpdater;
    private final CardLayout cardLayout = new CardLayout();
    private final LoginPanel loginPanel = new LoginPanel();
    private final JPanel adminDashboardCard = new JPanel(new BorderLayout());
    private final JPanel projectListCard = new JPanel(new BorderLayout());
    private transient SwingWorker<Void, Void> loginWorker;
    private boolean loginVisible = true;
    private final transient AtomicReference<SwingWorker<Void, Void>> dashboardWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> accountWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> projectWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> projectDetailWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> projectListWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> issueListWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> issueDetailWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> statisticsWorker = new AtomicReference<>();
    private final transient AtomicReference<SwingWorker<Void, Void>> deletedIssueWorker = new AtomicReference<>();

    SwingAppPanel(
            AuthenticationController authenticationController,
            DashboardController dashboardController,
            AccountController accountController,
            ProjectController projectController,
            IssueController issueController,
            Consumer<String> titleUpdater) {
        this(
                SwingControllers.withoutStatistics(
                        authenticationController,
                        dashboardController,
                        accountController,
                        projectController,
                        issueController),
                IssueActionSupport.disabled(),
                titleUpdater);
    }

    SwingAppPanel(
            AuthenticationController authenticationController,
            DashboardController dashboardController,
            AccountController accountController,
            ProjectController projectController,
            IssueController issueController,
            IssueActionSupport issueActionSupport,
            Consumer<String> titleUpdater) {
        this(
                SwingControllers.withoutStatistics(
                        authenticationController,
                        dashboardController,
                        accountController,
                        projectController,
                        issueController),
                issueActionSupport,
                titleUpdater);
    }

    SwingAppPanel(
            SwingControllers controllers,
            IssueActionSupport issueActionSupport,
            Consumer<String> titleUpdater) {
        this(
                controllers,
                issueActionSupport,
                new DeletedIssueDialogs.JOptionPaneDeletedIssuePrompt(),
                titleUpdater);
    }

    SwingAppPanel(
            SwingControllers controllers,
            IssueActionSupport issueActionSupport,
            DeletedIssuePrompt deletedIssuePrompt,
            Consumer<String> titleUpdater) {
        this.controllers = Objects.requireNonNull(controllers, "controllers");
        this.issueActionSupport = Objects.requireNonNull(issueActionSupport, "issueActionSupport");
        this.deletedIssuePrompt = Objects.requireNonNull(deletedIssuePrompt, "deletedIssuePrompt");
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
            cancelViewWorkers();
            titleUpdater.accept("Issue Tracker");
            loginPanel.showMessage(" ", false);
            loginPanel.clearPassword();
            loginPanel.setLoginEnabled(true);
            cardLayout.show(this, LOGIN_CARD);
            loginVisible = true;
            SwingUtilities.invokeLater(loginPanel::requestInitialFocus);
        });
    }

    void requestLoginFocus() {
        if (loginVisible) {
            SwingUtilities.invokeLater(loginPanel::requestInitialFocus);
        }
    }

    @Override
    public void showAdminDashboard(UserResult user) {
        Objects.requireNonNull(user, "user");
        SwingUtilities.invokeLater(() -> {
            cancelViewWorkers();
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
            loginVisible = false;
            loadAdminDashboard(panel);
        });
    }

    @Override
    public void showProjectList(UserResult user) {
        Objects.requireNonNull(user, "user");
        runOnEdtAndWait(() -> {
            cancelViewWorkers();
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
            loginVisible = false;
            startProjectListTask(panel, ProjectListPresenter::loadProjects);
        });
    }

    void logout() {
        cancelViewWorkers();
        controllers.authenticationController().logout();
        showLogin();
    }

    private void submitLogin() {
        if (loginWorker != null && !loginWorker.isDone()) {
            return;
        }

        LoginView capturedView = captureLoginRequest();
        LoginPresenter presenter = new LoginPresenter(controllers.authenticationController(), capturedView, this);
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

    private void showAccountManagement(UserResult user) {
        SwingUtilities.invokeLater(() -> {
            cancelViewWorkers();
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
            cancelViewWorkers();
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
            cancelViewWorkers();
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
            cancelViewWorkers();
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
                            () -> showDeletedIssues(user, projectId),
                            () -> showStatistics(user, projectId),
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

    private void showStatistics(UserResult user, long projectId) {
        SwingUtilities.invokeLater(() -> {
            cancelViewWorkers();
            titleUpdater.accept("Statistics");
            StatisticsPanel panel = new StatisticsPanel(
                    user,
                    new StatisticsPanel.StatisticsActions(
                            (panelRef, request) ->
                                    startStatisticsTask(
                                            panelRef,
                                            presenter -> presenter.loadStatistics(projectId, request)),
                            () -> showIssueList(user, projectId),
                            this::logout));
            projectListCard.removeAll();
            projectListCard.add(panel, BorderLayout.CENTER);
            projectListCard.revalidate();
            projectListCard.repaint();
            cardLayout.show(this, PROJECT_LIST_CARD);
            startStatisticsTask(panel, presenter -> presenter.loadStatistics(projectId));
        });
    }

    private void showDeletedIssues(UserResult user, long projectId) {
        SwingUtilities.invokeLater(() -> {
            cancelViewWorkers();
            titleUpdater.accept("Deleted issues");
            DeletedIssuePanel panel = new DeletedIssuePanel(
                    user,
                    new DeletedIssuePanel.DeletedIssueActions(
                            (panelRef, issue) -> showDeletedIssueRestore(projectId, panelRef, issue),
                            (panelRef, issue) -> showDeletedIssuePurge(projectId, panelRef, issue),
                            () -> showIssueList(user, projectId),
                            this::logout));
            projectListCard.removeAll();
            projectListCard.add(panel, BorderLayout.CENTER);
            projectListCard.revalidate();
            projectListCard.repaint();
            cardLayout.show(this, PROJECT_LIST_CARD);
            startDeletedIssueTask(panel, presenter -> presenter.loadDeletedIssues(projectId));
        });
    }

    private void showDeletedIssueRestore(long projectId, DeletedIssuePanel panel, IssueSummary issue) {
        try {
            deletedIssuePrompt.requestRestoreComment(panel, issue)
                    .ifPresent(comment -> startDeletedIssueTask(
                            panel,
                            presenter -> presenter.restoreIssue(projectId, issue.id(), comment)));
        } catch (RuntimeException exception) {
            panel.showMessage(exception.getMessage(), true);
        }
    }

    private void showDeletedIssuePurge(long projectId, DeletedIssuePanel panel, IssueSummary issue) {
        try {
            if (deletedIssuePrompt.confirmPurge(panel, issue)) {
                startDeletedIssueTask(panel, presenter -> presenter.purgeDeletedIssue(projectId, issue.id()));
            }
        } catch (RuntimeException exception) {
            panel.showMessage(exception.getMessage(), true);
        }
    }

    private void showIssueDetail(UserResult user, long projectId, long issueId) {
        SwingUtilities.invokeLater(() -> {
            cancelViewWorkers();
            titleUpdater.accept("Issue detail");
            IssueDetailPanel panel = new IssueDetailPanel(
                    user,
                    new IssueDetailPanel.IssueDetailActions(
                            (panelRef, action) -> showIssueAction(user, projectId, issueId, panelRef, action),
                            (panelRef, mode, selection) -> showIssueComment(issueId, panelRef, mode, selection),
                            (panelRef, mode, selection) -> showIssueDependency(issueId, panelRef, mode, selection),
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
            issueActionSupport.statusChange().prompt().prompt(panel, action, targetStatus.get())
                    .ifPresent(request -> startIssueDetailTask(
                            panel,
                            presenter -> presenter.changeStatus(
                                    issueId,
                                    request.targetStatus(),
                                    request.comment())));
            return;
        }
        Optional<IssueAssignmentMode> assignmentMode = IssueAssignmentActions.mode(action);
        if (assignmentMode.isPresent()) {
            startIssueAssignmentTask(panel, issueId, assignmentMode.get());
            return;
        }
        if ("ADD_COMMENT".equals(action)) {
            showIssueComment(issueId, panel, IssueCommentMode.ADD, null);
            return;
        }
        if ("ADD_DEPENDENCY".equals(action)) {
            showIssueDependency(issueId, panel, IssueDependencyMode.ADD, null);
            return;
        }
        if ("UPDATE_ISSUE".equals(action)) {
            showIssueEdit(issueId, panel, IssueEditMode.UPDATE);
            return;
        }
        if ("CHANGE_PRIORITY".equals(action)) {
            showIssueEdit(issueId, panel, IssueEditMode.CHANGE_PRIORITY);
            return;
        }
        if ("SOFT_DELETE".equals(action)) {
            showIssueSoftDelete(user, projectId, issueId, panel);
            return;
        }
        panel.showMessage("Unsupported issue action: " + action, true);
    }

    private void showIssueComment(
            long issueId,
            IssueDetailPanel panel,
            IssueCommentMode mode,
            IssueCommentSelection selection) {
        try {
            issueActionSupport.commentPrompt().prompt(panel, mode, selection)
                    .ifPresent(request -> startIssueDetailTask(
                            panel,
                            presenter -> presenter.changeComment(issueId, request)));
        } catch (RuntimeException exception) {
            panel.showMessage(exception.getMessage(), true);
        }
    }

    private void showIssueDependency(
            long issueId,
            IssueDetailPanel panel,
            IssueDependencyMode mode,
            IssueDependencySelection selection) {
        try {
            issueActionSupport.dependencyPrompt().prompt(panel, mode, selection, issueId)
                    .ifPresent(request -> startIssueDetailTask(
                            panel,
                            presenter -> presenter.changeDependency(issueId, request)));
        } catch (RuntimeException exception) {
            panel.showMessage(exception.getMessage(), true);
        }
    }

    private void showIssueEdit(long issueId, IssueDetailPanel panel, IssueEditMode mode) {
        try {
            issueActionSupport.editPrompt().prompt(panel, mode, panel.currentIssueEditContext())
                    .ifPresent(request -> startIssueDetailTask(
                            panel,
                            presenter -> runIssueEdit(issueId, mode, request, presenter)));
        } catch (RuntimeException exception) {
            panel.showMessage(exception.getMessage(), true);
        }
    }

    private static void runIssueEdit(
            long issueId,
            IssueEditMode mode,
            IssueEditRequest request,
            IssueDetailPresenter presenter) {
        switch (mode) {
            case UPDATE -> presenter.updateIssue(issueId, request);
            case CHANGE_PRIORITY -> presenter.changePriority(issueId, request);
            case SOFT_DELETE -> throw new IllegalArgumentException("Soft delete uses the delete workflow.");
        }
    }

    private void showIssueSoftDelete(UserResult user, long projectId, long issueId, IssueDetailPanel panel) {
        try {
            issueActionSupport.editPrompt().prompt(panel, IssueEditMode.SOFT_DELETE, panel.currentIssueEditContext())
                    .ifPresent(request -> {
                        AtomicBoolean deleted = new AtomicBoolean(false);
                        startIssueDetailTask(
                                panel,
                                presenter -> deleted.set(presenter.deleteIssue(issueId, request)),
                                deleted::get,
                                () -> showIssueList(user, projectId));
                    });
        } catch (RuntimeException exception) {
            panel.showMessage(exception.getMessage(), true);
        }
    }

    private void loadAdminDashboard(AdminDashboardPanel panel) {
        DashboardLoadWorker worker = new DashboardLoadWorker(panel);
        dashboardWorker.set(worker);
        worker.execute();
    }

    private void cancelViewWorkers() {
        cancelDashboardWorker();
        cancelAccountWorker();
        cancelProjectWorker();
        cancelProjectDetailWorker();
        cancelProjectListWorker();
        cancelIssueListWorker();
        cancelIssueDetailWorker();
        cancelStatisticsWorker();
        cancelDeletedIssueWorker();
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
        startIssueDetailTask(panel, task, null, null);
    }

    private void startIssueDetailTask(
            IssueDetailPanel panel,
            IssueDetailTask task,
            BooleanSupplier successCondition,
            Runnable onSuccess) {
        Objects.requireNonNull(panel, "panel");
        Objects.requireNonNull(task, "task");
        cancelIssueDetailWorker();
        panel.setBusy(true);
        IssueDetailWorker worker = new IssueDetailWorker(panel, task, successCondition, onSuccess);
        issueDetailWorker.set(worker);
        worker.execute();
    }

    private void startIssueAssignmentTask(IssueDetailPanel panel, long issueId, IssueAssignmentMode mode) {
        Objects.requireNonNull(panel, "panel");
        Objects.requireNonNull(mode, "mode");
        cancelIssueDetailWorker();
        panel.setBusy(true);
        IssueAssignmentOptionsWorker worker = new IssueAssignmentOptionsWorker(panel, issueId, mode);
        issueDetailWorker.set(worker);
        worker.execute();
    }

    private void cancelIssueDetailWorker() {
        SwingWorker<Void, Void> worker = issueDetailWorker.getAndSet(null);
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
    }

    private void startStatisticsTask(StatisticsPanel panel, StatisticsTask task) {
        Objects.requireNonNull(panel, "panel");
        Objects.requireNonNull(task, "task");
        cancelStatisticsWorker();
        panel.setBusy(true);
        StatisticsWorker worker = new StatisticsWorker(panel, task);
        statisticsWorker.set(worker);
        worker.execute();
    }

    private void cancelStatisticsWorker() {
        SwingWorker<Void, Void> worker = statisticsWorker.getAndSet(null);
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
    }

    private void startDeletedIssueTask(DeletedIssuePanel panel, DeletedIssueTask task) {
        Objects.requireNonNull(panel, "panel");
        Objects.requireNonNull(task, "task");
        cancelDeletedIssueWorker();
        panel.setBusy(true);
        DeletedIssueWorker worker = new DeletedIssueWorker(panel, task);
        deletedIssueWorker.set(worker);
        worker.execute();
    }

    private void cancelDeletedIssueWorker() {
        SwingWorker<Void, Void> worker = deletedIssueWorker.getAndSet(null);
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
                    controllers.dashboardController(),
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
                    controllers.dashboardController(),
                    controllers.accountController(),
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
                    controllers.dashboardController(),
                    controllers.projectController(),
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
                    controllers.projectController(),
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
                    controllers.dashboardController(),
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
                    controllers.projectController(),
                    controllers.issueController(),
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
        private final BooleanSupplier successCondition;
        private final Runnable onSuccess;

        private IssueDetailWorker(
                IssueDetailPanel panel,
                IssueDetailTask task,
                BooleanSupplier successCondition,
                Runnable onSuccess) {
            this.panel = Objects.requireNonNull(panel, "panel");
            this.task = Objects.requireNonNull(task, "task");
            this.successCondition = successCondition;
            this.onSuccess = onSuccess;
        }

        @Override
        protected Void doInBackground() {
            IssueDetailPresenter presenter = new IssueDetailPresenter(
                    controllers.issueController(),
                    issueActionSupport.statusChange().issueStateController(),
                    issueActionSupport.assignmentController(),
                    issueActionSupport.deletedIssueController(),
                    new CurrentIssueDetailView(panel, this, issueDetailWorker::get));
            task.run(presenter);
            return null;
        }

        @Override
        protected void done() {
            if (!issueDetailWorker.compareAndSet(this, null)) {
                return;
            }
            panel.setBusy(false);
            if (isCancelled()) {
                return;
            }
            try {
                get();
                if (onSuccess != null && (successCondition == null || successCondition.getAsBoolean())) {
                    onSuccess.run();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                panel.showMessage("Issue detail was interrupted. Please try again.", true);
            } catch (ExecutionException exception) {
                panel.showMessage("Issue detail failed. Please try again.", true);
            }
        }
    }

    private final class IssueAssignmentOptionsWorker extends SwingWorker<Void, Void> {

        private final IssueDetailPanel panel;
        private final long issueId;
        private final IssueAssignmentMode mode;
        private AssignmentOptionsResult options;

        private IssueAssignmentOptionsWorker(IssueDetailPanel panel, long issueId, IssueAssignmentMode mode) {
            this.panel = Objects.requireNonNull(panel, "panel");
            this.issueId = issueId;
            this.mode = Objects.requireNonNull(mode, "mode");
        }

        @Override
        protected Void doInBackground() {
            AssignmentController assignmentController = issueActionSupport.assignmentController();
            if (assignmentController == null) {
                throw new IllegalStateException("Assignment controller is not configured.");
            }
            options = assignmentController.startAssignment(issueId);
            return null;
        }

        @Override
        protected void done() {
            if (!issueDetailWorker.compareAndSet(this, null)) {
                return;
            }
            panel.setBusy(false);
            if (isCancelled()) {
                return;
            }
            try {
                get();
                issueActionSupport.assignmentPrompt().prompt(panel, mode, options)
                        .ifPresent(request -> startIssueDetailTask(
                                panel,
                                presenter -> presenter.changeAssignment(issueId, request)));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                panel.showMessage("Issue assignment was interrupted. Please try again.", true);
            } catch (ExecutionException exception) {
                panel.showMessage("Issue assignment failed. Please try again.", true);
            } catch (RuntimeException exception) {
                panel.showMessage(exception.getMessage(), true);
            }
        }
    }

    private final class StatisticsWorker extends SwingWorker<Void, Void> {

        private final StatisticsPanel panel;
        private final StatisticsTask task;

        private StatisticsWorker(StatisticsPanel panel, StatisticsTask task) {
            this.panel = Objects.requireNonNull(panel, "panel");
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        protected Void doInBackground() {
            StatisticsPresenter presenter = new StatisticsPresenter(
                    requireStatisticsController(),
                    new CurrentStatisticsView(panel, this, statisticsWorker::get));
            task.run(presenter);
            return null;
        }

        @Override
        protected void done() {
            finishWorker(statisticsWorker, this, () -> panel.setBusy(false), panel::showMessage, "Statistics");
        }

        private StatisticsController requireStatisticsController() {
            if (controllers.statisticsController() == null) {
                throw new IllegalStateException("Statistics controller is not configured.");
            }
            return controllers.statisticsController();
        }
    }

    private final class DeletedIssueWorker extends SwingWorker<Void, Void> {

        private final DeletedIssuePanel panel;
        private final DeletedIssueTask task;

        private DeletedIssueWorker(DeletedIssuePanel panel, DeletedIssueTask task) {
            this.panel = Objects.requireNonNull(panel, "panel");
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        protected Void doInBackground() {
            DeletedIssuePresenter presenter = new DeletedIssuePresenter(
                    requireDeletedIssueController(),
                    new CurrentDeletedIssueView(panel, this, deletedIssueWorker::get));
            task.run(presenter);
            return null;
        }

        @Override
        protected void done() {
            finishWorker(
                    deletedIssueWorker,
                    this,
                    () -> panel.setBusy(false),
                    panel::showMessage,
                    "Deleted issue management");
        }

        private DeletedIssueController requireDeletedIssueController() {
            if (issueActionSupport.deletedIssueController() == null) {
                throw new IllegalStateException("Deleted issue controller is not configured.");
            }
            return issueActionSupport.deletedIssueController();
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
    private interface StatisticsTask {

        void run(StatisticsPresenter presenter);
    }

    @FunctionalInterface
    private interface DeletedIssueTask {

        void run(DeletedIssuePresenter presenter);
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
