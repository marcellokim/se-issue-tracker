package com.github.marcellokim.issuetracker.technical;

import com.github.marcellokim.issuetracker.service.IssueIdProvider;
import java.util.UUID;

public final class IssueIdGenerator implements IssueIdProvider {

    private static final String ISSUE_ID_PREFIX = "ISSUE-";

    @Override
    public String nextIssueId() {
        return ISSUE_ID_PREFIX + UUID.randomUUID();
    }
}
