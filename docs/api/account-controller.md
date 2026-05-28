# AccountController API

## Scope

`AccountController` exposes ADMIN account-management operations. Every operation requires a current logged-in user and passes the domain `User` actor to `AccountService`.

## Public Operations

| Operation | Service call | Result |
| --- | --- | --- |
| `createAccount(loginId, name, password, role)` | `AccountService.createAccount(loginId, name, password, role, actor)` | `UserResult` |
| `updateAccount(loginId, name, role)` | `AccountService.updateAccount(loginId, name, role, actor)` | `UserResult` |
| `renameAccount(loginId, name)` | `AccountService.renameAccount(loginId, name, actor)` | `UserResult` |
| `changeAccountRole(loginId, role)` | `AccountService.changeAccountRole(loginId, role, actor)` | `UserResult` |
| `activateAccount(loginId)` | `AccountService.activateAccount(loginId, actor)` | `UserResult` |
| `deactivateAccount(loginId)` | `AccountService.deactivateAccount(loginId, actor)` | `UserResult` |

## Operation Details

All operations call `AuthenticationService.currentUser` first. If no session exists, the controller throws `SecurityException("Login is required.")`.

`UserResult` contains `loginId`, `name`, `role`, `active`, `createdAt`, and `updatedAt`.

Account creation hashes the password through `PasswordHasher`, rejects duplicate trimmed login ids, rejects `ADMIN` role creation, and rejects the reserved `admin` login id. Update and role-change operations reject self-management, ADMIN targets, ADMIN target roles, and role changes for users with project membership or active issue responsibility. Deactivation uses the same responsibility guard.

## UC/OC/DCD Traceability

| API | UC/SSD/OC | DCD/domain evidence |
| --- | --- | --- |
| `createAccount` | UC12, SSD-16; supporting API, not part of required OC list | `User` role/active fields in `docs/uml/dcd/its_dcd_ver2.puml`; implementation `AccountService.createAccount`, `User.create`, `UserResult.from` |
| `updateAccount`, `renameAccount`, `changeAccountRole` | UC12, SSD-17; supporting API, not part of required OC list | `User` role/name lifecycle in DCD; implementation `User.rename`, `User.changeRole`, `AccountService.rejectRoleChangeWithProjectResponsibility` |
| `activateAccount`, `deactivateAccount` | UC12, SSD-18; supporting API, not part of required OC list | `User.isActive` in DCD; implementation `User.activate`, `User.deactivate`, `AccountService.rejectDeactivationWithProjectResponsibility` |

## Implementation And Design Gaps

| Classification | Detail |
| --- | --- |
| `implementation-extra` | Account APIs are implemented in controller/service but not listed in `required_operation_contracts.md`. |
| `behavior-drift` | The implementation forbids creating or modifying ADMIN accounts and forbids self-management; those are service policy details beyond the visible controller signature. |

## Permission And Failure Summary

- Requires login at controller boundary.
- Requires `PermissionPolicy.assertCanManageAccount`, which allows only active ADMIN users.
- Throws `IllegalArgumentException` for duplicate accounts, missing target accounts, ADMIN account operations, self-management, role-change/deactivation responsibility conflicts, and null `role`.
- Throws `SecurityException` when the current user is missing or is not ADMIN.

## Evidence

- `src/main/java/com/github/marcellokim/issuetracker/controller/AccountController.java`: `AccountController.createAccount`, `updateAccount`, `renameAccount`, `changeAccountRole`, `activateAccount`, `deactivateAccount`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/AccountService.java`: `AccountService.createAccount`, `updateAccount`, `renameAccount`, `changeAccountRole`, `activateAccount`, `deactivateAccount`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanManageAccount`
- `src/main/java/com/github/marcellokim/issuetracker/service/UserResult.java`: `UserResult`