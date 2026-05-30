# 06. 프로젝트 목록 (Auth User)

## 진입 조건

- Actor: PL | Dev | Tester
- 선행 화면: 로그인 (`user.role != ADMIN`)

## 화면 동작 API

### 1. 참여 프로젝트 목록 조회

- **호출 시점**: 화면 진입 시
- **메서드**: `DashboardController.viewProjects()`
- **반환**: `List<DashboardProjectSummary>` (참여한 프로젝트만 필터링됨)

### 2. 로그아웃

- **메서드**: `AuthenticationController.logout()`

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 프로젝트 선택 | 이슈 목록 (선택한 projectId 전달) |
| 로그아웃 | 로그인 화면 |
