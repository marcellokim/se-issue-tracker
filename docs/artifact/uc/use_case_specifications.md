# 유스케이스 명세 (Use Case Specifications)

## 작성 범위

본 문서는 Issue Tracker System(ITS)의 전체 유스케이스 중 핵심 이슈 생명주기 흐름인 UC1-UC6을 Fully Dressed 형식으로 상세 명세한다.
UC7-UC16은 전체 시스템 기능 범위를 보이기 위해 Brief 수준으로 정리하며, 세부 system operation 흐름은 SSD, Operation Contract, Controller API 명세에서 보완한다.
UC8 Recommend Assignment Candidates와 UC14 Verify Permission은 독립적인 최종 사용자 목표라기보다 다른 유스케이스에서 include되는 보조 유스케이스이다. UC15 Edit Issue는 UC4 View Issue Detail에서 확장되는 유스케이스로 다룬다.
Admin은 UC11, UC12, UC13을 수행하는 별도 actor이며, PL/Dev/Tester가 수행하는 이슈 업무 유스케이스와 분리한다.

---

## 핵심 유스케이스 선정 근거

ITS의 핵심 목적은 이슈를 등록하고, 사용자와 이슈 사이의 상호작용을 기록하며, 이슈의 배정과 상태 변화를 관리하는 것이다.
이에 따라 UC1-UC6을 Fully Dressed 대상으로 선정하였다.

- **UC1 Register Issue**: 이슈 생명주기의 시작점
- **UC2 Add Comment**: 사용자와 이슈 간 상호작용 기록
- **UC3 Search Issues**: 사용자가 필요한 이슈를 찾는 진입점
- **UC4 View Issue Detail**: 이슈 정보와 코멘트 history 확인
- **UC5 Assign / Update Issue Assignment**: PL의 이슈 배정 및 배정 변경 관리
- **UC6 Change Issue State**: Dev, Tester, PL의 상태 전이 관리

UC7-UC16은 전체 시스템 기능에는 포함되지만, 핵심 이슈 생명주기를 보조하거나 관리하는 기능이므로 전체 유스케이스 커버리지 매트릭스에서 기능 범위를 보인다. 이 중 UC9는 삭제/복구 회의 결정을 반영하기 위해 별도 Fully-Dressed 명세를 추가한다.

---

## Actor 정의

표준 역할명은 `Admin`, `PL`, `Dev`, `Tester`이다. `Auth User = Tester | Dev | PL`인 abstract actor이며, `Admin`은 `Auth User`와 별도 계층이다.
`Admin`은 본 Fully Dressed 6개 UC(UC1-UC6)의 Primary Actor가 아니다.

| Actor | Meaning |
|---|---|
| Admin | 계정과 프로젝트를 관리하는 독립 액터. Auth User와 별도의 계층이다 |
| Auth User | 로그인한 이슈 관련 사용자에 대한 abstract actor. concrete actor는 Tester, Dev, PL이다 |
| Tester | Auth User의 specialization. 이슈 등록 및 검증 수행 |
| Dev | Auth User의 specialization. 배정된 이슈 수정 수행 |
| PL | Auth User의 specialization. 이슈 배정, 종료, 재개, 우선순위 변경 수행 |

### Actor <-> Usecase 매핑

| Actor | Participating Use Cases |
|---|---|
| Admin | UC11 Log In, UC12 Manage Accounts, UC13 Manage Projects |
| Auth User | 추상 actor. 실제 참여는 Tester, Dev, PL 행으로 구체화한다 |
| Tester | UC1 Register Issue, UC3 Search Issues, UC4 View Issue Detail, UC6 Change Issue State, UC10 Statistics, UC11 Log In. 또한 UC2 Add Comment와 UC15 Edit Issue는 관련 UC의 extend/include 흐름으로 참여한다 |
| Dev | UC1 Register Issue, UC3 Search Issues, UC4 View Issue Detail, UC6 Change Issue State, UC10 Statistics, UC11 Log In. 또한 UC2 Add Comment와 UC15 Edit Issue는 관련 UC의 extend/include 흐름으로 참여한다 |
| PL | UC1 Register Issue, UC3 Search Issues, UC4 View Issue Detail, UC5 Assign / Update Issue Assignment, UC6 Change Issue State, UC7 Manage Dependency, UC9 Manage Deleted Issue, UC10 Statistics, UC11 Log In, UC16 Change Priority. 또한 UC2 Add Comment와 UC15 Edit Issue는 관련 UC의 extend/include 흐름으로 참여한다 |

> UC2 Add Comment는 UC1, UC4, UC5의 선택적 확장 흐름이거나 UC6, UC9의 필수 포함 흐름으로 수행된다. UC15 Edit Issue는 사용자가 자신이 reporter인 NEW/REOPENED 이슈를 UC4 상세 화면에서 수정하려는 경우에만 확장 흐름으로 수행된다.
`UC14 Verify Permission`은 사용자 목표가 아니라 여러 UC에서 include되는 권한 검사 subfunction이다.
`UC8 Recommend Assignment Candidates`는 PL이 단독으로 실행하는 사용자 목표가 아니라 `UC5 Assign / Update Issue Assignment`에서 include되는 보조 유스케이스이다.
`UC15 Edit Issue`는 `UC4 View Issue Detail`에서 reporter-only, NEW/REOPENED 상태 조건으로 진입하는 extension use case이며, UCD에서는 Auth User와 직접 연결하지 않고 UC4의 extend 관계로 표현한다.

---

## include/extend 관계 요약

- UC1 Register Issue는 UC2 Add Comment에 의해 **extend**된다. 따라서 이슈 등록 코멘트는 선택 사항이다.
- UC4 View Issue Detail은 UC2 Add Comment에 의해 **extend**된다. 따라서 상세 조회 중 코멘트 추가는 선택 사항이다.
- UC5 Assign / Update Issue Assignment는 UC8 Recommend Assignment Candidates를 **include**한다. 배정/배정 변경 시작 시 대상 Issue status에 맞는 후보는 항상 계산되지만, PL이 추천 후보를 반드시 선택해야 하는 것은 아니다.
- UC5 Assign / Update Issue Assignment는 UC2 Add Comment에 의해 **extend**된다. 따라서 배정 코멘트는 선택 사항이다.
- UC6 Change Issue State는 UC2 Add Comment를 **include**한다. 따라서 상태 변경 사유 코멘트는 필수이다.
- UC14 Verify Permission은 UC1, UC5, UC6, UC7, UC9, UC10, UC12, UC13, UC16에서 **include**된다. 또한 다이어그램에 선으로 표시하지 않은 UC2, UC3, UC4, UC15 등 보호된 기능에서도 로그인, 프로젝트 소속, 역할 검사가 내부적으로 수행된다.
- UC9 Manage Deleted Issue는 UC14 Verify Permission을 **include**한다. 따라서 삭제 이슈 조회, soft delete, restore, 단건 영구 삭제 수행 전에 PL 권한과 대상 상태를 확인한다.
- UC9 Manage Deleted Issue는 UC2 Add Comment를 **include**한다. soft delete와 restore는 삭제/복구 사유 코멘트가 필수이다. 단, 단건 영구 삭제와 FIFO 보관 한도 정리는 사유 코멘트를 요구하지 않는다.

---

## Fully Dressed로 명세되는 핵심 유스케이스 6가지

- **UC1. Register Issue (이슈 등록)**
- **UC2. Add Comment (코멘트 추가)**
- **UC3. Search Issues (이슈 검색 및 브라우즈)**
- **UC4. View Issue Detail (이슈 상세 조회)**
- **UC5. Assign / Update Issue Assignment (이슈 배정 및 배정 변경)**
- **UC6. Change Issue State (이슈 상태 변경)**

---

## UC1. Register Issue (이슈 등록)

| 항목 | 내용 |
|---|---|
| ID | UC1 |
| Name | Register Issue |
| Scope | Issue Tracking System |
| Level | User goal |
| Primary Actor | Auth User (Tester | Dev | PL) |
| Stakeholders & Interests | - 이슈 발견자: 발견한 이슈를 시스템에 누락 없이 기록하고 싶다<br>- PL: 등록된 이슈를 추적할 수 있기를 원한다 |
| Preconditions | - 사용자가 시스템에 로그인한 상태이다<br>- 대상 프로젝트가 시스템에 존재한다<br>- 사용자가 대상 프로젝트의 active member이다 |
| Postconditions | - 시스템이 새 이슈를 저장한다<br>- 시스템이 reporter를 현재 사용자로, reportedDate를 현재 시각으로, status를 NEW로 설정한다<br>- 사용자가 priority를 선택하지 않았다면 시스템이 MAJOR를 기본값으로 부여한다<br>- 시스템이 이슈 생성 내역을 기록한다 |
| Trigger | 사용자가 메뉴에서 "Register Issue"를 선택한다 |

> 액터 범위: 데모와 SSD 예시는 `Tester`가 이슈를 등록하는 대표 시나리오를 사용하지만, 명세상 UC1은 `Auth User (Tester | Dev | PL)`가 수행할 수 있는 사용자 목표로 둔다.

### Main Flow
1. 사용자가 이슈 등록 화면을 연다
2. 시스템이 이슈 등록 폼을 보여준다
3. 사용자가 Title, Description, Priority를 입력 및 선택한다
4. 사용자가 저장 및 등록 버튼을 누른다
5. 시스템이 사용자가 이슈를 등록할 수 있는지 확인한다 (include UC14/권한 검사)
6. 시스템이 입력값과 프로젝트 내 이슈 제목 중복 여부를 확인한다
7. 시스템이 이슈를 저장한다
8. 시스템이 등록 완료 화면을 보여준다
extension point: UC2(Add Comment)

### Alternative Flows
- 5a. 사용자가 이슈 등록 권한을 가지지 않는다
    - 5a1. 시스템이 권한 거부 메시지를 보여준다
    - 5a2. 흐름이 종료된다   
- 6a. Title 또는 Description이 비어있다
    - 6a1. 시스템이 누락 항목을 표시한다
    - 6a2. 사용자가 다시 입력한다 (단계 3으로 복귀)
- 6b. 같은 프로젝트에 동일한 제목의 이슈가 이미 존재한다
    - 6b1. 시스템이 중복 제목 메시지를 보여준다
    - 6b2. 사용자가 다시 입력한다 (단계 3으로 복귀)    
- *. 사용자가 등록을 취소한다
    - *1. 시스템이 입력 내용을 폐기하고 이전 화면으로 돌아간다

---

## UC2. Add Comment (코멘트 추가)

| 항목 | 내용 |
|---|---|
| ID | UC2 |
| Name | Add Comment |
| Scope | Issue Tracking System |
| Level | Subfunction |
| Primary Actor | Auth User (Tester | Dev | PL) |
| Stakeholders & Interests | - 사용자: 이슈에 자신의 의견과 진행 상황 등을 기록하고 싶다<br>- 조직: 이슈 배정 이후 해당 이슈와 관련된 사용자들의 이슈에 대한 의견을 확인하고 싶다 |
| Preconditions | - 호출 UC가 진행 중이고, 대상 이슈가 화면에 있다<br>- 사용자가 시스템에 로그인한 상태이다<br>- 사용자가 대상 이슈가 속한 프로젝트의 active member이다<br>- 대상 이슈가 DELETED 상태가 아니다 |
| Postconditions | - 시스템이 새 코멘트를 대상 이슈에 추가한다<br>- 시스템이 코멘트 작성자를 현재 사용자로, 작성 시간을 현재 시각으로 기록한다<br>- 필요한 경우 시스템이 코멘트 추가 또는 호출 UC의 처리 이력을 함께 기록한다 |
| Trigger | - UC1(이슈 등록), UC4(이슈 상세 조회), UC5(이슈 배정 및 배정 변경)의 extension point에서 사용자가 코멘트 추가를 선택 (extend)<br>- UC6(이슈 상태 변경)의 상태 변경 사유 입력 단계와 UC9(삭제/복구 관리)의 삭제/복구 사유 입력 단계에서 자동 시작 (include) |

### Main Flow
1. 시스템이 코멘트 입력 영역을 보여준다
2. 사용자가 코멘트를 입력한다
3. 시스템이 사용자가 코멘트를 추가할 수 있는지 확인한다
4. 시스템이 코멘트 본문이 비어있지 않은지 확인한다
5. 시스템이 코멘트를 저장하고 필요한 처리 이력을 기록한다
6. 시스템이 호출 UC로 돌아간다

### Alternative Flows
- 3a. 사용자가 코멘트 추가 권한을 가지지 않는다
    - 3a1. 시스템이 권한 거부 메시지를 보여준다
    - 3a2. 흐름이 종료되고 호출 UC로 돌아간다
- 4a. 코멘트 내용이 비어있다
    - 4a1. UC1, UC4, UC5에서 호출된 경우 (extend)
        - 시스템이 코멘트 입력을 취소한 것으로 간주하여 호출 UC로 복귀한다
    - 4a2. UC6 또는 UC9에서 호출된 경우 (include)
        - 시스템이 코멘트 내용을 입력하라는 메시지를 보여준다
        - 단계 2로 되돌아간다
        - 사용자가 코멘트 내용을 입력할 때까지 호출 UC가 다음 단계로 진행하지 않는다

---

## UC3. Search Issues (이슈 검색 및 브라우즈)

| 항목 | 내용 |
|---|---|
| ID | UC3 |
| Name | Search Issues |
| Scope | Issue Tracking System |
| Level | User goal |
| Primary Actor | Auth User (Tester | Dev | PL) |
| Stakeholders & Interests | - Tester: 자신이 검증하여 fixed->resolved할 이슈를 찾고 싶다<br>- PL: 프로젝트 이슈들을 종류와 상태 기준으로 찾고 싶다<br>- Dev: 자신에게 지정된 이슈를 찾고 싶다 |
| Preconditions | - 사용자가 시스템에 로그인한 상태이다<br>- 대상 프로젝트가 시스템에 존재한다<br>- 사용자가 대상 프로젝트의 active member이다 |
| Postconditions | - 시스템이 검색 조건에 맞는 삭제되지 않은 이슈 목록을 보여준다 |
| Trigger | 사용자가 "Search Issues" 메뉴를 선택한다 |

### Main Flow
1. 사용자가 프로젝트 화면에서 이슈 검색 또는 브라우즈 영역을 연다
2. 시스템이 해당 프로젝트의 이슈 검색 조건 입력 영역을 보여준다
3. 사용자가 keyword, reporter, assignee, verifier, status, priority, reportedDate 범위 등의 검색 조건을 입력한다
4. 사용자가 검색 버튼을 누른다
5. 시스템이 조건에 맞는 이슈를 찾는다
6. 시스템이 결과 목록을 reportedDate 내림차순으로 보여준다

### Alternative Flows
- 3a. 사용자가 조건 없이 검색한다
    - 3a1. 시스템이 해당 프로젝트의 삭제되지 않은 모든 이슈 목록을 보여준다
- 5a. 조건에 맞는 결과가 없다
    - 5a1. 시스템이 조건에 맞는 결과가 없다는 메시지를 보여준다
- 6a. 사용자가 결과 목록에서 이슈 한 건을 선택한다
    - 6a1. 흐름이 종료되고 UC4가 시작된다

---

## UC4. View Issue Detail (이슈 상세 조회)

| 항목 | 내용 |
|---|---|
| ID | UC4 |
| Name | View Issue Detail (이슈 상세 조회) |
| Scope | Issue Tracking System |
| Level | User goal |
| Primary Actor | Auth User (Tester | Dev | PL) |
| Stakeholders & Interests | - 사용자: 특정 이슈에 대한 모든 정보를 확인하고 싶다 |
| Preconditions | - 사용자가 시스템에 로그인한 상태이다<br>- 사용자가 대상 이슈가 속한 프로젝트의 active member이다 |
| Postconditions | 시스템이 이슈의 기본 정보, 코멘트, 이슈 이력, 현재 이슈를 막는 의존성, 현재 이슈가 막고 있는 의존성, 사용자가 수행 가능한 액션을 화면에 보여준다 |
| Trigger | 사용자가 이슈 목록 또는 검색 결과에서 특정 이슈를 선택한다 |

### Main Flow
1. 사용자가 이슈를 선택한다
2. 시스템이 이슈를 불러온다
3. 시스템이 이슈의 기본 정보와 담당자 정보를 보여준다
4. 시스템이 코멘트, 이슈 이력, 현재 이슈를 막는 의존성과 현재 이슈가 막고 있는 의존성 정보를 보여준다
5. 시스템이 사용자의 역할과 이슈 상태에 따라 수행 가능한 액션을 보여준다
6. 사용자가 화면을 확인한다
extension points: UC2(Add Comment), UC15(Edit Issue)

### Alternative Flows
- 2a. 이슈가 존재하지 않는다
    - 2a1. 시스템이 이슈를 찾을 수 없다는 메시지를 보여준다
    - 2a2. 사용자가 이전 화면으로 돌아간다
- 2b. 대상 이슈가 삭제된 상태이다
    - 2b1. 시스템이 일반 상세 조회로는 삭제 이슈를 볼 수 없음을 알린다
    - 2b2. 흐름이 종료된다    

> 참고: 이슈 상세 화면은 이슈 배정, 상태 변경, 코멘트 추가, 이슈 수정 기능으로 이동할 수 있는 진입점 역할을 한다.

---

## UC5. Assign / Update Issue Assignment (이슈 배정 및 배정 변경)

| 항목 | 내용 |
|---|---|
| ID | UC5 |
| Name | Assign / Update Issue Assignment |
| Scope | Issue Tracking System |
| Level | User goal |
| Primary Actor | PL |
| Stakeholders & Interests | - PL: 이슈 상태에 맞게 적절한 Dev 또는 Tester에게 담당을 배정하거나 변경하고 싶다<br>- Dev: 자신에게 맞는 이슈를 배정 받고 싶다<br>- Tester: 자신이 검증할 이슈를 명확히 배정 받고 싶다 |
| Preconditions | - PL이 시스템에 로그인한 상태이다<br>- PL이 대상 이슈가 속한 프로젝트의 active PL이다<br>- 대상 이슈가 시스템에 존재한다<br>- 대상 이슈의 상태가 NEW, REOPENED, ASSIGNED 또는 FIXED이다 |
| Postconditions | - NEW: 시스템이 이슈의 assignee를 PL이 선택한 Dev로, verifier를 PL이 선택한 Tester로 설정하고 status를 ASSIGNED로 변경한다<br>- REOPENED: 시스템이 assignee와 verifier를 PL이 선택한 값으로 재설정하고 status를 ASSIGNED로 변경한다. 기존 fixer/resolver 기록은 보존한다<br>- ASSIGNED: 시스템이 assignee를 PL이 선택한 Dev로 변경하고 status를 ASSIGNED로 유지한다<br>- FIXED: 시스템이 verifier를 PL이 선택한 Tester로 변경하고 status를 FIXED로 유지한다<br>- 배정 또는 배정 변경이 완료되면 시스템이 배정 변경 이력을 기록한다<br>- NEW -> ASSIGNED 또는 REOPENED -> ASSIGNED처럼 상태가 함께 변경되는 경우에는 상태 변경 이력도 함께 기록한다 |
| Trigger | PL이 이슈 상세 화면에서 이슈 배정 또는 배정 변경 액션을 선택한다 |

### Main Flow
1. PL이 이슈 배정 또는 배정 변경 버튼을 누른다
2. 시스템이 권한과 전이 규칙을 확인한다 (include UC14/권한 검사)
3. 시스템이 UC8 Recommend Assignment Candidates를 include하여 이슈 상태에 맞는 Dev 또는 Tester 후보를 보여준다
    - NEW/REOPENED: Dev assignee 후보와 Tester verifier 후보를 보여준다
    - ASSIGNED: Dev assignee 후보를 보여준다
    - FIXED: Tester verifier 후보를 보여준다
4. PL이 상태별로 필요한 assignee 또는 verifier를 선택한다
5. 시스템이 이슈 상태에 맞게 assignee, verifier, status를 갱신한다
6. 시스템이 배정 또는 배정 변경 이력을 기록한다
7. 시스템이 갱신된 이슈 상세 화면을 보여준다
extension point: UC2(Add Comment)

### Alternative Flows
- 2a. PL이 배정 권한을 가지지 않는다
    - 2a1. 시스템이 권한 거부 메시지를 보여준다
    - 2a2. 흐름이 종료된다
- 2b. 이슈 상태가 NEW, REOPENED, ASSIGNED 또는 FIXED가 아니어서 배정/변경이 불가능하다
    - 2b1. 시스템이 현재 상태에서는 배정할 수 없다는 메시지를 보여준다
    - 2b2. PL이 이전 화면으로 돌아간다
- 3a. 필요한 역할에 대한 후보가 충분하지 않다
    - 3a1. 시스템이 선택 가능한 후보가 없거나 부족함을 보여준다
    - 3a2. PL이 배정을 취소하거나 다른 조치를 선택한다
- *. PL이 배정을 취소한다
    - *1. 시스템이 입력 내용을 폐기하고 이슈 상세 화면으로 돌아간다

---

## UC6. Change Issue State (이슈 상태 변경)

| 항목 | 내용 |
|---|---|
| ID | UC6 |
| Name | Change Issue State (이슈 상태 변경) |
| Scope | Issue Tracking System |
| Level | User goal |
| Primary Actor | Dev, Tester, PL (역할에 따라 수행 가능한 전이가 다름) |
| Stakeholders & Interests | - Dev: 자신이 해결한 이슈의 진행 상태를 시스템에 반영하고 싶다<br>- Tester: 자신이 검증을 담당하는 이슈의 수정 결과를 확인하고 검증 결과를 반영하고 싶다<br>- PL: 검증 완료된 이슈를 종료하거나, 미해결 이슈를 재개하고 싶다 |
| Preconditions | - 사용자가 시스템에 로그인한 상태이다<br>- 대상 이슈가 시스템에 존재한다<br>- 대상 이슈는 DELETED 상태가 아니다<br>- 사용자가 대상 이슈가 속한 프로젝트에서 해당 전이를 수행할 수 있는 active member이다<br>- 이슈의 현재 상태에서 사용자의 역할로 수행 가능한 전이가 존재한다 |
| Postconditions | - 시스템이 이슈의 status를 목표 상태로 변경한다<br>- 시스템이 전이에 따른 자동 필드를 갱신한다<br>- 시스템이 상태 변경 사유 코멘트를 코멘트 history에 추가한다<br>- 시스템이 상태 변경 이력을 기록한다 |
| Trigger | 사용자가 이슈 상세 화면에서 상태 변경 액션을 선택한다 |

### 역할별 허용되는 상태 전이

| Current Status | Target Status | Actor / Role | Auto Field | Description |
|---|---|---|---|---|
| ASSIGNED | FIXED | Dev (assignee 본인) | fixer = 현재 Dev | Dev가 이슈 수정을 완료했음을 표시 |
| FIXED | RESOLVED | Tester (현재 verifier) | resolver = 현재 Tester, assignee/verifier 제거, fixer 보존 | verifier가 수정 결과를 검증한다. 단, blocking issue가 모두 RESOLVED 또는 CLOSED일 때만 허용된다 |
| FIXED | ASSIGNED | Tester (현재 verifier) | 기존 assignee, verifier, fixer 유지 | verifier가 수정이 불충분하다고 판단하여 상태를 되돌림 |
| RESOLVED | CLOSED | PL | assignee/verifier 제거, fixer/resolver 보존 | PL이 검증 완료된 이슈를 active assignment 없이 종료시킨다 |
| RESOLVED / CLOSED | REOPENED | PL | fixer/resolver 보존 | PL이 종료된 이슈를 재개. assignee/verifier는 자동 복원하지 않고, PL은 필요 시 UC5를 통해 배정한다 |

설계 메모: fixer와 resolver는 각각 ASSIGNED -> FIXED, FIXED -> RESOLVED 상태 전이에서 설정되며, 각 상태 전이는 상태 변경 이력으로 기록된다.

### Main Flow
1. 사용자가 이슈 상세 화면에서 상태 변경 액션을 선택한다
2. 시스템이 사용자의 역할과 이슈의 현재 상태를 기준으로 수행 가능한 상태 전이를 확인한다 (include UC14/권한 검사)
3. 시스템이 수행 가능한 목표 상태와 상태 변경 사유 입력 영역을 보여준다
4. 사용자가 목표 상태를 선택하고 상태 변경 사유를 입력한다
5. 시스템이 상태 변경 사유가 입력되었는지 확인한다 (include UC2/코멘트 추가)
6. 시스템이 선택된 목표 상태가 현재 이슈 상태와 사용자 역할에서 허용되는 전이인지 최종 확인한다
    - FIXED -> RESOLVED 전이인 경우, 관련 blocking issue가 모두 RESOLVED 또는 CLOSED인지 확인한다
7. 시스템이 이슈의 status를 목표 상태로 변경하고, 전이에 따른 자동 필드를 갱신한다
8. 시스템이 상태 변경 사유 코멘트를 추가하고 상태 변경 이력을 기록한다
9. 시스템이 갱신된 이슈 상세 화면을 보여준다

### 상태 전이 처리 노트
- ASSIGNED -> FIXED: fixer를 현재 Dev로 기록한다.
- FIXED -> ASSIGNED: 기존 assignee, verifier, fixer를 유지한다. 이후 다시 FIXED 전이가 발생하면 fixer는 그 시점의 Dev로 갱신될 수 있다.
- FIXED -> RESOLVED: blocking issue 검사를 통과하면 resolver를 현재 Tester로 기록하고, assignee/verifier를 제거하며 fixer를 보존한다. 이때 dependency row는 자동 제거하지 않는다.
- RESOLVED -> CLOSED: assignee/verifier를 제거하고 fixer와 resolver를 보존한 채 상태를 최종 종료한다.
- RESOLVED/CLOSED -> REOPENED: fixer와 resolver를 보존한 채 재작업 가능 상태로 설정한다. assignee/verifier는 자동 복원하지 않으며 PL은 필요 시 UC5를 통해 배정한다.

### Alternative Flows
- 2a. 현재 상태에서 사용자의 역할로 수행 가능한 전이가 없다
    - 2a1. 시스템이 가능한 전이가 없다는 메시지를 보여준다
    - 2a2. 사용자가 이전 화면으로 돌아간다
- 5a. 사용자가 상태 변경 사유 코멘트를 입력하지 않는다
    - 5a1. 시스템이 코멘트 입력이 필요하다는 메시지를 보여준다
    - 5a2. 사용자는 상태 변경을 취소하거나 코멘트를 입력한다
- 6a. 사용자가 선택한 목표 상태가 허용된 전이가 아니다
    - 6a1. 시스템이 상태 변경을 거부하고 사유를 보여준다
    - 6a2. 사용자는 다시 목표 상태를 선택하거나 상태 변경을 취소한다
- 6b. FIXED -> RESOLVED 전이에서 unresolved blocking issue가 존재한다
    - 6b1. 시스템이 blocking issue가 남아 있어 resolve할 수 없음을 보여준다
    - 6b2. 사용자가 이전 화면으로 돌아간다
- *. 사용자가 상태 변경을 취소한다
    - *1. 시스템이 입력 내용을 폐기하고 이슈 상세 화면으로 돌아간다
---

## UC9. Manage Deleted Issue (삭제/복구 관리)

| 항목 | 내용 |
|---|---|
| ID | UC9 |
| Name | Manage Deleted Issue |
| Scope | Issue Tracking System |
| Level | User goal |
| Primary Actor | PL |
| Stakeholders & Interests | - PL: 더 이상 일반 흐름에 노출하지 않을 종료 이슈를 삭제 상태로 옮기고, 필요한 경우 삭제 상태 이슈를 복구하고 싶다<br>- 팀: 실수로 인한 즉시 물리 삭제를 피하면서 삭제 이슈 보관 한도를 관리하고 싶다 |
| Preconditions | - PL이 시스템에 로그인한 상태이다<br>- PL이 대상 프로젝트 또는 대상 이슈가 속한 프로젝트의 active PL이다<br>- 조회 경로: 대상 프로젝트가 시스템에 존재한다<br>- 삭제 경로: 대상 이슈가 시스템에 존재하며 상태가 NEW 또는 CLOSED이다<br>- 복구 경로: 대상 이슈가 시스템에 존재하며 상태가 DELETED이고 아직 영구 삭제되지 않았다<br>- 영구 삭제 경로: 대상 이슈가 시스템에 존재하며 상태가 DELETED이다 |
| Postconditions | - 조회 경로: 시스템이 해당 프로젝트의 DELETED 이슈 목록을 보여준다<br>- 삭제 경로: 시스템이 이슈의 status를 DELETED로 변경하고 deleted 전이 시각과 IssueHistory(STATUS_CHANGED)를 기록한다<br>- 삭제 경로: 시스템이 해당 이슈와 관련된 dependency를 제거한다<br>- 복구 경로: 시스템이 이슈의 status를 삭제 직전 상태(NEW 또는 CLOSED)로 변경하고 IssueHistory(STATUS_CHANGED)를 기록한다<br>- 복구 상태는 NEW/CLOSED -> DELETED 전이 때 기록된 IssueHistory(STATUS_CHANGED).previousValue에서 결정한다<br>- 복구 시 reporter, fixer, resolver, comments, history는 유지하고 assignee/verifier는 null 상태로 유지한다<br>- 삭제 시 제거된 dependency는 복구 시 자동 복원하지 않는다<br>- 복구된 이슈는 deleted 보관/FIFO 정리 후보에서 제외된다<br>- 영구 삭제 경로: 시스템이 선택한 DELETED 이슈와 관련 기록을 시스템에서 제거한다<br>- DELETED 상태 이슈가 보관 한도 30개를 초과하면 시스템이 FIFO 기준으로 가장 오래된 DELETED 이슈를 영구 삭제한다<br>- 삭제/복구 경로: 시스템이 PL이 입력한 사유 코멘트를 대상 이슈에 기록한다 |

| Trigger | PL이 Deleted Issue Management 화면을 열거나, NEW/CLOSED 이슈에서 삭제 액션을 선택하거나, DELETED 이슈 목록에서 복구/영구 삭제 액션을 선택한다 |

### Brief Main Flow
1. PL이 Deleted Issue Management 화면을 열거나, 이슈 삭제/복구/영구 삭제 액션을 선택한다
2. 시스템이 PL 권한과 대상 프로젝트 또는 대상 이슈의 현재 상태를 확인한다 (include UC14/권한 검사)
3. 조회 경로인 경우 시스템이 해당 프로젝트의 DELETED 이슈 목록을 보여준다
4. 삭제 또는 복구 경로인 경우 시스템이 UC2 Add Comment를 include하여 PL에게 삭제/복구 사유를 입력받는다
5. 삭제 경로인 경우 시스템이 이슈 상태를 NEW 또는 CLOSED에서 DELETED로 변경하고 관련 dependency를 제거한다
    - 삭제 후 DELETED 보관 한도를 초과하면 시스템이 오래된 DELETED 이슈부터 자동으로 정리한다
6. 복구 경로인 경우 시스템이 이슈 상태를 DELETED에서 삭제 직전 상태(NEW 또는 CLOSED)로 복구한다
7. 영구 삭제 경로인 경우 시스템이 선택한 DELETED 이슈와 관련 기록을 시스템에서 제거한다
8. 시스템이 갱신된 DELETED 이슈 목록 또는 이슈 상세 화면을 보여준다

### Alternative Flows
- 2a. PL 권한이 없다
    - 2a1. 시스템이 권한 거부 메시지를 보여준다
    - 2a2. 흐름이 종료된다
- 2b. 삭제 경로에서 대상 이슈가 NEW 또는 CLOSED가 아니다
    - 2b1. 시스템이 현재 상태에서는 삭제할 수 없다는 메시지를 보여준다
    - 2b2. PL이 이전 화면으로 돌아간다
- 2c. 복구 경로에서 대상 이슈가 DELETED가 아니거나 이미 영구 삭제되었다
    - 2c1. 시스템이 복구할 수 없다는 메시지를 보여준다
    - 2c2. PL이 이전 화면으로 돌아간다
- 2d. 영구 삭제 경로에서 대상 이슈가 DELETED가 아니다
    - 2d1. 시스템이 영구 삭제할 수 없다는 메시지를 보여준다
    - 2d2. PL이 이전 화면으로 돌아간다
- 2e. 대상 프로젝트 또는 대상 이슈가 존재하지 않는다
    - 2e1. 시스템이 대상을 찾을 수 없다는 메시지를 보여준다
    - 2e2. 흐름이 종료된다
- 4a. 삭제 또는 복구 사유가 비어있다
    - 4a1. 시스템이 사유 입력이 필요하다는 메시지를 보여준다
    - 4a2. PL이 사유를 다시 입력하거나 작업을 취소한다
- *. PL이 삭제, 복구 또는 영구 삭제를 취소한다
    - *1. 시스템이 변경 없이 이전 화면으로 돌아간다

### 설계 메모
- UC9는 UC6 Change Issue State에 편입하지 않는다. UC6과 UC9 모두 UC2 Add Comment를 include하지만, UC6은 일반 상태 변경 사유 코멘트이고 UC9는 삭제/복구 사유 코멘트를 강제하는 별도 PL 관리 목표이다.
- UC9의 UC2 include는 soft delete와 restore 경로에 적용되며, 단건 영구 삭제와 FIFO 보관 한도 정리에는 적용하지 않는다.
- FIFO 영구 삭제는 사용자가 직접 실행하는 별도 유스케이스가 아니라, DELETED 보관 한도 초과 시 적용되는 시스템 내부 보관 정책이다.
- 복구 대상 상태와 FIFO 정리 순서는 삭제 이력에 근거하여 판단한다.
- FIFO 기준 시각과 복구 대상 상태는 IssueHistory(STATUS_CHANGED) 기록을 기준으로 판단한다.
---

## 유스케이스 관계 요약

| Relation | Base UC | Related UC | Meaning |
|---|---|---|---|
| include | UC1 (이슈 등록) | UC14 (권한 검사) | 이슈 등록 전에 시스템이 사용자의 기본 권한을 확인한다 |
| include | UC5 (이슈 배정 및 배정 변경) | UC8 (Assignment Candidate 추천) | PL이 이슈를 배정하거나 변경할 때 시스템이 상태에 맞는 assignee/verifier 후보를 추천한다 |
| include | UC5 (이슈 배정 및 배정 변경) | UC14 (권한 검사) | 배정/변경 전에 시스템이 PL 권한과 대상 이슈 상태를 확인한다 |
| include | UC6 (이슈 상태 변경) | UC2 (코멘트 추가) | 상태 변경에는 사유 코멘트가 필요하므로 코멘트 추가 흐름을 포함한다 |
| include | UC6 (이슈 상태 변경) | UC14 (권한 검사) | 상태 변경 전에 시스템이 사용자 역할과 전이 가능 여부를 확인한다 |
| include | UC7 (이슈 의존성 관리) | UC14 (권한 검사) | 의존성 변경 전에 시스템이 PL 권한을 확인한다 |
| include | UC9 (삭제/복구 관리) | UC14 (권한 검사) | 삭제 이슈 조회, 삭제, 복구, 영구 삭제 전에 시스템이 PL 권한과 대상 상태를 확인한다 |
| include | UC9 (삭제/복구 관리) | UC2 (코멘트 추가) | soft delete와 restore에는 사유 코멘트가 필요하므로 코멘트 추가 흐름을 포함한다 |
| include | UC10 (이슈 통계 분석) | UC14 (권한 검사) | 통계 조회 전에 시스템이 프로젝트 통계 조회 권한을 확인한다 |
| include | UC12 (계정 관리) | UC14 (권한 검사) | 계정 관리 전에 시스템이 Admin 권한을 확인한다 |
| include | UC13 (프로젝트 관리) | UC14 (권한 검사) | 프로젝트 관리 전에 시스템이 Admin 권한을 확인한다 |
| include | UC16 (이슈 우선순위 변경) | UC14 (권한 검사) | 우선순위 변경 전에 시스템이 PL 권한을 확인한다 |
| extend | UC1 (이슈 등록) | UC2 (코멘트 추가) | 사용자가 등록 직후 코멘트를 남기고 싶을 때 추가한다 |
| extend | UC4 (이슈 상세 조회) | UC2 (코멘트 추가) | 사용자가 상세 화면에서 코멘트를 남기고 싶을 때 추가한다 |
| extend | UC5 (이슈 배정 및 배정 변경) | UC2 (코멘트 추가) | PL이 배정 또는 배정 변경 사유를 선택적으로 남기고 싶을 때 추가한다 |
| extend | UC4 (이슈 상세 조회) | UC15 (이슈 수정) | 사용자가 상세 화면에서 자신이 등록한 수정 가능한 이슈를 수정하고 싶을 때 추가한다 |

> 참고: 실제 구현에서는 UC2, UC3, UC4, UC15 등 보호된 기능에서도 로그인 여부, 프로젝트 소속, 역할, 이슈 상태 검사가 수행된다. 다만 유스케이스 다이어그램과 관계 요약에서는 UCD에 명시된 주요 include/extend 관계만 표시하고, 나머지 세부 권한 검사는 각 UC의 Preconditions와 Alternative Flows에서 설명한다.

---

## 전체 유스케이스 커버리지 매트릭스

| UC | Name | Feature Mapping | Detail Level |
|---|---|---|---|
| UC1 | Register Issue | 이슈 등록 | Fully Dressed |
| UC2 | Add Comment | 이슈 코멘트 추가 | Fully Dressed |
| UC3 | Search Issues | 이슈 검색/브라우즈 | Fully Dressed |
| UC4 | View Issue Detail | 이슈 상세 정보 및 코멘트 history 확인 | Fully Dressed |
| UC5 | Assign / Update Issue Assignment | 이슈 배정 및 배정 변경, 상태별 후보 추천 기능 사용 | Fully Dressed |
| UC6 | Change Issue State | 이슈 상태 변경 | Fully Dressed |
| UC7 | Manage Dependency | 이슈 의존성 관리 | Brief |
| UC8 | Recommend Assignment Candidate | Issue status, 과거 완료 이력, 이슈 내용 유사도 기반 assignee/verifier 후보 추천 | Brief / included by UC5 |
| UC9 | Manage Deleted Issue | NEW/CLOSED 이슈 soft-delete, DELETED 이슈 조회/복구/단건 영구 삭제; 30개 초과 FIFO 영구 삭제는 soft-delete 후 시스템 내부 보관 정책으로 자동 수행 | Fully Dressed |
| UC10 | Statistics | 프로젝트 범위에서 DELETED 이슈를 제외한 일/월별 이슈 등록, 상태 변경, 코멘트, 상태/우선순위 분포 통계를 조회 | Brief |
| UC11 | Log In | 로그인 | Brief |
| UC12 | Manage Accounts | 계정 추가 및 관리 (Admin) | Brief |
| UC13 | Manage Projects | 프로젝트 추가 및 관리 (Admin) | Brief |
| UC14 | Verify Permission | 권한 검사 | Brief / included by multiple UCs |
| UC15 | Edit Issue | 이슈 수정 | Brief / extends UC4 |
| UC16 | Change Priority | PL의 우선순위 변경 | Brief / includes UC14 |

---

## 데모 시나리오와 핵심 UC 매핑

| Demo Flow | Related UC |
|---|---|
| tester1이 이슈를 생성한다 | UC1 Register Issue |
| tester1이 생성한 이슈에 코멘트를 추가한다 | UC2 Add Comment |
| PL1이 모든 이슈를 브라우즈하고 NEW 상태 이슈를 찾는다 | UC3 Search Issues |
| PL1이 특정 이슈 상세 정보를 확인한다 | UC4 View Issue Detail |
| PL1이 dev1을 assignee로, tester1를 verifier로 지정한다 | UC5 Assign / Update Issue Assignment |
| dev1이 assigned 이슈를 찾고 내용을 확인한다 | UC3, UC4 |
| dev1이 수정 완료 후 fixed로 변경하고 코멘트를 남긴다 | UC6 Change Issue State, UC2 Add Comment |
| tester1이 fixed 이슈를 resolved로 변경한다 | UC6 Change Issue State |
| PL1이 resolved 이슈를 closed로 변경한다 | UC6 Change Issue State |
| 시스템이 이슈 상태와 fixer/resolver 이력을 바탕으로 assignee/verifier 후보를 추천한다 | UC8 Recommend Assignment Candidates (UC5에서 include) |
| 일/월별 이슈 통계를 확인한다 | UC10 Statistics |
| PL1이 CLOSED 이슈를 삭제 상태로 옮기거나 DELETED 이슈를 복구한다 | UC9 Manage Deleted Issue |

---

## 제출 전 팀 문장 검수 TODO

- [ ] 최종 제출 PDF 반영 전 팀원이 UC별 wording, actor 표현(`Admin`, `Auth User`, `Tester`, `Dev`, `PL`), include/extend 설명, 상태명, `reportedDate` 용어를 공동 검수한다.

---
