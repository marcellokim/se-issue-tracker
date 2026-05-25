package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import java.time.LocalDate;
import java.util.Objects;

public record DailyCountResult(LocalDate date, int count) {

    public DailyCountResult {
        date = Objects.requireNonNull(date, "date");
    }

    public static DailyCountResult from(DailyIssueCount count) {
        Objects.requireNonNull(count, "count");
        return new DailyCountResult(count.date(), count.count());
    }
}
