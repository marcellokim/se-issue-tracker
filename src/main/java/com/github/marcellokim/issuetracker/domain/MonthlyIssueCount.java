package com.github.marcellokim.issuetracker.domain;

import java.time.YearMonth;
import java.util.Objects;

public final class MonthlyIssueCount {

    private final YearMonth month;
    private final int count;

    public static MonthlyIssueCount create(YearMonth month, int count) {
        return new MonthlyIssueCount(month, count);
    }

    private MonthlyIssueCount(YearMonth month, int count) {
        this.month = month;
        this.count = count;
    }

    public YearMonth month() {
        return month;
    }

    public int count() {
        return count;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MonthlyIssueCount that)) {
            return false;
        }
        return count == that.count && Objects.equals(month, that.month);
    }

    @Override
    public int hashCode() {
        return Objects.hash(month, count);
    }

    @Override
    public String toString() {
        return "MonthlyIssueCount[month=" + month + ", count=" + count + "]";
    }
}
