# DeletedIssueController API

## 범위

`DeletedIssueController`는 PL 사용자를 위한 삭제 이슈 관리 API를 제공한다. 삭제된 이슈 조회, 활성 이슈의 soft delete, 삭제 이슈 복구, delete 시점의 보관 개수 제한 적용, 삭제 이슈 단건 물리 삭제를 다룬다.

## 공개 오퍼레이션

| 오퍼레이션 | 서비스 호출 | 결과 |
| --- | --- | --- |
| `viewDeletedIssues(projectId)` | `DeletedIssueService.viewDeletedIssues(projectId, actor)` | `List<IssueSummary>` |
| `deleteIssue(issueId, comment)` | `DeletedIssueService.deleteIssue(issueId, comment, actor)` | `IssueSummary` |
| `restoreIssue(issueId, comment)` | `DeletedIssueService.restoreIssue(issueId, comment, actor)` | `IssueSummary` |
| `purgeDeletedIssue(issueId)` | `DeletedIssueService.purgeDeletedIssue(issueId, actor)` | `void` |

## 오퍼레이션 상세

모든 오퍼레이션은 로그인한 actor를 요구한다. 세션이 없으면 컨트롤러는 `SecurityException("Login is required.")`를 던진다.

`viewDeletedIssues`는 특정 프로젝트의 `DELETED` 상태 이슈 목록을 `IssueSummary` 목록으로 반환한다.

`deleteIssue`는 비어 있지 않은 `comment`를 요구하고, `DeletedIssueRepository.softDelete`를 통해 이슈 상태를 `DELETED`로 바꾼다. 삭제 가능한 이슈 상태는 `NEW` 또는 `CLOSED`이다. soft delete 이후 같은 프로젝트에서 삭제 이슈가 `MAX_DELETED_ISSUES_PER_PROJECT = 30`개를 초과하면 FIFO 기준으로 초과분을 물리 삭제한다.

`restoreIssue`는 비어 있지 않은 `comment`를 요구하고, `DeletedIssueRepository.restore`에 위임한다. 복구 대상은 현재 `DELETED` 상태여야 한다. 복구 상태는 최신 `IssueHistory(STATUS_CHANGED, newValue=DELETED)`의 `previousValue`에서 읽는다. 삭제 이력이 없거나 삭제 전 상태가 `NEW` 또는 `CLOSED`가 아니면 복구에 실패한다.

`purgeDeletedIssue`는 이미 `DELETED` 상태인 이슈 하나를 물리 삭제한다. 대상 이슈가 `DELETED` 상태가 아니면 실패한다.

## UC/OC/DCD 추적성

| API | UC/SSD/OC | DCD/domain 근거 |
| --- | --- | --- |
| `deleteIssue` | UC9, OC-10 Delete Closed/New Issue, SSD-12 | DCD의 `Issue`, `IssueHistory(STATUS_CHANGED)`, `IssueDependency` 관계; 구현 `JdbcDeletedIssueRepository.softDelete` |
| `restoreIssue` | UC9, OC-11 Restore Deleted Issue, SSD-26 | DCD의 `Issue.restore`, `Issue.findDeleteStatusHistory`, `IssueHistory(STATUS_CHANGED)`; 구현 `JdbcDeletedIssueRepository.restore`, `latestPreDeleteStatus` |
| `viewDeletedIssues`, `purgeDeletedIssue` | UC9 보조 API | 삭제 이슈 관리 흐름; 구현 `DeletedIssueService.viewDeletedIssues`, `purgeDeletedIssue`, `JdbcDeletedIssueRepository.purgeDeletedBeyondLimit`, `purgeDeletedById` |

## 구현 및 설계 차이

| 분류 | 내용 |
| --- | --- |
| `오퍼레이션 시그니처 차이` | OC 이름에는 `comment`가 없지만, 구현된 `deleteIssue`와 `restoreIssue`는 사유 기록을 위해 `comment` 인자를 요구한다. |
| `설계와 일치하는 부분` | PL 전용 삭제 이슈 관리와 30개 보관 제한 초과분 정리가 구현되어 있다. |

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

- `src/main/java/com/github/marcellokim/issuetracker/controller/DeletedIssueController.java`: `DeletedIssueController.viewDeletedIssues`, `deleteIssue`, `restoreIssue`, `purgeDeletedIssue`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/DeletedIssueService.java`: `DeletedIssueService.viewDeletedIssues`, `deleteIssue`, `restoreIssue`, `purgeDeletedIssue`
- `src/main/java/com/github/marcellokim/issuetracker/persistence/jdbc/JdbcDeletedIssueRepository.java`: `JdbcDeletedIssueRepository.softDelete`, `restore`, `latestPreDeleteStatus`, `purgeDeletedBeyondLimit`, `purgeDeletedById`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanManageDeletedIssue`, `assertCanChangeStatus`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueSummary.java`: `IssueSummary`
