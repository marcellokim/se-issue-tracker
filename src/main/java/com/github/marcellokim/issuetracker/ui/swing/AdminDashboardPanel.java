package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

final class AdminDashboardPanel extends JPanel implements AdminDashboardView {

    private static final long serialVersionUID = 1L;
    private static final String[] PROJECT_COLUMNS = {"ID", "Project", "Members", "Issues"};
    private static final String[] USER_COLUMNS = {"Login ID", "Name", "Role", "Active"};

    private final DefaultTableModel projectTableModel = readOnlyTableModel(PROJECT_COLUMNS);
    private final DefaultTableModel userTableModel = readOnlyTableModel(USER_COLUMNS);
    private final JLabel messageLabel = new JLabel(" ");

    AdminDashboardPanel(
            UserResult user,
            Runnable onAccountManagement,
            Runnable onProjectManagement,
            Runnable onLogout) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(onAccountManagement, "onAccountManagement");
        Objects.requireNonNull(onProjectManagement, "onProjectManagement");
        Objects.requireNonNull(onLogout, "onLogout");

        setName("adminDashboardPanel");
        setLayout(new BorderLayout(SwingStyles.SECTION_GAP, SwingStyles.SECTION_GAP));
        setBackground(SwingStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING));

        add(header(user, onAccountManagement, onProjectManagement, onLogout), BorderLayout.NORTH);
        add(tables(), BorderLayout.CENTER);
    }

    @Override
    public void showDashboard(List<DashboardProjectSummary> projects, List<UserResult> users) {
        Objects.requireNonNull(projects, "projects");
        Objects.requireNonNull(users, "users");
        runOnEdtAndWait(() -> {
            messageLabel.setText(" ");
            replaceRows(projectTableModel, projectRows(projects));
            replaceRows(userTableModel, userRows(users));
        });
    }

    @Override
    public void showError(String message) {
        runOnEdtAndWait(() -> {
            String displayMessage = message == null || message.isBlank()
                    ? "Dashboard data could not be loaded."
                    : message;
            messageLabel.setText(displayMessage);
        });
    }

    private JPanel header(
            UserResult user,
            Runnable onAccountManagement,
            Runnable onProjectManagement,
            Runnable onLogout) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(SwingStyles.SURFACE);
        header.setBorder(SwingStyles.surfaceBorder());

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Admin dashboard");
        title.setName("adminDashboardTitle");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyTitle(title);
        titles.add(title);

        JLabel userLabel = new JLabel(user.name() + " (" + user.role() + ")");
        userLabel.setName("adminDashboardUser");
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyMuted(userLabel);
        titles.add(userLabel);

        messageLabel.setName("adminDashboardMessage");
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageLabel.setForeground(SwingStyles.ERROR_TEXT);
        titles.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        titles.add(messageLabel);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton accountButton = new JButton("Account management");
        accountButton.setName("accountManagementButton");
        accountButton.addActionListener(event -> onAccountManagement.run());
        actions.add(accountButton);

        JButton projectButton = new JButton("Project management");
        projectButton.setName("projectManagementButton");
        projectButton.addActionListener(event -> onProjectManagement.run());
        actions.add(projectButton);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setName("adminLogoutButton");
        logoutButton.addActionListener(event -> onLogout.run());
        actions.add(logoutButton);

        header.add(titles);
        header.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        header.add(actions);
        return header;
    }

    private JPanel tables() {
        JPanel tables = new JPanel(new GridLayout(1, 2, SwingStyles.SECTION_GAP, 0));
        tables.setOpaque(false);
        tables.add(section("Projects", table("adminProjectTable", projectTableModel)));
        tables.add(section("Users", table("adminUserTable", userTableModel)));
        return tables;
    }

    private static JPanel section(String titleText, JTable table) {
        JPanel section = new JPanel(new BorderLayout(0, SwingStyles.ROW_GAP));
        section.setBackground(SwingStyles.SURFACE);
        section.setBorder(SwingStyles.surfaceBorder());

        JLabel title = new JLabel(titleText);
        SwingStyles.applySectionTitle(title);
        section.add(title, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setColumnHeaderView(table.getTableHeader());
        section.add(scrollPane, BorderLayout.CENTER);
        return section;
    }

    private static JTable table(String name, DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setName(name);
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        applyColumnWidths(table);
        return table;
    }

    private static void applyColumnWidths(JTable table) {
        if (table.getColumnCount() != 4) {
            return;
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(72);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(88);
        table.getColumnModel().getColumn(3).setPreferredWidth(112);
    }

    private static Object[][] projectRows(List<DashboardProjectSummary> projects) {
        Object[][] rows = new Object[projects.size()][PROJECT_COLUMNS.length];
        for (int index = 0; index < projects.size(); index++) {
            DashboardProjectSummary project = projects.get(index);
            rows[index] = new Object[]{
                    project.projectId(),
                    project.projectName(),
                    project.memberCount(),
                    project.visibleIssueCount()
            };
        }
        return rows;
    }

    private static Object[][] userRows(List<UserResult> users) {
        Object[][] rows = new Object[users.size()][USER_COLUMNS.length];
        for (int index = 0; index < users.size(); index++) {
            UserResult user = users.get(index);
            rows[index] = new Object[]{
                    user.loginId(),
                    user.name(),
                    user.role().name(),
                    user.active() ? "Yes" : "No"
            };
        }
        return rows;
    }

    private static void replaceRows(DefaultTableModel model, Object[][] rows) {
        model.setRowCount(0);
        for (Object[] row : rows) {
            model.addRow(row);
        }
    }

    private static DefaultTableModel readOnlyTableModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static void runOnEdtAndWait(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while updating Swing dashboard.", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Swing dashboard update failed.", cause);
        }
    }
}
