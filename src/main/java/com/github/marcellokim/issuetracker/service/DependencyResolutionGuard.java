package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.repository.IssueDependencyRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import java.util.Objects;

public final class DependencyResolutionGuard implements IssueResolutionGuard {

    private final IssueRepository issueRepository;
    private final IssueDependencyRepository dependencyRepository;

    public DependencyResolutionGuard(
            IssueRepository issueRepository,
            IssueDependencyRepository dependencyRepository
    ) {
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.dependencyRepository = Objects.requireNonNull(dependencyRepository, "dependencyRepository");
    }

    @Override
    public void assertCanResolve(Issue issue) {
        Objects.requireNonNull(issue, "issue");
        for (var dependency : dependencyRepository.findByBlockedIssueId(issue.id())) {
            Issue blockingIssue = issueRepository.findById(dependency.blockingIssueId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Blocking issue not found: " + dependency.blockingIssueId()));
            if (!isCompleted(blockingIssue)) {
                throw new IllegalStateException("Blocking issue is not completed: " + blockingIssue.id());
            }
        }
    }

    private static boolean isCompleted(Issue issue) {
        // dependency edge는 Priority.BLOCKER가 아니며, 이 guard는 RESOLVED 전이 가능 여부만 확인한다.
        return issue.status() == IssueStatus.RESOLVED || issue.status() == IssueStatus.CLOSED;
    }
}
