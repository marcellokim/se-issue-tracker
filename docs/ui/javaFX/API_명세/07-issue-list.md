# 07. 이슈 목록 + 이슈 등록

## 진입 조건

- Actor: 프로젝트 멤버 (PL | Dev | Tester)
- 선행 화면: 프로젝트 목록 → 프로젝트 선택
- 전달 값: `projectId`

## 화면 동작 API

### 1. 프로젝트 정보 조회

- **호출 시점**: 화면 진입 시 (프로젝트 이름/설명 표시용)
- **메서드**: `ProjectController.viewProjectNonAdminDetail(projectId)`
- **파라미터**: projectId (long)
- **반환**: `ProjectResult`

| 필드 | 타입 | 설명 |
|---|---|---|
| id | long | 프로젝트 ID |
| name | String | 프로젝트 이름 |
| description | String | 프로젝트 설명 |
| managedByLoginId | String | 관리자 ID |
| createdDate | LocalDateTime | 생성일 |
| updatedAt | LocalDateTime | 수정일 |

### 2. 이슈 목록 조회

- **호출 시점**: 화면 진입 시
- **메서드**: `IssueController.viewRelatedProjectIssues(projectId)`
- **반환**: `List<IssueSummary>`

| 필드 | 타입 | 설명 |
|---|---|---|
| id | long | 이슈 내부 ID |
| issueId | String | 이슈 식별자 (ISSUE-1) |
| projectId | long | 프로젝트 ID |
| status | IssueStatus | 현재 상태 |
| priority | Priority | 우선순위 |
| title | String | 제목 |
| reporterId | String | 등록자 ID |
| assigneeId | String | 담당자 ID (nullable) |
| verifierId | String | 검증자 ID (nullable) |
| reportedDate | LocalDateTime | 등록일 |
| updatedAt | LocalDateTime | 수정일 |

> 프로젝트 멤버(PL/Dev/Tester)는 프로젝트 전체 일반 이슈를 볼 수 있고, 담당자 조건은 검색 필터로 좁힌다.

### 3. 이슈 검색

- **호출 시점**: 검색/필터 조건 입력 후
- **메서드**: `IssueController.searchIssues(projectId, keyword, status, priority)`
- **파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| projectId | long | Y | 프로젝트 ID |
| keyword | String | N | 제목/설명 검색어 |
| status | IssueStatus | N | 상태 필터 |
| priority | Priority | N | 우선순위 필터 |

- **반환**: `List<IssueSummary>`
- **확장 검색**: `searchIssues(projectId, keyword, status, priority, reporterId, assigneeId, verifierId, reportedFrom, reportedTo)`

### 4. 이슈 등록 가능 여부 확인

- **호출 시점**: 화면 진입 시 (등록 버튼 표시 여부)
- **메서드**: `IssueController.canRegisterIssue(projectId)`
- **반환**: `boolean`

### 5. 이슈 등록 (모달)

- **호출 시점**: 이슈 등록 버튼 → 모달에서 입력 후 확인
- **메서드**: `IssueController.registerIssue(projectId, title, description, priority)`
- **파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| projectId | long | Y | 프로젝트 ID |
| title | String | Y | 이슈 제목 |
| description | String | Y | 이슈 설명 |
| priority | Priority | N | 우선순위 (기본값 MAJOR) |

- **반환**: `IssueResult`
- **에러**:

| 조건 | 예외 |
|---|---|
| 같은 프로젝트에 동일 제목 존재 | IllegalArgumentException |
| 프로젝트 멤버 아님 | SecurityException |

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 이슈 클릭 | 이슈 상세 (issueId 전달) |
| 이슈 등록 완료 | 이슈 목록 새로고침 |
| 삭제 이슈 관리 버튼 (PL만) | 삭제 이슈 관리 |
| 통계 보기 버튼 | 통계 |
| 뒤로가기 | 프로젝트 목록 |
