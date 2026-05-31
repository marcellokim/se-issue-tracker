package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.ProjectAdminDetail;
import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

final class ProjectDetailPanel extends JPanel implements ProjectDetailView {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] PARTICIPANT_COLUMNS = {"User ID", "Name", "Role", "Active", "Joined"};
    private static final int[] PARTICIPANT_COLUMN_WIDTHS = {120, 180, 100, 80, 160};
    private static final Color SELECTION_BACKGROUND = new Color(219, 234, 254);
    private static final String DEFAULT_ERROR_MESSAGE = "Project detail failed. Please try again.";
    private static final SwingPanelSections.HeaderLabels HEADER_LABELS = new SwingPanelSections.HeaderLabels(
            "Project detail",
            "projectDetailTitle",
            "projectDetailUser",
            "projectDetailMessage",
            "projectDetailBackButton",
            "projectDetailLogoutButton");

    private final long projectId;
    private final transient ProjectDetailDialogs dialogs;
    private final transient ProjectDetailActions actions;
    private final JLabel messageLabel = new JLabel(" ");
    private final JLabel projectIdLabel = valueLabel("projectDetailIdValue");
    private final JLabel projectNameLabel = valueLabel("projectDetailNameValue");
    private final JLabel projectDescriptionLabel = valueLabel("projectDetailDescriptionValue");
    private final JLabel projectManagerLabel = valueLabel("projectDetailManagerValue");
    private final JLabel projectUpdatedAtLabel = valueLabel("projectDetailUpdatedAtValue");
    private final DefaultTableModel participantTableModel = readOnlyTableModel();
    private final JTable participantTable = table();
    private final JButton renameButton = new JButton("Rename");
    private final JButton descriptionButton = new JButton("Change description");
    private final JButton addParticipantButton = new JButton("Add member");
    private final JButton removeParticipantButton = new JButton("Remove member");
    private final List<ProjectMemberResult> participants = new ArrayList<>();
    private transient ProjectResult project;

    ProjectDetailPanel(
            UserResult user,
            long projectId,
            ProjectDetailDialogs dialogs,
            ProjectDetailActions actions) {
        Objects.requireNonNull(user, "user");
        this.projectId = projectId;
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
        this.actions = Objects.requireNonNull(actions, "actions");

        setName("projectDetailPanel");
        setLayout(new BorderLayout(SwingStyles.SECTION_GAP, SwingStyles.SECTION_GAP));
        setBackground(SwingStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING));

        add(header(user), BorderLayout.NORTH);
        add(content(), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);
        updateActionState();
    }

    @Override
    public void showDetail(ProjectAdminDetail detail) {
        Objects.requireNonNull(detail, "detail");
        ProjectResult projectResult = detail.project();
        String idText = String.valueOf(projectResult.id());
        String nameText = projectResult.name();
        String descriptionText = projectResult.description();
        String managerText = projectResult.managedByLoginId();
        String updatedAtText = DATE_TIME_FORMATTER.format(projectResult.updatedAt());
        List<ProjectMemberResult> participantSnapshot = List.copyOf(detail.participants());
        List<Object[]> participantRows = participantRows(participantSnapshot);

        runOnEdt(() -> {
            project = projectResult;
            projectIdLabel.setText(idText);
            projectNameLabel.setText(nameText);
            projectDescriptionLabel.setText(descriptionText);
            projectManagerLabel.setText(managerText);
            projectUpdatedAtLabel.setText(updatedAtText);
            replaceParticipants(participantSnapshot, participantRows);
            updateActionState();
        });
    }

    @Override
    public void showParticipants(List<ProjectMemberResult> participants) {
        Objects.requireNonNull(participants, "participants");
        List<ProjectMemberResult> snapshot = List.copyOf(participants);
        List<Object[]> participantRows = participantRows(snapshot);
        runOnEdt(() -> {
            String selectedUserId = selectedParticipant()
                    .map(ProjectMemberResult::userId)
                    .orElse(null);
            replaceParticipants(snapshot, participantRows);
            restoreSelection(selectedUserId);
            updateActionState();
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
            participantTable.setEnabled(enabled);
            updateActionState(enabled);
        });
    }

    private JPanel header(UserResult user) {
        return SwingPanelSections.managementHeader(
                HEADER_LABELS,
                user,
                messageLabel,
                new SwingPanelSections.NavigationActions(actions.onBack(), actions.onLogout()));
    }

    private JPanel content() {
        JPanel content = new JPanel(new BorderLayout(SwingStyles.SECTION_GAP, SwingStyles.SECTION_GAP));
        content.setOpaque(false);
        content.add(projectSection(), BorderLayout.NORTH);
        content.add(participantSection(), BorderLayout.CENTER);
        return content;
    }

    private JPanel projectSection() {
        JPanel section = new JPanel(new BorderLayout(0, SwingStyles.ROW_GAP));
        section.setBackground(SwingStyles.SURFACE);
        section.setBorder(SwingStyles.surfaceBorder());

        JLabel title = new JLabel("Project");
        SwingStyles.applySectionTitle(title);
        section.add(title, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        addField(fields, 0, "ID", projectIdLabel);
        addField(fields, 1, "Name", projectNameLabel);
        addField(fields, 2, "Description", projectDescriptionLabel);
        addField(fields, 3, "Manager", projectManagerLabel);
        addField(fields, 4, "Updated", projectUpdatedAtLabel);
        section.add(fields, BorderLayout.CENTER);
        return section;
    }

    private JPanel participantSection() {
        JPanel section = new JPanel(new BorderLayout(0, SwingStyles.ROW_GAP));
        section.setBackground(SwingStyles.SURFACE);
        section.setBorder(SwingStyles.surfaceBorder());

        JLabel title = new JLabel("Participants");
        SwingStyles.applySectionTitle(title);
        section.add(title, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(participantTable);
        scrollPane.setColumnHeaderView(participantTable.getTableHeader());
        section.add(scrollPane, BorderLayout.CENTER);
        return section;
    }

    private JPanel actions() {
        JPanel panel = new JPanel();
        panel.setBackground(SwingStyles.SURFACE);
        panel.setBorder(SwingStyles.surfaceBorder());

        renameButton.setName("renameProjectDetailButton");
        renameButton.addActionListener(event -> currentProject()
                .flatMap(current -> dialogs.requestRename(this, current))
                .ifPresent(name -> actions.onRename().accept(this, projectId, name)));
        panel.add(renameButton);

        descriptionButton.setName("changeProjectDetailDescriptionButton");
        descriptionButton.addActionListener(event -> currentProject()
                .flatMap(current -> dialogs.requestDescription(this, current))
                .ifPresent(description -> actions.onDescriptionChange().accept(this, projectId, description)));
        panel.add(descriptionButton);

        addParticipantButton.setName("addProjectParticipantButton");
        addParticipantButton.addActionListener(event -> dialogs.requestParticipantLoginId(this)
                .ifPresent(loginId -> actions.onAddParticipant().accept(this, projectId, loginId)));
        panel.add(addParticipantButton);

        removeParticipantButton.setName("removeProjectParticipantButton");
        removeParticipantButton.addActionListener(event -> selectedParticipant()
                .filter(participant -> dialogs.confirmRemove(this, participant))
                .ifPresent(participant ->
                        actions.onRemoveParticipant().accept(this, projectId, participant.userId())));
        panel.add(removeParticipantButton);
        return panel;
    }

    private JTable table() {
        JTable table = new JTable(participantTableModel) {
            @Override
            public java.awt.Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                return SwingPanelSections.stripedTableCell(
                        this,
                        super.prepareRenderer(renderer, row, column),
                        row,
                        SELECTION_BACKGROUND);
            }
        };
        SwingPanelSections.configureReadOnlyTable(table, "projectParticipantTable", SELECTION_BACKGROUND);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActionState();
            }
        });
        applyColumnWidths(table);
        return table;
    }

    private Optional<ProjectResult> currentProject() {
        return Optional.ofNullable(project);
    }

    private Optional<ProjectMemberResult> selectedParticipant() {
        int selectedRow = participantTable.getSelectedRow();
        if (selectedRow < 0) {
            return Optional.empty();
        }
        int modelRow = participantTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= participants.size()) {
            return Optional.empty();
        }
        return Optional.of(participants.get(modelRow));
    }

    private void replaceParticipants(List<ProjectMemberResult> participants, List<Object[]> participantRows) {
        this.participants.clear();
        this.participants.addAll(participants);
        participantTableModel.setRowCount(0);
        for (Object[] row : participantRows) {
            participantTableModel.addRow(row);
        }
    }

    private static List<Object[]> participantRows(List<ProjectMemberResult> participants) {
        return participants.stream()
                .map(participant -> new Object[]{
                        participant.userId(),
                        participant.userName(),
                        participant.role(),
                        participant.active() ? "Active" : "Inactive",
                        DATE_TIME_FORMATTER.format(participant.joinedAt())
                })
                .toList();
    }

    private void restoreSelection(String selectedUserId) {
        if (selectedUserId == null) {
            return;
        }
        for (int row = 0; row < participants.size(); row++) {
            if (selectedUserId.equals(participants.get(row).userId())) {
                int viewRow = participantTable.convertRowIndexToView(row);
                if (viewRow >= 0) {
                    participantTable.setRowSelectionInterval(viewRow, viewRow);
                }
                return;
            }
        }
    }

    private void updateActionState() {
        updateActionState(participantTable.isEnabled());
    }

    private void updateActionState(boolean enabled) {
        boolean hasProject = enabled && project != null;
        renameButton.setEnabled(hasProject);
        descriptionButton.setEnabled(hasProject);
        addParticipantButton.setEnabled(hasProject);
        removeParticipantButton.setEnabled(hasProject && selectedParticipant().isPresent());
    }

    private static void addField(JPanel panel, int row, String labelText, JLabel value) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 12);
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        JLabel label = new JLabel(labelText);
        SwingStyles.applyMuted(label);
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(value, constraints);
    }

    private void applyColumnWidths(JTable table) {
        if (table.getColumnCount() != PARTICIPANT_COLUMN_WIDTHS.length) {
            return;
        }
        for (int index = 0; index < PARTICIPANT_COLUMN_WIDTHS.length; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(PARTICIPANT_COLUMN_WIDTHS[index]);
        }
    }

    private static JLabel valueLabel(String name) {
        JLabel label = new JLabel(" ");
        label.setName(name);
        label.setForeground(SwingStyles.BODY_TEXT);
        return label;
    }

    private static DefaultTableModel readOnlyTableModel() {
        return new DefaultTableModel(PARTICIPANT_COLUMNS, 0) {
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

    @FunctionalInterface
    interface PanelBiConsumer<T, U> {

        void accept(ProjectDetailPanel panel, T first, U second);
    }

    record ProjectDetailActions(
            PanelBiConsumer<Long, String> onRename,
            PanelBiConsumer<Long, String> onDescriptionChange,
            PanelBiConsumer<Long, String> onAddParticipant,
            PanelBiConsumer<Long, String> onRemoveParticipant,
            Runnable onBack,
            Runnable onLogout) {

        ProjectDetailActions {
            Objects.requireNonNull(onRename, "onRename");
            Objects.requireNonNull(onDescriptionChange, "onDescriptionChange");
            Objects.requireNonNull(onAddParticipant, "onAddParticipant");
            Objects.requireNonNull(onRemoveParticipant, "onRemoveParticipant");
            Objects.requireNonNull(onBack, "onBack");
            Objects.requireNonNull(onLogout, "onLogout");
        }
    }

    static final class JOptionPaneProjectDetailDialogs implements ProjectDetailDialogs {

        @Override
        public Optional<String> requestRename(ProjectDetailPanel parent, ProjectResult project) {
            String name = (String) JOptionPane.showInputDialog(
                    parent,
                    "Name",
                    "Rename project",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    project.name());
            return Optional.ofNullable(name);
        }

        @Override
        public Optional<String> requestDescription(ProjectDetailPanel parent, ProjectResult project) {
            String description = (String) JOptionPane.showInputDialog(
                    parent,
                    "Description",
                    "Change project description",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    project.description());
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> requestParticipantLoginId(ProjectDetailPanel parent) {
            String loginId = JOptionPane.showInputDialog(parent, "Login ID", "Add member", JOptionPane.PLAIN_MESSAGE);
            return Optional.ofNullable(loginId);
        }

        @Override
        public boolean confirmRemove(ProjectDetailPanel parent, ProjectMemberResult selectedParticipant) {
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    "Remove member \"" + selectedParticipant.userId() + "\"?",
                    "Remove member",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            return result == JOptionPane.OK_OPTION;
        }
    }
}
