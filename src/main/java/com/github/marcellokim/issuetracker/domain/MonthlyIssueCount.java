package com.github.marcellokim.issuetracker.domain;

import java.time.YearMonth;

public record MonthlyIssueCount(
        YearMonth month,
        int count
) {
}
