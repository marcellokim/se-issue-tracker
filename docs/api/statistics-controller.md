# StatisticsController API

## Scope

`StatisticsController` exposes project statistics read APIs. This document is based on current `origin/dev` implementation.

## Public Operations

| Operation | Service call | Result |
| --- | --- | --- |
| `viewStatistics(projectId, dailyFromInclusive, dailyToInclusive, monthlyFromInclusive, monthlyToInclusive)` | `StatisticsService.viewStatistics(..., actor)` | `StatisticsReportResult` |
| `viewStatistics(projectId)` | delegates to the full overload with all ranges `null` | `StatisticsReportResult` |
| `canViewStatistics(projectId)` | `StatisticsService.canViewStatistics(projectId, actor)` | `boolean` |

## Operation Details

The controller requires a current user before all methods. The full `viewStatistics` overload accepts optional daily and monthly ranges; null range endpoints mean unbounded in the repository query. The convenience overload queries the full period.

`StatisticsReportResult` includes status counts, priority counts, daily/monthly issue counts, monthly status and priority counts, daily/monthly status-change counts, and daily/monthly comment counts.

## UC/OC/DCD Traceability

| API | UC/SSD/OC | DCD/domain evidence |
| --- | --- | --- |
| `viewStatistics` | UC10, SSD-14 view issue statistics; supporting API, not a required OC | DCD statistics are outside the core workflow DCD; implementation evidence is `StatisticsController.viewStatistics`, `StatisticsService.viewStatistics`, `StatisticsReportResult` |
| `canViewStatistics` | UC10/UC14 UI permission support API | `PermissionPolicy.assertCanViewStatistics`, `canViewStatistics`; active project membership check in `StatisticsService` |

## Implementation And Design Gaps

| Classification | Detail |
| --- | --- |
| `implementation-extra` | Statistics APIs are implemented and documented here, but the required OC list does not define statistics postconditions. |
| `behavior-drift` | `canViewStatistics` still requires a current user at the controller boundary; without login it throws `SecurityException` instead of returning `false`. |

## Permission And Failure Summary

- Requires login at controller boundary.
- `StatisticsService.viewStatistics` calls `PermissionPolicy.assertCanViewStatistics(actor, projectId)` and then requires active project membership.
- `PermissionPolicy.assertCanViewStatistics` allows active `PL`, `DEV`, and `TESTER`; ADMIN is not included.
- Throws `SecurityException("Only project members can view statistics.")` when the actor is not active in the project.
- Throws `IllegalArgumentException` when daily or monthly `from` is greater than `to`.

## Evidence

- `src/main/java/com/github/marcellokim/issuetracker/controller/StatisticsController.java`: `StatisticsController.viewStatistics`, `canViewStatistics`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/StatisticsService.java`: `StatisticsService.viewStatistics`, `canViewStatistics`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanViewStatistics`, `canViewStatistics`
- `src/main/java/com/github/marcellokim/issuetracker/service/StatisticsReportResult.java`: `StatisticsReportResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/DailyCountResult.java`: `DailyCountResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/MonthlyCountResult.java`: `MonthlyCountResult`