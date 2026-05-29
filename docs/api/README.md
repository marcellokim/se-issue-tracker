# Implementation API Specification

Baseline: PR branch based on `origin/dev` at `ac149af chore: PR 제목 표준과 생성 스크립트 정리`.

## Scope

This directory documents the controller/service public contract currently implemented in the repository. It is reverse-extracted from implementation; it does not define new behavior.

Statistics controller documentation now lives in `docs/api/statistics-controller.md` and has been rechecked against current code.

## Controller Documents

| Controller | Document | Primary audience |
| --- | --- | --- |
| `AccountController` | [account-controller.md](account-controller.md) | Admin account UI and final traceability |
| `AssignmentController` | [assignment-controller.md](assignment-controller.md) | PL assignment UI and UC5/UC8 traceability |
| `AuthenticationController` | [authentication-controller.md](authentication-controller.md) | Login/session UI |
| `DashboardController` | [dashboard-controller.md](dashboard-controller.md) | Dashboard and navigation UI |
| `DeletedIssueController` | [deleted-issue-controller.md](deleted-issue-controller.md) | Deleted issue management UI and UC9 traceability |
| `IssueController` | [issue-controller.md](issue-controller.md) | Core issue/comment/dependency UI |
| `IssueStateController` | [issue-state-controller.md](issue-state-controller.md) | State transition UI and UC6 traceability |
| `ProjectController` | [project-controller.md](project-controller.md) | Admin project management UI |
| `StatisticsController` | [statistics-controller.md](statistics-controller.md) | Statistics UI and refreshed API docs |

## Public Operation Inventory

| Controller | Public operations |
| --- | --- |
| `AccountController` | `createAccount`, `updateAccount`, `renameAccount`, `changeAccountRole`, `activateAccount`, `deactivateAccount` |
| `AssignmentController` | `startAssignment`, `assignIssue`, `reassignIssue`, `changeVerifier` |
| `AuthenticationController` | `login`, `logout` |
| `DashboardController` | `viewRelatedIssues` (disabled), `viewProjects`, `viewUsers` |
| `DeletedIssueController` | `viewDeletedIssues`, `deleteIssue`, `restoreIssue`, `purgeOverflow` |
| `IssueController` | `registerIssue`, `canRegisterIssue`, `viewIssueDetail`, `searchIssues`, `viewRelatedProjectIssues`, `updateIssue`, `changePriority`, `addComment`, `viewComments`, `addDependency`, `viewProjectDependencies`, `removeDependency`, `deleteComment`, `updateComment`, `viewAvailableActions`, `canUpdateComment`, `canDeleteComment` |
| `IssueStateController` | `changeStatus` |
| `ProjectController` | `create`, `viewProjects`, `viewProject`, `viewProjectParticipants`, `viewProjectDetail`, `createProject`, `deleteProject`, `addProjectParticipant`, `removeProjectParticipant` |
| `StatisticsController` | `viewStatistics`, `canViewStatistics` |

`ProjectController.create(...)` is a static factory, not a UI operation.

## UC/OC Mapping Summary

| UC/OC | Implemented API |
| --- | --- |
| UC1 / OC-01 Register Issue | `IssueController.registerIssue` |
| UC2 / OC-02 Add Comment | `IssueController.addComment` |
| UC3 Browse/Search Issues | `IssueController.searchIssues`, `viewRelatedProjectIssues` |
| UC4 View Issue Detail | `IssueController.viewIssueDetail` |
| UC5 / OC-03, OC-04, OC-05, OC-12 Assignment | `AssignmentController.assignIssue`, `reassignIssue`, `changeVerifier` |
| UC6 / OC-06, OC-07, OC-08, OC-09, OC-13 State change | `IssueStateController.changeStatus` |
| UC7 / OC-14, OC-15 Dependency | `IssueController.addDependency`, `removeDependency` |
| UC8 Recommend Assignment Candidates | `AssignmentController.startAssignment` |
| UC9 / OC-10, OC-11 Deleted issue | `DeletedIssueController.deleteIssue`, `restoreIssue` |
| UC10 Statistics | `StatisticsController.viewStatistics`, `canViewStatistics` |
| UC11 Log In | `AuthenticationController.login`, `logout` |
| UC12 Manage Accounts | `AccountController.createAccount`, `updateAccount`, `renameAccount`, `changeAccountRole`, `activateAccount`, `deactivateAccount` |
| UC13 Manage Projects | `ProjectController.createProject`, `deleteProject`, `addProjectParticipant`, `removeProjectParticipant`, project view APIs |
| UC14 Verify Permission | `PermissionPolicy.assertCan...` and `can...` methods reached from protected controller APIs |
| UC15 Edit Issue | `IssueController.updateIssue` |
| UC16 / OC-16 Change Priority | `IssueController.changePriority` |
| Supporting APIs | dashboard and workflow action-query APIs |

## DCD / Domain Evidence Summary

| Area | DCD/domain evidence |
| --- | --- |
| Issue workflows | `docs/uml/dcd/its_dcd_ver2.puml`: `IssueController`, `Issue`, `Comment`, `IssueHistory`, `IssueDependency`, `PermissionPolicy` |
| Assignment workflows | `docs/uml/dcd/its_dcd_ver2.puml`: `AssignmentController`, `AssignmentRecommendationService`, `Issue.assignFromNew`, `assignReopened`, `reassignAssignee`, `changeVerifier` |
| State transitions | `docs/uml/dcd/its_dcd_ver2.puml`: `IssueStateController`, `Issue.markFixed`, `resolve`, `rejectFix`, `close`, `reopen`, `IssueHistory(STATUS_CHANGED)` |
| Deleted issue workflow | `docs/uml/dcd/its_dcd_ver2.puml`: `DeletedIssueController`, `Issue.softDelete`, `restore`, `findDeleteStatusHistory`, dependency removal |
| Admin/support workflows | `docs/uml/dcd/its_dcd_ver2.puml`: `Project`, `User`, `PermissionPolicy`, plus implementation service/result classes not shown in the core workflow DCD |

## Design Gap Summary

| Classification | Notes |
| --- | --- |
| `implementation-extra` | Dashboard, account, project, statistics, comment edit/delete, workflow action-query, and rich issue search APIs are implemented but are not all represented as required OCs. |
| `signature-drift` | OC-15 names `removeDependency(dependencyId)`, while the implemented controller uses `removeDependency(blockingIssueId, blockedIssueId)`. |
| `behavior-drift` | `deleteIssue` and `restoreIssue` accept a `comment` argument in implementation; OC names omit it. |
| `matches` | Core register, add comment, assignment, state transition, dependency add, deleted issue, and priority operations have controller/service paths. |

## Evidence Convention

Each controller document cites code as `file path + method name`, not line numbers. Line numbers change too easily; method-level evidence remains stable enough for PR review and traceability.
