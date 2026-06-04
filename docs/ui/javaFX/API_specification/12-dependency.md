# 12. 의존성 보기/추가/제거

## 진입 조건

- Actor: 프로젝트 멤버 (보기), PL (추가/제거)
- 선행 화면: 이슈 상세
- 전달 값: `projectId`, `issueId`

## 권한 분리

| 동작 | 권한 | 조건 |
|---|---|---|
| 의존성 목록 보기 | 프로젝트 멤버 전체 | 항상 표시 |
| 의존성 추가 버튼 | PL만 | `canAddDependency=true` |
| 의존성 제거 버튼 | PL만 | `canRemoveDependency=true` |

## 화면 동작 API

`blockingIssueId`는 다른 이슈를 막는 이슈의 ID이고, `blockedIssueId`는 그 dependency 때문에 막히는 이슈의 ID이다. 이슈 상세 화면에서는 현재 이슈를 기준으로 “나를 막는 의존성”과 “내가 막고 있는 의존성”을 나누어 보여준다.

### 1. 의존성 목록 조회

- **호출 시점**: 이슈 상세 진입 시 (`viewIssueDetail`에 포함) 또는 별도 조회
- **메서드**: `IssueController.viewProjectDependencies(projectId)`
- **반환**: `List<DependencyResult>`

| 필드 | 타입 | 설명 |
|---|---|---|
| id | long | 의존성 내부 ID |
| dependencyId | String | 의존성 해시 ID |
| blockingIssueId | long | 막는 이슈 ID |
| blockingIssueKey | String | 막는 이슈 식별자 |
| blockedIssueId | long | 막힌 이슈 ID |
| blockedIssueKey | String | 막힌 이슈 식별자 |
| discoveredDate | LocalDateTime | 등록일 |

### 2. 의존성 추가

- **호출 시점**: 추가 버튼 클릭 (PL만)
- **메서드**: `IssueController.addDependency(blockingIssueId, blockedIssueId)`
- **파라미터**: blockingIssueId (long), blockedIssueId (long)
- **반환**: `DependencyResult`
- **에러**:

| 조건 | 예외 |
|---|---|
| 자기 자신에 대한 의존 | IllegalArgumentException |
| 이미 존재하는 의존 | IllegalArgumentException |
| 순환 의존 | IllegalArgumentException |
| 다른 프로젝트 이슈 | IllegalArgumentException |
| PL 아님 | SecurityException |

### 3. 의존성 제거

- **호출 시점**: 제거 버튼 클릭 (PL만)
- **메서드**: `IssueController.removeDependency(blockingIssueId, blockedIssueId)`
- **반환**: void

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 추가/제거 완료 | 이슈 상세 새로고침 |
