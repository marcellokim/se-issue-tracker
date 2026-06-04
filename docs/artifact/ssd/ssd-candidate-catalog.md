# SSD 후보 목록

이 문서는 제출용 SSD와 별도로, 과제 정합성 확인과 개발 기준 정리를 위해 만들 수 있는 SSD 후보 전체를 정리한다.

## 기준

- 제출용 SSD는 3개를 사용한다.
  - `UC1 Register Issue`
  - `UC5 Assign / Update Issue Assignment`
  - `UC6 Change Issue State - Mark Fixed`
- 전체 PUML source는 `docs/artifact/ssd/`에 둔다. PNG/SVG는 필요할 때 생성하는 산출물이다.
- SSD는 시스템을 black-box로 보고 actor와 `:Issue Tracking System` 사이의 system event만 표현한다.
- 내부 controller, service, repository, entity, database lifeline은 SSD에 넣지 않는다.
- UC14 권한 검사는 protected operation의 공통 조건이다. 독립 SSD는 참고용으로만 두고, 실제 base SSD에서는 precondition/note로 표현한다.
- UC6 Change Issue State는 하나의 UC지만 상태 전이 시나리오가 여러 개이므로 개발 기준에서는 여러 SSD 후보로 나눈다.
- UC9 Delete/Manage Deleted Issue는 삭제, 보관 정책, 복구를 분리해 개발 기준을 명확히 한다.

## 제출용 SSD

| File | UC | Actor | System Operation | 제출 선택 이유 |
| --- | --- | --- | --- | --- |
| `docs/artifact/ssd/ssd-01-register-issue.puml` | UC1 이슈 등록 | Auth User | `registerIssue(projectId, title, description, priority)`, `addComment(issueId, content)` | 이슈 생성, reporter/reportedDate/status `NEW`, history 생성 요구와 등록 직후 optional comment 흐름을 함께 보여준다. |
| `docs/artifact/ssd/ssd-05-assign-issue.puml` | UC5 이슈 배정/배정 변경 | PL | `startAssignment(issueId)`, `assignIssue(...)`, `reassignIssue(...)`, `changeVerifier(...)` | UC8 후보 추천 include, NEW/REOPENED 배정, ASSIGNED assignee 재배정, FIXED verifier 교체를 한 다이어그램에서 보여준다. |
| `docs/artifact/ssd/ssd-06-mark-fixed.puml` | UC6 상태 변경: Fixed 처리 | DEV | `addStatusChangeComment(issueId, comment)`, `changeStatus(issueId, targetStatus=FIXED)` | UC6 중 Dev가 ASSIGNED 이슈를 FIXED로 바꾸는 핵심 상태 전이와 필수 사유 comment 흐름을 보여준다. |

제출용 이미지는 `ssd-01-register-issue.puml`, `ssd-05-assign-issue.puml`, `ssd-06-mark-fixed.puml`에서 생성한 렌더 산출물을 사용한다.

## 전체 SSD 후보 목록

| 우선순위 | UC | SSD 후보 이름 | Actor | Main System Operation | 주요 결과/검증 포인트 | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| 제출 | UC1 이슈 등록 | Register Issue | Auth User | `registerIssue(projectId, title, description, priority)` | `Issue` 생성, reporter 자동 저장, reportedDate 자동 저장, status `NEW`, history 생성, 등록 직후 optional comment 가능 | 제출용으로 확정 |
| 개발 핵심 | UC2 코멘트 추가 | Add Comment | Auth User | `addComment(issueId, content)` | `Comment` 생성, writer/createdDate 저장, `IssueHistory(COMMENTED)` 기록 | UC1/UC4/UC5에서는 optional extension, UC6에서는 mandatory include |
| 개발 핵심 | UC3 이슈 검색/브라우즈 | Search Issues | Auth User | `searchIssues(criteria)` | assignee/status/reporter/project 조건 검색, 목록 반환 | 조회 UC라 Operation Contract 우선순위는 낮음 |
| 개발 핵심 | UC4 이슈 상세 조회 | View Issue Detail | Auth User | `viewIssueDetail(issueId)` | 이슈 필드, comments, history, blocked-by/blocking dependencies, availableActions 반환 | UC5/UC6/UC15는 상세 화면에서 시작될 수 있지만 별도 actor-goal UC |
| 제출 | UC5 이슈 배정/배정 변경 | Assign / Update Issue Assignment | PL | `startAssignment(issueId)`, `assignIssue(...)`, `reassignIssue(...)`, `changeVerifier(...)` | NEW/REOPENED는 assignee+verifier 후보 추천 후 ASSIGNED로 배정, ASSIGNED는 assignee 변경, FIXED는 verifier 변경 | UC8 상태별 후보 추천은 include, top 3 후보와 프로젝트 소속 active 후보 전체를 함께 반환, UC2 배정 코멘트는 optional extend |
| 제출 | UC6 상태 변경 | Mark Fixed | DEV | `addStatusChangeComment(issueId, comment)`, `changeStatus(issueId, targetStatus=FIXED)` | status `ASSIGNED -> FIXED`, fixer 자동 기록, 필수 comment와 `IssueHistory(STATUS_CHANGED)` 생성 | assignee 본인만 가능, 제출용으로 확정 |
| 개발 핵심 | UC6 상태 변경 | Resolve Issue | Tester | `changeStatus(issueId, targetStatus=RESOLVED, comment)` | status `FIXED -> RESOLVED`, resolver 자동 기록, 필수 comment/history 생성 | verifier 본인만 가능, blocking Issue가 모두 `RESOLVED` 또는 `CLOSED`여야 함 |
| 개발 핵심 | UC6 상태 변경 | Reject Fixed Issue | Tester | `changeStatus(issueId, targetStatus=ASSIGNED, comment)` | status `FIXED -> ASSIGNED`, 기존 assignee/verifier/fixer retained, 실패 사유 comment/history 저장 | tester rollback, 새 Dev는 UC5-RA에서 변경 |
| 개발 핵심 | UC6 상태 변경 | Close Issue | PL | `changeStatus(issueId, targetStatus=CLOSED, comment)` | status `RESOLVED -> CLOSED`, assignee/verifier null, fixer/resolver 보존, 필수 comment/history 생성 | PL 권한 |
| 개발 핵심 | UC6 상태 변경 | Reopen Issue | PL | `changeStatus(issueId, targetStatus=REOPENED, comment)` | status `RESOLVED/CLOSED -> REOPENED`, fixer/resolver 보존, assignee/verifier 자동복원 없음 | UC5로 배정 가능 |
| 개발 보조 | UC8 Assignment Candidate 추천 | Recommend Assignment Candidates | PL | `startAssignment(issueId)` | Issue status에 따라 assignee DEV 후보 또는 verifier Tester 후보를 반환; 과거 완료 이력과 이슈 내용 유사도 참고 | UC5의 include, reference SSD, 도메인 변경 없음 |
| 개발 핵심 | UC9 삭제 이슈 관리 | Delete Closed/New Issue | PL | `deleteIssue(issueId, comment)` | status `NEW/CLOSED -> DELETED`, 삭제 사유 comment 기록, 관련 dependency 제거, status change history 생성, 30개 초과 FIFO 보관 정책 적용 | soft-delete |
| 개발 보조 | UC9 삭제 이슈 관리 | Deleted Issue Retention Policy | Internal Policy | 외부 system operation 없음 | DELETED 이슈 30개 초과 시 deleted transition time 기준 FIFO 물리 삭제 | actor-goal SSD가 아닌 정책 reference |
| 개발 핵심 | UC9 삭제 이슈 관리 | Restore Closed/New Issue | PL | `viewDeletedIssues(projectId)`, `restoreIssue(issueId, comment)` | status `DELETED -> previousStatus(NEW 또는 CLOSED)`, 복구 사유 comment 기록, assignee/verifier null 유지, 보관 정책 대상에서 제외 | previousStatus는 삭제 시 기록된 `IssueHistory(STATUS_CHANGED).previousValue`에서 결정, dependency 자동 복구 없음 |
| 개발 보조 | UC10 이슈 통계 | View Issue Statistics | Auth User | `viewStatistics(projectId, dailyPeriod, monthlyPeriod)` | issueCounts, trendData, statusBreakdown, priorityBreakdown 반환 | 선택한 Project 범위에서 조회하며 DELETED Issue는 통계에서 제외 |
| 개발 보조 | UC11 로그인 | Log In | Admin/Auth User | `login(loginId, password)` | 인증 성공, 현재 사용자 식별 | 인증 식별자는 UC12의 `loginId`와 통일 |
| 개발 핵심 | UC12 계정 관리 | Create Account | Admin | `createAccount(loginId, name, password, role)` | `User` 생성, loginId/name/role 저장, password는 passwordHash로 저장, isActive=true | Admin이 다른 actor 계정을 준비 |
| 개발 보조 | UC12 계정 관리 | Update Account | Admin | `renameAccount(loginId, name)`, `changeAccountRole(loginId, role)` | `User.name`, `User.role` 변경 | `isActive`는 Deactivate Account에서만 처리 |
| 개발 보조 | UC12 계정 관리 | Deactivate Account | Admin | `deactivateAccount(userId)` | `User.isActive=false` | Admin-only |
| 개발 핵심 | UC13 프로젝트 관리 | Create Project | Admin | `createProject(name, description)` | `Project` 생성, createdDate 저장, Admin manages association 설정 | 과제 데모 project 준비 |
| 개발 보조 | UC13 프로젝트 관리 | Add Project Participant | Admin | `addProjectParticipant(projectId, userId)` | `ProjectMember` association 생성 | 프로젝트 참여자는 membership으로 관리 |
| 개발 보조 | UC13 프로젝트 관리 | Remove Project Participant | Admin | `removeProjectParticipant(projectId, userId)` | `ProjectMember` association 제거 | 프로젝트 참여자는 membership으로 관리 |
| 내부 공통 | UC14 권한 검사 | Verify Permission | Actor/Internal | `assertCanProtectedOperation(actor, resource)` | 권한 있음/없음 alt로 base operation 진행 또는 거부 | 직접 사용자 목표 SSD가 아니라 include되는 공통 권한 검사 reference |
| 개발 핵심 | UC15 이슈 수정 | Edit Issue | Reporter | `updateIssue(issueId, newTitle, newDescription)` | status `NEW/REOPENED`인 본인 이슈 title/description 수정, history 저장 | priority는 UC16, status는 UC6 |
| 개발 핵심 | UC7 의존성 관리 | Add Dependency | PL | `addDependency(blockingIssueId, blockedIssueId)` | `IssueDependency` 생성, 순환/자기참조 방지, history 저장 | 별도 dependencyName/type 파라미터 없음, resolve 가능 여부는 UC6 `FIXED -> RESOLVED` guard에서 판단 |
| 개발 보조 | UC7 의존성 관리 | Remove Dependency | PL | `removeDependency(blockingIssueId, blockedIssueId)` | dependency 제거, history 저장, `dependencyRemoved()` 확인 응답 | 응답에 반환 payload 없음 |
| 개발 핵심 | UC16 우선순위 변경 | Change Priority | PL | `changePriority(issueId, newPriority)` | `Issue.priority` 변경, `IssueHistory(PRIORITY_CHANGED)` 기록 | PL-only |

## Operation Contract 선정 기준

Operation Contract는 SSD의 모든 system operation을 1:1로 옮기기보다, operation 수행 후 도메인 객체의 생성, 상태 변화, 관계 변화가 분명한 경우를 중심으로 작성한다. SSD는 actor와 system 사이의 메시지 흐름을 보여주지만, 그 결과로 어떤 객체가 생기고 어떤 association이나 속성값이 바뀌었는지는 충분히 드러나지 않기 때문이다.

보고서 제출용 OC는 대표 SSD와의 연결성, 상태 전이 정책의 설명 필요성, dependency 관계의 모호성을 함께 고려하여 아래 3개를 사용한다.

| OC | System Operation | 선정 이유 |
| --- | --- | --- |
| OC-01 | `registerIssue(projectId, title, description, priority)` | SSD 01의 기본 흐름과 직접 연결된다. 새 `Issue` 생성, reporter 설정, status `NEW`, 기본 priority, 생성 history 기록을 가장 표준적인 OC 예시로 보여준다. |
| OC-07 | `changeStatus(issueId, targetStatus=RESOLVED, comment)` | `FIXED -> RESOLVED` 전이는 resolver 기록, assignee/verifier 제거, 필수 comment/history 기록, blocking issue guard가 함께 나타난다. SSD만으로는 사후조건을 충분히 설명하기 어렵다. |
| OC-14 | `addDependency(blockingIssueId, blockedIssueId)` | blocking issue와 blocked issue의 방향이 중요하고, dependency 생성 후 blocked issue history에 기록되는 정책을 명확히 설명할 필요가 있다. |

`assignIssue`, `reassignIssue`, `changeVerifier`, `deleteIssue`, `restoreIssue`, `changePriority` 등도 도메인 변경이 있는 operation이므로 reference OC로 유지한다. 다만 보고서 본문에서는 위 3개를 중심으로 설명하고, 나머지는 구현 정책과 시퀀스 다이어그램 검토용으로 둔다.

조회성 operation인 `searchIssues`, `viewIssueDetail`, `viewStatistics`, `recommendAssignmentCandidates`, `viewDeletedIssues`, `startAssignment`는 SSD 작성은 가능하지만 domain object 변경이 없거나 약하므로 Operation Contract 우선순위는 낮다.

## 개발 기준 권장 세트

제출용 SSD 1, 5, 6 이후 개발 정확도를 위해 추가로 그린다면 아래 순서가 적절하다.

1. `Resolve Issue`
2. `Reject Fixed Issue`
3. `Close Issue`
4. `Reopen Issue`
5. `Edit Issue`
6. `Delete Closed/New Issue`
7. `Restore Closed/New Issue`
8. `Change Priority`
9. `Add Dependency`
10. `Create Account`
11. `Create Project`

이 세트는 과제 데모 시나리오와 팀 확정 정책인 fixed, resolved, tester rollback, reporter edit 제한, soft-delete/restore, priority, dependency, 계정/프로젝트 준비를 대부분 덮는다.
