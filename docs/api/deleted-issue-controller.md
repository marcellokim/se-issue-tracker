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
| `deleteIssue` | UC9, OC-10 Delete Closed/New Issue, SSD-12 | DCD의 `Issue`, `IssueHistory(STATUS_CHANGED)`, `IssueDependency` 관계; implementation `JdbcIssueDeleteOperations.softDelete` |
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
# DeletedIssueController API

## 범위

`DeletedIssueController`는 PL 사용자를 위한 삭제 이슈 관리 API를 제공한다. 삭제된 이슈 조회, 활성 이슈의 soft delete, 삭제 이슈 복구, 보관 개수 초과분 정리, 삭제 이슈 단건 물리 삭제를 다룬다.

## 공개 오퍼레이션

| 오퍼레이션 | 서비스 호출 | 결과 |
| --- | --- | --- |
| `viewDeletedIssues(projectId)` | `DeletedIssueService.viewDeletedIssues(projectId, actor)` | `List<IssueSummary>` |
| `deleteIssue(issueId, comment)` | `DeletedIssueService.deleteIssue(issueId, comment, actor)` | `IssueSummary` |
| `restoreIssue(issueId, comment)` | `DeletedIssueService.restoreIssue(issueId, comment, actor)` | `IssueSummary` |
| `purgeOverflow(projectId)` | `DeletedIssueService.purgeOverflow(projectId, actor)` | `int` |
| `purgeDeletedIssue(issueId)` | `DeletedIssueService.purgeDeletedIssue(issueId, actor)` | `void` |

## 오퍼레이션 상세

모든 오퍼레이션은 로그인한 actor를 요구한다. 세션이 없으면 컨트롤러는 `SecurityException("Login is required.")`를 던진다.

`viewDeletedIssues`는 특정 프로젝트의 `DELETED` 상태 이슈 목록을 `IssueSummary` 목록으로 반환한다.

`deleteIssue`는 비어 있지 않은 `comment`를 요구하고, `IssueRepository.softDelete`를 통해 이슈 상태를 `DELETED`로 바꾼다. 삭제 가능한 이슈 상태는 `NEW` 또는 `CLOSED`이다. soft delete 이후 같은 프로젝트에서 삭제 이슈가 `MAX_DELETED_ISSUES_PER_PROJECT = 30`개를 초과하면 FIFO 기준으로 초과분을 물리 삭제한다.

`restoreIssue`는 비어 있지 않은 `comment`를 요구하고, `IssueRepository.restore`에 위임한다. 복구 대상은 현재 `DELETED` 상태여야 한다. 복구 상태는 최신 `IssueHistory(STATUS_CHANGED, newValue=DELETED)`의 `previousValue`에서 읽는다. 삭제 이력이 없거나 삭제 전 상태가 `NEW` 또는 `CLOSED`가 아니면 복구에 실패한다.

`purgeOverflow`는 특정 프로젝트의 삭제 이슈 보관 개수 초과분을 정리하고, 물리 삭제된 이슈 수를 반환한다.

`purgeDeletedIssue`는 이미 `DELETED` 상태인 이슈 하나를 물리 삭제한다. 대상 이슈가 `DELETED` 상태가 아니면 실패한다.

## UC/OC/DCD 추적성

| API | UC/SSD/OC | DCD/domain 근거 |
| --- | --- | --- |
| `deleteIssue` | UC9, OC-10 Delete Closed/New Issue, SSD-12 | DCD의 `Issue`, `IssueHistory(STATUS_CHANGED)`, `IssueDependency` 관계; 구현 `JdbcIssueDeleteOperations.softDelete` |
| `restoreIssue` | UC9, OC-11 Restore Deleted Issue, SSD-26 | DCD의 `Issue.restore`, `Issue.findDeleteStatusHistory`, `IssueHistory(STATUS_CHANGED)`; 구현 `JdbcIssueDeleteOperations.restore`, `latestPreDeleteStatus` |
| `viewDeletedIssues`, `purgeOverflow`, `purgeDeletedIssue` | UC9 보조 API | 삭제 이슈 관리 흐름; 구현 `DeletedIssueService.viewDeletedIssues`, `purgeOverflow`, `purgeDeletedIssue`, `JdbcIssueDeleteOperations.purgeDeletedBeyondLimit`, `purgeDeletedById` |

## 구현 및 설계 차이

| 분류 | 내용 |
| --- | --- |
| `signature-drift` | OC 이름에는 `comment`가 없지만, 구현된 `deleteIssue`와 `restoreIssue`는 사유 기록을 위해 `comment` 인자를 요구한다. |
| `matches` | PL 전용 삭제 이슈 관리와 30개 보관 제한 초과분 정리가 구현되어 있다. |

## 권한 및 실패 요약

- 컨트롤러 경계에서 로그인이 필요하다.
- 모든 오퍼레이션은 `PermissionPolicy.assertCanManageDeletedIssue`와 active project PL membership을 요구한다.
- `projectId`가 0 이하이면 삭제 이슈 관리 권한 검사에서 실패한다.
- `deleteIssue`는 추가로 `PermissionPolicy.assertCanChangeStatus(actor, issue, DELETED)`를 요구하므로 `NEW` 또는 `CLOSED` 이슈만 삭제할 수 있다.
- `deleteIssue`와 `restoreIssue`의 `comment`가 null 또는 blank이면 `IllegalArgumentException`을 던진다.
- 이슈를 찾을 수 없으면 `IllegalArgumentException`을 던진다.
- actor가 해당 프로젝트의 active PL이 아니면 `SecurityException`을 던진다.
- 복구는 이슈가 `DELETED` 상태가 아니거나, 삭제 이력이 없거나, 최신 삭제 전 상태가 `NEW` 또는 `CLOSED`가 아니면 실패한다.
- `purgeDeletedIssue`는 `issueId`가 0 이하이면 `IllegalArgumentException`을 던진다.
- `purgeDeletedIssue`는 대상 이슈가 `DELETED` 상태가 아니면 `IllegalArgumentException`을 던진다.
- `purgeDeletedIssue`가 물리 삭제 대상 행을 삭제하지 못하면 `IllegalStateException`을 던진다.

## 근거

- `src/main/java/com/github/marcellokim/issuetracker/controller/DeletedIssueController.java`: `DeletedIssueController.viewDeletedIssues`, `deleteIssue`, `restoreIssue`, `purgeOverflow`, `purgeDeletedIssue`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/DeletedIssueService.java`: `DeletedIssueService.viewDeletedIssues`, `deleteIssue`, `restoreIssue`, `purgeOverflow`, `purgeDeletedIssue`
- `src/main/java/com/github/marcellokim/issuetracker/persistence/jdbc/JdbcIssueDeleteOperations.java`: `JdbcIssueDeleteOperations.softDelete`, `restore`, `latestPreDeleteStatus`, `purgeDeletedBeyondLimit`, `purgeDeletedById`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanManageDeletedIssue`, `assertCanChangeStatus`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueSummary.java`: `IssueSummary`
