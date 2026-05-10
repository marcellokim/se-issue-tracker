# 유스케이스 명세 (Use Case Specifications)

## 작성 범위

본 문서는 Issue Tracker System(이하 ITS)의 전체 유스케이스 중 핵심 유스케이스 6개인 UC1-UC6을 Fully Dressed 형식으로 상세 명세한다.
나머지 UC7-UC16은 유스케이스 다이어그램과 전체 유스케이스 커버리지 매트릭스에서 전체 기능 범위를 보이되, 본 문서에서는 상세 명세하지 않는다.
UC8, UC14, UC15는 관련 UC에서 include/extend 관계로 참조되며, UC16은 PL 전용 우선순위 변경 유스케이스로 UC14를 include한다.

---

## 핵심 유스케이스 선정 근거

ITS의 핵심 목적은 이슈를 등록하고, 사용자와 이슈 사이의 상호작용을 기록하며, 이슈의 배정과 상태 변화를 관리하는 것이다.
이에 따라 UC1-UC6을 Fully Dressed 대상으로 선정하였다.

- **UC1 Register Issue**: 이슈 생명주기의 시작점
- **UC2 Add Comment**: 사용자와 이슈 간 상호작용 기록
- **UC3 Search Issues**: 사용자가 필요한 이슈를 찾는 진입점
- **UC4 View Issue Detail**: 이슈 정보와 코멘트 history 확인
- **UC5 Assign Issue**: PL의 이슈 배정 및 관리
- **UC6 Change Issue State**: Dev, Tester, PL의 상태 전이 관리

UC7-UC16은 전체 시스템 기능에는 포함되지만, 핵심 이슈 생명주기를 보조하거나 관리하는 기능이므로 Brief 수준으로만 다룬다.

---

## Actor 정의

| Actor | 의미 |
|---|---|
| Admin | 계정과 프로젝트를 관리하는 독립 액터. Auth User와 별도의 계층이다 |
| Auth User | 로그인한 이슈 관련 사용자에 대한 abstract actor |
| Tester | Auth User의 specialization. 이슈 등록 및 검증 수행 |
| Dev | Auth User의 specialization. 배정된 이슈 수정 수행 |
| PL | Auth User의 specialization. 이슈 배정, 종료, 재개, 우선순위 변경 수행 |

---

## include/extend 관계 요약

- UC1 Register Issue는 UC2 Add Comment에 의해 **extend**된다. 따라서 이슈 등록 코멘트는 선택 사항이다.
- UC4 View Issue Detail은 UC2 Add Comment에 의해 **extend**된다. 따라서 상세 조회 중 코멘트 추가는 선택 사항이다.
- UC5 Assign Issue는 UC8 Recommend Assignee를 **include**한다. 배정 시작 시 추천 후보는 항상 계산되지만, PL이 추천 후보를 반드시 선택해야 하는 것은 아니다.
- UC5 Assign Issue는 UC2 Add Comment에 의해 **extend**된다. 따라서 배정 코멘트는 선택 사항이다.
- UC6 Change Issue State는 UC2 Add Comment를 **include**한다. 따라서 상태 변경 사유 코멘트는 필수이다.

---

## 명세되는 유스케이스 6가지

- **UC1 : Register Issue (이슈 등록)**
- **UC2 : Add Comment (코멘트 추가)**
- **UC3 : Search Issues (이슈 검색 및 브라우즈)**
- **UC4 : View Issue Detail (이슈 상세 조회)**
- **UC5 : Assign Issue (이슈 배정)**
- **UC6 : Change Issue State (이슈 상태 변경)**

---

## UC1 : Register Issue (이슈 등록)

| 항목 | 내용 |
|---|---|
| ID | UC1 |
| 이름 | Register Issue |
| 범위 | Issue Tracking System |
| 수준 | User goal |
| Primary Actor | Auth User(Tester, Dev, PL) |
| Stakeholders & Interests | - 이슈 발견자: 발견한 이슈를 시스템에 누락 없이 기록하고 싶다<br>- PL: 등록된 이슈를 추적할 수 있기를 원한다 |
| Preconditions | - 사용자가 시스템에 로그인한 상태이다<br>- 대상 프로젝트가 시스템에 존재한다 |
| Postconditions | - 시스템이 새 이슈를 저장한다<br>- 시스템이 reporter를 현재 사용자로, reportedDate를 현재 시각으로, status를 NEW로 설정한다<br>- 사용자가 priority를 선택하지 않았다면 시스템이 MAJOR를 기본값으로 부여한다 |
| Trigger | 사용자가 메뉴에서 "Register Issue"를 선택한다 |

### Main Flow
1. 사용자가 이슈 등록 화면을 연다
2. 시스템이 이슈 등록 폼을 보여준다
3. 사용자가 Title, Description, Priority를 입력 및 선택한다
4. 사용자가 저장 및 등록 버튼을 누른다
5. 시스템이 권한을 확인한다 (include UC14/권한 검사)
6. 시스템이 입력값을 확인한다
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
- *. 사용자가 등록을 취소한다
    - *1. 시스템이 입력 내용을 폐기하고 이전 화면으로 돌아간다

---

## UC2. Add Comment (코멘트 추가)

| 항목 | 내용 |
|---|---|
| ID | UC2 |
| 이름 | Add Comment |
| 범위 | Issue Tracking System |
| 수준 | Subfunction |
| Primary Actor | Auth User(Tester, Dev, PL) |
| Stakeholders & Interests | - 사용자: 이슈에 자신의 의견과 진행 상황 등을 기록하고 싶다<br>- 조직: 이슈 배정 이후 해당 이슈와 관련된 사용자들의 이슈에 대한 의견을 확인하고 싶다 |
| Preconditions | - 호출 UC가 진행 중이고, 대상 이슈가 화면에 있다<br>- 사용자가 시스템에 로그인한 상태이다 |
| Postconditions | - 시스템이 새 코멘트를 이슈의 코멘트 history에 추가한다<br>- 시스템이 코멘트 등록자를 현재 사용자로, 코멘트 작성 시간을 현재 시각으로 기록한다 |
| Trigger | - UC1(이슈 등록), UC4(이슈 상세 조회), UC5(이슈 배정)의 extension point에서 사용자가 코멘트 추가를 선택 (extend)<br>- UC6(이슈 상태 변경)의 코멘트 입력 단계에서 자동 시작 (include) |

### Main Flow
1. 시스템이 코멘트 입력 영역을 보여준다
2. 사용자가 코멘트를 입력한다
3. 사용자가 코멘트 업로드 버튼을 누른다
4. 시스템이 본문이 비어있지 않은지 확인한다
5. 시스템이 코멘트를 이슈의 코멘트 history에 추가한다
6. 시스템이 호출 UC로 돌아간다

### Alternative Flows
- 4a. 코멘트 내용이 비어있다
    - 4a-1. UC1, UC4, UC5에서 호출된 경우 (extend)
        - 시스템이 코멘트 입력을 취소한 것으로 간주하여 호출 UC로 복귀한다
    - 4a-2. UC6에서 호출된 경우 (include)
        - 시스템이 코멘트 내용을 입력하라는 메시지를 보여준다
        - 단계 2로 되돌아간다
        - 사용자가 코멘트 내용을 입력할 때까지 호출 UC가 다음 단계로 진행하지 않는다

---

## UC3. Search Issues (이슈 검색 및 브라우즈)

| 항목 | 내용 |
|---|---|
| ID | UC3 |
| 이름 | Search Issues |
| 범위 | Issue Tracking System |
| 수준 | User goal |
| Primary Actor | Auth User (Tester, PL, Dev) |
| Stakeholders & Interests | - Tester: 자신이 검증하여 fixed→resolved할 이슈를 찾고 싶다<br>- PL: 프로젝트 이슈들을 종류와 상태 기준으로 찾고 싶다<br>- Dev: 자신에게 지정된 이슈를 찾고 싶다 |
| Preconditions | 사용자가 시스템에 로그인한 상태이다 |
| Postconditions | 시스템이 검색 조건에 맞는 이슈 목록을 보여준다 |
| Trigger | 사용자가 "Search Issues" 메뉴를 선택한다 |

### Main Flow
1. 사용자가 검색 화면을 연다
2. 시스템이 검색 조건 입력 영역을 보여준다
3. 사용자가 검색 조건을 입력한다
4. 사용자가 검색 버튼을 누른다
5. 시스템이 조건에 맞는 이슈를 찾는다
6. 시스템이 결과 목록을 reportedDate 내림차순으로 보여준다

### Alternative Flows
- 3a. 사용자가 조건 없이 검색한다
    - 3a1. 시스템이 모든 이슈를 보여준다
- 5a. 조건에 맞는 결과가 없다
    - 5a1. 시스템이 조건에 맞는 결과가 없다는 메시지를 보여준다
- 6a. 사용자가 결과 목록에서 이슈 한 건을 선택한다
    - 6a1. 흐름이 종료되고 UC4가 시작된다

---

## UC4. View Issue Detail (이슈 상세 조회)

| 항목 | 내용 |
|---|---|
| ID | UC4 |
| 이름 | View Issue Detail (이슈 상세 조회) |
| 범위 | Issue Tracking System |
| 수준 | User goal |
| Primary Actor | Auth User (Tester, Dev, PL) |
| Stakeholders & Interests | - 사용자: 특정 이슈에 대한 모든 정보를 확인하고 싶다 |
| Preconditions | - 사용자가 시스템에 로그인한 상태이다<br>- 대상 이슈가 시스템에 존재한다 |
| Postconditions | 시스템이 이슈의 모든 필드와 코멘트 history를 화면에 보여준다 |
| Trigger | 사용자가 이슈 목록 또는 검색 결과에서 특정 이슈를 선택한다 |

### Main Flow
1. 사용자가 이슈를 선택한다
2. 시스템이 이슈를 불러온다
3. 시스템이 이슈의 모든 필드를 보여준다
4. 시스템이 코멘트 history를 등록한 시간 순서대로 보여준다
5. 시스템이 사용자의 역할에 따라 가능한 액션 버튼을 보여준다
6. 사용자가 화면을 확인한다
extension points: UC2(Add Comment), UC15(이슈 수정)

### Alternative Flows
- 2a. 이슈가 존재하지 않는다
    - 2a1. 시스템이 이슈를 찾을 수 없다는 메시지를 보여준다
    - 2a2. 사용자가 이전 화면으로 돌아간다
- 5a. PL이 이슈 배정 액션을 선택한다
    - 5a1. UC4가 종료되고 UC5(이슈 배정)가 시작된다
- 5b. 사용자가 이슈 상태 변경 액션을 선택한다
    - 5b1. UC4가 종료되고 UC6(이슈 상태 변경)가 시작된다

---

## UC5. Assign Issue (이슈 배정)

| 항목 | 내용 |
|---|---|
| ID | UC5 |
| 이름 | Assign Issue |
| 범위 | Issue Tracking System |
| 수준 | User goal |
| Primary Actor | PL |
| Stakeholders & Interests | - PL: 적절한 Dev에게 이슈를 배정하고 싶다<br>- Dev: 자신에게 맞는 이슈를 배정 받고 싶다<br>- Tester: 이슈가 Dev에게 배정되어 수정된 후에 검증할 수 있기를 원한다 |
| Preconditions | - PL이 시스템에 로그인한 상태이다<br>- 대상 이슈가 시스템에 존재한다<br>- 대상 이슈의 상태가 NEW 또는 REOPENED이다 |
| Postconditions | - 시스템이 이슈의 assignee를 PL이 선택한 Dev로 설정한다<br>- 시스템이 이슈의 verifier를 PL이 선택한 Tester로 설정한다<br>- 시스템이 이슈의 status를 ASSIGNED로 변경한다 |
| Trigger | PL이 이슈 상세 화면에서 이슈 배정 액션을 선택한다 |

### Main Flow
1. PL이 이슈 배정 버튼을 누른다
2. 시스템이 권한과 전이 규칙을 확인한다 (include UC14/권한 검사)
3. 시스템이 배정 가능한 Dev 목록과 Tester 목록을 보여준다
4. 시스템이 resolved/closed 이력 기반으로 적합한 Dev 후보를 추천한다 (include UC8/Assignee 자동 추천)
5. PL이 assignee로 지정할 Dev와 verifier로 지정할 Tester를 선택한다
extension point: UC2(코멘트 추가)
6. 시스템이 이슈의 assignee, verifier, status를 갱신한다
7. 시스템이 갱신된 이슈 상세 화면을 보여준다

### Alternative Flows
- 2a. PL의 역할이 배정 권한을 가지지 않는다
    - 2a1. 시스템이 권한 거부 메시지를 보여준다
    - 2a2. 흐름이 종료된다
- 2b. 이슈 상태가 NEW 또는 REOPENED가 아니어서 배정 전이가 불가능하다
    - 2b1. 시스템이 현재 상태에서는 배정할 수 없다는 메시지를 보여준다
    - 2b2. PL이 이전 화면으로 돌아간다
- 3a. 배정 가능한 Dev가 없다
    - 3a1. 시스템이 배정 가능한 Dev가 없다는 메시지를 보여준다
    - 3a2. PL이 이전 화면으로 돌아간다
- 3b. 배정 가능한 Tester가 없다
    - 3b1. 시스템이 배정 가능한 Tester가 없다는 메시지를 보여준다
    - 3b2. PL이 이전 화면으로 돌아간다
- *. PL이 배정을 취소한다
    - *1. 시스템이 입력 내용을 폐기하고 이슈 상세 화면으로 돌아간다

---

## UC6. Change Issue State (이슈 상태 변경)

| 항목 | 내용 |
|---|---|
| ID | UC6 |
| 이름 | Change Issue State (이슈 상태 변경) |
| 범위 | Issue Tracking System |
| 수준 | User goal |
| Primary Actor | Dev, Tester, PL (역할에 따라 수행 가능한 전이가 다름) |
| Stakeholders & Interests | - Dev: 자신이 해결한 이슈의 진행 상태를 시스템에 반영하고 싶다<br>- Tester: 자신이 검증을 담당하는 이슈의 수정 결과를 확인하고 검증 결과를 반영하고 싶다<br>- PL: 검증 완료된 이슈를 종료하거나, 미해결 이슈를 재개하고 싶다 |
| Preconditions | - 사용자가 시스템에 로그인한 상태이다<br>- 대상 이슈가 시스템에 존재한다<br>- 이슈의 현재 상태에서 사용자의 역할로 수행 가능한 전이가 존재한다 |
| Postconditions | - 시스템이 이슈의 status를 목표 상태로 변경한다<br>- 시스템이 전이에 따른 자동 필드를 갱신한다<br>- 시스템이 상태 변경 사유 코멘트를 코멘트 history에 추가한다 |
| Trigger | 사용자가 이슈 상세 화면에서 상태 변경 액션을 선택한다 |

### 역할별 허용 전이
| 현재 상태 | 목표 상태 | 수행 역할 | 자동 필드 | 설명 |
|---|---|---|---|---|
| ASSIGNED | FIXED | Dev (assignee 본인) | fixer = 현재 Dev | Dev가 이슈 수정을 완료했음을 표시 |
| FIXED | RESOLVED | Tester (verifier 본인) | — | verifier가 수정 결과를 검증하여 해결됨을 확인 |
| FIXED | ASSIGNED | Tester (verifier 본인) | fixer 초기화 | verifier가 수정이 불충분하다고 판단하여 상태를 되돌림 |
| RESOLVED | CLOSED | PL | — | PL이 검증 완료된 이슈를 종료시킴 |
| CLOSED / RESOLVED | REOPENED | PL | 마지막 assignee, verifier, fixer를 복원 | PL이 종료된 이슈를 재개. PL은 필요 시 UC5를 통해 재배정할 수 있다 |

### Main Flow
1. 사용자가 상태 변경 버튼을 누른다
2. 시스템이 사용자의 역할과 이슈의 현재 상태에 따라 가능한 목표 상태 목록을 보여준다
3. 사용자가 목표 상태를 선택한다
4. 시스템이 상태 변경 사유 코멘트 입력을 요청한다 (include UC2/코멘트 추가)
5. 시스템이 권한과 전이 규칙을 확인한다 (include UC14/권한 검사)
6. 시스템이 이슈의 status를 갱신하고, 전이에 따른 자동 필드를 처리한다
    - ASSIGNED → FIXED: fixer를 현재 Dev로 기록한다
    - FIXED → RESOLVED: 추가 자동 필드 없이 상태만 변경한다
    - FIXED → ASSIGNED: fixer를 초기화하고 기존 assignee에게 재배정한다
    - RESOLVED → CLOSED: 추가 자동 필드 없이 상태를 최종 종료한다
    - CLOSED/RESOLVED → REOPENED: 마지막 assignee, verifier, fixer 정보를 복원하여 재작업 가능 상태로 설정한다. PL은 필요 시 UC5를 통해 재배정할 수 있다
7. 시스템이 갱신된 이슈 상세 화면을 보여준다

### Alternative Flows
- 2a. 현재 상태에서 사용자의 역할로 수행 가능한 전이가 없다
    - 2a1. 시스템이 가능한 전이가 없다는 메시지를 보여준다
    - 2a2. 사용자가 이전 화면으로 돌아간다
- 5a. 사용자의 역할이 선택한 전이를 수행할 수 없다
    - 5a1. 시스템이 거부 사유와 가능한 전이 목록을 보여준다
    - 5a2. 사용자가 다시 선택한다 (단계 3으로 복귀)
- 5b. FIXED → RESOLVED 또는 FIXED → ASSIGNED 전이에서 사용자가 해당 이슈의 verifier가 아니다
    - 5b1. 시스템이 지정된 verifier만 검증할 수 있다는 메시지를 보여준다
    - 5b2. 사용자가 이전 화면으로 돌아간다
- 5c. ASSIGNED → FIXED 전이에서 사용자가 해당 이슈의 assignee가 아니다
    - 5c1. 시스템이 지정된 assignee만 수정 완료할 수 있다는 메시지를 보여준다
    - 5c2. 사용자가 이전 화면으로 돌아간다
- *. 사용자가 상태 변경을 취소한다
    - *1. 시스템이 입력 내용을 폐기하고 이슈 상세 화면으로 돌아간다

---

## 유스케이스 관계 요약

| 관계 | Base UC | 관련 UC | 의미 |
|---|---|---|---|
| include | UC1 (이슈 등록) | UC14 (권한 검사) | 등록 시 시스템이 사용자 권한을 검사한다 |
| include | UC5 (이슈 배정) | UC8 (Assignee 자동 추천) | PL이 NEW 또는 REOPENED 이슈에 assignee를 지정하려는 시점에 시스템이 후보를 추천한다 |
| include | UC5 (이슈 배정) | UC14 (권한 검사) | 배정 시 시스템이 권한과 전이 규칙을 검사한다 |
| include | UC6 (이슈 상태 변경) | UC2 (코멘트 추가) | 상태를 변경할 때 시스템이 사유 코멘트를 강제한다 |
| include | UC6 (이슈 상태 변경) | UC14 (권한 검사) | 상태 전이 시 시스템이 권한과 전이 규칙을 검사한다 |
| include | UC16 (이슈 우선순위 변경) | UC14 (권한 검사) | 우선순위 변경 시 시스템이 PL 권한을 검사한다 |
| include | UC7 (이슈 의존성 관리) | UC14 (권한 검사) | 의존성 변경 시 시스템이 PL 권한을 검사한다 |
| include | UC9 (이슈 제거) | UC14 (권한 검사) | 삭제 시 시스템이 PL 권한을 검사한다 |
| include | UC10 (이슈 통계 분석) | UC14 (권한 검사) | 통계 조회 시 시스템이 권한을 검사한다 |
| include | UC12 (계정 관리) | UC14 (권한 검사) | 계정 관리 시 시스템이 Admin 권한을 검사한다 |
| include | UC13 (프로젝트 관리) | UC14 (권한 검사) | 프로젝트 관리 시 시스템이 Admin 권한을 검사한다 |
| extend | UC1 (이슈 등록) | UC2 (코멘트 추가) | 사용자가 등록 직후 코멘트를 남기고 싶을 때 추가한다 |
| extend | UC4 (이슈 상세 조회) | UC2 (코멘트 추가) | 사용자가 상세 화면에서 코멘트를 남기고 싶을 때 추가한다 |
| extend | UC5 (이슈 배정) | UC2 (코멘트 추가) | PL이 이슈를 배정할 때 배정 사유를 코멘트로 남기고 싶을 때 추가한다 |
| extend | UC4 (이슈 상세 조회) | UC15 (이슈 수정) | 사용자가 상세 화면에서 이슈를 수정하고 싶을 때 추가한다 |

---

## 전체 유스케이스 커버리지 매트릭스

| UC | 이름 | 과제 기능 대응 | 상세 수준 |
|---|---|---|---|
| UC1 | Register Issue | 이슈 등록 | Fully Dressed |
| UC2 | Add Comment | 이슈 코멘트 추가 | Fully Dressed |
| UC3 | Search Issues | 이슈 검색/브라우즈 | Fully Dressed |
| UC4 | View Issue Detail | 이슈 상세 정보 및 코멘트 history 확인 | Fully Dressed |
| UC5 | Assign Issue | 이슈 배정, 추천 기능 사용 | Fully Dressed |
| UC6 | Change Issue State | 이슈 상태 변경 | Fully Dressed |
| UC7 | Manage Dependency | 이슈 의존성 관리 | Brief |
| UC8 | Recommend Assignee | resolved/closed 이력 기반 assignee 추천 | Brief / included by UC5 |
| UC9 | Delete Issue | 이슈 삭제 (soft-delete) | Brief |
| UC10 | Statistics | 일/월별 이슈 발생 및 트렌드 분석 | Brief |
| UC11 | Log In | 로그인 | Brief |
| UC12 | Manage Accounts | 계정 추가 및 관리 (Admin) | Brief |
| UC13 | Manage Projects | 프로젝트 추가 및 관리 (Admin) | Brief |
| UC14 | Verify Permission | 권한 검사 | Brief / included by multiple UCs |
| UC15 | Edit Issue | 이슈 수정 | Brief / extends UC4 |
| UC16 | Change Priority | PL의 우선순위 변경 | Brief / includes UC14 |

---

## 데모 시나리오와 핵심 UC 매핑

| 데모 흐름 | 대응 UC |
|---|---|
| tester1이 이슈를 생성한다 | UC1 Register Issue |
| tester1이 생성한 이슈에 코멘트를 추가한다 | UC2 Add Comment |
| PL1이 모든 이슈를 브라우즈하고 NEW 상태 이슈를 찾는다 | UC3 Search Issues |
| PL1이 특정 이슈 상세 정보를 확인한다 | UC4 View Issue Detail |
| PL1이 dev1을 assignee로, tester1를 verifier로 지정한다 | UC5 Assign Issue |
| dev1이 assigned 이슈를 찾고 내용을 확인한다 | UC3, UC4 |
| dev1이 수정 완료 후 fixed로 변경하고 코멘트를 남긴다 | UC6 Change Issue State, UC2 Add Comment |
| tester1이 fixed 이슈를 resolved로 변경한다 | UC6 Change Issue State |
| PL1이 resolved 이슈를 closed로 변경한다 | UC6 Change Issue State |
| 시스템이 resolved/closed 이력을 바탕으로 assignee를 추천한다 | UC8 Recommend Assignee (UC5에서 include) |
| 일/월별 이슈 통계를 확인한다 | UC10 Statistics |

---

## 변경 이력
- 2026-04-29 _초안 작성_
- 2026-05-01 _텍스트 문법 수정_
- 2026-05-02 _가정 사항 변경을 반영하여 재작성 : 이슈 등록 actor를 Tester로 한정, UC2를 코멘트 추가로 변경, 등록&해결 전이 시 코멘트 정책 반영_
- 2026-05-02 _명세 정합성 보강 : 오타·단계 번호 정정, UC4 Extensions에 UC5 호출 명시, UC6 Trigger·Extension Point 명확화, UC12 include 관계 요약 표에 추가, Extension Points 항목 추가, include/extend 표기 통일_
- 2026-05-05 _UC 구조 변경 : 기존 UC5(배정 및 상태 변경)를 UC5(이슈 배정)와 UC6(이슈 상태 변경)로 분리, 기존 UC6(Assignee 추천)을 UC8로 번호 재배정, UC14(권한 검사)로 include 참조 일괄 수정, UC1 Primary Actor를 Auth User로 확장, UC2 Actor 직접 연결 제거 및 subfunction으로 정리, UC4에 UC15(이슈 수정) extension point 추가, UC5에 verifier 지정 흐름 반영, UC6에 역할별 허용 전이 요약, 역전이(FIXED→ASSIGNED) & RESOLVED→CLOSED 전이 추가, 유스케이스 관계 요약 전체 갱신_
- 2026-05-09 _UC5 Postconditions 수정: 배정 사유 코멘트 항목 삭제 (extend 관계이므로 선택적), 관계 요약표 UC5→UC2 설명 수정_
- 2026-05-10 _#63 리뷰 반영 전면 보강: 작성 범위·핵심 UC 선정 근거·Actor 정의·include/extend 관계 설명 섹션 추가, UC5 Main Flow 권한 검사 순서를 목록 노출 이전으로 이동(보안 노출 방지), UC6 REOPENED 자동 필드 설명 통일, include/extend 관계 요약에 UC4→UC2 extend 추가, 관계 요약표에 UC16→UC14 include 추가, 전체 유스케이스 커버리지 매트릭스 추가, 데모 시나리오 매핑 추가, UC4→UC16 extend 관계 제거(다이어그램 미확정으로 명세에서 선제거)_