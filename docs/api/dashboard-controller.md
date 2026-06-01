# DashboardController API

## 범위

`DashboardController`는 로그인 후 첫 화면과 화면 이동에 필요한 읽기 전용 API를 제공한다. 프로젝트와 사용자 요약 데이터는 `DashboardSummaryService`를 통해 조회한다. 프로젝트 내부 이슈 목록 조회는 대시보드 컨트롤러가 아니라 프로젝트 진입 후 `IssueController.viewProjectIssues`에서 처리한다.

## 공개 오퍼레이션

| 오퍼레이션 | 서비스 호출 | 결과 |
| --- | --- | --- |
| `viewProjects()` | `DashboardSummaryService.projectSummariesFor(currentUser)` | `List<DashboardProjectSummary>` |
| `viewUsers()` | `DashboardSummaryService.usersFor(currentUser)` | `List<UserResult>` |

## 오퍼레이션 상세

컨트롤러의 모든 오퍼레이션은 현재 로그인한 사용자를 요구한다. 세션이 없으면 `SecurityException("Login is required.")`를 던진다.

`viewProjects`는 현재 로그인한 사용자의 역할과 프로젝트 참여 여부에 따라 프로젝트 요약 목록을 반환한다. ADMIN 사용자는 전체 프로젝트 요약을 볼 수 있고, non-ADMIN 사용자는 자신이 참여한 프로젝트 요약만 볼 수 있다. 서비스는 active 사용자만 대시보드 프로젝트를 조회할 수 있도록 검사한다.

`viewUsers`는 ADMIN 사용자에게 전체 사용자 목록을 반환한다. non-ADMIN 사용자가 호출하면 예외를 던지지 않고 빈 목록을 반환한다.

`DashboardProjectSummary`는 프로젝트 식별자, 이름, 설명, 전체 참여자 수, 역할별 참여자 수, visible/deleted 이슈 수, 현재 상태별 이슈 수를 포함한다.

## UC/OC/DCD 추적성

| API | UC/SSD/OC | DCD/domain 근거 |
| --- | --- | --- |
| `viewProjects` | 프로젝트 선택과 화면 이동 지원; 직접 대응되는 필수 OC 없음 | DCD `Project`, `User participates in Project`; 구현 `DashboardSummaryService.projectSummariesFor`, `DashboardProjectSummary` |
| `viewUsers` | ADMIN 대시보드 보조 API; 직접 대응되는 필수 OC 없음 | DCD `User`; 구현 `DashboardSummaryService.usersFor`, `PermissionPolicy.canViewAllUsers` |

## 구현 및 설계 차이

| 분류 | 내용 |
| --- | --- |
| `implementation-extra` | 대시보드 집계는 서비스 읽기 모델로 구현되어 있지만 필수 OC에는 표현되어 있지 않다. |

## 권한 및 실패 요약

- 컨트롤러 경계에서 로그인이 필요하다.
- `viewProjects`는 `PermissionPolicy.canViewAllProjects`와 프로젝트 참여 여부를 기준으로 결과를 필터링한다.
- ADMIN은 전체 프로젝트 요약을 조회한다.
- non-ADMIN은 자신이 참여한 프로젝트 요약만 조회한다.
- `viewUsers`는 `PermissionPolicy.canViewAllUsers`를 사용한다.
- non-ADMIN의 `viewUsers`는 예외를 던지지 않고 빈 목록을 반환한다.
- 현재 사용자가 없으면 `SecurityException("Login is required.")`를 던진다.

## 근거

- `src/main/java/com/github/marcellokim/issuetracker/controller/DashboardController.java`: `DashboardController.viewProjects`, `viewUsers`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/DashboardSummaryService.java`: `DashboardSummaryService.projectSummariesFor`, `usersFor`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.canViewAllProjects`, `canViewAllUsers`
- `src/main/java/com/github/marcellokim/issuetracker/service/DashboardProjectSummary.java`: `DashboardProjectSummary`
