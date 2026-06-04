# 03. 계정 관리

## 진입 조건

- Actor: Admin
- 선행 화면: Admin 대시보드

## 화면 동작 API

### 1. 사용자 목록 조회

- **호출 시점**: 화면 진입 시
- **메서드**: `DashboardController.viewUsers()`
- **반환**: `List<UserResult>`

### 2. 계정 생성 (모달)

- **호출 시점**: 계정 생성 버튼 → 모달에서 입력 후 확인
- **메서드**: `AccountController.createAccount(loginId, name, password, role)`
- **파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| loginId | String | Y | 새 사용자 ID |
| name | String | Y | 사용자 이름 |
| password | String | Y | 비밀번호 |
| role | Role | Y | ADMIN / PL / DEV / TESTER |

- **반환**: `UserResult`
- **에러**:

| 조건 | 예외 |
|---|---|
| 중복 loginId | IllegalArgumentException |
| 빈 값 | IllegalArgumentException |
| Admin 권한 없음 | SecurityException |

### 3. 계정 이름 변경

- **메서드**: `AccountController.renameAccount(loginId, name)`
- **반환**: `UserResult`

### 4. 계정 역할 변경

- **메서드**: `AccountController.changeAccountRole(loginId, role)`
- **반환**: `UserResult`

### 5. 계정 활성화

- **메서드**: `AccountController.activateAccount(loginId)`
- **반환**: `UserResult`

### 6. 계정 비활성화

- **메서드**: `AccountController.deactivateAccount(loginId)`
- **반환**: `UserResult`
- **에러**:

| 조건 | 예외 |
|---|---|
| 현재 이슈 책임이 있는 사용자 | SecurityException |

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 뒤로가기 | Admin 대시보드 |
| 계정 생성 완료 | 사용자 목록 새로고침 |
| 계정 수정 완료 | 사용자 목록 새로고침 |
