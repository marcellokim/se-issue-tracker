# AccountController API

## 범위

`AccountController`는 ADMIN에게 계정 관리 기능을 제공한다. 모든 기능은 현재 로그인한 사용자(ADMIN)가 필요하며, 도메인 `User` actor를 `AccountService`로 전달한다.

## 공개 오퍼레이션

| 오퍼레이션 | 서비스 호출 | 결과 |
| --- | --- | --- |
| `createAccount(loginId, name, password, role)` | `AccountService.createAccount(loginId, name, password, role, actor)` | `UserResult` |
| `renameAccount(loginId, name)` | `AccountService.renameAccount(loginId, name, actor)` | `UserResult` |
| `changeAccountRole(loginId, role)` | `AccountService.changeAccountRole(loginId, role, actor)` | `UserResult` |
| `activateAccount(loginId)` | `AccountService.activateAccount(loginId, actor)` | `UserResult` |
| `deactivateAccount(loginId)` | `AccountService.deactivateAccount(loginId, actor)` | `UserResult` |

## 오퍼레이션 상세

모든 오퍼레이션은 먼저 `AuthenticationService.currentUser`를 호출한다. 세션이 없으면 컨트롤러는 `SecurityException("Login is required.")`를 던진다.

`UserResult`는 `loginId`, `name`, `role`, `active`, `createdAt`, `updatedAt`을 포함한다.

계정 생성은 `PasswordHashing` 포트를 통해 비밀번호를 해시하고 trim된 login id 중복을 거부하며, `ADMIN` role 생성과 예약된 `admin` login id를 거부한다. `loginId`와 `name`은 앞뒤 공백을 제거한 값을 사용하지만, `password`는 null/blank만 거부하고 trim하지 않는다. 이름 변경과 역할 변경은 자기 자신 관리, ADMIN 대상, ADMIN 대상 role, 프로젝트 멤버십 또는 현재 활성 이슈 책임을 가진 사용자의 역할 변경을 거부한다. 비활성화는 프로젝트 멤버십이 있거나 현재 `ASSIGNED`/`FIXED` 상태 이슈의 assignee/verifier 책임이 있는 계정을 거부한다.

## UC/OC/DCD 추적성

| API | UC/SSD/OC | DCD/domain 근거 |
| --- | --- | --- |
| `createAccount` | UC12, SSD-16; 필수 OC 목록에는 없는 보조 API | `docs/uml/dcd/its_dcd_ver2.puml`의 `User` role/active 필드; 구현 `AccountService.createAccount`, `User.create`, `UserResult.from` |
| `renameAccount`, `changeAccountRole` | UC12, SSD-17; 필수 OC 목록에는 없는 보조 API | DCD의 `User` role/name 생명주기; 구현 `User.rename`, `User.changeRole`, `AccountService.rejectRoleChangeWithProjectResponsibility` |
| `activateAccount`, `deactivateAccount` | UC12, SSD-18; 필수 OC 목록에는 없는 보조 API | DCD의 `User.isActive`; 구현 `User.activate`, `User.deactivate`, `AccountService.rejectDeactivationWithProjectResponsibility` |

## 구현 및 설계 차이

| 분류 | 내용 |
| --- | --- |
| `implementation-extra` | 계정 API는 controller/service에 구현되어 있지만 `required_operation_contracts.md`에는 나열되어 있지 않다. |
| `behavior-drift` | 구현은 ADMIN 계정 생성 또는 수정을 금지하고 자기 자신 관리를 금지한다. 이는 컨트롤러 시그니처만으로는 보이지 않는 서비스 정책 상세이다. |

## 권한 및 실패 요약

- 컨트롤러 경계에서 로그인이 필요하다.
- active ADMIN 사용자만 허용하는 `PermissionPolicy.assertCanManageAccount`가 필요하다.
- 중복 계정, 존재하지 않는 대상 계정, ADMIN 계정 작업, 자기 자신 관리, 같은 이름으로 이름 변경, 같은 role로 역할 변경, 이미 active인 계정 활성화, 이미 inactive인 계정 비활성화, 역할 변경/비활성화 책임 충돌은 `IllegalArgumentException`을 던진다.
- 역할 변경/비활성화 책임 충돌은 프로젝트 멤버십 또는 현재 `ASSIGNED`/`FIXED` 이슈의 assignee/verifier 책임을 의미한다.
- 계정 생성 또는 역할 변경에서 `role`이 null이면 `NullPointerException("role must not be null")`을 던진다.
- 현재 사용자가 없거나 ADMIN이 아니면 `SecurityException`을 던진다.

## 근거

- `src/main/java/com/github/marcellokim/issuetracker/controller/AccountController.java`: `AccountController.createAccount`, `renameAccount`, `changeAccountRole`, `activateAccount`, `deactivateAccount`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/AccountService.java`: `AccountService.createAccount`, `renameAccount`, `changeAccountRole`, `activateAccount`, `deactivateAccount`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanManageAccount`
- `src/main/java/com/github/marcellokim/issuetracker/service/UserResult.java`: `UserResult`
