# 05. 프로젝트 상세 (Admin)

## 진입 조건

- Actor: Admin
- 선행 화면: 프로젝트 관리 → 프로젝트 선택

## 화면 동작 API

### 1. 프로젝트 상세 + 멤버 조회 (초기 로딩)

- **호출 시점**: 화면 진입 시
- **메서드**: `ProjectController.viewProjectAdminDetail(projectId)`
- **파라미터**: projectId (long)
- **반환**: `ProjectAdminDetail`

| 필드 | 타입 | 설명 |
|---|---|---|
| project | ProjectResult | 프로젝트 정보 |
| participants | List\<ProjectMemberResult\> | 멤버 목록 |

- `ProjectMemberResult` 상세:

| 필드 | 타입 | 설명 |
|---|---|---|
| projectId | long | 프로젝트 ID |
| userId | String | 사용자 ID |
| userName | String | 사용자 이름 |
| role | Role | 역할 |
| active | boolean | 활성 여부 |
| joinedAt | LocalDateTime | 가입일 |

> 초기 로딩 시 `viewProjectAdminDetail()`이 프로젝트 정보 + 멤버 목록을 함께 반환하므로 `viewProjectParticipants()`를 별도 호출하지 않는다.

### 2. 멤버 목록 새로고침

- **호출 시점**: 멤버 추가/제거 후 부분 새로고침
- **메서드**: `ProjectController.viewProjectParticipants(projectId)`
- **반환**: `List<ProjectMemberResult>`

### 3. 프로젝트 이름 변경

- **메서드**: `ProjectController.renameProject(projectId, name)`
- **반환**: `ProjectResult`

### 4. 프로젝트 설명 변경

- **메서드**: `ProjectController.changeProjectDescription(projectId, description)`
- **반환**: `ProjectResult`

### 5. 프로젝트 삭제

- **메서드**: `ProjectController.deleteProject(projectId)`
- **반환**: void

### 6. 멤버 추가

- **메서드**: `ProjectController.addProjectParticipant(projectId, loginId)`
- **반환**: void
- **후속**: 멤버 목록 새로고침 (`viewProjectParticipants`)

### 7. 멤버 제거

- **메서드**: `ProjectController.removeProjectParticipant(projectId, loginId)`
- **반환**: void
- **에러**:

| 조건 | 예외 |
|---|---|
| 해당 사용자가 진행 중 이슈 책임이 있음 | SecurityException |

- **후속**: 멤버 목록 새로고침

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 뒤로가기 | 프로젝트 관리 |
| 프로젝트 삭제 완료 | 프로젝트 관리 |
