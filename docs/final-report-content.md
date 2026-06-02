# 최종 보고서 본문 재료

이 문서는 최종 PDF 편집본의 서식이 아니라, 팀원이 보고서에 옮겨 쓸 내용 중심 초안이다. 실제 PDF에서는 표지, 목차, 그림 크기, 여백, 캡션, 페이지 번호를 별도로 정리한다.

## A. 프로젝트 개요

### 프로젝트 목적

본 프로젝트는 다수의 사용자가 참여하는 소프트웨어 개발 과정에서 발생하는 이슈를 체계적으로 관리하기 위한 이슈 관리 시스템을 Java로 구현하는 것을 목표로 한다. 시스템은 이슈를 단순 목록으로 저장하는 데서 그치지 않고, 이슈가 등록된 이후 배정, 수정, 검증, 종료 또는 재오픈되는 전체 흐름을 추적한다.

이슈 관리 시스템의 사용자는 프로젝트별로 이슈를 등록하고, 담당자를 배정하며, 상태 변경과 처리 이력을 확인할 수 있다. 이를 위해 이슈 상태, 담당자, 댓글, 변경 이력, 의존성, 우선순위, 통계, 삭제 이슈 관리 정보를 함께 저장하고 조회할 수 있도록 구성하였다.

본 프로젝트에서 특히 중요하게 본 부분은 UI와 업무 로직의 분리이다. 과제 요구사항상 두 개 이상의 UI toolkit을 사용해야 했으므로 JavaFX와 Swing을 각각 하나의 완성된 UI로 구현하고, 두 UI가 같은 controller, service, domain, repository, JDBC 계층을 재사용하도록 설계하였다. 이를 통해 UI를 제외한 핵심 로직과 영속 저장소 코드가 거의 수정 없이 재사용될 수 있음을 보여 주고자 했다.

### 주요 기능 요약

| 기능 | 내용 | 구현 근거 |
|---|---|---|
| 로그인과 세션 | active 계정만 로그인 가능하며 역할별 화면으로 진입한다. | `AuthenticationController`, `AuthenticationService` |
| 계정 관리 | Admin이 계정을 생성하고 이름, 역할, active 상태를 관리한다. | `AccountController`, `AccountService` |
| 프로젝트 관리 | Admin이 프로젝트를 생성, 수정, 삭제하고 참여자를 관리한다. | `ProjectController`, `ProjectService` |
| 이슈 등록과 검색 | 프로젝트 active member가 이슈를 등록하고, keyword, 상태, 우선순위, reporter, assignee, verifier, 기간 조건으로 검색한다. | `IssueController`, `IssueService`, `IssueSearchCriteria` |
| 이슈 상세 조회 | 이슈 기본 정보, 댓글, 이력, 의존성, 가능한 액션을 함께 조회한다. | `IssueController.viewIssueDetail`, `IssueWorkflowService` |
| 배정과 추천 | PL이 상태에 따라 Dev assignee와 Tester verifier를 배정하고, 완료 이력 기반 추천 후보를 확인한다. | `AssignmentController`, `AssignmentService`, `AssignmentRecommendationService` |
| 상태 전이 | Dev, Tester, PL이 역할과 상태 조건에 맞게 이슈 상태를 변경하고 사유 댓글과 이력을 남긴다. | `IssueStateController`, `IssueStateService`, `Issue` |
| 댓글 | 일반 댓글은 프로젝트 접근 권한이 있는 사용자가 작성하고, 수정/삭제는 작성자 본인만 가능하다. | `IssueController`, `Comment`, `CommentPurpose` |
| 의존성 관리 | PL이 blocking issue와 blocked issue 관계를 추가/제거하고, unresolved blocking issue가 남은 경우 RESOLVED 전이를 막는다. | `IssueController`, `IssueDependencyRepository`, `IssueStateService` |
| 삭제 이슈 관리 | PL이 NEW/CLOSED 이슈를 soft delete하고, deleted issue를 restore 또는 purge한다. | `DeletedIssueController`, `DeletedIssueService`, `DeletedIssueRepository` |
| 통계 | 프로젝트 기준 일/월별 이슈 수, 상태 변경 수, 댓글 수, 상태/우선순위 분포를 조회한다. | `StatisticsController`, `StatisticsService`, `JdbcStatisticsRepository` |
| 다중 UI | JavaFX와 Swing이 같은 backend application 계층을 호출한다. | `Main`, `ApplicationBootstrap`, `ApplicationContext`, `ui.javafx`, `ui.swing` |

### 구현 범위와 제한사항

구현 범위는 과제에서 요구한 이슈 등록, 검색, 상세 조회, 댓글, 배정, 상태 변경, 우선순위 변경, 의존성 관리, 삭제 이슈 관리, 통계, 추천, 계정/프로젝트 관리, Oracle 기반 persistent storage, JUnit 테스트, JavaFX와 Swing UI를 포함한다.

Assignment 추천 기능은 프로젝트 내 active Dev/Tester 후보와 과거 RESOLVED/CLOSED 이슈의 fixer/resolver 이력을 기반으로 동작한다. 현재 구현은 이슈 title과 description의 유사도를 계산해 후보를 정렬하는 방식이며, 외부 학습 모델과의 연동은 구현 범위 밖의 향후 확장 지점으로 남겨 두었다.

통계는 프로젝트 단위 조회를 기준으로 한다. 삭제 이슈 관리 흐름과 일반 이슈 관리 흐름을 분리하기 위해 통계 쿼리에서는 `DELETED` 상태 이슈를 제외한다.

JavaFX와 Swing UI는 각각 별도 애플리케이션으로 실행된다. 두 UI는 같은 backend 계층을 재사용하지만, 세션 객체는 각 실행 흐름 안에서 관리한다고 가정하였다.

## B. 프로젝트 가정사항과 정책

### 사용자 역할 정책

시스템 사용자는 Admin, PL, Dev, Tester로 구분한다. 한 사용자는 하나의 역할만 가진다고 가정하였다. 역할을 단순하게 유지한 이유는 권한 판단을 명확히 하고, 프로젝트와 이슈 처리 흐름을 지나치게 복잡하게 만들지 않기 위해서이다.

Admin은 계정과 프로젝트를 관리하는 운영자이다. 계정 생성, 역할 변경, 계정 활성화/비활성화, 프로젝트 생성/수정/삭제, 프로젝트 참여자 관리를 수행하지만 일반 이슈 처리 흐름에는 직접 참여하지 않는다. 따라서 Admin 전용 프로젝트 화면에는 일반 이슈 목록을 표시하지 않는다.

PL은 프로젝트의 이슈 흐름을 관리한다. 자신이 active member로 참여한 프로젝트에서 이슈 배정, 재배정, 우선순위 변경, 의존성 관리, 삭제 이슈 관리, 종료, 재오픈을 수행한다.

Dev는 자신이 assignee로 배정된 이슈를 수정한다. 수정이 완료되면 이슈를 FIXED 상태로 변경하여 Tester가 검증할 수 있게 한다. Dev도 자신이 참여한 프로젝트 안에서는 이슈 등록, 조회, 댓글 작성이 가능하다.

Tester는 fixed 이슈를 검증한다. verifier로 배정된 이슈가 FIXED 상태가 되면 검증을 수행하고, 결과에 따라 RESOLVED로 전이하거나 다시 ASSIGNED로 되돌린다. 검증 결과와 사유는 댓글과 이력으로 남긴다.

assignee/verifier와 fixer/resolver는 구분한다. assignee와 verifier는 현재 작업 담당자이고, fixer와 resolver는 과거에 실제 수정 완료와 검증 완료를 수행한 사용자이다. 이슈가 RESOLVED 상태가 되면 현재 작업 배정은 끝난 것으로 보아 assignee와 verifier는 해제하고 fixer와 resolver는 이력성 정보로 보존한다.

### 프로젝트 접근 정책

프로젝트 접근은 사용자 역할과 프로젝트 참여 여부를 함께 고려한다. Admin은 전체 프로젝트 요약과 전체 사용자 목록을 볼 수 있고, 특정 프로젝트에 들어가면 프로젝트 기본 정보와 참여자를 확인할 수 있다. 다만 Admin은 일반 이슈 처리 actor가 아니므로 Admin 프로젝트 상세 화면에는 이슈 목록을 표시하지 않는다.

PL, Dev, Tester는 자신이 active member로 참여한 프로젝트만 볼 수 있다. 프로젝트에 들어가면 `DELETED` 상태가 아닌 일반 이슈 전체 목록을 확인할 수 있고, 검색 조건을 통해 필요한 이슈를 찾을 수 있다. `DELETED` 이슈는 일반 프로젝트 화면과 분리하고, PL 전용 deleted issue 관리 화면에서만 조회한다.

### 상태 전이 정책

기본 상태 흐름은 다음과 같다.

```text
NEW -> ASSIGNED -> FIXED -> RESOLVED -> CLOSED
```

NEW는 이슈가 새로 등록된 상태이고, ASSIGNED는 Dev와 Tester가 배정된 상태이다. FIXED는 Dev가 수정 완료를 표시한 상태이고, RESOLVED는 Tester가 검증을 완료한 상태이다. CLOSED는 PL이 최종 종료한 상태이다.

보조 흐름으로는 검증 실패와 재작업이 있다. Tester가 FIXED 이슈를 검증했지만 충분하지 않다고 판단하면 FIXED에서 ASSIGNED로 되돌릴 수 있다. PL이 RESOLVED 또는 CLOSED 이슈에 대해 재작업이 필요하다고 판단하면 REOPENED로 전이한다. REOPENED 이슈는 assignee와 verifier를 자동 복원하지 않는다. PL이 다시 assignee와 verifier를 배정하면 ASSIGNED 상태부터 재작업이 시작된다.

soft delete 흐름을 위해 DELETED 상태를 둔다. NEW 또는 CLOSED 상태의 이슈만 DELETED로 전이할 수 있으며, 이 전이는 PL만 수행한다. DELETED 이슈를 restore하면 soft delete 직전 상태인 NEW 또는 CLOSED로 되돌린다.

### 배정 정책

이슈 배정은 프로젝트의 PL만 수행할 수 있다. NEW, REOPENED 상태에서는 assignee Dev와 verifier Tester를 함께 지정하고, 배정이 완료되면 이슈는 ASSIGNED 상태가 된다. ASSIGNED 상태에서는 assignee만 변경할 수 있고, FIXED 상태에서는 verifier만 변경할 수 있다.

배정 또는 배정 변경이 발생하면 `ASSIGNMENT_CHANGED` 이력을 남긴다. NEW에서 ASSIGNED, REOPENED에서 ASSIGNED로 상태가 함께 바뀌는 경우에는 `STATUS_CHANGED` 이력도 함께 기록한다.

배정 화면에서는 추천 후보 Top 3와 전체 active Dev/Tester 후보 목록을 함께 제공한다. 추천 후보가 충분하지 않은 경우에도 전체 active 후보를 제공하여 PL이 직접 선택할 수 있게 한다.

### 댓글과 이력 정책

댓글은 일반 댓글과 상태 변경 사유 댓글로 구분한다. 일반 댓글은 의견이나 추가 설명을 남기기 위한 것이고, 상태 변경 사유 댓글은 상태 전이의 근거로 남기는 것이다.

일반 댓글은 특정 프로젝트에 접근 권한이 있는 사용자가 작성할 수 있으며, 수정과 삭제는 작성자 본인만 할 수 있다. 상태 변경 사유 댓글은 상태 전이 근거로 사용되므로 수정과 삭제를 허용하지 않는다.

일반 댓글의 추가, 수정, 삭제는 이슈 이력에 기록된다. 댓글 작성/수정 시간은 댓글 객체가 관리하고, 이슈의 updated time은 이슈 자체의 제목, 설명, 상태, 배정, 우선순위처럼 이슈 자체가 변경되는 경우를 기준으로 한다.

### 의존성 정책

이슈 간에는 blocking issue와 blocked issue 관계가 존재할 수 있다. blocked issue를 RESOLVED로 변경하려면 그 이슈를 막고 있는 blocking issue가 모두 RESOLVED 또는 CLOSED 상태여야 한다.

의존성은 별도의 BLOCKED 상태가 아니라 FIXED에서 RESOLVED로 전이할 때 확인하는 guard로 설계하였다. 이를 통해 이슈 상태 모델은 단순하게 유지하면서도 이슈 간 선후 관계를 구조화된 데이터로 관리할 수 있다.

이슈가 DELETED 상태가 되면 그 이슈가 포함된 의존성 관계는 함께 제거된다. 삭제된 이슈가 다른 이슈의 상태 전이에 계속 영향을 주지 않도록 하기 위한 정책이다.

### 삭제 이슈 관리 정책

이슈 삭제는 soft delete 방식으로 처리한다. PL이 이슈 삭제를 수행해도 DB에서 바로 물리 삭제하지 않고 먼저 DELETED 상태로 전이한다. soft delete는 NEW와 CLOSED 상태에만 허용한다. ASSIGNED, FIXED, RESOLVED, REOPENED 상태는 작업이나 검증 흐름이 남아 있을 수 있으므로 삭제 상태로 바로 옮기지 않는다.

soft delete와 restore에는 사유 댓글이 필요하다. PL은 삭제 직전 상태인 NEW 또는 CLOSED로 복구할 수 있지만, 삭제 당시 제거된 의존성 관계는 자동 복원하지 않는다.

프로젝트별 deleted issue 보관 한도는 30개이다. 한도를 초과하면 오래된 deleted issue부터 FIFO 방식으로 물리 삭제한다. PL은 특정 deleted issue를 단건으로 물리 삭제할 수도 있다. 단건 purge와 FIFO 정리는 별도 사유 댓글을 요구하지 않는다.

### 통계 정책

통계는 프로젝트 단위로 조회한다. 프로젝트 접근 권한을 가진 active member는 해당 프로젝트의 통계를 확인할 수 있다. 통계 항목은 일/월별 이슈 발생 수, 일/월별 상태 변경 수, 일/월별 댓글 수, 상태 분포, 우선순위 분포이다.

삭제 이슈 관리 흐름과 일반 이슈 흐름을 분리하기 위해 통계 범위에서는 DELETED 상태 이슈를 제외한다. JDBC 통계 쿼리도 `status <> 'DELETED'` 조건을 사용한다.

## C. 요구 정의와 분석

### 전체 Use Case Diagram 설명

Use Case Diagram에서는 시스템 사용자를 Admin, PL, Dev, Tester로 구분한다. PL, Dev, Tester는 일반 이슈 처리 흐름에 참여하는 사용자이고, Admin은 계정과 프로젝트를 관리하는 운영자이다. 따라서 Admin은 프로젝트 생성, 계정 생성, 참여자 관리 같은 관리 기능을 수행하지만, 이슈를 직접 수정하거나 상태를 변경하지 않는다.

이슈 등록, 댓글 추가, 이슈 검색, 이슈 상세 조회, 이슈 배정, 이슈 상태 변경은 이슈 생명주기의 중심 기능으로 보았다. 계정 관리, 프로젝트 관리, 통계 조회, 삭제 이슈 관리, 의존성 관리, 우선순위 변경은 핵심 이슈 흐름을 보조하거나 관리하는 기능으로 분리하였다.

그림 삽입 후보:
- `docs/uml/ucd/ucd-issue-tracking-system.puml`
- UC1-UC16 coverage table은 `docs/requirements-traceability.md`의 UC별 구현 인수 기준 표를 축약해서 사용한다.

### Include 관계

include 관계는 여러 유스케이스에서 반드시 수행되거나 반복적으로 필요한 흐름을 따로 분리하기 위해 사용하였다. UC5는 배정 과정에서 후보 추천이 필요하므로 UC8을 include 한다. UC6과 UC9는 상태 변경 또는 삭제/복구 사유 댓글이 필요하므로 UC2를 include 한다. UC1, UC5, UC6, UC7, UC9, UC10, UC12, UC13, UC16은 권한 확인이 필요한 보호 기능이므로 UC14 Verify Permission을 include 한다.

### Extend 관계

extend 관계는 기본 유스케이스가 독립적으로 수행될 수 있지만, 특정 상황에서 선택적으로 추가되는 흐름을 표현하기 위해 사용하였다. UC2 Add Comment는 이슈 등록 직후, 이슈 상세 화면, 배정 과정에서 선택적으로 수행될 수 있으므로 UC1, UC4, UC5를 extend 한다. UC15 Edit Issue는 사용자가 이슈 상세 화면에서 자신이 등록한 수정 가능한 이슈를 수정하는 경우에만 수행되므로 UC4를 extend 한다.

### Fully Dressed 유스케이스 선정

보고서에는 UC1 Register Issue, UC2 Add Comment, UC3 Search Issues, UC4 View Issue Detail, UC5 Assign/Update Issue Assignment, UC6 Change Issue State를 Fully Dressed 형식으로 제시한다. 이 6개 유스케이스는 사용자가 이슈를 등록하고, 조회하고, 상세를 확인한 뒤, 배정과 상태 변경을 통해 처리하는 핵심 흐름을 포함한다.

UC1은 이슈 생명주기의 시작점이다. active project member가 title, description, priority를 입력하면 시스템은 reporter, reportedDate, NEW 상태, priority 기본값, 생성 이력을 함께 기록한다.

UC2는 댓글 추가 흐름이다. 일반 댓글은 선택적 의견 기록으로 쓰이고, 상태 변경이나 삭제/복구 사유 댓글은 필수 흐름으로 사용된다. 댓글은 writer, created time, purpose를 가지고, 필요한 경우 IssueHistory와 연결된다.

UC3는 이슈 검색과 브라우즈 흐름이다. 프로젝트 active member는 role 구분 없이 프로젝트의 일반 이슈를 조회할 수 있으며, DELETED 이슈는 제외된다. keyword, reporter, assignee, verifier, status, priority, reported date 범위를 조건으로 사용할 수 있다.

UC4는 이슈 상세 조회 흐름이다. 시스템은 이슈 기본 정보, 담당자 정보, 댓글, 이력, 의존성, 사용자가 수행 가능한 action을 제공한다. 상세 조회는 댓글 추가, 수정, 배정, 상태 변경 같은 후속 작업의 진입점이다.

UC5는 배정과 배정 변경 흐름이다. PL은 NEW/REOPENED 상태에서는 assignee와 verifier를 함께 지정하고, ASSIGNED 상태에서는 assignee를 변경하며, FIXED 상태에서는 verifier를 변경한다. UC5는 추천 후보 조회를 위해 UC8을 include 한다.

UC6은 상태 변경 흐름이다. Dev는 ASSIGNED를 FIXED로, Tester는 FIXED를 RESOLVED 또는 ASSIGNED로, PL은 RESOLVED를 CLOSED로 또는 RESOLVED/CLOSED를 REOPENED로 바꿀 수 있다. 상태 변경은 사유 댓글과 이력을 동반하며, FIXED에서 RESOLVED로 전이할 때는 dependency guard를 확인한다.

### 도메인 모델

도메인 모델의 핵심 개념은 User, Project, ProjectMember, Issue, Comment, IssueHistory, IssueDependency이다. User는 역할과 활성 상태를 가진다. Project는 이슈 관리 범위이고, ProjectMember는 사용자가 프로젝트에 참여한다는 사실을 표현한다. ProjectMember를 별도 개념으로 둔 이유는 프로젝트 참여 여부가 권한 판단의 핵심 기준이 되기 때문이다.

Issue는 이슈 관리 시스템의 중심 객체이다. Issue는 title, description, priority, status를 가지며 reporter, assignee, verifier, fixer, resolver와 연결된다. Comment는 일반 댓글과 상태 변경 사유 댓글을 구분하고, IssueHistory는 이슈 생성, 상태 변경, 배정 변경, 우선순위 변경, 댓글 변경, 의존성 변경 등을 추적한다.

IssueDependency는 blocking issue와 blocked issue 관계를 나타낸다. 이 관계는 별도 상태가 아니라 상태 전이 guard로 사용된다. 통계 결과, 대시보드 요약, 추천 결과는 구현에서는 DTO 또는 read model로 존재하지만, 분석 단계의 도메인 모델에서는 화면 표시나 계산 결과에 가까우므로 제외한다.

그림 삽입 후보:
- `docs/uml/domain/domain-model-issue-tracking-system.puml`

### SSD 선정과 설명

보고서 대표 SSD는 UC1 Register Issue, UC5 Assign/Update Issue Assignment, UC6 Change Issue State - Mark Fixed를 사용한다.

SSD 01 Register Issue는 사용자가 이슈 등록에 필요한 title, description, priority를 시스템에 전달하고, 시스템이 새 이슈 등록 결과를 반환하는 흐름을 보여 준다. 등록 직후 선택적으로 댓글을 남길 수 있는 흐름도 함께 표현한다.

SSD 05 Assign/Update Issue Assignment는 PL이 배정 화면을 시작하고, 시스템이 현재 이슈 상태에 맞는 후보 목록을 제공한 뒤, 상태에 따라 assign, reassign, change verifier operation이 달라지는 흐름을 보여 준다.

SSD 06 Mark Fixed는 Dev가 자신에게 배정된 ASSIGNED 이슈를 FIXED 상태로 변경하는 흐름이다. 이 전이는 권한 확인과 사유 댓글을 요구하고, 성공하면 fixer와 상태 변경 이력이 기록된다.

그림 삽입 후보:
- `docs/uml/ssd/ssd-01-register-issue.puml`
- `docs/uml/ssd/ssd-05-assign-issue.puml`
- `docs/uml/ssd/ssd-06-mark-fixed.puml`

### Operation Contract 선정과 설명

보고서 대표 Operation Contract는 다음 세 가지를 사용한다.

| OC | System operation | 선정 이유 |
|---|---|---|
| OC-01 | `registerIssue(projectId, title, description, priority)` | 이슈 생성과 reporter/project/history 연결을 보여 주는 기본 생성 흐름 |
| OC-07 | `changeStatus(issueId, RESOLVED, comment)` | resolver 기록, active assignment 해제, 사유 댓글, dependency guard가 함께 발생하는 복합 상태 전이 |
| OC-14 | `addDependency(blockingIssueId, blockedIssueId)` | 즉시 상태를 바꾸지 않지만 이후 상태 전이에 영향을 주는 관계 생성 흐름 |

OC-01은 새 Issue 생성, Project와 reporter 연결, NEW 상태, reportedDate, priority, 생성 이력을 설명한다. OC-07은 FIXED 이슈를 RESOLVED로 전이할 때 resolver 기록, assignee/verifier 해제, 상태 변경 사유 댓글, 이력 기록, unresolved blocking issue 검사까지 함께 설명한다. OC-14는 IssueDependency가 생성되고 이후 blocked issue의 RESOLVED 전이에 영향을 주는 결과를 설명한다.

## D. 설계

### 전체 설계 구조와 계층 책임

본 프로젝트는 UI와 업무 로직을 분리하기 위해 MVC 스타일을 기반으로 설계하였다. JavaFX와 Swing을 기능별로 나누는 것이 아니라 각각 독립적으로 실행 가능한 UI로 구현하고, UI 이후에는 동일한 Controller, Service, Domain, Repository, JDBC 계층을 사용하도록 하였다.

전체 흐름은 다음과 같다.

```text
JavaFX / Swing UI
  -> Controller
  -> Service / Policy
  -> Domain + Repository Port + Service Port
  -> JDBC Adapter / Technical Adapter
  -> Oracle Database
```

UI는 화면 구성, 사용자 입력 수집, 결과 표시를 담당한다. Controller는 UI 요청을 받아 현재 로그인 사용자를 확인하고 Service로 전달한다. Service는 유스케이스 흐름을 조율하며 권한 검사, 정책 검사, Repository 호출, Domain 객체 호출을 함께 관리한다. Domain은 Issue, User, Project 같은 핵심 개념과 상태 전이, 배정, 댓글, 이력, 의존성 규칙을 담당한다. Repository는 저장소 계약이고, JDBC 구현체는 실제 Oracle DB 접근을 담당한다.

이 구조의 목적은 UI 변경이 이슈 처리 정책이나 도메인 규칙에 영향을 주지 않게 하는 것이다. JavaFX와 Swing은 서로 다른 화면 구현이지만 동일한 Controller와 Service를 호출하므로, 핵심 기능은 같은 정책으로 동작한다.

그림 삽입 후보:
- `docs/uml/architecture/logical-architecture-its.puml`
- `docs/uml/architecture/logical-architecture-its-detail.puml`

### Sequence Diagram

Sequence Diagram은 SSD와 Operation Contract에서 정의한 system operation을 내부 객체 협력으로 확장한 설계 산출물이다. SSD가 actor와 system 사이의 메시지를 블랙박스 관점에서 보여 준다면, SD는 요청이 내부에서 어떤 객체들에게 전달되고 각 객체가 어떤 책임을 수행하는지 보여 준다.

보고서 대표 SD는 SD-01 Register Issue와 SD-07 Resolve Fixed Issue를 사용한다. SD-01은 이슈가 생성되고 reporter, project, history와 연결되는 흐름을 보여 준다. SD-07은 FIXED 상태의 이슈가 RESOLVED로 전이될 때 사유 댓글, resolver 기록, active assignment 해제, dependency guard가 함께 처리되는 흐름을 보여 준다.

그림 삽입 후보:
- `docs/uml/sd/` 아래 register issue 관련 SD
- `docs/uml/sd/` 아래 resolve fixed issue 관련 SD

### Design Class Diagram

Design Class Diagram은 Domain Model을 설계 단계로 확장한 산출물이다. Domain Model이 업무 개념과 관계를 보여 주는 데 초점을 두었다면, DCD는 실제 설계 클래스가 어떤 필드와 operation을 가지는지 보여 준다.

DCD에서 가장 중심이 되는 클래스는 Issue이다. Issue는 title과 status를 가진 단순 데이터가 아니라 등록, 배정, 수정, 검증, 종료, 재오픈, 삭제 흐름을 거치는 핵심 객체이다. 따라서 Issue에는 상태 변경, assignee/verifier 관리, comment/history 생성, dependency 변경 같은 operation이 배치된다.

User는 시스템 사용자를 나타내며 Role과 active 상태를 가진다. Project는 이슈가 등록되는 단위이고 ProjectMember는 사용자가 특정 프로젝트에 참여한다는 사실을 표현한다. Comment와 IssueHistory는 이슈 처리 과정의 기록을 담당하고, IssueDependency는 이슈 간 선후 관계를 나타낸다.

그림 삽입 후보:
- `docs/uml/dcd/its_dcd.puml`

### GRASP 적용

GRASP Controller는 외부 system event를 받아 유스케이스 흐름을 시작하는 역할로 적용하였다. 이 프로젝트에서는 `AuthenticationController`, `IssueController`, `AssignmentController`, `IssueStateController`, `DeletedIssueController`, `ProjectController`, `AccountController`, `StatisticsController`가 이 역할을 맡는다.

Information Expert는 필요한 정보를 가장 많이 가진 객체에게 책임을 주는 원칙이다. 이슈의 상태, assignee, verifier, fixer, resolver, priority, comment, history는 Issue와 밀접하게 관련되어 있으므로 상태 전이, 배정 변경, 우선순위 변경, 댓글/의존성 이력 생성의 핵심 책임은 Issue에 배치하였다.

Creator 원칙은 Comment, IssueHistory, IssueDependency처럼 특정 Issue에 종속되는 기록 또는 관계를 생성할 때 적용하였다. 이 객체들은 이슈의 변화와 함께 생기므로 Issue 중심 흐름에서 생성되도록 설계하였다.

Low Coupling과 High Cohesion은 계층 분리로 반영하였다. Service는 JDBC 구현체가 아니라 Repository interface에 의존한다. Domain은 DB 접근을 알지 않고 자신의 상태와 규칙에 집중한다. password hashing, session, clock, id 생성 같은 technical 기능도 service port와 구현체로 분리하였다.

Protected Variations는 바뀔 수 있는 구현을 interface 뒤로 숨기는 설계에서 적용하였다. DB 저장 방식은 Repository interface, 비밀번호 해시는 `PasswordHashing`, 현재 시각은 `Clock`, 댓글 id 생성은 `CommentIdProvider`, 로그인 세션은 `CurrentUserSession`을 통해 분리하였다.

Pure Fabrication은 도메인 객체에 넣기 애매하지만 시스템에는 필요한 책임을 별도 객체로 분리한 부분에서 적용되었다. `PermissionPolicy`, `AssignmentRecommendationService`, `DashboardSummaryService`, `StatisticsService`, `ApplicationBootstrap`이 여기에 해당한다.

### 주요 정책의 설계 반영

권한 정책은 `PermissionPolicy`와 각 Service의 프로젝트 멤버십 검사를 통해 반영하였다. Service는 유스케이스를 수행하기 전에 현재 사용자가 대상 프로젝트와 이슈에 대해 올바른 권한을 가지는지 확인한다.

상태 전이 정책은 `IssueStateService`와 `Issue`를 중심으로 설계하였다. `IssueStateService`는 현재 사용자, 대상 이슈, 목표 상태, 상태 변경 사유, dependency guard를 검증하고, 실제 상태 변경은 Issue가 수행한다.

삭제 이슈 관리는 `DeletedIssueService`와 `DeletedIssueRepository`로 일반 이슈 흐름과 분리하였다. DELETED 상태 이슈는 일반 조회와 검색에서 제외되고, PL의 deleted issue 관리 흐름에서만 조회, 복구, 영구 삭제가 가능하다.

의존성 정책은 `IssueDependency`와 상태 전이 guard로 반영하였다. 의존성은 별도 status가 아니라 FIXED에서 RESOLVED로 전이할 때 확인하는 제약 조건이다.

Assignment 추천 기능은 추천 데이터 조회와 추천 계산 책임을 분리하였다. 과거 해결 이력과 active 후보 조회는 `AssignmentRecommendationRepository`가 담당하고, 후보 계산은 `KNNAssignmentRecommendation`이 담당한다.

### Controller API 흐름표

| UI 기능 | Controller API | Service 흐름 |
|---|---|---|
| 로그인/로그아웃 | `AuthenticationController.login`, `logout` | `AuthenticationService` |
| dashboard 조회 | `DashboardController.viewProjects`, `viewUsers` | `DashboardSummaryService` |
| 계정 관리 | `AccountController.createAccount`, `renameAccount`, `changeAccountRole`, `activateAccount`, `deactivateAccount` | `AccountService` |
| 프로젝트 관리 | `ProjectController.createProject`, `renameProject`, `changeProjectDescription`, `deleteProject`, `addProjectParticipant`, `removeProjectParticipant` | `ProjectService` |
| 이슈 등록/검색 | `IssueController.registerIssue`, `viewProjectIssues`, `searchIssues` | `IssueService` |
| 이슈 상세/가능 액션 | `IssueController.viewIssueDetail`, `viewAvailableActions`, `viewCommentActions` | `IssueService`, `IssueWorkflowService` |
| 댓글 | `IssueController.addComment`, `viewComments`, `updateComment`, `deleteComment` | `IssueService` |
| 우선순위/수정 | `IssueController.changePriority`, `updateIssue` | `IssueService` |
| 의존성 | `IssueController.addDependency`, `viewProjectDependencies`, `removeDependency` | `IssueService` |
| 배정 | `AssignmentController.startAssignment`, `assignIssue`, `reassignIssue`, `changeVerifier` | `AssignmentService`, `AssignmentRecommendationService` |
| 상태 변경 | `IssueStateController.changeStatus` | `IssueStateService` |
| 삭제 이슈 | `DeletedIssueController.viewDeletedIssues`, `deleteIssue`, `restoreIssue`, `purgeDeletedIssue` | `DeletedIssueService` |
| 통계 | `StatisticsController.viewStatistics` | `StatisticsService` |

## E. 구현 결과

### 실행 구조

기본 실행은 JavaFX이며, Swing은 별도 Gradle task 또는 `--swing` 인자를 통해 실행한다.

```text
JavaFX 실행: ./gradlew run --console=plain
Swing 실행: ./gradlew runSwing --console=plain
```

`Main`은 `--swing` 인자가 있으면 `SwingApp`을 실행하고, 그렇지 않으면 `JavaFXApp`을 실행한다. 두 UI는 `ApplicationBootstrap`이 생성한 같은 `ApplicationContext`의 controller들을 사용한다.

### 로그인

로그인 기능은 active 계정만 시스템에 접근할 수 있도록 한다. 로그인 성공 시 사용자의 역할에 따라 Admin dashboard 또는 일반 project list 화면으로 이동한다. 실패 시에는 잘못된 계정, 비밀번호, 비활성 계정 상태를 화면에 표시한다.

화면 캡처 후보:
- JavaFX login 화면
- Swing login 화면
- 비활성/잘못된 로그인 실패 메시지

### 대시보드와 프로젝트 진입

Admin은 전체 프로젝트 요약과 전체 사용자 목록을 확인할 수 있다. Admin 프로젝트 상세는 프로젝트 기본 정보와 참여자 정보를 보여 주지만, 일반 이슈 목록은 표시하지 않는다.

PL, Dev, Tester는 자신이 active member로 참여한 프로젝트 목록만 볼 수 있다. 프로젝트를 선택하면 일반 이슈 목록으로 들어가며, DELETED 상태의 이슈는 이 목록에서 제외된다.

화면 캡처 후보:
- Admin dashboard
- Admin project detail
- PL/Dev/Tester project list

### 계정 관리

Admin은 계정을 생성하고 이름, 역할, active 상태를 변경할 수 있다. 계정은 이력 보존을 위해 물리 삭제하지 않고 active 값으로 관리한다. 이미 프로젝트나 이슈 책임을 가진 계정의 역할 변경/비활성화는 Service 정책에서 제한한다.

화면 캡처 후보:
- Account management 목록
- 계정 생성/수정 dialog
- 비활성화 또는 역할 변경 실패 메시지

### 프로젝트 관리

Admin은 프로젝트를 생성, 수정, 삭제하고 참여자를 추가/제거한다. 프로젝트 참여자는 이슈 조회와 상태 변경 권한 판단의 기준이므로 `ProjectMember`로 별도 관리한다. active assignment가 남아 있는 사용자는 프로젝트에서 제거할 수 없도록 guard를 둔다.

화면 캡처 후보:
- Project management 목록
- Project detail/member management
- 프로젝트 멤버 추가/제거 화면

### 이슈 등록, 조회, 검색

프로젝트 active member는 이슈를 등록할 수 있다. 등록 시 reporter는 현재 사용자로 기록되고, reportedDate는 현재 시각으로 저장된다. priority가 생략되면 MAJOR가 기본값이 된다. 등록 직후 CREATED 이력이 남는다.

이슈 목록은 프로젝트의 일반 이슈를 보여 준다. 검색은 keyword, status, priority, reporter, assignee, verifier, reported date 범위를 지원한다. DELETED 이슈는 일반 검색에서 제외된다.

화면 캡처 후보:
- Issue list
- Issue register form
- Advanced search 조건
- 검색 결과

### 이슈 상세, 댓글, 이력

이슈 상세 화면은 이슈 기본 정보, reporter, assignee, verifier, fixer, resolver, priority, status, 댓글, 이력, 의존성, 가능한 action을 함께 보여 준다. 가능한 action은 UI 버튼 표시를 돕는 정보이며, 실제 실행 시에는 Service가 다시 권한과 정책을 검사한다.

댓글은 일반 댓글과 상태 변경 사유 댓글로 구분된다. 일반 댓글은 작성자가 수정/삭제할 수 있고, 상태 변경 사유 댓글은 수정/삭제할 수 없다.

화면 캡처 후보:
- Issue detail
- Comment add/edit/delete
- History table
- Available action buttons

### 이슈 상태 전이

상태 전이는 `IssueStateController.changeStatus`로 들어오고 `IssueStateService`가 권한, 상태, 프로젝트 멤버십, 사유 댓글, dependency guard를 검사한다. 실제 상태 변경과 이력 생성은 Issue 중심으로 처리된다.

대표 흐름은 다음과 같다.

```text
ASSIGNED -> FIXED: assignee Dev가 수정 완료 처리
FIXED -> RESOLVED: verifier Tester가 검증 완료 처리
FIXED -> ASSIGNED: Tester가 검증 실패로 재작업 요청
RESOLVED -> CLOSED: PL이 최종 종료
RESOLVED/CLOSED -> REOPENED: PL이 재오픈
```

화면 캡처 후보:
- Dev mark fixed dialog
- Tester resolve/reject dialog
- PL close/reopen dialog
- dependency guard 실패 메시지

### Assignment 추천과 배정

배정 기능은 `AssignmentController`와 `AssignmentService`가 담당한다. PL이 배정을 시작하면 Service는 현재 이슈 상태에 맞는 후보 목록을 만들고, `AssignmentRecommendationService`를 통해 추천 후보를 계산한다.

추천은 과거 resolved 이슈의 title/description과 fixer/resolver 이력을 사용한다. `KNNAssignmentRecommendation`은 keyword 기반 Jaccard 유사도와 description 기반 TF-IDF cosine 유사도를 조합하여 후보를 정렬한다. 추천 후보는 최대 3명으로 제한한다. 추천 데이터가 부족한 경우에도 전체 active Dev/Tester 후보 목록을 함께 제공한다.

추천은 자동 배정이 아니라 PL의 의사결정을 돕는 기능이다. 최종 배정은 PL이 선택한다.

화면 캡처 후보:
- Assignment start 화면
- 추천 후보 Top 3
- 전체 active Dev/Tester 후보 목록

### 의존성 관리

PL은 이슈 간 blocking/blocked 관계를 추가하거나 제거할 수 있다. 시스템은 같은 프로젝트 이슈인지, 자기 자신을 막는 관계인지, 중복 또는 순환 관계인지 확인한다.

의존성은 상태가 아니라 guard로 사용된다. blocked issue가 RESOLVED로 전이되려면 모든 blocking issue가 RESOLVED 또는 CLOSED 상태여야 한다.

화면 캡처 후보:
- Dependency add/remove dialog
- dependency graph 또는 dependency list
- unresolved blocking issue 실패 메시지

### 삭제 이슈 관리

PL은 NEW 또는 CLOSED 이슈를 DELETED 상태로 soft delete할 수 있다. 삭제 이슈는 일반 목록과 검색에서 제외되고, PL 전용 deleted issue 화면에서만 조회된다. PL은 deleted issue를 삭제 직전 상태로 restore하거나 단건 purge할 수 있다.

프로젝트별 deleted issue 보관 한도는 30개이며, 초과 시 오래된 deleted issue부터 FIFO 방식으로 물리 삭제한다.

화면 캡처 후보:
- Deleted issue list
- soft delete dialog
- restore dialog
- purge action

### 프로젝트 통계

통계 화면은 프로젝트별 일/월 단위 이슈 발생 수, 상태 변경 수, 댓글 수, 상태 분포, 우선순위 분포를 제공한다. 프로젝트 active member만 조회할 수 있고, DELETED 상태 이슈는 통계에서 제외한다.

화면 캡처 후보:
- Statistics overview
- 기간 필터
- status/priority distribution
- 잘못된 기간 입력 실패 메시지

### JavaFX와 Swing 재사용 증거

JavaFX와 Swing은 역할별로 나뉜 UI가 아니라, 같은 기능을 각각 제공하는 별도 UI surface이다. 두 UI 모두 로그인, 프로젝트 목록, 이슈 목록/상세, 상태 변경, 배정, 삭제 이슈, 통계 흐름을 제공한다.

핵심 증거는 두 UI가 같은 `ApplicationContext`에서 controller를 가져와 호출한다는 점이다. `ApplicationBootstrap`은 repository, service, controller를 한 번 조립하고, UI는 이 controller들을 사용한다. 따라서 화면 구현 방식이 달라도 업무 규칙과 DB 접근 코드는 공유된다.

보고서에서는 같은 계정과 같은 seed data로 JavaFX와 Swing에서 동일한 업무 흐름을 실행한 화면을 나란히 배치한다.

## F. JUnit 테스트 수행 내역

### 테스트 구조

현재 테스트는 domain, service, controller, persistence, setup, technical, UI 단위로 나뉜다. `./gradlew check --console=plain`은 일반 단위 테스트와 저장소 구성 검사를 수행한다. Oracle 통합 테스트는 CI 또는 local Oracle 환경에서 별도로 확인한다.

최신 로컬 실행 기준 JUnit XML 요약은 다음과 같다.

```text
test_classes=85
tests=696
failures=0
errors=0
skipped=34
```

테스트 클래스 분포는 다음과 같다.

| 영역 | 테스트 클래스 수 | 주요 목적 |
|---|---:|---|
| application smoke | 1 | 기본 entry point와 Swing 실행 인자 분기 확인 |
| controller | 10 | Controller가 현재 사용자 확인 후 service 경계로 요청을 넘기는지 확인 |
| domain | 8 | User, Project, Issue, Comment, History, Dependency 규칙 확인 |
| persistence | 7 | DB 환경, initializer, JDBC query/write support, Oracle repository integration 확인 |
| service | 13 | 권한, 이슈 등록/검색/상세, 배정, 상태 전이, 삭제, 통계, 추천 흐름 확인 |
| setup | 6 | architecture boundary, workflow guard, repository convention, 공개 이력 정책 확인 |
| support | 1 | 테스트 fixture/id provider 지원 코드 확인 |
| technical | 4 | password hashing, session, id generator 등 technical adapter 확인 |
| ui/javafx | 1 | JavaFX graph screen 단위 검증 |
| ui/swing | 34 | Swing presenter, panel, dialog, request 객체 단위 검증 |

### 주요 테스트 케이스 목적

Domain 테스트는 도메인 객체가 자기 책임을 올바르게 수행하는지 검증한다. `IssueWorkflowTest`는 상태 전이와 이력 생성, `IssueDependencyTest`는 의존성 규칙, `CommentTest`와 `IssueHistoryTest`는 댓글과 이력 기록을 확인한다.

Service 테스트는 유스케이스 흐름과 권한 정책을 검증한다. `IssueServiceTest`는 등록, 검색, 상세, 댓글, 의존성, 수정 흐름을 확인한다. `AssignmentServiceTest`는 상태별 배정 방식을 검증하고, `IssueStateServiceTest`는 상태 전이 권한과 dependency guard를 확인한다. `DeletedIssueServiceTest`는 soft delete, restore, purge, FIFO 보관 한도를 확인한다. `StatisticsServiceTest`와 `AssignmentRecommendationServiceTest`는 통계와 추천 기능을 검증한다.

Controller 테스트는 UI가 호출하는 API 경계를 검증한다. Controller는 current user를 확인하고 Service에 요청을 넘기므로, 로그인 요구, 권한 실패, 성공 결과 반환을 중심으로 확인한다.

Persistence 테스트는 Oracle 연결 환경과 JDBC repository 동작을 확인한다. `OracleRepositoryIntegrationTest`는 실제 Oracle 테스트 DB에서 repository가 schema/seed와 맞게 동작하는지 검증한다.

Setup 테스트는 설계와 저장소 운영 규칙을 지키는지 확인한다. `ArchitectureBoundaryTest`는 domain이 presentation/persistence에 의존하지 않는지 확인하고, `RepositoryConventionsSmokeTest`는 필수 자동화 파일과 정책 문서를 확인한다.

Swing UI 테스트는 화면 객체가 직접 DB나 Service를 새로 만들지 않고 presenter/controller 계약을 통해 동작하는지 확인한다. dialog request 객체, presenter, panel 단위 테스트를 통해 주요 UI 흐름을 검증한다.

### 검증 명령과 결과

보고서에는 최종 제출 직전 최신 `dev` 기준으로 아래 명령 결과를 캡처한다.

```text
./gradlew check --console=plain
./gradlew verifySubmissionMetadata --console=plain
./gradlew oracleLocalTest --console=plain
```

Oracle local 실행이 어려운 환경에서는 GitHub Actions의 `Oracle 통합 테스트` job과 artifact를 제출 증거로 연결한다.

### Swing QA

Swing QA는 `docs/qa/swing-full-qa-2026-06-01.md`를 기준으로 정리한다. Oracle fixed seed 기준으로 Admin, PL, Dev, Tester 주요 route smoke를 확인했고, 이슈 등록/조회/수정/우선순위/배정/댓글/의존성/삭제 이슈/통계 흐름을 점검했다.

다만 WSLg/Robot 자동화에서는 네이티브 마우스 focus 증거가 충분하지 않아, 발표 장비에서 로그인 ID/password field click focus와 Sign in 버튼 click submit을 수동으로 다시 확인해야 한다. 이 항목은 제출 전 #246에서 최종 확인한다.

## G. GitHub 프로젝트 활용 요약

### Repository와 Project

GitHub repository와 Project는 과제 진행 이력을 남기기 위한 협업 도구로 사용하였다.

```text
Repository: https://github.com/marcellokim/se-issue-tracker
Project: https://github.com/users/marcellokim/projects/1
```

보고서에는 Project board 전체 화면, M4 milestone open gate, 대표 PR 목록, CI checks 결과를 캡처해 넣는다. 협업 섹션은 2쪽 이내로 압축하고, 본문에는 대표 흐름만 넣는다.

### Issue / Branch / PR 흐름

작업 흐름은 이슈 생성, 브랜치 생성, 구현과 검증, PR 작성, 리뷰, dev 병합 순서로 운영하였다. 일반 PR의 대상 branch는 dev이다. branch 이름은 `feat/<issue>-<slug>`, `fix/<issue>-<slug>`, `docs/<issue>-<slug>`, `test/<issue>-<slug>`, `ci/<issue>-<slug>`, `chore/<issue>-<slug>`, `refactor/<issue>-<slug>` 형식을 사용한다.

PR 본문에는 목적, non-goals, diff map, verification evidence, reviewer focus areas, risk/rollback을 적어 리뷰어가 범위와 위험을 빠르게 판단할 수 있게 하였다.

### 주요 PR 요약

최종 보고서에는 모든 PR을 나열하지 않고 대표 PR만 넣는다.

| 범위 | 보고서에 넣을 대표 증거 |
|---|---|
| JavaFX 전체 UI | JavaFX UI PR, navigation map, 실행 화면 |
| Swing 전체 UI | Swing UI PR, Swing QA report, 실행 화면 |
| 이슈 열람 정책 | PR #266, UC3 Search Issues 정책 정리 |
| SonarCloud/CI 보정 | SonarCloud workflow와 branch protection 필수 체크 설명 |
| 제출 증빙 정리 | PR #271, README 템플릿, 패키징 기준, evidence checklist |

### 코드 리뷰 반영 사례

코드 리뷰는 단순히 승인 절차가 아니라 설계와 제출 정합성을 맞추기 위한 과정으로 사용하였다. 대표 사례는 다음과 같다.

- Admin 프로젝트 상세에서 일반 이슈 목록을 보여 주지 않도록 정책과 구현을 맞춤.
- DELETED 이슈를 일반 목록, 검색, 통계에서 제외하고 PL 전용 deleted issue 흐름으로 분리함.
- `IssueWorkflowService`의 available action 표시와 실제 실행 Service 정책이 어긋나지 않도록 조정함.
- 제출 zip에서 개인 설정, 보조 리뷰 도구 설정, QA artifact, 강의 원문 PDF가 포함되지 않도록 패키징 기준을 보강함.
- GraphQL 한도 영향을 받는 Project metadata 보정 자동화는 branch protection 필수 체크로 두지 않고, REST/local evidence와 최종 수동 캡처로 보완함.

### 팀원별 기여 내용 작성 슬롯

실제 PDF에서는 팀원이 최종 확인한 뒤 아래 슬롯을 채운다.

| 팀원 | 주요 담당 | 증거 |
|---|---|---|
| 김민채 | 최종 문서, traceability, 발표 흐름 | PDF 목차, UC별 구현 매핑, 슬라이드 초안 |
| 김윤동 | Swing UI, 제출 패키지, QA evidence | Swing 실행 화면, QA report, package command |
| 문상혁 | JavaFX UI, 상태 전이, 추천/통계 구현 | JavaFX 실행 화면, 상태 전이 사례, 추천/통계 seed data |

담당 표는 최종 PR/issue 담당자와 실제 발표 담당자를 기준으로 제출 직전에 다시 조정한다.

## H. 한계와 향후 보완

### UI 완성도와 최종 시연 확인

JavaFX와 Swing 모두 전체 UI 흐름을 제공하지만, 최종 발표 장비에서의 시각 확인은 별도로 필요하다. 특히 Swing은 WSLg 자동화에서 네이티브 마우스 click focus를 충분히 검증하지 못했으므로, 발표 장비에서 로그인 필드 클릭 입력, Sign in 버튼 클릭, 800x600 최소 크기 주요 화면 표시를 확인해야 한다.

### 추천 기능 고도화

현재 추천은 프로젝트 내부의 과거 resolved 이슈와 fixer/resolver 이력을 기반으로 동작한다. title keyword Jaccard 유사도와 description TF-IDF cosine 유사도를 조합해 후보를 정렬하고, 최대 3명의 추천 후보를 제공한다.

향후에는 외부 학습 모델을 도입하여 이슈 텍스트, component, label, 과거 처리 시간, 사용자 workload를 함께 반영할 수 있다. 다만 현재 과제 범위에서는 추천을 자동 배정으로 사용하지 않고 PL의 선택을 돕는 보조 기능으로 제한하였다.

추천 품질을 정량 평가하려면 실제 운영 데이터에서 Precision@K, Recall@K, Mean Reciprocal Rank 같은 기준을 사용할 수 있다. 현재는 seed data와 단위 테스트로 추천 결과 구조와 fallback 동작을 검증하였다.

### 통계 시각화 보완

현재 통계는 일/월 단위 이슈 발생 수, 상태 변경 수, 댓글 수, 상태/우선순위 분포를 제공한다. 향후에는 기간별 trend chart, 사용자별 처리량, 평균 처리 시간, 상태별 체류 시간 같은 지표를 추가할 수 있다.

통계는 DELETED 이슈를 제외하도록 구현되어 있다. 이 정책은 일반 이슈 흐름과 삭제 이슈 관리 흐름을 분리하기 위한 것이다.

### 댓글 조회 API 경계

현재 댓글 조회는 `IssueController.viewComments(issueId)`를 통해 제공된다. 댓글은 독립 resource처럼 보일 수도 있지만, 이 프로젝트에서는 댓글 접근 권한이 이슈와 프로젝트 접근 권한에 종속되므로 IssueController 아래에 두었다.

향후 API 구조를 더 엄격하게 나누려면 `CommentController`를 별도로 만들 수 있다. 다만 이 경우에도 댓글 조회는 반드시 issue 접근 권한과 comment 목적 정책을 함께 검사해야 한다. 따라서 현재 구조의 핵심은 "댓글이 이슈 상세 흐름의 하위 기능으로 조회된다"는 점이며, 별도 controller 분리 여부는 확장 시점의 선택 사항으로 둔다.

### 문서와 코드 동기화

프로젝트 후반에는 구현, 테스트, 문서, GitHub Project metadata가 동시에 바뀌므로 문서와 코드가 어긋날 위험이 있다. 이를 줄이기 위해 `docs/requirements-traceability.md`, `docs/submission-evidence-checklist.md`, `docs/final-report-outline.md`, `docs/demo-scenario.md`를 기준 문서로 두었다.

최종 제출 직전에는 다음 항목을 다시 확인한다.

- UC1-UC16 구현/API/test/UI evidence가 최신 dev 기준인지
- JavaFX와 Swing 스크린샷이 같은 기능을 비교할 수 있게 준비되었는지
- `./gradlew check`와 Oracle integration evidence가 최신인지
- GitHub Project, milestone, issue, PR, CI 캡처가 최신인지
- 최종 PDF, slides, demo video, README.txt, 제출 zip 내용이 서로 충돌하지 않는지

## 최종 PDF에 넣을 그림과 캡처 체크리스트

| 위치 | 넣을 자료 | 원천 |
|---|---|---|
| 요구사항 분석 | UCD, UC coverage matrix | `docs/uml/ucd/`, `docs/requirements-traceability.md` |
| 도메인 분석 | Domain Model | `docs/uml/domain/domain-model-issue-tracking-system.puml` |
| 시스템 동작 분석 | SSD 01, SSD 05, SSD 06 | `docs/uml/ssd/` |
| 시스템 동작 분석 | OC-01, OC-07, OC-14 요약 | `docs/uml/operation-contracts/required_operation_contracts.md` |
| 설계 | Logical Architecture | `docs/uml/architecture/logical-architecture-its*.puml` |
| 설계 | SD-01, SD-07 | `docs/uml/sd/` |
| 설계 | DCD | `docs/uml/dcd/its_dcd.puml` |
| 구현 결과 | JavaFX와 Swing 같은 기능 비교 캡처 | 실제 실행 화면 |
| 테스트 | `./gradlew check`, CI, Oracle 통합 테스트 | 로컬 실행 로그, GitHub Actions |
| 협업 | Project board, milestone, 대표 PR | GitHub 화면 캡처 |
