# Operation Contracts

이 문서는 UC 명세에서 Fully Dressed로 다룬 핵심 시나리오와 SSD에서 결과를 그림만으로 이해하기 어려운 system operation을 대상으로 작성한다. 보고서 본문에서는 `OC-01 Register Issue`, `OC-07 Resolve Fixed Issue`, `OC-14 Add Dependency`를 대표 Operation Contract로 사용한다. 세 operation은 각각 이슈 생성, 상태 전이, 이슈 의존성 관계 생성을 보여주기 때문에 SSD만으로는 부족한 도메인 객체의 생성, association 변화, 속성값 변화를 설명하기에 적합하다.

나머지 OC는 구현 정책과 설계 검토를 위한 reference로 유지한다.

작성 기준:
- Reference는 대응 Use Case를 기준으로 적는다.
- Preconditions는 UC 명세, SSD, 기본 가정의 권한/상태 정책을 함께 반영한다.
- Postconditions는 필요한 경우에만 instance 생성/삭제, association 형성/붕괴, 속성값 변화를 명시한다.
- `IssueHistory`가 생성되는 경우 해당 `Issue`와 `User`의 기록 association도 함께 형성된 것으로 본다.
- 모든 새 `IssueHistory` instance의 `changedDate`는 현재 시각으로 설정된다.

---

## OC-01. Register Issue

### 1. Operation 이름 및 파라미터

`registerIssue(projectId, title, description, priority)`

### 2. Reference

Use Case: UC1 Register Issue

### 3. Preconditions

- 현재 사용자는 `Auth User`로 로그인되어 있다.
- 이슈를 등록할 대상 `Project`가 존재하고 선택되어 있다.
- 현재 사용자는 선택된 `Project`의 active member이다.
- 현재 사용자는 선택된 `Project`에 이슈를 등록할 권한을 가진다.
- `title`과 `description`은 비어 있지 않다.
- 같은 프로젝트 안에 동일한 title을 가진 다른 `Issue`가 존재하지 않는다.

### 4. Postconditions

- Instance 생성
  - 새로운 `Issue` instance가 생성되었다.
  - `actionType=CREATED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - 새 `Issue`는 선택된 `Project`에 속하게 되었다.
  - 현재 `User`와 새 `Issue` 사이에 `reports` association이 형성되었다.
  - 새 `Issue`와 새 `IssueHistory` 사이에 `logs` association이 형성되었다.
  - 현재 `User`와 새 `IssueHistory` 사이에 `changes` association이 형성되었다.
- 속성값 변화
  - `Issue.title`은 `title`로 설정되었다.
  - `Issue.description`은 `description`으로 설정되었다.
  - `Issue.priority`는 `priority`로 설정되었다. 단, 값이 생략된 경우 `MAJOR`로 설정되었다.
  - `Issue.status`는 `NEW`로 설정되었다.
  - `Issue.reportedDate`는 현재 시각으로 설정되었다.

---

## OC-02. Add Comment

### 1. Operation 이름 및 파라미터

`addComment(issueId, content)`

### 2. Reference

Use Case: UC2 Add Comment

### 3. Preconditions

- 현재 사용자는 `Auth User`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- `issueId`가 대상 `Issue`를 식별한다.
- `content`는 비어 있지 않다.
- UC1/UC4/UC5의 extend 흐름 또는 UC6의 include 흐름에서 호출될 수 있다.

### 4. Postconditions

- Instance 생성
  - 새로운 `Comment` instance가 생성되었다.
  - `actionType=COMMENTED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - 대상 `Issue`와 새 `Comment` 사이에 `has` association이 형성되었다.
  - 현재 `User`와 새 `Comment` 사이에 `writes` association이 형성되었다.
  - 대상 `Issue`와 새 `IssueHistory` 사이에 `logs` association이 형성되었다.
  - 현재 `User`와 새 `IssueHistory` 사이에 `changes` association이 형성되었다.
- 속성값 변화
  - `Comment.content`는 `content`로 설정되었다.
  - `Comment.createdDate`는 현재 시각으로 설정되었다.
  - `IssueHistory.message`는 코멘트 등록 메시지로 설정되었다.

---

## OC-03. Assign Issue - NEW to ASSIGNED

### 1. Operation 이름 및 파라미터

`assignIssue(issueId, assigneeId, verifierId)`

### 2. Reference

Use Case: UC5 Assign / Update Issue Assignment

### 3. Preconditions

- 현재 사용자는 `PL`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- `Issue.status`는 `NEW`이다.
- 현재 사용자는 대상 `Issue`가 속한 `Project`의 active PL이다.
- `assigneeId`는 배정 가능한 `DEV`를 식별한다.
- `verifierId`는 배정 가능한 `TESTER`를 식별한다.
- UC8 Recommend Assignment Candidates가 대상 이슈 상태에 맞는 후보를 제공한 상태이다.
- 현재 사용자는 이슈를 배정할 권한을 가진다.

### 4. Postconditions

- Instance 생성
  - `actionType=ASSIGNMENT_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
  - `actionType=STATUS_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - 선택된 `DEV`와 대상 `Issue` 사이에 `assigned to` association이 형성되었다.
  - 선택된 `TESTER`와 대상 `Issue` 사이에 `verifies` association이 형성되었다.
  - 대상 `Issue`와 두 개의 새 `IssueHistory` 사이에 각각 `logs` association이 형성되었다.
  - 현재 `PL`과 두 개의 새 `IssueHistory` 사이에 각각 `changes` association이 형성되었다.
- 속성값 변화
  - `Issue.status`는 `NEW`에서 `ASSIGNED`로 변경되었다.
  - `ASSIGNMENT_CHANGED` history에는 새 assignee와 verifier 값이 기록되었다.
  - `STATUS_CHANGED` history에는 `previousValue=NEW`, `newValue=ASSIGNED`가 기록되었다.

---

## OC-04. Reassign Issue - ASSIGNED to ASSIGNED

### 1. Operation 이름 및 파라미터

`reassignIssue(issueId, assigneeId)`

### 2. Reference

Use Case: UC5 Assign / Update Issue Assignment

### 3. Preconditions

- 현재 사용자는 `PL`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- `Issue.status`는 `ASSIGNED`이다.
- 현재 사용자는 대상 `Issue`가 속한 `Project`의 active PL이다.
- `assigneeId`는 배정 가능한 `DEV`를 식별한다.
- 현재 사용자는 이슈 배정 정보를 변경할 권한을 가진다.
- UC8 Recommend Assignment Candidates가 `DEV` assignee 후보를 제공한 상태이다.

### 4. Postconditions

- Instance 생성
  - `actionType=ASSIGNMENT_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - 선택된 `DEV`와 대상 `Issue` 사이에 `assigned to` association이 형성되었다.
  - 대상 `Issue`와 새 `IssueHistory` 사이에 `logs` association이 형성되었다.
  - 현재 `PL`과 새 `IssueHistory` 사이에 `changes` association이 형성되었다.
- Association 붕괴
  - 기존 assignee가 존재했다면, 기존 assignee `DEV`와 대상 `Issue` 사이의 `assigned to` association은 제거되었다.
- 속성값 변화
  - `Issue.status`는 `ASSIGNED`로 유지되었다.
  - `ASSIGNMENT_CHANGED` history에는 이전 assignee와 새 assignee가 기록되었다.
  - 이 operation은 assignee만 변경했으며, 기존 `verifies`, `fixes`, `resolves` association은 변경되지 않았다.

---

## OC-05. Change Verifier - FIXED to FIXED

### 1. Operation 이름 및 파라미터

`changeVerifier(issueId, verifierId)`

### 2. Reference

Use Case: UC5 Assign / Update Issue Assignment

### 3. Preconditions

- 현재 사용자는 `PL`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- `Issue.status`는 `FIXED`이다.
- 현재 사용자는 대상 `Issue`가 속한 `Project`의 active PL이다.
- `verifierId`는 배정 가능한 `TESTER`를 식별한다.
- 현재 사용자는 이슈 배정 정보를 변경할 권한을 가진다.
- UC8 Recommend Assignment Candidates가 `TESTER` verifier 후보를 제공한 상태이다.

### 4. Postconditions

- Instance 생성
  - `actionType=ASSIGNMENT_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - 선택된 `TESTER`와 대상 `Issue` 사이에 `verifies` association이 형성되었다.
  - 대상 `Issue`와 새 `IssueHistory` 사이에 `logs` association이 형성되었다.
  - 현재 `PL`과 새 `IssueHistory` 사이에 `changes` association이 형성되었다.
- Association 붕괴
  - 기존 verifier가 존재했다면, 기존 verifier `TESTER`와 대상 `Issue` 사이의 `verifies` association은 제거되었다.
- 속성값 변화
  - `Issue.status`는 `FIXED`로 유지되었다.
  - `ASSIGNMENT_CHANGED` history에는 이전 verifier와 새 verifier가 기록되었다.
  - 기존 `assigned to`, `fixes`, `resolves` association은 변경되지 않았다.

---

## OC-06. Mark Issue as Fixed

### 1. Operation 이름 및 파라미터

`changeStatus(issueId, targetStatus=FIXED, comment)`

### 2. Reference

Use Case: UC6 Change Issue State

### 3. Preconditions

- 현재 사용자는 `DEV`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- 현재 사용자는 대상 `Issue`의 현재 assignee이다.
- 현재 사용자는 대상 `Issue`가 속한 `Project`의 active member이다.
- `Issue.status`는 `ASSIGNED`이다.
- 현재 사용자는 `ASSIGNED -> FIXED` 전이를 수행할 권한을 가진다.
- UC6은 UC2 Add Comment를 include하므로 `comment`는 비어 있지 않다.

### 4. Postconditions

- Instance 생성
  - 상태 변경 사유를 저장하기 위한 새로운 `Comment` instance가 생성되었다.
  - `actionType=COMMENTED`인 새로운 `IssueHistory` instance가 생성되었다.
  - `actionType=STATUS_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - 대상 `Issue`와 새 `Comment` 사이에 `has` association이 형성되었다.
  - 현재 `DEV`와 새 `Comment` 사이에 `writes` association이 형성되었다.
  - 현재 `DEV`와 대상 `Issue` 사이에 새로운 `fixes` association이 형성되었다.
  - 대상 `Issue`와 새 `IssueHistory` instances 사이에 각각 `logs` association이 형성되었다.
  - 현재 `DEV`와 새 `IssueHistory` instances 사이에 각각 `changes` association이 형성되었다.
- Association 붕괴
   - 대상 `Issue`에 기존 fixer가 존재했다면, 기존 fixer `DEV`와 대상 `Issue` 사이의 `fixes` association은 제거되었다.
- 속성값 변화
  - `Issue.status`는 `ASSIGNED`에서 `FIXED`로 변경되었다.
  - `Comment.content`는 `comment`로 설정되었다.
  - `Comment.createdDate`는 현재 시각으로 설정되었다.
  - `STATUS_CHANGED` history에는 `previousValue=ASSIGNED`, `newValue=FIXED`가 기록되었다.

---

## OC-07. Resolve Fixed Issue

### 1. Operation 이름 및 파라미터

`changeStatus(issueId, targetStatus=RESOLVED, comment)`

### 2. Reference

Use Case: UC6 Change Issue State

### 3. Preconditions

- 현재 사용자는 `TESTER`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- 현재 사용자는 대상 `Issue`의 현재 verifier이다.
- 현재 사용자는 대상 `Issue`가 속한 `Project`의 active member이다.
- `Issue.status`는 `FIXED`이다.
- 현재 사용자는 `FIXED -> RESOLVED` 전이를 수행할 권한을 가진다.
- UC6은 UC2 Add Comment를 include하므로 `comment`는 비어 있지 않다.
- 대상 `Issue`와 연결된 모든 blocking issue는 `RESOLVED` 또는 `CLOSED` 상태이다.

### 4. Postconditions

- Instance 생성
  - 상태 변경 사유를 저장하기 위한 새로운 `Comment` instance가 생성되었다.
  - `actionType=COMMENTED`인 새로운 `IssueHistory` instance가 생성되었다.
  - `actionType=STATUS_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - 대상 `Issue`와 새 `Comment` 사이에 `has` association이 형성되었다.
  - 현재 `TESTER`와 새 `Comment` 사이에 `writes` association이 형성되었다.
  - 현재 `TESTER`와 대상 `Issue` 사이에 `resolves` association이 형성되었다.
  - 대상 `Issue`와 새 `IssueHistory` instances 사이에 각각 `logs` association이 형성되었다.
  - 현재 `TESTER`와 새 `IssueHistory` instances 사이에 각각 `changes` association이 형성되었다.
- Association 붕괴
  - 대상 `Issue`에 기존 resolver가 존재했다면, 기존 resolver `TESTER`와 대상 `Issue` 사이의 `resolves` association은 제거되었다.
  - 기존 assignee `DEV`와 대상 `Issue` 사이의 `assigned to` association이 제거되었다.
  - 기존 verifier `TESTER`와 대상 `Issue` 사이의 `verifies` association이 제거되었다.
- 속성값 변화
  - `Issue.status`는 `FIXED`에서 `RESOLVED`로 변경되었다.
  - `Comment.content`는 `comment`로 설정되었다.
  - `Comment.createdDate`는 현재 시각으로 설정되었다.
  - `STATUS_CHANGED` history에는 `previousValue=FIXED`, `newValue=RESOLVED`가 기록되었다.
  - 기존 `fixes` association은 유지되었다.
  - `resolves` association은 현재 `TESTER`를 최신 resolver로 가리키도록 갱신되었다.
  - `assigned to`와 `verifies` association은 없는 상태/null 상태가 되었다.
  - resolve 성공 이후에도 기존 `IssueDependency` instance는 자동 삭제되지 않았다.
  - `BLOCK` 또는 `BLOCKED` `IssueStatus`는 생성되지 않았다.

---

## OC-08. Close Resolved Issue

### 1. Operation 이름 및 파라미터

`changeStatus(issueId, targetStatus=CLOSED, comment)`

### 2. Reference

Use Case: UC6 Change Issue State

### 3. Preconditions

- 현재 사용자는 `PL`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- `Issue.status`는 `RESOLVED`이다.
- 현재 사용자는 대상 `Issue`가 속한 `Project`의 active PL이다.
- 현재 사용자는 `RESOLVED -> CLOSED` 전이를 수행할 권한을 가진다.
- UC6은 UC2 Add Comment를 include하므로 `comment`는 비어 있지 않다.

### 4. Postconditions

- Instance 생성
  - 상태 변경 사유를 저장하기 위한 새로운 `Comment` instance가 생성되었다.
  - `actionType=COMMENTED`인 새로운 `IssueHistory` instance가 생성되었다.
  - `actionType=STATUS_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - 대상 `Issue`와 새 `Comment` 사이에 `has` association이 형성되었다.
  - 현재 `PL`과 새 `Comment` 사이에 `writes` association이 형성되었다.
  - 대상 `Issue`와 새 `IssueHistory` instances 사이에 각각 `logs` association이 형성되었다.
  - 현재 `PL`과 새 `IssueHistory` instances 사이에 각각 `changes` association이 형성되었다.
- Association 붕괴
  - 기존 assignee `DEV`와 대상 `Issue` 사이의 `assigned to` association이 제거되었다.
  - 기존 verifier `TESTER`와 대상 `Issue` 사이의 `verifies` association이 제거되었다.
- 속성값 변화
  - `Issue.status`는 `RESOLVED`에서 `CLOSED`로 변경되었다.
  - `Comment.content`는 `comment`로 설정되었다.
  - `Comment.createdDate`는 현재 시각으로 설정되었다.
  - `STATUS_CHANGED` history에는 `previousValue=RESOLVED`, `newValue=CLOSED`가 기록되었다.
  - 기존 `fixes`, `resolves` association은 유지되었다.

---

## OC-09. Reopen Issue

### 1. Operation 이름 및 파라미터

`changeStatus(issueId, targetStatus=REOPENED, comment)`

### 2. Reference

Use Case: UC6 Change Issue State

### 3. Preconditions

- 현재 사용자는 `PL`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- `Issue.status`는 `RESOLVED` 또는 `CLOSED`이다.
- 현재 사용자는 대상 `Issue`가 속한 `Project`의 active PL이다.
- 현재 사용자는 `RESOLVED/CLOSED -> REOPENED` 전이를 수행할 권한을 가진다.
- UC6은 UC2 Add Comment를 include하므로 `comment`는 비어 있지 않다.

### 4. Postconditions

- Instance 생성
  - reopen 사유를 저장하기 위한 새로운 `Comment` instance가 생성되었다.
  - `actionType=COMMENTED`인 새로운 `IssueHistory` instance가 생성되었다.
  - `actionType=STATUS_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - 대상 `Issue`와 새 `Comment` 사이에 `has` association이 형성되었다.
  - 현재 `PL`과 새 `Comment` 사이에 `writes` association이 형성되었다.
  - 대상 `Issue`와 새 `IssueHistory` instances 사이에 각각 `logs` association이 형성되었다.
  - 현재 `PL`와 새 `IssueHistory` instances 사이에 각각 `changes` association이 형성되었다.
- Association 붕괴
  - 대상 `Issue`에 active assignee가 존재했다면, 기존 `assigned to` association은 제거되었다.
  - 대상 `Issue`에 active verifier가 존재했다면, 기존 `verifies` association은 제거되었다.
- 속성값 변화
  - `Issue.status`는 `RESOLVED` 또는 `CLOSED`에서 `REOPENED`로 변경되었다.
  - `Comment.content`는 `comment`로 설정되었다.
  - `Comment.createdDate`는 현재 시각으로 설정되었다.
  - `STATUS_CHANGED` history에는 `previousValue=RESOLVED` 또는 `CLOSED`, `newValue=REOPENED`가 기록되었다.
  - 기존 `fixes`, `resolves` association은 유지되었다.
  - `assigned to`와 `verifies` association은 없는 상태/null 상태가 되었다.
  - 재작업 담당자 배정은 이 operation에서 수행되지 않았고, 별도 UC5의 `assignIssue(issueId, assigneeId, verifierId)` 대상으로 남았다.

---

## OC-10. Delete Closed/New Issue

### 1. Operation 이름 및 파라미터

`deleteIssue(issueId, comment)`

### 2. Reference

Use Case: UC9 Manage Deleted Issue

### 3. Preconditions

- 현재 사용자는 `PL`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- `Issue.status`는 `NEW` 또는 `CLOSED`이다.
- 현재 사용자는 대상 `Issue`가 속한 `Project`의 active PL이다.
- 현재 사용자는 이슈를 삭제할 권한을 가진다.
- 삭제 사유인 `comment`는 비어 있지 않다.
- 대상 이슈는 아직 물리적으로 삭제되지 않았다.

### 4. Postconditions

- Instance 생성
  - `actionType=STATUS_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
  - 삭제 과정에서 제거된 각 dependency에 대해 `actionType=DEPENDENCY_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Instance 삭제
  - 대상 `Issue`와 연결된 모든 `IssueDependency` instance가 제거되었다.
  - 이 operation 이후 `DELETED` 상태의 이슈가 30개를 초과한 경우, `IssueHistory(STATUS_CHANGED, newValue=DELETED).changedDate` 기준으로 초과분에 해당하는 오래된 `DELETED` 이슈가 물리적으로 삭제되었다.
- Association 형성
  - 대상 `Issue`와 새 `IssueHistory` 사이에 `logs` association이 형성되었다.
  - 현재 `PL`과 새 `IssueHistory` 사이에 `changes` association이 형성되었다.
  - 제거된 각 dependency의 blocked issue와 dependency 제거 `IssueHistory` 사이에 `logs` association이 형성되었다.
  - 현재 `PL`과 dependency 제거 `IssueHistory` 사이에 `changes` association이 형성되었다.
- Association 붕괴
  - 제거된 각 `IssueDependency`에 대해 `blockingIssue` association이 제거되었다.
  - 제거된 각 `IssueDependency`에 대해 `blockedIssue` association이 제거되었다.
- 속성값 변화
  - `Issue.status`는 `NEW` 또는 `CLOSED`에서 `DELETED`로 변경되었다.
  - `STATUS_CHANGED` history의 message에는 삭제 사유 `comment`가 기록되었다.
  - deleted transition time은 `IssueHistory(STATUS_CHANGED, newValue=DELETED).changedDate`에서 결정되었다.
  - `NEW` 또는 `CLOSED` 대상은 활성 assignee/verifier가 없는 상태로 취급되었다.
  - 제거된 dependency는 `restoreIssue`에서 자동 복원되지 않았다.

---

## OC-11. Restore Deleted Issue

### 1. Operation 이름 및 파라미터

`restoreIssue(issueId, comment)`

### 2. Reference

Use Case: UC9 Manage Deleted Issue

### 3. Preconditions

- 현재 사용자는 `PL`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- `Issue.status`는 `DELETED`이다.
- 대상 `Issue`는 아직 물리적으로 삭제되지 않았다.
- 현재 사용자는 대상 `Issue`가 속한 `Project`의 active PL이다.
- 현재 사용자는 이슈를 복구할 권한을 가진다.
- 복구 사유인 `comment`는 비어 있지 않다.
- 복구할 상태는 `IssueHistory(STATUS_CHANGED, newValue=DELETED).previousValue`에서 확인할 수 있다.

### 4. Postconditions

- Instance 생성
  - `actionType=STATUS_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
  - 새로운 `Issue` instance는 생성되지 않았고, 기존 `Issue` instance가 그대로 사용되었다.
- Association 형성
  - 대상 `Issue`와 새 `Comment` 사이에 `has` association이 형성되었다.
  - 현재 `PL`과 새 `Comment` 사이에 `writes` association이 형성되었다.
  - 대상 `Issue`와 새 `IssueHistory` 사이에 `logs` association이 형성되었다.
  - 현재 `PL`과 새 `IssueHistory` 사이에 `changes` association이 형성되었다.
- 속성값 변화
  - `Issue.status`는 `DELETED`에서 삭제 직전 상태인 `NEW` 또는 `CLOSED`로 변경되었다.
  - 복구 상태는 별도 `Issue.preDeleteStatus` attribute가 아니라 이전 `STATUS_CHANGED` history에서 결정되었다.
  - 새 `STATUS_CHANGED` history에는 `previousValue=DELETED`, `newValue=NEW` 또는 `CLOSED`가 기록되었다.
  - 새 `STATUS_CHANGED` history의 message에는 복구 사유 `comment`가 기록되었다.
  - 해당 이슈는 deleted 보관/FIFO 물리 삭제 대상에서 제외되었다.
  - 기존 reporter, fixer, resolver, comments, history는 유지되었다.
  - `assigned to`와 `verifies` association은 없는 상태/null 상태로 유지되었다.
  - `deleteIssue`에서 제거된 dependency는 자동 복원되지 않았다.

---

# Operation Contracts - 선택 작성 대상

이 섹션은 필수 적용 대상은 아니지만, SSD만으로 객체 생성/삭제, association 변화, 속성 변화가 충분히 드러나지 않을 수 있는 보조 system operation을 대상으로 작성한다.

---

## OC-12. Assign Reopened Issue - REOPENED to ASSIGNED

### 1. Operation 이름 및 파라미터

`assignIssue(issueId, assigneeId, verifierId)`

### 2. Reference

Use Case: UC5 Assign / Update Issue Assignment

### 3. Preconditions

- 현재 사용자는 `PL`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- `Issue.status`는 `REOPENED`이다.
- 현재 사용자는 대상 `Issue`가 속한 `Project`의 active PL이다.
- `assigneeId`는 배정 가능한 `DEV`를 식별한다.
- `verifierId`는 배정 가능한 `TESTER`를 식별한다.
- UC8 Recommend Assignment Candidates가 대상 이슈 상태에 맞는 후보를 제공한 상태이다.
- 현재 사용자는 reopen된 이슈의 assignee/verifier를 다시 지정할 권한을 가진다.

### 4. Postconditions

- Instance 생성
  - `actionType=ASSIGNMENT_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
  - `actionType=STATUS_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - 선택된 `DEV`와 대상 `Issue` 사이에 `assigned to` association이 형성되었다.
  - 선택된 `TESTER`와 대상 `Issue` 사이에 `verifies` association이 형성되었다.
  - 대상 `Issue`와 두 개의 새 `IssueHistory` 사이에 각각 `logs` association이 형성되었다.
  - 현재 `PL`과 두 개의 새 `IssueHistory` 사이에 각각 `changes` association이 형성되었다.
- Association 붕괴
  - 기존 assignee가 존재했다면, 기존 assignee `DEV`와 대상 `Issue` 사이의 `assigned to` association은 제거되었다.
  - 기존 verifier가 존재했다면, 기존 verifier `TESTER`와 대상 `Issue` 사이의 `verifies` association은 제거되었다.

- 속성값 변화
  - `Issue.status`는 `REOPENED`에서 `ASSIGNED`로 변경되었다.
  - `ASSIGNMENT_CHANGED` history에는 새 assignee와 verifier 값이 기록되었다.
  - `STATUS_CHANGED` history에는 `previousValue=REOPENED`, `newValue=ASSIGNED`가 기록되었다.
  - 기존 `fixes`, `resolves` association은 보존되었다.

---

## OC-13. Reject Fix - FIXED to ASSIGNED

### 1. Operation 이름 및 파라미터

`changeStatus(issueId, targetStatus=ASSIGNED, comment)`

### 2. Reference

Use Case: UC6 Change Issue State

### 3. Preconditions

- 현재 사용자는 `TESTER`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- 현재 사용자는 대상 `Issue`의 현재 verifier이다.
- 현재 사용자는 대상 `Issue`가 속한 `Project`의 active member이다.
- `Issue.status`는 `FIXED`이다.
- 현재 사용자는 `FIXED -> ASSIGNED` 전이를 수행할 권한을 가진다.
- UC6은 UC2 Add Comment를 include하므로 `comment`는 비어 있지 않다.

### 4. Postconditions

- Instance 생성
  - fix 거절 사유를 저장하기 위한 새로운 `Comment` instance가 생성되었다.
  - `actionType=COMMENTED`인 새로운 `IssueHistory` instance가 생성되었다.
  - `actionType=STATUS_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - 대상 `Issue`와 새 `Comment` 사이에 `has` association이 형성되었다.
  - 현재 `TESTER`와 새 `Comment` 사이에 `writes` association이 형성되었다.
  - 대상 `Issue`와 새 `IssueHistory` instances 사이에 각각 `logs` association이 형성되었다.
  - 현재 `TESTER`와 새 `IssueHistory` instances 사이에 각각 `changes` association이 형성되었다.
- 속성값 변화
  - `Issue.status`는 `FIXED`에서 `ASSIGNED`로 변경되었다.
  - `Comment.content`는 `comment`로 설정되었다.
  - `Comment.createdDate`는 현재 시각으로 설정되었다.
  - `STATUS_CHANGED` history에는 `previousValue=FIXED`, `newValue=ASSIGNED`가 기록되었다.
  - 기존 `assigned to`, `verifies`, `fixes` association은 유지되었다.
  - 새 DEV 교체는 이 operation에서 수행되지 않았고, 별도 UC5의 `reassignIssue(issueId, assigneeId)` 대상으로 남았다.

---

## OC-14. Add Dependency

### 1. Operation 이름 및 파라미터

`addDependency(blockingIssueId, blockedIssueId)`

### 2. Reference

Use Case: UC7 Manage Dependency

### 3. Preconditions

- 현재 사용자는 `PL`로 로그인되어 있다.
- `blockingIssueId`가 가리키는 `Issue`가 존재한다.
- `blockedIssueId`가 가리키는 `Issue`가 존재한다.
- blocking issue와 blocked issue는 같은 `Project`에 속한다.
- blocking issue와 blocked issue는 `DELETED` 상태가 아니다.
- blocking issue와 blocked issue는 서로 다른 이슈이다.
- 새 dependency는 기존 dependency와 중복되지 않는다.
- 새 dependency는 순환 dependency를 만들지 않는다.
- 현재 사용자는 blocked issue가 속한 `Project`의 active PL이다.
- 현재 사용자는 dependency를 추가할 권한을 가진다.

### 4. Postconditions

- Instance 생성
  - 새로운 `IssueDependency` instance가 생성되었다.
  - `actionType=DEPENDENCY_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - blocking issue와 새 `IssueDependency` 사이에 `blockingIssue` association이 형성되었다.
  - blocked issue와 새 `IssueDependency` 사이에 `blockedIssue` association이 형성되었다.
  - blocked issue와 새 `IssueHistory` 사이에 `logs` association이 형성되었다.
  - 현재 `PL`과 새 `IssueHistory` 사이에 `changes` association이 형성되었다.
- 속성값 변화
  - `IssueDependency.dependencyId`는 `blockingIssueId`와 `blockedIssueId` 조합에서 파생된 값으로 설정되었다.
  - `IssueDependency.discoveredDate`는 현재 시각으로 설정되었다.
  - `IssueHistory.previousValue`와 `IssueHistory.newValue`에는 dependency 추가 전후의 dependency 정보가 기록되었다.
  - 별도 `dependencyName` 또는 `DependencyType` 속성은 생성되지 않았다.
  - `Issue.status`는 변경되지 않았다.
  - 이 dependency는 별도 IssueStatus가 아니라 UC6의 `FIXED -> RESOLVED` guard에서만 사용되었다.

---

## OC-15. Remove Dependency

### 1. Operation 이름 및 파라미터

`removeDependency(blockingIssueId, blockedIssueId)`

### 2. Reference

Use Case: UC7 Manage Dependency

### 3. Preconditions

- 현재 사용자는 `PL`로 로그인되어 있다.
- `blockingIssueId`가 가리키는 `Issue`가 존재한다.
- `blockedIssueId`가 가리키는 `Issue`가 존재한다.
- blocking issue와 blocked issue는 같은 `Project`에 속한다.
- blocking issue와 blocked issue는 `DELETED` 상태가 아니다.
- blocking issue와 blocked issue 사이의 `IssueDependency`가 존재한다.
- 현재 사용자는 blocked issue가 속한 `Project`의 active PL이다.
- 현재 사용자는 dependency를 제거할 권한을 가진다.

### 4. Postconditions

- Instance 생성
  - `actionType=DEPENDENCY_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Instance 삭제
  - 대상 `IssueDependency` instance가 제거되었다.
- Association 형성
  - 제거 대상 `IssueDependency`의 blocked issue와 새 `IssueHistory` 사이에 `logs` association이 형성되었다.
  - 현재 `PL`과 새 `IssueHistory` 사이에 `changes` association이 형성되었다.
- Association 붕괴
  - 대상 `IssueDependency`와 blocking issue 사이의 `blockingIssue` association이 제거되었다.
  - 대상 `IssueDependency`와 blocked issue 사이의 `blockedIssue` association이 제거되었다.
- 속성값 변화
  - `IssueHistory.previousValue`와 `IssueHistory.newValue`에는 dependency 제거 전후의 dependency 정보가 기록되었다.
  - `Issue.status`는 변경되지 않았다.
  - 이 dependency 변경은 UC6의 `FIXED -> RESOLVED` guard에만 영향을 주었다.

---

## OC-16. Change Priority

### 1. Operation 이름 및 파라미터

`changePriority(issueId, newPriority)`

### 2. Reference

Use Case: UC16 Change Priority

### 3. Preconditions

- 현재 사용자는 `PL`로 로그인되어 있다.
- 대상 `Issue`가 존재한다.
- `newPriority`는 유효한 `Priority` 값이다.
- `newPriority`는 현재 `Issue.priority`와 다르다.
- 현재 사용자는 우선순위를 변경할 권한을 가진다.
- 대상 `Issue`는 `DELETED` 상태가 아니다.
- 현재 사용자는 대상 `Issue`가 속한 `Project`의 active PL이다.

### 4. Postconditions

- Instance 생성
  - `actionType=PRIORITY_CHANGED`인 새로운 `IssueHistory` instance가 생성되었다.
- Association 형성
  - 대상 `Issue`와 새 `IssueHistory` 사이에 `logs` association이 형성되었다.
  - 현재 `PL`과 새 `IssueHistory` 사이에 `changes` association이 형성되었다.
- 속성값 변화
  - `Issue.priority`는 기존 priority에서 `newPriority`로 변경되었다.
  - `IssueHistory.previousValue`에는 기존 priority가 기록되었다.
  - `IssueHistory.newValue`에는 `newPriority`가 기록되었다.
  - `Issue.status`는 변경되지 않았다.
  - assignment 관련 association인 `assigned to`, `verifies`, `fixes`, `resolves`는 변경되지 않았다.
