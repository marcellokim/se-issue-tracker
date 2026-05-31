package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

final class ProjectManagementPanel extends JPanel implements ProjectManagementView {

    private static final long serialVersionUID = 1L;
    private static final String[] PROJECT_COLUMNS = {"ID", "Project", "Description", "Members", "Issues"};
    private static final int[] PROJECT_COLUMN_WIDTHS = {72, 180, 280, 88, 88};
    private static final Color SELECTION_BACKGROUND = new Color(219, 234, 254);
    private static final Color EVEN_ROW_BACKGROUND = Color.WHITE;
    private static final Color ODD_ROW_BACKGROUND = new Color(248, 250, 252);
    private static final String DEFAULT_ERROR_MESSAGE = "Project management failed. Please try again.";

    private final ProjectDialogs dialogs;
    private final PanelConsumer<ProjectCreateRequest> onCreate;
    private final PanelConsumer<Long> onOpenDetail;
    private final PanelBiConsumer<Long, String> onRename;
    private final PanelBiConsumer<Long, String> onDescriptionChange;
    private final PanelConsumer<Long> onDelete;
    private final DefaultTableModel projectTableModel = readOnlyTableModel();
    private final JTable projectTable = table();
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton createButton = new JButton("Create project");
    private final JButton openButton = new JButton("Open detail");
    private final JButton renameButton = new JButton("Rename");
    private final JButton descriptionButton = new JButton("Change description");
    private final JButton deleteButton = new JButton("Delete");
    private final List<DashboardProjectSummary> projects = new ArrayList<>();

    ProjectManagementPanel(
            UserResult user,
            ProjectDialogs dialogs,
            PanelConsumer<ProjectCreateRequest> onCreate,
            PanelConsumer<Long> onOpenDetail,
            PanelBiConsumer<Long, String> onRename,
            PanelBiConsumer<Long, String> onDescriptionChange,
            PanelConsumer<Long> onDelete,
            Runnable onBack,
            Runnable onLogout) {
        Objects.requireNonNull(user, "user");
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
        this.onCreate = Objects.requireNonNull(onCreate, "onCreate");
        this.onOpenDetail = Objects.requireNonNull(onOpenDetail, "onOpenDetail");
        this.onRename = Objects.requireNonNull(onRename, "onRename");
        this.onDescriptionChange = Objects.requireNonNull(onDescriptionChange, "onDescriptionChange");
        this.onDelete = Objects.requireNonNull(onDelete, "onDelete");
        Objects.requireNonNull(onBack, "onBack");
        Objects.requireNonNull(onLogout, "onLogout");

        setName("projectManagementPanel");
        setLayout(new BorderLayout(SwingStyles.SECTION_GAP, SwingStyles.SECTION_GAP));
        setBackground(SwingStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING));

        add(header(user, onBack, onLogout), BorderLayout.NORTH);
        add(tableSection(), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);
        updateSelectionActions();
    }

    @Override
    public void showProjects(List<DashboardProjectSummary> projects) {
        Objects.requireNonNull(projects, "projects");
        List<DashboardProjectSummary> snapshot = List.copyOf(projects);
        runOnEdt(() -> {
            Long selectedProjectId = selectedProject()
                    .map(DashboardProjectSummary::projectId)
                    .orElse(null);
            this.projects.clear();
            this.projects.addAll(snapshot);
            replaceRows(snapshot);
            restoreSelection(selectedProjectId);
            updateSelectionActions();
        });
    }

    @Override
    public void showMessage(String message, boolean error) {
        String displayMessage = message == null || message.isBlank()
                ? (error ? DEFAULT_ERROR_MESSAGE : " ")
                : message;
        runOnEdt(() -> {
            messageLabel.setText(displayMessage);
            messageLabel.setForeground(error ? SwingStyles.ERROR_TEXT : SwingStyles.MUTED_TEXT);
        });
    }

    void setBusy(boolean busy) {
        runOnEdt(() -> {
            boolean enabled = !busy;
            projectTable.setEnabled(enabled);
            createButton.setEnabled(enabled);
            updateSelectionActions(enabled);
        });
    }

    private JPanel header(UserResult user, Runnable onBack, Runnable onLogout) {
        JPanel header = new JPanel(new BorderLayout(0, SwingStyles.ROW_GAP));
        header.setBackground(SwingStyles.SURFACE);
        header.setBorder(SwingStyles.surfaceBorder());

        JPanel topRow = new JPanel(new BorderLayout(SwingStyles.SECTION_GAP, 0));
        topRow.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Project management");
        title.setName("projectManagementTitle");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyTitle(title);
        titles.add(title);

        JLabel userLabel = new JLabel(user.name() + " (" + user.role() + ")");
        userLabel.setName("projectManagementUser");
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyMuted(userLabel);
        titles.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        titles.add(userLabel);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        JButton backButton = new JButton("Back");
        backButton.setName("projectManagementBackButton");
        backButton.addActionListener(event -> onBack.run());
        nav.add(backButton);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setName("projectManagementLogoutButton");
        logoutButton.addActionListener(event -> onLogout.run());
        nav.add(logoutButton);

        messageLabel.setName("projectManagementMessage");
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyMuted(messageLabel);

        topRow.add(titles, BorderLayout.CENTER);
        topRow.add(nav, BorderLayout.EAST);
        header.add(topRow, BorderLayout.CENTER);
        header.add(messageLabel, BorderLayout.SOUTH);
        return header;
    }

    private JPanel tableSection() {
        JPanel section = new JPanel(new BorderLayout(0, SwingStyles.ROW_GAP));
        section.setBackground(SwingStyles.SURFACE);
        section.setBorder(SwingStyles.surfaceBorder());

        JLabel title = new JLabel("Projects");
        SwingStyles.applySectionTitle(title);
        section.add(title, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(projectTable);
        scrollPane.setColumnHeaderView(projectTable.getTableHeader());
        section.add(scrollPane, BorderLayout.CENTER);
        return section;
    }

    private JPanel actions() {
        JPanel panel = new JPanel();
        panel.setBackground(SwingStyles.SURFACE);
        panel.setBorder(SwingStyles.surfaceBorder());

        createButton.setName("createProjectButton");
        createButton.addActionListener(event -> dialogs.requestCreate(this)
                .ifPresent(request -> onCreate.accept(this, request)));
        panel.add(createButton);

        openButton.setName("openProjectDetailButton");
        openButton.addActionListener(event -> selectedProject()
                .ifPresent(project -> onOpenDetail.accept(this, project.projectId())));
        panel.add(openButton);

        renameButton.setName("renameProjectButton");
        renameButton.addActionListener(event -> selectedProject().flatMap(project -> dialogs.requestRename(this, project)
                .map(name -> new RenameRequest(project.projectId(), name)))
                .ifPresent(request -> onRename.accept(this, request.projectId(), request.name())));
        panel.add(renameButton);

        descriptionButton.setName("changeProjectDescriptionButton");
        descriptionButton.addActionListener(event -> selectedProject()
                .flatMap(project -> dialogs.requestDescription(this, project)
                        .map(description -> new DescriptionRequest(project.projectId(), description)))
                .ifPresent(request -> onDescriptionChange.accept(
                        this,
                        request.projectId(),
                        request.description())));
        panel.add(descriptionButton);

        deleteButton.setName("deleteProjectButton");
        deleteButton.addActionListener(event -> selectedProject()
                .filter(project -> dialogs.confirmDelete(this, project))
                .ifPresent(project -> onDelete.accept(this, project.projectId())));
        panel.add(deleteButton);

        return panel;
    }

    private JTable table() {
        JTable table = new JTable(projectTableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                component.setForeground(SwingStyles.BODY_TEXT);
                if (isRowSelected(row)) {
                    component.setBackground(SELECTION_BACKGROUND);
                } else {
                    component.setBackground(row % 2 == 0 ? EVEN_ROW_BACKGROUND : ODD_ROW_BACKGROUND);
                }
                return component;
            }
        };
        table.setName("projectManagementTable");
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(SELECTION_BACKGROUND);
        table.setSelectionForeground(SwingStyles.BODY_TEXT);
        table.getTableHeader().setReorderingAllowed(false);
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
                            onOpenDetail.accept(ProjectManagementPanel.this, project.projectId()));
                }
            }
        });
        applyColumnWidths(table);
        return table;
    }

    private Optional<DashboardProjectSummary> selectedProject() {
        int selectedRow = projectTable.getSelectedRow();
        if (selectedRow < 0) {
            return Optional.empty();
        }
        int modelRow = projectTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= projects.size()) {
            return Optional.empty();
        }
        return Optional.of(projects.get(modelRow));
    }

    private void replaceRows(List<DashboardProjectSummary> projects) {
        projectTableModel.setRowCount(0);
        for (DashboardProjectSummary project : projects) {
            projectTableModel.addRow(new Object[]{
                    project.projectId(),
                    project.projectName(),
                    project.projectDescription(),
                    project.memberCount(),
                    project.visibleIssueCount()
            });
        }
    }

    private void restoreSelection(Long selectedProjectId) {
        if (selectedProjectId == null) {
            return;
        }
        for (int row = 0; row < projects.size(); row++) {
            if (selectedProjectId == projects.get(row).projectId()) {
                int viewRow = projectTable.convertRowIndexToView(row);
                if (viewRow >= 0) {
                    projectTable.setRowSelectionInterval(viewRow, viewRow);
                }
                return;
            }
        }
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
        if (table.getColumnCount() != PROJECT_COLUMN_WIDTHS.length) {
            return;
        }
        for (int index = 0; index < PROJECT_COLUMN_WIDTHS.length; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(PROJECT_COLUMN_WIDTHS[index]);
        }
    }

    private static DefaultTableModel readOnlyTableModel() {
        return new DefaultTableModel(PROJECT_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
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

    static final class JOptionPaneProjectDialogs implements ProjectDialogs {

        @Override
        public Optional<ProjectCreateRequest> requestCreate(ProjectManagementPanel parent) {
            JTextField name = new JTextField();
            JTextField description = new JTextField();
            JPanel form = formPanel(
                    new JLabel("Name"), name,
                    new JLabel("Description"), description);
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
                    "Description",
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

        static JPanel formPanel(Component... components) {
            if (components.length % 2 != 0) {
                throw new IllegalArgumentException("Form components must be label-field pairs.");
            }
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 1.0;
            for (int index = 0; index < components.length; index += 2) {
                constraints.gridy = index / 2;
                constraints.gridx = 0;
                constraints.weightx = 0.0;
                panel.add(components[index], constraints);
                constraints.gridx = 1;
                constraints.weightx = 1.0;
                Component field = components[index + 1];
                field.setPreferredSize(new Dimension(260, SwingStyles.FIELD_HEIGHT));
                panel.add(field, constraints);
            }
            return panel;
        }
    }
}
