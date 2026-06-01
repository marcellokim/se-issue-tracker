# OOAD / GRASP / MVC 설계 적용 근거

이 문서는 Issue Tracking System을 구현하면서 객체지향 분석/설계, GRASP, MVC를 어떤 식으로 적용했는지 정리한 것이다. 설명은 현재 로컬 코드의 `src/main/java/com/github/marcellokim/issuetracker` 구조와 `docs/uml`에 작성한 설계 문서를 기준으로 한다.

## 1. 설계 목표

이 시스템은 이슈 등록, 배정, 상태 변경, 댓글, 의존성 관리, 삭제 이슈 관리, 통계 조회를 하나의 흐름으로 다룬다. 또 과제 조건상 JavaFX와 Swing 두 UI를 모두 지원해야 하므로, 화면 구현이 바뀌어도 도메인, 서비스, 저장소 코드는 최대한 그대로 재사용할 수 있어야 했다.

그래서 전체 설계는 다음 방향으로 잡았다.

- 업무 규칙은 UI가 아니라 도메인 객체와 서비스 계층에 둔다.
- UI는 직접 업무 판단을 하지 않고 Controller를 호출한다.
- Service는 JDBC 구현체가 아니라 Repository interface와 service port를 사용한다.
- 실제 구현체 생성과 연결은 `ApplicationBootstrap`에 모은다.
- JavaFX와 Swing은 같은 `ApplicationContext`를 통해 같은 Controller, Service, Repository, Domain 계층을 사용한다.

## 2. OOAD 적용

### 2.1 도메인 개념 추출

도메인 모델을 만들 때는 Java 클래스 목록을 그대로 옮기기보다, 이슈 트래킹 업무에서 실제로 기억해야 하는 개념을 먼저 뽑았다. 현재 도메인 모델과 DCD는 아래 객체를 중심으로 정리되어 있다.

| 도메인 개념 | 코드 위치 | 의미 |
|---|---|---|
| User | `domain.User` | 사용자 계정, 역할, 활성 상태 |
| Project | `domain.Project` | 프로젝트 기본 정보 |
| ProjectMember | `domain.ProjectMember` | 프로젝트와 사용자의 참여 관계 |
| Issue | `domain.Issue` | 이슈 상태, 담당자, 댓글, 이력, 의존성 변경의 중심 |
| Comment | `domain.Comment` | 일반 댓글과 상태 변경 사유 댓글 |
| IssueHistory | `domain.IssueHistory` | 이슈 변경 이력 |
| IssueDependency | `domain.IssueDependency` | blocking issue와 blocked issue의 관계 |

특히 `ProjectMember`와 `IssueDependency`는 단순 연결선으로만 보기 어렵다. 참여 시각, dependency id, 발견 시각처럼 관계 자체가 정보를 갖기 때문에 association class 성격으로 분리했다. 이 부분은 도메인 모델을 DCD로 확장할 때도 그대로 반영했다.

### 2.2 도메인 객체와 서비스 책임 분리

도메인 객체는 자기 상태와 직접 연결된 규칙을 책임진다.

- `User`는 이름 변경, 역할 변경, 활성/비활성 변경을 담당한다.
- `Project`는 프로젝트 이름과 설명 변경을 담당한다.
- `Issue`는 제목/설명 변경, 우선순위 변경, 배정 변경, 상태 전이, 댓글/의존성 변경 이력 생성을 담당한다.

반대로 여러 객체를 조회하고 엮어야 하는 흐름은 Service에서 처리한다.

- `IssueService`는 이슈 등록, 검색, 상세 조회, 댓글, 의존성 흐름을 조율한다.
- `AssignmentService`는 이슈 상태에 맞는 배정/재배정 흐름을 처리한다.
- `IssueStateService`는 상태 변경 요청에서 권한, 프로젝트 소속, dependency guard를 함께 본다.
- `DeletedIssueService`는 soft delete, restore, 단건 purge, 보관 한도 정리를 맡는다.

이렇게 나눈 이유는 모든 규칙을 도메인 객체 하나에 넣을 수 없기 때문이다. 예를 들어 어떤 이슈를 `RESOLVED`로 바꿀 수 있는지는 해당 이슈의 상태만 봐서는 부족하다. blocking issue가 남아 있는지, 현재 사용자가 verifier인지, 프로젝트 멤버인지까지 확인해야 한다. 그래서 이런 흐름은 Service가 Repository와 Domain을 조합해서 처리하도록 했다.

### 2.3 Result DTO와 조회 모델 분리

Service는 도메인 객체를 그대로 UI에 넘기지 않고 `IssueResult`, `IssueDetailResult`, `DashboardProjectSummary`, `StatisticsReportResult` 같은 결과 객체로 바꿔서 반환한다.

이유는 크게 세 가지다.

- UI가 도메인 객체 내부 상태를 직접 바꾸지 못하게 한다.
- 화면에 필요한 정보만 골라서 넘길 수 있다.
- dashboard, statistics처럼 여러 테이블을 조합한 결과는 도메인 객체라기보다 조회 결과에 가깝다.

예를 들어 dashboard는 `DashboardSummaryRepository.DashboardProjectSnapshot`으로 조회한 값을 `DashboardProjectSummary`로 변환한다. 이 과정에서 deleted issue count처럼 화면 정책상 보여주지 않는 값은 service result에 넣지 않는다.

## 3. GRASP 적용

### 3.1 Controller

GRASP의 Controller는 외부 요청을 받아 적절한 객체로 넘기는 역할이다. 우리 코드에서는 `controller` 패키지의 클래스들이 이 역할을 맡는다.

| Controller | 맡은 흐름 |
|---|---|
| `AuthenticationController` | 로그인, 로그아웃, 현재 사용자 조회 |
| `DashboardController` | 로그인 사용자 기준 dashboard 조회 |
| `ProjectController` | 프로젝트 관리, 프로젝트 상세 조회 |
| `IssueController` | 이슈 등록, 검색, 상세 조회, 댓글, 의존성, 우선순위 변경 |
| `AssignmentController` | 이슈 배정과 배정 후보 조회 |
| `IssueStateController` | 이슈 상태 변경 |
| `DeletedIssueController` | deleted issue 조회, soft delete, restore, purge |
| `StatisticsController` | 프로젝트 통계 조회 |

Controller는 현재 로그인 사용자를 확인하고 Service에 요청을 넘기는 데 집중한다. Repository나 JDBC를 직접 호출하지 않는다. 이렇게 둔 이유는 UI와 업무 로직 사이의 경계를 단순하게 유지하기 위해서다.

### 3.2 Information Expert

Information Expert는 필요한 정보를 가장 많이 가진 객체에게 책임을 주는 원칙이다. 이 프로젝트에서 가장 대표적인 객체는 `Issue`이다.

`Issue`는 현재 상태, reporter, assignee, verifier, fixer, resolver, priority를 알고 있다. 댓글이나 이력도 이슈의 변경과 직접 연결되어 있다. 그래서 상태 전이, 배정 변경, 우선순위 변경, 댓글/의존성 변경 이력 생성은 `Issue`가 중심이 되도록 했다.

다른 예도 있다.

- `User`는 자신의 role과 active 상태를 알고 있으므로 계정 상태 변경 책임을 가진다.
- `Project`는 자신의 name과 description을 알고 있으므로 프로젝트 기본 정보 변경 책임을 가진다.
- `IssueDependency`는 blocking issue id와 blocked issue id를 함께 가지므로 이슈 간 의존 관계를 표현한다.

이렇게 하면 Service가 모든 필드를 직접 만지는 구조를 피할 수 있고, 도메인 규칙이 여러 곳에 흩어지는 것도 줄어든다.

### 3.3 Creator

Creator 원칙은 어떤 객체가 다른 객체를 생성하는 것이 자연스러운지 판단할 때 사용했다. 현재 코드에서는 `Issue`가 자기 업무 흐름 안에서 `Comment`, `IssueHistory`, `IssueDependency` 생성에 관여한다.

예를 들어 댓글 추가나 상태 변경은 결국 특정 이슈의 변화다. 따라서 이 변화에 따른 history 생성도 `Issue` 쪽에서 처리하는 편이 자연스럽다. 다만 DB id 생성, 저장, transaction 처리는 도메인이 아니라 Repository/JDBC 계층에 남겨 두었다.

### 3.4 Low Coupling / High Cohesion

레이어를 나눈 가장 큰 이유는 결합도를 낮추고 각 객체가 자기 역할에 집중하게 하기 위해서다.

- Controller는 Service만 호출한다.
- Service는 Repository interface, Domain, service port를 사용한다.
- Domain은 Controller, Service, Repository, Persistence, Technical 계층을 모른다.
- JDBC 구현체는 Repository interface를 구현한다.
- technical 구현체는 service port를 구현한다.

예를 들어 `IssueService`는 `JdbcIssueRepository`를 직접 알지 않고 `IssueRepository`를 사용한다. `AuthenticationService`도 `PasswordHasher`나 `SessionStore`를 직접 만들지 않고 `PasswordHashing`, `CurrentUserSession`을 통해 사용한다.

이 구조 덕분에 저장 방식이나 technical 구현이 바뀌어도 Service의 변경 범위를 줄일 수 있다.

### 3.5 Indirection / Protected Variations

바뀔 가능성이 있거나 외부 구현에 가까운 부분은 interface나 port로 한 번 감쌌다.

| 바뀔 수 있는 부분 | Port / Interface | 구현체 |
|---|---|---|
| DB 저장 방식 | Repository interfaces | JDBC repositories |
| 비밀번호 해시 | `PasswordHashing` | `PasswordHasher` |
| 로그인 세션 | `CurrentUserSession` | `SessionStore` |
| 현재 시각 | `Clock` | `SystemClock` |
| 댓글 id 생성 | `CommentIdProvider` | `CommentIdGenerator` |

예를 들어 나중에 password hashing 방식이 바뀌어도 Service는 `PasswordHashing`만 계속 사용하면 된다. session 저장 방식이나 clock도 마찬가지다.

### 3.6 Pure Fabrication

도메인 객체에 넣기에는 애매하지만 시스템에는 필요한 책임도 있다. 그런 책임은 별도 객체로 분리했다.

- `PermissionPolicy`: role과 상태 기반 권한 규칙을 모은다.
- `AssignmentRecommendationService`: KNN 기반 배정 후보 추천을 담당한다.
- `DashboardSummaryService`: dashboard용 조회 결과를 구성한다.
- `StatisticsService`: 통계 조회 권한과 기간 범위 검사를 담당한다.
- `ApplicationBootstrap`: 실제 구현체 생성과 연결을 맡는다.

이 객체들은 현실 세계의 개념이라기보다는 설계를 정리하기 위해 만든 소프트웨어 객체다. 그래서 Pure Fabrication에 해당한다고 볼 수 있다.

## 4. MVC 아키텍처 적용

### 4.1 전체 구조

현재 구현은 UI와 업무 로직을 분리하기 위해 MVC 스타일을 따른다. 여기서 View는 JavaFX/Swing 화면이고, Controller는 화면 요청을 Service로 넘기는 경계 역할을 한다.

```text
JavaFX / Swing UI
  -> Controller
  -> Service / Policy
  -> Domain + Repository Port + Service Port
  -> JDBC Adapter / Technical Adapter
  -> Oracle Database
```

각 레이어의 책임은 다음과 같이 정리할 수 있다.

| 계층 | 코드 위치 | 책임 |
|---|---|---|
| View | `ui.javafx`, `ui.swing` | 화면 구성, 사용자 입력 수집, 결과 표시 |
| Controller | `controller` | 현재 로그인 사용자 확인, Service 호출 |
| Service / Policy | `service` | 유스케이스 조율, 권한/정책 검사, 도메인 호출 |
| Domain Model | `domain` | 핵심 업무 상태와 행위 |
| Repository Port | `repository` | Service가 사용하는 저장소 계약 |
| Persistence Adapter | `persistence.jdbc` | SQL 실행, row mapping, transaction 처리 |
| Technical Adapter | `technical` | password, session, clock, id generation 구현 |
| Bootstrap | `config.ApplicationBootstrap` | 구현체 생성과 의존성 연결 |

### 4.2 UI 재사용 구조

`Main`은 기본으로 JavaFX를 실행하고, `--swing` 인자가 있으면 Swing을 실행한다.

```text
Main
  -> JavaFXApp (default)
  -> SwingApp (--swing)
```

두 UI는 화면 객체는 다르지만, 업무 기능은 같은 `ApplicationContext`에서 가져온 Controller를 호출한다. 그래서 아래 계층은 UI와 관계없이 재사용된다.

- Controller
- Service
- Repository interface
- JDBC repository
- Domain
- Technical adapter

이 구조는 과제 요구사항인 "두 개 이상의 UI Toolkit을 사용하되 UI를 제외한 나머지 코드를 거의 수정 없이 재사용"한다는 조건을 만족시키기 위한 것이다.

### 4.3 Controller와 Service를 분리한 이유

Controller에 업무 규칙을 넣으면 JavaFX와 Swing이 같은 규칙을 각각 다시 구현해야 한다. 그래서 Controller는 얇게 두고, 실제 권한/정책/상태 전이는 Service와 Domain 쪽에 둔다.

예를 들어 이슈 상태 변경은 다음 흐름으로 나뉜다.

- UI: 사용자가 상태 변경 버튼을 누르고 comment를 입력한다.
- `IssueStateController`: 현재 로그인 사용자를 확인하고 요청을 넘긴다.
- `IssueStateService`: 이슈 조회, 프로젝트 멤버 확인, 권한 검사, dependency guard를 처리한다.
- `Issue`: 실제 상태 전이와 history 생성을 수행한다.
- Repository/JDBC: 변경된 issue와 history를 저장한다.

이렇게 나누면 화면은 바뀌어도 상태 변경 정책은 한 곳에서 유지된다.

### 4.4 ApplicationBootstrap을 둔 이유

`ApplicationBootstrap`은 실행 시 필요한 객체들을 조립하는 composition root이다. 여기서 JDBC repository, technical adapter, service, controller를 만든다.

이 방식을 사용한 이유는 다음과 같다.

- Service 안에서 `new Jdbc...`, `new PasswordHasher`, `new SessionStore` 같은 구체 구현체 생성을 하지 않게 한다.
- 실행 환경 초기화와 업무 로직을 분리한다.
- JavaFX와 Swing이 같은 `ApplicationContext`를 사용할 수 있다.
- 테스트에서는 fake repository나 fake port를 넣어 Service를 검증하기 쉽다.

즉 Bootstrap은 객체 생성과 연결을 맡고, Service는 받은 객체로 유스케이스를 수행하는 데 집중한다.

## 5. 대표 설계 사례

### 5.1 이슈 상세 조회와 availableActions

이슈 상세 화면에서는 이슈 기본 정보, 댓글, history, dependency, 가능한 액션을 함께 보여준다. `IssueDetailResult`에 `availableActions`를 포함한 이유는 UI가 버튼을 켜고 끌 때 참고할 수 있게 하기 위해서다.

다만 `availableActions`는 화면 표시를 돕는 정보일 뿐이다. 실제 변경 요청이 들어오면 각 Service에서 다시 권한과 정책을 검사한다. 즉 `IssueWorkflowService`는 UI 안내를 돕고, `IssueService`, `AssignmentService`, `IssueStateService`, `DeletedIssueService`가 최종 실행 정책을 보장한다.

### 5.2 Deleted issue workflow 분리

`DELETED` 상태의 이슈는 일반 이슈 조회, 검색, 상태 변경 흐름에서 분리했다. deleted issue는 일반 작업 대상이 아니라 복구나 영구 삭제 대상에 가깝기 때문이다.

현재 흐름은 다음처럼 나뉜다.

- 일반 이슈 작업: `IssueController`, `IssueService`, `IssueStateController`
- deleted issue 관리: `DeletedIssueController`, `DeletedIssueService`, `DeletedIssueRepository`

이렇게 분리하면 "일반 사용자는 deleted 이슈를 볼 수 없다", "restore는 deleted issue 관리 흐름에서만 한다", "단건 물리 삭제는 DELETED 상태만 가능하다" 같은 정책을 더 분명하게 유지할 수 있다.

### 5.3 Dashboard 전용 Repository

Dashboard는 프로젝트 요약, 참여자 수, 상태별 이슈 수처럼 여러 테이블을 묶은 집계 결과가 필요하다. 일반 repository를 여러 번 호출해서 Service에서 조합할 수도 있지만, 그러면 Service가 DB 조회 방식까지 너무 많이 알게 된다.

그래서 `DashboardSummaryRepository`를 두고, dashboard용 read query는 JDBC 구현체가 담당하게 했다. Service는 "admin이면 전체 프로젝트, non-admin이면 참여 프로젝트"라는 정책만 판단한다.

이 선택은 Service의 역할을 줄이고, 집계 SQL과 조회 최적화 책임을 persistence adapter 쪽에 두기 위한 것이다.

### 5.4 기술 관심사의 port 분리

비밀번호 해시, 세션, 현재 시각, 댓글 id 생성은 업무 규칙이 아니라 기술 관심사에 가깝다. Service가 이런 구현체를 직접 만들면 technical 계층에 묶이게 된다.

그래서 다음처럼 service port와 technical adapter를 분리했다.

- `PasswordHashing` / `PasswordHasher`
- `CurrentUserSession` / `SessionStore`
- `Clock` / `SystemClock`
- `CommentIdProvider` / `CommentIdGenerator`

이 구조는 DIP를 적용한 것이다. Service는 추상 계약을 사용하고, technical 구현체가 그 계약을 구현한다.

## 6. 테스트와 문서로 확인하는 설계 경계

현재 코드에는 architecture boundary test가 있다. 이 테스트는 설계 문서에 적은 계층 경계가 코드에서도 지켜지는지 확인한다.

주요 검사 내용은 다음과 같다.

- Domain은 바깥 계층을 import하지 않는다.
- Service는 controller, persistence, ui, config, technical 구현체를 직접 import하지 않는다.
- Controller는 repository, persistence, ui를 import하지 않는다.
- UI는 repository, persistence를 직접 import하지 않는다.
- technical 구현체는 service port를 구현한다.
- JDBC repository factory와 user repository는 `PasswordHashing` port를 사용한다.

이 테스트는 단순히 버그를 잡기 위한 테스트라기보다, 아키텍처 규칙이 코드에서 계속 유지되는지 확인하는 장치에 가깝다.

## 7. 정리

현재 설계의 의도는 다음과 같다.

- 도메인 객체는 자기 상태와 핵심 업무 규칙을 지킨다.
- Service는 하나의 유스케이스 흐름을 조율한다.
- Controller는 UI와 Service 사이의 얇은 경계 역할을 한다.
- Repository interface와 service port는 구현체 변경에 대비한 간접화 지점이다.
- `ApplicationBootstrap`은 구체 객체 조립을 한 곳에 모아 결합도를 낮춘다.
- JavaFX와 Swing은 서로 다른 View이지만 같은 Controller, Service, Domain을 재사용한다.

따라서 이 프로젝트는 도메인 중심으로 객체를 나누고, GRASP로 책임을 배치하고, MVC로 UI와 업무 로직을 분리한 구조라고 정리할 수 있다.
