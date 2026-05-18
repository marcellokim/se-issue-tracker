# ITS Design Class Diagram 설명

## 반영한 산출물

- SSD: actor가 시스템에 보내는 system operation을 Controller operation 후보로 삼았다.
- Operation Contract: postcondition의 객체 생성, association 형성/제거, 속성 변경을 domain class의 attribute와 operation에 반영했다.
- Detailed SD: 실제 상호작용 파트너와 메시지를 DCD의 operation 및 dependency로 반영했다.
- Domain Model: `Project`, `Issue`, `User`, `Comment`, `IssueHistory`, `IssueDependency`와 enum을 DCD의 핵심 domain class로 유지했다.
- Logical Architecture: MVC 계층, Controller, Service/Policy, Repository 경계를 반영했다.

## MVC 적용

- `IssueTrackingView`는 boundary class로 두고, 직접 domain object를 조작하지 않게 했다.
- View의 요청은 `IssueController`, `AssignmentController`, `IssueStateController`, `DeletedIssueController`로 들어간다.
- Controller는 system operation의 첫 non-UI 수신 객체이며, 인증/권한/조회/저장/현재 시각 획득을 조정한다.
- 실제 상태 변경, 이력 생성, 코멘트 생성, dependency 검증은 domain object가 수행한다.
- Repository는 interface로 두어 application layer가 JDBC 같은 영속성 구현에 직접 의존하지 않도록 했다.

## Class Type 표기 원칙

- 일반 `class` 박스는 concrete class로 해석한다.
- `interface` 키워드로 선언한 `ProjectRepository`, `IssueRepository`, `UserRepository`는 interface/contract이다.
- `enum`으로 선언한 `Role`, `IssueStatus`, `Priority`, `ActionType`은 enumeration value type이다.
- 현재 DCD에는 abstract class를 의도적으로 두지 않았다. Controller나 domain entity 사이에 공통 상속 행위를 억지로 만들 근거가 부족하므로, abstract class를 추가하면 오히려 불필요한 상속 결합이 생길 수 있다.
- `<<boundary>>`, `<<control>>`, `<<entity>>`, `<<service>>`, `<<policy>>`, `<<value object>>`는 UML class의 역할 stereotype이며, class/interface 여부를 대체하지 않는다.

## GRASP 적용 근거

### Controller

SSD의 system operation을 use case 성격별 controller에 배치했다.

- `IssueController`: UC1 등록, UC2 코멘트, UC3/UC4 조회, UC7 dependency, UC16 priority 변경
- `AssignmentController`: UC5 배정/재배정/verifier 변경
- `IssueStateController`: UC6 상태 전이
- `DeletedIssueController`: UC9 삭제/복구/보관 한도 정리
- `StatisticsController`: UC10 통계 조회
- `AccountController`: UC12 계정 생성/수정/비활성화, 로그인
- `ProjectController`: UC13 프로젝트 생성/참여자 관리

### Information Expert

`Issue`는 status, priority, reporter/assignee/verifier/fixer/resolver, comment/history/dependency association을 가장 잘 알고 있으므로 대부분의 핵심 규칙을 가진다.

- UC5: `assignFromNew`, `assignReopened`, `reassignAssignee`, `changeVerifier`
- UC6: `markFixed`, `resolve`, `rejectFix`, `close`, `reopen`
- UC7: `validateDependencyCandidate`, `addDependency`, `removeDependency`
- UC9: `softDelete`, `restore`, `findDeleteStatusHistory`
- UC16: `verifyPriorityChange`, `changePriority`

### Creator

- `Project`는 `Issue`를 포함하므로 UC1에서 `registerIssue(...)`를 통해 새 `Issue`를 생성하고 `issues` association에 추가한다.
- `Issue`는 `Comment`, `IssueHistory`, `IssueDependency`를 자신의 aggregate 내부 기록으로 관리하므로 해당 객체의 생성과 연결을 책임진다.

### Low Coupling / High Cohesion

Controller가 domain field를 직접 set하지 않고 의도 중심 메시지를 보낸다. 예를 들어 `status = FIXED`를 직접 설정하지 않고 `Issue.markFixed(...)`를 호출한다. 이 때문에 상태 전이 규칙, 필수 comment, fixer/resolver 기록, history 기록이 `Issue` 주변으로 응집된다.

### Pure Fabrication / Indirection

- `PermissionPolicy`는 UC14 권한 검사를 여러 use case에서 반복하지 않기 위해 분리한 policy class이다.
- `AssignmentRecommendationService`는 UC8 후보 추천 규칙을 `Issue`에 과도하게 넣지 않기 위해 분리했다.
- Repository interface는 저장소 구현 변경이 controller/domain 설계에 직접 전파되지 않도록 하는 indirection이다.

## 주요 설계 결정

- 삭제 전이 시각과 복구 대상 상태는 별도 `Issue` 속성으로 두지 않고, `IssueHistory(STATUS_CHANGED)`에서 계산한다.
- dependency는 `blockingIssue`와 `blockedIssue` association 방향으로 의미를 표현하고, 별도 dependency type/name class는 두지 않았다.
- `IssueDependency`의 시간 속성은 OC-14와 맞추어 `discoveredDate`로 둔다.
- UC5의 네 branch는 모두 `Issue` operation으로 분리했다. `NEW/REOPENED`는 status 변경과 assignment 변경을 함께 수행하고, `ASSIGNED/FIXED`는 status를 유지하며 assignment history만 기록한다.
- UC6 상태 전이는 모두 comment와 history를 동반하므로 `IssueStateController`가 값을 직접 바꾸지 않고 `Issue`에 전이 의도를 전달한다.
- UC1의 기본 priority 결정과 초기 `IssueHistory(CREATED)` 기록은 이슈 생성의 원자적 책임으로 보아 `Project.registerIssue(...)`와 `Issue.createWithDefaultPriority(...)` 주변에 배치했다.
- UC16 priority 변경은 OC-16과 SD-27을 기준으로 `IssueHistory(PRIORITY_CHANGED)`만 생성한다. 요구사항 추적표의 comment 문구는 구현 확인 항목의 표현으로 보고, DCD에서는 현재 OC/SD 계약을 우선했다.
- UC14 권한 검사는 공통 `verifyPermission(user, operation, resource)`와 use case별 `assertCan...` operation을 함께 둔다. 전자는 SSD-22 및 Logical Architecture의 공통 권한 검사를 반영하고, 후자는 controller에서 읽기 쉬운 application policy entry point로 사용한다.
