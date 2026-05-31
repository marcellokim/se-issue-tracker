package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.StatisticsController;
import com.github.marcellokim.issuetracker.service.StatisticsReportResult;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import java.util.Map;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

            VBox statusBox = new VBox(4);
            statusBox.getChildren().add(new Label("Issues by Status:"));
            for (Map.Entry<IssueStatus, Integer> entry : report.statusCounts().entrySet()){
                statusBox.getChildren().add(new Label("  " + entry.getKey() + ": " + entry.getValue()));
            }

            VBox priorityBox = new VBox(4);
            priorityBox.getChildren().add(new Label("Issues by Priority:"));
            for (Map.Entry<Priority, Integer> entry : report.priorityCounts().entrySet()){
                priorityBox.getChildren().add(new Label("  " + entry.getKey() + ": " + entry.getValue()));
            }

            Label dailyLabel = new Label("Daily issue count: " + report.dailyCounts().size() + " days");
            Label monthlyLabel = new Label("Monthly issue count: " + report.monthlyCounts().size() + " months");

            getChildren().addAll(statusBox, priorityBox, dailyLabel, monthlyLabel);
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
    }
}
