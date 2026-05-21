package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueDependencyChangeRepository;
import com.github.marcellokim.issuetracker.repository.IssueDependencyRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class IssueDependencyService {

    private final IssueRepository issueRepository;
    private final IssueDependencyRepository dependencyRepository;
    private final IssueDependencyChangeRepository dependencyChangeRepository;
    private final UserRepository userRepository;
    private final PermissionPolicy permissionPolicy;
    private final Clock clock;

    public IssueDependencyService(
            IssueRepository issueRepository,
            IssueDependencyRepository dependencyRepository,
            IssueDependencyChangeRepository dependencyChangeRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy,
            Clock clock
    ) {
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.dependencyRepository = Objects.requireNonNull(dependencyRepository, "dependencyRepository");
        this.dependencyChangeRepository = Objects.requireNonNull(
                dependencyChangeRepository,
                "dependencyChangeRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public IssueDependencyResult addDependency(long blockingIssueId, long blockedIssueId, String currentUserId) {
        Issue blockingIssue = findIssue(blockingIssueId);
        Issue blockedIssue = findIssue(blockedIssueId);
        User actor = findUser(currentUserId);
        permissionPolicy.assertCanManageDependency(actor, blockedIssue);
        rejectSelfDependency(blockingIssueId, blockedIssueId);
        rejectStoredDuplicate(blockingIssueId, blockedIssueId);
        rejectCycle(blockingIssueId, blockedIssueId);

        String dependencyKey = IssueDependency.dependencyIdFor(blockingIssue.id(), blockedIssue.id());
        IssueDependency dependency = blockedIssue.addDependency(dependencyKey, blockingIssue, actor, now());
        IssueDependency savedDependency = dependencyChangeRepository.saveWithBlockedIssueHistory(
                dependency,
                blockedIssue);
        return IssueDependencyResult.from(savedDependency);
    }

    public List<IssueDependencyResult> listDependencies(long issueId, String currentUserId) {
        Issue issue = findIssue(issueId);
        User actor = findUser(currentUserId);
        permissionPolicy.assertCanManageDependency(actor, issue);
        return dependencyRepository.findByIssueId(issueId).stream()
                .map(IssueDependencyResult::from)
                .toList();
    }

    public void removeDependency(long dependencyId, String currentUserId) {
        IssueDependency dependency = findDependency(dependencyId);
        Issue blockedIssue = findIssue(dependency.blockedIssueId());
        User actor = findUser(currentUserId);
        permissionPolicy.assertCanManageDependency(actor, blockedIssue);
        blockedIssue.recordDependencyRemoved(dependency, actor, now());
        dependencyChangeRepository.deleteWithBlockedIssueHistory(dependency, blockedIssue);
    }

    private void rejectSelfDependency(long blockingIssueId, long blockedIssueId) {
        if (blockingIssueId == blockedIssueId) {
            throw new IllegalArgumentException("Issue cannot depend on itself");
        }
    }

    private void rejectStoredDuplicate(long blockingIssueId, long blockedIssueId) {
        if (dependencyRepository.existsByPair(blockingIssueId, blockedIssueId)) {
            throw new IllegalArgumentException("Dependency already exists");
        }
    }

    private void rejectCycle(long blockingIssueId, long blockedIssueId) {
        if (reaches(blockedIssueId, blockingIssueId)) {
            throw new IllegalArgumentException("Dependency cycle is not allowed");
        }
    }

    private boolean reaches(long startIssueId, long targetIssueId) {
        Set<Long> visitedIssueIds = new HashSet<>();
        ArrayDeque<Long> pendingIssueIds = new ArrayDeque<>();
        pendingIssueIds.add(startIssueId);
        while (!pendingIssueIds.isEmpty()) {
            long currentIssueId = pendingIssueIds.removeFirst();
            if (!visitedIssueIds.add(currentIssueId)) {
                continue;
            }
            if (currentIssueId == targetIssueId) {
                return true;
            }
            dependencyRepository.findByBlockingIssueId(currentIssueId).stream()
                    .map(IssueDependency::blockedIssueId)
                    .forEach(pendingIssueIds::addLast);
        }
        return false;
    }

    private Issue findIssue(long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
    }

    private IssueDependency findDependency(long dependencyId) {
        return dependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new IllegalArgumentException("Dependency not found: " + dependencyId));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private LocalDateTime now() {
        return clock.now();
    }
}
