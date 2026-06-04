# StatisticsController API

## 범위

`StatisticsController`는 선택된 프로젝트 화면에서 사용하는 프로젝트 통계 조회 API를 제공한다. 통계는 전체 시스템 대시보드나 이슈 상세 화면 기준이 아니라, 특정 `projectId`를 기준으로 조회한다. JavaFX와 Swing UI는 화면 구성이 달라도 같은 컨트롤러와 서비스 API를 재사용한다.

## 공개 오퍼레이션

| 오퍼레이션 | 서비스 호출 | 결과 |
| --- | --- | --- |
| `viewStatistics(projectId, dailyFromInclusive, dailyToInclusive, monthlyFromInclusive, monthlyToInclusive)` | `StatisticsService.viewStatistics(projectId, dailyFromInclusive, dailyToInclusive, monthlyFromInclusive, monthlyToInclusive, actor)` | `StatisticsReportResult` |
| `viewStatistics(projectId)` | 전체 overload에 모든 범위를 `null`로 넘김 | `StatisticsReportResult` |

## 오퍼레이션 상세

모든 오퍼레이션은 현재 로그인한 사용자를 요구한다. 세션이 없으면 컨트롤러 경계에서 `SecurityException("Login is required.")`를 던진다.

전체 overload는 일별 범위(`LocalDate`)와 월별 범위(`YearMonth`)를 선택적으로 받는다. 범위 값이 `null`이면 해당 방향은 제한하지 않는다. 인자가 없는 convenience overload는 전체 기간 통계를 조회한다.

`StatisticsReportResult`는 다음 통계 결과를 포함한다.

- 현재 상태별 이슈 수: `issues.status` 기준, `DELETED` 제외
- 현재 우선순위별 이슈 수: `issues.priority` 기준, `DELETED` 제외
- 일별 이슈 등록 수: `issues.reported_at` 기준, `DELETED` 제외
- 월별 이슈 등록 수: `issues.reported_at` 기준, `DELETED` 제외
- 월별 현재 상태별 이슈 수: `issues.reported_at`의 월과 현재 `issues.status` 기준, `DELETED` 제외
- 월별 현재 우선순위별 이슈 수: `issues.reported_at`의 월과 현재 `issues.priority` 기준, `DELETED` 제외
- 일별 상태 변경 수: `issue_history.changed_at`, `action_type = 'STATUS_CHANGED'` 기준, 현재 `DELETED` 이슈 제외
- 월별 상태 변경 수: `issue_history.changed_at`, `action_type = 'STATUS_CHANGED'` 기준, 현재 `DELETED` 이슈 제외
- 일별 코멘트 작성 수: `comments.created_at` 기준, 현재 `DELETED` 이슈 제외
- 월별 코멘트 작성 수: `comments.created_at` 기준, 현재 `DELETED` 이슈 제외

일별/월별 범위의 양끝 값이 모두 주어지면, 데이터가 없는 날짜 또는 월도 count 0으로 채워 반환한다. 한쪽 범위라도 `null`이면 실제 집계 결과가 있는 날짜 또는 월만 반환한다.

## UC/OC/DCD 추적성

| API | UC/SSD/OC | DCD/domain 근거 |
| --- | --- | --- |
| `viewStatistics` | UC10, SSD-14 view issue statistics; 필수 OC 목록에는 없는 보조 API | 통계는 핵심 workflow DCD 밖의 조회성 결과 모델이며, 구현 근거는 `StatisticsController.viewStatistics`, `StatisticsService.viewStatistics`, `StatisticsReportResult`이다. |

## 구현 및 설계 차이

| 분류 | 내용 |
| --- | --- |
| `구현에서 보강된 부분` | 통계 API는 구현되어 있고 이 문서에 명세하지만, `operation_contracts.md`에는 통계 postcondition이 따로 정의되어 있지 않다. |
| `프로젝트 화면 기준` | 통계는 프로젝트 선택 후 프로젝트 화면에서 조회한다. 이슈 상세 단위 통계 또는 전체 시스템 통계가 아니다. |
| `삭제 이슈 제외 정책` | 삭제 이슈 관리 화면 접근은 별도 workflow로 분리되며, 현재 통계 쿼리는 `DELETED` 상태 이슈를 제외한다. |

## 권한 및 실패 요약

- 컨트롤러 경계에서 로그인이 필요하다.
- `projectId`는 양수여야 하며, 0 이하이면 `IllegalArgumentException(" project Id must be positive")`를 던진다.
- 통계 조회자는 active `PL`, `DEV`, `TESTER` 중 하나여야 한다.
- 통계 조회자는 해당 프로젝트의 active member여야 한다.
- ADMIN은 통계 조회 권한에 포함되지 않는다.
- 존재하지 않는 `projectId`는 별도 프로젝트 조회가 아니라 active project membership 검사를 통과하지 못해 `SecurityException("Only project members can view statistics.")`로 처리된다.
- 일별 `from > to`이면 `IllegalArgumentException`을 던진다.
- 월별 `from > to`이면 `IllegalArgumentException`을 던진다.
- JDBC 집계 중 SQL 오류가 발생하면 `RepositoryException`으로 감싸서 던진다.

## 하위 레이어 흐름

1. UI가 선택된 프로젝트의 `projectId`로 `StatisticsController.viewStatistics(...)`를 호출한다.
2. `StatisticsController`가 `AuthenticationService.currentUser()`로 현재 로그인 사용자를 확인한다.
3. `StatisticsController`가 현재 사용자 `User` 객체와 날짜/월 범위를 `StatisticsService.viewStatistics(...)`로 전달한다.
4. `StatisticsService`가 `projectId` 양수 여부, actor null 여부, active project membership, 통계 조회 role, 날짜/월 범위 순서를 검사한다.
5. `StatisticsService`가 `StatisticsRepository.buildReport(...)`를 호출한다.
6. `JdbcStatisticsRepository`가 상태/우선순위/등록일/상태변경/코멘트 집계를 SQL로 조회한다.
7. `StatisticsService`가 `StatisticsReport`를 `StatisticsReportResult`로 변환해 컨트롤러로 반환한다.

## 근거

- `src/main/java/com/github/marcellokim/issuetracker/controller/StatisticsController.java`: `StatisticsController.viewStatistics`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/StatisticsService.java`: `StatisticsService.viewStatistics`, active project member 검사, range 검사
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanViewStatistics`
- `src/main/java/com/github/marcellokim/issuetracker/service/StatisticsReportResult.java`: `StatisticsReportResult`
- `src/main/java/com/github/marcellokim/issuetracker/repository/StatisticsRepository.java`: `StatisticsRepository.buildReport`
- `src/main/java/com/github/marcellokim/issuetracker/persistence/jdbc/JdbcStatisticsRepository.java`: 통계 SQL 집계 구현
- `src/main/java/com/github/marcellokim/issuetracker/service/DailyCountResult.java`: `DailyCountResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/MonthlyCountResult.java`: `MonthlyCountResult`
