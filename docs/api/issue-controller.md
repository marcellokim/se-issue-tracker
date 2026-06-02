# IssueController API

## 범위

`IssueController`는 이슈 등록, 조회, 검색, 수정, 댓글, 의존성, workflow action 조회를 담당하는 주요 API 경계이다. 모든 UI 오퍼레이션은 현재 로그인한 사용자를 요구한다. 대부분의 기능은 `IssueService`에 위임하고, 버튼 활성화와 같은 action 조회 기능은 `IssueWorkflowService`에 위임한다.

## 공개 오퍼레이션

| 오퍼레이션 | 서비스 호출 | 결과 |
| --- | --- | --- |
| `registerIssue(projectId, title, description, priority)` | `IssueService.registerIssue(..., currentUserId)` | `IssueResult` |
| `canRegisterIssue(projectId)` | `IssueService.canRegisterIssue(projectId, currentUserId)` | `boolean` |
| `viewIssueDetail(issueId)` | `IssueService.viewIssueDetail(issueId, currentUserId)`, 필요 시 `IssueWorkflowService.viewAvailableActions` | `IssueDetailResult` |
| `searchIssues(projectId, keyword, status, priority)` | 추가 필터를 `null`로 채운 전체 `searchIssues` 오버로드 | `List<IssueSummary>` |
| `searchIssues(projectId, keyword, status, priority, reporterId, assigneeId, verifierId, reportedFrom, reportedTo)` | `IssueService.searchIssues(...)` | `List<IssueSummary>` |
| `viewProjectIssues(projectId)` | `IssueService.viewProjectIssues(projectId, currentUserId)` | `List<IssueSummary>` |
| `updateIssue(issueId, title, description)` | `IssueService.updateIssue(issueId, title, description, currentUserId)` | `IssueResult` |
| `changePriority(issueId, priority)` | `IssueService.changePriority(issueId, priority, currentUserId)` | `IssueResult` |
| `addComment(issueId, content)` | `IssueService.addComment(issueId, content, currentUserId)` | `CommentResult` |
| `viewComments(issueId)` | `IssueService.viewComments(issueId, currentUserId)` | `List<CommentResult>` |
| `addDependency(blockingIssueId, blockedIssueId)` | `IssueService.addDependency(blockingIssueId, blockedIssueId, currentUserId)` | `DependencyResult` |
| `viewProjectDependencies(projectId)` | `IssueService.viewProjectDependencies(projectId, currentUserId)` | `List<DependencyResult>` |
| `removeDependency(blockingIssueId, blockedIssueId)` | `IssueService.removeDependency(blockingIssueId, blockedIssueId, currentUserId)` | `void` |
| `deleteComment(issueId, commentId)` | `IssueService.deleteComment(issueId, commentId, currentUserId)` | `void` |
| `updateComment(issueId, commentId, content)` | `IssueService.updateComment(issueId, commentId, content, currentUserId)` | `CommentResult` |
| `viewAvailableActions(issueId)` | `IssueWorkflowService.viewAvailableActions(issueId, currentUserId)` | `IssueWorkflowActions` |
| `canUpdateComment(issueId, commentId)` | `IssueWorkflowService.canUpdateComment(issueId, commentId, currentUserId)` | `boolean` |
| `canDeleteComment(issueId, commentId)` | `IssueWorkflowService.canDeleteComment(issueId, commentId, currentUserId)` | `boolean` |

## 오퍼레이션 상세

모든 오퍼레이션은 컨트롤러 경계에서 현재 로그인한 사용자를 요구한다. 세션이 없으면 `SecurityException("Login is required.")`를 던진다.

`registerIssue`는 프로젝트 존재 여부, active PL/DEV/TESTER 역할, active project membership, 프로젝트 내 title 중복을 검사한다. `title`과 `description`은 null 또는 blank일 수 없다. `priority`가 null이면 `Priority.MAJOR`가 기본값으로 사용된다.

`canRegisterIssue`는 이슈 등록 가능 여부를 boolean으로 반환한다. 내부 검사 중 예외가 발생하면 예외를 밖으로 던지지 않고 `false`를 반환한다.

검색과 상세 조회 API는 active project membership을 요구하고, 일반 이슈 경로에서 `DELETED` 이슈를 차단한다. `searchIssues`는 항상 특정 프로젝트 내부에서만 검색하며, `status == DELETED`는 삭제 이슈 관리 흐름으로 분리되어 있으므로 거부한다. `keyword`, `reporterId`, `assigneeId`, `verifierId`는 선택 필터이며 null 또는 blank이면 필터에서 제외된다. `reportedFrom`이 `reportedTo`보다 뒤이면 실패한다.

`viewIssueDetail`은 이슈 기본 정보, reporter/assignee/verifier/fixer/resolver, 댓글, 히스토리, 현재 이슈를 막고 있는 dependency 정보를 반환한다. 컨트롤러가 `IssueWorkflowService`와 함께 생성된 경우에는 `IssueWorkflowActions`를 계산해 `IssueDetailResult.availableActions` 이름 목록으로 포함한다. `IssueWorkflowService`가 없으면 상세 정보만 반환하고 action 목록은 비어 있다.

`viewProjectIssues`는 특정 프로젝트 내부의 일반 이슈 목록을 반환한다. PL/DEV/TESTER 프로젝트 멤버는 같은 프로젝트의 일반 이슈를 볼 수 있고, reporter, 현재 assignee, 현재 verifier 조건은 `searchIssues` 필터로 좁힌다. fixer와 resolver는 완료 이력으로 보며 기본 이슈 목록 범위를 제한하지 않는다.

`updateIssue`는 이슈 reporter만 수행할 수 있고, 이슈 상태가 `NEW` 또는 `REOPENED`일 때만 허용된다. 같은 프로젝트 내 다른 이슈와 title이 중복되면 실패한다.

`changePriority`는 해당 프로젝트의 active PL만 수행할 수 있다. `priority`는 null일 수 없고, 현재 priority와 같은 값으로 변경하면 도메인 계층에서 실패한다.

`addComment`와 `viewComments`는 active project member만 수행할 수 있고, `DELETED` 이슈에는 사용할 수 없다. `addComment`는 `GENERAL` 댓글을 생성하고, 댓글 작성 이력을 issue history에 함께 기록한다.

`updateComment`와 `deleteComment`는 해당 댓글 작성자만 수행할 수 있고, `CommentPurpose.GENERAL` 댓글에만 허용된다. 댓글은 대상 이슈에 속해야 하며, actor는 해당 프로젝트의 active member여야 한다. 같은 내용으로 댓글을 수정하면 실패한다. 댓글 삭제는 comment row를 물리 삭제하고, 댓글 삭제 이력은 issue history에 남긴다. `CommentResult.commentId`는 저장된 DB comment id의 문자열 표현이며, 수정/삭제 API의 `commentId` 인자는 numeric DB id를 받는다.

의존성 추가와 삭제는 해당 프로젝트의 active PL만 수행할 수 있다. 의존성은 같은 프로젝트의 이슈 사이에서만 허용되며, 자기 자신 의존성, 중복 의존성, 순환 의존성은 거부된다. `addDependency`는 blocked issue가 `RESOLVED` 또는 `CLOSED` 상태이면 거부한다. `removeDependency`는 외부에서 받은 `blockingIssueId`, `blockedIssueId` 쌍으로 내부 dependency id를 계산해 삭제한다.

`viewProjectDependencies`는 해당 프로젝트의 active member가 프로젝트 내부 의존성 목록을 조회하는 기능이다. 의존성 관리는 PL만 가능하지만, 의존성 조회는 PL/DEV/TESTER 프로젝트 멤버에게 허용된다.

`viewAvailableActions`, `canUpdateComment`, `canDeleteComment`는 `IssueWorkflowService`가 설정된 컨트롤러에서만 사용할 수 있다. `IssueWorkflowService`가 없는 컨트롤러에서 직접 호출하면 `IllegalStateException("Issue workflow service is not configured.")`를 던진다.

## UC/OC/DCD 추적성

| API | UC/SSD/OC | DCD/domain 근거 |
| --- | --- | --- |
| `registerIssue` | UC1, OC-01, SSD-01 | `docs/uml/dcd/its_dcd.puml`의 `Issue`, `IssueHistory`, reporter 연관; 구현 `IssueController.registerIssue`, `IssueService.registerIssue` |
| `addComment` | UC2, OC-02, SSD-02 | `IssueService.addComment`, `Comment`, `IssueHistory(COMMENTED)`, writer 연관 |
| `addDependency` | UC7, OC-14, SSD-24 | `Issue.addDependency`, `IssueDependency`, blocking/blocked issue 연관, `IssueHistory(DEPENDENCY_CHANGED)` |
| `removeDependency` | UC7, OC-15, SSD-25 | DCD는 `removeDependency(dependencyId)`로 표현하지만, 구현은 `IssueService.removeDependency(blockingIssueId, blockedIssueId)`와 `Issue.removeDependency`를 사용 |
| `changePriority` | UC16, OC-16, SSD-27 | `Issue.changePriority`, `Issue.verifyPriorityChange`, `IssueHistory(PRIORITY_CHANGED)`; status와 role 연관은 변경되지 않음 |
| `viewIssueDetail` | UC4, SSD-04 및 UI 지원 | `Issue`, `Comment`, `IssueHistory`, `IssueDependency`, role 연관을 `IssueDetailResult`로 반환 |
| `searchIssues`, `viewProjectIssues` | UC3, SSD-03 및 UI 지원 | DCD `Issue`의 status/priority/reporter/assignee/verifier 속성; 구현 `IssueSearchCriteria`, `IssueSummary` |
| `updateIssue` | UC15, SSD-23 이슈 수정 지원 | `Issue.updateTitleAndDescription`, `IssueHistory(TITLE_DESCRIPTION_UPDATED)`; priority/status는 별도 UC에서 처리 |
| `deleteComment`, `updateComment`, `canUpdateComment`, `canDeleteComment`, `viewAvailableActions` | 직접 대응되는 필수 OC가 없는 구현 보조 API | `Comment`, `IssueWorkflowActions`, `PermissionPolicy.assertCan...` 메서드 |

## 구현 및 설계 차이

| 분류 | 내용 |
| --- | --- |
| `matches` | 이슈 등록, 댓글 추가, 의존성 추가, priority 변경은 컨트롤러에 연결되어 있고 서비스에서 권한과 정책을 검사한다. |
| `signature-drift` | OC-15는 `removeDependency(dependencyId)`를 문서화하지만, 현재 구현은 `removeDependency(blockingIssueId, blockedIssueId)`를 노출한다. |
| `implementation-extra` | 검색, 상세 조회, action 조회, 댓글 수정/삭제, 프로젝트 관련 이슈 조회 API는 필수 OC 목록 밖에 추가로 구현되어 있다. |

## 권한 및 실패 요약

- 컨트롤러 경계에서 로그인이 필요하다.
- id 계열 인자가 0 이하이면 `IllegalArgumentException`을 던진다.
- 필수 문자열 인자가 null 또는 blank이면 `IllegalArgumentException`을 던진다.
- 프로젝트, 이슈, 사용자, 댓글, 의존성 레코드를 찾을 수 없으면 `IllegalArgumentException`을 던진다.
- `registerIssue`, 검색, 상세 조회, 댓글, 의존성 조회는 active project membership을 요구한다.
- `changePriority`, 의존성 추가/삭제는 해당 프로젝트의 active PL membership을 요구한다.
- `viewAvailableActions`, `canUpdateComment`, `canDeleteComment`는 `IssueWorkflowService`가 설정되어 있어야 한다.
- 일반 이슈 API에서 `DELETED` 이슈에 접근하면 `SecurityException("Deleted issues must be managed through deleted issue workflow.")`를 던진다.
- `searchIssues`에서 `reportedFrom > reportedTo`이면 `IllegalArgumentException`을 던진다.
- `updateIssue`는 reporter가 아니거나 상태가 `NEW`/`REOPENED`가 아니면 `SecurityException`을 던진다.
- `updateComment`와 `deleteComment`는 댓글 작성자가 아니거나 `GENERAL` 댓글이 아니면 실패한다.
- `addDependency`는 다른 프로젝트 이슈, 자기 자신 의존성, 중복 의존성, 순환 의존성, `RESOLVED`/`CLOSED` blocked issue를 거부한다.

## 근거

- `src/main/java/com/github/marcellokim/issuetracker/controller/IssueController.java`: 모든 public `IssueController` 메서드, `requireCurrentUser`, `requireIssueWorkflowService`, `availableActionNames`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueService.java`: `IssueService.registerIssue`, `canRegisterIssue`, `viewIssueDetail`, `searchIssues`, `viewProjectIssues`, `updateIssue`, `changePriority`, `addComment`, `viewComments`, `addDependency`, `viewProjectDependencies`, `removeDependency`, `deleteComment`, `updateComment`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueWorkflowService.java`: `IssueWorkflowService.viewAvailableActions`, `canUpdateComment`, `canDeleteComment`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: issue/comment/dependency/priority 권한 검사 메서드
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueResult.java`: `IssueResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueDetailResult.java`: `IssueDetailResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueWorkflowActions.java`: `IssueWorkflowActions`
