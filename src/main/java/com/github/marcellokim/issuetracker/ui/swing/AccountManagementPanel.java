package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

final class AccountManagementPanel extends JPanel implements AccountManagementView {

    private static final long serialVersionUID = 1L;
    private static final String[] USER_COLUMNS = {"Login ID", "Name", "Role", "Active"};
    private static final int[] USER_COLUMN_WIDTHS = {120, 180, 96, 72};
    private static final SwingPanelSections.HeaderLabels HEADER_LABELS = new SwingPanelSections.HeaderLabels(
            "Account management",
            "accountManagementTitle",
            "accountManagementUser",
            "accountManagementMessage",
            "accountBackButton",
            "accountLogoutButton");

    private final AccountDialogs dialogs;
    private final PanelConsumer<AccountCreateRequest> onCreate;
    private final PanelBiConsumer<String, String> onRename;
    private final PanelBiConsumer<String, Role> onRoleChange;
    private final PanelConsumer<String> onActivate;
    private final PanelConsumer<String> onDeactivate;
    private final DefaultTableModel userTableModel = readOnlyTableModel();
    private final JTable userTable = table();
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton createButton = new JButton("Create account");
    private final JButton renameButton = new JButton("Rename");
    private final JButton roleButton = new JButton("Change role");
    private final JButton activateButton = new JButton("Activate");
    private final JButton deactivateButton = new JButton("Deactivate");
    private final List<UserResult> users = new ArrayList<>();

    AccountManagementPanel(
            UserResult user,
            AccountDialogs dialogs,
            PanelConsumer<AccountCreateRequest> onCreate,
            PanelBiConsumer<String, String> onRename,
            PanelBiConsumer<String, Role> onRoleChange,
            PanelConsumer<String> onActivate,
            PanelConsumer<String> onDeactivate,
            Runnable onBack,
            Runnable onLogout) {
        Objects.requireNonNull(user, "user");
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
        this.onCreate = Objects.requireNonNull(onCreate, "onCreate");
        this.onRename = Objects.requireNonNull(onRename, "onRename");
        this.onRoleChange = Objects.requireNonNull(onRoleChange, "onRoleChange");
        this.onActivate = Objects.requireNonNull(onActivate, "onActivate");
        this.onDeactivate = Objects.requireNonNull(onDeactivate, "onDeactivate");
        Objects.requireNonNull(onBack, "onBack");
        Objects.requireNonNull(onLogout, "onLogout");

        setName("accountManagementPanel");
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
    public void showUsers(List<UserResult> users) {
        Objects.requireNonNull(users, "users");
        List<UserResult> snapshot = List.copyOf(users);
        runOnEdt(() -> {
            String selectedLoginId = selectedUser()
                    .map(UserResult::loginId)
                    .orElse(null);
            this.users.clear();
            this.users.addAll(snapshot);
            replaceRows(snapshot);
            restoreSelection(selectedLoginId);
            updateSelectionActions();
        });
    }

    @Override
    public void showMessage(String message, boolean error) {
        String displayMessage = message == null || message.isBlank() ? " " : message;
        runOnEdt(() -> {
            messageLabel.setText(displayMessage);
            messageLabel.setForeground(error ? SwingStyles.ERROR_TEXT : SwingStyles.MUTED_TEXT);
        });
    }

    void setBusy(boolean busy) {
        runOnEdt(() -> {
            boolean enabled = !busy;
            userTable.setEnabled(enabled);
            createButton.setEnabled(enabled);
            updateSelectionActions(enabled);
        });
    }

    private JPanel header(UserResult user, Runnable onBack, Runnable onLogout) {
        return SwingPanelSections.managementHeader(
                HEADER_LABELS,
                user,
                messageLabel,
                new SwingPanelSections.NavigationActions(onBack, onLogout));
    }

    private JPanel tableSection() {
        JPanel section = new JPanel(new BorderLayout(0, SwingStyles.ROW_GAP));
        section.setBackground(SwingStyles.SURFACE);
        section.setBorder(SwingStyles.surfaceBorder());

        JLabel title = new JLabel("Users");
        SwingStyles.applySectionTitle(title);
        section.add(title, BorderLayout.NORTH);
        section.add(new JScrollPane(userTable), BorderLayout.CENTER);
        return section;
    }

    private JPanel actions() {
        JPanel panel = new JPanel();
        panel.setBackground(SwingStyles.SURFACE);
        panel.setBorder(SwingStyles.surfaceBorder());

        createButton.setName("createAccountButton");
        createButton.addActionListener(event -> dialogs.requestCreate(this)
                .ifPresent(request -> onCreate.accept(this, request)));
        panel.add(createButton);

        renameButton.setName("renameAccountButton");
        renameButton.addActionListener(event -> selectedUser().flatMap(user -> dialogs.requestRename(this, user)
                .map(name -> new RenameRequest(user.loginId(), name)))
                .ifPresent(request -> onRename.accept(this, request.loginId(), request.name())));
        panel.add(renameButton);

        roleButton.setName("changeRoleButton");
        roleButton.addActionListener(event -> selectedUser().flatMap(user -> dialogs.requestRole(this, user)
                .map(role -> new RoleChangeRequest(user.loginId(), role)))
                .ifPresent(request -> onRoleChange.accept(this, request.loginId(), request.role())));
        panel.add(roleButton);

        activateButton.setName("activateAccountButton");
        activateButton.addActionListener(event -> selectedUser()
                .filter(user -> dialogs.confirmActivation(this, user, true))
                .ifPresent(user -> onActivate.accept(this, user.loginId())));
        panel.add(activateButton);

        deactivateButton.setName("deactivateAccountButton");
        deactivateButton.addActionListener(event -> selectedUser()
                .filter(user -> dialogs.confirmActivation(this, user, false))
                .ifPresent(user -> onDeactivate.accept(this, user.loginId())));
        panel.add(deactivateButton);

        return panel;
    }

    private JTable table() {
        JTable table = new JTable(userTableModel);
        table.setName("accountUserTable");
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionActions();
            }
        });
        applyColumnWidths(table);
        return table;
    }

    private Optional<UserResult> selectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            return Optional.empty();
        }
        int modelRow = userTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= users.size()) {
            return Optional.empty();
        }
        return Optional.of(users.get(modelRow));
    }

    private void replaceRows(List<UserResult> users) {
        userTableModel.setRowCount(0);
        for (UserResult user : users) {
            userTableModel.addRow(new Object[]{
                    user.loginId(),
                    user.name(),
                    user.role().name(),
                    user.active() ? "Yes" : "No"
            });
        }
    }

    private void restoreSelection(String selectedLoginId) {
        if (selectedLoginId == null) {
            return;
        }
        for (int row = 0; row < users.size(); row++) {
            if (selectedLoginId.equals(users.get(row).loginId())) {
                int viewRow = userTable.convertRowIndexToView(row);
                if (viewRow >= 0) {
                    userTable.setRowSelectionInterval(viewRow, viewRow);
                }
                return;
            }
        }
    }

    private void updateSelectionActions() {
        updateSelectionActions(userTable.isEnabled());
    }

    private void updateSelectionActions(boolean enabled) {
        Optional<UserResult> selected = selectedUser();
        boolean hasSelection = enabled && selected.isPresent();
        boolean selectedActive = selected.map(UserResult::active).orElse(false);
        renameButton.setEnabled(hasSelection);
        roleButton.setEnabled(hasSelection);
        activateButton.setEnabled(hasSelection && !selectedActive);
        deactivateButton.setEnabled(hasSelection && selectedActive);
    }

    private static DefaultTableModel readOnlyTableModel() {
        return new DefaultTableModel(USER_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static void applyColumnWidths(JTable table) {
        for (int index = 0; index < USER_COLUMN_WIDTHS.length; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(USER_COLUMN_WIDTHS[index]);
        }
    }

    private static void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        SwingUtilities.invokeLater(action);
    }

    private record RenameRequest(String loginId, String name) {
    }

    private record RoleChangeRequest(String loginId, Role role) {
    }

    @FunctionalInterface
    interface PanelConsumer<T> {

        void accept(AccountManagementPanel panel, T value);
    }

    @FunctionalInterface
    interface PanelBiConsumer<T, U> {

        void accept(AccountManagementPanel panel, T first, U second);
    }

    static final class JOptionPaneAccountDialogs implements AccountDialogs {

        @Override
        public Optional<AccountCreateRequest> requestCreate(AccountManagementPanel parent) {
            JTextField loginId = new JTextField();
            JTextField name = new JTextField();
            JPasswordField password = new JPasswordField();
            JComboBox<Role> role = new JComboBox<>(manageableRoles());
            JPanel form = SwingPanelSections.formPanel(
                    220,
                    new JLabel("Login ID"), loginId,
                    new JLabel("Name"), name,
                    new JLabel("Password"), password,
                    new JLabel("Role"), role);
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    form,
                    "Create account",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            char[] passwordChars = password.getPassword();
            try {
                return Optional.of(new AccountCreateRequest(
                        loginId.getText(),
                        name.getText(),
                        new String(passwordChars),
                        (Role) role.getSelectedItem()));
            } finally {
                Arrays.fill(passwordChars, '\0');
            }
        }

        @Override
        public Optional<String> requestRename(AccountManagementPanel parent, UserResult selectedUser) {
            String name = (String) JOptionPane.showInputDialog(
                    parent,
                    "Name",
                    "Rename account",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    selectedUser.name());
            return Optional.ofNullable(name);
        }

        @Override
        public Optional<Role> requestRole(AccountManagementPanel parent, UserResult selectedUser) {
            JComboBox<Role> role = new JComboBox<>(manageableRoles());
            role.setSelectedItem(selectedUser.role());
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    role,
                    "Change role",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            return Optional.of((Role) role.getSelectedItem());
        }

        @Override
        public boolean confirmActivation(AccountManagementPanel parent, UserResult selectedUser, boolean active) {
            String action = active ? "activate" : "deactivate";
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    "Do you want to " + action + " " + selectedUser.loginId() + "?",
                    active ? "Activate account" : "Deactivate account",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            return result == JOptionPane.OK_OPTION;
        }

        private static Role[] manageableRoles() {
            return Arrays.stream(Role.values())
                    .filter(role -> role != Role.ADMIN)
                    .toArray(Role[]::new);
        }
    }
}
