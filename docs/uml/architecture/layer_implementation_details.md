# DCD Implementation Details

이 문서는 기존 DCD/GRASP/MVC 구현 기준선 문서와 별개로, 현재 dev 브랜치 구현이 의존성 주입, composition root, factory, controller-service-repository wiring을 어떻게 다루는지 정리함. 목적은 이후 구현자가 DCD를 보고 코드를 작성할 때 application/service/repository 책임을 같은 방향으로 맞추는 것임.

## 현재 구현 방향 요약

현재 dev 구현은 setter 주입이 아니라 생성자 주입을 기본 방향으로 사용함.

Controller와 Service는 필요한 collaborator를 생성자에서 전달받고, `private final` field로 보관함. 예: `AssignmentController`는 `AuthenticationService`와 `AssignmentService`를 생성자에서 받고, `AssignmentService`는 `IssueRepository`, `UserRepository`, `PermissionPolicy`, `AssignmentRecommendationService`, `Clock`을 생성자에서 받음.

구조 기준:

- 장기 collaborator는 생성자 주입 사용.
- Controller는 UI 요청의 첫 non-UI 수신자 역할.
- Controller가 복잡한 repository 조회, 권한 검사, 상태별 branch, 저장 흐름을 모두 직접 수행하지 않음.
- 복잡한 application orchestration은 service로 분리.
- Repository는 interface로 받고, JDBC 구현체에 직접 의존하지 않음.
- 실행 시점의 실제 구현체 조립은 composition root/factory에서 처리.

## Setter Injection

현재 확인한 dev 구조에서는 controller/service 핵심 collaborator를 setter로 바꾸는 방향 아님.

Setter 주입은 객체 생성 후 필수 의존성이 비어 있는 중간 상태를 만들 수 있고, use case controller/service처럼 항상 필요한 collaborator가 명확한 class에는 적합도 낮음. 따라서 구현 기준은 생성자 주입 기본.

테스트 fixture나 optional 설정이 필요한 예외가 생기더라도, 핵심 repository/policy/clock/authentication 의존성은 생성자 주입 우선.

## Factory와 Composition Root

현재 실행 조립의 중심은 `ApplicationBootstrap`과 `JdbcRepositoryFactory`.

### `ApplicationContext` / `ApplicationBootstrap`

`ApplicationBootstrap`는 Oracle 환경에서 실제 runtime collaborator를 조립하는 composition root.

역할:

- `DatabaseEnvironment`를 통한 `ITS_DB_URL`, `ITS_DB_USER`, `ITS_DB_PASSWORD` 환경변수 확인/읽기
- `DriverManagerConnectionProvider` 생성
- `DatabaseInitializer.initialize(connectionProvider)` 호출
- `JdbcRepositoryFactory` 생성
- UI, CLI가 공유하는 application service와 presenter collaborator 생성
- CLI 진단 출력에 필요한 `DatabaseConnectionSummary` 생성

`ApplicationContext`는 JavaFX 실행에 필요한 `AuthenticationService`와 `DemoDashboardPresenter`를 노출하는 얇은 UI context로 유지.

즉, application/service/controller/UI/CLI가 직접 JDBC 구현체를 생성하지 않고 실행 시작 지점에서 조립.

### CLI Boundary

`CommandLineEntryPoint`는 기존 `--cli-demo`와 `--login-check` mode를 선택하고, 각 command object로 위임.

CLI command는 service 결과를 출력 형식으로 변환. CLI command가 repository interface나 JDBC 구현체를 직접 import하지 않도록 `ArchitectureBoundaryTest`가 `cli` package의 repository/persistence import 차단.

이 구조는 새 CLI 기능 추가가 아니라, 기존 CLI smoke/demo 흐름의 presentation 책임을 `Main`에서 분리한 리팩토링임.

### `JdbcRepositoryFactory`

`JdbcRepositoryFactory`는 JDBC repository 구현체를 생성하는 factory.

외부 노출 타입:

- `UserRepository`
- `ProjectRepository`
- `IssueRepository`
- `CommentRepository`
- `IssueHistoryRepository`
- `IssueDependencyRepository`
- `StatisticsRepository`
- `AssignmentRecommendationRepository`

내부에서는 `JdbcUserRepository`, `JdbcProjectRepository`, `JdbcIssueRepository` 같은 JDBC 구현체 생성. 이 덕분에 application/service 계층은 JDBC class가 아니라 repository interface에 의존.

### JDBC Issue Repository Split

`JdbcIssueRepository`는 `IssueRepository` contract를 유지하되 내부 책임을 package-private helper로 분리.

- `JdbcIssueQueries`: issue 조회/search SQL과 binder 순서
- `JdbcIssueRowMapper`: issue row와 joined user mapping
- `JdbcIssueWriteSupport`: transient comment/history와 status history insert
- `JdbcIssueDeleteOperations`: soft-delete, restore, purge transaction workflow

이 분리는 SQL 결과, 정렬, deleted issue 포함/제외 기준, rollback, generated key handling, comment/history persistence order를 바꾸지 않는 pure refactor.

## Controller-Service-Repository Wiring

현재 dev 구현에서 assignment/status 흐름은 Layer Architecture View보다 service 계층이 더 명확하게 분리됨.

### Assignment Flow

현재 구현 구조:

```text
AssignmentController
  -> AuthenticationService
  -> AssignmentService

AssignmentService
  -> IssueRepository
  -> UserRepository
  -> PermissionPolicy
  -> AssignmentRecommendationService
  -> Clock
```

`AssignmentController` 책임:

- 현재 로그인 사용자 확인
- UC5 system operation 수신
- `AssignmentService`로 요청 위임

`AssignmentService` 책임:

- 대상 issue 조회
- 현재 사용자 및 assignee/verifier 사용자 조회
- PL assignment 권한 검사
- assignment 시작 시 추천 후보 조회
- 상태별 assignment branch 처리
- `Issue.assignFromNew`, `Issue.assignReopened`, `Issue.reassignAssignee`, `Issue.changeVerifier` 호출
- 변경된 issue 저장

따라서 Layer Architecture View에서 `AssignmentController`가 `IssueRepository`, `UserRepository`, `PermissionPolicy`, `AssignmentRecommendationService`, `Clock`을 직접 사용하는 것처럼 보이는 부분은 현재 구현과 다름. 현재 구현 기준으로는 이 의존성이 `AssignmentService` 쪽에 있어야 함.

### Issue State Flow

현재 구현 구조:

```text
IssueStateController
  -> AuthenticationService
  -> IssueStateService

IssueStateService
  -> IssueRepository
  -> UserRepository
  -> PermissionPolicy
  -> Clock
```

`IssueStateController` 책임:

- 현재 로그인 사용자 확인
- `changeStatus(issueId, targetStatus, comment)` system operation 수신
- `IssueStateService`로 요청 위임

`IssueStateService` 책임:

- 대상 issue 조회
- 현재 사용자 조회
- target status별 권한 검사
- 필수 comment 검증
- `Issue.markFixed`, `Issue.resolve`, `Issue.close` 등 domain operation 호출
- comment 기록
- 변경된 issue 저장

#43 Tester reject fix도 같은 구조로 확장 가능. 단, operation 이름은 새로 만들지 않고 `changeStatus(issueId, targetStatus=ASSIGNED, comment)` branch로 유지.

## 구현 가이드라인

앞으로 구현할 때 따를 기준:

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
| 실행 환경별 runtime 객체 조립 | ApplicationBootstrap |

Controller가 repository, policy, clock, recommendation service를 직접 들고 조정하는 구조는 legacy 예외로만 남김. 신규 구현과 clean-code slice에서는 controller가 system operation 수신과 service 위임을 담당하고, repository interface 조정은 service 뒤로 이동.

## DCD 반영 기준

`its_layer_architecture.puml`은 layered implementation overview이므로 현재 구현과 맞게 service 계층 반영 권장.

반영 대상:

- `IssueService` 추가
- `AssignmentService` 추가
- `IssueStateService` 추가
- `DeletedIssueService` 추가
- `AccountService` 추가
- `StatisticsService` 추가
- `IssueController`의 repository/policy/clock 직접 dependency를 `IssueService`로 이동
- `AssignmentController`의 repository/policy/clock 직접 dependency를 `AssignmentService`로 이동
- `IssueStateController`의 repository/policy/clock 직접 dependency를 `IssueStateService`로 이동
- `DeletedIssueController`, `AccountController`, `StatisticsController`의 repository/policy/clock 직접 dependency를 각 application service로 이동

`dcd-ver2`는 core workflow DCD이므로 수정하지 않음. ver2는 repository/authentication/clock infrastructure를 의도적으로 생략한 보고서 본문용 요약 DCD로 유지.

## Executable Architecture Boundary Guard

`ArchitectureBoundaryTest`는 production Java source의 `import` 선언을 스캔해서 MVC/GRASP/SOLID 패키지 의존성 방향을 실행 가능한 규칙으로 검증. 이 테스트는 현재 DCD 설명이 코드 구조와 함께 유지되도록 하는 경계 가드.

가드 규칙:

- `domain`은 `controller`, `service`, `repository`, `persistence`, `ui`, `config`, `technical`을 import하지 않음.
- `service`는 `controller`, `persistence`, `ui`, `config`를 import하지 않음. Repository interface import는 허용.
- `controller`는 `repository`, `persistence`, `ui`를 import하지 않음.
- `ui`는 `repository`, `persistence`를 import하지 않음.

현재 확인된 boundary guard 기준에는 임시 예외 없음. 예외는 영구 설계가 아니라 debt marker로만 허용하며, 새 예외를 추가해야 할 때는 해당 clean-code slice에서 제거 계획도 같이 기록.

Controller layer의 repository 직접 의존 예외는 clean-code slice에서 `IssueService`, `AssignmentService`, `IssueStateService`, `AccountService`, `DeletedIssueService`, `StatisticsService`로 이동됨. 이 상태 유지를 위해 신규 controller는 repository interface를 직접 import하지 않음.

UI layer의 repository 직접 의존 예외는 clean-code slice에서 `DashboardSummaryService` 뒤로 이동됨. 이 상태 유지를 위해 신규 JavaFX presenter/view는 repository interface를 직접 import하지 않음.

Review comment 정책:

- 임시 architecture exception, non-obvious responsibility boundary, side effect를 막는 ordering rule에는 짧은 code comment 남김.
- 명백한 assignment, getter, 단순 delegation에는 comment 남기지 않음.
- debt를 표시하는 comment는 제거를 맡을 slice/workflow 이름도 같이 기록.
- 리팩토링 PR의 문서 변경은 구조와 책임 배치 설명에 한정. 별도 feature PR과 테스트 근거가 없는 기능은 traceability나 README에서 완료로 표시하지 않음.

## 주의사항

- 생성자 주입인지 setter 주입인지까지 DCD에 과하게 그리지 않음.
- DCD에서는 dependency 방향과 interface realization을 보여 주고, 조립 방식은 이 문서에서 설명.
- DB table, SQL, DTO 내부 field, UI widget 세부사항은 DCD에 넣지 않음.
- Domain Model을 Java class diagram처럼 바꾸지 않음.
- `Role`은 subclass가 아니라 `User.role: Role` enum으로 유지.
- abstract class는 현재 책임 배치상 추가하지 않음.
