package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.service.IssueIdProvider;

public final class SequentialIssueIdProvider implements IssueIdProvider {

    private long next;

    public SequentialIssueIdProvider() {
        this(1L);
    }

    public SequentialIssueIdProvider(long first) {
        this.next = first;
    }

    @Override
    public String nextIssueId() {
        return "ISSUE-" + next++;
    }
}
