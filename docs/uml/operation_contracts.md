# Operation Contracts

> 본 문서는 이슈 관리 시스템(ITS)의 시스템 시퀀스 다이어그램(SSD)에서 도출된 시스템 연산에 대한 Operation Contract를 정리한다.
> 도메인 모델, 유스케이스 명세, 기본 가정 문서를 기반으로 작성되었다.

---

## 1. UC1 — Register Issue (이슈 등록)

### Contract CO1: registerIssue()

| 항목 | 내용 |
|---|---|
| **Operation** | registerIssue() |
| **Cross References** | UC1 (Register Issue) |
| **Preconditions** | Auth User가 시스템에 로그인한 상태이다. 대상 프로젝트가 시스템에 존재한다. enterIssueDetails(title, description, priority)를 통해 title, description, priority 값이 시스템에 전달된 상태이다. |
| **Postconditions** | 아래 참조 |

**Postconditions (상세):**

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

## 2. UC2 — Add Comment (코멘트 추가)

### Contract CO2: submitComment(issueID, content)

| 항목 | 내용 |
|---|---|
| **Operation** | submitComment(issueID, content) |
| **Cross References** | UC2 (Add Comment) — UC1(이슈 등록), UC4(이슈 상세 조회), UC5(이슈 배정)에서 extend로 호출되고, UC6(이슈 상태 변경)에서 include로 호출됨 |
| **Preconditions** | 호출 UC가 진행 중이며, `issueID`에 해당하는 Issue 인스턴스가 시스템에 존재한다. Auth User가 시스템에 로그인한 상태이다. |
| **Postconditions** | 아래 참조 |

**Postconditions (상세):**

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

## 작성 근거

- **SSD 기준**: UC1의 SSD에서 `enterIssueDetails(title, description, priority)` → `registerIssue()` 순서로 시스템 연산이 도출된다. `enterIssueDetails`는 입력 데이터를 전달하는 연산이고, `registerIssue()`가 실제 도메인 상태 변화를 일으키므로 Contract는 `registerIssue()`에 대해 작성한다. 단, precondition에서 enterIssueDetails를 통해 값이 전달된 상태임을 명시한다.
- **UC2의 SSD 기준**: `submitComment(issueID, content)` 연산이 도메인 상태 변화를 일으키는 핵심 연산이다. SSD의 `alt` 분기(content가 빈 경우의 취소/재입력)는 도메인 객체 상태 변화가 없으므로 Contract에서는 정상 흐름의 사후조건만 기술한다.
- **도메인 모델 기준**: Comment와 IssueHistory는 별도 개념이다. Comment는 사용자의 자발적 의견이고, IssueHistory는 시스템이 모든 변경 사건을 자동으로 추적하는 기록이다. 따라서 코멘트 추가 시에도 IssueHistory(`COMMENTED`)가 함께 생성된다.
- **ActionType 열거값**: 도메인 규칙 노트에 정의된 `CREATED`, `COMMENTED` 등을 사용한다.
