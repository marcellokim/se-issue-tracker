# Operation Contracts

이 문서는 UC 명세에서 Fully Dressed로 다룬 핵심 시나리오와 SSD만으로 결과를 이해하기 어려운 system operation을 대상으로 작성한다. 보고서 본문에서는 `OC-01 Register Issue`, `OC-07 Resolve Fixed Issue`, `OC-14 Add Dependency`를 대표 Operation Contract로 사용한다. 세 operation은 각각 이슈 생성, 상태 전이, 이슈 의존성 관계 생성을 보여주기 때문에 SSD만으로는 부족한 도메인 객체의 생성, association 변화, 속성값 변화를 설명하기에 적합하다.

나머지 OC는 구현 정책과 설계 검토를 위한 reference로 유지한다.

작성 기준:
- Reference는 대응 Use Case를 기준으로 적는다.
- Preconditions는 operation 수행에 꼭 필요한 상태, 권한, 입력 조건만 적는다.
- 로그인 여부, 대상 객체 존재 여부, active 사용자 여부, 기본 권한 검사는 보호된 operation의 공통 전제로 보고, 각 OC에서는 필요한 경우에만 다시 적는다.
- Postconditions는 성공 이후 남는 도메인 객체 변화만 적는다. 단순 서비스 호출 순서, SQL 처리, UI 표시 내용은 적지 않는다.
- `IssueHistory`가 생성되는 경우 해당 `Issue`와 변경한 `User`의 기록 관계도 함께 형성된 것으로 본다.
- 상태 변경 사유 comment처럼 comment와 comment 이력이 함께 생기는 경우에는 한 문장으로 묶어 적는다.
- 모든 새 `IssueHistory.changedDate`와 새 `Comment.createdDate`는 현재 시각으로 설정된 것으로 본다.

---

## OC-01. Register Issue

### 1. Operation 이름 및 파라미터

`registerIssue(projectId, title, description, priority)`

### 2. Reference

Use Case: UC1 Register Issue

### 3. Preconditions

- 현재 사용자는 선택된 `Project`의 active member이다.
- `title`과 `description`은 비어 있지 않다.
- 같은 프로젝트 안에 동일한 title의 `Issue`가 존재하지 않는다.

### 4. Postconditions

- 새로운 `Issue`가 생성되었다.
- 새 `Issue`는 선택된 `Project`에 속하게 되었고, 현재 `User`가 reporter로 연결되었다.
- `Issue.title`, `Issue.description`, `Issue.priority`가 입력값으로 설정되었다. 단, priority가 생략되면 `MAJOR`로 설정되었다.
- `Issue.status`는 `NEW`로, `Issue.reportedDate`는 현재 시각으로 설정되었다.
- `actionType=CREATED`인 `IssueHistory`가 생성되었다.

---

## OC-02. Add Comment

### 1. Operation 이름 및 파라미터

`addComment(issueId, content)`

### 2. Reference

Use Case: UC2 Add Comment

### 3. Preconditions

- 현재 사용자는 대상 `Issue`가 속한 프로젝트의 active member이다.
- 대상 `Issue`는 `DELETED` 상태가 아니다.
- `content`는 비어 있지 않다.

### 4. Postconditions

- 새로운 `Comment`가 생성되고 대상 `Issue`에 연결되었다.
- 현재 `User`가 새 `Comment`의 writer로 연결되었다.
- `Comment.content`는 `content`로 설정되었다.
- 일반 댓글 추가를 나타내는 `IssueHistory(actionType=COMMENTED)`가 생성되었다.

---

## OC-03. Assign Issue - NEW to ASSIGNED

### 1. Operation 이름 및 파라미터

`assignIssue(issueId, assigneeId, verifierId)`

### 2. Reference

Use Case: UC5 Assign / Update Issue Assignment

### 3. Preconditions

- 현재 사용자는 대상 `Issue`가 속한 프로젝트의 active PL이다.
- `Issue.status`는 `NEW`이다.
- `assigneeId`는 배정 가능한 active `DEV`를 식별한다.
- `verifierId`는 배정 가능한 active `TESTER`를 식별한다.

### 4. Postconditions

- 선택된 `DEV`가 대상 `Issue`의 assignee로 연결되었다.
- 선택된 `TESTER`가 대상 `Issue`의 verifier로 연결되었다.
- `Issue.status`는 `NEW`에서 `ASSIGNED`로 변경되었다.
- `IssueHistory(actionType=ASSIGNMENT_CHANGED)`가 생성되었다.
- `IssueHistory(actionType=STATUS_CHANGED, previousValue=NEW, newValue=ASSIGNED)`가 생성되었다.

---

## OC-04. Reassign Issue - ASSIGNED to ASSIGNED

### 1. Operation 이름 및 파라미터

`reassignIssue(issueId, assigneeId)`

### 2. Reference

Use Case: UC5 Assign / Update Issue Assignment

### 3. Preconditions

- 현재 사용자는 대상 `Issue`가 속한 프로젝트의 active PL이다.
- `Issue.status`는 `ASSIGNED`이다.
- `assigneeId`는 배정 가능한 active `DEV`를 식별한다.

### 4. Postconditions

- 대상 `Issue`의 assignee가 선택된 `DEV`로 변경되었다.
- `Issue.status`는 `ASSIGNED`로 유지되었다.
- 기존 verifier, fixer, resolver 정보는 변경되지 않았다.
- `IssueHistory(actionType=ASSIGNMENT_CHANGED)`가 생성되었다.

---

## OC-05. Change Verifier - FIXED to FIXED

### 1. Operation 이름 및 파라미터

`changeVerifier(issueId, verifierId)`

### 2. Reference

Use Case: UC5 Assign / Update Issue Assignment

### 3. Preconditions

- 현재 사용자는 대상 `Issue`가 속한 프로젝트의 active PL이다.
- `Issue.status`는 `FIXED`이다.
- `verifierId`는 배정 가능한 active `TESTER`를 식별한다.

### 4. Postconditions

- 대상 `Issue`의 verifier가 선택된 `TESTER`로 변경되었다.
- `Issue.status`는 `FIXED`로 유지되었다.
- 기존 assignee, fixer, resolver 정보는 변경되지 않았다.
- `IssueHistory(actionType=ASSIGNMENT_CHANGED)`가 생성되었다.

---

## OC-06. Mark Issue as Fixed

### 1. Operation 이름 및 파라미터

`changeStatus(issueId, targetStatus=FIXED, comment)`

### 2. Reference

Use Case: UC6 Change Issue State

### 3. Preconditions

- 현재 사용자는 대상 `Issue`의 현재 assignee인 active `DEV`이다.
- `Issue.status`는 `ASSIGNED`이다.
- 상태 변경 사유 `comment`는 비어 있지 않다.

### 4. Postconditions

- `Issue.status`는 `ASSIGNED`에서 `FIXED`로 변경되었다.
- 현재 `DEV`가 대상 `Issue`의 fixer로 기록되었다.
- 상태 변경 사유를 담은 `Comment(purpose=STATUS_CHANGE)`와 comment 이력이 생성되었다.
- `IssueHistory(actionType=STATUS_CHANGED, previousValue=ASSIGNED, newValue=FIXED)`가 생성되었다.

---

## OC-07. Resolve Fixed Issue

### 1. Operation 이름 및 파라미터

`changeStatus(issueId, targetStatus=RESOLVED, comment)`

### 2. Reference

Use Case: UC6 Change Issue State

### 3. Preconditions

- 현재 사용자는 대상 `Issue`의 현재 verifier인 active `TESTER`이다.
- `Issue.status`는 `FIXED`이다.
- 상태 변경 사유 `comment`는 비어 있지 않다.
- 대상 `Issue`를 막고 있는 모든 blocking issue는 `RESOLVED` 또는 `CLOSED` 상태이다.

### 4. Postconditions

- `Issue.status`는 `FIXED`에서 `RESOLVED`로 변경되었다.
- 현재 `TESTER`가 대상 `Issue`의 resolver로 기록되었다.
- 대상 `Issue`의 assignee와 verifier는 제거되었다.
- 기존 fixer 정보는 유지되었다.
- 상태 변경 사유를 담은 `Comment(purpose=STATUS_CHANGE)`와 comment 이력이 생성되었다.
- `IssueHistory(actionType=STATUS_CHANGED, previousValue=FIXED, newValue=RESOLVED)`가 생성되었다.
- 기존 `IssueDependency`는 자동 삭제되지 않았고, 별도 `BLOCK/BLOCKED` 상태도 생성되지 않았다.

---

## OC-08. Close Resolved Issue

### 1. Operation 이름 및 파라미터

`changeStatus(issueId, targetStatus=CLOSED, comment)`

### 2. Reference

Use Case: UC6 Change Issue State

### 3. Preconditions

- 현재 사용자는 대상 `Issue`가 속한 프로젝트의 active PL이다.
- `Issue.status`는 `RESOLVED`이다.
- 상태 변경 사유 `comment`는 비어 있지 않다.

### 4. Postconditions

- `Issue.status`는 `RESOLVED`에서 `CLOSED`로 변경되었다.
- 기존 fixer와 resolver 정보는 유지되었다.
- 상태 변경 사유를 담은 `Comment(purpose=STATUS_CHANGE)`와 comment 이력이 생성되었다.
- `IssueHistory(actionType=STATUS_CHANGED, previousValue=RESOLVED, newValue=CLOSED)`가 생성되었다.

---

## OC-09. Reopen Issue

### 1. Operation 이름 및 파라미터

`changeStatus(issueId, targetStatus=REOPENED, comment)`

### 2. Reference

Use Case: UC6 Change Issue State

### 3. Preconditions

- 현재 사용자는 대상 `Issue`가 속한 프로젝트의 active PL이다.
- `Issue.status`는 `RESOLVED` 또는 `CLOSED`이다.
- 상태 변경 사유 `comment`는 비어 있지 않다.

### 4. Postconditions

- `Issue.status`는 `RESOLVED` 또는 `CLOSED`에서 `REOPENED`로 변경되었다.
- assignee와 verifier는 자동 복원되지 않았다.
- 기존 fixer와 resolver 정보는 유지되었다.
- 상태 변경 사유를 담은 `Comment(purpose=STATUS_CHANGE)`와 comment 이력이 생성되었다.
- `IssueHistory(actionType=STATUS_CHANGED, previousValue=RESOLVED 또는 CLOSED, newValue=REOPENED)`가 생성되었다.

---

## OC-10. Delete Closed/New Issue

### 1. Operation 이름 및 파라미터

`deleteIssue(issueId, comment)`

### 2. Reference

Use Case: UC9 Manage Deleted Issue

### 3. Preconditions

- 현재 사용자는 대상 `Issue`가 속한 프로젝트의 active PL이다.
- `Issue.status`는 `NEW` 또는 `CLOSED`이다.
- 삭제 사유 `comment`는 비어 있지 않다.

### 4. Postconditions

- `Issue.status`는 `NEW` 또는 `CLOSED`에서 `DELETED`로 변경되었다.
- `IssueHistory(actionType=STATUS_CHANGED, newValue=DELETED)`가 생성되고, message에는 삭제 사유가 기록되었다.
- 대상 `Issue`가 포함된 `IssueDependency`는 모두 제거되었다.
- 제거된 dependency에 대해서는 blocked issue 기준의 `IssueHistory(actionType=DEPENDENCY_CHANGED)`가 생성되었다.
- soft delete 이후 프로젝트별 보관 한도 30개를 초과한 경우, 오래된 `DELETED` 이슈가 FIFO 기준으로 물리 삭제되었다.

---

## OC-11. Restore Deleted Issue

### 1. Operation 이름 및 파라미터

`restoreIssue(issueId, comment)`

### 2. Reference

Use Case: UC9 Manage Deleted Issue

### 3. Preconditions

- 현재 사용자는 대상 `Issue`가 속한 프로젝트의 active PL이다.
- `Issue.status`는 `DELETED`이다.
- 복구 사유 `comment`는 비어 있지 않다.
- 삭제 직전 상태는 이전 `STATUS_CHANGED` history에서 확인할 수 있다.

### 4. Postconditions

- `Issue.status`는 `DELETED`에서 삭제 직전 상태인 `NEW` 또는 `CLOSED`로 변경되었다.
- `IssueHistory(actionType=STATUS_CHANGED, previousValue=DELETED)`가 생성되고, message에는 복구 사유가 기록되었다.
- 기존 reporter, fixer, resolver, comments, history는 유지되었다.
- assignee와 verifier는 null 상태로 유지되었다.
- 삭제 시 제거된 dependency는 자동 복원되지 않았다.
- 복구된 이슈는 deleted 보관/FIFO 물리 삭제 대상에서 제외되었다.

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

- 현재 사용자는 대상 `Issue`가 속한 프로젝트의 active PL이다.
- `Issue.status`는 `REOPENED`이다.
- `assigneeId`는 배정 가능한 active `DEV`를 식별한다.
- `verifierId`는 배정 가능한 active `TESTER`를 식별한다.

### 4. Postconditions

- 선택된 `DEV`가 대상 `Issue`의 assignee로 연결되었다.
- 선택된 `TESTER`가 대상 `Issue`의 verifier로 연결되었다.
- `Issue.status`는 `REOPENED`에서 `ASSIGNED`로 변경되었다.
- 기존 fixer와 resolver 정보는 유지되었다.
- `IssueHistory(actionType=ASSIGNMENT_CHANGED)`가 생성되었다.
- `IssueHistory(actionType=STATUS_CHANGED, previousValue=REOPENED, newValue=ASSIGNED)`가 생성되었다.

---

## OC-13. Reject Fix - FIXED to ASSIGNED

### 1. Operation 이름 및 파라미터

`changeStatus(issueId, targetStatus=ASSIGNED, comment)`

### 2. Reference

Use Case: UC6 Change Issue State

### 3. Preconditions

- 현재 사용자는 대상 `Issue`의 현재 verifier인 active `TESTER`이다.
- `Issue.status`는 `FIXED`이다.
- 상태 변경 사유 `comment`는 비어 있지 않다.

### 4. Postconditions

- `Issue.status`는 `FIXED`에서 `ASSIGNED`로 변경되었다.
- 기존 assignee, verifier, fixer 정보는 유지되었다.
- 상태 변경 사유를 담은 `Comment(purpose=STATUS_CHANGE)`와 comment 이력이 생성되었다.
- `IssueHistory(actionType=STATUS_CHANGED, previousValue=FIXED, newValue=ASSIGNED)`가 생성되었다.

---

## OC-14. Add Dependency

### 1. Operation 이름 및 파라미터

`addDependency(blockingIssueId, blockedIssueId)`

### 2. Reference

Use Case: UC7 Manage Dependency

### 3. Preconditions

- 현재 사용자는 blocked issue가 속한 프로젝트의 active PL이다.
- blocking issue와 blocked issue는 같은 프로젝트에 속하며, 둘 다 `DELETED` 상태가 아니다.
- blocking issue와 blocked issue는 서로 다른 이슈이다.
- 동일한 dependency가 아직 존재하지 않는다.

### 4. Postconditions

- 새로운 `IssueDependency`가 생성되었다.
- 새 dependency는 blocking issue와 blocked issue를 연결한다.
- `IssueDependency.dependencyId`는 `blockingIssueId`와 `blockedIssueId` 조합에서 파생된 값으로 설정되었다.
- blocked issue 기준으로 `IssueHistory(actionType=DEPENDENCY_CHANGED)`가 생성되었다.
- `Issue.status`는 변경되지 않았고, 이 dependency는 이후 `FIXED -> RESOLVED` guard에서 사용된다.

### 5. 실패 조건

- 시스템이 기존 dependency 관계를 따라 순환 dependency 여부를 검사하고, 순환이 감지되면 operation을 거부한다.

---

## OC-15. Remove Dependency

### 1. Operation 이름 및 파라미터

`removeDependency(blockingIssueId, blockedIssueId)`

### 2. Reference

Use Case: UC7 Manage Dependency

### 3. Preconditions

- 현재 사용자는 blocked issue가 속한 프로젝트의 active PL이다.
- blocking issue와 blocked issue는 같은 프로젝트에 속하며, 둘 다 `DELETED` 상태가 아니다.
- 두 이슈 사이의 `IssueDependency`가 존재한다.

### 4. Postconditions

- 대상 `IssueDependency`가 제거되었다.
- dependency와 blocking/blocked issue 사이의 연결이 끊어졌다.
- blocked issue 기준으로 `IssueHistory(actionType=DEPENDENCY_CHANGED)`가 생성되었다.
- `Issue.status`는 변경되지 않았고, 이후 `FIXED -> RESOLVED` guard 결과에만 영향을 준다.

---

## OC-16. Change Priority

### 1. Operation 이름 및 파라미터

`changePriority(issueId, newPriority)`

### 2. Reference

Use Case: UC16 Change Priority

### 3. Preconditions

- 현재 사용자는 대상 `Issue`가 속한 프로젝트의 active PL이다.
- 대상 `Issue`는 `DELETED` 상태가 아니다.
- `newPriority`는 현재 `Issue.priority`와 다른 유효한 `Priority` 값이다.

### 4. Postconditions

- `Issue.priority`는 기존 priority에서 `newPriority`로 변경되었다.
- `IssueHistory(actionType=PRIORITY_CHANGED)`가 생성되었다.
- `Issue.status`와 배정 관련 정보는 변경되지 않았다.
