package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

final class ProjectListPanel extends JPanel implements ProjectListView {

    private static final long serialVersionUID = 1L;
    private static final String[] PROJECT_COLUMNS = {"ID", "Project", "Description", "Members", "Issues"};
    private static final int[] PROJECT_COLUMN_WIDTHS = {72, 180, 300, 88, 88};
    private static final Color SELECTION_BACKGROUND = new Color(219, 234, 254);
    private static final String DEFAULT_ERROR_MESSAGE = "Project list failed. Please try again.";

    private final transient ProjectListActions actions;
    private final DefaultTableModel projectTableModel = SwingPanelSections.readOnlyTableModel(PROJECT_COLUMNS);
    private final ProjectTableRows projectRows = new ProjectTableRows(projectTableModel);
    private final JTable projectTable = table();
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton openButton = new JButton("Open issues");

    ProjectListPanel(UserResult user, ProjectListActions actions) {
        Objects.requireNonNull(user, "user");
        this.actions = Objects.requireNonNull(actions, "actions");

        setName("projectListPanel");
        setLayout(new BorderLayout(SwingStyles.SECTION_GAP, SwingStyles.SECTION_GAP));
        setBackground(SwingStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING));

        add(header(user), BorderLayout.NORTH);
        add(tableSection(), BorderLayout.CENTER);
        add(actionBar(), BorderLayout.SOUTH);
        updateActionState();
    }

    @Override
    public void showProjects(List<DashboardProjectSummary> projects) {
        Objects.requireNonNull(projects, "projects");
        List<DashboardProjectSummary> snapshot = List.copyOf(projects);
        SwingPanelSections.runOnEdt(() -> {
            projectRows.replaceKeepingSelection(projectTable, snapshot);
            updateActionState();
        });
    }

    @Override
    public void showMessage(String message, boolean error) {
        SwingPanelSections.runOnEdt(() -> SwingPanelSections.updateMessage(
                messageLabel,
                message,
                error,
                DEFAULT_ERROR_MESSAGE));
    }

    void setBusy(boolean busy) {
        SwingPanelSections.runOnEdt(() -> {
            projectTable.setEnabled(!busy);
            updateActionState(!busy);
        });
    }

    private JPanel header(UserResult user) {
        JPanel header = new JPanel(new BorderLayout(SwingStyles.SECTION_GAP, 0));
        header.setBackground(SwingStyles.SURFACE);
        header.setBorder(SwingStyles.surfaceBorder());

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Project list");
        title.setName("projectListTitle");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyTitle(title);
        titles.add(title);

        JLabel userLabel = new JLabel(user.name() + " (" + user.role() + ")");
        userLabel.setName("projectListUser");
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyMuted(userLabel);
        titles.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        titles.add(userLabel);
        titles.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));

        messageLabel.setName("projectListMessage");
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyMuted(messageLabel);
        titles.add(messageLabel);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setName("projectListLogoutButton");
        logoutButton.addActionListener(event -> actions.onLogout().run());

        header.add(titles, BorderLayout.CENTER);
        header.add(logoutButton, BorderLayout.EAST);
        return header;
    }

    private JPanel tableSection() {
        return SwingPanelSections.tableSection("Projects", projectTable);
    }

    private JPanel actionBar() {
        JPanel panel = new JPanel();
        panel.setBackground(SwingStyles.SURFACE);
        panel.setBorder(SwingStyles.surfaceBorder());

        openButton.setName("openProjectIssuesButton");
        openButton.addActionListener(event -> selectedProject()
                .ifPresent(project -> actions.onOpenIssues().accept(project.projectId())));
        panel.add(openButton);
        return panel;
    }

    private JTable table() {
        JTable table = new JTable(projectTableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                return SwingPanelSections.stripedTableCell(
                        this,
                        super.prepareRenderer(renderer, row, column),
                        row,
                        SELECTION_BACKGROUND);
            }
        };
        SwingPanelSections.configureReadOnlyTable(table, "projectListTable", SELECTION_BACKGROUND);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActionState();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0 && table.isEnabled()) {
                    selectedProject().ifPresent(project ->
                            actions.onOpenIssues().accept(project.projectId()));
                }
            }
        });
        applyColumnWidths(table);
        return table;
    }

    private Optional<DashboardProjectSummary> selectedProject() {
        return projectRows.selectedProject(projectTable);
    }

    private void updateActionState() {
        updateActionState(projectTable.isEnabled());
    }

    private void updateActionState(boolean enabled) {
        openButton.setEnabled(enabled && selectedProject().isPresent());
    }

    private void applyColumnWidths(JTable table) {
        SwingPanelSections.applyColumnWidths(table, PROJECT_COLUMN_WIDTHS);
    }

    @FunctionalInterface
    interface ProjectOpenConsumer {

        void accept(long projectId);
    }

    record ProjectListActions(ProjectOpenConsumer onOpenIssues, Runnable onLogout) {

        ProjectListActions {
            Objects.requireNonNull(onOpenIssues, "onOpenIssues");
            Objects.requireNonNull(onLogout, "onLogout");
        }
    }
}
