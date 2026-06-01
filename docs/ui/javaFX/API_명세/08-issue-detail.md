# 08. 이슈 상세

## 진입 조건

- Actor: 프로젝트 멤버
- 선행 화면: 이슈 목록 → 이슈 클릭
- 전달 값: `issueId`

## 화면 동작 API

### 1. 이슈 상세 조회 (초기 로딩)

- **호출 시점**: 화면 진입 시
- **메서드**: `IssueController.viewIssueDetail(issueId)`
- **반환**: `IssueDetailResult`

| 필드 | 타입 | 설명 |
|---|---|---|
| id | long | 이슈 내부 ID |
| projectId | long | 프로젝트 ID |
| issueId | String | 이슈 식별자 |
| status | IssueStatus | 현재 상태 |
| priority | Priority | 우선순위 |
| title | String | 제목 |
| description | String | 설명 |
| reporter | UserResult | 등록자 |
| assignee | UserResult | 담당자 (nullable) |
| verifier | UserResult | 검증자 (nullable) |
| fixer | UserResult | 수정자 (nullable) |
| resolver | UserResult | 해결자 (nullable) |
| reportedDate | LocalDateTime | 등록일 |
| updatedAt | LocalDateTime | 수정일 |
| comments | List\<CommentResult\> | 코멘트 목록 |
| histories | List\<HistoryResult\> | 이력 목록 |
| blockedByDependencies | List\<DependencyResult\> | 현재 이슈를 막고 있는 의존성 목록 |
| blockingDependencies | List\<DependencyResult\> | 현재 이슈가 다른 이슈를 막고 있는 의존성 목록 |
| availableActions | List\<String\> | 가능한 액션 목록 |

> `comments`, `histories`, `blockedByDependencies`, `blockingDependencies`, `availableActions`가 모두 포함되어 있으므로 이슈 상세 화면의 기본 정보는 별도 API 호출 없이 구성할 수 있다.

### 2. 가능 액션 부분 새로고침

- **호출 시점**: 상태 변경/배정 등 액션 완료 후 버튼 상태 갱신
- **메서드**: `IssueController.viewAvailableActions(issueId)`
- **반환**: `IssueWorkflowActions`

| 필드 | 타입 | 설명 |
|---|---|---|
| canUpdateIssue | boolean | 이슈 수정 가능 |
| canChangePriority | boolean | 우선순위 변경 가능 |
| canStartAssignment | boolean | 배정 시작 가능 |
| canAssign | boolean | 신규 배정 가능 |
| canReassign | boolean | 재배정 가능 |
| canChangeVerifier | boolean | 검증자 변경 가능 |
| canMarkFixed | boolean | FIXED 전이 가능 |
| canRejectFix | boolean | 반려 가능 |
| canResolve | boolean | RESOLVED 전이 가능 |
| canClose | boolean | CLOSED 전이 가능 |
| canReopen | boolean | REOPENED 전이 가능 |
| canAddDependency | boolean | 의존성 추가 가능 |
| canRemoveDependency | boolean | 의존성 제거 가능 |
| canAddComment | boolean | 코멘트 추가 가능 |
| canSoftDelete | boolean | 이슈 삭제 가능 |

> 초기 로딩 시에는 `viewIssueDetail()`의 `availableActions`로 대체 가능. 상태 변경 후 전체 새로고침 없이 버튼만 갱신할 때 사용.

### 3. 코멘트 부분 새로고침

- **호출 시점**: 코멘트 추가/수정/삭제 후
- **메서드**: `IssueController.viewComments(issueId)`
- **반환**: `List<CommentResult>`

| 필드 | 타입 | 설명 |
|---|---|---|
| commentId | String | 코멘트 ID |
| content | String | 내용 |
| purpose | CommentPurpose | GENERAL / STATUS_CHANGE |
| writerLoginId | String | 작성자 ID |
| writer | UserResult | 작성자 정보 |
| createdDate | LocalDateTime | 작성일 |
| updatedDate | LocalDateTime | 수정일 |

### 4. 코멘트 수정 가능 여부

- **호출 시점**: 각 코멘트 렌더링 시
- **메서드**: `IssueController.canUpdateComment(issueId, commentId)`
- **반환**: `boolean`

### 5. 코멘트 삭제 가능 여부

- **호출 시점**: 각 코멘트 렌더링 시
- **메서드**: `IssueController.canDeleteComment(issueId, commentId)`
- **반환**: `boolean`

## 버튼 활성화 제어

`viewIssueDetail()`의 `availableActions` 필드(`List<String>`)로 버튼 표시 여부를 결정한다. UI는 문자열 상수 포함 여부(`list.contains("MARK_FIXED")`)로만 판단하며, 비즈니스 규칙을 UI에 중복 구현하지 않는다.

> 별도 `viewAvailableActions(issueId)`를 호출하면 `IssueWorkflowActions` (boolean 필드)를 반환한다. 초기 로딩에서는 `viewIssueDetail().availableActions`를 사용하고, 상태 변경 후 버튼만 갱신할 때는 `viewAvailableActions()`를 사용한다.

| 버튼 | availableActions 문자열 | viewAvailableActions boolean | 다음 화면/동작 |
|---|---|---|---|
| FIXED 처리 | `"MARK_FIXED"` | `canMarkFixed` | → 상태 변경 dialog |
| RESOLVED 처리 | `"RESOLVE"` | `canResolve` | → 상태 변경 dialog |
| 반려 | `"REJECT_FIX"` | `canRejectFix` | → 상태 변경 dialog |
| CLOSED 처리 | `"CLOSE"` | `canClose` | → 상태 변경 dialog |
| REOPENED 처리 | `"REOPEN"` | `canReopen` | → 상태 변경 dialog |
| 배정 | `"ASSIGN"` | `canAssign` | → 배정 화면 |
| 재배정 | `"REASSIGN_DEV"` | `canReassign` | → 배정 화면 |
| 검증자 변경 | `"CHANGE_TESTER"` | `canChangeVerifier` | → 배정 화면 |
| 이슈 수정 | `"UPDATE_ISSUE"` | `canUpdateIssue` | → 이슈 수정 |
| 우선순위 변경 | `"CHANGE_PRIORITY"` | `canChangePriority` | → 우선순위 변경 |
| 배정 시작 | `"START_ASSIGNMENT"` | `canStartAssignment` | → 배정 화면 |
| 의존성 추가 | `"ADD_DEPENDENCY"` | `canAddDependency` | → 의존성 관리 |
| 의존성 제거 | `"REMOVE_DEPENDENCY"` | `canRemoveDependency` | → 의존성 관리 |
| 코멘트 추가 | `"ADD_COMMENT"` | `canAddComment` | → 코멘트 입력 |
| 이슈 삭제 | `"SOFT_DELETE"` | `canSoftDelete` | → 삭제 사유 dialog |

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 뒤로가기 | 이슈 목록 |
| 상태 변경 완료 | 이슈 상세 새로고침 |
| 배정 완료 | 이슈 상세 새로고침 |
| 코멘트 추가/수정/삭제 완료 | 코멘트 영역 새로고침 |
| 이슈 수정 완료 | 이슈 상세 새로고침 |
| 이슈 삭제 완료 | 이슈 목록으로 이동 |
