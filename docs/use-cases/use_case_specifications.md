# 유스케이스 명세 (Use Case Specifications)

> 이 문서는 이슈 관리 시스템의 include + extend 관계를 포함한 유스케이스 6개에 대한 Fully Dressed 명세입니다. UC14(권한 검사)는 다른 UC가 include하는 유스케이스 이며 UC15(이슈 수정)은 다른 UC를 extend하는 유스케이스입니다. 본 문서에서는 명세를 작성하지 않습니다.

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
| Primary Actor | Auth User(Tester, Dev, PL)|
| Stakeholders & Interests | - 이슈 발견자: 발견한 이슈를 시스템에 누락 없이 기록하고 싶다<br>- PL: 등록된 이슈를 추적할 수 있기를 원한다 |
| Preconditions | - Auth User가 시스템에 로그인한 상태이다<br>- 대상 프로젝트가 시스템에 존재한다 |
| Postconditions  | - 시스템이 새 이슈를 저장한다<br>- 시스템이 reporter를 현재 Auth User로, reportedDate를 현재 시각으로, status를 NEW로 설정한다<br>- Auth User가 priority를 선택하지 않았다면 시스템이 MAJOR를 기본값으로 부여한다 |
| Trigger | Auth User가 메뉴에서 "Register Issue"를 선택한다 |

### Main Flow
1. Auth User가 이슈 등록 화면을 연다
2. 시스템이 이슈 등록 폼을 보여준다
3. Auth User가 Title, Description, Priority를 입력 및 선택한다
4. Auth User가 저장 및 등록 버튼을 누른다
5. 시스템이 권한을 확인한다(include UC14/권한 검사)
6. 시스템이 입력값을 확인한다
7. 시스템이 이슈를 저장한다
8. 시스템이 등록 완료 화면을 보여준다
extension point : UC2(Add Comment)

### Alternative Flows
- 5a. 사용자가 이슈 등록 권한을 가지지 않는다
    - 5a1. 시스템이 권한 거부 메시지를 보여준다
    - 5a2. 흐름이 종료된다
- 6a. Title 또는 Description이 비어있다
    - 6a1. 시스템이 누락 항목을 표시한다
    - 6a2. Auth User가 다시 입력한다 (단계 3으로 복귀)
- *. Auth User가 등록을 취소한다
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
| Stakeholders & Interests | - Auth User: 이슈에 자신의 의견과 진행 상황 등을 기록하고 싶다<br>- 조직: 이슈 배정 이후 해당 이슈와 관련된 사용자들의 이슈에 대한 
의견을 확인하고 싶다 |
| Preconditions | - 호출 UC가 진행 중이고, 대상 이슈가 화면에 있다<br>- Auth User가 시스템에 로그인한 상태이다 |
| Postconditions  | - 시스템이 새 코멘트를 이슈의 코멘트 history에 추가한다<br>- 시스템이 코멘트 등록자를 현재 Auth User로, 코멘트 작성 시간을 현재 시각으로 기록한다 |
| Trigger | - UC1(이슈 등록), UC4(이슈 상세 조회)의 extension point에서 사용자가 코멘트 추가를 선택(<<extend>>)<br>-UC5(이슈 배정), UC6(이슈 상태 변경)의 코멘트 입력 단계에서 자동 시작 (<<include>>) |

### Main Flow
1. 시스템이 코멘트 입력 영역을 보여준다
2. Auth User가 코멘트를 입력한다
3. Auth User가 코멘트 업로드 버튼을 누른다
4. 시스템이 본문이 비어있지 않은지 확인한다
5. 시스템이 코멘트를 이슈의 코멘트 history에 추가한다
6. 시스템이 호출 UC로 돌아간다

### Alternative Flows
- 4a. 코멘트 내용이 비어있다
    - 4a-1. UC1 또는 UC4에서 호출된 경우
        - 시스템이 코멘트 입력을 취소한것으로 간주하여 호출 UC로 복귀한다
    - 4a-2. UC5, UC6에서 호출된 경우
        - 시스템이 코멘트 내용을 입력하라는 메시지를 보여준다
        - 단계 2로 되돌아 간다
        - Auth User가 코멘트 내용을 입력할 때까지 호출 UC가 다음 단계로 진행하지 않는다

---

## UC3. Search Issues (이슈 검색 및 브라우즈)

| 항목 | 내용 |
|---|---|
| ID | UC3 |
| 이름 | Search Issues |
| 범위 | Issue Tracking System |
| 수준 | User goal |
| Primary Actor | Auth User (Tester, PL, Dev)|
| Stakeholders & Interests | - Tester: 자신이 검증하여 fixed->resolved할 이슈를 찾고 싶다<br>- PL: 프로젝트 이슈들을 종류와 상태 기준으로 찾고 싶다<br>-Dev: 자신에게 지정된 이슈를 찾고 싶다 |
| Preconditions | Auth User가 시스템에 로그인한 상태이다 |
| Postconditions  | 시스템이 검색 조건에 맞는 이슈 목록을 보여준다 |
| Trigger | Auth User가 "Search Issues" 메뉴를 선택한다 |

### Main Flow
1. Auth User가 검색 화면을 연다
2. 시스템이 검색 조건 입력 영역을 보여준다
3. Auth User가 검색 조건을 입력한다
4. Auth User가 검색 버튼을 누른다
5. 시스템이 조건에 맞는 이슈를 찾는다
6. 시스템이 결과 목록을 reportedDate 내림차순으로 보여준다

### Alternative Flows
- 3a. Auth User가 조건 없이 검색한다
    - 3a1. 시스템이 모든 이슈를 보여준다
- 5a. 조건에 맞는 결과가 없다
    - 5a1. 시스템이 조건에 맞는 결과가 없다는 메시지를 보여준다
- 6a. Auth User가 결과 목록에서 이슈 한 건을 선택한다
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
| Stakeholders & Interests | - Auth User: 특정 이슈에 대한 모든 정보를 확인하고 싶다 |
| Preconditions | - Auth User가 시스템에 로그인한 상태이다<br>- 대상 이슈가 시스템에 존재한다 |
| Postconditions | 시스템이 이슈의 모든 필드와 코멘트 history를 화면에 보여준다 |
| Trigger | Auth User가 이슈 목록 또는 검색 결과에서 특정 이슈를 선택한다 |

### Main Flow
1. Auth User가 이슈를 선택한다
2. 시스템이 이슈를 불러온다
3. 시스템이 이슈의 모든 필드를 보여준다
4. 시스템이 코멘트 history를 등록한 시간 순서대로 보여준다
5. 시스템이 Auth User의 역할에 따라 가능한 액션 버튼을 보여준다
6. Auth User가 화면을 확인한다
extension points : UC2(Add Comment), UC15(이슈 수정) 

### Alternative Flows
- 2a. 이슈가 존재하지 않는다
    - 2a1. 시스템이 이슈를 찾을 수 없다는 메시지를 보여준다
    - 2a2. Auth User가 이전 화면으로 돌아간다
- 5a. PL이 이슈 배정 액션을 선택한다
    - 5a1. UC4가 종료되고 UC5(이슈 배정)가 시작된다
- 5b. Auth User가 이슈 상태 변경 액션을 선택한다
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
| Preconditions | - PL이 시스템에 로그인한 상태이다<br>- 대상 이슈가 시스템에 존재한다<br>- 대상 이슈의 상태가 NEW이다 |
| Postconditions | - 시스템이 이슈의 assignee를 PL이 선택한 Dev로 설정한다<br>- 시스템이 이슈의 verifier를 PL이 선택한 Tester로 설정한다<br>- 시스템이 이슈의 status를 ASSIGNED로 변경한다<br>- 시스템이 배정 사유 코멘트를 코멘트 history에 추가한다 |
| Trigger | PL이 이슈 상세 화면에서 이슈 배정 액션을 선택한다 |

### Main Flow
1. PL이 이슈 배정 버튼을 누른다
2. 시스템이 배정 가능한 Dev 목록과 Tester 목록을 보여준다
extension point: UC8 (Assignee 자동 추천)
3. PL이 assignee로 지정할 Dev와 verifier로 지정할 Tester를 선택한다
4. 시스템이 배정 사유 코멘트 입력을 요청한다 (include UC2/코멘트 추가)
5. 시스템이 권한과 전이 규칙을 확인한다 (include UC14/권한 검사)
6. 시스템이 이슈의 assignee, verifier, status를 갱신한다
7. 시스템이 갱신된 이슈 상세 화면을 보여준다.

### Alternative Flows
- 2a. 배정 가능한 Dev가 없다
    - 2a1. 시스템이 배정 가능한 Dev가 없다는 메시지를 보여준다
    - 2a2. PL이 이전 화면으로 돌아간다
- 2b. 배정 가능한 Tester가 없다
    - 2b1. 시스템이 배정 가능한 Tester가 없다는 메시지를 보여준다
    - 2b2. PL이 이전 화면으로 돌아간다
- 5a. PL의 역할이 배정 권한을 가지지 않는다
    - 5a1. 시스템이 권한 거부 메시지를 보여준다
    - 5a2. 흐름이 종료된다
- 5b. 이슈 상태가 NEW가 아니어서 배정 전이가 불가능하다
    - 5b1. 시스템이 현재 상태에서는 배정할 수 없다는 메시지를 보여준다
    - 5b2. PL이 이전 화면으로 돌아간다
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
| CLOSED / RESOLVED | REOPENED | PL | assignee, verifier, fixer 초기화 | PL이 종료된 이슈를 재개 |

### Main Flow
1. 사용자가 상태 변경 버튼을 누른다
2. 시스템이 사용자의 역할과 이슈의 현재 상태에 따라 가능한 목표 상태 목록을 보여준다
3. 사용자가 목표 상태를 선택한다
4. 시스템이 상태 변경 사유 코멘트 입력을 요청한다 (include UC2/코멘트 추가)
5. 시스템이 권한과 전이 규칙을 확인한다 (include UC14/권한 검사)
6. 시스템이 이슈의 status를 갱신하고, 전이에 따른 자동 필드를 처리한다
    - ASSIGNED -> FIXED: fixer를 현재 Dev로 기록한다
    - FIXED -> RESOLVED: 추가 자동 필드 없이 상태만 변경한다
    - FIXED -> ASSIGNED: fixer를 초기화하고 기존 assignee에게 재배정한다
    - RESOLVED -> CLOSED: 추가 자동 필드 없이 상태를 최종 종료한다
    - CLOSED/RESOLVED -> REOPENED: assignee, verifier, fixer를 마지막으로 지정되었던 값으로 할당하여 상태를 Assigned와 동일하게 설정하여 이슈 작업을 재개한다
7. 시스템이 갱신된 이슈 상세 화면을 보여준다

### Alternative Flows
- 2a. 현재 상태에서 사용자의 역할로 수행 가능한 전이가 없다
    - 2a1. 시스템이 가능한 전이가 없다는 메시지를 보여준다
    - 2a2. 사용자가 이전 화면으로 돌아간다
- 5a. 사용자의 역할이 선택한 전이를 수행할 수 없다
    - 5a1. 시스템이 거부 사유와 가능한 전이 목록을 보여준다
    - 5a2. 사용자가 다시 선택한다 (단계 3으로 복귀)
- 5b. FIXED -> RESOLVED 또는 FIXED -> ASSIGNED 전이에서 사용자가 해당 이슈의 verifier가 아니다
    - 5b1. 시스템이 지정된 verifier만 검증할 수 있다는 메시지를 보여준다
    - 5b2. 사용자가 이전 화면으로 돌아간다
- 5c. ASSIGNED->FIXED 전이에서 사용자가 해당 이슈의 assignee가 아니다
    - 5c1. 시스템이 지정된 assignee만 수정 완료할 수 있다는 메시지를 보여준다
    - 5c2. 사용자가 이전 화면으로 돌아간다
- *. 사용자가 상태 변경을 취소한다
    - *1. 시스템이 입력 내용을 폐기하고 이슈 상세 화면으로 돌아간다

---

## 유스케이스 관계 요약

| 관계 | Base UC | 관련 UC | 의미 |
|---|---|---|---|
| <<include>> | UC1 (이슈 등록) | UC14 (권한 검사) | 등록 시 시스템이 사용자 권한을 검사한다 |
| <<include>> | UC5 (이슈 배정) | UC2 (코멘트 추가) | PL이 이슈를 배정할 때 시스템이 사유 코멘트를 강제한다 |
| <<include>> | UC5 (이슈 배정) | UC14 (권한 검사) | 배정 시 시스템이 권한과 전이 규칙을 검사한다 |
| <<include>> | UC6 (이슈 상태 변경) | UC2 (코멘트 추가) | 상태를 변경할 때 시스템이 사유 코멘트를 강제한다 |
| <<include>> | UC6 (이슈 상태 변경) | UC14 (권한 검사) | 상태 전이 시 시스템이 권한과 전이 규칙을 검사한다 |
| <<extend>> | UC1 (이슈 등록) | UC2 (코멘트 추가) | Auth User가 등록 직후 코멘트를 남기고 싶을 때 추가한다 |
| <<extend>> | UC4 (이슈 상세 조회) | UC2 (코멘트 추가) | Auth User가 상세 화면에서 코멘트를 남기고 싶을 때 추가한다 |
| <<extend>> | UC4 (이슈 상세 조회) | UC15 (이슈 수정) | Auth User가 상세 화면에서 이슈를 수정하고 싶을 때 추가한다 |
| <<extend>> | UC5 (이슈 배정) | UC8 (Assignee 자동 추천) | PL이 NEW 이슈에 assignee를 지정하려는 시점에 시스템이 후보를 추천한다 |

---

## 변경 이력
- 2026-04-29 _초안 작성_
- 2026-05-01 _텍스트 문법 수정_
- 2026-05-02 _가정 사항 변경을 반영하여 재작성 : 이슈 등록 actor를 Tester로 한정, UC2를 코멘트 추가로 변경, 등록&해결 전이 시 코멘트 정책 반영_
- 2026-05-02 _명세 정합성 보강 : 오타·단계 번호 정정, UC4 Extensions에 UC5 호출 명시, UC6 Trigger·Extension Point 명확화, UC12 include 관계 요약 표에 추가, Extension Points 항목 추가, <<include>>/<<extend>> 표기 통일_
- 2026-05-05 _UC 구조 변경 : 기존 UC5(배정 및 상태 변경)를 UC5(이슈 배정)와 UC6(이슈 상태 변경)로 분리, 기존 UC6(Assignee 추천)을 UC8로 번호 재배정, UC14(권한 검사)로 include 참조 일괄 수정, UC1 Primary Actor를 Auth User로 확장, UC2 Actor 직접 연결 제거 및 subfunction으로 정리, UC4에 UC15(이슈 수정) extension point 추가, UC5에 verifier 지정 흐름 반영, UC6에 역할별 허용 전이 요약, 역전이(FIXED->ASSIGNED) & RESOLVED→CLOSED 전이 추가, 유스케이스 관계 요약 전체 갱신_