package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.StatisticsReportResult;

interface StatisticsView {

    void showReport(StatisticsReportResult report);

    void showMessage(String message, boolean error);
}
