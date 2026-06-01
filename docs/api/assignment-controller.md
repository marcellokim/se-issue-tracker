# AssignmentController API

## 범위

`AssignmentController`는 PL이 이슈 담당자를 배정하기 위해 사용하는 API를 제공한다. 후보 조회, NEW 또는 REOPENED 이슈 배정, ASSIGNED 이슈의 DEV 담당자 재배정, FIXED 이슈의 TESTER 검증자 변경을 다룬다.

## 공개 오퍼레이션

| 오퍼레이션 | 서비스 호출 | 결과 |
| --- | --- | --- |
| `startAssignment(issueId)` | `AssignmentService.startAssignment(issueId, currentUserId)` | `AssignmentOptionsResult` |
| `assignIssue(issueId, assigneeId, verifierId)` | `AssignmentService.assignIssue(issueId, assigneeId, verifierId, currentUserId)` | `AssignmentResult` |
| `reassignIssue(issueId, assigneeId)` | `AssignmentService.reassignIssue(issueId, assigneeId, currentUserId)` | `AssignmentResult` |
| `changeVerifier(issueId, verifierId)` | `AssignmentService.changeVerifier(issueId, verifierId, currentUserId)` | `AssignmentResult` |

## 오퍼레이션 상세

컨트롤러는 현재 로그인한 사용자를 요구하고, `user.getLoginId()`를 서비스로 전달한다. `AssignmentOptionsResult`는 추천 후보와 전체 후보 목록을 함께 반환한다. `devAssigneeCandidates`와 `testerVerifierCandidates`는 상위 3명 추천 목록이고, `allDevAssignees`와 `allTesterVerifiers`는 프로젝트 내 전체 후보 목록이다. `AssignmentResult`는 이슈 DB id, 이슈 식별자, 상태, assignee, verifier를 반환한다.

`startAssignment`는 `NEW`, `REOPENED`, `ASSIGNED`, `FIXED` 상태에서만 허용된다. `NEW`와 `REOPENED`는 DEV assignee 후보와 TESTER verifier 후보를 모두 반환한다. `ASSIGNED`는 DEV assignee 후보만 반환하고, `FIXED`는 TESTER verifier 후보만 반환한다.

`assignIssue`는 `NEW`와 `REOPENED` 상태에서만 허용된다. assignee는 해당 프로젝트의 active DEV여야 하고, verifier는 해당 프로젝트의 active TESTER여야 한다. `reassignIssue`는 도메인 `Issue.reassignAssignee`에 위임하며, `ASSIGNED` 상태에서 DEV assignee를 변경한다. `changeVerifier`는 도메인 `Issue.changeVerifier`에 위임하며, `FIXED` 상태에서 TESTER verifier를 변경한다.

## UC/OC/DCD 추적성

| API | UC/SSD/OC | DCD/domain 근거 |
| --- | --- | --- |
| `startAssignment` | UC8 Recommend Assignment Candidates; 배정 전 후보 조회 보조 오퍼레이션 | DCD의 `Issue` assignee/verifier 관계와 상태별 배정 operation; 구현 `AssignmentRecommendationService.recommendAssignmentCandidates` |
| `assignIssue` | UC5, OC-03 NEW to ASSIGNED, OC-12 REOPENED to ASSIGNED | `Issue.assignFromNew`, `Issue.assignReopened`, `IssueHistory(ASSIGNMENT_CHANGED, STATUS_CHANGED)`, assignee/verifier 연관 |
| `reassignIssue` | UC5, OC-04 ASSIGNED to ASSIGNED | `Issue.reassignAssignee`, `IssueHistory(ASSIGNMENT_CHANGED)`, assignee 연관 변경 |
| `changeVerifier` | UC5, OC-05 FIXED to FIXED | `Issue.changeVerifier`, `IssueHistory(ASSIGNMENT_CHANGED)`, verifier 연관 변경 |

## 구현 및 설계 차이

| 분류 | 내용 |
| --- | --- |
| `matches` | Assignment 오퍼레이션은 컨트롤러에 연결되어 있고 서비스에서 권한과 정책을 검사한다. |
| `implementation-extra` | `startAssignment`는 후보 조회 API이며, 그 자체가 필수 OC 사후조건을 변경하는 오퍼레이션은 아니다. |

## 권한 및 실패 요약

- 컨트롤러 경계에서 로그인이 필요하다.
- 모든 배정 기능은 `PermissionPolicy.assertCanAssignIssue`를 통해 active PL role을 요구한다.
- actor는 해당 이슈 프로젝트의 active PL이어야 한다.
- `assignIssue`는 해당 프로젝트의 active DEV assignee와 active TESTER verifier를 요구한다.
- `reassignIssue`는 해당 프로젝트의 active DEV assignee를 요구한다.
- `changeVerifier`는 해당 프로젝트의 active TESTER verifier를 요구한다.
- `issueId`가 0 이하이면 `IllegalArgumentException`을 던진다.
- 이슈 또는 사용자 id를 찾을 수 없으면 `IllegalArgumentException`을 던진다.
- actor가 프로젝트 PL이 아니거나 후보자가 필요한 role을 가진 active 프로젝트 멤버가 아니면 `SecurityException`을 던진다.
- 이슈 상태가 해당 배정 작업을 허용하지 않으면 `IllegalStateException`을 던진다.
- 같은 assignee로 재배정하거나 같은 verifier로 변경하면 도메인 계층에서 `IllegalArgumentException`을 던진다.

## 근거

- `src/main/java/com/github/marcellokim/issuetracker/controller/AssignmentController.java`: `AssignmentController.startAssignment`, `assignIssue`, `reassignIssue`, `changeVerifier`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/AssignmentService.java`: `AssignmentService.startAssignment`, `assignIssue`, `reassignIssue`, `changeVerifier`
- `src/main/java/com/github/marcellokim/issuetracker/service/AssignmentRecommendationService.java`: `AssignmentRecommendationService.recommendAssignmentCandidates`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanAssignIssue`
- `src/main/java/com/github/marcellokim/issuetracker/service/AssignmentResult.java`: `AssignmentResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/AssignmentOptionsResult.java`: `AssignmentOptionsResult`
