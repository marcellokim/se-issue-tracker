# DashboardController API

## Scope

`DashboardController` exposes read-model APIs for the first screen and navigation. Project and user data are read through `DashboardSummaryService`; the related-issue dashboard entry point is currently disabled.

## Public Operations

| Operation | Service call | Result |
| --- | --- | --- |
| `viewRelatedIssues()` | Disabled; requires current user and returns an empty list | `List<IssueSummary>` |
| `viewProjects()` | `DashboardSummaryService.projectSummariesFor(currentUser)` | `List<DashboardProjectSummary>` |
| `viewUsers()` | `DashboardSummaryService.usersFor(currentUser)` | `List<UserResult>` |

## Operation Details

The controller requires a current user for every operation. `viewRelatedIssues()` is disabled and returns an empty list after authentication. ADMIN users can see all projects and users. Non-ADMIN users see project summaries for projects where they are participants.

`DashboardProjectSummary` includes project identity, member counts by role, visible/deleted issue counts, and current status counts.

## UC/OC/DCD Traceability

| API | UC/SSD/OC | DCD/domain evidence |
| --- | --- | --- |
| `viewRelatedIssues` | Disabled dashboard entry point; UC3 browse/search remains under `IssueController.searchIssues` and `viewRelatedProjectIssues` | Implementation `DashboardController.viewRelatedIssues` returns an empty list after login check |
| `viewProjects` | Supports project selection/navigation; no direct required OC | DCD `Project`, `User participates in Project`; implementation `DashboardSummaryService.projectSummariesFor`, `DashboardProjectSummary` |
| `viewUsers` | Admin dashboard support; no direct required OC | DCD `User`; implementation `DashboardSummaryService.usersFor`, `PermissionPolicy.canViewAllUsers` |

## Implementation And Design Gaps

| Classification | Detail |
| --- | --- |
| `implementation-extra` | Dashboard aggregation is implemented as a service read model but is not represented in required OCs. |

## Permission And Failure Summary

- Requires login at controller boundary.
- Uses `PermissionPolicy.canViewAllProjects` and `canViewAllUsers` for dashboard project/user filtering.
- Non-admin `viewUsers` returns an empty list instead of throwing.
- `viewRelatedIssues` returns an empty list for authenticated users.
- Throws `SecurityException("Login is required.")` if no current user exists.

## Evidence

- `src/main/java/com/github/marcellokim/issuetracker/controller/DashboardController.java`: `DashboardController.viewRelatedIssues`, `viewProjects`, `viewUsers`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/DashboardSummaryService.java`: `DashboardSummaryService.projectSummariesFor`, `usersFor`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.canViewAllProjects`, `canViewAllUsers`
- `src/main/java/com/github/marcellokim/issuetracker/service/DashboardProjectSummary.java`: `DashboardProjectSummary`
