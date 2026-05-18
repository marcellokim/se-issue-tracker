package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public interface StatisticsRepository {

    Map<IssueStatus, Integer> countByStatus(long projectId);

    Map<Priority, Integer> countByPriority(long projectId);

    List<DailyIssueCount> countReportedIssuesByDay(long projectId);

    List<DailyIssueCount> countReportedIssuesByDay(long projectId, LocalDate fromInclusive, LocalDate toInclusive);

    List<MonthlyIssueCount> countReportedIssuesByMonth(long projectId);

    List<MonthlyIssueCount> countReportedIssuesByMonth(long projectId, YearMonth fromInclusive, YearMonth toInclusive);

    StatisticsReport buildReport(
            long projectId,
            LocalDate dailyFromInclusive,
            LocalDate dailyToInclusive,
            YearMonth monthlyFromInclusive,
            YearMonth monthlyToInclusive
    );
}
