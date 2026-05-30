# 09. 상태 변경 (사유 dialog)

## 진입 조건

- Actor: 역할별 상이 (availableActions 기반)
- 선행 화면: 이슈 상세 → 상태 변경 버튼 클릭
- 전달 값: `issueId`, `targetStatus`

## 화면 동작 API

### 1. 상태 변경

- **호출 시점**: 사유 입력 dialog에서 확인 버튼 클릭
- **메서드**: `IssueStateController.changeStatus(issueId, targetStatus, comment)`
- **파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| issueId | long | Y | 이슈 ID |
| targetStatus | IssueStatus | Y | 목표 상태 |
| comment | String | Y | 사유 (빈 값 불가) |

- **반환**: `IssueStateResult`

| 필드 | 타입 | 설명 |
|---|---|---|
| id | long | 이슈 ID |
| issueId | String | 이슈 식별자 |
| status | IssueStatus | 변경된 상태 |
| assignee | UserResult | 담당자 |
| verifier | UserResult | 검증자 |
| fixer | UserResult | 수정자 |
| resolver | UserResult | 해결자 |

- **에러**:

| 조건 | 예외 |
|---|---|
| comment 빈 값 | IllegalArgumentException |
| 권한 없음 | SecurityException |
| 상태 전이 불가 | UnsupportedOperationException |
| 미해결 blocking issue 존재 (RESOLVED 전이 시) | IllegalStateException |

## 상태별 Actor 매핑

| 전이 | Actor | availableActions 조건 |
|---|---|---|
| ASSIGNED → FIXED | Dev (현재 assignee) | canMarkFixed |
| FIXED → RESOLVED | Tester (현재 verifier) | canResolve |
| FIXED → ASSIGNED (반려) | Tester (현재 verifier) | canRejectFix |
| RESOLVED → CLOSED | PL | canClose |
| RESOLVED/CLOSED → REOPENED | PL | canReopen |

## UI 구현 주의사항

- 버튼 클릭 시 바로 API를 호출하지 않는다.
- 사유 입력 dialog를 먼저 표시하고, 사유 입력 후 확인 시 API 호출.
- comment는 필수이므로 빈 값 제출 방지 필요.

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 변경 완료 | 이슈 상세 새로고침 |
| 취소 | 이슈 상세 (변경 없음) |
