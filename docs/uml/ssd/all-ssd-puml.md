# SSD PUML 전체 모음

이 파일은 `docs/uml/ssd/`의 개별 SSD PUML 파일을 Markdown으로 모은 문서입니다.

- Source directory: `docs/uml/ssd/`
- Style include: 각 PUML은 기존처럼 `!include ssd-style.puml`을 유지합니다.

## SSD 01 - `ssd-01-register-issue.puml`

```plantuml
@startuml ssd-01-register-issue
!include ssd-style.puml
title SSD 01 - UC1 이슈 등록
actor "Auth User" as A
participant ":Issue Tracking System" as S
A -> S: registerIssue(projectId, title, description, priority)
S --> A: issueRegistered(issueId, status=NEW, confirmation)
opt Auth User가 등록 후 코멘트 추가를 원함
  A -> S: addComment(issueId, content)
  S --> A: commentRegistered(commentId, createdDate, confirmation)
end
@enduml
```

## SSD 02 - `ssd-02-add-comment.puml`

```plantuml
@startuml ssd-02-add-comment
!include ssd-style.puml
title SSD 02 - UC2 코멘트 추가
actor "Auth User" as A
participant ":Issue Tracking System" as S
A -> S: addComment(issueId, content)
S --> A: commentRegistered(commentId, createdDate, confirmation)
@enduml
```

## SSD 03 - `ssd-03-search-issues.puml`

```plantuml
@startuml ssd-03-search-issues
!include ssd-style.puml
title SSD 03 - UC3 Search / Browse Issues

actor "Auth User" as A
participant ":Issue Tracking System" as S


A -> S: searchIssues(projectId, issueSearchFilters)
alt 조건 없이 브라우즈
  S --> A: issueList(nonDeletedProjectIssues)
else 조건 검색
  S --> A: issueList(matchingNonDeletedIssues)
end

@enduml
```

## SSD 04 - `ssd-04-view-issue-details.puml`

```plantuml
@startuml ssd-04-view-issue-details
!include ssd-style.puml
title SSD 04 - UC4 이슈 상세 조회
actor "Auth User" as A
participant ":Issue Tracking System" as S
A -> S: viewIssueDetail(issueId)
S --> A: issueDetails(issue, comments, history,\nblockedByDependencies, blockingDependencies,\navailableActions)
@enduml
```

## SSD 05 - `ssd-05-assign-issue.puml`

```plantuml
@startuml ssd-05-assign-issue
!include ssd-style.puml
title SSD 05 - UC5 Assign / Update Issue Assignment

actor "PL" as A
participant ":Issue Tracking System" as S


A -> S: startAssignment(issueId)
S --> A: assignmentOptions(devAssigneeCandidates, testerVerifierCandidates, allDevAssignees, allTesterVerifiers)

alt Issue.status == NEW or REOPENED
  opt UC2 Add Comment 선택
    A -> S: addComment(issueId, content)
    S --> A: commentRegistered(commentId, createdDate, confirmation)
  end
  A -> S: assignIssue(issueId, assigneeId, verifierId)
  S --> A: issueAssigned(issueId, status=ASSIGNED, confirmation)

else Issue.status == ASSIGNED
  opt UC2 Add Comment 선택
    A -> S: addComment(issueId, content)
    S --> A: commentRegistered(commentId, createdDate, confirmation)
  end
  A -> S: reassignIssue(issueId, assigneeId)
  S --> A: issueReassigned(issueId, status=ASSIGNED, confirmation)

else Issue.status == FIXED
  opt UC2 Add Comment 선택
    A -> S: addComment(issueId, content)
    S --> A: commentRegistered(commentId, createdDate, confirmation)
  end
  A -> S: changeVerifier(issueId, verifierId)
  S --> A: verifierChanged(issueId, status=FIXED, confirmation)
end


@enduml
```

## SSD 06 - `ssd-06-mark-fixed.puml`

```plantuml
@startuml ssd-06-mark-fixed
!include ssd-style.puml
title SSD 06 - UC6 상태 변경: Fixed 처리
actor "DEV" as A
participant ":Issue Tracking System" as S
A -> S: changeStatus(issueId, targetStatus=FIXED, comment)
alt 상태 또는 권한이 맞지 않음
  S --> A: statusChangeRejected(reason)
else comment가 비어 있음
  S --> A: statusChangeRejected(commentRequired)
else 현재 assignee DEV가 FIXED 처리
  S --> A: updatedIssue(issueId, status=FIXED, fixer=currentDev)
end
@enduml
```

## SSD 07 - `ssd-07-resolve-issue.puml`

```plantuml
@startuml ssd-07-resolve-issue
!include ssd-style.puml
title SSD 07 - UC6 상태 변경: Resolve 처리
actor "Tester" as A
participant ":Issue Tracking System" as S
A -> S: changeStatus(issueId, targetStatus=RESOLVED, comment)
alt 상태 또는 권한이 맞지 않음
  S --> A: statusChangeRejected(reason)
else comment가 비어 있음
  S --> A: statusChangeRejected(commentRequired)
else unresolved blocking issue가 존재함
  S --> A: statusChangeRejected(blockingIssueNotResolved)
else 현재 verifier TESTER가 resolve
  S --> A: updatedIssue(issueId, status=RESOLVED, resolver=currentTester)
end
@enduml
```

## SSD 08 - `ssd-08-reject-fix.puml`

```plantuml
@startuml ssd-08-reject-fix
!include ssd-style.puml
title SSD 08 - UC6 상태 변경: Fix 거절
actor "Tester" as A
participant ":Issue Tracking System" as S
A -> S: changeStatus(issueId, targetStatus=ASSIGNED, comment)
alt 상태 또는 권한이 맞지 않음
  S --> A: statusChangeRejected(reason)
else comment가 비어 있음
  S --> A: statusChangeRejected(commentRequired)
else 현재 verifier TESTER가 fix 거절
  S --> A: updatedIssue(issueId, status=ASSIGNED, fixer=retained)
end
@enduml
```

## SSD 09 - `ssd-09-close-issue.puml`

```plantuml
@startuml ssd-09-close-issue
!include ssd-style.puml
title SSD 09 - UC6 상태 변경: Close 처리
actor "PL" as A
participant ":Issue Tracking System" as S
A -> S: changeStatus(issueId, targetStatus=CLOSED, comment)
alt 상태 또는 권한이 맞지 않음
  S --> A: statusChangeRejected(reason)
else comment가 비어 있음
  S --> A: statusChangeRejected(commentRequired)
else PL이 RESOLVED 이슈를 close
  S --> A: updatedIssue(issueId, status=CLOSED, assignee=null, verifier=null)
end
@enduml
```

## SSD 10 - `ssd-10-reopen-issue.puml`

```plantuml
@startuml ssd-10-reopen-issue
!include ssd-style.puml
title SSD 10 - UC6 상태 변경: Reopen 처리
actor "PL" as A
participant ":Issue Tracking System" as S
A -> S: changeStatus(issueId, targetStatus=REOPENED, comment)
alt 상태 또는 권한이 맞지 않음
  S --> A: statusChangeRejected(reason)
else comment가 비어 있음
  S --> A: statusChangeRejected(commentRequired)
else PL이 RESOLVED/CLOSED 이슈를 reopen
  S --> A: updatedIssue(issueId, status=REOPENED, fixer=preserved, resolver=preserved)
end
@enduml
```

## SSD 11 - `ssd-11-recommend-assignees.puml`

```plantuml
@startuml ssd-11-recommend-assignees
!include ssd-style.puml
title SSD 11 - UC8 Recommend Assignment Candidates

actor "PL" as A
participant ":Issue Tracking System" as S


A -> S: startAssignment(issueId)
S --> A: assignmentOptions(devAssigneeCandidates, testerVerifierCandidates, allDevAssignees, allTesterVerifiers)


@enduml
```

## SSD 12 - `ssd-12-delete-issue.puml`

```plantuml
@startuml ssd-12-delete-issue
!include ssd-style.puml
title SSD 12 - UC9 Delete Closed/New Issue
actor "PL" as A
participant ":Issue Tracking System" as S


A -> S: deleteIssue(issueId, comment)
alt 상태 또는 권한이 맞지 않음
  S --> A: deleteRejected(reason)
else comment가 비어 있음
  S --> A: deleteRejected(commentRequired)
else PL이 NEW/CLOSED 이슈를 soft delete
  S --> A: issueDeleted(issueId, status=DELETED, retentionResult)
end

@enduml
```

## SSD 13 - `ssd-13-purge-deleted-issues.puml`

```plantuml
@startuml ssd-13-purge-deleted-issues
!include ssd-style.puml
title SSD 13 - UC9 Purge Deleted Issue / Deleted Issue Retention

actor "PL" as A
participant ":Issue Tracking System" as S


alt PL이 DELETED 이슈 하나를 영구 삭제한다
  A -> S: purgeDeletedIssue(issueId)
  S --> A: deletedIssuePurged(issueId, confirmation)

else soft delete 이후 DELETED 보관 한도가 초과된다
  A -> S: deleteIssue(issueId, comment)
  S --> A: issueDeleted(issueId, status=DELETED, retentionResult)
end


@enduml
```

## SSD 14 - `ssd-14-view-issue-statistics.puml`

```plantuml
@startuml ssd-14-view-issue-statistics
!include ssd-style.puml
title SSD 14 - UC10 View Issue Statistics

actor "Auth User" as A
participant ":Issue Tracking System" as S


A -> S: viewStatistics(projectId, dailyPeriod, monthlyPeriod)
S --> A: statisticsReport(summaryCounts, trendCounts, commentCounts)


@enduml
```

## SSD 15 - `ssd-15-log-in.puml`

```plantuml
@startuml ssd-15-log-in
!include ssd-style.puml
title SSD 15 - UC11 Log In

actor "User" as A
participant ":Issue Tracking System" as S


A -> S: login(loginId, password)

alt 이미 로그인된 세션이 존재한다
  S --> A: loginFailed(reason)

else loginId 또는 password가 비어 있다
  S --> A: loginFailed(reason)

else 인증 정보가 올바르지 않다
  S --> A: loginFailed(reason)

else 계정이 비활성 상태이다
  S --> A: loginFailed(reason)

else 인증 성공
  S --> A: loginSucceeded(loginId, name, role)
end


@enduml
```

## SSD 16 - `ssd-16-create-account.puml`

```plantuml
@startuml ssd-16-create-account
!include ssd-style.puml
title SSD 16 - UC12 Create Account

actor "Admin" as A
participant ":Issue Tracking System" as S


A -> S: createAccount(loginId, name, password, role)
S --> A: accountCreated(loginId, name, role, isActive=true, confirmation)


@enduml
```

## SSD 17 - `ssd-17-update-account.puml`

```plantuml
@startuml ssd-17-update-account
!include ssd-style.puml
title SSD 17 - UC12 Update Account

actor "Admin" as A
participant ":Issue Tracking System" as S


alt 계정 이름 변경
  A -> S: renameAccount(loginId, name)
  S --> A: accountRenamed(loginId, name, confirmation)

else 계정 역할 변경
  A -> S: changeAccountRole(loginId, role)
  S --> A: accountRoleChanged(loginId, role, confirmation)
end


@enduml
```

## SSD 18 - `ssd-18-deactivate-account.puml`

```plantuml
@startuml ssd-18-activate-deactivate-account
!include ssd-style.puml
title SSD 18 - UC12 Activate / Deactivate Account

actor "Admin" as A
participant ":Issue Tracking System" as S


alt 계정 비활성화
  A -> S: deactivateAccount(loginId)
  S --> A: accountDeactivated(loginId, isActive=false, confirmation)

else 계정 활성화
  A -> S: activateAccount(loginId)
  S --> A: accountActivated(loginId, isActive=true, confirmation)
end


@enduml
```

## SSD 19 - `ssd-19-create-project.puml`

```plantuml
@startuml ssd-19-create-project
!include ssd-style.puml
title SSD 19 - UC13 Create Project

actor "Admin" as A
participant ":Issue Tracking System" as S


A -> S: createProject(name, description)
S --> A: projectCreated(projectId, name, description, confirmation)


@enduml
```

## SSD 20 - `ssd-20-add-project-member.puml`

```plantuml
@startuml ssd-20-add-project-member
!include ssd-style.puml
title SSD 20 - UC13 Add Project Member

actor "Admin" as A
participant ":Issue Tracking System" as S


A -> S: addProjectParticipant(projectId, loginId)
S --> A: projectParticipantAdded(projectId, loginId, confirmation)


@enduml
```

## SSD 21 - `ssd-21-remove-project-member.puml`

```plantuml
@startuml ssd-21-remove-project-member
!include ssd-style.puml
title SSD 21 - UC13 Remove Project Member

actor "Admin" as A
participant ":Issue Tracking System" as S


A -> S: removeProjectParticipant(projectId, loginId)
S --> A: projectParticipantRemoved(projectId, loginId, confirmation)


@enduml
```

## SSD 22 - `ssd-22-verify-permission.puml`

```plantuml
@startuml ssd-22-verify-permission
!include ssd-style.puml
title SSD 22 - UC14 권한 검사
actor "Actor" as A
participant ":Issue Tracking System" as S
A -> S: assertCanProtectedOperation(actor, resource)
alt 권한 있음
  S --> A: permissionVerified()
else 권한 없음
  S --> A: permissionDenied(reason)
end
@enduml
```

## SSD 23 - `ssd-23-edit-issue.puml`

```plantuml
@startuml ssd-23-edit-issue
!include ssd-style.puml
title SSD 23 - UC15 Edit Issue

actor "Reporter" as A
participant ":Issue Tracking System" as S


A -> S: updateIssue(issueId, title, description)
S --> A: issueUpdated(issueId, title, description, confirmation)


@enduml
```

## SSD 24 - `ssd-24-add-dependency.puml`

```plantuml
@startuml ssd-24-add-dependency
!include ssd-style.puml
title SSD 24 - UC7 Add Dependency

actor "PL" as A
participant ":Issue Tracking System" as S


A -> S: addDependency(blockingIssueId, blockedIssueId)
S --> A: dependencyAdded(dependencyId, blockingIssueId, blockedIssueId, confirmation)


@enduml
```

## SSD 25 - `ssd-25-remove-dependency.puml`

```plantuml
@startuml ssd-25-remove-dependency
!include ssd-style.puml
title SSD 25 - UC7 Remove Dependency

actor "PL" as A
participant ":Issue Tracking System" as S


A -> S: removeDependency(blockingIssueId, blockedIssueId)
S --> A: dependencyRemoved(blockingIssueId, blockedIssueId, confirmation)


@enduml
```

## SSD 26 - `ssd-26-restore-deleted-issue.puml`

```plantuml
@startuml ssd-26-restore-deleted-issue
!include ssd-style.puml
title SSD 26 - UC9 Restore Deleted Issue

actor "PL" as A
participant ":Issue Tracking System" as S


A -> S: viewDeletedIssues(projectId)
S --> A: deletedIssueList(issues)

A -> S: restoreIssue(issueId, comment)
alt 상태 또는 권한이 맞지 않음
  S --> A: restoreRejected(reason)
else comment가 비어 있음
  S --> A: restoreRejected(commentRequired)
else PL이 DELETED 이슈를 복구
  S --> A: issueRestored(issueId, status=NEW or CLOSED, confirmation)
end

@enduml
```

## SSD 27 - `ssd-27-change-priority.puml`

```plantuml
@startuml ssd-27-change-priority
!include ssd-style.puml
title SSD 27 - UC16 우선순위 변경
actor "PL" as A
participant ":Issue Tracking System" as S
A -> S: changePriority(issueId, newPriority)
S --> A: priorityChanged(issueId, priority=newPriority)
@enduml
```
