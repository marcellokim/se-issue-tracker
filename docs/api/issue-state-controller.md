# IssueStateController API

## 범위

`IssueStateController`는 이슈 상태 전이 command API를 제공한다. 컨트롤러 메서드는 하나이지만, `targetStatus` 인자를 통해 여러 OC 상태 전이로 분기된다.

## 공개 오퍼레이션

| 오퍼레이션 | 서비스 호출 | 결과 |
| --- | --- | --- |
| `changeStatus(issueId, targetStatus, comment)` | `IssueStateService.changeStatus(issueId, targetStatus, comment, currentUserId)` | `IssueStateResult` |

## 오퍼레이션 상세

컨트롤러는 현재 로그인한 사용자를 요구하고, `user.getLoginId()`를 서비스로 전달한다. 세션이 없으면 `SecurityException("Login is required.")`를 던진다.

서비스는 `issueId > 0`, null이 아닌 `targetStatus`, blank가 아닌 `currentUserId`, blank가 아닌 `comment`를 요구한다. 일반 상태 전이 경로에서는 `DELETED` 이슈를 차단하며, 삭제된 이슈는 삭제 이슈 관리 흐름에서만 다룬다.

지원하는 target status는 `FIXED`, `ASSIGNED`, `RESOLVED`, `CLOSED`, `REOPENED`이다. 그 외 target status, 예를 들어 `NEW` 또는 `DELETED`는 이 서비스 경계에서 `UnsupportedOperationException`으로 거부된다.

| Target status | 서비스 경로 | 주요 guard |
| --- | --- | --- |
| `FIXED` | `IssueStateService.markFixed` | 현재 상태 `ASSIGNED`; active DEV assignee; active project member |
| `ASSIGNED` | `IssueStateService.rejectFix` | 현재 상태 `FIXED`; active TESTER verifier; active project member |
| `RESOLVED` | `IssueStateService.resolve` | 현재 상태 `FIXED`; active TESTER verifier; active project member; 모든 blocking issue가 `RESOLVED` 또는 `CLOSED` |
| `CLOSED` | `IssueStateService.close` | 현재 상태 `RESOLVED`; active project PL |
| `REOPENED` | `IssueStateService.reopen` | 현재 상태 `RESOLVED` 또는 `CLOSED`; active project PL |

모든 지원 상태 전이는 도메인 상태 변경과 함께 `CommentPurpose.STATUS_CHANGE` 댓글을 추가한다. 댓글 id는 `CommentIdProvider`를 통해 생성한다. 이후 변경된 이슈 aggregate를 `IssueRepository.save`로 저장하고, `IssueStateResult`를 반환한다.

`IssueStateResult`는 이슈 DB id, 이슈 식별자, 현재 상태, assignee, verifier, fixer, resolver를 포함한다.

## UC/OC/DCD 추적성

| Target | UC/SSD/OC | DCD/domain 근거 |
| --- | --- | --- |
| `FIXED` | UC6, OC-06 Mark Issue as Fixed, SSD-06 | `Issue.markFixed`, fixer 연관, `Comment(STATUS_CHANGE)`, `IssueHistory(STATUS_CHANGED)` |
| `RESOLVED` | UC6, OC-07 Resolve Fixed Issue, SSD-07 | `Issue.resolve`, resolver 연관, `IssueDependency` 기반 blocking issue guard, `IssueHistory(STATUS_CHANGED)` |
| `CLOSED` | UC6, OC-08 Close Resolved Issue, SSD-09 | `Issue.close`, active assignee/verifier 제거, `IssueHistory(STATUS_CHANGED)` |
| `REOPENED` | UC6, OC-09 Reopen Issue, SSD-10 | `Issue.reopen`, active assignee/verifier 제거, fixer/resolver 이력 유지, `IssueHistory(STATUS_CHANGED)` |
| `ASSIGNED` | UC6, OC-13 Reject Fix, SSD-08 | `Issue.rejectFix`, assignment 유지, `Comment(STATUS_CHANGE)`, `IssueHistory(STATUS_CHANGED)` |

## 구현 및 설계 차이

| 분류 | 내용 |
| --- | --- |
| `설계와 일치하는 부분` | 구현된 상태 전이 matrix는 삭제를 제외한 UC6 필수 target status를 다룬다. 삭제는 `DeletedIssueController`가 담당한다. |
| `설계 문서와 다른 구현 세부사항` | `DELETED` 상태 전이는 이 컨트롤러에서 거부되며, 삭제 이슈 관리 workflow로 분리되어 있다. |

## 권한 및 실패 요약

- 컨트롤러 경계에서 로그인이 필요하다.
- `issueId`가 0 이하이면 `IllegalArgumentException`을 던진다.
- `comment` 또는 `currentUserId`가 null 또는 blank이면 `IllegalArgumentException`을 던진다.
- `targetStatus`가 null이면 `NullPointerException("targetStatus")` 계열 예외를 던진다.
- 이슈 또는 사용자를 찾을 수 없으면 `IllegalArgumentException`을 던진다.
- 지원하지 않는 target status는 `UnsupportedOperationException`을 던진다.
- `DELETED` 이슈에 대해 일반 상태 전이를 시도하면 `SecurityException("Deleted issues must be managed through deleted issue workflow.")`을 던진다.
- `FIXED` 전이는 현재 DEV assignee만 수행할 수 있다.
- `ASSIGNED`로 되돌리는 reject fix 전이는 현재 TESTER verifier만 수행할 수 있다.
- `RESOLVED` 전이는 현재 TESTER verifier만 수행할 수 있고, unresolved blocking issue가 있으면 `IllegalStateException`을 던진다. 조건을 통과해도 dependency row는 자동 제거하지 않는다.
- `CLOSED`와 `REOPENED` 전이는 해당 프로젝트의 active PL만 수행할 수 있다.
- actor가 상태별 role 조건을 만족해도 해당 프로젝트의 active member 또는 active PL이 아니면 `SecurityException`을 던진다.
- 도메인 상태 전제조건이 맞지 않으면 `IllegalStateException`을 던진다.

## 근거

- `src/main/java/com/github/marcellokim/issuetracker/controller/IssueStateController.java`: `IssueStateController.changeStatus`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueStateService.java`: `IssueStateService.changeStatus`, `markFixed`, `rejectFix`, `resolve`, `close`, `reopen`, `rejectUnresolvedBlockingIssues`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanChangeStatus`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueStateResult.java`: `IssueStateResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/CommentIdProvider.java`: `CommentIdProvider`
