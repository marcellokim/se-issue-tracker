# IssueStateController API

## Scope

`IssueStateController` exposes the state-transition command API. One controller method maps to multiple OC transitions through the `targetStatus` argument.

## Public Operations

| Operation | Service call | Result |
| --- | --- | --- |
| `changeStatus(issueId, targetStatus, comment)` | `IssueStateService.changeStatus(issueId, targetStatus, comment, currentUserId)` | `IssueStateResult` |

## Operation Details

The controller requires a current user and passes `user.getLoginId()` to the service. The service requires non-null `targetStatus` and non-blank `comment`.

Supported targets are `FIXED`, `ASSIGNED`, `RESOLVED`, `CLOSED`, and `REOPENED`. Other targets, including `NEW` and `DELETED`, are rejected by `UnsupportedOperationException` at this service boundary.

| Target status | Service path | Main guard |
| --- | --- | --- |
| `FIXED` | `IssueStateService.markFixed` | active DEV assignee; active project member |
| `ASSIGNED` | `IssueStateService.rejectFix` | active TESTER verifier when current status is `FIXED`; active project member |
| `RESOLVED` | `IssueStateService.resolve` | active TESTER verifier; active project member; all blocking issues `RESOLVED` or `CLOSED` |
| `CLOSED` | `IssueStateService.close` | active project PL |
| `REOPENED` | `IssueStateService.reopen` | active project PL |

Every supported transition adds a `STATUS_CHANGE` comment through `issue.addComment(..., CommentPurpose.STATUS_CHANGE)` and then saves the issue.

## UC/OC/DCD Traceability

| Target | UC/SSD/OC | DCD/domain evidence |
| --- | --- | --- |
| `FIXED` | UC6, OC-06 Mark Issue as Fixed, SSD-06 | `Issue.markFixed`, fixer association, `Comment(STATUS_CHANGE)`, `IssueHistory(STATUS_CHANGED)` |
| `RESOLVED` | UC6, OC-07 Resolve Fixed Issue, SSD-07 | `Issue.resolve`, resolver association, dependency guard through `IssueDependency`, `IssueHistory(STATUS_CHANGED)` |
| `CLOSED` | UC6, OC-08 Close Resolved Issue, SSD-09 | `Issue.close`, active assignee/verifier clearing, `IssueHistory(STATUS_CHANGED)` |
| `REOPENED` | UC6, OC-09 Reopen Issue, SSD-10 | `Issue.reopen`, active assignee/verifier clearing, fixer/resolver retained, `IssueHistory(STATUS_CHANGED)` |
| `ASSIGNED` | UC6, OC-13 Reject Fix, SSD-08 | `Issue.rejectFix`, assignment retained, `Comment(STATUS_CHANGE)`, `IssueHistory(STATUS_CHANGED)` |

## Implementation And Design Gaps

| Classification | Detail |
| --- | --- |
| `matches` | The implemented transition matrix covers the required UC6 target statuses except deletion, which is handled by `DeletedIssueController`. |
| `behavior-drift` | `DELETED` is rejected here and implemented through deleted issue workflow instead. |

## Permission And Failure Summary

- Requires login at controller boundary.
- Throws `IllegalArgumentException` for blank comment or missing issue/user.
- Throws `NullPointerException` for null `targetStatus` via `Objects.requireNonNull`.
- Throws `UnsupportedOperationException` for unsupported target statuses.
- Throws `SecurityException` for role/assignment/project-membership failures.
- Throws `IllegalStateException` when resolving an issue with unresolved blocking dependencies.

## Evidence

- `src/main/java/com/github/marcellokim/issuetracker/controller/IssueStateController.java`: `IssueStateController.changeStatus`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueStateService.java`: `IssueStateService.changeStatus`, `markFixed`, `rejectFix`, `resolve`, `close`, `reopen`, `rejectUnresolvedBlockingIssues`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanChangeStatus`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueStateResult.java`: `IssueStateResult`
