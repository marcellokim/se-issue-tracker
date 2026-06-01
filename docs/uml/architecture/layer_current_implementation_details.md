# DCD Implementation Details

이 문서는 현재 코드 구현을 기준으로 DCD 문서가 어떤 책임 분리를 표현해야 하는지 정리한다.

## 현재 구현 방향 요약

현재 구현은 다음 레이어 방향을 기준으로 한다.

```text
Presentation / CLI
  -> Controller
  -> Service
  -> Repository Port / Domain / Service Port
  -> Persistence Adapter / Technical Adapter
```

핵심 원칙은 다음과 같다.

- UI와 CLI는 Controller를 통해서만 하위 기능을 호출한다.
- Controller는 현재 로그인 사용자 확인 후 Service로 위임한다.
- Controller는 Repository, JDBC, Technical adapter를 직접 사용하지 않는다.
- Service는 권한/정책 검사와 use case 흐름 조합을 담당한다.
- Service는 JDBC 구현체가 아니라 Repository interface에 의존한다.
- Service가 필요로 하는 기술 기능은 service port로 둔다.
- Technical adapter는 service port를 구현한다.
- 실제 구현체 생성과 조립은 `ApplicationBootstrap`에서 수행한다.

## Composition Root

`ApplicationBootstrap`은 현재 실행 환경의 composition root이다.

주요 책임은 다음과 같다.

- DB 환경 정보 확인
- `DriverManagerConnectionProvider` 생성
- `DatabaseInitializer` 실행
- `JdbcRepositoryFactory` 생성
- `PasswordHasher`, `SessionStore`, `SystemClock`, `CommentIdGenerator` 생성
- Repository, Service, Controller 조립
- `ApplicationContext` 반환

이 구조 때문에 Service나 Controller가 `technical` 구현체를 직접 생성하지 않아도 된다.

## Service Port / Technical Adapter 분리

현재 service layer에 둔 port는 다음과 같다.

| Service Port | Technical Adapter | 목적 |
|---|---|---|
| `PasswordHashing` | `PasswordHasher` | 비밀번호 해시/검증 |
| `CurrentUserSession` | `SessionStore` | 현재 로그인 사용자 세션 관리 |
| `Clock` | `SystemClock` | 현재 시각 공급 |
| `CommentIdProvider` | `CommentIdGenerator` | 댓글 식별자 생성 |

이 구조의 목적은 service layer가 technical implementation을 직접 알지 않게 하는 것이다.

## Repository Factory

`JdbcRepositoryFactory`는 JDBC repository 구현체를 생성하는 persistence factory이다.

현재 기준은 다음과 같다.

- `JdbcRepositoryFactory`는 `PasswordHashing`을 생성하지 않는다.
- `PasswordHashing`은 `ApplicationBootstrap`에서 생성되어 주입된다.
- `JdbcUserRepository`도 `PasswordHasher`가 아니라 `PasswordHashing` port를 받는다.
- JDBC repository는 repository interface를 구현한다.

## Layer Architecture View 반영 기준

`its_layer_architecture.puml`은 현재 구현 레이어를 보여준다.

반영해야 할 주요 요소는 다음과 같다.

- Presentation/Entry Point
  - `Main`
  - `CommandLineEntryPoint`
  - `ConsoleCliOutput`
- Composition Root
  - `ApplicationBootstrap`
  - `ApplicationContext`
- Controller Layer
  - 현재 존재하는 모든 Controller
- Service Layer
  - 현재 존재하는 주요 Service
  - `PermissionPolicy`
  - service port
  - Result/DTO record
- Repository Ports
  - repository interface
- Persistence Adapters
  - JDBC repository 구현체
  - `JdbcRepositoryFactory`
- Technical Adapters
  - `PasswordHasher`
  - `SessionStore`
  - `SystemClock`
  - `CommentIdGenerator`
- Domain Layer
  - `User`, `Project`, `ProjectMember`, `Issue`, `Comment`, `IssueHistory`, `IssueDependency`
  - enum/value object

`Layer Architecture View`는 구현 관점 문서이므로 repository, technical adapter, composition root까지 포함한다.

## DCD 반영 기준

`its_dcd.puml`은 도메인 모델을 설계 클래스 수준으로 확장한 Larman/GRASP 관점 DCD이다.

따라서 다음 구현 세부는 제외한다.

- Controller와 Service 조율 구조
- JDBC repository 구현체
- `JdbcRepositoryFactory`
- DB connection provider
- Bootstrap 조립 코드
- Technical adapter
- 단순 DTO/Result record
- UI widget

대신 다음을 중심으로 표현한다.

- 핵심 domain class와 주요 attribute
- domain object가 직접 수행하는 operation
- domain class 사이의 association, role name, multiplicity, navigability
- `Issue`가 담당하는 상태 전이, 배정, 댓글, 의존성 변경 책임
- `Project`, `User`, `ProjectMember`, `Comment`, `IssueHistory`, `IssueDependency`의 도메인 관계

## 현재 주요 설계 결정

### Project와 Issue 관계

`Project`는 issue list를 직접 소유하지 않는다.

현재 설계에서는 다음 방식으로 관계를 표현한다.

- `Issue.projectId`가 프로젝트 소속을 나타낸다.
- 프로젝트 삭제, 참여자 관리, 이슈 조회 조합은 Service/Repository가 담당한다.
- Admin project detail은 project와 participants만 포함하며, 프로젝트 issue list는 포함하지 않는다.
- Non-admin project 화면에 필요한 이슈 목록은 별도 issue 조회 경로로 가져온다.

### Issue Detail과 availableActions

현재 `IssueDetailResult`에는 `availableActions`가 있다.

이 결정은 다음 의미를 가진다.

- 상세 조회 DTO가 댓글, 이력, 의존성뿐 아니라 상세 화면에서 노출할 수 있는 workflow action 이름 목록을 함께 제공한다.
- `IssueController`는 `IssueWorkflowService`가 주입된 경우 `viewIssueDetail` 응답에 action 이름을 채운다.
- 상태 전이, 배정, 댓글 수정/삭제 가능 여부는 여전히 Controller/Service operation과 `PermissionPolicy.assertCan...` 검사로 최종 보장한다.

### Deleted Issue Workflow

deleted 이슈는 일반 이슈 조회/조작 경로와 분리한다.

현재 deleted issue 관련 public 흐름은 다음과 같다.

- `viewDeletedIssues`
- `deleteIssue`
- `restoreIssue`
- `purgeDeletedIssue`

삭제 이슈 보관 개수 초과분 정리는 `deleteIssue` 내부에서 수행한다. 단건 물리 삭제는 `purgeDeletedIssue`로 명시적으로 분리되어 있으며, DELETED 상태의 이슈만 대상으로 한다.

### Issue Dependency Workflow

이슈 의존성은 같은 프로젝트 내 이슈들끼리만 허용한다.

현재 정책은 다음과 같다.

- 의존성 관리는 프로젝트 PL만 가능하다.
- 의존성 조회는 해당 프로젝트 소속 PL/DEV/TESTER가 가능하다.
- `addDependency`는 blocking issue와 blocked issue가 같은 프로젝트인지 검사한다.
- `removeDependency`는 blocking issue id와 blocked issue id를 함께 받아 삭제한다.
- deleted 이슈 경로에서는 일반 dependency 조작을 차단한다.

## Controller-Service 책임 기준

Controller가 맡는 책임:

- 현재 로그인 사용자 조회
- Service 호출
- Service 결과 반환

Service가 맡는 책임:

- 입력값 검증
- 권한/정책 검사
- domain operation 호출
- repository 조회/저장 조합
- result/DTO 변환

Domain이 맡는 책임:

- 자기 상태 변경
- 상태 전이 규칙의 핵심 불변식 유지
- history/comment/dependency 같은 aggregate 내부 변경 생성

Repository가 맡는 책임:

- DB 조회/저장
- SQL 실행
- row mapping
- persistence-specific cascade/transaction handling

## Architecture Boundary Guard

현재 아키텍처 테스트는 다음 방향을 지키도록 보강되어 있다.

- domain은 outer layer를 import하지 않는다.
- service는 controller, persistence, ui, config, technical implementation을 직접 import하지 않는다.
- controller는 repository, persistence, ui를 import하지 않는다.
- ui/cli는 repository, persistence를 직접 import하지 않는다.
- technical adapter는 service port를 구현한다.
- `JdbcRepositoryFactory`, `JdbcUserRepository`는 `PasswordHashing` port를 사용한다.

이 테스트는 DCD와 코드 구조가 서로 어긋나지 않도록 유지하는 실행 가능한 문서 역할을 한다.

## 문서 갱신 원칙

다음 변경이 생기면 DCD 문서를 함께 수정한다.

- Controller public API 변경
- Service public API 변경
- Repository port 추가/삭제
- Technical adapter 또는 service port 변경
- 권한/정책 책임 위치 변경
- Project-Issue 관계 정책 변경
- deleted issue workflow 변경
- assignment/status/comment/dependency workflow 변경
