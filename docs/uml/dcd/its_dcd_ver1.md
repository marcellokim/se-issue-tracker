# ITS Design Class Diagram 설명

## 반영한 산출물

- SSD: actor가 시스템에 보내는 system operation을 Controller operation 후보로 삼았다.
- Operation Contract: postcondition의 객체 생성, association 형성/제거, 속성 변경을 domain class의 attribute와 operation에 반영했다.
- Detailed SD: 실제 상호작용 파트너와 메시지를 DCD의 operation 및 dependency로 반영했다.
- Domain Model: `Project`, `Issue`, `User`, `Comment`, `IssueHistory`, `IssueDependency`와 enum을 DCD의 핵심 domain class로 유지했다.
- Logical Architecture: MVC 계층, Controller, Service/Policy, Repository, Technical Services 경계를 반영했다.

## 최종 보고서 사용 위치

| 산출물 | 사용 위치 | 목적 |
|---|---|---|
| DCD ver2 core workflow | 최종 보고서 본문 | 이슈 등록, 배정, 상태 전이, 코멘트/이력 생성으로 이어지는 핵심 workflow를 설명한다. |
| DCD ver1 layered overview | appendix 또는 구현 설명 | JavaFX/Swing, controller, service/policy, repository, technical services, domain class의 계층 경계를 설명한다. |
| Logical Architecture | 설계 개요 또는 구현 설명 | `Presentation -> Application -> Domain -> Persistence/Technical Services` 의존 방향을 설명한다. |

최종 보고서 본문에서는 독자가 핵심 기능 흐름을 빠르게 이해할 수 있도록 `its_dcd_ver2`를 대표 DCD로 사용한다.  
`its_dcd_ver1`은 layered DCD로, 구현자가 UI toolkit, controller, service/policy, repository, domain object의 책임 경계를 확인할 수 있도록 appendix 또는 구현 설명에 사용한다.

## MVC 적용

- JavaFX와 Swing 화면은 Presentation layer의 boundary로 보고, domain object를 직접 조작하지 않는다.
- View의 요청은 `IssueController`, `AssignmentController`, `IssueStateController`, `DeletedIssueController`, `AccountController`, `ProjectController`,
`StatisticsController`로 들어간다.
- Controller는 system operation의 첫 non-UI 수신 객체이며, 현재 사용자 확인과 요청 위임을 담당한다. 권한/조회/저장/현재 시각 획득 조정은 application service 뒤에 둔다.
- 실제 상태 변경, 이력 생성, 코멘트 생성, dependency 검증은 domain object가 수행한다.
- UI toolkit은 교체 가능한 presentation 기술로 취급한다. JavaFX와 Swing은 같은 application/domain/repository 계층을 사용해야 하며, toolkit별 widget이나 화면 component를 domain model이나 DCD의 핵심 class로 올리지 않는다.

## Class Type 표기 원칙

- 일반 `class` 박스는 concrete class로 해석한다.
- `interface` 키워드로 선언한 repository는 interface/contract이다.
- `enum`으로 선언한 `Role`, `IssueStatus`, `Priority`, `ActionType`은 enumeration value type이다.
- `<<read model>>`은 application service가 조회 use case를 위해 조합해 반환하는 DTO 성격의 결과 모델이다. Domain value object와 구분한다.
- 현재 DCD에는 abstract class를 두지 않았다. 강의자료 기준에서 abstract class는 직접 instance가 없어야 하는 superclass가 있고, 하위 class들이 공통 attribute/operation을 상속받는 generalization 구조가 명확할 때 유용하다. 현재 설계에서는 `User`의 역할을 subclass가 아니라 `Role` enum으로 표현하고, Controller도 use case별 책임으로 분리했으므로 공통 abstract superclass를 추가할 근거가 부족하다. 불필요한 abstract class는 오히려 상속 결합을 만들 수 있으므로, 변동 가능성이 큰 persistence 쪽은 abstract class가 아니라 repository interface로 분리한다.
- `<<boundary>>`, `<<control>>`, `<<entity>>`, `<<service>>`, `<<policy>>`, `<<value object>>`는 UML class의 역할 stereotype이며, class/interface 여부를 대체하지 않는다.
- DCD는 Java class diagram의 전체 소스 목록이 아니라 설계 책임을 설명하는 다이어그램이므로 DB table, DTO, UI widget을 과하게 포함하지 않는다.

## GRASP 적용 근거

### Controller

SSD와 Operation Contract의 system operation을 use case 성격별 controller에 배치했다.

- `IssueController`: `registerIssue`, `addComment`, 이슈 검색/조회, `addDependency`, `removeDependency`, `changePriority`
- `AssignmentController`: `assignIssue`, `reassignIssue`, `changeVerifier`
- `IssueStateController`: `changeStatus`
- `DeletedIssueController`: `deleteIssue`, `restoreIssue`, 삭제 보관 한도 정리
- `StatisticsController`: 일/월별 통계와 추이 조회
- `AccountController`: 계정 생성/수정/비활성화, 로그인
- `ProjectController`: 프로젝트 생성/참여자 관리

Controller는 UI toolkit이나 DB 구현체의 세부사항을 직접 알지 않고, application service에 system operation을 위임한다. Service는 policy/domain/repository 협력을 조정한다.

### Information Expert

`Issue`는 status, priority, reporter/assignee/verifier/fixer/resolver, comment/history/dependency association을 가장 잘 알고 있으므로 대부분의 핵심 규칙을 가진다.

- UC5: `assignFromNew`, `assignReopened`, `reassignAssignee`, `changeVerifier`
- UC6: `changeStatus(issueId, targetStatus, comment)` 하나를 system operation으로 두고, target status에 따라 `markFixed`, `resolve`, `rejectFix`, `close`, `reopen` domain operation으로 분기한다.
- UC7: `validateDependencyCandidate`, `addDependency`, `removeDependency`
- UC9: `softDelete`, `restore`, `findDeleteStatusHistory`
- UC16: `verifyPriorityChange`, `changePriority`

### Creator

- `Project`는 `Issue` 목록을 내부 collection으로 소유하지 않는다. UC1에서는 application service가 대상 `Project`를 조회해 권한과 존재 여부를 확인한 뒤, `Issue.create(...)`로 새 `Issue`를 생성하고 `Issue.projectId`로 프로젝트 관계를 기록한다.
- `Issue`는 `Comment`, `IssueHistory`, `IssueDependency`를 자신의 aggregate 내부 기록으로 관리하므로 해당 객체의 생성과 연결을 책임진다.
- ID 생성, repository 저장, transaction 경계는 application/service 또는 technical service 책임이다. `Project`는 프로젝트명/설명/관리자 식별자 같은 자기 상태의 불변식에 집중하고, 프로젝트-이슈 목록 조합은 repository 조회와 application read model에서 처리한다.

### Low Coupling / High Cohesion

Controller가 domain field를 직접 set하지 않고 의도 중심 메시지를 보낸다. 예를 들어 `status = FIXED`를 직접 설정하지 않고 `Issue.markFixed(...)`를 호출한다. 이 때문에 상태 전이 규칙, 필수 comment, fixer/resolver 기록, history 기록이 `Issue` 주변으로 응집된다.

Repository는 interface로 두고 JDBC 구현체는 persistence package에 둔다. 따라서 controller와 domain은 JDBC API, SQL, table 구조에 직접 의존하지 않는다.

### Pure Fabrication / Indirection

- `PermissionPolicy`는 UC14 권한 검사를 여러 use case에서 반복하지 않기 위해 분리한 policy class이다.
- `AssignmentRecommendationService`는 UC8 후보 추천 규칙을 `Issue`에 과도하게 넣지 않기 위해 분리했다.
- `AuthenticationService`는 로그인과 현재 사용자 식별 책임을 domain entity에서 분리한다.
- `Clock`은 현재 시각 획득을 technical service로 분리하여 상태 전이, comment, history 생성 시각을 테스트 가능하게 한다.
- Repository interface는 저장소 구현 변경이 controller/domain 설계에 직접 전파되지 않도록 하는 indirection이다.

### Application Read Model

`ProjectDetail`은 domain value object가 아니라 `ProjectService`가 관리/조회 use case를 위해 repository 결과를 조합해 반환하는 application read model이다.
`Project` aggregate에 participants/issues collection을 넣지 않는 대신, service가 `Project`, `ProjectMember`, `Issue` 조회 결과를 묶어 presentation boundary로 전달한다.

## Repository Pattern 적용 근거

Repository는 application/domain 계층이 영속성 세부 구현에 직접 의존하지 않도록 하는 경계이다.

- Repository interface는 `repository` 계층의 contract로 둔다.
- JDBC 기반 구현체는 `persistence/jdbc` 계층에 둔다.
- Service는 repository interface에 의존하고, SQL과 connection handling은 JDBC 구현체가 담당한다.
- DB table은 persistence 구현 상세이므로 DCD의 핵심 domain class로 표현하지 않는다.

Persistence 테스트 근거는 repository interface와 JDBC 구현체가 분리되어 있다는 점을 확인하는 데 둔다. 예를 들어 repository convention smoke test, JDBC integration test, persistence resource smoke test는 다음을 확인해야 한다.

- repository interface가 application/domain에서 사용할 수 있는 안정적인 contract를 제공하는가
- JDBC implementation이 실제 schema/resource와 연결되는가
- domain object의 상태 전이 결과가 저장/조회 시 보존되는가

## Strategy Pattern 적용 근거

UC8 담당자 추천은 이후 알고리즘 변경 가능성이 높으므로 `AssignmentRecommendationService`로 분리한다.

- Controller는 추천 후보가 필요할 때 `AssignmentRecommendationService`에 요청한다.
- 추천 service는 해결/종료 이력 기반 후보 산출 규칙을 캡슐화한다.
- 추천 이력 조회는 repository를 통해 수행한다.
- 향후 단순 빈도 기반, 최근 해결 이력 기반, priority/role 가중치 기반 알고리즘으로 바뀌어도 controller와 domain object의 변경을 줄일 수 있다.

따라서 추천 알고리즘은 domain entity의 필수 책임이 아니라 교체 가능한 application policy/strategy로 취급한다.

## 주요 설계 결정

- 삭제 전이 시각과 복구 대상 상태는 별도 `Issue` 속성으로 두지 않고, `IssueHistory(STATUS_CHANGED)`에서 계산한다.
- dependency는 `blockingIssue`와 `blockedIssue` association 방향으로 의미를 표현하고, 별도 dependency type/name class는 두지 않았다.
- `IssueDependency`의 시간 속성은 OC-14와 맞추어 `discoveredDate`로 둔다.
- UC5의 네 branch는 모두 `Issue` operation으로 분리했다. `NEW/REOPENED`는 status 변경과 assignment 변경을 함께 수행하고, `ASSIGNED/FIXED`는 status를 유지하며 assignment history만 기록한다.
- UC6 상태 전이는 모두 comment와 history를 동반하므로 `IssueStateController`가 값을 직접 바꾸지 않고 `Issue`에 전이 의도를 전달한다.
- UC1의 기본 priority 결정과 초기 `IssueHistory(CREATED)` 기록은 이슈 생성의 원자적 책임으로 보아 `Issue.create(...)` 주변에 배치했다. Project는 issue collection을 직접 갱신하지 않으며, 새 이슈는 `Issue.projectId`와 repository 저장으로 프로젝트에 연결된다.
- UC16 priority 변경은 OC-16과 SD-27을 기준으로 `IssueHistory(PRIORITY_CHANGED)`만 생성한다. 요구사항 추적표의 comment 문구는 구현 확인 항목의 표현으로 보고, DCD에서는 현재 OC/SD 계약을 우선했다.
- UC14 권한 검사는 공통 `verifyPermission(user, operation, resource)`와 use case별 `assertCan...` operation을 함께 둔다. 전자는 SSD-22 및 Logical Architecture의 공통 권한 검사를 반영하고, 후자는 controller에서 읽기 쉬운 application policy entry point로 사용한다.
- 상세 SD와 DCD의 operation 이름이 충돌하면 SSD/OC/SD의 system operation 이름을 우선한다.

## 구현 이슈별 클래스 책임 연결

| 구현 이슈 | Controller | Service/Policy/Technical Service | Domain 책임 | Repository/Persistence 책임 |
|---|---|---|---|---|
| #16 계정, 역할, 프로젝트 기본 모델 구현 | `AccountController`, `ProjectController` | `AccountService`, `ProjectService`, `AuthenticationService`, `PermissionPolicy` | `User`, `Role`, `Project` | `UserRepository`, `ProjectRepository`, JDBC 구현체 |
| #17 이슈, 댓글, 우선순위, 상태 전이 모델 구현 | `IssueController`, `IssueStateController` | `IssueService`, `IssueStateService`, `PermissionPolicy`, `Clock` | `Issue`, `Comment`, `IssueHistory`, `IssueStatus`, `Priority`, `ActionType` | `IssueRepository`, `CommentRepository`, `IssueHistoryRepository` |
| #18 DB 기반 영속 저장소와 데모 초기 데이터 준비 | CLI/composition root가 repository demo 진입점 제공 | `RepositoryDemoSummaryService`, technical service/resource setup | domain object는 persistence 구현을 알지 않음 | repository interface와 JDBC implementation 분리, schema/seed data 준비 |
| #19 이슈 등록, 검색, 상세 조회, 코멘트 서비스 구현 | `IssueController` | `IssueService`, `PermissionPolicy`, `Clock` | `Issue.create`, `Issue.addComment` | `ProjectRepository`, `IssueRepository`, `CommentRepository`, `IssueHistoryRepository` |
| #20 이슈 배정과 상태 변경 흐름 구현 | `AssignmentController`, `IssueStateController` | `AssignmentService`, `IssueStateService`, `PermissionPolicy`, `AssignmentRecommendationService`, `Clock` | `Issue.assignFromNew`, `Issue.assignReopened`, `Issue.reassignAssignee`, `Issue.changeVerifier`, `Issue.markFixed`, `Issue.resolve`, `Issue.close`, `Issue.reopen` | `IssueRepository`, `IssueHistoryRepository`, `CommentRepository`, recommendation 조회용 repository |
| #21 일/월별 이슈 통계와 추이 조회 구현 | `StatisticsController` | `StatisticsService`, `PermissionPolicy` | `IssueStatus`, `Priority` 등 집계 기준 | `StatisticsRepository` 구현체가 기간별 count/trend 조회 |
| #22 해결 이력 기반 담당자 추천 기능 구현 | `AssignmentController` | `AssignmentRecommendationService` | `IssueHistory`, `IssueStatus`, `Role`은 추천 근거 데이터 제공 | `AssignmentRecommendationRepository` 또는 이력 조회 repository |
| #23 JavaFX 메인 UI로 기본 사용자 흐름 구현 | JavaFX boundary가 controller 호출 | 기존 application service 재사용 | domain 직접 조작 금지 | repository 직접 접근 금지 |
| #24 Swing 보조 UI로 모델 재사용 구조 입증 | Swing boundary가 controller 호출 | JavaFX와 동일한 application service 재사용 | domain 직접 조작 금지 | repository 직접 접근 금지 |
| #25 모델, 서비스, 영속 저장소 JUnit 테스트 구성 | controller/service 테스트 대상 | policy, service, clock test double 활용 | domain 상태 전이 단위 테스트 | repository convention, JDBC integration, persistence resource smoke test |
| #26 최종 프로젝트 문서, 발표 자료, 영상, 제출 패키지 준비 | DCD controller 책임 설명 | service/policy/repository pattern 설명 | domain model과 DCD의 책임 연결 설명 | persistence test와 repository 분리 근거 설명 |
| #43 Tester 검증 실패 시 fixed 이슈를 assigned로 되돌리는 역전이 구현 | `IssueStateController` | `IssueStateService`, `PermissionPolicy`, `Clock` | `Issue.rejectFix`, `Comment`, `IssueHistory` | `IssueRepository`, `CommentRepository`, `IssueHistoryRepository` |
| #44 deleted 상태와 삭제 후보 보관/FIFO 정리 정책 구현 | `DeletedIssueController` | `DeletedIssueService`, `PermissionPolicy`, `Clock` | `Issue.softDelete`, `Issue.restore`, `Issue.findDeleteStatusHistory`, `IssueDependency` 제거 | `IssueRepository`, `IssueDependencyRepository`, `IssueHistoryRepository` |
| #45 이슈 dependency 관계와 선행 이슈 해결 제약 구현 | `IssueController`, `IssueStateController` | `IssueService`, `IssueStateService`, `PermissionPolicy` | `IssueDependency`, `Issue.validateDependencyCandidate`, `Issue.addDependency`, `Issue.removeDependency`, `Issue.resolve` guard | `IssueDependencyRepository`, `IssueRepository`, `IssueHistoryRepository` |
| #46 assigned 전까지만 reporter의 title/description 수정 허용 | `IssueController` | `IssueService`, `PermissionPolicy`, `Clock` | `Issue`가 reporter와 status 기준으로 수정 가능 여부 검증 | `IssueRepository`, 필요 시 `IssueHistoryRepository` |
| #47 PL 전용 reopen 및 재배정 시작 흐름 구현 | `IssueStateController`, `AssignmentController` | `IssueStateService`, `AssignmentService`, `PermissionPolicy`, `AssignmentRecommendationService`, `Clock` | `Issue.reopen`, `Issue.assignReopened` | `IssueRepository`, `CommentRepository`, `IssueHistoryRepository` |

## Operation 이름 정합성

DCD 설명과 구현 이슈 연결에서 사용하는 system operation 이름은 Operation Contract와 SSD/SD 이름을 우선한다.

- `registerIssue(title, description, priority)`
- `addComment(issueId, content)`
- `assignIssue(issueId, assigneeId, verifierId)`
- `reassignIssue(issueId, assigneeId)`
- `changeVerifier(issueId, verifierId)`
- `changeStatus(issueId, targetStatus=FIXED, comment)`
- `changeStatus(issueId, targetStatus=RESOLVED, comment)`
- `changeStatus(issueId, targetStatus=CLOSED, comment)`
- `changeStatus(issueId, targetStatus=REOPENED, comment)`
- `changeStatus(issueId, targetStatus=ASSIGNED, comment)`
- `deleteIssue(issueId)`
- `restoreIssue(issueId)`
- `addDependency(blockingIssueId, blockedIssueId)`
- `removeDependency(dependencyId)`
- `changePriority(issueId, newPriority)`

따라서 #43의 Tester reject fix는 별도 새 operation 이름을 만들지 않고 `changeStatus(issueId, targetStatus=ASSIGNED, comment)`로 연결한다.
