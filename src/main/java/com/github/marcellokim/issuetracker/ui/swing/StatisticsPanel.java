package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.service.DailyCountResult;
import com.github.marcellokim.issuetracker.service.MonthlyCountResult;
import com.github.marcellokim.issuetracker.service.StatisticsReportResult;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

final class StatisticsPanel extends JPanel implements StatisticsView {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_ERROR_MESSAGE = "Statistics failed. Please try again.";
    private static final Color SELECTION_BACKGROUND = new Color(219, 234, 254);
    private static final String COUNT_LABEL = "Count";
    private static final String[] COUNT_COLUMNS = {"Value", COUNT_LABEL};
    private static final String[] DATE_COUNT_COLUMNS = {"Date", COUNT_LABEL};
    private static final String[] MONTH_COUNT_COLUMNS = {"Month", COUNT_LABEL};
    private static final String[] MONTHLY_COUNT_COLUMNS = {"Month", "Value", COUNT_LABEL};
    private static final List<IssueStatus> VISIBLE_STATUSES = Arrays.stream(IssueStatus.values())
            .filter(status -> status != IssueStatus.DELETED)
            .toList();
    private static final List<Priority> PRIORITY_VALUES = Arrays.asList(Priority.values());

    private final transient StatisticsActions actions;
    private final JLabel messageLabel = new JLabel(" ");
    private final JTextField dailyFromField = new JTextField();
    private final JTextField dailyToField = new JTextField();
    private final JTextField monthlyFromField = new JTextField();
    private final JTextField monthlyToField = new JTextField();
    private final JButton loadButton = new JButton("Apply filter");
    private final DefaultTableModel statusModel = readOnlyTableModel(COUNT_COLUMNS);
    private final DefaultTableModel priorityModel = readOnlyTableModel(COUNT_COLUMNS);
    private final DefaultTableModel dailyModel = readOnlyTableModel(DATE_COUNT_COLUMNS);
    private final DefaultTableModel monthlyModel = readOnlyTableModel(MONTH_COUNT_COLUMNS);
    private final DefaultTableModel monthlyStatusModel = readOnlyTableModel(MONTHLY_COUNT_COLUMNS);
    private final DefaultTableModel monthlyPriorityModel = readOnlyTableModel(MONTHLY_COUNT_COLUMNS);
    private final DefaultTableModel dailyStatusChangeModel = readOnlyTableModel(DATE_COUNT_COLUMNS);
    private final DefaultTableModel monthlyStatusChangeModel = readOnlyTableModel(MONTH_COUNT_COLUMNS);
    private final DefaultTableModel dailyCommentModel = readOnlyTableModel(DATE_COUNT_COLUMNS);
    private final DefaultTableModel monthlyCommentModel = readOnlyTableModel(MONTH_COUNT_COLUMNS);
    private final JTable statusTable = table("statisticsStatusTable", statusModel);
    private final JTable priorityTable = table("statisticsPriorityTable", priorityModel);
    private final JTable dailyTable = table("statisticsDailyTable", dailyModel);
    private final JTable monthlyTable = table("statisticsMonthlyTable", monthlyModel);
    private final JTable monthlyStatusTable = table("statisticsMonthlyStatusTable", monthlyStatusModel);
    private final JTable monthlyPriorityTable = table("statisticsMonthlyPriorityTable", monthlyPriorityModel);
    private final JTable dailyStatusChangeTable = table("statisticsDailyStatusChangeTable", dailyStatusChangeModel);
    private final JTable monthlyStatusChangeTable = table("statisticsMonthlyStatusChangeTable", monthlyStatusChangeModel);
    private final JTable dailyCommentTable = table("statisticsDailyCommentTable", dailyCommentModel);
    private final JTable monthlyCommentTable = table("statisticsMonthlyCommentTable", monthlyCommentModel);
    private boolean busy;

    StatisticsPanel(UserResult user, StatisticsActions actions) {
        Objects.requireNonNull(user, "user");
        this.actions = Objects.requireNonNull(actions, "actions");

        setName("statisticsPanel");
        setLayout(new BorderLayout(SwingStyles.SECTION_GAP, SwingStyles.SECTION_GAP));
        setBackground(SwingStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING));

        add(topSection(user), BorderLayout.NORTH);
        add(tabs(), BorderLayout.CENTER);
    }

    @Override
    public void showReport(StatisticsReportResult report) {
        showReport(report, () -> true);
    }

    void showReport(StatisticsReportResult report, BooleanSupplier shouldApply) {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(shouldApply, "shouldApply");
        List<Object[]> statusRows = statusRows(report.statusCounts());
        List<Object[]> priorityRows = priorityRows(report.priorityCounts());
        List<Object[]> dailyRows = dailyRows(report.dailyCounts());
        List<Object[]> monthlyRows = monthlyRows(report.monthlyCounts());
        List<Object[]> monthlyStatusRows = monthlyStatusRows(report.monthlyStatusCounts());
        List<Object[]> monthlyPriorityRows = monthlyPriorityRows(report.monthlyPriorityCounts());
        List<Object[]> dailyStatusChangeRows = dailyRows(report.dailyStatusChangeCounts());
        List<Object[]> monthlyStatusChangeRows = monthlyRows(report.monthlyStatusChangeCounts());
        List<Object[]> dailyCommentRows = dailyRows(report.dailyCommentCounts());
        List<Object[]> monthlyCommentRows = monthlyRows(report.monthlyCommentCounts());

        runOnEdt(() -> {
            if (!shouldApply.getAsBoolean()) {
                return;
            }
            replaceRows(statusModel, statusRows);
            replaceRows(priorityModel, priorityRows);
            replaceRows(dailyModel, dailyRows);
            replaceRows(monthlyModel, monthlyRows);
            replaceRows(monthlyStatusModel, monthlyStatusRows);
            replaceRows(monthlyPriorityModel, monthlyPriorityRows);
            replaceRows(dailyStatusChangeModel, dailyStatusChangeRows);
            replaceRows(monthlyStatusChangeModel, monthlyStatusChangeRows);
            replaceRows(dailyCommentModel, dailyCommentRows);
            replaceRows(monthlyCommentModel, monthlyCommentRows);
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
            boolean enabled = !busy;
            dailyFromField.setEnabled(enabled);
            dailyToField.setEnabled(enabled);
            monthlyFromField.setEnabled(enabled);
            monthlyToField.setEnabled(enabled);
            loadButton.setEnabled(enabled);
        });
    }

    private JPanel topSection(UserResult user) {
        JPanel top = new JPanel(new BorderLayout(0, SwingStyles.SECTION_GAP));
        top.setOpaque(false);
        top.add(SwingPanelSections.managementHeader(
                new SwingPanelSections.HeaderLabels(
                        "Statistics",
                        "statisticsTitle",
                        "statisticsUser",
                        "statisticsMessage",
                        "statisticsBackButton",
                        "statisticsLogoutButton"),
                user,
                messageLabel,
                new SwingPanelSections.NavigationActions(actions.onBack(), actions.onLogout())), BorderLayout.NORTH);
        top.add(filterPanel(), BorderLayout.SOUTH);
        return top;
    }

    private JPanel filterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SwingStyles.SURFACE);
        panel.setBorder(SwingStyles.surfaceBorder());

        dailyFromField.setName("statisticsDailyFromField");
        dailyToField.setName("statisticsDailyToField");
        monthlyFromField.setName("statisticsMonthlyFromField");
        monthlyToField.setName("statisticsMonthlyToField");
        dailyFromField.setColumns(10);
        dailyToField.setColumns(10);
        monthlyFromField.setColumns(8);
        monthlyToField.setColumns(8);
        loadButton.setName("loadStatisticsButton");
        loadButton.addActionListener(event -> publishRangeRequest());
        SwingStyles.applySecondaryButton(loadButton);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        addFilter(panel, constraints, 0, "Daily from", dailyFromField);
        addFilter(panel, constraints, 2, "Daily to", dailyToField);
        addFilter(panel, constraints, 4, "Monthly from", monthlyFromField);
        addFilter(panel, constraints, 6, "Monthly to", monthlyToField);
        constraints.gridx = 8;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        panel.add(loadButton, constraints);
        return panel;
    }

    private void publishRangeRequest() {
        if (busy) {
            return;
        }
        try {
            actions.onLoad().accept(this, StatisticsRangeRequest.parse(
                    dailyFromField.getText(),
                    dailyToField.getText(),
                    monthlyFromField.getText(),
                    monthlyToField.getText()));
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage(), true);
        }
    }

    private JTabbedPane tabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("statisticsTabs");
        tabs.addTab("Summary", pairedTables("Status", statusTable, "Priority", priorityTable));
        tabs.addTab("Created", pairedTables("Daily created", dailyTable, "Monthly created", monthlyTable));
        tabs.addTab("Distribution", pairedTables(
                "Monthly status",
                monthlyStatusTable,
                "Monthly priority",
                monthlyPriorityTable));
        tabs.addTab("Activity", activityTables());
        return tabs;
    }

    private JPanel pairedTables(String firstTitle, JTable first, String secondTitle, JTable second) {
        JPanel panel = new JPanel(new GridLayout(1, 2, SwingStyles.SECTION_GAP, SwingStyles.SECTION_GAP));
        panel.setBackground(SwingStyles.SURFACE);
        panel.add(tableSection(firstTitle, first));
        panel.add(tableSection(secondTitle, second));
        return panel;
    }

    private JPanel activityTables() {
        JPanel panel = new JPanel(new GridLayout(2, 2, SwingStyles.SECTION_GAP, SwingStyles.SECTION_GAP));
        panel.setBackground(SwingStyles.SURFACE);
        panel.add(tableSection("Daily status changes", dailyStatusChangeTable));
        panel.add(tableSection("Monthly status changes", monthlyStatusChangeTable));
        panel.add(tableSection("Daily comments", dailyCommentTable));
        panel.add(tableSection("Monthly comments", monthlyCommentTable));
        return panel;
    }

    private JPanel tableSection(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SwingStyles.SURFACE);
        panel.setBorder(SwingStyles.surfaceBorder());
        JLabel titleLabel = new JLabel(title);
        SwingStyles.applySectionTitle(titleLabel);
        panel.add(titleLabel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setColumnHeaderView(table.getTableHeader());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JTable table(String name, DefaultTableModel model) {
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

    private static void addFilter(
            JPanel panel,
            GridBagConstraints constraints,
            int column,
            String labelText,
            JTextField field) {
        constraints.gridx = column;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        panel.add(new JLabel(labelText), constraints);
        constraints.gridx = column + 1;
        constraints.weightx = 1.0;
        panel.add(field, constraints);
    }

    private static List<Object[]> statusRows(Map<IssueStatus, Integer> counts) {
        List<Object[]> rows = new ArrayList<>();
        for (IssueStatus status : VISIBLE_STATUSES) {
            Integer count = counts.get(status);
            if (count != null) {
                rows.add(new Object[]{status, count});
            }
        }
        return rows;
    }

    private static List<Object[]> priorityRows(Map<Priority, Integer> counts) {
        List<Object[]> rows = new ArrayList<>();
        for (Priority priority : PRIORITY_VALUES) {
            Integer count = counts.get(priority);
            if (count != null) {
                rows.add(new Object[]{priority, count});
            }
        }
        return rows;
    }

    private static List<Object[]> dailyRows(List<DailyCountResult> counts) {
        return counts.stream()
                .map(count -> new Object[]{count.date(), count.count()})
                .toList();
    }

    private static List<Object[]> monthlyRows(List<MonthlyCountResult> counts) {
        return counts.stream()
                .map(count -> new Object[]{count.month(), count.count()})
                .toList();
    }

    private static List<Object[]> monthlyStatusRows(Map<YearMonth, Map<IssueStatus, Integer>> counts) {
        return monthlyValueRows(counts, VISIBLE_STATUSES);
    }

    private static List<Object[]> monthlyPriorityRows(Map<YearMonth, Map<Priority, Integer>> counts) {
        return monthlyValueRows(counts, PRIORITY_VALUES);
    }

    private static <T> List<Object[]> monthlyValueRows(Map<YearMonth, Map<T, Integer>> counts, List<T> values) {
        List<Object[]> rows = new ArrayList<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    for (T value : values) {
                        rows.add(new Object[]{entry.getKey(), value, entry.getValue().getOrDefault(value, 0)});
                    }
                });
        return rows;
    }

    private static void replaceRows(DefaultTableModel model, List<Object[]> rows) {
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

    private static void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        SwingUtilities.invokeLater(action);
    }

    @FunctionalInterface
    interface PanelConsumer<T> {

        void accept(StatisticsPanel panel, T value);
    }

    record StatisticsActions(
            PanelConsumer<StatisticsRangeRequest> onLoad,
            Runnable onBack,
            Runnable onLogout) {

        StatisticsActions {
            Objects.requireNonNull(onLoad, "onLoad");
            Objects.requireNonNull(onBack, "onBack");
            Objects.requireNonNull(onLogout, "onLogout");
        }
    }
}
