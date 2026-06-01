package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.StatisticsReport;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.DailyIssueCount;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.service.StatisticsReportResult;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing statistics panel")
class StatisticsPanelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("renders report tables and publishes filter/back/logout actions")
    void rendersReportTablesAndPublishesActions() throws Exception {
        AtomicReference<StatisticsRangeRequest> requestRef = new AtomicReference<>();
        AtomicInteger backClicks = new AtomicInteger();
        AtomicInteger logoutClicks = new AtomicInteger();

        SwingComponentTestSupport.onEdt(() -> {
            StatisticsPanel panel = panel(requestRef, backClicks, logoutClicks);
            panel.showReport(StatisticsReportResult.from(report()));
            panel.showMessage("3 issues", false);

            assertEquals("Statistics", SwingComponentTestSupport.find(panel, "statisticsTitle", JLabel.class)
                    .getText());
            assertEquals("3 issues", SwingComponentTestSupport.find(panel, "statisticsMessage", JLabel.class)
                    .getText());
            assertEquals(2, SwingComponentTestSupport.find(panel, "statisticsStatusTable", JTable.class)
                    .getRowCount());
            assertEquals(IssueStatus.NEW, SwingComponentTestSupport.find(panel, "statisticsStatusTable", JTable.class)
                    .getValueAt(0, 0));
            assertEquals(2, SwingComponentTestSupport.find(panel, "statisticsPriorityTable", JTable.class)
                    .getRowCount());
            assertEquals(1, SwingComponentTestSupport.find(panel, "statisticsDailyTable", JTable.class).getRowCount());
            assertEquals(1, SwingComponentTestSupport.find(panel, "statisticsMonthlyTable", JTable.class).getRowCount());
            JTable monthlyStatusTable = SwingComponentTestSupport.find(
                    panel, "statisticsMonthlyStatusTable", JTable.class);
            assertEquals(visibleStatusCount(), monthlyStatusTable.getRowCount());
            assertEquals(2, countFor(monthlyStatusTable, IssueStatus.NEW));
            assertEquals(0, countFor(monthlyStatusTable, IssueStatus.CLOSED));
            JTable monthlyPriorityTable = SwingComponentTestSupport.find(
                    panel, "statisticsMonthlyPriorityTable", JTable.class);
            assertEquals(Priority.values().length, monthlyPriorityTable.getRowCount());
            assertEquals(1, countFor(monthlyPriorityTable, Priority.CRITICAL));
            assertEquals(0, countFor(monthlyPriorityTable, Priority.MAJOR));
            assertEquals(1, SwingComponentTestSupport.find(panel, "statisticsDailyStatusChangeTable", JTable.class)
                    .getRowCount());
            assertEquals(1, SwingComponentTestSupport.find(panel, "statisticsMonthlyCommentTable", JTable.class)
                    .getRowCount());

            SwingComponentTestSupport.find(panel, "statisticsDailyFromField", JTextField.class)
                    .setText("2026-05-01");
            SwingComponentTestSupport.find(panel, "statisticsDailyToField", JTextField.class)
                    .setText("2026-05-31");
            SwingComponentTestSupport.find(panel, "statisticsMonthlyFromField", JTextField.class)
                    .setText("2026-05");
            SwingComponentTestSupport.find(panel, "statisticsMonthlyToField", JTextField.class)
                    .setText("2026-06");
            SwingComponentTestSupport.find(panel, "loadStatisticsButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "statisticsBackButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "statisticsLogoutButton", JButton.class).doClick();
        });

        assertEquals(
                new StatisticsRangeRequest(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 31),
                        YearMonth.of(2026, 5),
                        YearMonth.of(2026, 6)),
                requestRef.get());
        assertEquals(1, backClicks.get());
        assertEquals(1, logoutClicks.get());
    }

    @Test
    @DisplayName("shows validation errors without publishing a filter request")
    void showsValidationErrorsWithoutPublishingFilterRequest() throws Exception {
        AtomicReference<StatisticsRangeRequest> requestRef = new AtomicReference<>();

        SwingComponentTestSupport.onEdt(() -> {
            StatisticsPanel panel = panel(requestRef, new AtomicInteger(), new AtomicInteger());
            SwingComponentTestSupport.find(panel, "statisticsDailyFromField", JTextField.class)
                    .setText("2026/05/01");
            SwingComponentTestSupport.find(panel, "loadStatisticsButton", JButton.class).doClick();

            assertEquals(
                    "Daily from must use yyyy-MM-dd.",
                    SwingComponentTestSupport.find(panel, "statisticsMessage", JLabel.class).getText());
        });

        assertEquals(null, requestRef.get());
    }

    @Test
    @DisplayName("renders statistics screen snapshot")
    void rendersStatisticsSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            StatisticsPanel panel = panel(new AtomicReference<>(), new AtomicInteger(), new AtomicInteger());
            panel.showReport(StatisticsReportResult.from(report()));
            panel.setSize(SwingStyles.WINDOW_SIZE);
            layoutRecursively(panel);

            BufferedImage image = render(panel, Path.of("build/reports/ui/swing-statistics-view.png"));

            assertTrue(hasRenderedContent(image));
        });
    }

    @Test
    @DisplayName("renders statistics distribution snapshot")
    void rendersDistributionSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            StatisticsPanel panel = panel(new AtomicReference<>(), new AtomicInteger(), new AtomicInteger());
            panel.showReport(StatisticsReportResult.from(report()));
            SwingComponentTestSupport.find(panel, "statisticsTabs", JTabbedPane.class).setSelectedIndex(2);
            panel.setSize(SwingStyles.WINDOW_SIZE);
            layoutRecursively(panel);

            BufferedImage image = render(panel, Path.of("build/reports/ui/swing-statistics-distribution-view.png"));

            assertTrue(hasRenderedContent(image));
        });
    }

    private static StatisticsPanel panel(
            AtomicReference<StatisticsRangeRequest> requestRef,
            AtomicInteger backClicks,
            AtomicInteger logoutClicks) {
        return new StatisticsPanel(
                userResult("dev1", Role.DEV),
                new StatisticsPanel.StatisticsActions(
                        (source, request) -> requestRef.set(request),
                        backClicks::incrementAndGet,
                        logoutClicks::incrementAndGet));
    }

    private static StatisticsReport report() {
        Map<IssueStatus, Integer> statusCounts = new EnumMap<>(IssueStatus.class);
        statusCounts.put(IssueStatus.NEW, 2);
        statusCounts.put(IssueStatus.CLOSED, 1);
        Map<Priority, Integer> priorityCounts = new EnumMap<>(Priority.class);
        priorityCounts.put(Priority.CRITICAL, 1);
        priorityCounts.put(Priority.MAJOR, 2);
        Map<YearMonth, Map<IssueStatus, Integer>> monthlyStatusCounts = Map.of(
                YearMonth.of(2026, 5),
                Map.of(IssueStatus.NEW, 2));
        Map<YearMonth, Map<Priority, Integer>> monthlyPriorityCounts = Map.of(
                YearMonth.of(2026, 5),
                Map.of(Priority.CRITICAL, 1));
        return StatisticsReport.create(
                statusCounts,
                priorityCounts,
                List.of(new DailyIssueCount(LocalDate.of(2026, 5, 31), 3)),
                List.of(new MonthlyIssueCount(YearMonth.of(2026, 5), 3)),
                monthlyStatusCounts,
                monthlyPriorityCounts,
                List.of(new DailyIssueCount(LocalDate.of(2026, 5, 31), 2)),
                List.of(new MonthlyIssueCount(YearMonth.of(2026, 5), 2)),
                List.of(new DailyIssueCount(LocalDate.of(2026, 5, 31), 4)),
                List.of(new MonthlyIssueCount(YearMonth.of(2026, 5), 4)));
    }

    private static UserResult userResult(String loginId, Role role) {
        return UserResult.from(User.fromPersistence(loginId, loginId, "stored-password", role, true, NOW, NOW));
    }

    private static int visibleStatusCount() {
        return (int) Arrays.stream(IssueStatus.values())
                .filter(status -> status != IssueStatus.DELETED)
                .count();
    }

    private static int countFor(JTable table, Object value) {
        for (int row = 0; row < table.getRowCount(); row++) {
            if (value == table.getValueAt(row, 1)) {
                return ((Number) table.getValueAt(row, 2)).intValue();
            }
        }
        throw new AssertionError("Missing row for " + value);
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutRecursively(nested);
            }
        }
    }

    private static BufferedImage render(StatisticsPanel panel, Path output) throws java.io.IOException {
        BufferedImage image = new BufferedImage(
                SwingStyles.WINDOW_SIZE.width,
                SwingStyles.WINDOW_SIZE.height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            panel.printAll(graphics);
        } finally {
            graphics.dispose();
        }
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
        return image;
    }

    private static boolean hasRenderedContent(BufferedImage image) {
        int background = SwingStyles.BACKGROUND.getRGB();
        int white = Color.WHITE.getRGB();
        int contentPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                if (pixel != background && pixel != white) {
                    contentPixels++;
                }
            }
        }
        return contentPixels > 10_000;
    }
}
