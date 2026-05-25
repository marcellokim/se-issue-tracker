package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import java.time.YearMonth;
import java.util.Objects;

public record MonthlyCountResult(YearMonth month, int count) {

    public MonthlyCountResult {
        month = Objects.requireNonNull(month, "month");
    }

    public static MonthlyCountResult from(MonthlyIssueCount count) {
        Objects.requireNonNull(count, "count");
        return new MonthlyCountResult(count.month(), count.count());
    }
}
