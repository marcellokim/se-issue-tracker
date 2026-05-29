# DeletedIssueController API

## Scope

`DeletedIssueController` exposes deleted issue management APIs for PL users. It covers viewing deleted issues, soft-deleting active issues, restoring deleted issues, and purging overflow beyond the retention limit.

## Public Operations

| Operation | Service call | Result |
| --- | --- | --- |
| `viewDeletedIssues(projectId)` | `DeletedIssueService.viewDeletedIssues(projectId, actor)` | `List<IssueSummary>` |
| `deleteIssue(issueId, comment)` | `DeletedIssueService.deleteIssue(issueId, comment, actor)` | `IssueSummary` |
| `restoreIssue(issueId, comment)` | `DeletedIssueService.restoreIssue(issueId, comment, actor)` | `IssueSummary` |
| `purgeOverflow(projectId)` | `DeletedIssueService.purgeOverflow(projectId, actor)` | `int` |
| `purgeDeletedIssue(issueId)` | `DeletedIssueService.purgeDeletedIssue(issueId, actor)` | `void` |

## Operation Details

All operations require a logged-in actor. `deleteIssue` uses `IssueRepository.softDelete` and then purges deleted issues beyond `MAX_DELETED_ISSUES_PER_PROJECT = 30`. `restoreIssue` delegates to `IssueRepository.restore`. `purgeOverflow` returns the count purged. `purgeDeletedIssue` physically deletes one issue that is already in `DELETED` status.

`deleteIssue` and `restoreIssue` require non-blank `comment` values at the JDBC delete/restore operation boundary. Restore succeeds only for an issue currently in `DELETED` status. The restore target status is read from the latest `IssueHistory(STATUS_CHANGED, newValue=DELETED).previousValue`; missing delete history or a pre-delete status other than `NEW` or `CLOSED` fails.

## UC/OC/DCD Traceability

| API | UC/SSD/OC | DCD/domain evidence |
| --- | --- | --- |
| `deleteIssue` | UC9, OC-10 Delete Closed/New Issue, SSD-12 | `DeletedIssueController`, `Issue.softDelete`, `IssueHistory(STATUS_CHANGED)`, `IssueDependency` removal in `docs/uml/dcd/its_dcd_ver2.puml`; implementation `JdbcIssueDeleteOperations.softDelete` |
| `restoreIssue` | UC9, OC-11 Restore Deleted Issue, SSD-26 | `Issue.restore`, `Issue.findDeleteStatusHistory`, `IssueHistory(STATUS_CHANGED)` in DCD; implementation `JdbcIssueDeleteOperations.restore`, `latestPreDeleteStatus` |
| `viewDeletedIssues`, `purgeOverflow`, `purgeDeletedIssue` | UC9 support APIs | DCD deleted issue workflow; implementation `DeletedIssueService.viewDeletedIssues`, `purgeOverflow`, `purgeDeletedIssue`, `JdbcIssueDeleteOperations.purgeDeletedBeyondLimit`, `purgeDeletedById` |

## Implementation And Design Gaps

| Classification | Detail |
| --- | --- |
| `signature-drift` | OC names omit `comment`, while implemented `deleteIssue` and `restoreIssue` require a comment argument. |
| `matches` | PL-only deleted issue management and 30-item overflow purge are implemented. |

## Permission And Failure Summary

- Requires login at controller boundary.
- Requires `PermissionPolicy.assertCanManageDeletedIssue` and active project PL membership.
- `deleteIssue` also requires `PermissionPolicy.assertCanChangeStatus(actor, issue, DELETED)`, so only `NEW` or `CLOSED` issues are deletable.
- Throws `IllegalArgumentException` when the issue is missing.
- Throws `IllegalArgumentException` when delete/restore `comment` is blank at repository operation boundary.
- Throws `SecurityException` when actor is not the project PL or issue status cannot be deleted.
- Restore fails when the issue is not `DELETED`, when delete history is missing, or when the latest pre-delete status is not `NEW` or `CLOSED`.

## Evidence

- `src/main/java/com/github/marcellokim/issuetracker/controller/DeletedIssueController.java`: `DeletedIssueController.viewDeletedIssues`, `deleteIssue`, `restoreIssue`, `purgeOverflow`, `purgeDeletedIssue`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/DeletedIssueService.java`: `DeletedIssueService.viewDeletedIssues`, `deleteIssue`, `restoreIssue`, `purgeOverflow`, `purgeDeletedIssue`
- `src/main/java/com/github/marcellokim/issuetracker/persistence/jdbc/JdbcIssueDeleteOperations.java`: `JdbcIssueDeleteOperations.softDelete`, `restore`, `latestPreDeleteStatus`, `purgeDeletedBeyondLimit`, `purgeDeletedById`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanManageDeletedIssue`, `assertCanChangeStatus`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueSummary.java`: `IssueSummary`
