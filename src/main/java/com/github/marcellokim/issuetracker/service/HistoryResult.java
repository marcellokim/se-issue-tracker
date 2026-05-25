package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.ActionType;
import java.time.LocalDateTime;

public record HistoryResult(
        long id,
        long issueId,
        String changedById,
        ActionType actionType,
        String previousValue,
        String newValue,
        String message,
        LocalDateTime changedDate
) {
}
