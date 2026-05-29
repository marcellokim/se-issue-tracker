# ITS DCD 문서 설명

이 문서는 현재 코드 기준으로 `DCD_ver1`, `DCD_ver2`를 어떻게 읽어야 하는지 정리한다.

## 문서 역할

| 문서 | 역할 | 사용 위치 |
|---|---|---|
| `its_dcd_ver1.puml` | 현재 구현 코드의 레이어 분리와 의존성 방향을 보여주는 구현 관점 DCD | 구현 설명, 아키텍처 검토, appendix |
| `its_dcd_ver2.puml` | Larman/GRASP 관점의 설계 클래스 다이어그램 | 최종 보고서 본문, 설계 설명 |

## DCD ver1: 현재 구현 레이어 관점

`DCD_ver1`은 현재 코드의 실제 패키지 구조와 의존성 방향을 반영한다.

현재 기준의 핵심 구조는 다음과 같다.

- `Presentation / Entry Points`
  - `Main`, `CommandLineEntryPoint`, `ConsoleCliOutput`
  - 현재 완성된 JavaFX/Swing View 구현은 DCD에 concrete boundary로 넣지 않는다.
  - 향후 UI는 Controller만 호출해야 하며 Service, Repository, JDBC를 직접 호출하지 않는다.
- `Composition Root / Runtime Config`
  - `ApplicationBootstrap`, `ApplicationContext`
  - 실제 구현체 조립 책임을 가진다.
  - `PasswordHasher`, `SessionStore`, `SystemClock`, `CommentIdGenerator`, JDBC repository factory를 여기서 조립한다.
- `Controller Layer`
  - `AuthenticationController`, `DashboardController`, `AccountController`, `ProjectController`, `IssueController`, `AssignmentController`, `IssueStateController`, `DeletedIssueController`, `StatisticsController`
  - Controller는 현재 로그인 사용자 확인 후 Service로 위임한다.
  - Controller는 Repository/JDBC를 직접 호출하지 않는다.
- `Service Layer`
  - `IssueService`, `ProjectService`, `AccountService`, `AssignmentService`, `IssueStateService`, `DeletedIssueService`, `StatisticsService`, `DashboardSummaryService` 등
  - 권한/정책 검사는 `PermissionPolicy`와 각 Service의 프로젝트 소속/책임 검사가 함께 담당한다.
  - `Clock`, `CurrentUserSession`, `CommentIdProvider`, `PasswordHashing`은 service layer의 port로 둔다.
- `Repository Ports`
  - `UserRepository`, `ProjectRepository`, `IssueRepository`, `CommentRepository`, `IssueHistoryRepository`, `IssueDependencyRepository`, `StatisticsRepository`, `AssignmentRecommendationRepository`
  - Service는 이 인터페이스에 의존한다.
- `Persistence Adapters`
  - `JdbcUserRepository`, `JdbcProjectRepository`, `JdbcIssueRepository` 등
  - Repository port를 구현한다.
- `Technical Adapters`
  - `SystemClock`, `SessionStore`, `CommentIdGenerator`, `PasswordHasher`
  - service port를 구현한다.
- `Domain Layer`
  - `User`, `Project`, `ProjectMember`, `Issue`, `Comment`, `IssueHistory`, `IssueDependency`
  - `IssueStatus`, `Priority`, `Role`, `CommentPurpose`, `ActionType`

## DCD ver2: Larman / GRASP 설계 관점

`DCD_ver2`는 코드 패키지 전체를 그대로 그리는 다이어그램이 아니다. Larman 방식에 맞춰 system operation을 받는 Controller, application service, 핵심 domain class의 협력을 보여준다.

따라서 다음 구현 세부는 의도적으로 제외한다.

- JDBC 구현체
- Repository factory
- Database connection provider
- Password/session/clock/comment id 기술 구현체
- UI widget 또는 화면 컴포넌트
- 단순 Result/DTO record
- Bootstrap 조립 코드

`DCD_ver2`에서 강조하는 설계 포인트는 다음과 같다.

- Controller는 SSD의 system operation을 받는 GRASP Controller이다.
- Service는 여러 Repository 조회, 권한 검사, transaction 성격의 orchestration을 담당하는 Pure Fabrication/Indirection이다.
- `PermissionPolicy`는 역할 기반 권한 검사를 중앙화한다.
- `Issue`는 상태 전이, 배정 변경, 댓글, 의존성, history 생성의 Information Expert이다.
- `Project`는 issue 목록을 in-memory collection으로 소유하지 않는다. 이슈는 `Issue.projectId`로 프로젝트와 연결된다.
- Admin 프로젝트 상세 조회는 프로젝트 기본 정보, 참여자 정보, 프로젝트 이슈 목록을 반환한다.
- 일반 사용자 프로젝트 상세 화면은 프로젝트 기본 정보만 반환하며, 별도 이슈 목록은 `IssueService` 또는 `DashboardSummaryService`를 통해 조회한다.
- deleted 이슈 관리는 일반 이슈 조회/조작 경로와 분리되어 `DeletedIssueService`가 담당한다.

## 현재 설계 기준

현재 코드 기준으로 문서에 반영된 주요 결정은 다음과 같다.

- UI는 Controller만 호출한다.
- Service는 Domain, Repository port, Service port를 사용한다.
- Service가 `technical` 구현체를 직접 import하지 않는다.
- `PasswordHashing`, `CurrentUserSession`, `Clock`, `CommentIdProvider`는 service port이다.
- `PasswordHasher`, `SessionStore`, `SystemClock`, `CommentIdGenerator`는 technical adapter이다.
- `JdbcRepositoryFactory`는 `PasswordHashing`을 외부에서 주입받는다.
- `IssueDetailResult`에는 상세 화면에서 이어질 수 있는 workflow action 이름 목록인 `availableActions`를 포함한다.
- 이슈 검색은 프로젝트 내부 검색을 기준으로 한다.
- deleted 이슈는 일반 이슈 조회/조작 경로에서 차단하고 deleted issue workflow로 관리한다.
- 단건 deleted issue 물리 삭제는 `DeletedIssueController.purgeDeletedIssue`와 `DeletedIssueService.purgeDeletedIssue` 경로로 분리한다.

## GRASP 적용 요약

### Controller

다음 Controller들이 system operation을 받는다.

- `AuthenticationController`
- `DashboardController`
- `AccountController`
- `ProjectController`
- `IssueController`
- `AssignmentController`
- `IssueStateController`
- `DeletedIssueController`
- `StatisticsController`

Controller는 사용자의 요청을 Service에 전달하는 역할에 집중한다.

### Information Expert

`Issue`는 다음 책임의 중심이다.

- 이슈 제목/설명 변경
- 우선순위 변경
- 담당자/검증자 배정 및 변경
- 상태 전이
- 댓글 생성/수정/삭제 history 기록
- 의존성 추가/삭제 history 기록

`User`는 계정 상태, 역할, 이름 변경을 관리한다.

`Project`는 프로젝트 이름/설명 변경을 관리한다.

### Creator

`Issue`는 자신의 aggregate 내부에 속하는 `Comment`, `IssueHistory`, `IssueDependency` 생성 흐름에 관여한다. 다만 실제 DB 저장, id 생성, transaction 경계는 service/repository 계층의 책임이다.

### Low Coupling / High Cohesion

- Controller는 Repository/JDBC를 모른다.
- Service는 JDBC 구현체를 모른다.
- Domain은 Service, Repository, Persistence, Technical 계층을 모른다.
- Technical adapter는 service port를 구현한다.

### Pure Fabrication / Indirection

- `PermissionPolicy`: 권한 정책 중앙화
- `AssignmentRecommendationService`: 배정 후보 추천 정책 분리
- `DashboardSummaryService`: 대시보드용 조회 결과 조합
- `StatisticsService`: 통계 조회 정책과 결과 조합
- Repository interface: persistence 구현에 대한 간접화

## 향후 문서 유지 기준

코드를 변경할 때 다음 항목이 바뀌면 DCD도 함께 갱신한다.

- Controller public API 변경
- Service public API 변경
- 권한/정책 책임 위치 변경
- Repository port 추가/삭제
- Technical adapter와 service port 구조 변경
- Domain class의 주요 책임 변경
- `Project`와 `Issue` 관계 정책 변경
- deleted issue workflow 변경
