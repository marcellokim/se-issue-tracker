# Operation Contracts

> 본 문서는 이슈 관리 시스템(ITS)의 시스템 시퀀스 다이어그램(SSD)에서 도출된 시스템 연산에 대한 Operation Contract를 정리한다.
> 도메인 모델, 유스케이스 명세, 기본 가정 문서를 기반으로 작성되었으며, Larman의 Operation Contract 형식을 따른다.
> 사후조건은 선언적(declarative) · 과거형으로 기술하고, 각 항목에 유형 태그를 명시한다.

---

## Contract CO1: registerIssue()

| 항목 | 내용 |
|---|---|
| **Operation** | registerIssue() |
| **Cross References** | UC1 (Register Issue) |
| **Preconditions** | Auth User가 시스템에 로그인한 상태이다. 대상 프로젝트가 시스템에 존재한다. enterIssueDetails(title, description, priority)를 통해 title, description, priority 값이 시스템에 전달된 상태이다. |
| **Postconditions** | 아래 참조 |

### Postconditions (상세)

- Issue 인스턴스 `i`가 새로 생성되었다 *(instance creation)*
- `i.title`이 전달된 `title`로 설정되었다 *(attribute modification)*
- `i.description`이 전달된 `description`으로 설정되었다 *(attribute modification)*
- `i.priority`가 Auth User가 선택한 `priority`로 설정되었다. Auth User가 priority를 선택하지 않은 경우 `MAJOR`로 설정되었다 *(attribute modification)*
- `i.status`가 `NEW`로 설정되었다 *(attribute modification)*
- `i.reportedDate`가 현재 시각으로 설정되었다 *(attribute modification)*
- `i`가 현재 로그인한 Auth User와 연관되었다 — 도메인 모델의 User `reports` Issue 관계에 따라 reporter가 현재 Auth User로 기록되었다 *(association formed)*
- `i`가 대상 Project의 이슈 목록에 추가되었다 — 도메인 모델의 Project `Contains` Issue 관계 *(association formed)*
- IssueHistory 인스턴스 `h`가 새로 생성되었다 *(instance creation)*
- `h.action`이 `CREATED`로 설정되었다 *(attribute modification)*
- `h.changedDate`가 현재 시각으로 설정되었다 *(attribute modification)*
- `h`가 해당 Issue의 이력 목록에 추가되었다 — Issue `Logs` IssueHistory 관계 *(association formed)*

---

## Contract CO2: submitComment(issueID, content)

| 항목 | 내용 |
|---|---|
| **Operation** | submitComment(issueID, content) |
| **Cross References** | UC2 (Add Comment) — UC1(이슈 등록), UC4(이슈 상세 조회), UC5(이슈 배정)에서 extend로 호출되고, UC6(이슈 상태 변경)에서 include로 호출됨 |
| **Preconditions** | 호출 UC가 진행 중이며, `issueID`에 해당하는 Issue 인스턴스가 시스템에 존재한다. Auth User가 시스템에 로그인한 상태이다. |
| **Postconditions** | 아래 참조 |

### Postconditions (상세)

- Comment 인스턴스 `c`가 새로 생성되었다 *(instance creation)*
- `c.content`가 파라미터 `content`로 설정되었다 *(attribute modification)*
- `c.createdAt`이 현재 시각으로 설정되었다 *(attribute modification)*
- `c`가 현재 로그인한 Auth User(작성자)와 연관되었다 — 도메인 모델의 User `writes` Comment 관계 *(association formed)*
- `c`가 `issueID`에 해당하는 Issue의 코멘트 목록에 추가되었다 — 도메인 모델의 Issue `has` Comment 관계 *(association formed)*
- IssueHistory 인스턴스 `h`가 새로 생성되었다 *(instance creation)*
- `h.action`이 `COMMENTED`로 설정되었다 *(attribute modification)*
- `h.changedDate`가 현재 시각으로, `h.message`가 `content`로 설정되었다 *(attribute modification)*
- `h`가 해당 Issue의 이력 목록에 추가되었다 — Issue `Logs` IssueHistory 관계 *(association formed)*

---

## Contract CO3: confirmStateChange(issueId, targetState)

| 항목 | 내용 |
|---|---|
| **Operation** | confirmStateChange(issueId, targetState) |
| **Cross References** | UC6 (Change Issue State) |
| **Preconditions** | Auth User가 시스템에 로그인한 상태이다. `issueId`에 해당하는 Issue가 시스템에 존재한다. Issue의 현재 상태에서 `targetState`로의 전이가 허용된다. Auth User의 역할이 해당 전이를 수행할 권한을 가진다. submitComment(issueID, content)를 통해 상태 변경 사유 코멘트가 입력된 상태이다. |
| **Postconditions** | 아래 참조 |

### Postconditions — 공통 (모든 전이)

- Issue의 `status`가 `targetState`로 변경되었다 *(attribute modification)*
- IssueHistory 인스턴스 `h`가 새로 생성되었다 *(instance creation)*
- `h.action`이 `STATUS_CHANGED`로 설정되었다 *(attribute modification)*
- `h.previousValue`가 변경 전 상태로, `h.newValue`가 `targetState`로 설정되었다 *(attribute modification)*
- `h.changedDate`가 현재 시각으로 설정되었다 *(attribute modification)*
- `h`가 해당 Issue의 이력 목록에 추가되었다 — Issue `Logs` IssueHistory 관계 *(association formed)*

### Postconditions — 전이별 조건부 사후조건

| 전이 | 수행 역할 | 추가 사후조건 | 유형 |
|---|---|---|---|
| ASSIGNED → FIXED | Dev (assignee 본인) | `issue.fixer`가 현재 Dev로 설정되었다 | *(attribute modification)* |
| FIXED → RESOLVED | Tester (verifier 본인) | 추가 필드 변경 없음 | — |
| FIXED → ASSIGNED | Tester (verifier 본인) | `issue.fixer`가 초기화되었다 | *(attribute modification)* |
| RESOLVED → CLOSED | PL | 추가 필드 변경 없음 | — |
| CLOSED/RESOLVED → REOPENED | PL | `issue.assignee`, `issue.verifier`, `issue.fixer`가 마지막 지정 값으로 복원되었다 | *(attribute modification)* |

---

## 작성 근거

### Contract 선택 기준

| Contract | 선택 이유 |
|---|---|
| CO1 `registerIssue()` | 인스턴스 생성 + 속성 설정 + 연관 형성이 모두 포함되어 Operation Contract의 세 가지 기술 유형을 골고루 보여준다. |
| CO2 `submitComment()` | UC2가 독립 SSD를 가지며, 여러 UC에서 호출되는 공통 연산이다. Comment와 IssueHistory가 동시에 생성되는 복합적 사후조건을 보여준다. |
| CO3 `confirmStateChange()` | 조건부 사후조건이 있어서, 하나의 오퍼레이션이 맥락(전이 종류)에 따라 다른 결과를 낳는 구조를 보여준다. SSD에서는 대표 시나리오(ASSIGNED→FIXED) 하나만 그렸지만, Operation Contract에서 모든 전이의 차이를 사전/사후조건으로 커버한다. |

### SSD와의 연결

- **CO1**: UC1 SSD에서 `enterIssueDetails(title, description, priority)` → `registerIssue()` 순서로 도출된다. `enterIssueDetails`는 입력 데이터를 전달하는 연산이고, `registerIssue()`가 실제 도메인 상태 변화를 일으키므로 Contract는 `registerIssue()`에 대해 작성한다. 단, precondition에서 enterIssueDetails를 통해 값이 전달된 상태임을 명시한다.
- **CO2**: UC2 SSD에서 `submitComment(issueID, content)` 연산이 도메인 상태 변화를 일으키는 핵심 연산이다. SSD의 `alt` 분기(content가 빈 경우의 취소/재입력)는 도메인 객체 상태 변화가 없으므로 Contract에서는 정상 흐름의 사후조건만 기술한다.
- **CO3**: UC6 SSD에서 `requestStateChange(issueId)` → `selectTargetState(targetState)` → `ref UC2` → `confirmStateChange(issueId, targetState)` 순서로 도출된다. 앞선 연산들은 조회/선택이고, `confirmStateChange()`가 실제 상태 변경을 일으키므로 Contract는 이 연산에 대해 작성한다.

### 도메인 모델 참조

- Comment와 IssueHistory는 별도 개념이다. Comment는 사용자의 자발적 의견이고, IssueHistory는 시스템이 모든 변경 사건을 자동으로 추적하는 기록이다. 따라서 코멘트 추가 시에도 IssueHistory(`COMMENTED`)가 함께 생성된다.
- ActionType 열거값: 도메인 규칙 노트에 정의된 `CREATED`, `COMMENTED`, `STATUS_CHANGED` 등을 사용한다.

---

## 변경 이력

- 2026-05-09 _초안 작성 (CO1, CO2)_
- 2026-05-09 _CO3 추가 및 통합 정리_