package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.StatisticsController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.service.DailyCountResult;
import com.github.marcellokim.issuetracker.service.MonthlyCountResult;
import com.github.marcellokim.issuetracker.service.StatisticsReportResult;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

final class StatisticsScreen extends VBox {

    private final StatisticsController statisticsController;
    private final long projectId;
    private final Label messageLabel = ScreenComponents.messageLabel();
    private Runnable onBack;

    StatisticsScreen(StatisticsController statisticsController, long projectId){
        this.statisticsController = statisticsController;
        this.projectId = projectId;
        ScreenComponents.applyScreenDefaults(this);

        Button backButton = ScreenComponents.backButton("← Issues", () -> { if (onBack != null) onBack.run(); });
        Label titleLabel = ScreenComponents.titleLabel("Statistics");

        getChildren().addAll(
                ScreenComponents.header(backButton, titleLabel),
                messageLabel);
        loadStatistics();
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private void loadStatistics(){
        try{
            StatisticsReportResult report = statisticsController.viewStatistics(projectId);
            VBox content = new VBox(8);

            content.getChildren().add(buildMapSection("Issues by Status", report.statusCounts()));
            content.getChildren().add(buildMapSection("Issues by Priority", report.priorityCounts()));
            content.getChildren().addAll(new Separator(), buildDailySection("Daily Issue Counts", report.dailyCounts()));
            content.getChildren().addAll(new Separator(), buildMonthlySection("Monthly Issue Counts", report.monthlyCounts()));
            content.getChildren().addAll(new Separator(), buildNestedSection("Monthly Status Breakdown", report.monthlyStatusCounts()));
            content.getChildren().addAll(new Separator(), buildNestedSection("Monthly Priority Breakdown", report.monthlyPriorityCounts()));
            content.getChildren().addAll(new Separator(), buildDailySection("Daily Status Changes", report.dailyStatusChangeCounts()));
            content.getChildren().addAll(new Separator(), buildMonthlySection("Monthly Status Changes", report.monthlyStatusChangeCounts()));
            content.getChildren().addAll(new Separator(), buildDailySection("Daily Comment Counts", report.dailyCommentCounts()));
            content.getChildren().addAll(new Separator(), buildMonthlySection("Monthly Comment Counts", report.monthlyCommentCounts()));

            ScrollPane scrollPane = new ScrollPane(content);
            scrollPane.setFitToWidth(true);
            javafx.scene.layout.VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
            getChildren().add(scrollPane);
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    private static <K> VBox buildMapSection(String title, Map<K, Integer> data){
        VBox box = new VBox(2);
        Label header = new Label(title);
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        box.getChildren().add(header);
        if (data.isEmpty()){
            box.getChildren().add(new Label("  (no data)"));
        } else{
            for (Map.Entry<K, Integer> entry : data.entrySet()){
                box.getChildren().add(new Label("  " + entry.getKey() + ": " + entry.getValue()));
            }
        }
        return box;
    }

    private static VBox buildDailySection(String title, List<DailyCountResult> data){
        VBox box = new VBox(2);
        Label header = new Label(title + " (" + data.size() + " days)");
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        box.getChildren().add(header);
        if (data.isEmpty()){
            box.getChildren().add(new Label("  (no data)"));
        } else{
            for (DailyCountResult d : data){
                box.getChildren().add(new Label("  " + d.date() + ": " + d.count()));
            }
        }
        return box;
    }

    private static VBox buildMonthlySection(String title, List<MonthlyCountResult> data){
        VBox box = new VBox(2);
        Label header = new Label(title + " (" + data.size() + " months)");
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        box.getChildren().add(header);
        if (data.isEmpty()){
            box.getChildren().add(new Label("  (no data)"));
        } else{
            for (MonthlyCountResult m : data){
                box.getChildren().add(new Label("  " + m.month() + ": " + m.count()));
            }
        }
        return box;
    }

    private static <K> VBox buildNestedSection(String title, Map<YearMonth, Map<K, Integer>> data){
        VBox box = new VBox(2);
        Label header = new Label(title);
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        box.getChildren().add(header);
        if (data.isEmpty()){
            box.getChildren().add(new Label("  (no data)"));
        } else{
            for (Map.Entry<YearMonth, Map<K, Integer>> monthEntry : data.entrySet()){
                box.getChildren().add(new Label("  " + monthEntry.getKey() + ":"));
                for (Map.Entry<K, Integer> entry : monthEntry.getValue().entrySet()){
                    box.getChildren().add(new Label("    " + entry.getKey() + ": " + entry.getValue()));
                }
            }
        }
        return box;
    }
}
