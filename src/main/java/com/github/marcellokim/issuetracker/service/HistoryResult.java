package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.User;
import java.time.LocalDateTime;

public record HistoryResult(
        String historyId,
        ActionType action,
        String previousValue,
        String newValue,
        String message,
        User changedBy,
        LocalDateTime changedDate
) {
}
