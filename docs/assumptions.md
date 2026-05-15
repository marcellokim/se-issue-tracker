# 기본 가정

이 문서는 텀 프로젝트 초기에 합의한 기본 가정을 정리합니다. 구현이 진행되면서 바뀌면 PR과 함께 갱신합니다.

## 상태 전이 정의
상태 이름은 두 층에서 분리해서 사용합니다.

### 애플리케이션 이슈 상태
과제 PDF의 최소 상태 목록과 팀 회의 확정 상태를 함께 반영합니다.

기본 흐름:

`new -> assigned -> fixed -> resolved -> closed`

보조 상태/흐름:
- `fixed`는 Dev가 수정 완료를 주장한 중간 상태입니다.
- Tester가 fixed 이슈를 검증 성공하면 `resolved`로 전이합니다.
- `fixed -> resolved` 전이에서는 resolver를 현재 Tester로 기록하고 assignee/verifier/fixer는 유지합니다.
- Tester가 fixed 이슈를 검증 실패하면 `assigned`로 되돌리고 실패 사유를 comment/history에 남깁니다.
- `reopened`는 resolved/closed 이슈를 PL이 다시 작업 대상으로 판단할 때 사용합니다. Reopen 후에는 PL이 UC5로 assignee/verifier를 지정해 `assigned` 상태부터 재작업을 시작합니다.
- `deleted`는 불필요한 이슈의 soft-delete 상태입니다. deleted 이슈가 30개를 초과하면 deleted 전이 시각 기준 FIFO로 오래된 이슈부터 물리 삭제합니다.
- deleted 전이 시각은 `IssueHistory(STATUS_CHANGED, newValue=DELETED).changedDate`에서 결정합니다. 별도 `Issue.deletedAt` 속성은 두지 않습니다.
- 만약 `deleted` 상태인 이슈에 대해 PL이 해당 이슈가 다시 필요하다고 여기면 해당 이슈가 물리적으로 삭제되기 전에 삭제 직전 상태(`NEW` 또는 `CLOSED`)로 복구해 물리적 삭제를 막을 수 있습니다.

### 상태 변경 이력과 자동 필드 동기화 정책
- `ASSIGNED -> FIXED` 전이에서 fixer가 설정되고, `FIXED -> RESOLVED` 전이에서 resolver가 설정됩니다.
- 두 전이는 각각 `IssueHistory(STATUS_CHANGED)`에 함께 기록되며, fixer/resolver는 해당 상태 전이 기록과 의미상 일치해야 합니다.

### deleted 상태 전이 세부 정책
- new->deleted, closed->deleted로의 전이만 가능합니다.
- deleted로의 상태 전이는 PL만 할 수 있는 권한 입니다.
- DEV, TESTER는 deleted 상태의 이슈를 볼 수 없습니다.
- 이슈의 상태가 deleted에서 삭제 직전 상태(`NEW` 또는 `CLOSED`)로 복구되면 DEV, TESTER는 권한 범위 안에서 해당 이슈를 다시 볼 수 있습니다.
- deleted 이슈는 PL만 볼 수 있는 bin이라는 별도의 페이지에 축적됩니다.
- deleted 이슈가 bin에 30개 초과일 때 FIFO 방식대로 물리적으로 삭제됩니다.
- PL은 bin에서 deleted 이슈를 restore할 수 있으며, 이 경우 복구 상태는 `NEW/CLOSED -> DELETED` 전이 때 기록된 `IssueHistory(STATUS_CHANGED).previousValue`에서 결정합니다.

### reopen 상태 전이 세부 정책
- resolved/closed 상태에서만 reopen 상태 전이가 가능합니다.
- reopen 상태 전이는 PL만 할 수 있는 고유 권한입니다.
- reopen 상태 전이가 발생해도 assignee와 verifier를 자동 복원하지 않습니다.
- reopen시 reporter 값은 변경할 수 없습니다.
- reopen시 기존 fixer와 resolver는 보존되어 PL에게 참고 정보로 제시될 수 있습니다.
- PL이 reopen 상태의 이슈에서 assignee와 verifier를 결정하면 해당 이슈는 assigned 상태로 전이됩니다.(reopen->assigned)
- resolved->closed 전이에서는 assignee와 verifier를 null로 제거하고 fixer와 resolver는 보존합니다.

### UC5 배정/배정 변경 정책
- UC5는 PL이 이슈의 assignee/verifier를 배정하거나 변경하는 사용자 목표입니다.
- UC5의 상태별 흐름은 `new->assigned`, `reopened->assigned`, `assigned->assigned(assignee 변경)`, `fixed->fixed(verifier 변경)`입니다.
- UC8 Recommend Assignment Candidates는 UC5에서 항상 include되며, 대상 Issue status에 따라 추천 후보 종류를 다르게 반환합니다.
- `new`와 `reopened`에서는 Dev assignee 후보와 Tester verifier 후보를 모두 추천합니다.
- `assigned`에서는 Dev assignee 후보만 추천합니다.
- `fixed`에서는 Tester verifier 후보만 추천합니다.

### 배정 정보 변경 이력 정책
- `assigned->assigned(assignee 변경)`과 `fixed->fixed(verifier 변경)`은 status 값이 바뀌지 않는 배정 정보 변경입니다.
- 이 변경은 상태 전이(`STATUS_CHANGED`)가 아니라 배정 변경 이력(`ASSIGNMENT_CHANGED`)으로 기록합니다.
- `ASSIGNMENT_CHANGED`를 사용하는 경우 도메인 모델의 `ActionType`에도 해당 값을 포함합니다.

### 이슈 종속성에 따른 상태 전이 영향
- 종속되는 이슈(blocked issue)를 해결하기 위해서는 종속하는 이슈(blocking issue)를 먼저 해결해야합니다
- 예를 들어, 두 이슈 A, B에 대해 B를 해결하기 위해서는 A가 B보다 먼저 해결되어야 합니다.(A가 blocking issue, B가 blocked issue)
- blocking issue가 resolved 또는 closed 상태가 아닌데 blocked issue가 resolved로 상태전이를 시도하면, 시스템상에서 이슈 종속성 관계 검사를 통해 blocked issue의 resolved로의 상태전이를 차단합니다.
- 별도 `BLOCK`/`BLOCKED` 이슈 상태는 도입하지 않고, dependency는 `FIXED -> RESOLVED` 전이에 대한 guard로만 사용합니다.

### deleted 상태 전이 세부 정책 
- new->delted, closed->deleted로의 전이만 가능합니다.
- deleted로의 상태 전이는 PL만 할 수 있는 권한 입니다.
- DEV, TESTER는 delted 상태의 이슈를 볼 수 없습니다.
- 이슈의 상태가 delted->closed로 상태 전이가 되면 DEV, TESTER는 해당 이슈를 다시 볼 수 있습니다.
- deleted 이슈는 PL만 볼 수 있는 bin이라는 별도의 페이지에 축적됩니다.
- deleted 이슈가 bin에 30개 초과일 때 FIFO 방식대로 물리적으로 삭제됩니다.
- PL은 bin에서 deleted 이슈를 restore할 수 있으며, 이 경우에 delted 이슈의 상태는 closed로 전이됩니다.

### reopen 상태 전이 세부 정책 
- resolved/closed 상태에서만 reopen 상태 전이가 가능합니다. 
- reopen 상태 전이는 PL만 할 수 있는 고유 권한입니다.
- reopen 상태 전이가 발생하면 해당 이슈의 히스토리 이력을 통해 reporter는 초기 이슈 생성자, assignee와 verifier는 각각 가장 최근 DEV, TESTER값으로 자동 배정됩니다.
- reopen시 reporter 값은 변경할 수 없습니다 
- reopen시 assignee와 verifier는 해당 이슈의 히스토리를 기반으로 최신값들로 자동 설정되지만 PL이 원하는 assignee와 verifier를 지정할 수도 있습니다. 
- PL이 reopen 상태의 이슈에서 assignee와 verifier를 재결정하면 해당 이슈는 assigned 상태로 전이됩니다.(reopen->assigned)  

### 이슈 종속성에 따른 상태 전이 영향 
- 종속되는 이슈(blocked issue)를 해결하기 위해서는 종속하는 이슈(blocking issue)를 먼저 해결해야합니다 
- 예를 들어, 두 이슈 A, B에 대해 B를 해결하기 위해서는 A가 B보다 먼저 해결되어야 합니다.(A가 blocking issue, B가 blcoked issue) 
- blocking issue가 resolved되지 않은 상태임에도 blocked issue가 resolved로 상태전이를 시도하면, 시스템상에서 이슈 종속성 관계 검사를 통해 blocked issue의 resolved로의 상태전이를 차단합니다. 

### 권한 및 수정 정책
- User당 직군/역할은 하나만 부여합니다.
- Reporter는 assigned 전까지만 자신의 이슈 title/description을 수정할 수 있습니다.
- assigned 이후 title/description 정정과 추가 정보는 comment로 남깁니다.
- Priority는 PL만 변경할 수 있으며, assigned 상태와 무관하게 변경 가능합니다.
- 이슈 dependency 관계는 comment가 아니라 구조화된 데이터로 관리합니다.

### GitHub Project 작업 상태
GitHub Project에서 팀 작업을 관리할 때는 다음 흐름을 사용합니다.

`대기 -> 준비됨 -> 진행 중 -> 리뷰 중 -> 완료`

보조 규칙:
- 구현 전에는 가능한 한 GitHub 이슈를 생성합니다.
- PR이 열리면 Project 상태는 `리뷰 중`으로 이동합니다.
- PR이 머지되고 관련 이슈가 닫히면 `완료`로 이동합니다.

## 저장소 방식 (DB 기반 영속 저장소)
과제 원문은 File System 또는 DBMS 사용을 모두 허용하지만, 이 저장소의 표준 구현은 DB 기반 persistence로 고정합니다.

- 외부 서버가 필요 없는 내장형 DB를 우선합니다.
- Java 구현에서는 JDBC repository 계층을 표준 경계로 둡니다.
- schema, seed 데이터, 예외 처리 정책은 #18 구현과 함께 문서화합니다.

## UI 툴킷
초기 가정 UI 구성은 다음과 같습니다.

- 사용자용 메인 UI: JavaFX
- 관리자/보조 도구 UI: Swing

혼합 사용 시 공통 도메인 모델과 서비스 계층은 UI 프레임워크와 분리합니다.

## 추천 방식 (휴리스틱)
추천 기능은 초기에는 휴리스틱 기반으로 구현한다고 가정합니다.

UC5의 배정 후보 추천은 현재 Issue status와 과거 이력에 따라 반환 범위를 달리합니다. assignee 후보는 실제 수정 완료자인 fixer 이력을 우선 참고하고, verifier 후보는 실제 검증 완료자인 resolver 이력을 우선 참고합니다.

예시 기준:
- 우선순위
- 최근 활동 여부
- 상태
- 담당 가능성 또는 관련 태그

초기 단계에서는 설명 가능한 규칙 기반 접근을 우선하고, 추후 필요 시 개선합니다.

## 통계 조회 범위 정책
UC10 Statistics의 조회 범위는 사용자가 임의의 role 값을 top-level parameter로 넘기는 방식이 아니라, 시스템의 currentProject/currentUserRole context와 filters.scope에 따라 결정합니다.

- filters는 기간, 상태, 담당자 등 통계 조건을 표현한다.
- filters.scope는 전체 이슈, 내 관련 이슈, 프로젝트 범위, 직군별 범위 등 조회 범위를 표현한다.
- currentProject와 currentUserRole은 인증 및 현재 프로젝트 context에서 결정한다.

## 권한 가정
초기 권한 모델은 단순화합니다.

- 일반 사용자(PL, TESTER, DEV)
- 관리자(Admin)

필요 시 아래 항목을 확장합니다.
- 생성/수정/삭제 권한
- 상태 변경 권한
- 관리자 전용 화면 접근 권한
