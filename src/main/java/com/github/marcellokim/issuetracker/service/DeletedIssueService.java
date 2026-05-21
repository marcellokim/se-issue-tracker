package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import java.util.List;
import java.util.Objects;

public final class DeletedIssueService {

    private static final int MAX_DELETED_ISSUES_PER_PROJECT = 30;

    private final IssueRepository issueRepository;
    private final PermissionPolicy permissionPolicy;
    private final Clock clock;

    public DeletedIssueService(
            IssueRepository issueRepository,
            PermissionPolicy permissionPolicy,
            Clock clock
    ) {
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public List<Issue> viewDeletedIssues(long projectId, User actor) {
        requireDeletedIssuePermission(actor, projectId);
        return issueRepository.findDeletedByProject(projectId);
    }

    public Issue deleteIssue(long issueId, String comment, User actor) {
        Issue issue = findIssue(issueId);
        permissionPolicy.assertCanManageDeletedIssue(actor, issue);

        Issue deletedIssue = issueRepository.softDelete(issueId, actor.getLoginId(), comment, clock.now());
        issueRepository.purgeDeletedBeyondLimit(deletedIssue.projectId(), MAX_DELETED_ISSUES_PER_PROJECT);
        return deletedIssue;
    }

    public Issue restoreIssue(long issueId, String comment, User actor) {
        Issue issue = findIssue(issueId);
        permissionPolicy.assertCanManageDeletedIssue(actor, issue);
        return issueRepository.restore(issueId, actor.getLoginId(), comment, clock.now());
    }

    public int purgeOverflow(long projectId, User actor) {
        requireDeletedIssuePermission(actor, projectId);
        return issueRepository.purgeDeletedBeyondLimit(projectId, MAX_DELETED_ISSUES_PER_PROJECT);
    }

    private Issue findIssue(long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue was not found: " + issueId));
    }

    private void requireDeletedIssuePermission(User actor, long projectId) {
        /*
         * 삭제 이슈 권한 확인을 repository 작업과 같은 service 경계에 배치.
         * 이후 리뷰에서 삭제 부수효과를 controller까지 추적하지 않아도 됨.
         */
        if (!permissionPolicy.verifyPermission(actor, "MANAGE_DELETED_ISSUE", projectId)) {
            throw new SecurityException("Only PL can manage deleted issues.");
        }
    }
}
