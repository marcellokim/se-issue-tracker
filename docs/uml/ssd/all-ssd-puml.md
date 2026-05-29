# SSD PUML 전체 모음

이 파일은 docs/uml/ssd/의 SSD 01-27 PUML 원문을 공유하기 쉽게 하나의 Markdown 파일로 모은 것이다.

- Source directory: `docs/uml/ssd/`
- Style include: 각 PUML은 기존처럼 `!include ssd-style.puml`을 유지한다.

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
end note
A -> S: registerIssue(title, description, priority)
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
title SSD 03 - UC3 이슈 검색/브라우즈
actor "Auth User" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: 사용자 로그인, 조회 가능한 Project 존재
end note
A -> S: searchIssues(issueSearchFilters)
S --> A: issueList(issues)
note over S
issueSearchFilters:
titleKeyword, assignee, reporter, status, priority, reportedDateRange, project
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
사전조건: Auth User 로그인, Issue 존재
end note
A -> S: viewIssueDetail(issueId)
S --> A: issueDetails(issue, comments, history, dependencies, availableActions)
note over S
결과: 도메인 상태를 변경하지 않음.
UC15 Edit Issue는 UC4를 확장하는 subfunction이다.
UC5와 UC6은 별도 actor-goal UC에서 실행된다.
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
- Issue가 NEW, ASSIGNED, FIXED 또는 REOPENED 상태이다.
- 필요한 DEV assignee 또는 Tester verifier 후보가 존재한다.
- UC14 권한 검사는 시스템 내부에서 수행된다.
end note

note over A, S
공통 흐름:
- 각 상태별 branch는 UC8 Recommend Assignment Candidates를 include한다.
- UC2 코멘트 추가는 배정/변경 사유가 필요할 때 선택적으로 수행한다.
end note

A -> S: startAssignment(issueId)
alt Issue.status == NEW (NEW -> ASSIGNED)
  ref over A, S : UC8 Recommend Assignment Candidates\nrecommendAssignmentCandidates(issueId)
  S --> A: assignmentOptions(devList, testerList, assigneeCandidates, verifierCandidates)
  note over S
  If reporter is a Tester, the reporter is prioritized as the default verifier candidate.
  end note
  opt UC2 코멘트 추가 선택
    ref over A, S : UC2 Add Comment\naddComment(issueId, content)
  end
  A -> S: assignIssue(issueId, assigneeId, verifierId)
  S --> A: issueAssigned(issueId, status=ASSIGNED, confirmation)
else Issue.status == ASSIGNED (ASSIGNED -> ASSIGNED)
  ref over A, S : UC8 Recommend Assignment Candidates\nrecommendAssignmentCandidates(issueId)
  S --> A: reassignmentOptions(devList, currentAssignee, assigneeCandidates)
  opt UC2 코멘트 추가 선택
    ref over A, S : UC2 Add Comment\naddComment(issueId, content)
  end
  A -> S: reassignIssue(issueId, assigneeId)
  S --> A: issueReassigned(issueId, status=ASSIGNED, confirmation)
else Issue.status == FIXED (FIXED -> FIXED)
  ref over A, S : UC8 Recommend Assignment Candidates\nrecommendAssignmentCandidates(issueId)
  S --> A: verifierChangeOptions(testerList, currentVerifier, verifierCandidates)
  opt UC2 코멘트 추가 선택
    ref over A, S : UC2 Add Comment\naddComment(issueId, content)
  end
  A -> S: changeVerifier(issueId, verifierId)
  S --> A: verifierChanged(issueId, status=FIXED, confirmation)
else Issue.status == REOPENED (REOPENED -> ASSIGNED)
  ref over A, S : UC8 Recommend Assignment Candidates\nrecommendAssignmentCandidates(issueId)
  S --> A: assignmentOptions(devList, testerList, assigneeCandidates, verifierCandidates, preservedFixer, preservedResolver)
  opt UC2 코멘트 추가 선택
    ref over A, S : UC2 Add Comment\naddComment(issueId, content)
  end
  A -> S: assignIssue(issueId, assigneeId, verifierId)
  S --> A: issueAssigned(issueId, status=ASSIGNED, fixer=preserved, resolver=preserved, confirmation)
end

note over S
결과:
- NEW -> ASSIGNED: assignee DEV와 verifier Tester association을 설정하고 status=ASSIGNED로 변경한다.
- ASSIGNED -> ASSIGNED: assignee DEV만 변경하고 status=ASSIGNED를 유지한다. fixer/resolver는 변경하지 않는다.
- FIXED -> FIXED: verifier Tester만 변경하고 status=FIXED를 유지한다. fixer/resolver는 변경하지 않는다.
- REOPENED -> ASSIGNED: assignee와 verifier를 PL 선택값으로 재설정하고 status=ASSIGNED로 변경한다. 기존 fixer/resolver는 보존한다.
- 모든 branch는 IssueHistory(ASSIGNMENT_CHANGED)를 기록한다.
- NEW -> ASSIGNED와 REOPENED -> ASSIGNED는 IssueHistory(STATUS_CHANGED)도 함께 기록한다.
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
결과: status=RESOLVED, resolver=current Tester 기록, assignee/verifier/fixer 유지, 필수 Comment와 IssueHistory(STATUS_CHANGED, previousValue=FIXED, newValue=RESOLVED) 기록
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
title SSD 11 - UC8 Assignment Candidate 추천 Reference
actor "PL" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: PL 로그인, 대상 Issue 존재, UC5 startAssignment 흐름에서 include됨
end note
A -> S: recommendAssignmentCandidates(issueId)
S --> A: assignmentCandidates(assigneeCandidates, verifierCandidates, reasons)
note over S
결과: Issue.status에 따라 UC5에 필요한 후보 종류를 반환한다.
NEW/REOPENED: assignee DEV 후보와 verifier Tester 후보를 반환한다.
ASSIGNED: assignee DEV 후보만 반환한다.
FIXED: verifier Tester 후보만 반환한다.
assignee 후보는 과거 RESOLVED/CLOSED Issue의 fixer 이력을 우선 참고한다.
verifier 후보는 과거 RESOLVED/CLOSED Issue의 resolver 이력을 우선 참고한다.
Recommendation 도메인 객체와 IssueHistory는 생성하지 않음
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
사전조건: PL 로그인, Issue 존재, Issue.status가 NEW 또는 CLOSED, UC14 권한 검사
end note
A -> S: deleteIssue(issueId)
S --> A: issueDeleted(issueId, status=DELETED, retentionResult)
note over S
결과: status가 NEW/CLOSED에서 DELETED로 변경됨
NEW/CLOSED 대상은 assignee/verifier가 null인 상태로 취급한다.
IssueHistory(STATUS_CHANGED, previousValue=NEW 또는 CLOSED, newValue=DELETED) 기록
삭제 시 해당 Issue와 연결된 IssueDependency association을 제거한다.
제거된 dependency는 restore 시 자동 복원하지 않는다.
DELETED Issue가 30개를 초과하면 deleted transition time 기준 oldest-first FIFO로 물리 삭제
end note
@enduml
```

## SSD 13 - `ssd-13-purge-deleted-issues.puml`

```plantuml
@startuml ssd-13-purge-deleted-issues
!include ssd-style.puml
title SSD 13 - UC9 삭제 이슈 보관 정책 Reference
participant ":Issue Tracking System" as S
note over S
이 그림은 actor-goal SSD가 아니라 UC9 deleteIssue(issueId) 내부 보관 정책 참고도이다.
DELETED Issue가 30개를 초과하면 deleted transition time 기준 oldest-first FIFO로 오래된 DELETED Issue를 물리 삭제한다.
보관 정책은 별도 외부 system operation으로 공개하지 않는다.
end note
@enduml
```

## SSD 14 - `ssd-14-view-issue-statistics.puml`

```plantuml
@startuml ssd-14-view-issue-statistics
!include ssd-style.puml
title SSD 14 - UC10 이슈 통계 조회
actor "Auth User" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: Auth User 로그인, 사용자가 프로젝트를 선택해 프로젝트 화면에 진입,
현재 Project 안에 조회 가능한 Issue 데이터 존재, UC14 권한 검사
end note
A -> S: viewStatistics(period, filters)
S --> A: statisticsReport(issueCounts, trendData, statusBreakdown, priorityBreakdown)
note over S
결과: 현재 Project 범위의 일/월별 발생 수, trend, status/priority breakdown 반환
통계는 프로젝트 화면의 현재 Project context를 사용한다.
UI는 선택된 projectId를 controller에 전달할 수 있지만,
사용자가 임의 입력하는 검색 필드로 projectId를 받지 않는다.
currentUserRole은 인증 context에서 결정하며 사용자가 임의 입력하지 않는다.
filters.scope는 전체/내 직군/특정 직군별 통계 범위를 구분한다.
Statistics 도메인 객체와 IssueHistory는 생성하지 않음
end note
@enduml
```

## SSD 15 - `ssd-15-log-in.puml`

```plantuml
@startuml ssd-15-log-in
!include ssd-style.puml
title SSD 15 - UC11 로그인
actor "User" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: User 존재, isActive=true
end note
A -> S: login(loginId, password)
alt 인증 성공
  S --> A: loginSucceeded(userId, role, name)
else 인증 실패
  S --> A: loginFailed(reason)
end
note over S
결과: 성공 시 loginId로 현재 사용자 식별, 실패 시 거부
end note
@enduml
```

## SSD 16 - `ssd-16-create-account.puml`

```plantuml
@startuml ssd-16-create-account
!include ssd-style.puml
title SSD 16 - UC12 계정 생성
actor "Admin" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: Admin 로그인, loginId 중복 없음, UC14 권한 검사
end note
A -> S: createAccount(loginId, name, password, role)
S --> A: accountCreated(userId, loginId, role, isActive=true)
note over S
결과: User 생성, loginId/name/role 저장, password는 passwordHash로 저장, isActive=true
end note
@enduml
```

## SSD 17 - `ssd-17-update-account.puml`

```plantuml
@startuml ssd-17-update-account
!include ssd-style.puml
title SSD 17 - UC12 계정 관리: 계정 역할 변경
actor "Admin" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: Admin 로그인, 대상 User 존재, UC14 권한 검사
end note
A -> S: renameAccount(loginId, name)
A -> S: changeAccountRole(loginId, role)
S --> A: accountUpdated(userId, name, role)
note over S
결과: User의 name/role 수정, isActive 변경은 deactivateAccount에서 처리
end note
@enduml
```

## SSD 18 - `ssd-18-deactivate-account.puml`

```plantuml
@startuml ssd-18-deactivate-account
!include ssd-style.puml
title SSD 18 - UC12 계정 관리: 계정 비활성화
actor "Admin" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: Admin 로그인, 대상 User 존재, User.isActive=true, UC14 권한 검사
end note
A -> S: deactivateAccount(userId)
S --> A: accountDeactivated(userId, isActive=false)
note over S
결과: User.isActive=false, 이후 로그인/작업 제한
end note
@enduml
```

## SSD 19 - `ssd-19-create-project.puml`

```plantuml
@startuml ssd-19-create-project
!include ssd-style.puml
title SSD 19 - UC13 프로젝트 생성
actor "Admin" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: Admin 로그인, project name 유효 및 중복 없음, UC14 권한 검사
end note
A -> S: createProject(name, description)
S --> A: projectCreated(projectId, name, createdDate)
note over S
결과: Project 생성, description과 createdDate 저장, Admin manages association 설정
end note
@enduml
```

## SSD 20 - `ssd-20-add-project-member.puml`

```plantuml
@startuml ssd-20-add-project-member
!include ssd-style.puml
title SSD 20 - UC13 프로젝트 멤버 추가
actor "Admin" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: Admin 로그인, Project와 active User 존재, 아직 참여자가 아님, UC14 권한 검사
end note
A -> S: addProjectParticipant(projectId, userId)
S --> A: projectParticipantAdded(projectId, userId, confirmation)
note over S
결과: User participates-in Project association 설정, 해당 Project 작업 가능
end note
@enduml
```

## SSD 21 - `ssd-21-remove-project-member.puml`

```plantuml
@startuml ssd-21-remove-project-member
!include ssd-style.puml
title SSD 21 - UC13 프로젝트 멤버 제거
actor "Admin" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: Admin 로그인, User participates-in Project association 존재, UC14 권한 검사
end note
A -> S: removeProjectParticipant(projectId, userId)
S --> A: projectParticipantRemoved(projectId, userId)
note over S
결과: User participates-in Project association 제거, 해당 Project 접근 제한
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
title SSD 23 - UC15 이슈 수정
actor "Reporter" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: Reporter 로그인, 본인이 등록한 Issue, status=NEW, UC14 권한 검사
end note
A -> S: updateIssue(issueId, newTitle, newDescription)
S --> A: issueUpdated(issueId, title, description)
note over S
결과: title/description 수정, IssueHistory(TITLE_DESCRIPTION_UPDATED) 기록
priority 변경은 UC16, status 변경은 UC6에서만 처리
end note
@enduml
```

## SSD 24 - `ssd-24-add-dependency.puml`

```plantuml
@startuml ssd-24-add-dependency
!include ssd-style.puml
title SSD 24 - UC7 의존성 추가
actor "PL" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: PL 로그인, 두 Issue 존재, 자기참조/순환 아님, UC14 권한 검사
end note
A -> S: addDependency(blockingIssueId, blockedIssueId)
S --> A: dependencyAdded(dependencyId)
note over S
결과: IssueDependency 생성, blockingIssue/blockedIssue association 설정
dependencyId는 blockingIssueId와 blockedIssueId 조합에서 파생
별도 dependencyName/type 파라미터는 추가하지 않음
의존성 효과는 UC6 FIXED -> RESOLVED guard에서만 검사
IssueHistory(DEPENDENCY_CHANGED) 기록
end note
@enduml
```

## SSD 25 - `ssd-25-remove-dependency.puml`

```plantuml
@startuml ssd-25-remove-dependency
!include ssd-style.puml
title SSD 25 - UC7 의존성 제거
actor "PL" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: PL 로그인, IssueDependency 존재, UC14 권한 검사
end note
A -> S: removeDependency(dependencyId)
S --> A: dependencyRemoved()
note over S
결과: IssueDependency 제거, blockingIssue/blockedIssue association 제거
응답은 확인용이며 반환 payload는 없음
IssueHistory(DEPENDENCY_CHANGED) 기록
end note
@enduml
```

## SSD 26 - `ssd-26-restore-deleted-issue.puml`

```plantuml
@startuml ssd-26-restore-deleted-issue
!include ssd-style.puml
title SSD 26 - UC9 Restore Closed/New Issue
actor "PL" as A
participant ":Issue Tracking System" as S
note over A, S
사전조건: PL 로그인, Project 존재, 복구 대상 Issue가 DELETED 상태, 물리 삭제 전, UC14 권한 검사
end note
A -> S: viewDeletedIssues(projectId)
S --> A: deletedIssueList(issues)
A -> S: restoreIssue(issueId)
S --> A: issueRestored(issueId, status=NEW or CLOSED)
note over S
결과: status=preDeleteStatus(NEW 또는 CLOSED), 삭제 보관 정책 대상에서 제외
preDeleteStatus는 NEW/CLOSED -> DELETED 전이 때 기록된 IssueHistory(STATUS_CHANGED).previousValue에서 결정
새 Issue.preDeleteStatus attribute는 도입하지 않음
IssueHistory(STATUS_CHANGED, previousValue=DELETED, newValue=preDeleteStatus) 기록
reporter/fixer/resolver, comments, history는 유지하고 새 Issue는 생성하지 않음
assignee와 verifier는 null 상태로 유지한다.
삭제 시 제거된 dependency는 자동 복원하지 않으며 필요하면 UC7에서 다시 설정한다.
preDeleteStatus가 CLOSED이고 재작업이 필요하면 UC6 Reopen Issue에서 CLOSED -> REOPENED를 수행한다.
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
