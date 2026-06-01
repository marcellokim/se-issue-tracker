# 기본 가정

이 문서는 텀 프로젝트 초기에 합의한 기본 가정을 정리합니다. 구현이 진행되면서 바뀌면 PR과 함께 갱신합니다. 현재 내용은 로컬 코드의 Controller/Service/Repository 정책을 기준으로 보정합니다.

## 상태 전이 정의
상태 이름은 두 층에서 분리해서 사용합니다.

### 애플리케이션 이슈 상태
과제 PDF의 최소 상태 목록과 팀 회의 확정 상태를 함께 반영합니다.

기본 흐름:

`new -> assigned -> fixed -> resolved -> closed`

보조 상태/흐름:
- `fixed`는 Dev가 수정 완료를 주장한 중간 상태입니다.
- Tester가 fixed 이슈를 검증 성공하면 `resolved`로 전이합니다.
- `fixed -> resolved` 전이에서는 resolver를 현재 Tester로 기록하고 fixer는 유지합니다. assignee/verifier는 현재 작업 배정이 끝난 것으로 보고 제거합니다.(assignee와 verifier 둘다 null로 드랍)
- Tester가 fixed 이슈를 검증 실패하면 `assigned`로 되돌리고 실패 사유를 comment/history에 남깁니다.
- `reopened`는 resolved/closed 이슈를 PL이 다시 작업 대상으로 판단할 때 사용합니다. Reopen 후에는 PL이 UC5로 assignee/verifier를 지정해 `assigned` 상태부터 재작업을 시작합니다.
- `deleted`는 불필요한 이슈의 soft-delete 상태입니다. soft delete 이후 프로젝트별 deleted 이슈가 30개를 초과하면 deleted 전이 시각 기준 FIFO로 오래된 이슈부터 물리 삭제합니다.
- deleted 전이 시각은 `IssueHistory(STATUS_CHANGED, newValue=DELETED).changedDate`에서 결정합니다. 별도 `Issue.deletedAt` 속성은 두지 않습니다.
- 만약 `deleted` 상태인 이슈에 대해 PL이 해당 이슈가 다시 필요하다고 여기면 해당 이슈가 물리적으로 삭제되기 전에 삭제 직전 상태(`NEW` 또는 `CLOSED`)로 복구해 물리적 삭제를 막을 수 있습니다.

### 상태 변경 이력과 자동 필드 동기화 정책
- `ASSIGNED -> FIXED` 전이에서 fixer가 설정되고, `FIXED -> RESOLVED` 전이에서 resolver가 설정됩니다.
- 두 전이는 각각 `IssueHistory(STATUS_CHANGED)`에 함께 기록되며, fixer/resolver는 해당 상태 전이 기록과 의미상 일치해야 합니다.

### deleted 상태 전이 세부 정책
- new->deleted, closed->deleted로의 전이만 가능합니다.
- deleted로의 상태 전이는 해당 프로젝트의 active PL만 할 수 있는 권한입니다.
- DEV, TESTER는 deleted 상태의 이슈를 볼 수 없습니다.
- 이슈의 상태가 deleted에서 삭제 직전 상태(`NEW` 또는 `CLOSED`)로 복구되면 DEV, TESTER는 권한 범위 안에서 해당 이슈를 다시 볼 수 있습니다.
- deleted 이슈는 해당 프로젝트 PL만 볼 수 있는 별도의 deleted issue 관리 흐름에 축적됩니다.
- soft delete와 restore에는 사유 comment가 필요합니다.
- deleted 이슈가 프로젝트별 보관 한도 30개를 초과하면 FIFO 방식으로 오래된 deleted 이슈가 물리 삭제됩니다.
- PL은 deleted issue 관리 흐름에서 deleted 이슈를 restore할 수 있으며, 이 경우 복구 상태는 `NEW/CLOSED -> DELETED` 전이 때 기록된 `IssueHistory(STATUS_CHANGED).previousValue`에서 결정합니다.
- PL은 deleted 상태의 이슈를 단건으로 물리 삭제할 수 있습니다. 단건 물리 삭제와 보관 한도 초과 정리는 별도 사유 comment를 요구하지 않습니다.

### reopen 상태 전이 세부 정책
- resolved/closed 상태에서만 reopen 상태 전이가 가능합니다.
- reopen 상태 전이는 PL만 할 수 있는 고유 권한입니다.
- reopen 상태 전이가 발생해도 assignee와 verifier를 자동 복원하지 않습니다.
- reopen시 reporter 값은 변경할 수 없습니다.
- reopen시 기존 fixer와 resolver는 보존되어 PL에게 참고 정보로 제시될 수 있습니다.
- PL이 reopen 상태의 이슈에서 assignee와 verifier를 결정하면 해당 이슈는 assigned 상태로 전이됩니다.(reopen->assigned)
- `resolved`-> `closed` 전이에서는 active assignee/verifier 없이 종료 상태로 이동하며, fixer와 resolver는 보존합니다.

### UC5 배정/배정 변경 정책
- UC5는 PL이 이슈의 assignee/verifier를 배정하거나 변경하는 사용자 목표입니다.
- UC5의 상태별 흐름은 `new->assigned`, `reopened->assigned`, `assigned->assigned(assignee 변경)`, `fixed->fixed(verifier 변경)`입니다.
- UC8 Recommend Assignment Candidates는 UC5에서 항상 include되며, 대상 Issue status에 따라 추천 후보 종류를 다르게 반환합니다.
- `new`와 `reopened`에서는 Dev assignee 후보와 Tester verifier 후보를 모두 추천합니다.
- `assigned`에서는 Dev assignee 후보만 추천합니다.
- `fixed`에서는 Tester verifier 후보만 추천합니다.
- 추천 응답은 상태별 추천 후보 Top 3와 프로젝트의 active DEV/TESTER 전체 후보 목록을 함께 반환합니다.
- reopened에서는 기존 fixer가 현재 프로젝트의 active DEV 후보이면 Dev 추천 후보의 앞쪽에 우선 배치합니다.
- `assigned`와 `fixed`에서는 각각 현재 assignee, 현재 verifier를 우선 후보로 보여줄 수 있습니다.

### 배정 정보 변경 이력 정책
- `assigned->assigned(assignee 변경)`과 `fixed->fixed(verifier 변경)`은 status 값이 바뀌지 않는 배정 정보 변경입니다.
- 이 변경은 상태 전이(`STATUS_CHANGED`)가 아니라 배정 변경 이력(`ASSIGNMENT_CHANGED`)으로 기록합니다.
- `ASSIGNMENT_CHANGED`를 사용하는 경우 도메인 모델의 `ActionType`에도 해당 값을 포함합니다.

### 이슈 종속성에 따른 상태 전이 영향
- 종속되는 이슈(blocked issue)를 해결하기 위해서는 종속하는 이슈(blocking issue)를 먼저 해결해야합니다
- 예를 들어, 두 이슈 A, B에 대해 B를 해결하기 위해서는 A가 B보다 먼저 해결되어야 합니다.(A가 blocking issue, B가 blocked issue)
- blocking issue가 resolved 또는 closed 상태가 아닌데 blocked issue가 resolved로 상태전이를 시도하면, 시스템상에서 이슈 종속성 관계 검사를 통해 blocked issue의 resolved로의 상태전이를 차단합니다.
- 별도 `BLOCK`/`BLOCKED` 이슈 상태는 도입하지 않고, dependency는 `FIXED -> RESOLVED` 전이에 대한 guard로만 사용합니다.
- 이슈가 PL에 의해 DELETED 상태로 전이되면, 해당 이슈가 blocking issue이든 blocked issue이든 그 이슈가 포함된 dependency row는 자동 제거됩니다.

### 권한 및 수정 정책
- User당 직군/역할은 하나만 부여합니다.
- inactive 계정은 로그인과 project와 issue 접근이 불가능합니다.
- ADMIN은 계정 관리와 프로젝트 관리의 주체입니다. 일반 이슈 작업 흐름은 PL/DEV/TESTER의 active project membership을 기준으로 합니다.
- Dashboard에서 ADMIN은 전체 프로젝트 요약과 전체 사용자 목록을 볼 수 있고, non-ADMIN은 자신이 참여한 프로젝트 요약만 볼 수 있습니다.
- ADMIN 프로젝트 상세 화면은 프로젝트 기본 정보와 참여자 정보를 보여주며, 프로젝트 이슈 목록은 포함하지 않습니다.
- non-ADMIN 프로젝트 화면은 프로젝트 기본 정보와 해당 프로젝트 내 관련 이슈 목록을 나누어 조회합니다.
- PL은 해당 프로젝트의 일반 이슈를 볼 수 있고, DEV/TESTER는 reporter, 현재 assignee, 현재 verifier로 관련된 이슈를 봅니다. fixer/resolver 완료 이력자는 현재 관련 이슈 기준에 포함하지 않습니다.
- Reporter는 `NEW` 또는 `REOPENED` 상태일때만 자신의 이슈 title/description을 수정할 수 있습니다.
- assigned 이후 title/description 정정과 추가 정보는 comment로 남깁니다.
- Priority는 PL만 변경할 수 있으며, assigned 상태와 무관하게 변경 가능합니다.
- 이슈 dependency 관계는 comment가 아니라 구조화된 데이터로 관리합니다.
- 이슈 상세 조회 결과에는 UI 버튼 활성화를 위한 `availableActions`가 포함될 수 있습니다. 단, 실제 변경은 각 Controller/Service의 권한/정책 검사로 다시 보장합니다.
- GENERAL comment의 추가, 수정, 삭제는 이슈 history에 기록합니다. comment 자체의 created/updated 시간은 comment에서 관리하고, 이슈의 `updatedAt`은 상태/배정/우선순위/내용 변경 같은 이슈 변경 흐름에서 관리합니다.

### GitHub Project 작업 상태
GitHub Project에서 팀 작업을 관리할 때는 다음 흐름을 사용합니다.

`대기 -> 준비됨 -> 진행 중 -> 리뷰 중 -> 완료`

보조 규칙:
- 구현 전에는 가능한 한 GitHub 이슈를 생성합니다.
- PR이 열리면 Project 상태는 `리뷰 중`으로 이동합니다.
- PR이 머지되고 관련 이슈가 닫히면 `완료`로 이동합니다.

## 저장소 방식 (DB 기반 영속 저장소)
과제 원문은 File System 또는 DBMS 사용을 모두 허용하지만, 이 저장소의 표준 구현은 DB 기반 persistence로 고정합니다.

- 표준 실행과 통합 테스트는 Oracle DB와 JDBC repository 계층을 기준으로 합니다.
- Java 구현에서는 Repository interface를 서비스 계층의 표준 경계로 두고, JDBC 구현체는 persistence adapter로 둡니다.
- schema, seed 데이터, 예외 처리 정책은 DB 초기화와 Oracle 통합 테스트 문서에 맞춰 유지합니다.

## UI 툴킷
UI 구성은 다음과 같습니다.

- 기본 실행: JavaFX
- `--swing` 실행 인자: Swing
- 두 UI는 동일한 `ApplicationContext` 구성 방식을 사용해 Controller/Service/Repository/Domain 계층을 재사용합니다.
- JavaFX와 Swing을 각각 별도 앱으로 실행하는 경우 로그인 세션은 공유하지 않습니다.

혼합 사용 시 공통 도메인 모델과 서비스 계층은 UI 프레임워크와 분리합니다.

## 추천 방식
추천 기능은 현재 이슈의 title/description과 과거 해결 이력을 활용하는 KNN 기반 유사도 추천을 사용합니다.

UC5의 배정 후보 추천은 현재 Issue status와 과거 이력에 따라 반환 범위를 달리합니다. assignee 후보는 실제 수정 완료자인 fixer 이력을 우선 참고하고, verifier 후보는 실제 검증 완료자인 resolver 이력을 우선 참고합니다.

추천 대상은 해당 프로젝트의 active DEV/TESTER입니다. 과거 해결 이력이 없거나 유사도 추천 결과가 부족해도 전체 active 후보 목록은 함께 반환합니다.

## 통계 조회 범위 정책
UC10 Statistics는 사용자가 프로젝트를 선택한 뒤 진입한 프로젝트 화면에서 해당 프로젝트 기준으로 조회합니다.

- 조회 입력은 projectId와 일/월 단위 기간 범위입니다.
- 통계 조회는 해당 프로젝트의 active member만 수행할 수 있습니다.
- 통계 결과는 이슈 발생 수, 상태/우선순위 분포, 상태 변경 수, comment 수를 일/월 단위로 제공합니다.
- 현재 통계 쿼리는 deleted issue 관리 흐름과 분리하기 위해 `DELETED` 상태 이슈를 제외합니다.
- JavaFX와 Swing UI는 서로 다른 화면 구현을 제공할 수 있지만, 통계 조회 로직은 동일한 `StatisticsController`와 `StatisticsService`를 재사용합니다.

## 권한 가정
권한 모델은 role과 active project membership을 함께 사용합니다.

- 관리자(Admin): 계정 관리, 프로젝트 관리, 전체 dashboard 조회를 수행합니다.
- PL: 자신이 active member인 프로젝트에서 이슈 배정, 의존성 관리, 우선순위 변경, deleted issue 관리를 수행합니다.
- DEV: 자신이 active member인 프로젝트에서 이슈 등록, 조회, comment, 담당 이슈의 수정 완료 처리를 수행합니다.
- TESTER: 자신이 active member인 프로젝트에서 이슈 등록, 조회, comment, 검증 결과 반영을 수행합니다.
- inactive 사용자는 로그인과 프로젝트 사용 기능 수행을 허용하지 않습니다.