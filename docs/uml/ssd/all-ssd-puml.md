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
note over A, S
사전조건:
- Auth User가 로그인되어 있다.
- 이슈를 등록할 Project가 선택되어 있다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
- Auth User는 대상 Project의 active member이다.
end note
A -> S: registerIssue(projectId, title, description, priority)
S --> A: issueRegistered(issueId, status=NEW, confirmation)
opt Auth User가 등록 후 코멘트 추가를 원함
  ref over A, S : UC2 Add Comment\naddComment(issueId, content)
end
note over S
결과:
- Issue가 생성된다.
- reporter=current Auth User, reportedDate=current time이 저장된다.
- status=NEW가 저장된다.
- IssueHistory(CREATED)가 기록된다.
end note
@enduml
```
## SSD 02 - `ssd-02-add-comment.puml`

```plantuml
@startuml ssd-02-add-comment
!include ssd-style.puml
title SSD 02 - UC2 코멘트 추가
actor "Auth User" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: Auth User 로그인, Issue 존재, content가 비어 있지 않음
end note
A -> S: addComment(issueId, content)
S --> A: commentRegistered(commentId, createdDate, confirmation)
note over S
결과: Comment 생성, writer=current Auth User, createdDate 저장, IssueHistory(COMMENTED) 기록
end note
@enduml
```

## SSD 03 - `ssd-03-search-issues.puml`

```plantuml
@startuml ssd-03-search-issues
!include ssd-style.puml
title SSD 03 - UC3 Search / Browse Issues

actor "Auth User" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- Auth User가 로그인되어 있다.
- 대상 Project가 존재한다.
- Auth User는 대상 Project의 active member이다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

A -> S: searchIssues(issueSearchFilters)
S --> A: issueList(issues)

note over S
issueSearchFilters:
- projectId
- keyword(title/description)
- status
- priority
- reporterId
- assigneeId
- verifierId
- reportedDateRange

정책:
- keyword가 비어 있으면 keyword 조건 없이 검색한다.
- status와 priority가 비어 있으면 해당 조건 없이 검색한다.
- reporterId, assigneeId, verifierId가 비어 있으면 해당 조건 없이 검색한다.
- reportedDateRange가 비어 있으면 기간 조건 없이 검색한다.
- 일반 검색에서는 DELETED 상태 이슈를 반환하지 않는다.
- DELETED 이슈 관리는 UC9 Manage Deleted Issue에서 처리한다.
end note

@enduml
```

## SSD 04 - `ssd-04-view-issue-details.puml`

```plantuml
@startuml ssd-04-view-issue-details
!include ssd-style.puml
title SSD 04 - UC4 이슈 상세 조회
actor "Auth User" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: 대상 Issue는 DELETED 상태가 아니다, Auth User는 대상 Issue가 속한 Project의 active member이다.
end note
A -> S: viewIssueDetail(issueId)
S --> A: issueDetails(issue, comments, history, dependencies, availableActions)
note over S
결과: 도메인 상태를 변경하지 않음.
UC15 Edit Issue는 UC4를 확장하는 subfunction이다.
UC4 상세 화면의 availableActions에서 UC5 Assign / Update Issue Assignment와 UC6 Change Issue State가 파생/연계 실행될 수 있다.
end note
@enduml
```

## SSD 05 - `ssd-05-assign-issue.puml`

```plantuml
@startuml ssd-05-assign-issue
!include ssd-style.puml
title SSD 05 - UC5 Assign / Update Issue Assignment

actor "PL" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- PL이 로그인되어 있다.
- 대상 Issue가 존재한다.
- Issue가 NEW, REOPENED, ASSIGNED 또는 FIXED 상태이다.
- PL은 대상 Issue가 속한 Project의 active PL이다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

A -> S: startAssignment(issueId)
S --> A: assignmentOptions(devAssigneeCandidates, testerVerifierCandidates, allDevAssignees, allTesterVerifiers)

note over S
UC5는 UC8 Recommend Assignment Candidates를 include한다.

상태별 후보 반환:
- NEW/REOPENED: DEV assignee 후보와 TESTER verifier 후보를 반환한다.
- ASSIGNED: DEV assignee 후보를 반환한다.
- FIXED: TESTER verifier 후보를 반환한다.

devAssigneeCandidates와 testerVerifierCandidates는 top 3 후보이다.
allDevAssignees와 allTesterVerifiers는 프로젝트 소속 active 후보 전체이다.
해결 이력이 없으면 completedIssueCount=0인 active project member가 후보로 반환된다.
필요한 역할의 후보가 없으면 빈 후보 목록을 반환하고, PL은 배정을 완료할 수 없다.
end note

alt Issue.status == NEW or REOPENED
  opt UC2 Add Comment 선택
    ref over A, S : UC2 Add Comment\naddComment(issueId, content)
  end
  A -> S: assignIssue(issueId, assigneeId, verifierId)
  S --> A: issueAssigned(issueId, status=ASSIGNED, confirmation)

else Issue.status == ASSIGNED
  opt UC2 Add Comment 선택
    ref over A, S : UC2 Add Comment\naddComment(issueId, content)
  end
  A -> S: reassignIssue(issueId, assigneeId)
  S --> A: issueReassigned(issueId, status=ASSIGNED, confirmation)

else Issue.status == FIXED
  opt UC2 Add Comment 선택
    ref over A, S : UC2 Add Comment\naddComment(issueId, content)
  end
  A -> S: changeVerifier(issueId, verifierId)
  S --> A: verifierChanged(issueId, status=FIXED, confirmation)
end

note over S
결과:
- NEW/REOPENED -> ASSIGNED: assignee DEV와 verifier TESTER를 설정하고 status를 ASSIGNED로 변경한다.
- ASSIGNED -> ASSIGNED: assignee DEV만 변경하고 status를 유지한다.
- FIXED -> FIXED: verifier TESTER만 변경하고 status를 유지한다.
- 모든 배정/변경 branch는 IssueHistory(ASSIGNMENT_CHANGED)를 기록한다.
- NEW/REOPENED -> ASSIGNED는 IssueHistory(STATUS_CHANGED)도 함께 기록한다.
end note

@enduml
```

## SSD 06 - `ssd-06-mark-fixed.puml`

```plantuml
@startuml ssd-06-mark-fixed
!include ssd-style.puml
title SSD 06 - UC6 상태 변경: Fixed 처리
actor "DEV" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: DEV 로그인, 자신에게 배정된 Issue, status=ASSIGNED, UC14 권한 검사
end note
A -> S: changeStatus(issueId, targetStatus=FIXED, comment)
S --> A: updatedIssue(issueId, status=FIXED)
note over S
UC6 includes UC2; the comment is mandatory and recorded with the status change.
결과: status=FIXED, fixer=current DEV 자동 기록, 필수 Comment와 IssueHistory(STATUS_CHANGED, previousValue=ASSIGNED, newValue=FIXED) 기록
end note
@enduml
```

## SSD 07 - `ssd-07-resolve-issue.puml`

```plantuml
@startuml ssd-07-resolve-issue
!include ssd-style.puml
title SSD 07 - UC6 상태 변경: Resolve 처리
actor "Tester" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: Tester 로그인, verifier 본인, Issue가 FIXED 상태, UC14 권한 검사
모든 blocking Issue가 RESOLVED 또는 CLOSED 상태여야 함
end note
A -> S: changeStatus(issueId, targetStatus=RESOLVED, comment)
S --> A: updatedIssue(issueId, status=RESOLVED, resolver=currentTester)
note over S
UC6 includes UC2; the comment is mandatory and recorded with the status change.
결과: status=RESOLVED, resolver=current Tester 기록, assignee/verifier 제거, fixer 보존, 필수 Comment와 IssueHistory(...) 기록
blocking Issue 중 RESOLVED/CLOSED가 아닌 항목이 있으면 FIXED -> RESOLVED 전이를 거부
별도 BLOCK/BLOCKED IssueStatus는 만들지 않음
end note
@enduml
```

## SSD 08 - `ssd-08-reject-fix.puml`

```plantuml
@startuml ssd-08-reject-fix
!include ssd-style.puml
title SSD 08 - UC6 상태 변경: Fix 거절
actor "Tester" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: Tester 로그인, verifier 본인, Issue가 FIXED 상태, 거절 사유 존재, UC14 권한 검사
end note
A -> S: changeStatus(issueId, targetStatus=ASSIGNED, comment)
S --> A: updatedIssue(issueId, status=ASSIGNED, fixer=retained)
note over S
UC6 includes UC2; the comment is mandatory and recorded with the status change.
결과: status=ASSIGNED로 복귀, 기존 assignee/verifier/fixer 유지
새 DEV로 바꾸려면 PL이 UC5-RA로 assignee를 재배정한다.
실패 사유 Comment와 IssueHistory(STATUS_CHANGED, previousValue=FIXED, newValue=ASSIGNED) 기록
end note
@enduml
```

## SSD 09 - `ssd-09-close-issue.puml`

```plantuml
@startuml ssd-09-close-issue
!include ssd-style.puml
title SSD 09 - UC6 상태 변경: Close 처리
actor "PL" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: PL 로그인, Issue가 RESOLVED 상태, UC14 권한 검사
end note
A -> S: changeStatus(issueId, targetStatus=CLOSED, comment)
S --> A: updatedIssue(issueId, status=CLOSED, assignee=null, verifier=null)
note over S
UC6 includes UC2; the comment is mandatory and recorded with the status change.
결과: status=CLOSED, assignee와 verifier 제거, fixer와 resolver는 보존
필수 Comment와 IssueHistory(STATUS_CHANGED, previousValue=RESOLVED, newValue=CLOSED) 기록
end note
@enduml
```

## SSD 10 - `ssd-10-reopen-issue.puml`

```plantuml
@startuml ssd-10-reopen-issue
!include ssd-style.puml
title SSD 10 - UC6 상태 변경: Reopen 처리
actor "PL" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: PL 로그인, Issue가 RESOLVED/CLOSED 상태, Reopen 사유 존재, UC14 권한 검사
end note
A -> S: changeStatus(issueId, targetStatus=REOPENED, comment)
S --> A: updatedIssue(issueId, status=REOPENED, fixer=preserved, resolver=preserved)
note over S
UC6 includes UC2; the comment is mandatory and recorded with the status change.
결과: status=REOPENED, 기존 fixer와 resolver는 보존되어 PL에게 제시 가능
assignee/verifier를 자동 복원하지 않으며, PL이 재작업 담당자를 정하려면 UC5 Assign / Update Issue Assignment를 수행한다.
사유 Comment와 IssueHistory(STATUS_CHANGED, previousValue=RESOLVED/CLOSED, newValue=REOPENED) 기록
end note
@enduml
```

## SSD 11 - `ssd-11-recommend-assignees.puml`

```plantuml
@startuml ssd-11-recommend-assignees
!include ssd-style.puml
title SSD 11 - UC8 Recommend Assignment Candidates

actor "PL" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- PL이 로그인되어 있다.
- 대상 Issue가 존재한다.
- UC5 startAssignment 흐름에서 include된다.
- PL은 대상 Issue가 속한 Project의 active PL이다.
end note

A -> S: startAssignment(issueId)
S --> A: assignmentOptions(devAssigneeCandidates, testerVerifierCandidates, allDevAssignees, allTesterVerifiers)

note over S
결과:
- Issue.status에 따라 UC5에 필요한 후보 종류를 반환한다.
- NEW/REOPENED: DEV assignee 후보와 TESTER verifier 후보를 반환한다.
- ASSIGNED: DEV assignee 후보만 반환한다.
- FIXED: TESTER verifier 후보만 반환한다.
- RESOLVED/CLOSED/DELETED: 배정 후보를 반환하지 않는다.

devAssigneeCandidates와 testerVerifierCandidates는 각각 top 3 후보이다.
allDevAssignees와 allTesterVerifiers는 프로젝트 소속 active 후보 전체이다.

추천 기준:
- assignee 후보는 과거 RESOLVED/CLOSED Issue의 fixer 이력을 참고한다.
- verifier 후보는 과거 RESOLVED/CLOSED Issue의 resolver 이력을 참고한다.
- 해결 이력이 없으면 completedIssueCount=0인 active project member를 후보로 반환한다.
- 해당 역할의 active project member가 없으면 빈 후보 목록을 반환한다.

UC8은 Issue 상태, 담당자, 코멘트, IssueHistory를 변경하지 않는다.
end note

@enduml
```

## SSD 12 - `ssd-12-delete-issue.puml`

```plantuml
@startuml ssd-12-delete-issue
!include ssd-style.puml
title SSD 12 - UC9 Delete Closed/New Issue
actor "PL" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건: PL 로그인, Issue 존재, Issue.status가 NEW 또는 CLOSED, 삭제 사유 존재, UC14 권한 검사
end note

A -> S: deleteIssue(issueId, comment)
S --> A: issueDeleted(issueId, status=DELETED, retentionResult)

note over S
결과: status가 NEW/CLOSED에서 DELETED로 변경됨
IssueHistory(STATUS_CHANGED, previousValue=NEW 또는 CLOSED, newValue=DELETED) 기록
삭제 시 해당 Issue와 연결된 IssueDependency association을 제거한다.
제거된 dependency는 restore 시 자동 복원하지 않는다.
DELETED Issue가 30개를 초과하면 시스템이 오래된 DELETED Issue부터 FIFO로 자동 물리 삭제한다.
end note
@enduml
```

## SSD 13 - `ssd-13-purge-deleted-issues.puml`

```plantuml
@startuml ssd-13-purge-deleted-issues
!include ssd-style.puml
title SSD 13 - UC9 Purge Deleted Issue / Deleted Issue Retention

actor "PL" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- PL이 로그인되어 있다.
- 단건 영구 삭제 대상 Issue가 DELETED 상태이다.
- 대상 Issue가 아직 물리 삭제되지 않았다.
- PL은 대상 Issue가 속한 Project의 active PL이다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

alt PL이 DELETED 이슈 하나를 영구 삭제한다
  A -> S: purgeDeletedIssue(issueId)
  S --> A: deletedIssuePurged(issueId, confirmation)

else soft delete 이후 DELETED 보관 한도가 초과된다
  A -> S: deleteIssue(issueId, comment)
  S --> A: issueDeleted(issueId, status=DELETED, retentionResult)
end

note over S
결과:
- 단건 영구 삭제는 선택한 DELETED Issue를 시스템에서 물리 삭제한다.
- Issue와 관련된 comments, issue histories, dependencies는 함께 제거된다.
- 단건 영구 삭제는 삭제/복구 사유 comment를 요구하지 않는다.

자동 FIFO 정리:
- 사용자가 직접 30개를 한꺼번에 purge하는 사용자 목표가 아니다.
- soft delete 이후 DELETED Issue가 보관 한도 30개를 초과하면 시스템 내부 정책으로 수행된다.
- 기준은 deleted transition time이며, 오래된 DELETED Issue부터 물리 삭제한다.
end note

@enduml
```

## SSD 14 - `ssd-14-view-issue-statistics.puml`

```plantuml
@startuml ssd-14-view-issue-statistics
!include ssd-style.puml
title SSD 14 - UC10 View Issue Statistics

actor "Auth User" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- Auth User가 로그인되어 있다.
- 사용자가 프로젝트를 선택해 프로젝트 화면에 진입했다.
- Auth User는 선택한 Project의 active member이다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

A -> S: viewStatistics(projectId, dailyPeriod, monthlyPeriod)
S --> A: statisticsReport(
  statusCounts,
  priorityCounts,
  dailyIssueCounts,
  monthlyIssueCounts,
  monthlyStatusCounts,
  monthlyPriorityCounts,
  dailyStatusChangeCounts,
  monthlyStatusChangeCounts,
  dailyCommentCounts,
  monthlyCommentCounts
)

note over S
결과:
- 선택한 Project 범위의 통계만 반환한다.
- 현재 상태별 Issue 개수를 반환한다.
- 현재 우선순위별 Issue 개수를 반환한다.
- 일별/월별 Issue 등록 수를 반환한다.
- 일별/월별 상태 변경 수를 반환한다.
- 일별/월별 코멘트 작성 수를 반환한다.
- 월별 상태별 Issue 개수를 반환한다.
- 월별 우선순위별 Issue 개수를 반환한다.

정책:
- 통계는 프로젝트 화면에서 선택된 projectId 기준으로 조회한다.
- 사용자가 해당 Project의 active member가 아니면 조회할 수 없다.
- DELETED 상태 Issue는 통계에서 제외한다.
- 기간 조건이 없으면 시스템 기본 조회 범위 또는 실제 집계 가능한 범위로 통계를 반환한다.
- 시작 기간이 종료 기간보다 늦으면 조회를 거부한다.
- 통계 조회는 Issue, Comment, IssueHistory를 변경하지 않는다.
end note

@enduml
```

## SSD 15 - `ssd-15-log-in.puml`

```plantuml
@startuml ssd-15-log-in
!include ssd-style.puml
title SSD 15 - UC11 Log In

actor "User" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- 사용자가 로그인 화면에 접근한다.
end note

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

note over S
결과:
- 성공 시 시스템은 현재 로그인 사용자를 세션에 기록한다.
- 실패 시 로그인 상태를 변경하지 않고 실패 사유를 반환한다.
- loginId는 앞뒤 공백을 제거해 조회한다.
- password는 사용자가 입력한 값을 그대로 인증에 사용한다.
end note

@enduml
```

## SSD 16 - `ssd-16-create-account.puml`

```plantuml
@startuml ssd-16-create-account
!include ssd-style.puml
title SSD 16 - UC12 Create Account

actor "Admin" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- Admin이 로그인되어 있다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

A -> S: createAccount(loginId, name, password, role)
S --> A: accountCreated(loginId, name, role, isActive=true, confirmation)

note over S
결과:
- User 계정이 생성된다.
- loginId, name, role이 저장된다.
- password는 평문으로 저장하지 않고 password hash로 저장한다.
- 새 계정은 active 상태로 생성된다.

정책:
- loginId, name, password, role은 필수 입력이다.
- loginId는 기존 계정과 중복될 수 없다.
- ADMIN 계정은 추가 생성할 수 없다.
- role=ADMIN인 계정은 생성할 수 없다.
end note

@enduml
```

## SSD 17 - `ssd-17-update-account.puml`

```plantuml
@startuml ssd-17-update-account
!include ssd-style.puml
title SSD 17 - UC12 Update Account

actor "Admin" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- Admin이 로그인되어 있다.
- 대상 User가 존재한다.
- 대상 User는 ADMIN 계정이 아니다.
- Admin은 자기 자신의 계정을 수정하지 않는다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

alt 계정 이름 변경
  A -> S: renameAccount(loginId, name)
  S --> A: accountRenamed(loginId, name, confirmation)

else 계정 역할 변경
  A -> S: changeAccountRole(loginId, role)
  S --> A: accountRoleChanged(loginId, role, confirmation)
end

note over S
결과:
- 이름 변경 경로에서는 User.name이 변경된다.
- 역할 변경 경로에서는 User.role이 변경된다.
- 변경 시 User.updatedAt이 갱신된다.

역할 변경 정책:
- ADMIN 계정으로 변경할 수 없다.
- 대상 User가 프로젝트 participant이면 역할 변경을 거부한다.
- 대상 User가 현재 진행 중인 assignee/verifier 책임을 가지고 있으면 역할 변경을 거부한다.
- fixer/resolver는 완료 이력으로 보며 현재 책임으로 보지 않는다.
end note

@enduml
```

## SSD 18 - `ssd-18-deactivate-account.puml`

```plantuml
@startuml ssd-18-activate-deactivate-account
!include ssd-style.puml
title SSD 18 - UC12 Activate / Deactivate Account

actor "Admin" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- Admin이 로그인되어 있다.
- 대상 User가 존재한다.
- 대상 User는 ADMIN 계정이 아니다.
- Admin은 자기 자신의 계정을 활성화/비활성화하지 않는다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

alt 계정 비활성화
  A -> S: deactivateAccount(loginId)
  S --> A: accountDeactivated(loginId, isActive=false, confirmation)

else 계정 활성화
  A -> S: activateAccount(loginId)
  S --> A: accountActivated(loginId, isActive=true, confirmation)
end

note over S
결과:
- 비활성화 경로에서는 User.isActive=false로 변경된다.
- 활성화 경로에서는 User.isActive=true로 변경된다.
- 변경 시 User.updatedAt이 갱신된다.

비활성화 정책:
- 대상 User가 프로젝트 participant이면 비활성화할 수 없다.
- 대상 User가 현재 진행 중인 assignee/verifier 책임을 가지고 있으면 비활성화할 수 없다.
- fixer/resolver는 완료 이력으로 보며 현재 책임으로 보지 않는다.

활성화 정책:
- 이미 active인 계정은 다시 활성화하지 않는다.
- inactive 계정만 활성화할 수 있다.
end note

@enduml
```

## SSD 19 - `ssd-19-create-project.puml`

```plantuml
@startuml ssd-19-create-project
!include ssd-style.puml
title SSD 19 - UC13 Create Project

actor "Admin" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- Admin이 로그인되어 있다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

A -> S: createProject(name, description)
S --> A: projectCreated(projectId, name, description, confirmation)

note over S
결과:
- Project가 생성된다.
- Project.name과 Project.description이 저장된다.
- Project.createdBy가 현재 Admin으로 기록된다.
- Project.createdAt과 Project.updatedAt이 기록된다.

정책:
- name은 필수이며 비어 있을 수 없다.
- description은 필수이며 비어 있을 수 없다.
- 같은 이름의 Project가 이미 존재하면 생성할 수 없다.
- 프로젝트 참여자 추가는 UC13의 별도 흐름에서 수행한다.
end note

@enduml
```

## SSD 20 - `ssd-20-add-project-member.puml`

```plantuml
@startuml ssd-20-add-project-member
!include ssd-style.puml
title SSD 20 - UC13 Add Project Member

actor "Admin" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- Admin이 로그인되어 있다.
- 대상 Project가 존재한다.
- 추가할 User가 존재한다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

A -> S: addProjectParticipant(projectId, loginId)
S --> A: projectParticipantAdded(projectId, loginId, confirmation)

note over S
결과:
- User participates-in Project association이 생성된다.
- 추가된 User는 해당 Project 범위의 작업에 참여할 수 있다.

정책:
- active User만 프로젝트 참여자로 추가할 수 있다.
- ADMIN 계정은 프로젝트 참여자로 추가할 수 없다.
- 이미 참여 중인 User는 다시 추가할 수 없다.
- Project에는 PL을 한 명만 추가할 수 있다.
end note

@enduml
```

## SSD 21 - `ssd-21-remove-project-member.puml`

```plantuml
@startuml ssd-21-remove-project-member
!include ssd-style.puml
title SSD 21 - UC13 Remove Project Member

actor "Admin" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- Admin이 로그인되어 있다.
- 대상 Project가 존재한다.
- 제거할 User가 대상 Project에 참여 중이다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

A -> S: removeProjectParticipant(projectId, loginId)
S --> A: projectParticipantRemoved(projectId, loginId, confirmation)

note over S
결과:
- User participates-in Project association이 제거된다.
- 제거된 User는 해당 Project의 일반 작업 범위에서 제외된다.

정책:
- 대상 User가 현재 진행 중인 Issue의 assignee 또는 verifier이면 제거할 수 없다.
- 현재 진행 중인 책임은 ASSIGNED/FIXED 상태의 assignee/verifier를 기준으로 판단한다.
- fixer/resolver는 완료 이력으로 보며 제거 차단 조건으로 보지 않는다.
- PL participant 제거는 허용한다.
end note

@enduml
```

## SSD 22 - `ssd-22-verify-permission.puml`

```plantuml
@startuml ssd-22-verify-permission
!include ssd-style.puml
title SSD 22 - UC14 권한 검사
actor "Actor" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: Actor 로그인, 보호된 operation/resource 식별
이 그림은 직접 사용자 목표 SSD가 아니라 include되는 공통 권한 검사 참고도이다.
end note
A -> S: assertCanProtectedOperation(actor, resource)
alt 권한 있음
  S --> A: permissionVerified()
else 권한 없음
  S --> A: permissionDenied(reason)
end
note over S
결과: 대부분의 base UC에 include되는 공통 권한 조건
end note
@enduml
```

## SSD 23 - `ssd-23-edit-issue.puml`

```plantuml
@startuml ssd-23-edit-issue
!include ssd-style.puml
title SSD 23 - UC15 Edit Issue

actor "Reporter" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- Reporter가 로그인되어 있다.
- 대상 Issue가 존재한다.
- Reporter는 대상 Issue를 등록한 사용자이다.
- 대상 Issue가 속한 Project의 active member이다.
- 대상 Issue는 DELETED 상태가 아니다.
- 대상 Issue.status는 NEW 또는 REOPENED이다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

A -> S: updateIssue(issueId, title, description)
S --> A: issueUpdated(issueId, title, description, confirmation)

note over S
결과:
- Issue.title이 변경된다.
- Issue.description이 변경된다.
- Issue.updatedAt이 갱신된다.
- IssueHistory(TITLE_DESCRIPTION_UPDATED)가 기록된다.

정책:
- 같은 Project 안에서 다른 Issue와 title이 중복되면 수정할 수 없다.
- 같은 title/description으로 수정하는 경우는 변경으로 보지 않는다.
- priority 변경은 UC16 Change Priority에서 처리한다.
- status 변경은 UC6 Change Issue State에서 처리한다.
- assigned 이후의 정정이나 보충 설명은 comment로 남긴다.
end note

@enduml
```

## SSD 24 - `ssd-24-add-dependency.puml`

```plantuml
@startuml ssd-24-add-dependency
!include ssd-style.puml
title SSD 24 - UC7 Add Dependency

actor "PL" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- PL이 로그인되어 있다.
- blocking Issue와 blocked Issue가 존재한다.
- 두 Issue는 같은 Project에 속한다.
- 두 Issue는 DELETED 상태가 아니다.
- blocked Issue는 RESOLVED 또는 CLOSED 상태가 아니다.
- 자기참조 dependency가 아니다.
- 순환 dependency가 아니다.
- 동일한 dependency가 아직 존재하지 않는다.
- PL은 대상 Project의 active PL이다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

A -> S: addDependency(blockingIssueId, blockedIssueId)
S --> A: dependencyAdded(dependencyId, blockingIssueId, blockedIssueId, confirmation)

note over S
결과:
- IssueDependency가 생성된다.
- blocking Issue와 blocked Issue 사이의 dependency association이 설정된다.
- dependencyId는 blockingIssueId와 blockedIssueId 조합에서 생성된다.
- dependency 변경 내역은 blocked Issue의 IssueHistory(DEPENDENCY_CHANGED)에 기록된다.

정책:
- dependency는 comment가 아니라 구조화된 관계 데이터로 관리한다.
- dependency 효과는 UC6 FIXED -> RESOLVED 전이에서 blocking Issue가 모두 RESOLVED/CLOSED인지 검사할 때 반영된다.
end note

@enduml
```

## SSD 25 - `ssd-25-remove-dependency.puml`

```plantuml
@startuml ssd-25-remove-dependency
!include ssd-style.puml
title SSD 25 - UC7 Remove Dependency

actor "PL" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- PL이 로그인되어 있다.
- blocking Issue와 blocked Issue가 존재한다.
- 두 Issue는 같은 Project에 속한다.
- 두 Issue는 DELETED 상태가 아니다.
- 두 Issue 사이의 dependency가 존재한다.
- PL은 대상 Project의 active PL이다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

A -> S: removeDependency(blockingIssueId, blockedIssueId)
S --> A: dependencyRemoved(blockingIssueId, blockedIssueId, confirmation)

note over S
결과:
- blocking Issue와 blocked Issue 사이의 IssueDependency가 제거된다.
- dependency association이 제거된다.
- dependency 제거 내역은 blocked Issue의 IssueHistory(DEPENDENCY_CHANGED)에 기록된다.
- 응답은 확인 메시지 중심이며, 별도 domain 객체 반환은 필수로 보지 않는다.

정책:
- dependency는 comment가 아니라 구조화된 관계 데이터로 관리한다.
- 존재하지 않는 dependency 제거 요청은 거부된다.
- 다른 Project에 속한 Issue 간 dependency 제거 요청은 거부된다.
end note

@enduml
```

## SSD 26 - `ssd-26-restore-deleted-issue.puml`

```plantuml
@startuml ssd-26-restore-deleted-issue
!include ssd-style.puml
title SSD 26 - UC9 Restore Deleted Issue

actor "PL" as A
participant ":Issue Tracking System" as S

note over A, S
사전조건:
- PL이 로그인되어 있다.
- 대상 Project가 존재한다.
- 복구 대상 Issue가 DELETED 상태이다.
- 대상 Issue가 아직 물리 삭제되지 않았다.
- PL은 대상 Issue가 속한 Project의 active PL이다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

A -> S: viewDeletedIssues(projectId)
S --> A: deletedIssueList(issues)

A -> S: restoreIssue(issueId, comment)
S --> A: issueRestored(issueId, status=NEW or CLOSED, confirmation)

note over S
UC9는 UC2 Add Comment를 include한다.
따라서 restoreIssue의 comment는 복구 사유로 필수 입력된다.

결과:
- Issue.status가 DELETED에서 삭제 직전 상태로 복구된다.
- 삭제 직전 상태는 NEW/CLOSED -> DELETED 전이 때 기록된 IssueHistory(STATUS_CHANGED).previousValue에서 결정된다.
- 별도 Issue.preDeleteStatus attribute는 두지 않는다.
- IssueHistory(STATUS_CHANGED, previousValue=DELETED, newValue=preDeleteStatus)를 기록한다.
- 복구 사유 comment가 대상 Issue에 기록된다.
- reporter/fixer/resolver, comments, history는 유지하고 새 Issue는 생성하지 않는다.
- assignee와 verifier는 null 상태로 유지한다.
- 삭제 시 제거된 dependency는 자동 복원하지 않으며, 필요하면 UC7에서 다시 설정한다.
- preDeleteStatus가 CLOSED이고 재작업이 필요하면 UC6 Reopen Issue를 별도로 수행한다.
end note

@enduml
```

## SSD 27 - `ssd-27-change-priority.puml`

```plantuml
@startuml ssd-27-change-priority
!include ssd-style.puml
title SSD 27 - UC16 우선순위 변경
actor "PL" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: PL 로그인, Issue 존재, newPriority 값 유효, 현재 priority와 다름, UC14 권한 검사
end note
A -> S: changePriority(issueId, newPriority)
S --> A: priorityChanged(issueId, priority=newPriority)
note over S
결과: Issue.priority 변경, IssueHistory(PRIORITY_CHANGED, previousValue, newValue) 기록
Issue.status와 assignment 관련 association은 변경하지 않음
end note
@enduml
```
