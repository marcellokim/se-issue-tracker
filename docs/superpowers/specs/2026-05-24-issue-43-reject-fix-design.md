# Issue #43 Reject Fix Design

## Context

Issue #43 implements the UC6 alternative flow where the current verifier rejects a fixed issue and moves it from `FIXED` back to `ASSIGNED`.

The current `origin/dev` baseline already contains the domain and policy foundation:

- `Issue.rejectFix(tester, comment, changedDate)` changes `FIXED -> ASSIGNED`.
- `PermissionPolicy.assertCanChangeStatus(actor, issue, ASSIGNED)` allows only the active current verifier when the issue is `FIXED`.
- The OC and current implementation API describe the rejection as `changeStatus(issueId, targetStatus=ASSIGNED, comment)`.
- The DCD includes state-specific convenience operation names such as `rejectFix(issueId, comment)`, but the current controller/service public API exposes `changeStatus(...)`; this issue completes that existing route instead of expanding the public API.
- The DCD and OC do not define `CommentPurpose.STATUS_CHANGE_REASON`; current code uses `CommentPurpose.STATUS_CHANGE` for status-change comments.

The remaining gap is application-service routing. `IssueStateService.changeStatus(...)` currently routes `FIXED`, `RESOLVED`, and `CLOSED`, while `ASSIGNED` still falls through to the unsupported-target branch.

## Decision

Implement #43 by wiring `targetStatus=ASSIGNED` inside the existing `IssueStateService.changeStatus(...)` flow.

Do not add a new public `rejectFix` controller/service operation. Do not introduce a new issue status. Do not add or rename `CommentPurpose` values.

## Architecture

`IssueStateController.changeStatus(...)` remains unchanged. It matches the current implementation and OC operation shape and passes the authenticated user's login id to `IssueStateService`.

`IssueStateService.changeStatus(...)` will add an `ASSIGNED` switch branch that delegates to a private `rejectFix(Issue issue, User actor, String comment)` method.

The private method will follow the existing status-change service pattern:

1. `permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.ASSIGNED)`
2. `LocalDateTime changedAt = now()`
3. `issue.rejectFix(actor, comment, changedAt)`
4. `issue.addComment(CommentIdGenerator.nextCommentId(), comment, actor, changedAt, CommentPurpose.STATUS_CHANGE)`

The existing outer method will continue saving the issue with `issueRepository.save(issue)` and returning `IssueStateResult`.

## Data Flow

1. A controller caller requests `changeStatus(issueId, ASSIGNED, comment)`.
2. The controller loads the current session user.
3. The service rejects blank comments before repository lookup.
4. The service loads the issue and actor.
5. The `ASSIGNED` branch delegates permission validation to `PermissionPolicy`.
6. The domain object performs the `FIXED -> ASSIGNED` transition and preserves current assignee, verifier, and fixer.
7. The service records the rejection reason as a `STATUS_CHANGE` comment.
8. The repository saves the updated issue.
9. The service returns `IssueStateResult` with the updated status and retained role associations.

## Error Handling

- Blank `comment` fails before issue/user lookup with `IllegalArgumentException`.
- Null `targetStatus` fails with `NullPointerException`.
- A non-verifier actor on a `FIXED` issue fails with `SecurityException`.
- A non-`FIXED` issue fails during the domain transition with `IllegalStateException` when the actor passes the broader `ASSIGNED` permission path, such as a PL actor.
- This design intentionally does not change `PermissionPolicy` ordering. For `targetStatus=ASSIGNED`, the policy currently treats `FIXED` as verifier rejection and non-`FIXED` as PL assignment permission.
- On failed domain transition, no new comment, history, or save side effect should occur.
- `REOPENED` remains unsupported in this issue because #47 owns reopen routing.

## Testing

Update `IssueStateServiceTest`.

Success coverage:

- Current verifier calls `changeStatus(issueId, ASSIGNED, "Reject fix", verifierId)`.
- Result status is `ASSIGNED`.
- Issue status is `ASSIGNED`.
- Existing assignee is retained.
- Existing verifier is retained.
- Existing fixer is retained.
- Resolver remains unchanged; if it was `null`, it stays `null`.
- One status-change comment is created with `CommentPurpose.STATUS_CHANGE`.
- Latest histories are `STATUS_CHANGED` followed by `COMMENTED`.
- `STATUS_CHANGED` records `previousValue=FIXED` and `newValue=ASSIGNED`.

Failure coverage:

- Wrong actor on a `FIXED` issue cannot reject the fix and fails with `SecurityException`.
- A non-`FIXED` issue cannot be rejected. Use a PL actor for this service-level test if expecting `IllegalStateException`, because the current permission policy allows PL assignment on non-`FIXED` issues before the domain `rejectFix` guard runs.
- Existing unsupported-target coverage removes `ASSIGNED` from unsupported cases and keeps `REOPENED` as unsupported.

## Scope Exclusions

- No controller API expansion beyond existing `changeStatus`.
- No `CommentPurpose` enum/schema change.
- No persistence schema change.
- No UC5 reassignment behavior.
- No #47 reopen behavior.
- No UI behavior.

## Verification

Required local checks:

- `./gradlew test --tests com.github.marcellokim.issuetracker.service.IssueStateServiceTest --console=plain`
- `./gradlew check --console=plain`

PR evidence should mention branch `feat/43-reject-fix-transition`, base `origin/dev`, and that Oracle-local integration and UI/manual flows are outside this issue's scope unless run separately.

Suggested PR description wording:

```text
Implements #43 by completing the existing IssueStateService.changeStatus(...)
routing for targetStatus=ASSIGNED when the current issue is FIXED.

This keeps the public controller/service operation shape aligned with the
current implementation and OC: changeStatus(issueId, targetStatus=ASSIGNED, comment).
No new public rejectFix API, IssueStatus, CommentPurpose, or persistence schema is added.
```
