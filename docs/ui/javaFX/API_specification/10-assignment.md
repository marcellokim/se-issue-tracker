# 10. 배정/재배정 (KNN 추천)

## 진입 조건

- Actor: PL
- 선행 화면: 이슈 상세 → 배정/재배정/검증자 변경 버튼 클릭
- 전달 값: `issueId`

## 화면 동작 API

### 1. 배정 후보 조회

- **호출 시점**: 화면 진입 시
- **메서드**: `AssignmentController.startAssignment(issueId)`
- **반환**: `AssignmentOptionsResult`

| 필드 | 타입 | 설명 |
|---|---|---|
| devAssigneeCandidates | List\<AssignmentCandidateResult\> | KNN 추천 DEV 후보 (최대 3명) |
| testerVerifierCandidates | List\<AssignmentCandidateResult\> | KNN 추천 TESTER 후보 (최대 3명) |
| allDevAssignees | List\<AssignmentCandidateResult\> | 전체 active DEV 목록 |
| allTesterVerifiers | List\<AssignmentCandidateResult\> | 전체 active TESTER 목록 |

- `AssignmentCandidateResult` 상세:

| 필드 | 타입 | 설명 |
|---|---|---|
| loginId | String | 사용자 ID |
| name | String | 사용자 이름 |
| role | Role | DEV 또는 TESTER |
| completedIssueCount | int | 해결 이슈 수 |
| reason | String | 추천 사유 |

### 2. 신규 배정 (NEW/REOPENED)

- **메서드**: `AssignmentController.assignIssue(issueId, assigneeId, verifierId)`
- **파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| issueId | long | Y | 이슈 ID |
| assigneeId | String | Y | DEV 담당자 ID |
| verifierId | String | Y | TESTER 검증자 ID |

- **반환**: `AssignmentResult`

### 3. 재배정 (ASSIGNED)

- **메서드**: `AssignmentController.reassignIssue(issueId, assigneeId)`
- **파라미터**: issueId, assigneeId
- **반환**: `AssignmentResult`

### 4. 검증자 변경 (FIXED)

- **메서드**: `AssignmentController.changeVerifier(issueId, verifierId)`
- **파라미터**: issueId, verifierId
- **반환**: `AssignmentResult`

- `AssignmentResult` 상세:

| 필드 | 타입 | 설명 |
|---|---|---|
| id | long | 이슈 ID |
| issueId | String | 이슈 식별자 |
| status | IssueStatus | 변경된 상태 |
| assignee | UserResult | 담당자 |
| verifier | UserResult | 검증자 |

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 배정/재배정/변경 완료 | 이슈 상세 새로고침 |
| 취소 | 이슈 상세 (변경 없음) |
