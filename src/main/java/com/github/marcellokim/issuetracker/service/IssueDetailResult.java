package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
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
        UserResult reporter,
        UserResult assignee,
        UserResult verifier,
        UserResult fixer,
        UserResult resolver,
        LocalDateTime reportedDate,
        LocalDateTime updatedAt,
        List<CommentResult> comments,
        List<HistoryResult> histories,
        List<DependencyResult> dependencies) {

    public IssueDetailResult {
        comments = List.copyOf(comments);
        histories = List.copyOf(histories);
        dependencies = List.copyOf(dependencies);
    }
}
