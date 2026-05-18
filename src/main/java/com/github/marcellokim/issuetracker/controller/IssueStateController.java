package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.service.IssueStateResult;
import com.github.marcellokim.issuetracker.service.IssueStateService;
import java.util.Objects;

public class IssueStateController {

    private final IssueStateService issueStateService;

    public IssueStateController(IssueStateService issueStateService) {
        this.issueStateService = Objects.requireNonNull(issueStateService, "issueStateService must not be null");
    }

    public IssueStateResult changeStatus(String issueId, IssueStatus targetStatus, String comment, String currentUserId) {
        return issueStateService.changeStatus(issueId, targetStatus, comment, currentUserId);
    }
}
