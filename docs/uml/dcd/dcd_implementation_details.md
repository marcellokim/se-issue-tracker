# DCD Implementation Details

이 문서는 기존 DCD/GRASP/MVC 구현 기준선 문서와 별개로, 현재 dev 브랜치 구현이 의존성 주입, composition root, factory, controller-service-repository wiring을 어떻게 다루는지 정리한다. 목적은 이후 구현자가 DCD를 보고 코드를 작성할 때 application/service/repository 책임을 같은 방향으로 맞추는 것이다.

## 현재 구현 방향 요약

현재 dev 구현은 setter 주입이 아니라 생성자 주입을 기본 방향으로 사용한다.

Controller와 Service는 필요한 collaborator를 생성자에서 전달받고, `private final` field로 보관한다. 예를 들어 `AssignmentController`는 `AuthenticationService`와 `AssignmentService`를 생성자에서 받고, `AssignmentService`는 `IssueRepository`, `UserRepository`, `PermissionPolicy`, `AssignmentRecommendationService`, `Clock`을 생성자에서 받는다.

이 구조의 기준은 다음과 같다.

- 장기 collaborator는 생성자 주입으로 받는다.
- Controller는 UI 요청의 첫 non-UI 수신자 역할을 한다.
- Controller가 복잡한 repository 조회, 권한 검사, 상태별 branch, 저장 흐름을 모두 직접 수행하지 않는다.
- 복잡한 application orchestration은 service로 분리한다.
- Repository는 interface로 받고, JDBC 구현체에 직접 의존하지 않는다.
- 실행 시점의 실제 구현체 조립은 composition root/factory에서 처리한다.

## Setter Injection

현재 확인한 dev 구조에서는 controller/service 핵심 collaborator를 setter로 바꾸는 방향이 아니다.

Setter 주입은 객체 생성 후 필수 의존성이 비어 있는 중간 상태를 만들 수 있고, use case controller/service처럼 항상 필요한 collaborator가 명확한 class에는 적합도가 낮다. 따라서 구현 기준은 생성자 주입을 기본으로 한다.

예외적으로 테스트 fixture나 optional 설정이 필요한 경우가 생기더라도, 핵심 repository/policy/clock/authentication 의존성은 생성자 주입을 우선한다.

## Factory와 Composition Root

현재 실행 조립의 중심은 `ApplicationContext`와 `JdbcRepositoryFactory`이다.

### `ApplicationContext`

`ApplicationContext.fromEnvironment()`는 실행 환경에서 필요한 객체를 조립하는 composition root 역할을 한다.

현재 dev 기준 역할은 다음과 같다.

- `ITS_DB_URL`, `ITS_DB_USER`, `ITS_DB_PASSWORD` 환경변수 확인
- `DriverManagerConnectionProvider` 생성
- `DatabaseInitializer.initialize(connectionProvider)` 호출
- `JdbcRepositoryFactory` 생성
- `AuthenticationService` 생성
- UI/presenter에 repository interface 전달

즉, application/service/controller가 직접 JDBC 구현체를 생성하지 않고, 실행 시작 지점에서 조립한다.

### `JdbcRepositoryFactory`
`JdbcRepositoryFactory`는 JDBC repository 구현체를 생성하는 factory이다.

외부에는 다음과 같은 repository interface 타입으로 노출한다.

- `UserRepository`
- `ProjectRepository`
- `IssueRepository`
- `CommentRepository`
- `IssueHistoryRepository`
- `IssueDependencyRepository`
- `StatisticsRepository`
- `AssignmentRecommendationRepository`

내부에서는 `JdbcUserRepository`, `JdbcProjectRepository`, `JdbcIssueRepository` 같은 JDBC 구현체를 생성한다. 이 덕분에 application/service 계층은 JDBC class가 아니라 repository interface에 의존한다.

## Controller-Service-Repository Wiring

현재 dev 구현에서 assignment/status 흐름은 DCD ver1보다 service 계층이 더 명확하게 분리되어 있다.

### Assignment Flow

현재 구현 구조는 다음과 같다.
AssignmentController
  -> AuthenticationService
  -> AssignmentService

AssignmentService
  -> IssueRepository
  -> UserRepository
  -> PermissionPolicy
  -> AssignmentRecommendationService
  -> Clock

`AssignmentController`의 책임은 다음 정도로 제한된다.

- 현재 로그인 사용자 확인
- UC5 system operation 수신
- `AssignmentService`로 요청 위임

`AssignmentService`의 책임은 다음이다.

- 대상 issue 조회
- 현재 사용자 및 assignee/verifier 사용자 조회
- PL assignment 권한 검사
- assignment 시작 시 추천 후보 조회
- 상태별 assignment branch 처리
- `Issue.assignFromNew`, `Issue.assignReopened`, `Issue.reassignAssignee`, `Issue.changeVerifier` 호출
- 변경된 issue 저장

따라서 DCD ver1에서 `AssignmentController`가 `IssueRepository`, `UserRepository`, `PermissionPolicy`, `AssignmentRecommendationService`, `Clock`을 직접 사용하는 것처럼 보이는 부분은 현재 구현과 다르다. 현재 구현 기준으로는 이 의존성이 `AssignmentService` 쪽에 있어야 한다.

### Issue State Flow

현재 구현 구조는 다음과 같다.
text
IssueStateController
  -> AuthenticationService
  -> IssueStateService

IssueStateService
  -> IssueRepository
  -> UserRepository
  -> PermissionPolicy
  -> IssueResolutionGuard
  -> Clock
`IssueStateController`의 책임은 다음이다.

- 현재 로그인 사용자 확인
- `changeStatus(issueId, targetStatus, comment)` system operation 수신
- `IssueStateService`로 요청 위임

`IssueStateService`의 책임은 다음이다.

- 대상 issue 조회
- 현재 사용자 조회
- target status별 권한 검사
- 필수 comment 검증
- `targetStatus=RESOLVED`일 때만 `IssueResolutionGuard` 호출
- `Issue.markFixed`, `Issue.resolve`, `Issue.rejectFix`, `Issue.close`, `Issue.reopen` domain operation 호출
- comment 기록
- 변경된 issue 저장

#43 Tester reject fix는 같은 구조로 구현한다. 별도 operation 이름을 만들지 않고 `changeStatus(issueId, targetStatus=ASSIGNED, comment)` branch에서 `Issue.rejectFix`를 호출한다.

#47 Reopen은 같은 `changeStatus(issueId, targetStatus=REOPENED, comment)` route에서 구현한다. `Issue.reopen`이 assignee/verifier 제거와 fixer/resolver 보존 정책을 소유하고, PL 권한 검사는 `PermissionPolicy`가 담당한다.

#98 dependency-based resolve guard는 `IssueStateService`의 RESOLVED branch에서만 실행한다. `DependencyResolutionGuard`는 blocked issue의 dependency edge를 `IssueDependencyRepository.findByBlockedIssueId(issue.id())`로 읽고, 각 blocking issue를 `IssueRepository`에서 조회한다. 모든 blocker가 `RESOLVED` 또는 `CLOSED` 상태일 때만 FIXED -> RESOLVED 전이를 허용하며, 누락되었거나 아직 완료되지 않은 blocker가 있으면 domain mutation과 status-change comment 기록 전에 실패한다. 이 정책은 `BLOCK`/`BLOCKED` 상태를 추가하지 않고, `Priority.BLOCKER`도 사용하지 않는다.

### Issue Dependency Flow

현재 #45 dependency workflow 구조는 다음과 같다.
text
IssueDependencyController
  -> AuthenticationService
  -> IssueDependencyService

IssueDependencyService
  -> IssueRepository
  -> IssueDependencyRepository
  -> IssueDependencyChangeRepository
  -> UserRepository
  -> PermissionPolicy
  -> Clock

`IssueDependencyController`의 책임은 다음이다.

- 현재 로그인 사용자 확인
- `addDependency(blockingIssueId, blockedIssueId)`, `listDependencies(issueId)`, `removeDependency(dependencyId)` system operation 수신
- 로그인 사용자의 `loginId`를 `IssueDependencyService`로 전달

`IssueDependencyService`의 책임은 다음이다.

- blocking issue, blocked issue, 현재 사용자, 저장된 dependency 조회
- `PermissionPolicy.assertCanManageDependency`를 통한 PL 권한 검사
- 자기 의존성, 저장소 기준 중복, 저장소 edge 기반 cycle 검사
- `Issue.addDependency`와 `Issue.recordDependencyRemoved`를 통한 aggregate-local dependency history 기록
- `IssueDependencyRepository`를 통한 dependency 조회, 목록, 중복, cycle 검사
- `IssueDependencyChangeRepository`를 통한 dependency 저장/삭제와 blocked issue history 저장

`IssueDependencyChangeRepository`는 dependency relation row와 blocked issue의 `DEPENDENCY_CHANGED` history를 하나의 persistence operation으로 묶는 write contract이다. JDBC 구현에서는 같은 connection에서 autocommit을 끄고 dependency insert/update 또는 delete와 transient history insert를 같은 transaction으로 commit한다. 삭제 경로는 stale dependency id가 이력만 남기는 것을 막기 위해 정확히 1개 row가 삭제된 경우에만 removal history를 insert한다. 따라서 service는 dependency 저장/삭제 뒤에 `IssueRepository.save(blockedIssue)`를 별도로 호출하지 않는다.

이 경계에서 `Priority.BLOCKER`는 dependency workflow와 분리한다. BLOCKER priority는 우선순위 값이고, #45의 dependency 추가/목록/삭제는 `IssueDependency` 관계와 `DEPENDENCY_CHANGED` 이력만 다룬다. FIXED -> RESOLVED dependency guard(#98)는 dependency CRUD를 변경하지 않고 status workflow/service 쪽 resolve eligibility 정책으로 둔다.

## 구현 가이드라인

앞으로 구현할 때는 다음 기준을 따른다.

| 상황 | 권장 위치 |
| --- | --- |
| UI 입력 수집 | JavaFX/Swing View |
| system operation 수신 | Controller |
| 로그인 사용자 확인 | Controller 또는 AuthenticationService 호출부 |
| 여러 repository 조회가 필요한 흐름 | Service |
| 권한 검사 | PermissionPolicy |
| 추천 후보 계산 | AssignmentRecommendationService 또는 내부 strategy |
| 상태 전이/assignment 규칙 | Issue domain object |
| 저장 | Repository interface |
| JDBC 구현체 생성 | JdbcRepositoryFactory |
| 실행 환경별 객체 조립 | ApplicationContext |

Controller가 repository, policy, clock, recommendation service를 직접 들고 조정하는 구조는 legacy 예외로만 남긴다. 신규 구현과 clean-code slice에서는 controller가 system operation 수신과 service 위임을 담당하고, repository interface 조정은 service 뒤로 이동한다.

## DCD 반영 기준

`dcd-ver1`은 layered implementation overview이므로 현재 구현과 맞게 service 계층을 반영하는 편이 좋다.

반영 대상은 다음이다.

- `AssignmentService` 추가
- `IssueStateService` 추가
- `AssignmentController`의 repository/policy/clock 직접 dependency를 `AssignmentService`로 이동
- `IssueStateController`의 repository/policy/clock 직접 dependency를 `IssueStateService`로 이동

`dcd-ver2`는 core workflow DCD이므로 수정하지 않는다. ver2는 repository/authentication/clock infrastructure를 의도적으로 생략한 보고서 본문용 요약 DCD로 유지한다.

## Executable Architecture Boundary Guard

`ArchitectureBoundaryTest`는 production Java source의 `import` 선언을 스캔해서 MVC/GRASP/SOLID 패키지 의존성 방향을 실행 가능한 규칙으로 검증한다. 이 테스트는 현재 DCD 설명이 코드 구조와 함께 유지되도록 하는 경계 가드이다.

가드 규칙은 다음이다.

- `domain`은 `controller`, `service`, `repository`, `persistence`, `ui`, `config`, `technical`을 import하지 않는다.
- `service`는 `controller`, `persistence`, `ui`, `config`를 import하지 않는다. Repository interface import는 허용한다.
- `controller`는 `repository`, `persistence`, `ui`를 import하지 않는다.
- `ui`는 `repository`, `persistence`를 import하지 않는다.

현재 dev 구현에는 임시 예외가 없다. 예외는 영구 설계가 아니라 debt marker로만 허용하며, 새 예외를 추가해야 할 때는 해당 clean-code slice에서 제거 계획을 같이 남긴다.

Controller layer의 repository 직접 의존 예외는 clean-code slice에서 `AccountService`, `DeletedIssueService`, `StatisticsService`로 이동했다. 이 상태를 유지하기 위해 신규 controller는 repository interface를 직접 import하지 않는다.

UI layer의 repository 직접 의존 예외는 clean-code slice에서 `DashboardSummaryService` 뒤로 이동했다. 이 상태를 유지하기 위해 신규 JavaFX presenter/view는 repository interface를 직접 import하지 않는다.

Review comment 정책은 다음 기준을 따른다.

- 임시 architecture exception, non-obvious responsibility boundary, side effect를 막는 ordering rule에는 짧은 code comment를 남긴다.
- 명백한 assignment, getter, 단순 delegation에는 comment를 남기지 않는다.
- debt를 표시하는 comment는 제거를 맡을 slice/workflow 이름을 같이 적는다.

## 주의사항

- 생성자 주입인지 setter 주입인지까지 DCD에 과하게 그리지 않는다.
- DCD에서는 dependency 방향과 interface realization을 보여 주고, 조립 방식은 이 문서에서 설명한다.
- DB table, SQL, DTO 내부 field, UI widget 세부사항은 DCD에 넣지 않는다.
- Domain Model을 Java class diagram처럼 바꾸지 않는다.
- `Role`은 subclass가 아니라 `User.role: Role` enum으로 유지한다.
- abstract class는 현재 책임 배치상 추가하지 않는다.
