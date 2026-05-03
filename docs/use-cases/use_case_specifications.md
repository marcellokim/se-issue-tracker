# 유스케이스 명세 (Use Case Specifications)

> 이 문서는 이슈 관리 시스템의 include + extend 관계를 포함한 유스케이스 6개에 대한 Fully Dressed 명세입니다. UC12(권한 검사)는 다른 UC가 «include»하는 보조 유스케이스이며, 본 문서에서는 명세를 작성하지 않고 다이어그램과 관계 요약 표에서만 다룹니다.

---

## 유스케이스 종류
- **UC1 : Register Issue (이슈 등록)**
- **UC2 : Add Comment (코멘트 추가)**
- **UC3 : Search Issues (이슈 검색)**
- **UC4 : View Issue Detail (이슈 상세 조회)**
- **UC5 : Change Issue State (이슈 배정 및 상태 변경)**
- **UC6 : Recommend Assignee (Assignee 자동 추천)**

---

## UC1 : Register Issue (이슈 등록)

| 항목 | 내용 |
|---|---|
| ID | UC1 |
| 이름 | Register Issue (이슈 등록) |
| 범위 | Issue Tracking System |
| 수준 | User goal |
| Primary Actor | Tester |
| Stakeholders & Interests | - Tester: 발견한 결함을 시스템에 누락 없이 기록하고 싶다<br>- PL: 등록된 이슈를 추적할 수 있기를 원한다 |
| Preconditions | - Tester가 시스템에 로그인한 상태이다<br>- 대상 프로젝트가 시스템에 존재한다 |
| Postconditions  | - 시스템이 새 이슈를 저장한다<br>- 시스템이 reporter를 현재 tester로, reportedDate를 현재 시각으로, status를 NEW로 설정한다<br>- Tester가 priority를 선택하지 않았다면 시스템이 MAJOR를 기본값으로 부여한다 |
| Trigger | Tester가 메뉴에서 "Register Issue"를 선택한다 |
| Extension Points | EP1: 단계 8 직후 (UC2 Add Comment) |

### Main Success Scenario
1. Tester가 이슈 등록 화면을 연다
2. 시스템이 이슈 등록 폼을 보여준다
3. Tester가 Title, Description, Priority를 입력 및 선택한다
4. Tester가 저장 및 등록 버튼을 누른다
5. 시스템이 권한을 확인한다 «include»UC12
6. 시스템이 입력값을 확인한다
7. 시스템이 이슈를 저장한다
8. 시스템이 등록 완료 화면을 보여준다

### Extensions (대안 흐름)
- 5a. 사용자가 이슈 등록 권한을 가지지 않는다
    - 5a1. 시스템이 권한 거부 메시지를 보여준다
    - 5a2. 흐름이 종료된다
- 6a. Title 또는 Description이 비어있다
    - 6a1. 시스템이 누락 항목을 표시한다
    - 6a2. Tester가 다시 입력한다 (단계 3으로 복귀)
- *. Tester가 등록을 취소한다
    - *1. 시스템이 입력 내용을 폐기하고 이전 화면으로 돌아간다

---

## UC2. Add Comment (코멘트 추가)

| 항목 | 내용 |
|---|---|
| ID | UC2 |
| 이름 | Add Comment (코멘트 추가) |
| 범위 | Issue Tracking System |
| 수준 | Subfunction |
| Primary Actor | Authenticated User : UC1, UC4의 extension point에서 시작 시<br>없음 : UC5에서 include로 호출 시 |
| Stakeholders & Interests | - 사용자: 이슈에 자신의 의견과 진행 상황 등을 기록하고 싶다<br>- 조직: 이슈 배정 및 상태 변경 시점의 사유가 누락 없이 남기를 원한다 |
| Preconditions | - 호출 UC가 진행 중이고, 대상 이슈가 화면에 있다<br>- 사용자가 시스템에 로그인한 상태이다 |
| Postconditions  | - 시스템이 새 코멘트를 이슈의 코멘트 history에 추가한다<br>- 시스템이 author를 현재 사용자로, 코멘트 작성 시간을 현재 시각으로 기록한다 |
| Trigger | - UC1, UC4의 extension point에서 사용자가 코멘트 추가를 선택 («extend») <br> - UC5의 코멘트 입력 단계에서 자동 시작 («include») |

### Main Success Scenario
1. 시스템이 코멘트 입력 영역을 보여준다
2. 사용자가 코멘트를 입력한다
3. 사용자가 코멘트 업로드 버튼을 누른다
4. 시스템이 본문이 비어있지 않은지 확인한다
5. 시스템이 코멘트를 이슈에 추가한다
6. 시스템이 호출 UC로 돌아간다

### Extensions
- 4a. 코멘트 내용이 비어있다
    - 4a-1. UC1 또는 UC4에서 호출된 경우
        - 시스템이 입력을 취소한 것으로 간주하고 호출 UC로 돌아간다
    - 4a-2. UC5에서 호출된 경우
        - 시스템이 코멘트 내용을 입력하라는 메시지를 보여준다
        - 사용자가 코멘트 내용을 입력할 때까지 호출 UC가 다음 단계로 진행하지 않는다

---

## UC3. Search Issues (이슈 검색)

| 항목 | 내용 |
|---|---|
| ID | UC3 |
| 이름 | Search Issues (이슈 검색) |
| 범위 | Issue Tracking System |
| 수준 | User goal |
| Primary Actor | Authenticated User (Admin, PL, Developer, Tester) |
| Stakeholders & Interests | - 사용자: 자신의 관심사에 맞는 이슈를 빠르게 찾고 싶다<br>- PL: 프로젝트 이슈들 종류와 상태를 찾고 싶다 |
| Preconditions | 사용자가 시스템에 로그인한 상태이다 |
| Postconditions  | 시스템이 검색 조건에 맞는 이슈 목록을 보여준다 |
| Trigger | 사용자가 "Search Issues" 메뉴를 선택한다 |

### Main Success Scenario
1. 사용자가 검색 화면을 연다
2. 시스템이 검색 조건 입력 영역을 보여준다
3. 사용자가 검색 조건을 입력한다
4. 사용자가 검색 버튼을 누른다
5. 시스템이 조건에 맞는 이슈를 찾는다
6. 시스템이 결과 목록을 reportedDate 내림차순으로 보여준다

### Extensions
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
| Primary Actor | Authenticated User |
| Stakeholders & Interests | - 사용자: 특정 이슈에 대한 모든 정보를 확인하고 싶다<br>- Dev: 자신에게 배정된 이슈의 내용을 파악하고 싶다 |
| Preconditions | - 사용자가 시스템에 로그인한 상태이다<br>- 대상 이슈가 시스템에 존재한다 |
| Postconditions | 시스템이 이슈의 모든 필드와 코멘트 history를 화면에 보여준다 |
| Trigger | 사용자가 이슈 목록 또는 검색 결과에서 특정 이슈를 선택한다 |
| Extension Points | EP1: 단계 6 (UC2 Add Comment) |

### Main Success Scenario
1. 사용자가 이슈를 선택한다
2. 시스템이 이슈를 불러온다
3. 시스템이 이슈의 모든 필드를 보여준다
4. 시스템이 코멘트 history를 등록한 시간 순서대로 보여준다
5. 시스템이 사용자 역할에 따라 가능한 액션 버튼을 보여준다
6. 사용자가 화면을 확인한다

### Extensions
- 2a. 이슈가 존재하지 않는다
    - 2a1. 시스템이 이슈를 찾을 수 없다는 메시지를 보여준다
    - 2a2. 사용자가 이전 화면으로 돌아간다
- 5a. 사용자가 이슈 상태 변경 액션을 선택한다
    - 5a1. UC4가 종료되고 UC5가 시작된다

---

## UC5. Change Issue State (이슈 배정 및 상태 변경)

| 항목 | 내용 |
|---|---|
| ID | UC5 |
| 이름 | Change Issue State (이슈 배정 및 상태 변경) |
| 범위 | Issue Tracking System |
| 수준 | User goal |
| Primary Actor | PL, Developer, Tester |
| Stakeholders & Interests | - PL: 적절한 dev에게 이슈를 배정하고 싶다<br>- Dev: 자신의 이슈 해결 작업 진행을 시스템에 반영하고 싶다<br>- Tester: 수정된 이슈가 실제로 해결되었는지 확인하고 싶다 |
| Preconditions | - 사용자가 시스템에 로그인한 상태이다<br>- 대상 이슈가 시스템에 존재한다<br>- 사용자의 역할이 요청한 전이를 수행할 수 있다 |
| Postconditions  | - 시스템이 이슈의 status를 목표 상태로 변경한다<br>- 시스템이 전이에 따른 자동 필드(assignee, fixer 등)를 갱신한다<br>- 시스템이 이슈 상태 변경 이유 코멘트를 코멘트 history에 추가한다 |
| Trigger | 사용자가 상세 화면에서 상태 변경 액션을 선택한다 |
| Extension Points | EP1: 단계 3 직전 (NEW → ASSIGNED 전이 시 UC6 Recommend Assignee) |

### Main Success Scenario
1. 사용자가 상태 변경 버튼을 누른다
2. 시스템이 사용자의 역할에 따라 가능한 목표 상태 목록을 보여준다
3. 사용자가 목표 상태를 선택한다
    - NEW -> ASSIGNED 전이: PL이 assignee를 선택한다
    - ASSIGNED -> FIXED 전이: Dev가 추가 정보를 입력한다
    - FIXED -> RESOLVED 전이: Tester가 추가 정보를 입력한다
4. 시스템이 사유 코멘트를 받는다 «include»UC2
5. 시스템이 권한과 전이 규칙을 확인한다 «include»UC12
6. 시스템이 전이에 따른 자동 필드를 채운다
    - NEW -> ASSIGNED: assignee를 PL이 선택한 dev로 채운다
    - ASSIGNED -> FIXED: fixer를 현재 Dev로 채운다
    - FIXED -> RESOLVED: 자동으로 채우는 필드는 없다
7. 시스템이 변경 사항을 저장한다
8. 시스템이 변경 이력을 시스템 메시지로 코멘트 history에 남긴다
9. 시스템이 갱신된 상세 화면을 보여준다

### Extensions
- 4a. 사용자가 사유 코멘트를 비워두고 저장한다
    - 4a1. UC2의 대안 흐름 4a-2로 진입하여 코멘트 입력을 강제한다
    - 4a2. 코멘트가 입력될 때까지 단계 5로 진행하지 못한다
- 5a. 사용자의 역할이 요청한 전이를 수행할 수 없다
    - 5a1. 시스템이 거부 사유와 가능한 전이 목록을 보여준다
    - 5a2. 사용자가 다시 입력한다 (단계 3으로 복귀)
- *. 사용자가 변경을 취소한다
    - *1. 시스템이 입력 내용을 폐기하고 상세 화면으로 돌아간다

---

## UC6. Recommend Assignee (Assignee 자동 추천)

| 항목 | 내용 |
|---|---|
| ID | UC6 |
| 이름 | Recommend Assignee (Assignee 자동 추천) |
| 범위 | Issue Tracking System |
| 수준 | Subfunction |
| Primary Actor | PL |
| Stakeholders & Interests | - PL: 이슈 내용에 가장 적합한 dev를 빠르게 찾고 싶다<br>- Dev: 자신의 전문성에 맞는 이슈를 받고 싶다 |
| Preconditions | - UC5의 NEW → ASSIGNED 전이가 선택된 시점이다<br>- 대상 프로젝트에 RESOLVED 또는 CLOSED 상태의 과거 이슈가 1건 이상 있다 |
| Postconditions  | 시스템이 추천 후보 dev를 최대 3명까지 적합도 순서로 PL에게 보여준다 |
| Trigger | UC5의 단계 3 직전 extension point가 충족된다 (NEW 이슈에 대한 ASSIGNED 전이) |

### Main Success Scenario
1. 시스템이 UC5의 extension point에서 추천 흐름을 시작한다
2. 시스템이 대상 이슈의 Title과 Description에서 키워드를 뽑는다
3. 시스템이 RESOLVED 또는 CLOSED 상태의 과거 이슈들을 가져온다
4. 시스템이 과거 이슈와 현재 이슈의 유사도를 계산한다
5. 시스템이 유사한 과거 이슈들의 fixer를 모아 점수를 매긴다
6. 시스템이 점수 상위 3명을 PL에게 보여준다
7. PL이 추천 후보 중 한 명을 선택한다
8. 시스템이 UC5로 돌아간다

### Extensions
- 3a. 과거 이슈가 한 건도 없다
    - 3a1. 시스템이 추천에 사용할 데이터가 부족하다는 메시지를 보여준다
    - 3a2. UC5로 복귀한다
- 5a. 유사한 과거 이슈가 없다
    - 5a1. 시스템이 유사한 이슈가 없다는 메시지를 보여준다
    - 5a2. UC5로 복귀한다
- 6a. 후보가 3명보다 적다
    - 6a1. 시스템이 매칭된 후보만 보여준다 (1-2명)

---

## 유스케이스 관계 요약

| 관계 | Base UC | 관련 UC | 의미 |
|---|---|---|---|
| «include» | UC1 (이슈 등록) | UC12 (권한 검사) | 등록 시 시스템이 사용자 권한을 검사한다 |
| «include» | UC5 (배정 및 상태 변경) | UC2 (코멘트 추가) | 사용자가 이슈를 배정하거나 상태를 바꿀 때 시스템이 사유 코멘트를 강제한다 |
| «include» | UC5 (배정 및 상태 변경) | UC12 (권한 검사) | 상태 전이 시 시스템이 권한과 전이 규칙을 검사한다 |
| «extend» | UC1 (이슈 등록) | UC2 (코멘트 추가) | Tester가 등록 직후 코멘트를 남기고 싶을 때 추가한다 |
| «extend» | UC4 (이슈 상세 조회) | UC2 (코멘트 추가) | 사용자가 상세 화면에서 코멘트를 남기고 싶을 때 추가한다 |
| «extend» | UC5 (배정 및 상태 변경) | UC6 (Assignee 추천) | PL이 NEW 이슈에 assignee를 지정하려는 시점에 시스템이 후보를 추천한다 |

---

## 변경 이력
- 2026-04-29 _초안 작성_
- 2026-05-01 _텍스트 문법 수정_
- 2026-05-02 _가정 사항 변경을 반영하여 재작성 : 이슈 등록 actor를 Tester로 한정, UC2를 코멘트 추가로 변경, 등록&해결 전이 시 코멘트 정책 반영_
- 2026-05-02 _명세 정합성 보강 : 오타·단계 번호 정정, UC4 Extensions에 UC5 호출 명시, UC6 Trigger·Extension Point 명확화, UC12 include 관계 요약 표에 추가, Extension Points 항목 추가, «include»/«extend» 표기 통일_