# AuthenticationController API

## 범위

`AuthenticationController`는 세션 시작과 종료 오퍼레이션을 제공한다. 기존에 로그인된 현재 사용자가 없어도 호출할 수 있다.

## 공개 오퍼레이션

| 오퍼레이션 | 서비스 호출 | 결과 |
| --- | --- | --- |
| `login(loginId, password)` | `AuthenticationService.login(loginId, password)` | `AuthenticationResult` |
| `logout()` | `AuthenticationService.logout()` | `void` |

## 오퍼레이션 상세

`login`은 `success`, 선택적 `user`, `message`를 가진 `AuthenticationResult`를 반환한다. 이미 로그인된 세션이 있으면 실패 결과를 반환하고, 먼저 로그아웃해야 한다. 빈 id 또는 빈 password, 잘못된 인증 정보, 비활성 계정도 예외를 던지지 않고 실패 결과를 반환한다.

로그인 시 `loginId`는 앞뒤 공백을 제거한 값으로 사용자 조회에 사용한다. `password`는 trim하지 않고 입력값 그대로 `PasswordHashing.matches`에 전달한다. 로그인에 성공하면 `CurrentUserSession`을 통해 현재 세션이 시작된다.

`logout`은 현재 세션을 비우고 값을 반환하지 않는다.

## UC/OC/DCD 추적성

| API | UC/SSD/OC | DCD/domain 근거 |
| --- | --- | --- |
| `login` | UC11, SSD-15 log in; 필수 OC 목록에는 없는 보조 API | `docs/uml/dcd/its_dcd.puml`의 `User.loginId`, `passwordHash`, `role`, `active`; 구현 `AuthenticationService.login`, `AuthenticationResult` |
| `logout` | UC11 세션 보조 API | 구현 세션 경계 `AuthenticationService.logout`; 필수 OC 없음 |

## 구현 및 설계 차이

| 분류 | 내용 |
| --- | --- |
| `implementation-extra` | 로그인/로그아웃은 컨트롤러 API로 구현되어 있지만 필수 OC 목록 밖에 있다. |

## 권한 및 실패 요약

- `login`은 예상 가능한 인증 실패에 대해 예외를 던지지 않고 `AuthenticationResult.failure`를 반환한다.
- 이미 로그인된 세션이 있으면 `AuthenticationResult.failure("Already logged in. Please logout first.")`를 반환한다.
- `loginId` 또는 `password`가 null이거나 blank이면 `AuthenticationResult.failure("ID and password are required.")`를 반환한다.
- 존재하지 않는 id 또는 비밀번호 불일치이면 `AuthenticationResult.failure("Invalid ID or password.")`를 반환한다.
- 비활성 계정이면 `AuthenticationResult.failure("This account is inactive.")`를 반환한다.
- `logout`은 이미 로그아웃된 상태에서 다시 호출되어도 문제가 없으며 `CurrentUserSession.clear`에 위임한다.

## 근거

- `src/main/java/com/github/marcellokim/issuetracker/controller/AuthenticationController.java`: `AuthenticationController.login`, `logout`
- `src/main/java/com/github/marcellokim/issuetracker/service/AuthenticationService.java`: `AuthenticationService.login`, `logout`, `currentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/AuthenticationResult.java`: `AuthenticationResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/CurrentUserSession.java`: `CurrentUserSession`
- `src/main/java/com/github/marcellokim/issuetracker/service/PasswordHashing.java`: `PasswordHashing`
