package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDate;

public record DailyIssueCount(
        LocalDate date,
        int count
) {
}
