# 구현 API 명세

## 범위

이 폴더는 현재 로컬 코드에 구현된 Controller/Service 공개 계약을 정리한다. 문서는 구현에서 역추출한 명세이며, 새로운 동작을 정의하지 않는다.

각 컨트롤러 문서는 다음 내용을 중심으로 작성한다.

- 공개 오퍼레이션과 하위 서비스 호출
- 권한/정책 검사
- 주요 실패 조건
- UC/OC/DCD 추적성
- 구현과 설계 문서 사이의 차이 또는 보강 사항

통계는 전체 시스템이나 이슈 상세가 아니라 선택된 프로젝트 화면에서 조회한다. non-ADMIN 사용자의 프로젝트 이슈 목록은 대시보드가 아니라 프로젝트 진입 후 `IssueController.viewRelatedProjectIssues(projectId)`로 조회한다.

## 컨트롤러 문서

| 컨트롤러 | 문서 | 주 사용 대상 |
| --- | --- | --- |
| `AccountController` | [account-controller.md](account-controller.md) | ADMIN 계정 관리 UI, UC12 추적성 |
| `AssignmentController` | [assignment-controller.md](assignment-controller.md) | PL 배정 UI, UC5/UC8 추적성 |
| `AuthenticationController` | [authentication-controller.md](authentication-controller.md) | 로그인/로그아웃, 세션 UI |
| `DashboardController` | [dashboard-controller.md](dashboard-controller.md) | 로그인 후 첫 화면, 프로젝트/유저 요약 |
| `DeletedIssueController` | [deleted-issue-controller.md](deleted-issue-controller.md) | 삭제 이슈 관리 UI, UC9 추적성 |
| `IssueController` | [issue-controller.md](issue-controller.md) | 이슈 등록/조회/검색/댓글/의존성/액션 조회 |
| `IssueStateController` | [issue-state-controller.md](issue-state-controller.md) | 이슈 상태 전이 UI, UC6 추적성 |
| `ProjectController` | [project-controller.md](project-controller.md) | 프로젝트 상세 조회와 ADMIN 프로젝트 관리 |
| `StatisticsController` | [statistics-controller.md](statistics-controller.md) | 프로젝트 통계 조회 UI, UC10 추적성 |

## 공개 오퍼레이션 목록

| 컨트롤러 | 공개 오퍼레이션 |
| --- | --- |
| `AccountController` | `createAccount`, `renameAccount`, `changeAccountRole`, `activateAccount`, `deactivateAccount` |
| `AssignmentController` | `startAssignment`, `assignIssue`, `reassignIssue`, `changeVerifier` |
| `AuthenticationController` | `login`, `logout` |
| `DashboardController` | `viewProjects`, `viewUsers` |
| `DeletedIssueController` | `viewDeletedIssues`, `deleteIssue`, `restoreIssue`, `purgeDeletedIssue` |
| `IssueController` | `registerIssue`, `canRegisterIssue`, `viewIssueDetail`, `searchIssues`, `viewRelatedProjectIssues`, `updateIssue`, `changePriority`, `addComment`, `viewComments`, `addDependency`, `viewProjectDependencies`, `removeDependency`, `deleteComment`, `updateComment`, `viewAvailableActions`, `canUpdateComment`, `canDeleteComment` |
| `IssueStateController` | `changeStatus` |
| `ProjectController` | `viewProjectNonAdminDetail`, `viewProjectAdminDetail`, `viewProjectParticipants`, `createProject`, `renameProject`, `changeProjectDescription`, `deleteProject`, `addProjectParticipant`, `removeProjectParticipant` |
| `StatisticsController` | `viewStatistics` |

## 주요 화면 분기

### ADMIN

1. `AuthenticationController.login(loginId, password)`
2. `DashboardController.viewProjects()`
   - 전체 프로젝트 요약을 조회한다.
3. `DashboardController.viewUsers()`
   - 전체 유저 정보를 조회한다.
4. 프로젝트 선택 시 `ProjectController.viewProjectAdminDetail(projectId)`
   - 프로젝트 기본 정보와 참여자 목록을 조회한다.
   - ADMIN 프로젝트 화면에는 이슈 목록을 표시하지 않는다.
5. 프로젝트 관리 시 `ProjectController`의 생성/수정/삭제/참여자 관리 API를 사용한다.
6. 계정 관리 시 `AccountController` API를 사용한다.

### Non-ADMIN: PL/DEV/TESTER

1. `AuthenticationController.login(loginId, password)`
2. `DashboardController.viewProjects()`
   - 자신이 참여한 프로젝트 요약만 조회한다.
3. 프로젝트 선택 시 `ProjectController.viewProjectNonAdminDetail(projectId)`
   - 프로젝트 기본 정보를 조회한다.
4. 같은 프로젝트 화면에서 `IssueController.viewRelatedProjectIssues(projectId)`
   - PL은 해당 프로젝트의 일반 이슈를 조회한다.
   - DEV/TESTER는 reporter, 현재 assignee, 현재 verifier로 관련된 이슈를 조회한다.
   - fixer/resolver 같은 완료 이력자는 현재 관련 이슈 기준에 포함하지 않는다.
5. 같은 프로젝트 화면에서 `StatisticsController.viewStatistics(projectId)`
   - 해당 프로젝트 통계를 조회한다.
6. 이슈 선택 시 `IssueController.viewIssueDetail(issueId)`
   - 이슈 상세, 댓글, 히스토리, 의존성, 사용 가능한 액션 이름을 조회한다.

## UC/OC 매핑 요약

| UC/OC | 구현 API |
| --- | --- |
| UC1 / OC-01 Register Issue | `IssueController.registerIssue` |
| UC2 / OC-02 Add Comment | `IssueController.addComment` |
| UC3 Browse/Search Issues | `IssueController.searchIssues`, `viewRelatedProjectIssues` |
| UC4 View Issue Detail | `IssueController.viewIssueDetail` |
| UC5 / OC-03, OC-04, OC-05, OC-12 Assignment | `AssignmentController.assignIssue`, `reassignIssue`, `changeVerifier` |
| UC6 / OC-06, OC-07, OC-08, OC-09, OC-13 State change | `IssueStateController.changeStatus` |
| UC7 / OC-14, OC-15 Dependency | `IssueController.addDependency`, `removeDependency` |
| UC8 Recommend Assignment Candidates | `AssignmentController.startAssignment` |
| UC9 / OC-10, OC-11 Deleted issue | `DeletedIssueController.deleteIssue`, `restoreIssue`, `purgeDeletedIssue` |
| UC10 Statistics | `StatisticsController.viewStatistics` |
| UC11 Log In | `AuthenticationController.login`, `logout` |
| UC12 Manage Accounts | `AccountController.createAccount`, `renameAccount`, `changeAccountRole`, `activateAccount`, `deactivateAccount` |
| UC13 Manage Projects | `ProjectController.createProject`, `renameProject`, `changeProjectDescription`, `deleteProject`, `addProjectParticipant`, `removeProjectParticipant`, project view APIs |
| UC14 Verify Permission | 보호된 Controller API에서 호출되는 `PermissionPolicy.assertCan...` 계열과 일부 `can...` 계열 |
| UC15 Edit Issue | `IssueController.updateIssue` |
| UC16 / OC-16 Change Priority | `IssueController.changePriority` |
| Supporting APIs | dashboard, statistics, workflow action-query APIs |

## DCD / Domain 근거 요약

| 영역 | DCD/domain 근거 |
| --- | --- |
| 이슈 workflow | `docs/uml/dcd/its_dcd.puml`: `Issue`, `Comment`, `IssueHistory`, `IssueDependency` |
| 배정 workflow | `docs/uml/dcd/its_dcd.puml`: `Issue.assignFromNew`, `assignReopened`, `reassignAssignee`, `changeVerifier`, assignee/verifier 관계 |
| 상태 전이 | `docs/uml/dcd/its_dcd.puml`: `Issue.markFixed`, `resolve`, `rejectFix`, `close`, `reopen`, `IssueHistory(STATUS_CHANGED)` |
| 삭제 이슈 workflow | `docs/uml/dcd/its_dcd.puml`: `Issue`, `IssueHistory`, `IssueDependency`; 삭제/복구 조율은 service/JDBC 문서에서 다룬다 |
| ADMIN/support workflow | `docs/uml/dcd/its_dcd.puml`: `Project`, `User`, `ProjectMember`; 권한 정책과 result 구현 클래스는 DCD에서 생략한다 |

## 설계 차이 요약

| 분류 | 내용 |
| --- | --- |
| `implementation-extra` | Dashboard, account, project, statistics, comment edit/delete, workflow action-query, rich issue search API는 구현되어 있지만 필수 OC에 모두 개별 항목으로 표현되어 있지는 않다. |
| `signature-drift` | OC-15는 `removeDependency(dependencyId)`를 언급하지만, 현재 구현은 `removeDependency(blockingIssueId, blockedIssueId)`를 사용한다. |
| `behavior-drift` | `deleteIssue`와 `restoreIssue` 구현은 comment 인자를 받지만, OC 이름에는 comment 인자가 드러나지 않는다. |
| `matches` | 이슈 등록, 댓글 추가, 배정, 상태 전이, 의존성 추가, 삭제 이슈, 우선순위 변경은 Controller/Service 경로가 명확하게 존재한다. |

## 문서 작성 규칙

각 컨트롤러 문서는 코드 근거를 `파일 경로 + 메서드 이름`으로 적는다. 라인 번호는 수정에 따라 쉽게 바뀌므로, PR 리뷰와 추적성 문서에서는 메서드 단위 근거를 우선한다.

문서는 현재 구현을 설명하는 자료이며, 정책 변경이 필요한 경우에는 먼저 서비스/테스트를 수정한 뒤 문서를 역전파한다.
