# ITS 구현 가정사항 및 정책

이 문서는 현재 ITS 구현 과정에서 합의한 도메인, 서비스, JDBC, UI, DB 초기화 정책을 정리한다.
코드가 안정화되기 전까지는 구현 판단의 기준 문서로 사용하고, 정책이 바뀌면 PR에서 함께 갱신한다.

## 1. 레이어 책임

### UI
- UI는 사용자 입력을 받고 화면에 결과를 표시한다.
- UI는 Service, Repository, JDBC를 직접 호출하지 않는다.
- UI는 Controller를 통해서만 기능을 실행한다.
- UI가 Domain 객체를 화면 표시용으로 읽는 것은 허용한다.
- UI가 Domain 객체의 상태 변경 메서드를 직접 호출하는 것은 금지한다.

### Controller
- Controller는 system operation의 진입점이다.
- Controller는 로그인 사용자 확인, 입력 전달, Service 위임을 담당한다.
- 복잡한 비즈니스 흐름, 여러 repository 조회, 권한 검사 흐름은 Service로 넘긴다.

### Service
- Service는 use case 흐름을 조정한다.
- 여러 repository 조회, 권한 검사, 상태별 분기, 저장 순서를 담당한다.
- Service는 Repository interface에 의존하고 JDBC 구현체에 직접 의존하지 않는다.

### Domain
- Domain 객체는 자신의 상태로 판단 가능한 불변식과 상태 변경을 책임진다.
- Domain 객체는 DB, Repository, JDBC, Session, current user를 직접 알지 않는다.
- Domain 필드와 메서드는 합의 없이 임의로 추가하거나 변경하지 않는다.

### Repository/JDBC
- Repository는 영속성 접근의 추상화이다.
- JDBC 구현체는 SQL과 DB 매핑을 담당한다.
- 단일 aggregate 저장은 해당 repository에서 처리한다.
- 여러 테이블 write가 인과적으로 묶여야 하는 경우에는 하나의 JDBC transaction으로 처리한다.

## 2. Domain 생성 정책

- `create(...)`는 새로 생성되어 아직 DB에 저장되지 않은 객체를 만든다.
- `fromPersistence(...)`는 DB에서 읽어 온 값을 Domain 객체로 복원한다.
- 생성자는 `private`으로 두고 factory method를 통해 생성한다.
- `Issue` 신규 등록은 `Issue.create(Issue.persistedState(...))`를 사용한다.
- `Issue` DB 조회는 `Issue.fromPersistence(Issue.persistedState(...))`를 사용한다.

## 3. User 정책

- `loginId`, `passwordHash`, `createdAt`은 변경하지 않는다.
- `name`, `role`, `active`, `updatedAt`은 변경 가능하다.
- 계정은 물리 삭제하지 않고 `active` 값으로 활성/비활성을 표현한다.
- 최초 ADMIN 계정은 항상 존재한다고 가정한다.
- ADMIN만 사용자 생성, 수정, 비활성화, 재활성화를 수행할 수 있다.

## 4. Project 정책

- 프로젝트 생성, 수정, 삭제, 멤버 관리는 ADMIN만 수행한다.
- 프로젝트에는 PL이 정확히 1명만 있어야 한다.
- 비활성 PL도 프로젝트 PL 슬롯을 차지한다.
- PL 교체가 필요하면 ADMIN이 기존 PL 제거, 재활성화, 교체 절차를 수행한다.
- Project는 현재 Issue 목록을 aggregate field로 소유하지 않는다.

## 5. Issue 등록 정책

- 이슈 등록은 IssueService가 담당한다.
- 등록 시 프로젝트 존재, reporter 존재, 권한을 확인한다.
- 같은 `project_id` 안에서는 같은 `title`을 가진 이슈를 새로 등록할 수 없다.
- 제목 중복 검사는 `DELETED` 상태의 이슈까지 포함한다.
- 서로 다른 프로젝트에서는 같은 제목의 이슈를 허용한다.
- 이슈 등록 시 `CREATED` 이력이 함께 저장되어야 한다.

## 6. Issue 수정 및 우선순위 정책

- 이슈 제목/내용 수정은 reporter만 수행할 수 있다.
- 제목/내용 수정은 이슈 상태가 `NEW` 또는 `REOPENED`일 때만 허용한다.
- Priority 변경은 해당 프로젝트의 PL만 수행할 수 있다.
- Priority는 이슈 상태와 무관하게 변경할 수 있다.

## 7. Assignment 및 상태 전이 정책

- NEW 또는 REOPENED 이슈는 PL이 DEV assignee와 TESTER verifier를 배정한다.
- ASSIGNED 이슈의 assignee 재배정은 PL이 수행한다.
- FIXED 이슈의 verifier 변경은 PL이 수행한다.
- ASSIGNED -> FIXED는 현재 assignee DEV만 수행한다.
- FIXED -> RESOLVED는 현재 verifier TESTER만 수행한다.
- FIXED -> ASSIGNED reject는 현재 verifier TESTER만 수행한다.
- RESOLVED -> CLOSED는 해당 프로젝트 PL만 수행한다.
- RESOLVED/CLOSED -> REOPENED는 해당 프로젝트 PL만 수행한다.
- 상태 전이에는 필수 사유 comment가 필요하다.

## 8. Comment 정책

- Comment 추가는 일반 이슈 참여자가 수행할 수 있다.
- Comment 수정은 작성자 본인만 수행할 수 있다.
- Comment 삭제는 작성자 본인만 수행할 수 있다.
- 삭제 가능한 comment는 `GENERAL` purpose만 해당한다.
- `STATUS_CHANGE` purpose comment는 상태 전이 근거이므로 삭제하지 않는다.
- Comment 수정/삭제는 `IssueHistory(COMMENTED)` 이력으로 기록한다.
- UI에서 표시되는 comment는 항상 최신 content 기준이다.

## 9. Issue Dependency 정책

- circular dependency는 금지한다.
- dependency는 blocking issue와 blocked issue의 관계이다.
- blocked issue를 resolve하려면 blocking issue가 먼저 resolved 또는 closed 상태여야 한다.
- blocking issue 상태 검사는 dependency 추가 시점이 아니라 blocked issue를 `FIXED -> RESOLVED`로 전이하는 시점에 수행한다.
- resolve가 성공해도 dependency row를 자동 제거하지 않는다.
- dependency 추가/삭제 권한은 blocked issue가 속한 프로젝트 PL 기준으로 판단한다.
- dependency 변경 이력은 blocked issue의 history에만 기록한다.
- dependency row 저장/삭제, issue updated_at 변경, issue history 기록은 하나의 transaction으로 처리한다.

## 10. DB 및 Seed 정책

- 앱 실행 시 기존 DB 데이터는 보존한다.
- 앱 실행 시 테이블이 없으면 schema를 만들고 기본 seed를 삽입한다.
- 테이블이 이미 있으면 자동으로 seed를 다시 덮어쓰지 않는다.
- 개발/테스트에서 고정 seed가 필요하면 명시적인 reset task를 사용한다.
- schema compatibility 검사는 초기화 단계에서 fail-fast로 처리한다.
- Oracle 통합 테스트는 테스트 스키마에서 수행하며, 실제 데모 DB와 분리한다.

## 11. UI Toolkit 정책

- JavaFX와 Swing 등 2개 이상의 UI Toolkit을 사용할 수 있도록 설계한다.
- UI를 제외한 Controller, Service, Domain, Repository, JDBC 코드는 재사용 가능해야 한다.
- 현재 JavaFX UI 검증은 내부 로직 재사용 가능성을 확인하기 위한 기준 구현으로 사용한다.
