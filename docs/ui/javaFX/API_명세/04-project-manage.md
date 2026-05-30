# 04. 프로젝트 관리

## 진입 조건

- Actor: Admin
- 선행 화면: Admin 대시보드

## 화면 동작 API

### 1. 프로젝트 목록 조회

- **호출 시점**: 화면 진입 시
- **메서드**: `DashboardController.viewProjects()`
- **반환**: `List<DashboardProjectSummary>`

### 2. 프로젝트 생성 (모달)

- **호출 시점**: 프로젝트 생성 버튼 → 모달에서 입력 후 확인
- **메서드**: `ProjectController.createProject(name, description)`
- **파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| name | String | Y | 프로젝트 이름 |
| description | String | N | 프로젝트 설명 |

- **반환**: `ProjectResult`

| 필드 | 타입 | 설명 |
|---|---|---|
| id | long | 프로젝트 ID |
| name | String | 프로젝트 이름 |
| description | String | 프로젝트 설명 |
| managedByLoginId | String | 관리자 ID |
| createdDate | LocalDateTime | 생성일 |
| updatedAt | LocalDateTime | 수정일 |

- **에러**:

| 조건 | 예외 |
|---|---|
| 빈 이름 | IllegalArgumentException |
| Admin 권한 없음 | SecurityException |

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 프로젝트 선택 | 프로젝트 상세 |
| 프로젝트 생성 완료 | 프로젝트 목록 새로고침 |
| 뒤로가기 | Admin 대시보드 |
