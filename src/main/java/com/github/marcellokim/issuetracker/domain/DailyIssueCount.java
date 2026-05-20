package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDate;
import java.util.Objects;

public final class DailyIssueCount {

    private final LocalDate date;
    private final int count;

    public static DailyIssueCount create(LocalDate date, int count) {
        return new DailyIssueCount(date, count);
    }

    private DailyIssueCount(LocalDate date, int count) {
        this.date = date;
        this.count = count;
    }

    public LocalDate date() {
        return date;
    }

    public int count() {
        return count;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DailyIssueCount that)) {
            return false;
        }
        return count == that.count && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, count);
    }

    @Override
    public String toString() {
        return "DailyIssueCount[date=" + date + ", count=" + count + "]";
    }
}
