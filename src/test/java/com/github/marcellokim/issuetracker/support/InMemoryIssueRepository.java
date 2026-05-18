package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryIssueRepository implements IssueRepository {

    private final Map<String, Issue> issues = new LinkedHashMap<>();

    public InMemoryIssueRepository(Issue... issues) {
        for (var issue : issues) {
            save(issue);
        }
    }

    @Override
    public Optional<Issue> findById(String issueId) {
        return Optional.ofNullable(issues.get(issueId));
    }

    @Override
    public void save(Issue issue) {
        issues.put(issue.getIssueId(), issue);
    }
}
