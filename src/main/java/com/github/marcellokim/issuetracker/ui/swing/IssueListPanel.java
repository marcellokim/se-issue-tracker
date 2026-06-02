package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

final class IssueListPanel extends JPanel implements IssueListView {

    private static final long serialVersionUID = 1L;
    private static final String[] ISSUE_COLUMNS = {
            "ID", "Issue", "Status", "Priority", "Title", "Reporter", "Assignee", "Verifier", "Updated"
    };
    private static final int[] ISSUE_COLUMN_WIDTHS = {64, 96, 100, 92, 240, 112, 112, 112, 144};
    private static final Color SELECTION_BACKGROUND = new Color(219, 234, 254);
    private static final String DEFAULT_ERROR_MESSAGE = "Issue list failed. Please try again.";

    private final transient IssueDialogs dialogs;
    private final transient IssueListActions actions;
    private final JLabel titleLabel = new JLabel("Project");
    private final JLabel descriptionLabel = new JLabel(" ");
    private final JLabel userLabel;
    private final JLabel messageLabel = new JLabel(" ");
    private final JTextField searchField = new JTextField();
    private final JComboBox<IssueStatus> statusFilter = new JComboBox<>();
    private final JComboBox<Priority> priorityFilter = new JComboBox<>();
    private final DefaultTableModel issueTableModel = SwingPanelSections.readOnlyTableModel(ISSUE_COLUMNS);
    private final transient IssueTableRows issueRows = new IssueTableRows(issueTableModel);
    private final JTable issueTable = table();
    private final JButton searchButton = new JButton("Search");
    private final JButton registerButton = new JButton("Register");
    private final JButton openButton = new JButton("Open");
    private final JButton deletedIssuesButton = new JButton("Deleted");
    private final JButton statisticsButton = new JButton("Statistics");
    private boolean busy;
    private boolean registerAllowed;

    IssueListPanel(
            UserResult user,
            IssueDialogs dialogs,
            IssueListActions actions) {
        Objects.requireNonNull(user, "user");
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
        this.actions = Objects.requireNonNull(actions, "actions");
        this.userLabel = new JLabel(user.name() + " (" + user.role() + ")");

        setName("issueListPanel");
        setLayout(new BorderLayout(SwingStyles.SECTION_GAP, SwingStyles.SECTION_GAP));
        setBackground(SwingStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING));

        configureFilters();
        add(header(), BorderLayout.NORTH);
        add(tableSection(), BorderLayout.CENTER);
        add(actionBar(), BorderLayout.SOUTH);
        updateActionState();
    }

    @Override
    public void showProject(ProjectResult project) {
        Objects.requireNonNull(project, "project");
        String projectName = project.name();
        String description = project.description();
        SwingPanelSections.runOnEdt(() -> {
            titleLabel.setText(projectName);
            descriptionLabel.setText(description == null || description.isBlank() ? " " : description);
        });
    }

    @Override
    public void showIssues(List<IssueSummary> issues) {
        Objects.requireNonNull(issues, "issues");
        List<IssueSummary> snapshot = List.copyOf(issues);
        SwingPanelSections.runOnEdt(() -> {
            issueRows.replaceKeepingSelection(issueTable, snapshot);
            updateActionState();
        });
    }

    @Override
    public void setRegisterEnabled(boolean enabled) {
        SwingPanelSections.runOnEdt(() -> {
            registerAllowed = enabled;
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
            this.busy = busy;
            boolean enabled = !busy;
            issueTable.setEnabled(enabled);
            searchField.setEnabled(enabled);
            statusFilter.setEnabled(enabled);
            priorityFilter.setEnabled(enabled);
            searchButton.setEnabled(enabled);
            updateActionState();
        });
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(SwingStyles.SECTION_GAP, 0));
        header.setBackground(SwingStyles.SURFACE);
        header.setBorder(SwingStyles.surfaceBorder());

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        titleLabel.setName("issueListTitle");
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyTitle(titleLabel);
        titles.add(titleLabel);

        descriptionLabel.setName("issueListProjectDescription");
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyMuted(descriptionLabel);
        titles.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        titles.add(descriptionLabel);

        userLabel.setName("issueListUser");
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyMuted(userLabel);
        titles.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        titles.add(userLabel);

        messageLabel.setName("issueListMessage");
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyMuted(messageLabel);
        titles.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        titles.add(messageLabel);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        JButton backButton = new JButton("Back");
        backButton.setName("issueListBackButton");
        backButton.addActionListener(event -> actions.onBack().run());
        SwingStyles.applySecondaryButton(backButton);
        nav.add(backButton);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setName("issueListLogoutButton");
        logoutButton.addActionListener(event -> actions.onLogout().run());
        SwingStyles.applySecondaryButton(logoutButton);
        nav.add(logoutButton);

        header.add(titles, BorderLayout.CENTER);
        header.add(nav, BorderLayout.EAST);
        return header;
    }

    private JPanel tableSection() {
        return SwingPanelSections.tableSection("Issues", issueTable);
    }

    private JPanel actionBar() {
        JPanel panel = new JPanel();
        panel.setBackground(SwingStyles.SURFACE);
        panel.setBorder(SwingStyles.surfaceBorder());

        searchField.setName("issueSearchField");
        searchField.setColumns(8);
        panel.add(searchField);

        statusFilter.setName("issueStatusFilter");
        panel.add(statusFilter);

        priorityFilter.setName("issuePriorityFilter");
        panel.add(priorityFilter);

        searchButton.setName("searchIssuesButton");
        searchButton.addActionListener(event -> actions.onSearch().accept(this, currentSearchRequest()));
        SwingStyles.applySecondaryButton(searchButton);
        panel.add(searchButton);

        registerButton.setName("registerIssueButton");
        registerButton.addActionListener(event -> dialogs.requestRegister(this)
                .ifPresent(request -> actions.onRegister().accept(this, request)));
        SwingStyles.applySecondaryButton(registerButton);
        panel.add(registerButton);

        openButton.setName("openIssueDetailButton");
        openButton.addActionListener(event -> selectedIssue()
                .ifPresent(issue -> actions.onOpenIssue().accept(issue.id())));
        SwingStyles.applySecondaryButton(openButton);
        panel.add(openButton);

        deletedIssuesButton.setName("deletedIssuesButton");
        deletedIssuesButton.addActionListener(event -> actions.onDeletedIssues().run());
        SwingStyles.applySecondaryButton(deletedIssuesButton);
        panel.add(deletedIssuesButton);

        statisticsButton.setName("statisticsButton");
        statisticsButton.addActionListener(event -> actions.onStatistics().run());
        SwingStyles.applySecondaryButton(statisticsButton);
        panel.add(statisticsButton);
        return panel;
    }

    private JTable table() {
        JTable table = new JTable(issueTableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                return SwingPanelSections.stripedTableCell(
                        this,
                        super.prepareRenderer(renderer, row, column),
                        row,
                        SELECTION_BACKGROUND);
            }
        };
        SwingPanelSections.configureReadOnlyTable(table, "issueListTable", SELECTION_BACKGROUND);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActionState();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int clickedRow = table.rowAtPoint(event.getPoint());
                if (event.getClickCount() == 2 && clickedRow >= 0 && table.isEnabled()) {
                    selectedIssue().ifPresent(issue -> actions.onOpenIssue().accept(issue.id()));
                }
            }
        });
        applyColumnWidths(table);
        return table;
    }

    private void configureFilters() {
        statusFilter.addItem(null);
        for (IssueStatus status : IssueStatus.values()) {
            if (status != IssueStatus.DELETED) {
                statusFilter.addItem(status);
            }
        }
        priorityFilter.addItem(null);
        for (Priority priority : Priority.values()) {
            priorityFilter.addItem(priority);
        }
        statusFilter.setRenderer(new OptionalValueRenderer("All statuses"));
        priorityFilter.setRenderer(new OptionalValueRenderer("All priorities"));
    }

    private IssueSearchRequest currentSearchRequest() {
        return new IssueSearchRequest(
                searchField.getText(),
                selectedStatus(),
                selectedPriority());
    }

    private IssueStatus selectedStatus() {
        return (IssueStatus) statusFilter.getSelectedItem();
    }

    private Priority selectedPriority() {
        return (Priority) priorityFilter.getSelectedItem();
    }

    private Optional<IssueSummary> selectedIssue() {
        return issueRows.selectedIssue(issueTable);
    }

    private void updateActionState() {
        boolean enabled = !busy;
        registerButton.setEnabled(enabled && registerAllowed);
        openButton.setEnabled(enabled && selectedIssue().isPresent());
        deletedIssuesButton.setEnabled(enabled);
        statisticsButton.setEnabled(enabled);
    }

    private void applyColumnWidths(JTable table) {
        SwingPanelSections.applyColumnWidths(table, ISSUE_COLUMN_WIDTHS);
    }

    @FunctionalInterface
    interface PanelConsumer<T> {

        void accept(IssueListPanel panel, T value);
    }

    record IssueListActions(
            PanelConsumer<IssueSearchRequest> onSearch,
            PanelConsumer<IssueRegisterRequest> onRegister,
            LongConsumer onOpenIssue,
            Runnable onDeletedIssues,
            Runnable onStatistics,
            Runnable onBack,
            Runnable onLogout) {

        IssueListActions {
            Objects.requireNonNull(onSearch, "onSearch");
            Objects.requireNonNull(onRegister, "onRegister");
            Objects.requireNonNull(onOpenIssue, "onOpenIssue");
            Objects.requireNonNull(onDeletedIssues, "onDeletedIssues");
            Objects.requireNonNull(onStatistics, "onStatistics");
            Objects.requireNonNull(onBack, "onBack");
            Objects.requireNonNull(onLogout, "onLogout");
        }
    }

    static final class JOptionPaneIssueDialogs implements IssueDialogs {

        @Override
        public Optional<IssueRegisterRequest> requestRegister(IssueListPanel parent) {
            JTextField title = new JTextField();
            JTextArea description = new JTextArea(4, 28);
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            JComboBox<Priority> priority = new JComboBox<>(Priority.values());
            priority.setSelectedItem(Priority.MAJOR);
            JPanel form = SwingPanelSections.formPanel(
                    280,
                    new JLabel("Title"), title,
                    new JLabel("Description"), new JScrollPane(description),
                    new JLabel("Priority"), priority);
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    form,
                    "Register issue",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            return Optional.of(new IssueRegisterRequest(
                    title.getText(),
                    description.getText(),
                    (Priority) priority.getSelectedItem()));
        }
    }

    private static final class OptionalValueRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;
        private final String emptyText;

        private OptionalValueRenderer(String emptyText) {
            this.emptyText = Objects.requireNonNull(emptyText, "emptyText");
        }

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list,
                    value == null ? emptyText : value,
                    index,
                    isSelected,
                    cellHasFocus);
            label.setText(value == null ? emptyText : value.toString());
            return label;
        }
    }
}
