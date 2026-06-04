# 13. 이슈 수정 + 우선순위 변경 + 이슈 삭제

## 진입 조건

- 선행 화면: 이슈 상세 → 해당 버튼 클릭
- 전달 값: `issueId`

## 화면 동작 API

### 1. 이슈 수정

- **Actor**: reporter (NEW 또는 REOPENED 상태만, `canUpdateIssue=true`)
- **메서드**: `IssueController.updateIssue(issueId, title, description)`
- **파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| issueId | long | Y | 이슈 ID |
| title | String | Y | 수정할 제목 |
| description | String | Y | 수정할 설명 |

- **반환**: `IssueResult`
- **에러**:

| 조건 | 예외 |
|---|---|
| reporter 아님 | SecurityException |
| NEW/REOPENED 아님 | SecurityException |
| 같은 프로젝트에 동일 제목 존재 | IllegalArgumentException |

### 2. 우선순위 변경

- **Actor**: PL (`canChangePriority=true`)
- **메서드**: `IssueController.changePriority(issueId, priority)`
- **파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| issueId | long | Y | 이슈 ID |
| priority | Priority | Y | BLOCKER / CRITICAL / MAJOR / MINOR / TRIVIAL |

- **반환**: `IssueResult`

### 3. 이슈 삭제 (soft delete)

- **Actor**: PL (`canSoftDelete=true`)
- **대상**: NEW 또는 CLOSED 상태만
- **호출 시점**: 삭제 버튼 → 사유 입력 dialog → 확인
- **메서드**: `DeletedIssueController.deleteIssue(issueId, comment)`
- **파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| issueId | long | Y | 이슈 ID |
| comment | String | Y | 삭제 사유 (빈 값 불가) |

- **반환**: `IssueSummary`
- **에러**:

| 조건 | 예외 |
|---|---|
| PL 아님 | SecurityException |
| NEW/CLOSED 아님 | SecurityException |
| comment 빈 값 | IllegalArgumentException |

> 삭제 후 DELETED 이슈가 30개를 초과하면 FIFO로 자동 purge 실행.

## UI 구현 주의사항

- 이슈 삭제 시 사유 입력 dialog 필수 (상태 변경과 동일 패턴).
- 삭제 완료 후 이슈 상세가 아닌 **이슈 목록**으로 이동.

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 이슈 수정 완료 | 이슈 상세 새로고침 |
| 우선순위 변경 완료 | 이슈 상세 새로고침 |
| 이슈 삭제 완료 | 이슈 목록으로 이동 |
| 취소 | 이슈 상세 (변경 없음) |
