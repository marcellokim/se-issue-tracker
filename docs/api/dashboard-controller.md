# DashboardController API

## Scope

`DashboardController` exposes read-model APIs for the first screen and navigation. Project and user data are read through `DashboardSummaryService`. Related issue lookup is handled by `IssueController.viewRelatedProjectIssues`, not by the dashboard controller.

## Public Operations

| Operation | Service call | Result |
| --- | --- | --- |
| `viewProjects()` | `DashboardSummaryService.projectSummariesFor(currentUser)` | `List<DashboardProjectSummary>` |
| `viewUsers()` | `DashboardSummaryService.usersFor(currentUser)` | `List<UserResult>` |

## Operation Details

The controller requires a current user for every operation. ADMIN users can see all projects and users. Non-ADMIN users see project summaries for projects where they are participants.

`DashboardProjectSummary` includes project identity, member counts by role, visible/deleted issue counts, and current status counts.

## UC/OC/DCD Traceability

| API | UC/SSD/OC | DCD/domain evidence |
| --- | --- | --- |
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
- Throws `SecurityException("Login is required.")` if no current user exists.

## Evidence

- `src/main/java/com/github/marcellokim/issuetracker/controller/DashboardController.java`: `DashboardController.viewProjects`, `viewUsers`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/DashboardSummaryService.java`: `DashboardSummaryService.projectSummariesFor`, `usersFor`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.canViewAllProjects`, `canViewAllUsers`
- `src/main/java/com/github/marcellokim/issuetracker/service/DashboardProjectSummary.java`: `DashboardProjectSummary`
