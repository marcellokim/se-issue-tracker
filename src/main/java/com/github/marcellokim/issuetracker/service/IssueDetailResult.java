package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.User;
import java.time.LocalDateTime;
import java.util.List;

public record IssueDetailResult(
        long id,
        long projectId,
        String issueId,
        IssueStatus status,
        Priority priority,
        String title,
        String description,
        User reporter,
        User assignee,
        User verifier,
        User fixer,
        User resolver,
        LocalDateTime reportedDate,
        LocalDateTime updatedAt,
        List<CommentResult> comments,
        List<HistoryResult> histories,
        List<DependencyResult> dependencies,
        List<String> availableActions
) {

    public IssueDetailResult {
        comments = List.copyOf(comments);
        histories = List.copyOf(histories);
        dependencies = List.copyOf(dependencies);
        availableActions = List.copyOf(availableActions);
    }

    public IssueDetailResult withAvailableActions(List<String> nextAvailableActions) {
        return new IssueDetailResult(
                id,
                projectId,
                issueId,
                status,
                priority,
                title,
                description,
                reporter,
                assignee,
                verifier,
                fixer,
                resolver,
                reportedDate,
                updatedAt,
                comments,
                histories,
                dependencies,
                nextAvailableActions);
    }
}
