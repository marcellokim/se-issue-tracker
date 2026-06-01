package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.HistoryResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueWorkflowActions;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

final class IssueDetailPanel extends JPanel implements IssueDetailView {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DEFAULT_ERROR_MESSAGE = "Issue detail failed. Please try again.";
    private static final String ADD_DEPENDENCY_ACTION = "ADD_DEPENDENCY";
    private static final String REMOVE_DEPENDENCY_ACTION = "REMOVE_DEPENDENCY";
    private static final Color SELECTION_BACKGROUND = new Color(219, 234, 254);
    private static final int SUMMARY_SECTION_HEIGHT = 168;
    private static final int ACTION_SECTION_HEIGHT = 150;
    private static final int COMMENT_SECTION_HEIGHT = 220;
    private static final int HISTORY_SECTION_HEIGHT = 170;
    private static final int DEPENDENCY_SECTION_HEIGHT = 180;
    private static final String[] COMMENT_COLUMNS = {
            "ID", "Purpose", "Writer", "Content", "Updated", "Edit", "Delete"
    };
    private static final String[] HISTORY_COLUMNS = {
            "ID", "Action", "By", "Previous", "New", "Message", "Changed"
    };
    private static final String[] DEPENDENCY_COLUMNS = {
            "ID", "Dependency", "Blocking", "Blocked", "Discovered"
    };
    private static final List<ActionSpec> ACTION_SPECS = List.of(
            new ActionSpec("MARK_FIXED", "Mark fixed"),
            new ActionSpec("RESOLVE", "Resolve"),
            new ActionSpec("REJECT_FIX", "Reject fix"),
            new ActionSpec("CLOSE", "Close"),
            new ActionSpec("REOPEN", "Reopen"),
            new ActionSpec("START_ASSIGNMENT", "Start assignment"),
            new ActionSpec("ASSIGN", "Assign"),
            new ActionSpec("REASSIGN_DEV", "Reassign dev"),
            new ActionSpec("CHANGE_TESTER", "Change tester"),
            new ActionSpec("UPDATE_ISSUE", "Edit issue"),
            new ActionSpec("CHANGE_PRIORITY", "Change priority"),
            new ActionSpec(ADD_DEPENDENCY_ACTION, "Add dependency"),
            new ActionSpec(REMOVE_DEPENDENCY_ACTION, "Remove dependency"),
            new ActionSpec("ADD_COMMENT", "Add comment"),
            new ActionSpec("SOFT_DELETE", "Delete issue"));

    private final transient IssueDetailActions actions;
    private final JLabel titleLabel = new JLabel("Issue");
    private final JLabel stateLabel = new JLabel(" ");
    private final JLabel userLabel;
    private final JLabel descriptionLabel = new JLabel(" ");
    private final JLabel reporterLabel = new JLabel("Reporter: -");
    private final JLabel assigneeLabel = new JLabel("Assignee: -");
    private final JLabel verifierLabel = new JLabel("Verifier: -");
    private final JLabel fixerLabel = new JLabel("Fixer: -");
    private final JLabel resolverLabel = new JLabel("Resolver: -");
    private final JLabel messageLabel = new JLabel(" ");
    private final DefaultTableModel commentTableModel = SwingPanelSections.readOnlyTableModel(COMMENT_COLUMNS);
    private final DefaultTableModel historyTableModel = SwingPanelSections.readOnlyTableModel(HISTORY_COLUMNS);
    private final DefaultTableModel dependencyTableModel = SwingPanelSections.readOnlyTableModel(DEPENDENCY_COLUMNS);
    private final JTable commentTable = table(commentTableModel, "issueCommentTable");
    private final JTable historyTable = table(historyTableModel, "issueHistoryTable");
    private final JTable dependencyTable = table(dependencyTableModel, "issueDependencyTable");
    private final JButton addCommentButton = new JButton("Add comment");
    private final JButton editCommentButton = new JButton("Edit selected");
    private final JButton deleteCommentButton = new JButton("Delete selected");
    private final JButton addDependencyButton = new JButton("Add dependency");
    private final JButton removeDependencyButton = new JButton("Remove selected");
    private final Map<String, JButton> actionButtons = new LinkedHashMap<>();
    private boolean busy;
    private Set<String> availableActions = Set.of();
    private transient List<IssueCommentActionState> commentActionStates = List.of();
    private transient List<DependencyResult> dependencyActionStates = List.of();
    private String currentTitle = "Issue";
    private String currentDescription = " ";
    private Priority currentPriority = Priority.MAJOR;

    IssueDetailPanel(UserResult user, IssueDetailActions actions) {
        Objects.requireNonNull(user, "user");
        this.actions = Objects.requireNonNull(actions, "actions");
        this.userLabel = new JLabel(user.name() + " (" + user.role() + ")");

        setName("issueDetailPanel");
        setLayout(new BorderLayout(SwingStyles.SECTION_GAP, SwingStyles.SECTION_GAP));
        setBackground(SwingStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING));

        add(header(), BorderLayout.NORTH);
        add(content(), BorderLayout.CENTER);
        configureCommentActions();
        configureDependencyActions();
        updateActionButtons();
    }

    @Override
    public void showDetail(
            IssueDetailResult detail,
            List<IssueCommentActionState> commentActions,
            List<DependencyResult> projectDependencies) {
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(commentActions, "commentActions");
        Objects.requireNonNull(projectDependencies, "projectDependencies");
        Map<String, IssueCommentActionState> commentActionById = commentActionById(commentActions);
        runOnEdt(() -> {
            titleLabel.setText("[" + detail.issueId() + "] " + detail.title());
            stateLabel.setText(detail.status() + " / " + detail.priority());
            descriptionLabel.setText(detail.description());
            currentTitle = detail.title();
            currentDescription = detail.description();
            currentPriority = detail.priority();
            reporterLabel.setText("Reporter: " + formatUser(detail.reporter()));
            assigneeLabel.setText("Assignee: " + formatUser(detail.assignee()));
            verifierLabel.setText("Verifier: " + formatUser(detail.verifier()));
            fixerLabel.setText("Fixer: " + formatUser(detail.fixer()));
            resolverLabel.setText("Resolver: " + formatUser(detail.resolver()));
            availableActions = Set.copyOf(detail.availableActions());
            commentActionStates = List.copyOf(commentActions);
            dependencyActionStates = List.copyOf(projectDependencies);
            replaceRows(commentTableModel, commentRows(detail.comments(), commentActionById));
            commentTable.clearSelection();
            replaceRows(historyTableModel, historyRows(detail.histories()));
            replaceRows(dependencyTableModel, dependencyRows(projectDependencies));
            dependencyTable.clearSelection();
            updateActionButtons();
            updateCommentButtons();
            updateDependencyButtons();
        });
    }

    @Override
    public void showActions(IssueWorkflowActions actions) {
        Objects.requireNonNull(actions, "actions");
        runOnEdt(() -> {
            availableActions = Set.copyOf(actions.availableActionNames());
            updateActionButtons();
            updateDependencyButtons();
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
            this.busy = busy;
            commentTable.setEnabled(!busy);
            historyTable.setEnabled(!busy);
            dependencyTable.setEnabled(!busy);
            updateActionButtons();
            updateCommentButtons();
            updateDependencyButtons();
        });
    }

    IssueEditContext currentIssueEditContext() {
        return new IssueEditContext(currentTitle, currentDescription, currentPriority);
    }

    private JPanel header() {
        JButton backButton = new JButton("Back");
        backButton.setName("issueDetailBackButton");
        backButton.addActionListener(event -> actions.onBack().run());

        JButton logoutButton = new JButton("Logout");
        logoutButton.setName("issueDetailLogoutButton");
        logoutButton.addActionListener(event -> actions.onLogout().run());
        configureHeaderLabel(titleLabel, "issueDetailTitle", true);
        configureHeaderLabel(stateLabel, "issueDetailState", false);
        configureHeaderLabel(userLabel, "issueDetailUser", false);
        configureHeaderLabel(messageLabel, "issueDetailMessage", false);
        return SwingPanelSections.navigationHeader(
                List.of(titleLabel, stateLabel, userLabel, messageLabel),
                List.of(backButton, logoutButton));
    }

    private JPanel content() {
        JPanel content = new VerticalScrollablePanel();
        content.setBackground(SwingStyles.BACKGROUND);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(constrainedSection(summarySection(), SUMMARY_SECTION_HEIGHT));
        content.add(Box.createVerticalStrut(SwingStyles.SECTION_GAP));
        content.add(constrainedSection(actionSection(), ACTION_SECTION_HEIGHT));
        content.add(Box.createVerticalStrut(SwingStyles.SECTION_GAP));
        content.add(constrainedSection(commentSection(), COMMENT_SECTION_HEIGHT));
        content.add(Box.createVerticalStrut(SwingStyles.SECTION_GAP));
        content.add(constrainedSection(SwingPanelSections.tableSection("History", historyTable), HISTORY_SECTION_HEIGHT));
        content.add(Box.createVerticalStrut(SwingStyles.SECTION_GAP));
        content.add(constrainedSection(dependencySection(), DEPENDENCY_SECTION_HEIGHT));

        return SwingPanelSections.verticalScrollPanel(content);
    }

    private JPanel summarySection() {
        JPanel section = new JPanel();
        section.setBackground(SwingStyles.SURFACE);
        section.setBorder(SwingStyles.surfaceBorder());
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));

        descriptionLabel.setName("issueDetailDescription");
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionLabel.setForeground(SwingStyles.BODY_TEXT);
        section.add(descriptionLabel);
        section.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));

        for (JLabel label : List.of(reporterLabel, assigneeLabel, verifierLabel, fixerLabel, resolverLabel)) {
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            SwingStyles.applyMuted(label);
            section.add(label);
        }
        return section;
    }

    private JPanel actionSection() {
        JPanel section = new JPanel(new BorderLayout(0, SwingStyles.ROW_GAP));
        section.setBackground(SwingStyles.SURFACE);
        section.setBorder(SwingStyles.surfaceBorder());

        JLabel title = new JLabel("Available actions");
        SwingStyles.applySectionTitle(title);
        section.add(title, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, SwingStyles.ROW_GAP, SwingStyles.ROW_GAP));
        buttons.setOpaque(false);
        for (ActionSpec spec : ACTION_SPECS) {
            JButton button = new JButton(spec.label());
            button.setName("issueActionButton_" + spec.action());
            button.addActionListener(event -> publishAction(spec.action()));
            actionButtons.put(spec.action(), button);
            buttons.add(button);
        }
        section.add(buttons, BorderLayout.CENTER);
        return section;
    }

    private JPanel commentSection() {
        JPanel section = new JPanel(new BorderLayout(0, SwingStyles.ROW_GAP));
        section.setBackground(SwingStyles.SURFACE);
        section.setBorder(SwingStyles.surfaceBorder());

        JPanel header = new JPanel(new BorderLayout(SwingStyles.ROW_GAP, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("Comments");
        SwingStyles.applySectionTitle(title);
        header.add(title, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, SwingStyles.ROW_GAP, 0));
        buttons.setOpaque(false);
        addCommentButton.setName("addCommentButton");
        editCommentButton.setName("editCommentButton");
        deleteCommentButton.setName("deleteCommentButton");
        buttons.add(addCommentButton);
        buttons.add(editCommentButton);
        buttons.add(deleteCommentButton);
        header.add(buttons, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(commentTable);
        scrollPane.setColumnHeaderView(commentTable.getTableHeader());
        section.add(header, BorderLayout.NORTH);
        section.add(scrollPane, BorderLayout.CENTER);
        return section;
    }

    private JPanel dependencySection() {
        JPanel section = new JPanel(new BorderLayout(0, SwingStyles.ROW_GAP));
        section.setBackground(SwingStyles.SURFACE);
        section.setBorder(SwingStyles.surfaceBorder());

        JPanel header = new JPanel(new BorderLayout(SwingStyles.ROW_GAP, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("Dependencies");
        SwingStyles.applySectionTitle(title);
        header.add(title, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, SwingStyles.ROW_GAP, 0));
        buttons.setOpaque(false);
        addDependencyButton.setName("addDependencyButton");
        removeDependencyButton.setName("removeDependencyButton");
        buttons.add(addDependencyButton);
        buttons.add(removeDependencyButton);
        header.add(buttons, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(dependencyTable);
        scrollPane.setColumnHeaderView(dependencyTable.getTableHeader());
        section.add(header, BorderLayout.NORTH);
        section.add(scrollPane, BorderLayout.CENTER);
        return section;
    }

    private static JPanel constrainedSection(JPanel section, int height) {
        Dimension size = new Dimension(SwingStyles.WINDOW_SIZE.width - SwingStyles.OUTER_PADDING * 2, height);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMinimumSize(new Dimension(0, height));
        section.setPreferredSize(size);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return section;
    }

    private JTable table(DefaultTableModel model, String name) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                return SwingPanelSections.stripedTableCell(
                        this,
                        super.prepareRenderer(renderer, row, column),
                        row,
                        SELECTION_BACKGROUND);
            }
        };
        SwingPanelSections.configureReadOnlyTable(table, name, SELECTION_BACKGROUND);
        return table;
    }

    private void updateActionButtons() {
        boolean enabled = !busy;
        actionButtons.forEach((action, button) -> {
            boolean canRun = availableActions.contains(action);
            if (REMOVE_DEPENDENCY_ACTION.equals(action)) {
                canRun = canRun && selectedDependency().isPresent();
            }
            button.setEnabled(enabled && canRun);
        });
        addCommentButton.setEnabled(enabled && availableActions.contains("ADD_COMMENT"));
        addDependencyButton.setEnabled(enabled && availableActions.contains(ADD_DEPENDENCY_ACTION));
    }

    private void publishAction(String action) {
        if (ADD_DEPENDENCY_ACTION.equals(action)) {
            actions.onDependencyAction().accept(this, IssueDependencyMode.ADD, null);
            return;
        }
        if (REMOVE_DEPENDENCY_ACTION.equals(action)) {
            selectedDependency().ifPresent(selection -> actions.onDependencyAction().accept(
                    this,
                    IssueDependencyMode.REMOVE,
                    selection));
            return;
        }
        actions.onAction().accept(this, IssueAssignmentActions.effectiveAction(action, availableActions));
    }

    private void configureCommentActions() {
        commentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setColumnWidths(commentTable, 64, 92, 96, 320, 132, 64, 64);
        commentTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateCommentButtons();
            }
        });
        addCommentButton.addActionListener(event ->
                actions.onCommentAction().accept(this, IssueCommentMode.ADD, null));
        editCommentButton.addActionListener(event -> selectedComment()
                .ifPresent(selection -> actions.onCommentAction().accept(this, IssueCommentMode.UPDATE, selection)));
        deleteCommentButton.addActionListener(event -> selectedComment()
                .ifPresent(selection -> actions.onCommentAction().accept(this, IssueCommentMode.DELETE, selection)));
        updateCommentButtons();
    }

    private void configureDependencyActions() {
        dependencyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setColumnWidths(dependencyTable, 64, 160, 120, 120, 132);
        dependencyTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateDependencyButtons();
            }
        });
        addDependencyButton.addActionListener(event ->
                actions.onDependencyAction().accept(this, IssueDependencyMode.ADD, null));
        removeDependencyButton.addActionListener(event -> selectedDependency()
                .ifPresent(selection -> actions.onDependencyAction().accept(
                        this,
                        IssueDependencyMode.REMOVE,
                        selection)));
        updateDependencyButtons();
    }

    private static void setColumnWidths(JTable table, int... widths) {
        for (int index = 0; index < widths.length && index < table.getColumnCount(); index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
    }

    private void updateCommentButtons() {
        Optional<IssueCommentActionState> selected = selectedCommentState();
        editCommentButton.setEnabled(!busy && selected.map(IssueCommentActionState::canUpdate).orElse(false));
        deleteCommentButton.setEnabled(!busy && selected.map(IssueCommentActionState::canDelete).orElse(false));
    }

    private Optional<IssueCommentSelection> selectedComment() {
        return selectedCommentState()
                .filter(state -> state.numericCommentId() != null)
                .map(state -> new IssueCommentSelection(state.numericCommentId(), state.content()));
    }

    private Optional<IssueCommentActionState> selectedCommentState() {
        int selectedRow = commentTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= commentTable.getRowCount()) {
            return Optional.empty();
        }
        int modelRow = commentTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= commentActionStates.size()) {
            return Optional.empty();
        }
        return Optional.of(commentActionStates.get(modelRow));
    }

    private void updateDependencyButtons() {
        boolean enabled = !busy && availableActions.contains(REMOVE_DEPENDENCY_ACTION) && selectedDependency().isPresent();
        removeDependencyButton.setEnabled(enabled);
        JButton removeActionButton = actionButtons.get(REMOVE_DEPENDENCY_ACTION);
        if (removeActionButton != null) {
            removeActionButton.setEnabled(enabled);
        }
    }

    private Optional<IssueDependencySelection> selectedDependency() {
        int selectedRow = dependencyTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= dependencyTable.getRowCount()) {
            return Optional.empty();
        }
        int modelRow = dependencyTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= dependencyActionStates.size()) {
            return Optional.empty();
        }
        DependencyResult dependency = dependencyActionStates.get(modelRow);
        return Optional.of(new IssueDependencySelection(
                dependency.blockingIssueId(),
                dependency.blockedIssueId()));
    }

    private static Map<String, IssueCommentActionState> commentActionById(
            List<IssueCommentActionState> commentActions) {
        Map<String, IssueCommentActionState> byId = new LinkedHashMap<>();
        for (IssueCommentActionState action : commentActions) {
            byId.put(action.displayCommentId(), action);
        }
        return byId;
    }

    private static List<Object[]> commentRows(
            List<CommentResult> comments,
            Map<String, IssueCommentActionState> commentActionById) {
        return comments.stream()
                .map(comment -> {
                    IssueCommentActionState state = commentActionById.get(comment.commentId());
                    return new Object[]{
                            comment.commentId(),
                            comment.purpose(),
                            comment.writerLoginId(),
                            comment.content(),
                            DATE_TIME_FORMATTER.format(comment.updatedDate()),
                            yesNo(state != null && state.canUpdate()),
                            yesNo(state != null && state.canDelete())
                    };
                })
                .toList();
    }

    private static List<Object[]> historyRows(List<HistoryResult> histories) {
        return histories.stream()
                .map(history -> new Object[]{
                        history.id(),
                        history.actionType().toString(),
                        history.changedById(),
                        valueOrDash(history.previousValue()),
                        valueOrDash(history.newValue()),
                        valueOrDash(history.message()),
                        DATE_TIME_FORMATTER.format(history.changedDate())
                })
                .toList();
    }

    private static List<Object[]> dependencyRows(List<DependencyResult> dependencies) {
        return dependencies.stream()
                .map(dependency -> new Object[]{
                        dependency.id(),
                        dependency.dependencyId(),
                        dependency.blockingIssueKey(),
                        dependency.blockedIssueKey(),
                        DATE_TIME_FORMATTER.format(dependency.discoveredDate())
                })
                .toList();
    }

    private static void replaceRows(DefaultTableModel model, List<Object[]> rows) {
        model.setRowCount(0);
        for (Object[] row : rows) {
            model.addRow(row);
        }
    }

    private static String formatUser(UserResult user) {
        return user == null ? "-" : user.loginId() + " (" + user.name() + ")";
    }

    private static String yesNo(boolean value) {
        return value ? "Y" : "N";
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static void configureHeaderLabel(JLabel label, String name, boolean title) {
        label.setName(name);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (title) {
            SwingStyles.applyTitle(label);
            return;
        }
        SwingStyles.applyMuted(label);
    }

    private static void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        SwingUtilities.invokeLater(action);
    }

    @FunctionalInterface
    interface PanelStringConsumer {

        void accept(IssueDetailPanel panel, String value);
    }

    @FunctionalInterface
    interface PanelCommentConsumer {

        void accept(IssueDetailPanel panel, IssueCommentMode mode, IssueCommentSelection selection);
    }

    @FunctionalInterface
    interface PanelDependencyConsumer {

        void accept(IssueDetailPanel panel, IssueDependencyMode mode, IssueDependencySelection selection);
    }

    record IssueDetailActions(
            PanelStringConsumer onAction,
            PanelCommentConsumer onCommentAction,
            PanelDependencyConsumer onDependencyAction,
            Runnable onBack,
            Runnable onLogout) {

        IssueDetailActions(PanelStringConsumer onAction, Runnable onBack, Runnable onLogout) {
            this(onAction, (panel, mode, selection) -> {
            }, (panel, mode, selection) -> {
            }, onBack, onLogout);
        }

        IssueDetailActions(
                PanelStringConsumer onAction,
                PanelCommentConsumer onCommentAction,
                Runnable onBack,
                Runnable onLogout) {
            this(onAction, onCommentAction, (panel, mode, selection) -> {
            }, onBack, onLogout);
        }

        IssueDetailActions {
            Objects.requireNonNull(onAction, "onAction");
            Objects.requireNonNull(onCommentAction, "onCommentAction");
            Objects.requireNonNull(onDependencyAction, "onDependencyAction");
            Objects.requireNonNull(onBack, "onBack");
            Objects.requireNonNull(onLogout, "onLogout");
        }
    }

    private record ActionSpec(String action, String label) {

        private ActionSpec {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(label, "label");
        }
    }

    private static final class VerticalScrollablePanel extends JPanel implements Scrollable {

        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(24, visibleRect.height - 24);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
