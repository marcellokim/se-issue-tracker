# SSD 후보 목록

이 문서는 제출용 SSD 2개와 별도로, 과제 정합성 확인과 개발 기준 정리를 위해 만들 수 있는 SSD 후보 전체를 정리한다.

## 기준

- 제출용 SSD는 2개만 사용한다.
  - `UC1 Register Issue`
  - `UC5 Assign / Update Issue Assignment`
- 전체 PUML source는 `docs/uml/ssd/`에 둔다. PNG/SVG는 필요할 때 생성하는 산출물이다.
- SSD는 시스템을 black-box로 보고 actor와 `:Issue Tracking System` 사이의 system event만 표현한다.
- 내부 controller, service, repository, entity, database lifeline은 SSD에 넣지 않는다.
- UC14 권한 검사는 protected operation의 공통 조건이다. 독립 SSD는 참고용으로만 두고, 실제 base SSD에서는 precondition/note로 표현한다.
- UC6 Change Issue State는 하나의 UC지만 상태 전이 시나리오가 여러 개이므로 개발 기준에서는 여러 SSD 후보로 나눈다.
- UC9 Delete/Manage Deleted Issue는 삭제, 보관 정책, 복구를 분리해 개발 기준을 명확히 한다.

## 제출용 SSD

| File | UC | Actor | System Operation | 제출 선택 이유 |
| --- | --- | --- | --- | --- |
| `docs/uml/ssd/ssd-01-register-issue.puml` | UC1 이슈 등록 | Auth User | `registerIssue(title, description, priority)` | 이슈 생성, reporter/reportedDate/status `NEW`, history 생성 요구와 직접 대응한다. |
| `docs/uml/ssd/ssd-05-assign-issue.puml` | UC5 이슈 배정/배정 변경 | PL | `startAssignment(issueId)`, `assignIssue(...)`, `reassignIssue(...)`, `changeVerifier(...)` | NEW/REOPENED 배정, ASSIGNED assignee 재배정, FIXED verifier 교체를 보여준다. |

제출용 이미지는 `ssd-01-register-issue.puml`, `ssd-05-assign-issue.puml`에서 생성한 렌더 산출물을 사용한다.

## 전체 SSD 후보 목록

| 우선순위 | UC | SSD 후보 이름 | Actor | Main System Operation | 주요 결과/검증 포인트 | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| 제출 | UC1 이슈 등록 | Register Issue | Auth User | `registerIssue(title, description, priority)` | `Issue` 생성, reporter 자동 저장, reportedDate 자동 저장, status `NEW`, history 생성 | 제출용으로 확정 |
| 개발 핵심 | UC2 코멘트 추가 | Add Comment | Auth User | `addComment(issueId, content)` | `Comment` 생성, writer/createdDate 저장, `IssueHistory(COMMENTED)` 기록 | UC1/UC4/UC5에서는 optional extension, UC6에서는 mandatory include |
| 개발 핵심 | UC3 이슈 검색/브라우즈 | Search Issues | Auth User | `searchIssues(criteria)` | assignee/status/reporter/project 조건 검색, 목록 반환 | 조회 UC라 Operation Contract 우선순위는 낮음 |
| 개발 핵심 | UC4 이슈 상세 조회 | View Issue Detail | Auth User | `viewIssueDetail(issueId)` | 이슈 필드, comments, history, dependencies, availableActions 반환 | UC5/UC6/UC15는 상세 화면에서 시작될 수 있지만 별도 actor-goal UC |
| 제출 | UC5 이슈 배정/배정 변경 | Assign / Update Issue Assignment | PL | `startAssignment(issueId)`, `assignIssue(...)`, `reassignIssue(...)`, `changeVerifier(...)` | NEW/REOPENED는 assignee+verifier 후보 추천 후 ASSIGNED로 배정, ASSIGNED는 assignee 변경, FIXED는 verifier 변경 | UC8 상태별 후보 추천은 include, UC2 배정 코멘트는 optional extend |
| 개발 핵심 | UC6 상태 변경 | Mark Fixed | DEV | `changeStatus(issueId, targetStatus=FIXED, comment)` | status `ASSIGNED -> FIXED`, fixer 자동 기록, 필수 comment/history 생성 | assignee 본인만 가능 |
| 개발 핵심 | UC6 상태 변경 | Resolve Issue | Tester | `changeStatus(issueId, targetStatus=RESOLVED, comment)` | status `FIXED -> RESOLVED`, resolver 자동 기록, 필수 comment/history 생성 | verifier 본인만 가능, blocking Issue가 모두 `RESOLVED` 또는 `CLOSED`여야 함 |
| 개발 핵심 | UC6 상태 변경 | Reject Fixed Issue | Tester | `changeStatus(issueId, targetStatus=ASSIGNED, comment)` | status `FIXED -> ASSIGNED`, 기존 assignee/verifier/fixer retained, 실패 사유 comment/history 저장 | tester rollback, 새 Dev는 UC5-RA에서 변경 |
| 개발 핵심 | UC6 상태 변경 | Close Issue | PL | `changeStatus(issueId, targetStatus=CLOSED, comment)` | status `RESOLVED -> CLOSED`, assignee/verifier null, fixer/resolver 보존, 필수 comment/history 생성 | PL 권한 |
| 개발 핵심 | UC6 상태 변경 | Reopen Issue | PL | `changeStatus(issueId, targetStatus=REOPENED, comment)` | status `RESOLVED/CLOSED -> REOPENED`, fixer/resolver 보존, assignee/verifier 자동복원 없음 | UC5로 배정 가능 |
| 개발 보조 | UC8 Assignment Candidate 추천 | Recommend Assignment Candidates | PL | `recommendAssignmentCandidates(issueId)` | Issue status에 따라 assignee DEV 후보 또는 verifier Tester 후보를 반환; assignee 후보는 fixer 이력, verifier 후보는 resolver 이력 참고 | UC5의 include, reference SSD, 도메인 변경 없음 |
| 개발 핵심 | UC9 삭제 이슈 관리 | Delete Closed/New Issue | PL | `deleteIssue(issueId)` | status `NEW/CLOSED -> DELETED`, 관련 dependency 제거, status change history 생성, 30개 초과 FIFO 보관 정책 적용 | soft-delete |
| 개발 보조 | UC9 삭제 이슈 관리 | Deleted Issue Retention Policy | Internal Policy | 외부 system operation 없음 | DELETED 이슈 30개 초과 시 deleted transition time 기준 FIFO 물리 삭제 | actor-goal SSD가 아닌 정책 reference |
| 개발 핵심 | UC9 삭제 이슈 관리 | Restore Closed/New Issue | PL | `viewDeletedIssues(projectId)`, `restoreIssue(issueId)` | status `DELETED -> previousStatus(NEW 또는 CLOSED)`, assignee/verifier null 유지, 보관 정책 대상에서 제외 | previousStatus는 삭제 시 기록된 `IssueHistory(STATUS_CHANGED).previousValue`에서 결정, dependency 자동 복구 없음 |
| 개발 보조 | UC10 이슈 통계 | View Issue Statistics | Auth User | `viewStatistics(period, filters)` | issueCounts, trendData, statusBreakdown, priorityBreakdown 반환 | 현재 Project context 사용, filters.scope로 전체/직군별 통계 범위 구분 |
| 개발 보조 | UC11 로그인 | Log In | Admin/Auth User | `login(loginId, password)` | 인증 성공, 현재 사용자 식별 | 인증 식별자는 UC12의 `loginId`와 통일 |
| 개발 핵심 | UC12 계정 관리 | Create Account | Admin | `createAccount(loginId, name, password, role)` | `User` 생성, loginId/name/role 저장, password는 passwordHash로 저장, isActive=true | Admin이 다른 actor 계정을 준비 |
| 개발 보조 | UC12 계정 관리 | Update Account | Admin | `renameAccount(loginId, name)`, `changeAccountRole(loginId, role)` | `User.name`, `User.role` 변경 | `isActive`는 Deactivate Account에서만 처리 |
| 개발 보조 | UC12 계정 관리 | Deactivate Account | Admin | `deactivateAccount(userId)` | `User.isActive=false` | Admin-only |
| 개발 핵심 | UC13 프로젝트 관리 | Create Project | Admin | `createProject(name, description)` | `Project` 생성, createdDate 저장, Admin manages association 설정 | 과제 데모 project 준비 |
| 개발 보조 | UC13 프로젝트 관리 | Add Project Participant | Admin | `addProjectParticipant(projectId, userId)` | `User participates-in Project` association 생성 | 별도 membership class는 두지 않음 |
| 개발 보조 | UC13 프로젝트 관리 | Remove Project Participant | Admin | `removeProjectParticipant(projectId, userId)` | `User participates-in Project` association 제거 | 별도 membership class는 두지 않음 |
| 내부 공통 | UC14 권한 검사 | Verify Permission | Actor/Internal | `assertCanProtectedOperation(actor, resource)` | 권한 있음/없음 alt로 base operation 진행 또는 거부 | 직접 사용자 목표 SSD가 아니라 include되는 공통 권한 검사 reference |
| 개발 핵심 | UC15 이슈 수정 | Edit Issue | Reporter | `updateIssue(issueId, newTitle, newDescription)` | status `NEW`인 본인 이슈 title/description 수정, history 저장 | priority는 UC16, status는 UC6 |
| 개발 핵심 | UC7 의존성 관리 | Add Dependency | PL | `addDependency(blockingIssueId, blockedIssueId)` | `IssueDependency` 생성, 순환/자기참조 방지, history 저장 | 별도 dependencyName/type 파라미터 없음, 효과는 UC6 `FIXED -> RESOLVED` guard |
| 개발 보조 | UC7 의존성 관리 | Remove Dependency | PL | `removeDependency(dependencyId)` | dependency 제거, history 저장, `dependencyRemoved()` 확인 응답 | 응답에 반환 payload 없음 |
| 개발 핵심 | UC16 우선순위 변경 | Change Priority | PL | `changePriority(issueId, newPriority)` | `Issue.priority` 변경, `IssueHistory(PRIORITY_CHANGED)` 기록 | PL-only |

## Operation Contract 우선순위

Operation Contract는 상태 변화가 크고 도메인 객체 변경이 명확한 operation부터 작성한다.

1. `registerIssue(title, description, priority)`
2. `assignIssue(issueId, assigneeId, verifierId)`
3. `reassignIssue(issueId, assigneeId)`
4. `changeVerifier(issueId, verifierId)`
5. `changeStatus(issueId, targetStatus=FIXED, comment)`
6. `changeStatus(issueId, targetStatus=RESOLVED, comment)`
7. `changeStatus(issueId, targetStatus=ASSIGNED, comment)`
8. `changeStatus(issueId, targetStatus=CLOSED, comment)`
9. `changeStatus(issueId, targetStatus=REOPENED, comment)`
10. `updateIssue(issueId, newTitle, newDescription)`
11. `deleteIssue(issueId)`
12. `restoreIssue(issueId)`
13. `changePriority(issueId, newPriority)`
14. `addDependency(blockingIssueId, blockedIssueId)`
15. `createAccount(loginId, name, password, role)`
16. `createProject(name, description)`

조회성 operation인 `searchIssues`, `viewIssueDetail`, `viewStatistics`, `recommendAssignmentCandidates`, `viewDeletedIssues`, `startAssignment`는 SSD 작성은 가능하지만 domain object 변경이 없거나 약하므로 Operation Contract 우선순위는 낮다.

## 개발 기준 권장 세트

제출 이후 개발 정확도를 위해 추가로 그린다면 아래 순서가 적절하다.

1. `Mark Fixed`
2. `Resolve Issue`
3. `Reject Fixed Issue`
4. `Close Issue`
5. `Reopen Issue`
6. `Edit Issue`
7. `Delete Closed/New Issue`
8. `Restore Closed/New Issue`
9. `Change Priority`
10. `Add Dependency`
11. `Create Account`
12. `Create Project`

이 세트는 과제 데모 시나리오와 팀 확정 정책인 fixed, resolved, tester rollback, reporter edit 제한, soft-delete/restore, priority, dependency, 계정/프로젝트 준비를 대부분 덮는다.
