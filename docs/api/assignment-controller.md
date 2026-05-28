# AssignmentController API

## Scope

`AssignmentController` exposes PL-facing assignment operations for starting candidate selection, assigning NEW or REOPENED issues, reassigning DEV owners, and changing TESTER verifiers.

## Public Operations

| Operation | Service call | Result |
| --- | --- | --- |
| `startAssignment(issueId)` | `AssignmentService.startAssignment(issueId, currentUserId)` | `AssignmentOptionsResult` |
| `assignIssue(issueId, assigneeId, verifierId)` | `AssignmentService.assignIssue(issueId, assigneeId, verifierId, currentUserId)` | `AssignmentResult` |
| `reassignIssue(issueId, assigneeId)` | `AssignmentService.reassignIssue(issueId, assigneeId, currentUserId)` | `AssignmentResult` |
| `changeVerifier(issueId, verifierId)` | `AssignmentService.changeVerifier(issueId, verifierId, currentUserId)` | `AssignmentResult` |

## Operation Details

The controller requires a current user and passes `user.getLoginId()` to the service. `AssignmentOptionsResult` returns recommended and complete DEV/TESTER candidate lists. `AssignmentResult` returns issue database id, issue key, status, assignee, and verifier.

`assignIssue` accepts issues in `NEW` and `REOPENED`; other statuses fail. `reassignIssue` delegates to the domain `Issue.reassignAssignee`. `changeVerifier` delegates to `Issue.changeVerifier`.

## UC/OC/DCD Traceability

| API | UC/SSD/OC | DCD/domain evidence |
| --- | --- | --- |
| `startAssignment` | UC8 Recommend Assignment Candidates; supporting operation before assignment | `AssignmentController` -> `AssignmentRecommendationService.recommendAssignmentCandidates` in `docs/uml/dcd/its_dcd_ver2.puml` |
| `assignIssue` | UC5, OC-03 NEW to ASSIGNED, OC-12 REOPENED to ASSIGNED | `Issue.assignFromNew`, `Issue.assignReopened`, `IssueHistory(ASSIGNMENT_CHANGED, STATUS_CHANGED)`, assignee/verifier associations |
| `reassignIssue` | UC5, OC-04 ASSIGNED to ASSIGNED | `Issue.reassignAssignee`, `IssueHistory(ASSIGNMENT_CHANGED)`, assignee association update |
| `changeVerifier` | UC5, OC-05 FIXED to FIXED | `Issue.changeVerifier`, `IssueHistory(ASSIGNMENT_CHANGED)`, verifier association update |

## Implementation And Design Gaps

| Classification | Detail |
| --- | --- |
| `matches` | Assignment operations are controller-wired and service-enforced. |
| `implementation-extra` | `startAssignment` is a candidate-query API and not itself a required OC postcondition operation. |

## Permission And Failure Summary

- Requires login at controller boundary.
- Requires `PermissionPolicy.assertCanAssignIssue`, active PL role, and active project PL membership.
- `assignIssue` requires active project DEV assignee and active project TESTER verifier.
- Throws `IllegalArgumentException` when issue or user ids cannot be found.
- Throws `SecurityException` when actor is not the project PL or candidates are not active members with required roles.
- Throws `IllegalStateException` when assignment is not allowed for the issue status.

## Evidence

- `src/main/java/com/github/marcellokim/issuetracker/controller/AssignmentController.java`: `AssignmentController.startAssignment`, `assignIssue`, `reassignIssue`, `changeVerifier`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/AssignmentService.java`: `AssignmentService.startAssignment`, `assignIssue`, `reassignIssue`, `changeVerifier`
- `src/main/java/com/github/marcellokim/issuetracker/service/AssignmentRecommendationService.java`: `AssignmentRecommendationService.recommendAssignmentCandidates`
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanAssignIssue`
- `src/main/java/com/github/marcellokim/issuetracker/service/AssignmentResult.java`: `AssignmentResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/AssignmentOptionsResult.java`: `AssignmentOptionsResult`
