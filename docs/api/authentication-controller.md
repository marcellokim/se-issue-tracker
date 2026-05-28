# AuthenticationController API

## Scope

`AuthenticationController` exposes session entry and exit operations. It does not require an existing current user.

## Public Operations

| Operation | Service call | Result |
| --- | --- | --- |
| `login(loginId, password)` | `AuthenticationService.login(loginId, password)` | `AuthenticationResult` |
| `logout()` | `AuthenticationService.logout()` | `void` |

## Operation Details

`login` returns an `AuthenticationResult` with `success`, optional `user`, and `message`. Blank id/password returns a failure result instead of throwing. Invalid credentials and inactive accounts also return failure results. Successful login starts a session in `SessionStore`.

`logout` clears the current session and returns no value.

## UC/OC/DCD Traceability

| API | UC/SSD/OC | DCD/domain evidence |
| --- | --- | --- |
| `login` | UC11, SSD-15 log in; supporting API, not part of required OC list | `User.loginId`, `passwordHash`, `role`, `isActive` in `docs/uml/dcd/its_dcd_ver2.puml`; implementation `AuthenticationService.login`, `AuthenticationResult` |
| `logout` | UC11 session support API | Implementation session boundary `AuthenticationService.logout`; no required OC |

## Implementation And Design Gaps

| Classification | Detail |
| --- | --- |
| `implementation-extra` | Login/logout are implemented controller APIs but are outside the required OC list. |

## Permission And Failure Summary

- `login` does not throw for expected credential failures; it returns `AuthenticationResult.failure`.
- `logout` is idempotent at the controller boundary and delegates to `SessionStore.clear`.

## Evidence

- `src/main/java/com/github/marcellokim/issuetracker/controller/AuthenticationController.java`: `AuthenticationController.login`, `logout`
- `src/main/java/com/github/marcellokim/issuetracker/service/AuthenticationService.java`: `AuthenticationService.login`, `logout`, `currentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/AuthenticationResult.java`: `AuthenticationResult`
