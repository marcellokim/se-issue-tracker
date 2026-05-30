# 14. 삭제 이슈 관리

## 진입 조건

- Actor: PL
- 선행 화면: 이슈 목록 → 삭제 이슈 관리 버튼
- 전달 값: `projectId`

## 화면 동작 API

### 1. 보관 한도 조회

- **호출 시점**: 화면 진입 시
- **메서드**: `DeletedIssueController.getMaxRetentionLimit()`
- **반환**: `int` (현재 30)

### 2. 삭제 이슈 목록 조회

- **호출 시점**: 화면 진입 시
- **메서드**: `DeletedIssueController.viewDeletedIssues(projectId)`
- **반환**: `List<IssueSummary>`

> 화면 표시: "삭제 이슈 {list.size()}/{maxRetentionLimit}"

### 3. 이슈 복구

- **호출 시점**: 복구 버튼 → 사유 입력 dialog → 확인
- **메서드**: `DeletedIssueController.restoreIssue(issueId, comment)`
- **파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| issueId | long | Y | 이슈 ID |
| comment | String | Y | 복구 사유 (빈 값 불가) |

- **반환**: `IssueSummary`
- **후속**: 삭제 이슈 목록 새로고침

### 4. 영구 삭제

- **호출 시점**: 영구 삭제 버튼 클릭
- **메서드**: `DeletedIssueController.purgeDeletedIssue(issueId)`
- **파라미터**: issueId (long)
- **반환**: void
- **에러**:

| 조건 | 예외 |
|---|---|
| DELETED 상태 아님 | IllegalArgumentException |
| PL 아님 | SecurityException |

- **후속**: 삭제 이슈 목록 새로고침

> `purgeOverflow`는 softDelete 시 자동 실행되므로 이 화면에 별도 버튼 없음.

## UI 미노출 API

### purgeOverflow

- **메서드**: `DeletedIssueController.purgeOverflow(projectId)`
- **반환**: `int` (삭제된 건수)
- **호출 주체**: `deleteIssue` 실행 시 서비스 내부에서 자동 호출
- **UI 노출**: 없음 — 별도 버튼 불필요

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 뒤로가기 | 이슈 목록 |
| 복구 완료 | 삭제 이슈 목록 새로고침 |
| 영구 삭제 완료 | 삭제 이슈 목록 새로고침 |
