# IssueController API

## Scope

`IssueController` is the main issue, comment, dependency, search, and workflow-action API boundary. All UI operations require a current user. Several methods delegate to `IssueService`; action-query methods delegate to `IssueWorkflowService`.

## Public Operations

| Operation | Service call | Result |
| --- | --- | --- |
| `registerIssue(projectId, title, description, priority)` | `IssueService.registerIssue(..., currentUserId)` | `IssueResult` |
| `canRegisterIssue(projectId)` | `IssueService.canRegisterIssue(projectId, currentUserId)` | `boolean` |
| `viewIssueDetail(issueId)` | `IssueService.viewIssueDetail(issueId, currentUserId)`, optionally `IssueWorkflowService.viewAvailableActions` | `IssueDetailResult` |
| `searchIssues(projectId, keyword, status, priority)` | full `searchIssues` overload with extra filters as `null` | `List<IssueSummary>` |
| `searchIssues(projectId, keyword, status, priority, reporterId, assigneeId, verifierId, reportedFrom, reportedTo)` | `IssueService.searchIssues(...)` | `List<IssueSummary>` |
| `viewRelatedProjectIssues(projectId)` | `IssueService.viewRelatedProjectIssues(projectId, currentUserId)` | `List<IssueSummary>` |
| `updateIssue(issueId, title, description)` | `IssueService.updateIssue(issueId, title, description, currentUserId)` | `IssueResult` |
| `changePriority(issueId, priority)` | `IssueService.changePriority(issueId, priority, currentUserId)` | `IssueResult` |
| `addComment(issueId, content)` | `IssueService.addComment(issueId, content, currentUserId)` | `CommentResult` |
| `viewComments(issueId)` | `IssueService.viewComments(issueId, currentUserId)` | `List<CommentResult>` |
| `addDependency(blockingIssueId, blockedIssueId)` | `IssueService.addDependency(blockingIssueId, blockedIssueId, currentUserId)` | `DependencyResult` |
| `viewProjectDependencies(projectId)` | `IssueService.viewProjectDependencies(projectId, currentUserId)` | `List<DependencyResult>` |
| `removeDependency(blockingIssueId, blockedIssueId)` | `IssueService.removeDependency(blockingIssueId, blockedIssueId, currentUserId)` | `void` |
| `deleteComment(issueId, commentId)` | `IssueService.deleteComment(issueId, commentId, currentUserId)` | `void` |
| `updateComment(issueId, commentId, content)` | `IssueService.updateComment(issueId, commentId, content, currentUserId)` | `CommentResult` |
| `viewAvailableActions(issueId)` | `IssueWorkflowService.viewAvailableActions(issueId, currentUserId)` | `IssueWorkflowActions` |
| `canUpdateComment(issueId, commentId)` | `IssueWorkflowService.canUpdateComment(issueId, commentId, currentUserId)` | `boolean` |
| `canDeleteComment(issueId, commentId)` | `IssueWorkflowService.canDeleteComment(issueId, commentId, currentUserId)` | `boolean` |

## Operation Details

`registerIssue` checks project existence, active auth-user role, active project membership, and duplicate title. Null priority defaults to `Priority.MAJOR`.

Search and detail APIs require active project membership and non-deleted issues. `viewIssueDetail` returns comments, histories, dependencies, and optionally computed action names when the controller was constructed with `IssueWorkflowService`.

`viewRelatedProjectIssues` treats reporter, current assignee, and current verifier as related participants. Fixer and resolver are completion history and do not make an issue a current related issue.

`updateIssue` is limited to the issue reporter and `NEW` or `REOPENED` status. `changePriority` is PL-only. General comment edit/delete is limited to the comment writer and only for `CommentPurpose.GENERAL`.

Dependency add/remove is PL-only, same-project only, rejects self-dependency, duplicate dependency, and circular dependency.

## UC/OC/DCD Traceability

| API | UC/SSD/OC | DCD/domain evidence |
| --- | --- | --- |
| `registerIssue` | UC1, OC-01, SSD-01 | `IssueController.registerIssue`, `Issue.create`, `IssueHistory(CREATED)`, reporter association in `docs/uml/dcd/its_dcd_ver2.puml` |
| `addComment` | UC2, OC-02, SSD-02 | `Issue.addComment`, `Comment`, `IssueHistory(COMMENTED)`, writer association |
| `addDependency` | UC7, OC-14, SSD-24 | `Issue.addDependency`, `IssueDependency`, blocking/blocked issue associations, `IssueHistory(DEPENDENCY_CHANGED)` |
| `removeDependency` | UC7, OC-15, SSD-25 | DCD names `removeDependency(dependencyId)`; implementation uses `IssueService.removeDependency(blockingIssueId, blockedIssueId)` and `Issue.removeDependency` |
| `changePriority` | UC16, OC-16, SSD-27 | `Issue.changePriority`, `Issue.verifyPriorityChange`, `IssueHistory(PRIORITY_CHANGED)`; status and role associations unchanged |
| `viewIssueDetail` | UC4, SSD-04 and UI support | `Issue`, `Comment`, `IssueHistory`, `IssueDependency`, role associations returned through `IssueDetailResult` |
| `searchIssues`, `viewRelatedProjectIssues` | UC3, SSD-03 and UI support | DCD `Issue` attributes status/priority/reporter/assignee/verifier; implementation `IssueSearchCriteria`, `IssueSummary` |
| `updateIssue` | UC15, SSD-23 edit issue support | `Issue.updateTitleAndDescription`, `IssueHistory(TITLE_DESCRIPTION_UPDATED)`; priority/status handled by separate UCs |
| `deleteComment`, `updateComment`, `canUpdateComment`, `canDeleteComment`, `viewAvailableActions` | Implementation support APIs without direct required OC | `Comment`, `IssueWorkflowActions`, `PermissionPolicy.assertCan...` methods |

## Implementation And Design Gaps

| Classification | Detail |
| --- | --- |
| `matches` | Register, add comment, add dependency, and change priority are controller-wired and service-enforced. |
| `signature-drift` | OC-15 documents `removeDependency(dependencyId)`, while implementation exposes `removeDependency(blockingIssueId, blockedIssueId)`. |
| `implementation-extra` | Search, detail, action-query, comment update/delete, and related-project issue APIs are implemented beyond the required OC list. |

## Permission And Failure Summary

- Requires login at controller boundary.
- `registerIssue`, search, detail, comments, and dependency viewing require active project membership.
- `changePriority`, dependency add/remove, and assignment-related action flags require project PL membership.
- `requireIssueWorkflowService` throws `IllegalStateException("Issue workflow service is not configured.")` if action-query methods are called on a controller constructed without workflow service.
- Missing project, issue, user, comment, or dependency records throw `IllegalArgumentException`.
- Deleted issues throw `SecurityException("Deleted issues must be managed through deleted issue workflow.")` for normal issue APIs.

## Evidence

- `src/main/java/com/github/marcellokim/issuetracker/controller/IssueController.java`: all public `IssueController` methods, `requireCurrentUser`, `requireIssueWorkflowService`, `availableActionNames`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueService.java`: `IssueService.registerIssue`, `canRegisterIssue`, `viewIssueDetail`, `searchIssues`, `viewRelatedProjectIssues`, `updateIssue`, `changePriority`, `addComment`, `viewComments`, `addDependency`, `viewProjectDependencies`, `removeDependency`, `deleteComment`, `updateComment`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueWorkflowService.java`: `IssueWorkflowService.viewAvailableActions`, `canUpdateComment`, `canDeleteComment`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: issue/comment/dependency/priority permission methods
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueResult.java`: `IssueResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueDetailResult.java`: `IssueDetailResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/IssueWorkflowActions.java`: `IssueWorkflowActions`
