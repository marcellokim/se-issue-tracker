package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.StatisticsController;
import com.github.marcellokim.issuetracker.service.StatisticsReportResult;
import java.util.Objects;

final class StatisticsPresenter {

    private final StatisticsController statisticsController;
    private final StatisticsView view;

    StatisticsPresenter(StatisticsController statisticsController, StatisticsView view) {
        this.statisticsController = Objects.requireNonNull(statisticsController, "statisticsController");
        this.view = Objects.requireNonNull(view, "view");
    }

    void loadStatistics(long projectId) {
        loadStatistics(projectId, StatisticsRangeRequest.allTime());
    }

    void loadStatistics(long projectId, StatisticsRangeRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            StatisticsReportResult report = statisticsController.viewStatistics(
                    projectId,
                    request.dailyFromInclusive(),
                    request.dailyToInclusive(),
                    request.monthlyFromInclusive(),
                    request.monthlyToInclusive());
            view.showReport(report);
            view.showMessage(summaryMessage(report), false);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    private static String summaryMessage(StatisticsReportResult report) {
        int visibleIssues = report.statusCounts().values().stream().mapToInt(Integer::intValue).sum();
        return visibleIssues + " issues";
    }
}
