package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.service.IssueIdProvider;
import java.util.concurrent.atomic.AtomicLong;

public final class SequentialIssueIdProvider implements IssueIdProvider {

    private final AtomicLong next;

    public SequentialIssueIdProvider() {
        this(1L);
    }

    public SequentialIssueIdProvider(long first) {
        this.next = new AtomicLong(first);
    }

    @Override
    public String nextIssueId() {
        return "ISSUE-" + next.getAndIncrement();
    }
}
