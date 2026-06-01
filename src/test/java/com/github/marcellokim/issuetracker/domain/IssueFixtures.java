package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;

final class IssueFixtures {

    private static final long TEST_PROJECT_ID = 1L;

    private IssueFixtures() {
    }

    static Issue create(
            String issueId,
            String title,
            String description,
            Priority priority,
            User reporter,
            LocalDateTime reportedDate) {
        Issue.PersistedState state = Issue.persistedState(TEST_PROJECT_ID, title, description, reporter)
                .issueId(issueId)
                .reportedDate(reportedDate)
                .updatedAt(reportedDate);
        if (priority != null) {
            state.priority(priority);
        }
        return Issue.create(state);
    }
}
