# Domain Rule Ownership Audit

작성 기준: `origin/dev` 42db174, 2026-06-02.

## Purpose

DDD Lite, GRASP, SOLID, Tell-Don't-Ask, Law of Demeter 관점에서 핵심 규칙의 소유 위치를 점검한다. 목표는 패턴을 새로 늘리는 것이 아니라, 현재 코드에서 규칙이 어느 계층에 있어야 안정적인지 확인하고 실제로 분리할 가치가 있는 항목만 후속 작업으로 남기는 것이다.

## Scope

- `Issue` 상태 전이, 배정, 우선순위, 댓글, dependency 변경 규칙
- `PermissionPolicy` 권한 판단
- `IssueService`, `IssueStateService`, `AssignmentService`, `DeletedIssueService`, `IssueWorkflowService`
- `AccountService`의 계정 역할 변경과 비활성화 책임 검사
- JDBC repository에 남아 있는 업무 규칙성 조건

## Non-goals

- 단순 CRUD 흐름에 DDD 패턴을 추가하지 않는다.
- 상태별 클래스를 만들거나 Strategy/Specification을 선제 도입하지 않는다.
- UI 표시 편의를 위한 `IssueWorkflowActions`를 실행 권한의 최종 근거로 보지 않는다.

## Ownership Table

| Rule area | Current owner | 판단 | 근거 |
|---|---|---|---|
| 이슈 상태 전이의 핵심 불변식 | `domain.Issue` | 적절 | `markFixed`, `resolve`, `close`, `reopen`, `rejectFix`가 현재 상태, 담당자 역할, active assignment 정리, history 생성을 함께 처리한다. |
| 상태 변경 실행 흐름 | `IssueStateService` + `PermissionPolicy` + `Issue` | 적절 | service가 issue/user/dependency 조회와 프로젝트 멤버십 검사를 조율하고, policy가 actor 권한을 검사한 뒤, domain이 실제 상태를 바꾼다. |
| 이슈 등록/수정/우선순위 변경 | `IssueService` + `PermissionPolicy` + `Issue` | 적절 | 프로젝트 존재, 중복 제목, reporter/PL 권한, 멤버십은 service/policy가 보고, title/description/priority 변경과 history는 `Issue`가 가진다. |
| 배정/재배정/verifier 변경 | `AssignmentService` + `Issue` | 대체로 적절 | service는 PL 여부, 후보 사용자 조회, 프로젝트 active member 여부를 확인하고, `Issue`는 상태 전이와 assignment history를 기록한다. |
| dependency 추가/삭제 | `IssueService` + `Issue` + `IssueDependencyRepository` | 부분 개선 후보 | 같은 프로젝트, cycle, persisted duplicate은 service/repository 조합이 필요하다. 다만 self/loaded duplicate 검사가 `Issue`에도 있어 split 자체는 허용하되 drift 위험은 남는다. |
| resolve 전 blocking issue 검사 | `IssueStateService`, `IssueWorkflowService` | 개선 후보 | 실행 경로와 버튼 표시 경로가 같은 규칙을 각각 검사한다. 실제 실행은 안전하지만 UI action 표시와 실행 가능 조건이 어긋날 수 있다. |
| 권한 판단 | `PermissionPolicy` | 적절 | role, active user, assigned actor, admin/PL 권한을 한 곳에서 검사한다. project membership은 repository 조회가 필요해 service 쪽에 남아 있다. |
| active project member / active PL 검사 | 각 service + `UserRepository` | 개선 후보 | `actor.getRole() == Role.PL && existsActiveProjectMember(...)` 형태가 여러 service에 반복된다. 규칙 변경 시 중복 수정 위험이 있다. |
| deleted issue workflow | `DeletedIssueService` + `DeletedIssueRepository` | 부분 개선 후보 | service가 PL 권한과 delete/restore 흐름을 맡고 repository가 transaction, dependency removal, FIFO purge를 맡는다. 다만 restore status 검증 일부가 JDBC에 남아 있어 boundary가 흐려질 수 있다. |
| deleted issue 일반 경로 차단 | `IssueService`, `IssueStateService`, `IssueWorkflowService`, query default | 적절 | 일반 조회/검색/상태 변경은 `DELETED`를 차단하고, deleted issue 관리는 전용 controller/service/repository로 분리되어 있다. |
| 계정 역할 변경/비활성화 책임 검사 | `AccountService` + repository predicate | 적절 | project membership과 current issue responsibility는 여러 aggregate를 봐야 하므로 service orchestration에 두는 것이 맞다. |
| dashboard/statistics 조회 모델 | 전용 repository/read model | 적절 | 집계 결과는 domain entity가 아니라 read model이다. domain 패키지에 넣지 않는 현재 방향이 맞다. |

## Severity

### High

현재 기준으로 즉시 merge를 막을 정도의 rule ownership 결함은 확인되지 않았다. 실행 경로의 최종 권한 검사는 service/policy/domain에서 다시 수행되므로 UI action 표시가 최종 보안 경계가 되지는 않는다.

### Medium

#### Dependency action availability mismatch

`IssueService.addDependency`는 같은 프로젝트, 권한, deleted 여부, cycle, duplicate을 기준으로 판단한다. 반면 `IssueWorkflowService.canAddDependency`는 `RESOLVED`, `CLOSED` 상태에서 add dependency 버튼을 숨긴다. 실행 정책과 표시 정책의 목적이 다르면 괜찮지만, 현재 문서상 같은 workflow 안내로 쓰이기 때문에 drift 위험이 있다.

권장 후속 작업:

- 실행 정책을 기준으로 `IssueWorkflowService`를 맞춘다.
- 반대로 완료 이슈에 dependency 추가를 금지할 정책이라면 `IssueService.addDependency`와 테스트를 함께 바꾼다.
- 두 경로가 공유할 수 있는 작은 policy method나 specification은 이 불일치를 정리한 뒤에만 도입한다.

#### Active project member / active PL duplication

active project member와 active PL 판단이 `IssueService`, `IssueStateService`, `AssignmentService`, `DeletedIssueService`, `IssueWorkflowService`에 반복된다. 현재는 단순 조건이라 동작 위험은 낮지만, role-aware membership 기준이 바뀌면 여러 service를 동시에 고쳐야 한다.

권장 후속 작업:

- `ProjectMembershipPolicy`를 service layer에 두거나 repository predicate를 `existsActiveProjectMemberWithRole(projectId, loginId, role)`처럼 명명한다.
- 권한의 actor role 판단은 `PermissionPolicy`에 남기고, membership 조회가 필요한 조건만 별도 policy로 묶는다.

#### Resolve blocking issue rule duplication

`IssueStateService`는 unresolved blocking issue가 있으면 `RESOLVED` 전이를 막고, `IssueWorkflowService`는 같은 이유로 resolve action을 false로 만든다. 실행 경로는 안전하지만 같은 graph traversal 규칙이 둘로 갈라져 있다.

권장 후속 작업:

- `DependencyResolutionPolicy` 또는 service-private helper를 추출해 두 service가 같은 판단을 사용하게 한다.
- 추출 전에는 테스트에서 command path와 workflow path를 함께 검증한다.

#### Deleted issue restore rule leakage

`DeletedIssueService`는 `DELETED` 여부와 PL 권한을 검사하지만, `JdbcDeletedIssueRepository.restore`는 pre-delete status가 `NEW` 또는 `CLOSED`인지 다시 검사한다. transaction 처리와 pre-delete history 조회는 repository 책임이 맞지만, `NEW/CLOSED만 restore 대상` 같은 lifecycle 규칙은 service/domain 쪽 이름으로 드러나는 편이 낫다.

권장 후속 작업:

- repository는 pre-delete status 조회와 update transaction을 유지한다.
- service가 restore 가능한 pre-delete status 정책을 명명하거나, repository가 반환한 pre-delete status를 service에서 검증하는 구조를 검토한다.
- FIFO purge 한도는 `DeletedIssueService`의 application policy로 유지한다.

### Low

#### Account current responsibility naming

`AccountService`는 role 변경과 비활성화 전에 project membership과 current issue responsibility를 검사한다. 현재 repository predicate가 의미를 감추고 있으므로, 향후 프로젝트 참여자 제거 정책과 함께 쓰일 경우 용어를 맞추는 정도가 좋다.

권장 후속 작업:

- `hasCurrentIssueResponsibility`의 의미를 `ASSIGNED/FIXED`의 assignee/verifier 책임으로 문서화한다.
- 같은 개념이 project membership 제거나 user deactivation에서 반복되면 공통 policy로 묶는다.

#### Domain role checks and policy role checks

`Issue`도 role sanity를 확인하고 `PermissionPolicy`도 actor 권한을 확인한다. 중복처럼 보이지만 성격이 다르다. `PermissionPolicy`는 "요청자가 할 수 있는가"를 보고, `Issue`는 "이 mutation에 들어온 participant가 도메인 역할에 맞는가"를 본다. 현재는 유지한다.

## Pattern Decisions

| Pattern | 판단 | 이유 |
|---|---|---|
| DDD Lite | 유지 | `Issue`, `User`, `Project`, `Comment`, `IssueDependency`, `IssueHistory`가 핵심 상태와 mutation을 가진다. |
| Transaction Script | 유지 | account/project 같은 단순 CRUD와 cross-aggregate 조회 흐름은 service transaction script가 더 단순하다. |
| Policy | 선별 적용 | `PermissionPolicy`는 적절하다. membership, dependency resolution처럼 중복되는 조건은 실제 drift가 확인된 지점부터 추가 policy로 뺀다. |
| Specification | 보류 | status/priority branch가 아직 작고, Specification을 넣으면 호출 구조가 더 무거워진다. dependency resolution처럼 실행/표시 경로가 갈라진 규칙에만 후보가 있다. |
| Strategy | 보류 | 상태별 알고리즘이 분리될 정도로 복잡하지 않다. 현재 switch와 domain method가 더 읽기 쉽다. |
| Factory/Builder | 현 구조 유지 | `Issue.PersistedState`는 persistence rehydration과 creation input을 분리하는 데 충분하다. 별도 factory는 현재 이득이 작다. |
| Facade | 보류 | controller가 service를 직접 호출해도 흐름이 과도하게 복잡하지 않다. |
| Result/Either | 보류 | 현재 서비스 예외 흐름과 controller 호출 방식에 맞춰져 있다. 전체 error-handling 정책 없이 부분 도입하지 않는다. |

## Follow-up Split

실제 refactor가 필요한 항목은 아래 단위로 나눈다. 제출 직전 범위에서는 새 abstraction을 한 번에 넣기보다, 테스트로 불일치를 고정한 뒤 작은 PR로 처리한다.

| Priority | PR-sized item | Scope |
|---|---|---|
| 1 | dependency action 표시 정책 정렬 | `IssueWorkflowService.canAddDependency`, 관련 workflow/action 테스트, `IssueService.addDependency` 정책 중 하나를 기준으로 맞춘다. |
| 2 | active project membership/PL predicate 명명 | 중복 private helper를 service layer policy 또는 role-aware repository predicate로 정리한다. |
| 3 | resolve blocking issue policy 공유 | `IssueStateService`와 `IssueWorkflowService`가 같은 unresolved blocking issue 판단을 쓰게 한다. |
| 4 | deleted issue restore lifecycle boundary 정리 | restore 가능한 pre-delete status 정책을 service/domain 이름으로 드러내고 JDBC는 transaction과 SQL에 집중하게 한다. |
| 5 | current issue responsibility 용어 정리 | account role/deactivation guard와 project membership 관련 guard에서 같은 개념을 같은 이름으로 사용한다. |

## Current Conclusion

현재 구현은 큰 방향에서 `Controller -> Service/Policy -> Domain/Repository Port -> JDBC Adapter` 경계를 지키고 있다. 즉시 필요한 조치는 대규모 패턴 도입이 아니라, 실행 정책과 표시 정책이 갈라질 수 있는 조건을 작은 후속 PR로 정렬하는 것이다. #171은 boundary/testability guard 확장 범위로 유지하고, 이 문서는 #168의 rule ownership 판단 기준으로 둔다.

## Verification Plan

```bash
git diff --check
./gradlew test --tests '*Issue*Test' --tests '*PermissionPolicyTest' --tests '*AccountServiceTest' --console=plain
./gradlew check --console=plain
```
