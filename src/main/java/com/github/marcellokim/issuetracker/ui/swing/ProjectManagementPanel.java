package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

final class ProjectManagementPanel extends JPanel implements ProjectManagementView {

    private static final long serialVersionUID = 1L;
    private static final String DESCRIPTION_TEXT = "Description";
    private static final String[] PROJECT_COLUMNS = {"ID", "Project", DESCRIPTION_TEXT, "Members", "Issues"};
    private static final int[] PROJECT_COLUMN_WIDTHS = {72, 180, 280, 88, 88};
    private static final Color SELECTION_BACKGROUND = new Color(219, 234, 254);
    private static final String DEFAULT_ERROR_MESSAGE = "Project management failed. Please try again.";
    private static final SwingPanelSections.HeaderLabels HEADER_LABELS = new SwingPanelSections.HeaderLabels(
            "Project management",
            "projectManagementTitle",
            "projectManagementUser",
            "projectManagementMessage",
            "projectManagementBackButton",
            "projectManagementLogoutButton");

    private final transient ProjectDialogs dialogs;
    private final transient ProjectManagementActions actions;
    private final DefaultTableModel projectTableModel = SwingPanelSections.readOnlyTableModel(PROJECT_COLUMNS);
    private final ProjectTableRows projectRows = new ProjectTableRows(projectTableModel);
    private final JTable projectTable = table();
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton createButton = new JButton("Create project");
    private final JButton openButton = new JButton("Open detail");
    private final JButton renameButton = new JButton("Rename");
    private final JButton descriptionButton = new JButton("Change description");
    private final JButton deleteButton = new JButton("Delete");

    ProjectManagementPanel(
            UserResult user,
            ProjectDialogs dialogs,
            ProjectManagementActions actions) {
        Objects.requireNonNull(user, "user");
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
        this.actions = Objects.requireNonNull(actions, "actions");

        setName("projectManagementPanel");
        setLayout(new BorderLayout(SwingStyles.SECTION_GAP, SwingStyles.SECTION_GAP));
        setBackground(SwingStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING));

        add(header(user), BorderLayout.NORTH);
        add(tableSection(), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);
        updateSelectionActions();
    }

    @Override
    public void showProjects(List<DashboardProjectSummary> projects) {
        Objects.requireNonNull(projects, "projects");
        List<DashboardProjectSummary> snapshot = List.copyOf(projects);
        runOnEdt(() -> {
            projectRows.replaceKeepingSelection(projectTable, snapshot);
            updateSelectionActions();
        });
    }

    @Override
    public void showMessage(String message, boolean error) {
        runOnEdt(() -> SwingPanelSections.updateMessage(
                messageLabel,
                message,
                error,
                DEFAULT_ERROR_MESSAGE));
    }

    void setBusy(boolean busy) {
        runOnEdt(() -> {
            boolean enabled = !busy;
            projectTable.setEnabled(enabled);
            createButton.setEnabled(enabled);
            updateSelectionActions(enabled);
        });
    }

    private JPanel header(UserResult user) {
        return SwingPanelSections.managementHeader(
                HEADER_LABELS,
                user,
                messageLabel,
                new SwingPanelSections.NavigationActions(actions.onBack(), actions.onLogout()));
    }

    private JPanel tableSection() {
        return SwingPanelSections.tableSection("Projects", projectTable);
    }

    private JPanel actions() {
        JPanel panel = new JPanel();
        panel.setBackground(SwingStyles.SURFACE);
        panel.setBorder(SwingStyles.surfaceBorder());

        createButton.setName("createProjectButton");
        createButton.addActionListener(event -> dialogs.requestCreate(this)
                .ifPresent(request -> actions.onCreate().accept(this, request)));
        SwingStyles.applySecondaryButton(createButton);
        panel.add(createButton);

        openButton.setName("openProjectDetailButton");
        openButton.addActionListener(event -> selectedProject()
                .ifPresent(project -> actions.onOpenDetail().accept(this, project.projectId())));
        SwingStyles.applySecondaryButton(openButton);
        panel.add(openButton);

        renameButton.setName("renameProjectButton");
        renameButton.addActionListener(event -> selectedProject().flatMap(project -> dialogs.requestRename(this, project)
                .map(name -> new RenameRequest(project.projectId(), name)))
                .ifPresent(request -> actions.onRename().accept(this, request.projectId(), request.name())));
        SwingStyles.applySecondaryButton(renameButton);
        panel.add(renameButton);

        descriptionButton.setName("changeProjectDescriptionButton");
        descriptionButton.addActionListener(event -> selectedProject()
                .flatMap(project -> dialogs.requestDescription(this, project)
                        .map(description -> new DescriptionRequest(project.projectId(), description)))
                .ifPresent(request -> actions.onDescriptionChange().accept(
                        this,
                        request.projectId(),
                        request.description())));
        SwingStyles.applySecondaryButton(descriptionButton);
        panel.add(descriptionButton);

        deleteButton.setName("deleteProjectButton");
        deleteButton.addActionListener(event -> selectedProject()
                .filter(project -> dialogs.confirmDelete(this, project))
                .ifPresent(project -> actions.onDelete().accept(
                        this,
                        project.projectId(),
                        project.projectName())));
        SwingStyles.applySecondaryButton(deleteButton);
        panel.add(deleteButton);

        return panel;
    }

    private JTable table() {
        JTable table = new JTable(projectTableModel) {
            @Override
            public java.awt.Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                return SwingPanelSections.stripedTableCell(
                        this,
                        super.prepareRenderer(renderer, row, column),
                        row,
                        SELECTION_BACKGROUND);
            }
        };
        SwingPanelSections.configureReadOnlyTable(table, "projectManagementTable", SELECTION_BACKGROUND);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionActions();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0 && table.isEnabled()) {
                    selectedProject().ifPresent(project ->
                            actions.onOpenDetail().accept(ProjectManagementPanel.this, project.projectId()));
                }
            }
        });
        applyColumnWidths(table);
        return table;
    }

    private Optional<DashboardProjectSummary> selectedProject() {
        return projectRows.selectedProject(projectTable);
    }

    private void updateSelectionActions() {
        updateSelectionActions(projectTable.isEnabled());
    }

    private void updateSelectionActions(boolean enabled) {
        boolean hasSelection = enabled && selectedProject().isPresent();
        openButton.setEnabled(hasSelection);
        renameButton.setEnabled(hasSelection);
        descriptionButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
    }

    private void applyColumnWidths(JTable table) {
        SwingPanelSections.applyColumnWidths(table, PROJECT_COLUMN_WIDTHS);
    }

    private static void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        SwingUtilities.invokeLater(action);
    }

    private record RenameRequest(long projectId, String name) {
    }

    private record DescriptionRequest(long projectId, String description) {
    }

    @FunctionalInterface
    interface PanelConsumer<T> {

        void accept(ProjectManagementPanel panel, T value);
    }

    @FunctionalInterface
    interface PanelBiConsumer<T, U> {

        void accept(ProjectManagementPanel panel, T first, U second);
    }

    record ProjectManagementActions(
            PanelConsumer<ProjectCreateRequest> onCreate,
            PanelConsumer<Long> onOpenDetail,
            PanelBiConsumer<Long, String> onRename,
            PanelBiConsumer<Long, String> onDescriptionChange,
            PanelBiConsumer<Long, String> onDelete,
            Runnable onBack,
            Runnable onLogout) {

        ProjectManagementActions {
            Objects.requireNonNull(onCreate, "onCreate");
            Objects.requireNonNull(onOpenDetail, "onOpenDetail");
            Objects.requireNonNull(onRename, "onRename");
            Objects.requireNonNull(onDescriptionChange, "onDescriptionChange");
            Objects.requireNonNull(onDelete, "onDelete");
            Objects.requireNonNull(onBack, "onBack");
            Objects.requireNonNull(onLogout, "onLogout");
        }
    }

    static final class JOptionPaneProjectDialogs implements ProjectDialogs {

        @Override
        public Optional<ProjectCreateRequest> requestCreate(ProjectManagementPanel parent) {
            JTextField name = new JTextField();
            JTextField description = new JTextField();
            JPanel form = SwingPanelSections.formPanel(
                    260,
                    new JLabel("Name"), name,
                    new JLabel(DESCRIPTION_TEXT), description);
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    form,
                    "Create project",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            return Optional.of(new ProjectCreateRequest(name.getText(), description.getText()));
        }

        @Override
        public Optional<String> requestRename(ProjectManagementPanel parent, DashboardProjectSummary selectedProject) {
            String name = (String) JOptionPane.showInputDialog(
                    parent,
                    "Name",
                    "Rename project",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    selectedProject.projectName());
            return Optional.ofNullable(name);
        }

        @Override
        public Optional<String> requestDescription(
                ProjectManagementPanel parent,
                DashboardProjectSummary selectedProject) {
            String description = (String) JOptionPane.showInputDialog(
                    parent,
                    DESCRIPTION_TEXT,
                    "Change project description",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    selectedProject.projectDescription());
            return Optional.ofNullable(description);
        }

        @Override
        public boolean confirmDelete(ProjectManagementPanel parent, DashboardProjectSummary selectedProject) {
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    "Delete project \"" + selectedProject.projectName() + "\"?",
                    "Delete project",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            return result == JOptionPane.OK_OPTION;
        }

    }
}
