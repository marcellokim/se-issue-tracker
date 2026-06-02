package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.StatisticsController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.service.DailyCountResult;
import com.github.marcellokim.issuetracker.service.MonthlyCountResult;
import com.github.marcellokim.issuetracker.service.StatisticsReportResult;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import javafx.concurrent.Task;
import javafx.geometry.Side;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

final class StatisticsScreen extends VBox {

    private final StatisticsController statisticsController;
    private final long projectId;
    private final VBox contentArea = new VBox(12);
    private final Label messageLabel = ScreenComponents.messageLabel();
    private StatisticsReportResult report;
    private final DatePicker dailyFromPicker = new DatePicker();
    private final DatePicker dailyToPicker = new DatePicker();
    private final TextField monthlyFromField = new TextField();
    private final TextField monthlyToField = new TextField();
    private Runnable onBack;

    StatisticsScreen(StatisticsController statisticsController, long projectId){
        this.statisticsController = statisticsController;
        this.projectId = projectId;
        ScreenComponents.applyScreenDefaults(this);

        Button backButton = ScreenComponents.backButton("← Issues", () -> { if (onBack != null) onBack.run(); });
        Label titleLabel = ScreenComponents.titleLabel("Statistics");

        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        getChildren().addAll(
                ScreenComponents.header(backButton, titleLabel),
                buildFilterBar(),
                messageLabel, scrollPane);
        loadOverview();
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private void loadOverview(){
        loadOverview(null, null, null, null);
    }

    private void loadOverview(LocalDate dailyFrom, LocalDate dailyTo, YearMonth monthlyFrom, YearMonth monthlyTo){
        ScreenComponents.showInfo(messageLabel, "Loading statistics...");
        Task<StatisticsReportResult> task = new Task<>(){
            @Override protected StatisticsReportResult call(){
                return statisticsController.viewStatistics(projectId, dailyFrom, dailyTo, monthlyFrom, monthlyTo);
            }
        };
        task.setOnSucceeded(event -> {
            report = task.getValue();
            showOverview();
        });
        task.setOnFailed(event -> ScreenComponents.showError(messageLabel,
                new RuntimeException("Statistics loading failed.")));
        new Thread(task).start();
    }

    private void showOverview(){
        contentArea.getChildren().clear();

        PieChart statusPie = new PieChart();
        statusPie.setTitle("Issues by Status");
        statusPie.setLegendSide(Side.BOTTOM);
        statusPie.setPrefHeight(280);
        for (IssueStatus s : IssueStatus.values()){
            if (s == IssueStatus.DELETED) continue;
            int count = report.statusCounts().getOrDefault(s, 0);
            statusPie.getData().add(new PieChart.Data(s + " (" + count + ")", count));
        }

        BarChart<String, Number> priorityBar = createBarChart("Issues by Priority", "Priority", "Count");
        priorityBar.setPrefHeight(250);
        CategoryAxis priorityAxis = (CategoryAxis) priorityBar.getXAxis();
        for (Priority p : Priority.values()) priorityAxis.getCategories().add(p.name());
        XYChart.Series<String, Number> prioritySeries = new XYChart.Series<>();
        prioritySeries.setName("Issues");
        for (Priority p : Priority.values()){
            int count = report.priorityCounts().getOrDefault(p, 0);
            prioritySeries.getData().add(new XYChart.Data<>(p.name(), count));
        }
        priorityBar.getData().add(prioritySeries);

        HBox detailButtons = new HBox(8);
        detailButtons.getChildren().addAll(
                detailButton("Daily Issues", this::showDailyIssues),
                detailButton("Monthly Issues", this::showMonthlyIssues),
                detailButton("Monthly Breakdown", this::showMonthlyBreakdown),
                detailButton("Status Changes", this::showStatusChanges),
                detailButton("Comment Activity", this::showCommentActivity));

        contentArea.getChildren().addAll(
                statusPie, new Separator(),
                priorityBar, new Separator(),
                new Label("Detail Views:"), detailButtons);
    }

    private void showDailyIssues(){
        contentArea.getChildren().clear();
        contentArea.getChildren().add(backToOverviewButton());
        contentArea.getChildren().add(buildDailyBarChart("Daily Issue Registrations", report.dailyCounts()));
    }

    private void showMonthlyIssues(){
        contentArea.getChildren().clear();
        contentArea.getChildren().add(backToOverviewButton());
        contentArea.getChildren().add(buildMonthlyBarChart("Monthly Issue Registrations", report.monthlyCounts()));
    }

    private void showMonthlyBreakdown(){
        contentArea.getChildren().clear();
        contentArea.getChildren().add(backToOverviewButton());

        if (!report.monthlyStatusCounts().isEmpty()){
            Label statusTitle = new Label("Monthly Status Breakdown");
            statusTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            BarChart<String, Number> statusChart = createBarChart("", "Month", "Count");
            statusChart.setPrefHeight(300);
            CategoryAxis statusXAxis = (CategoryAxis) statusChart.getXAxis();
            for (YearMonth ym : report.monthlyStatusCounts().keySet()) statusXAxis.getCategories().add(ym.toString());
            for (IssueStatus st : IssueStatus.values()){
                if (st == IssueStatus.DELETED) continue;
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(st.name());
                for (Map.Entry<YearMonth, Map<IssueStatus, Integer>> monthEntry : report.monthlyStatusCounts().entrySet()){
                    int count = monthEntry.getValue().getOrDefault(st, 0);
                    series.getData().add(new XYChart.Data<>(monthEntry.getKey().toString(), count));
                }
                statusChart.getData().add(series);
            }
            contentArea.getChildren().addAll(statusTitle, statusChart, new Separator());
        }

        if (!report.monthlyPriorityCounts().isEmpty()){
            Label priorityTitle = new Label("Monthly Priority Breakdown");
            priorityTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            BarChart<String, Number> priorityChart = createBarChart("", "Month", "Count");
            priorityChart.setPrefHeight(300);
            CategoryAxis prioXAxis = (CategoryAxis) priorityChart.getXAxis();
            for (YearMonth ym : report.monthlyPriorityCounts().keySet()) prioXAxis.getCategories().add(ym.toString());
            for (Priority p : Priority.values()){
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(p.name());
                for (Map.Entry<YearMonth, Map<Priority, Integer>> monthEntry : report.monthlyPriorityCounts().entrySet()){
                    int count = monthEntry.getValue().getOrDefault(p, 0);
                    series.getData().add(new XYChart.Data<>(monthEntry.getKey().toString(), count));
                }
                priorityChart.getData().add(series);
            }
            contentArea.getChildren().addAll(priorityTitle, priorityChart);
        }
    }

    private void showStatusChanges(){
        contentArea.getChildren().clear();
        contentArea.getChildren().add(backToOverviewButton());
        contentArea.getChildren().add(buildDailyBarChart("Daily Status Changes", report.dailyStatusChangeCounts()));
        contentArea.getChildren().add(new Separator());
        contentArea.getChildren().add(buildMonthlyBarChart("Monthly Status Changes", report.monthlyStatusChangeCounts()));
    }

    private void showCommentActivity(){
        contentArea.getChildren().clear();
        contentArea.getChildren().add(backToOverviewButton());
        contentArea.getChildren().add(buildDailyBarChart("Daily Comments", report.dailyCommentCounts()));
        contentArea.getChildren().add(new Separator());
        contentArea.getChildren().add(buildMonthlyBarChart("Monthly Comments", report.monthlyCommentCounts()));
    }

    private BarChart<String, Number> buildDailyBarChart(String title, List<DailyCountResult> data){
        BarChart<String, Number> chart = createBarChart(title, "Date", "Count");
        chart.setPrefHeight(300);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(title);
        for (DailyCountResult d : data){
            series.getData().add(new XYChart.Data<>(d.date().toString(), d.count()));
        }
        chart.getData().add(series);
        chart.setLegendVisible(false);
        return chart;
    }

    private BarChart<String, Number> buildMonthlyBarChart(String title, List<MonthlyCountResult> data){
        BarChart<String, Number> chart = createBarChart(title, "Month", "Count");
        chart.setPrefHeight(300);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(title);
        for (MonthlyCountResult m : data){
            series.getData().add(new XYChart.Data<>(m.month().toString(), m.count()));
        }
        chart.getData().add(series);
        chart.setLegendVisible(false);
        return chart;
    }

    private static BarChart<String, Number> createBarChart(String title, String xLabel, String yLabel){
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setAnimated(false);
        return chart;
    }

    private Button backToOverviewButton(){
        Button btn = new Button("← Overview");
        btn.setOnAction(e -> showOverview());
        return btn;
    }

    private static Button detailButton(String text, Runnable action){
        Button btn = new Button(text);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private HBox buildFilterBar(){
        dailyFromPicker.setPromptText("Daily from");
        dailyToPicker.setPromptText("Daily to");
        monthlyFromField.setPromptText("yyyy-MM");
        monthlyFromField.setPrefWidth(90);
        monthlyToField.setPromptText("yyyy-MM");
        monthlyToField.setPrefWidth(90);
        Button applyButton = new Button("Apply");
        applyButton.setOnAction(e -> applyFilter());
        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {
            dailyFromPicker.setValue(null);
            dailyToPicker.setValue(null);
            monthlyFromField.clear();
            monthlyToField.clear();
            loadOverview();
        });
        HBox bar = new HBox(8, new Label("Daily:"), dailyFromPicker, dailyToPicker,
                new Label("Monthly:"), monthlyFromField, monthlyToField, applyButton, resetButton);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return bar;
    }

    private void applyFilter(){
        try {
            YearMonth monthlyFrom = parseYearMonth(monthlyFromField.getText());
            YearMonth monthlyTo = parseYearMonth(monthlyToField.getText());
            loadOverview(dailyFromPicker.getValue(), dailyToPicker.getValue(), monthlyFrom, monthlyTo);
        } catch (RuntimeException ex){
            ScreenComponents.showError(messageLabel,
                    new RuntimeException("Invalid month format. Use yyyy-MM."));
        }
    }

    private static YearMonth parseYearMonth(String text){
        if (text == null || text.isBlank()) return null;
        return YearMonth.parse(text.trim());
    }
}
