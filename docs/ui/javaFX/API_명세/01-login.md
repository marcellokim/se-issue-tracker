# 01. 로그인 화면

## 진입 조건

- Actor: Admin | PL | Dev | Tester (미인증)
- 선행 화면: 없음 (앱 시작 시) 또는 로그아웃 후

## 화면 동작 API

### 1. 로그인

- **호출 시점**: 로그인 버튼 클릭
- **메서드**: `AuthenticationController.login(loginId, password)`
- **파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| loginId | String | Y | 사용자 ID |
| password | String | Y | 비밀번호 |

- **반환**: `AuthenticationResult`

| 필드 | 타입 | 설명 |
|---|---|---|
| success | boolean | 로그인 성공 여부 |
| user | UserResult | 성공 시 사용자 정보 (실패 시 null) |
| message | String | 결과 메시지 |

- `UserResult` 상세:

| 필드 | 타입 | 설명 |
|---|---|---|
| loginId | String | 사용자 ID |
| name | String | 사용자 이름 |
| role | Role | ADMIN / PL / DEV / TESTER |
| active | boolean | 활성 여부 |

- **에러**:

| 조건 | 결과 |
|---|---|
| ID/PW 공백 | `success=false`, message="ID and password are required." |
| 사용자 없음 | `success=false`, message="Invalid ID or password." |
| 비밀번호 불일치 | `success=false`, message="Invalid ID or password." |
| 비활성 계정 | `success=false`, message="This account is inactive." |
| 이미 로그인 | `success=false`, message="Already logged in. Please logout first." |

## 화면 전이

| 조건 | 다음 화면 | 분기 기준 |
|---|---|---|
| `success=true` && `user.role=ADMIN` | Admin 대시보드 | `result.user().role()` |
| `success=true` && `user.role!=ADMIN` | 프로젝트 목록 | `result.user().role()` |
| `success=false` | 로그인 화면 유지 | 에러 메시지 표시 |
