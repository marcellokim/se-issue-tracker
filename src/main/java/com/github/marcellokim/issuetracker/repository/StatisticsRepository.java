package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import java.util.List;
import java.util.Map;

public interface StatisticsRepository {

    Map<IssueStatus, Integer> countByStatus(long projectId);

    Map<Priority, Integer> countByPriority(long projectId);

    List<DailyIssueCount> countReportedIssuesByDay(long projectId);

    List<MonthlyIssueCount> countReportedIssuesByMonth(long projectId);
}
