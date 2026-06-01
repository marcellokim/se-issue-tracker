package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing statistics range request")
class StatisticsRangeRequestTest {

    @Test
    @DisplayName("parses blank values as an all-time range")
    void parsesBlankValuesAsAllTimeRange() {
        StatisticsRangeRequest request = StatisticsRangeRequest.parse(" ", "", null, "   ");

        assertNull(request.dailyFromInclusive());
        assertNull(request.dailyToInclusive());
        assertNull(request.monthlyFromInclusive());
        assertNull(request.monthlyToInclusive());
    }

    @Test
    @DisplayName("parses ISO date and year-month text")
    void parsesIsoDateAndYearMonthText() {
        StatisticsRangeRequest request = StatisticsRangeRequest.parse(
                "2026-05-01",
                "2026-05-31",
                "2026-05",
                "2026-06");

        assertEquals(LocalDate.of(2026, 5, 1), request.dailyFromInclusive());
        assertEquals(LocalDate.of(2026, 5, 31), request.dailyToInclusive());
        assertEquals(YearMonth.of(2026, 5), request.monthlyFromInclusive());
        assertEquals(YearMonth.of(2026, 6), request.monthlyToInclusive());
    }

    @Test
    @DisplayName("rejects invalid text and reversed ranges before calling controllers")
    void rejectsInvalidTextAndReversedRanges() {
        assertThrows(IllegalArgumentException.class,
                () -> StatisticsRangeRequest.parse("2026/05/01", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> StatisticsRangeRequest.parse("2026-05-31", "2026-05-01", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> StatisticsRangeRequest.parse(null, null, "2026-08", "2026-07"));
    }
}
