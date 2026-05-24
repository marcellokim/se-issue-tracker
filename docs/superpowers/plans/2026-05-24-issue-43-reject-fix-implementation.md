# Issue 43 Reject Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete #43 by routing `changeStatus(issueId, ASSIGNED, comment, currentUserId)` through the existing reject-fix domain behavior.

**Architecture:** Keep the public API unchanged. `IssueStateController` continues calling `IssueStateService.changeStatus(...)`; `IssueStateService` adds an `ASSIGNED` branch that validates permission, calls `Issue.rejectFix(...)`, records a `CommentPurpose.STATUS_CHANGE` comment, saves the issue, and returns `IssueStateResult`.

**Tech Stack:** Java 21, Gradle, JUnit Jupiter 6.1.0, existing in-memory test repositories.

---

## File Structure

- Modify `src/test/java/com/github/marcellokim/issuetracker/service/IssueStateServiceTest.java`
  - Owns service-level behavior for status transitions.
  - Add failing #43 tests before production code.
  - Keep `REOPENED` as unsupported because #47 owns reopen routing.
- Modify `src/main/java/com/github/marcellokim/issuetracker/service/IssueStateService.java`
  - Owns application-service orchestration for status transitions.
  - Add one `ASSIGNED` switch branch and one private `rejectFix(...)` helper.
- No changes to `IssueStateController`, `Issue`, `PermissionPolicy`, `CommentPurpose`, repositories, schema, or UI.

---

### Task 1: Add Failing Service Tests

**Files:**
- Modify: `src/test/java/com/github/marcellokim/issuetracker/service/IssueStateServiceTest.java`
- Test: `src/test/java/com/github/marcellokim/issuetracker/service/IssueStateServiceTest.java`

- [ ] **Step 1: Add the successful reject-fix service test**

Insert this test after `resolveFixedIssue()` and before `closeResolvedIssue()`:

```java
    @Test
    @DisplayName("verifier rejects fixed issue back to assigned")
    void rejectFixedIssueBackToAssigned() {
        var issue = fixedIssue();
        var service = service(issue);

        var result = service.changeStatus(ISSUE_ID, IssueStatus.ASSIGNED, "Needs more work",
                verifier.getLoginId());

        assertEquals(IssueStatus.ASSIGNED, result.status());
        assertEquals(IssueStatus.ASSIGNED, issue.getStatus());
        assertSame(assignee, result.assignee());
        assertSame(verifier, result.verifier());
        assertSame(assignee, result.fixer());
        assertNull(result.resolver());
        assertSame(assignee, issue.getAssignee());
        assertSame(verifier, issue.getVerifier());
        assertSame(assignee, issue.getFixer());
        assertNull(issue.getResolver());
        assertEquals(1, issue.getComments().size());
        assertEquals("Needs more work", issue.getComments().getFirst().getContent());
        assertEquals(CommentPurpose.STATUS_CHANGE, issue.getComments().getFirst().getPurpose());

        var histories = issue.getHistories();
        var statusHistory = histories.get(histories.size() - 2);
        assertEquals(ActionType.STATUS_CHANGED, statusHistory.getAction());
        assertEquals(IssueStatus.FIXED.name(), statusHistory.getPreviousValue());
        assertEquals(IssueStatus.ASSIGNED.name(), statusHistory.getNewValue());
        assertEquals("Needs more work", statusHistory.getMessage());
        assertStatusChangedThenCommented(issue);
    }
```

- [ ] **Step 2: Add wrong-actor and non-FIXED failure tests**

Insert these tests after `rejectCloseByPlFromOtherProject()` and before `rejectBlankCommentAndWrongParticipant()`:

```java
    @Test
    @DisplayName("only current verifier can reject a fixed issue")
    void rejectFixRequiresCurrentVerifier() {
        var issue = fixedIssue();
        int commentCount = issue.getComments().size();
        int historyCount = issue.getHistories().size();
        var service = service(issue);

        assertThrows(SecurityException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.ASSIGNED, "Needs more work",
                        otherDev.getLoginId()));

        assertEquals(commentCount, issue.getComments().size());
        assertEquals(historyCount, issue.getHistories().size());
    }

    @Test
    @DisplayName("reject fix requires fixed issue status")
    void rejectFixRequiresFixedIssueStatus() {
        var issue = assignedIssue();
        int commentCount = issue.getComments().size();
        int historyCount = issue.getHistories().size();
        var service = service(issue);

        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.ASSIGNED, "Needs more work",
                        pl.getLoginId()));

        assertEquals(commentCount, issue.getComments().size());
        assertEquals(historyCount, issue.getHistories().size());
    }
```

- [ ] **Step 3: Narrow the unsupported-target test to REOPENED only**

Replace the existing `rejectUnsupportedTargetStatus()` test with:

```java
    @Test
    @DisplayName("reopened status change target remains a feature gap")
    void rejectUnsupportedReopenedTargetStatus() {
        var resolved = resolvedIssue();
        var resolvedService = service(resolved);

        assertThrows(UnsupportedOperationException.class,
                () -> resolvedService.changeStatus(ISSUE_ID, IssueStatus.REOPENED, "Needs more work",
                        pl.getLoginId()));
    }
```

- [ ] **Step 4: Run the service test and verify the new tests fail for the expected reason**

Run:

```bash
./gradlew test --tests com.github.marcellokim.issuetracker.service.IssueStateServiceTest --console=plain
```

Expected:

- `rejectFixedIssueBackToAssigned` fails because `IssueStatus.ASSIGNED` still reaches `UnsupportedOperationException`.
- `rejectFixRequiresCurrentVerifier` fails because `IssueStatus.ASSIGNED` still reaches `UnsupportedOperationException`.
- `rejectFixRequiresFixedIssueStatus` fails because `IssueStatus.ASSIGNED` still reaches `UnsupportedOperationException`.
- Existing FIXED, RESOLVED, CLOSED tests continue passing.

---

### Task 2: Route ASSIGNED Through Reject Fix

**Files:**
- Modify: `src/main/java/com/github/marcellokim/issuetracker/service/IssueStateService.java`
- Test: `src/test/java/com/github/marcellokim/issuetracker/service/IssueStateServiceTest.java`

- [ ] **Step 1: Add the ASSIGNED switch branch**

In `IssueStateService.changeStatus(...)`, replace the switch with:

```java
        switch (requiredTargetStatus) {
            case FIXED -> markFixed(issue, actor, requiredComment);
            case RESOLVED -> resolve(issue, actor, requiredComment);
            case ASSIGNED -> rejectFix(issue, actor, requiredComment);
            case CLOSED -> close(issue, actor, requiredComment);
            default -> throw new UnsupportedOperationException("Unsupported target status: " + requiredTargetStatus);
        }
```

- [ ] **Step 2: Add the private rejectFix helper**

Add this helper between `resolve(...)` and `close(...)`:

```java
    private void rejectFix(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.ASSIGNED);
        LocalDateTime changedAt = now();
        issue.rejectFix(actor, comment, changedAt);
        issue.addComment(
                CommentIdGenerator.nextCommentId(),
                comment,
                actor,
                changedAt,
                CommentPurpose.STATUS_CHANGE);
    }
```

- [ ] **Step 3: Run the focused service test**

Run:

```bash
./gradlew test --tests com.github.marcellokim.issuetracker.service.IssueStateServiceTest --console=plain
```

Expected:

- `BUILD SUCCESSFUL`
- `rejectFixedIssueBackToAssigned` passes.
- `rejectFixRequiresCurrentVerifier` passes with `SecurityException`.
- `rejectFixRequiresFixedIssueStatus` passes with `IllegalStateException`.
- `rejectUnsupportedReopenedTargetStatus` still passes with `UnsupportedOperationException`.

- [ ] **Step 4: Commit the implementation**

Run:

```bash
git add src/main/java/com/github/marcellokim/issuetracker/service/IssueStateService.java \
        src/test/java/com/github/marcellokim/issuetracker/service/IssueStateServiceTest.java
git commit -m "feat: route reject fix status change"
```

Expected:

- Commit succeeds.
- Pre-commit repository setup check passes.

---

### Task 3: Full Verification

**Files:**
- Verify: `src/main/java/com/github/marcellokim/issuetracker/service/IssueStateService.java`
- Verify: `src/test/java/com/github/marcellokim/issuetracker/service/IssueStateServiceTest.java`
- Verify: `docs/superpowers/specs/2026-05-24-issue-43-reject-fix-design.md`

- [ ] **Step 1: Check whitespace and patch hygiene**

Run:

```bash
git diff --check
```

Expected:

- No output.
- Exit code `0`.

- [ ] **Step 2: Run the full Gradle check**

Run:

```bash
./gradlew check --console=plain
```

Expected:

- `BUILD SUCCESSFUL`
- No unit-test regression outside `IssueStateServiceTest`.

- [ ] **Step 3: Inspect final branch state**

Run:

```bash
git status --short --branch
git log --oneline --decorate -5
```

Expected:

- Branch is `feat/43-reject-fix-transition`.
- Worktree is clean.
- Branch contains the design commits plus `feat: route reject fix status change`.

- [ ] **Step 4: Prepare PR evidence**

Use this PR evidence summary:

```text
Branch: feat/43-reject-fix-transition
Base: origin/dev

Implemented #43 by completing IssueStateService.changeStatus(...) routing for
targetStatus=ASSIGNED when the current issue is FIXED.

No new public rejectFix API, IssueStatus, CommentPurpose, persistence schema,
UC5 reassignment behavior, #47 reopen behavior, or UI behavior was added.

Verification:
- ./gradlew test --tests com.github.marcellokim.issuetracker.service.IssueStateServiceTest --console=plain
- ./gradlew check --console=plain

Not run:
- Oracle-local integration
- UI/manual flow
```
