# 02. Admin 대시보드

## 진입 조건

- Actor: Admin
- 선행 화면: 로그인 (`user.role=ADMIN`)

## 화면 동작 API

### 1. 전체 프로젝트 목록 조회

- **호출 시점**: 화면 진입 시
- **메서드**: `DashboardController.viewProjects()`
- **파라미터**: 없음
- **반환**: `List<DashboardProjectSummary>`

| 필드 | 타입 | 설명 |
|---|---|---|
| projectId | long | 프로젝트 ID |
| projectName | String | 프로젝트 이름 |
| projectDescription | String | 프로젝트 설명 |
| memberCount | int | 전체 참여자 수 |
| projectLeaderCount | int | PL 수 |
| developerCount | int | DEV 수 |
| testerCount | int | TESTER 수 |
| visibleIssueCount | int | 보이는 이슈 수 (DELETED 제외) |
| statusCounts | Map\<IssueStatus, Integer\> | 상태별 이슈 수 |

### 2. 전체 사용자 목록 조회

- **호출 시점**: 화면 진입 시
- **메서드**: `DashboardController.viewUsers()`
- **파라미터**: 없음
- **반환**: `List<UserResult>`

| 필드 | 타입 | 설명 |
|---|---|---|
| loginId | String | 사용자 ID |
| name | String | 사용자 이름 |
| role | Role | 역할 |
| active | boolean | 활성 여부 |
| createdAt | LocalDateTime | 생성일 |
| updatedAt | LocalDateTime | 수정일 |

### 3. 로그아웃

- **호출 시점**: 로그아웃 버튼 클릭
- **메서드**: `AuthenticationController.logout()`
- **파라미터**: 없음
- **반환**: void

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 계정 관리 버튼 | 계정 관리 |
| 프로젝트 관리 버튼 | 프로젝트 관리 |
| 로그아웃 | 로그인 화면 |
