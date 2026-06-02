# 최종 보고서 본문 초안

이 문서는 최종 PDF 편집 전 본문 초안을 정리한 것이다. 표지, 팀원 학번, 제출일, 실제 화면 캡처, 렌더링 이미지 배치, 페이지 번호와 최종 편집 양식은 별도 제출본에서 정리한다.

## A. 프로젝트 개요

### 1. 프로젝트 목적

본 프로젝트는 팀 단위 소프트웨어 개발 과정에서 발생하는 이슈를 체계적으로 등록, 배정, 처리, 검증, 종료하기 위한 이슈 관리 시스템을 Java로 구현하는 것을 목표로 한다. 사용자는 프로젝트별로 이슈를 등록하고, 담당자를 배정하며, 상태 변경과 댓글, 변경 이력, 의존성 정보를 함께 확인할 수 있다.

이 시스템은 이슈를 단순 목록으로 저장하는 데서 끝나지 않고, 이슈가 등록된 이후 배정, 수정, 검증, 종료 또는 재오픈되는 전체 흐름을 관리하는 데 초점을 둔다. 이를 위해 이슈 상태, 담당자, 댓글, 변경 이력, 의존성, 삭제 이슈 보관 정책, 통계, 배정 후보 추천 기능을 함께 설계하였다.

### 2. 주요 기능 요약

| 기능 영역 | 주요 내용 |
| --- | --- |
| 계정 관리 | Admin이 사용자 계정 생성, 이름 변경, 역할 변경, 활성/비활성 처리를 수행 |
| 프로젝트 관리 | Admin이 프로젝트 생성, 이름/설명 수정, 삭제, 참여자 추가/제거를 수행 |
| 이슈 등록/검색/상세 조회 | 프로젝트 active member가 일반 이슈를 등록하고 조건별로 검색하며 상세 정보를 조회 |
| 댓글과 이력 | 일반 댓글과 상태 변경 사유 댓글을 구분하고, 주요 변경은 IssueHistory에 기록 |
| 배정과 추천 | PL이 상태별로 assignee Dev와 verifier Tester를 배정하고, 과거 해결 이력 기반 추천 후보를 참고 |
| 상태 전이 | NEW, ASSIGNED, FIXED, RESOLVED, CLOSED, REOPENED, DELETED 상태 흐름 관리 |
| 의존성 관리 | blocking issue와 blocked issue 관계를 관리하고, unresolved dependency가 있는 RESOLVED 전이를 차단 |
| 삭제 이슈 관리 | PL 전용 soft delete, restore, 단건 purge, FIFO 보관 한도 정리 |
| 통계 | 프로젝트 기준 일/월별 이슈 발생, 상태 변경, 댓글, 상태/우선순위 분포 조회 |
| 다중 UI | JavaFX 전체 UI와 Swing 전체 UI가 같은 Controller, Service, Domain, Repository 계층을 재사용 |
| 테스트와 협업 | JUnit, Oracle 통합 테스트, GitHub Actions, Issue/PR/Project 기반 협업 흐름 관리 |

### 3. 구현 범위 및 제한사항

본 프로젝트는 JavaFX와 Swing 두 가지 UI Toolkit을 사용하여 각각 독립적으로 실행 가능한 전체 UI를 구현하였다. 두 UI는 화면 구현 방식만 다르고, Controller, Service, Domain, Repository, JDBC 계층은 공통으로 재사용한다. 이는 UI를 제외한 핵심 업무 로직과 저장소 코드가 거의 수정 없이 재사용될 수 있음을 보이기 위한 설계이다.

영속 저장소는 Oracle DB 기반으로 구현하였다. 과제 요구사항은 파일 시스템 또는 DBMS persistence를 허용하지만, 본 프로젝트는 실제 이슈 관리 시스템의 데이터 일관성, 검색, 이력 관리, 통합 테스트를 고려하여 DB 기반 persistence를 표준으로 정하였다.

Assignment 추천 기능은 프로젝트 내 active 후보와 과거 이슈 처리 이력을 기반으로 계산한다. 외부 학습 모델과의 연동은 이번 구현 범위에 포함하지 않고, 추후 확장 가능 지점으로 남겼다.

## B. 프로젝트 가정사항 및 정책

### 1. 사용자 역할

시스템 사용자는 Admin, PL, Dev, Tester로 구분한다. 한 사용자는 하나의 역할만 가진다고 가정하였다. 역할을 단순하게 유지한 이유는 권한 판단을 명확하게 하고, 프로젝트와 이슈 처리 흐름을 불필요하게 복잡하게 만들지 않기 위해서이다.

Admin은 계정과 프로젝트를 관리하는 운영자 역할이다. 계정 생성, 역할 변경, 계정 활성화/비활성화, 프로젝트 생성/수정/삭제, 프로젝트 참여자 관리를 수행하지만 일반 이슈 처리 흐름에는 직접 참여하지 않는다. 따라서 Admin 전용 프로젝트 화면에는 일반 이슈 목록을 표시하지 않는다.

PL은 프로젝트의 이슈 흐름을 관리한다. 자신이 active member로 참여한 프로젝트에서 이슈 배정, 재배정, 우선순위 변경, 의존성 관리, 삭제 이슈 관리, 종료 및 재오픈을 수행한다.

Dev는 자신이 assignee로 배정된 이슈를 수정하는 역할이다. 수정 완료 후 이슈를 FIXED 상태로 변경하여 Tester가 검증할 수 있도록 한다. 또한 자신이 참여한 프로젝트 안에서 이슈 등록, 조회, 댓글 작성이 가능하다.

Tester는 수정 완료된 이슈를 검증한다. verifier로 배정된 이슈가 FIXED 상태가 되면 검증을 수행하고, 결과에 따라 RESOLVED로 전이하거나 다시 ASSIGNED로 되돌린다.

assignee/verifier와 fixer/resolver는 구분한다. assignee와 verifier는 현재 작업 담당자이고, fixer와 resolver는 과거에 실제 수정 완료와 검증 완료를 수행한 사용자이다. 이슈가 RESOLVED 상태가 되면 현재 작업 배정은 끝난 것으로 보고 assignee와 verifier를 해제하며, fixer와 resolver는 이력성 정보로 보존한다.

비활성 사용자는 로그인할 수 없고 프로젝트 조회나 이슈 처리 기능도 사용할 수 없다. 사용자 기록과 이슈 이력을 보존하기 위해 계정은 물리 삭제하지 않고 active 상태값으로 관리한다.

### 2. 프로젝트 접근 정책

Admin은 전체 프로젝트 요약과 전체 사용자 목록을 볼 수 있고, 특정 프로젝트에서는 프로젝트 기본 정보와 참여자 정보를 확인할 수 있다. 다만 Admin은 일반 이슈 처리 actor가 아니므로 프로젝트 상세에서 이슈 목록을 보지 않는다.

PL, Dev, Tester는 자신이 active member로 참여한 프로젝트만 볼 수 있다. 프로젝트에 들어가면 DELETED 상태가 아닌 일반 이슈 전체 목록을 확인할 수 있고, keyword, status, priority, reporter, assignee, verifier, 날짜 범위 등의 검색 조건으로 이슈를 찾을 수 있다. DELETED 상태의 이슈는 일반 프로젝트 화면과 분리하여 PL 전용 deleted issue 관리 화면에서만 조회한다.

### 3. 이슈 상태 전이

기본 이슈 상태 흐름은 다음과 같다.

```text
NEW -> ASSIGNED -> FIXED -> RESOLVED -> CLOSED
```

NEW는 이슈가 새로 등록된 상태이고, ASSIGNED는 Dev와 Tester가 배정된 상태이다. FIXED는 Dev가 수정 완료를 표시한 상태이며, RESOLVED는 Tester가 검증을 완료한 상태이다. CLOSED는 PL이 최종 종료한 상태이다.

보조 흐름으로는 검증 실패와 재작업이 있다. Tester가 FIXED 상태의 이슈를 검증했지만 수정이 충분하지 않다고 판단하면 FIXED -> ASSIGNED 전이를 수행한다. PL이 RESOLVED 또는 CLOSED 상태의 이슈에 재작업이 필요하다고 판단하면 RESOLVED/CLOSED -> REOPENED 전이를 수행한다. REOPENED 상태의 이슈는 assignee와 verifier를 자동 복원하지 않고, PL이 다시 배정해야 ASSIGNED 상태부터 재작업을 시작한다.

삭제 흐름은 soft delete로 처리한다. NEW 또는 CLOSED 상태의 이슈만 DELETED 상태로 전환할 수 있으며, 이 전이는 PL만 수행할 수 있다. DELETED 이슈는 일반 조회와 검색에서 제외되고, PL이 삭제 이슈 관리 화면에서 복구하거나 물리 삭제할 수 있다.

### 4. 이슈 배정 정책

이슈 배정은 프로젝트의 active PL만 수행할 수 있다. NEW, REOPENED 상태에서는 assignee Dev와 verifier Tester를 함께 지정하고, 배정이 완료되면 이슈는 ASSIGNED 상태가 된다. ASSIGNED 상태에서는 assignee Dev만 변경할 수 있고, FIXED 상태에서는 verifier Tester만 변경할 수 있다.

배정 또는 배정 변경이 발생하면 ASSIGNMENT_CHANGED 이력을 남긴다. NEW -> ASSIGNED, REOPENED -> ASSIGNED처럼 상태도 함께 바뀌는 경우에는 STATUS_CHANGED 이력도 함께 기록한다.

배정 화면은 추천 후보 Top 3와 전체 active Dev/Tester 후보 목록을 함께 제공한다. 추천 후보는 현재 이슈의 title, description과 과거 fixer/resolver 이력을 참고하여 계산한다. 추천 데이터가 부족해도 전체 active 후보 목록은 함께 제공하여 PL이 직접 담당자를 선택할 수 있도록 한다.

### 5. 댓글과 이력

댓글은 일반 댓글과 상태 변경 사유 댓글로 구분한다. 일반 댓글은 이슈에 의견이나 추가 설명을 남기기 위한 것이고, 상태 변경 사유 댓글은 상태 전이의 근거를 남기기 위한 것이다.

일반 댓글은 대상 프로젝트에 접근 권한이 있는 사용자가 작성할 수 있고, 수정과 삭제는 작성자 본인만 할 수 있다. 상태 변경 사유 댓글은 상태 전이 근거이므로 수정과 삭제를 허용하지 않는다.

이슈 생성, 상태 변경, 배정 변경, 우선순위 변경, 제목/내용 수정, 댓글 변경, 의존성 변경은 IssueHistory에 기록한다. 댓글 작성/수정 시간은 Comment가 관리하고, Issue의 updatedDate는 이슈 자체 속성이 변경되는 경우를 기준으로 갱신한다.

### 6. 이슈 의존성

이슈 간에는 blocking issue와 blocked issue 관계가 존재할 수 있다. blocked issue를 RESOLVED로 변경하려면 해당 이슈를 막고 있는 blocking issue가 먼저 RESOLVED 또는 CLOSED 상태가 되어야 한다.

의존성은 별도의 BLOCKED 상태가 아니라 FIXED -> RESOLVED 전이에서 확인하는 guard로 설계하였다. 이 방식은 상태 모델을 단순하게 유지하면서도 선행 해결 조건을 구조화된 데이터로 관리할 수 있게 한다.

이슈가 DELETED 상태로 전이되면 해당 이슈가 포함된 의존성 관계는 함께 제거한다. 삭제된 이슈가 다른 이슈의 상태 전이에 계속 영향을 주지 않도록 하기 위한 정책이다.

### 7. 삭제 이슈 관리

삭제는 soft delete 방식이다. PL이 이슈 삭제를 수행하면 DB에서 바로 제거하지 않고 먼저 DELETED 상태로 전환한다. 삭제 가능한 상태는 NEW와 CLOSED로 제한한다. NEW는 처리 흐름이 시작되지 않은 이슈이고, CLOSED는 이미 처리가 끝난 이슈이므로 삭제 상태로 격리해도 진행 중인 작업 흐름을 크게 깨뜨리지 않는다.

soft delete와 restore에는 사유 댓글이 필요하다. restore는 삭제 직전 상태인 NEW 또는 CLOSED로 되돌린다. 삭제 당시 제거된 의존성 관계는 자동 복원하지 않는다.

프로젝트별 deleted issue 보관 한도는 30개이다. soft delete 이후 한도를 초과하면 오래된 deleted issue부터 FIFO 방식으로 물리 삭제한다. PL은 특정 deleted issue를 단건으로 물리 삭제할 수도 있다.

### 8. 통계

통계는 프로젝트 단위로 조회한다. 프로젝트 접근 권한을 가진 active member는 해당 프로젝트의 통계를 볼 수 있다. 통계 범위는 전체 시스템 기준이 아니라 사용자가 접근할 수 있는 프로젝트 범위이다.

통계는 일 단위와 월 단위로 제공한다. 주요 항목은 이슈 발생 수, 이슈 상태 변경 수, 댓글 수, 이슈 상태 분포, 이슈 우선순위 분포이다. 일반 이슈 흐름과 삭제 이슈 관리 흐름을 분리하기 위해 DELETED 상태 이슈는 통계에서 제외한다.

### 9. UI 및 세션

본 프로젝트는 JavaFX와 Swing 두 가지 UI Toolkit을 사용한다. 두 UI는 화면 구현만 다르고 Controller, Service, Domain, Repository, JDBC 계층은 공통으로 재사용한다.

JavaFX와 Swing은 각각 별도 애플리케이션으로 실행된다. 로그인 세션은 애플리케이션 실행 흐름 안에서 관리하며, 팀 데모 가정은 한 번에 하나의 UI 애플리케이션을 실행하는 것이다.

## C. 요구 정의 및 분석

요구 정의는 기능 목록을 나열하는 방식이 아니라 사용자가 시스템 안에서 실제로 수행하는 업무 흐름을 기준으로 정리하였다. 핵심 흐름은 이슈 등록, 검색, 상세 조회, 배정, 상태 변경, 검증, 종료이다.

유스케이스 다이어그램에서는 Actor를 Admin, PL, Dev, Tester로 구분하였다. Admin은 계정과 프로젝트를 관리하는 운영자이며, PL/Dev/Tester는 일반 이슈 처리 흐름에 참여하는 사용자이다.

핵심 유스케이스는 UC1 Register Issue, UC2 Add Comment, UC3 Search Issues, UC4 View Issue Detail, UC5 Assign/Update Issue Assignment, UC6 Change Issue State로 선정하였다. 이 6개는 이슈 생명주기의 중심 흐름을 대표한다. UC7 Manage Dependency, UC8 Recommend Assignment Candidates, UC9 Manage Deleted Issue, UC10 Statistics, UC11 Log In, UC12 Manage Accounts, UC13 Manage Projects, UC14 Verify Permission, UC15 Edit Issue, UC16 Change Priority는 핵심 흐름을 보조하거나 관리 기능을 담당한다.

include 관계는 여러 유스케이스에서 반드시 수행되거나 반복적으로 필요한 흐름을 분리하기 위해 사용하였다. UC5는 배정 후보 추천을 위해 UC8을 include하고, UC6과 UC9는 상태 변경 또는 삭제/복구 사유 댓글이 필요하므로 UC2를 include한다. 또한 보호된 기능들은 UC14 Verify Permission을 include한다.

extend 관계는 기본 유스케이스가 독립적으로 수행될 수 있지만 특정 상황에서 선택적으로 추가되는 흐름을 표현하기 위해 사용하였다. UC2 Add Comment는 이슈 등록 직후, 이슈 상세 화면, 배정 과정에서 선택적으로 수행될 수 있으므로 UC1, UC4, UC5를 extend한다. UC15 Edit Issue는 사용자가 이슈 상세 화면에서 자신이 등록한 수정 가능한 이슈를 수정하는 경우에만 수행되므로 UC4를 extend한다.

### 대표 유스케이스 요약

| UC | 요약 |
| --- | --- |
| UC1 Register Issue | 프로젝트 active member가 title, description, priority로 이슈를 등록하고 reporter, reportedDate, NEW 상태가 기록됨 |
| UC2 Add Comment | 권한 있는 사용자가 일반 댓글 또는 상태 변경 사유 댓글을 추가함 |
| UC3 Search Issues | 프로젝트 일반 이슈를 조건별로 검색하고 DELETED 이슈는 제외함 |
| UC4 View Issue Detail | 이슈 기본 정보, 담당자, 댓글, 이력, 의존성, 가능한 액션을 조회함 |
| UC5 Assign/Update Issue Assignment | PL이 상태별로 assignee/verifier를 배정하거나 변경함 |
| UC6 Change Issue State | 역할과 상태 조건에 맞게 상태 전이를 수행하고 사유 댓글과 이력을 남김 |

### 도메인 모델

도메인 모델은 업무에서 중요한 개념과 관계를 중심으로 작성하였다. 핵심 개념은 User, Project, ProjectMember, Issue, Comment, IssueHistory, IssueDependency이다.

User는 시스템 사용자를 나타낸다. 사용자는 Role과 active 상태를 가진다. Project는 이슈가 등록되고 관리되는 단위이며, 사용자의 이슈 접근 권한도 프로젝트 참여 여부를 기준으로 판단한다.

ProjectMember는 사용자가 프로젝트에 참여한다는 사실을 나타내는 관계 객체이다. 단순한 다대다 관계로만 보지 않고 별도 개념으로 둔 이유는 프로젝트 참여 여부가 권한 판단의 핵심 기준이기 때문이다.

Issue는 시스템의 핵심 개념이다. Issue는 title, description, priority, status를 가지며, reporter, assignee, verifier, fixer, resolver와 관계를 가진다. Comment는 이슈에 남기는 의견 또는 처리 기록이고, IssueHistory는 이슈에서 발생한 주요 변경을 추적한다. IssueDependency는 한 이슈가 다른 이슈의 해결을 막는 관계를 나타낸다.

통계 결과, 대시보드 요약, 추천 결과는 실제 구현에서는 DTO나 read model로 존재하지만, 분석 단계의 도메인 모델에서는 제외하였다. 이들은 핵심 업무 개념이라기보다 화면 표시나 계산 결과에 가깝기 때문이다.

### SSD와 Operation Contract

SSD는 외부 actor가 시스템에 보내는 system operation을 블랙박스 관점에서 표현한다. 보고서 본문에서는 대표 SSD로 Register Issue, Assign/Update Issue Assignment, Mark Fixed를 사용한다.

Operation Contract는 SSD만으로 충분히 드러나지 않는 system operation 수행 후의 상태 변화를 설명한다. 대표 Operation Contract는 registerIssue, resolve fixed issue, add dependency를 중심으로 정리한다. registerIssue는 Issue 생성과 reporter/project/history 연결을 보여 주고, resolve fixed issue는 resolver 기록, assignee/verifier 해제, 상태 변경 사유 댓글, dependency guard를 함께 설명한다. add dependency는 이후 상태 전이에 영향을 주는 관계 생성이라는 점을 보여 준다.

## D. 설계

### 1. 전체 설계 구조

본 프로젝트는 UI와 업무 로직을 분리하기 위해 MVC 스타일과 계층 분리를 함께 적용하였다. JavaFX와 Swing은 서로 다른 UI Toolkit이지만, UI 이후에는 동일한 Controller, Service, Domain, Repository, JDBC 계층을 사용한다.

```text
JavaFX / Swing UI
  -> Controller
  -> Service / Policy
  -> Domain + Repository Port + Service Port
  -> JDBC Adapter / Technical Adapter
  -> Oracle Database
```

Controller는 UI에서 들어온 요청을 받아 현재 로그인한 사용자를 확인하고 Service로 전달한다. Service는 유스케이스 흐름을 조율하며 권한 검사, 정책 검사, Repository 호출, Domain 객체 호출을 함께 관리한다. Domain은 Issue, User, Project처럼 핵심 개념과 상태 전이, 배정, 댓글, 이력, 의존성 같은 업무 규칙을 담당한다. Repository는 Service가 DB에 직접 의존하지 않도록 하는 interface이며, JDBC 구현체는 Oracle DB 접근을 담당한다.

이 구조의 핵심은 UI 변경이 이슈 처리 정책이나 도메인 규칙에 영향을 주지 않도록 하는 것이다. JavaFX와 Swing은 화면 구성과 이벤트 처리 방식은 다르지만, 이슈 등록, 배정, 상태 변경, 삭제 이슈 관리 같은 요청은 동일한 Controller와 Service를 거쳐 처리된다.

### 2. Sequence Diagram

Sequence Diagram은 SSD와 Operation Contract에서 정의한 system operation을 내부 객체 협력으로 확장한 설계 산출물이다. 본 프로젝트의 SD는 실제 코드의 모든 메서드 호출을 그대로 옮기기보다, 설계 단계에서 중요한 객체 간 메시지와 책임 배분을 중심으로 작성하였다.

대표 SD는 Register Issue와 Resolve Fixed Issue이다. Register Issue는 새 Issue가 생성되고 Project, reporter, IssueHistory와 연결되는 흐름을 보여 준다. Resolve Fixed Issue는 FIXED 상태의 이슈가 RESOLVED로 전이되는 과정에서 권한 확인, 상태 변경 사유, dependency guard, resolver 기록, active assignment 해제가 함께 처리되는 흐름을 보여 준다.

### 3. Design Class Diagram

DCD는 Domain Model을 설계 클래스 수준으로 확장한 산출물이다. 중심 클래스는 Issue이다. Issue는 단순 데이터가 아니라 등록 이후 배정, 수정, 검증, 종료, 재오픈, 삭제 흐름을 거치는 핵심 객체이다. 따라서 Issue에는 상태 변경, 배정 변경, 댓글과 이력 생성, 의존성 추가/제거에 관련된 operation이 배치되어 있다.

User는 시스템 사용자를 나타내며 Role과 active 상태를 가진다. Project는 이슈 관리 단위이고, ProjectMember는 사용자와 프로젝트의 참여 관계를 나타낸다. Comment와 IssueHistory는 이슈 처리 기록을 담당하며, IssueDependency는 이슈 간 선후 관계를 나타낸다.

### 4. GRASP 적용

GRASP Controller는 외부 system event를 받아 유스케이스 흐름을 시작하는 객체로 적용하였다. `AuthenticationController`, `IssueController`, `AssignmentController`, `IssueStateController`, `ProjectController` 등이 이 역할을 수행한다.

Information Expert는 필요한 정보를 가장 많이 가진 객체에게 책임을 주는 원칙이다. Issue는 자신의 상태, assignee, verifier, fixer, resolver, priority, comment, history를 알고 있으므로 상태 전이와 배정 변경의 핵심 책임을 가진다.

Creator 원칙은 Comment, IssueHistory, IssueDependency처럼 Issue에 종속되는 기록 또는 관계 객체 생성 흐름에 적용하였다. 댓글 추가, 상태 변경, 의존성 변경이 발생하면 Issue 흐름 안에서 관련 이력과 관계가 함께 생성된다.

Low Coupling과 High Cohesion은 계층 분리를 통해 반영하였다. Service는 JDBC 구현체가 아니라 Repository interface에 의존하고, Domain은 DB 접근을 알지 않는다. Technical 기능인 password hashing, session, clock, id generation도 port와 adapter로 분리하였다.

Protected Variations는 변경 가능성이 높은 부분을 interface로 감싼 설계에서 반영된다. DB 접근은 Repository interface 뒤에 JDBC 구현체를 두었고, 비밀번호 해싱, 현재 시각, 세션, id 생성도 port로 분리하였다.

Pure Fabrication은 현실 세계의 도메인 객체는 아니지만 설계를 정리하기 위해 필요한 객체에 적용하였다. `PermissionPolicy`, `AssignmentRecommendationService`, `DashboardSummaryService`, `StatisticsService`, `ApplicationBootstrap`이 대표적이다.

### 5. 주요 정책의 설계 반영

권한 정책은 `PermissionPolicy`와 각 Service의 프로젝트 멤버십 검사를 통해 반영하였다. 상태 전이 정책은 `IssueStateService`와 `Issue`를 중심으로 배치하였다. 삭제 이슈 관리는 일반 이슈 흐름과 성격이 다르므로 `DeletedIssueService`와 `DeletedIssueRepository`로 분리하였다.

의존성 정책은 `IssueDependency`와 상태 전이 guard로 반영하였다. 의존성은 별도 IssueStatus가 아니라, blocked issue가 RESOLVED로 전이될 때 확인하는 제약 조건이다.

Assignment 추천 기능은 추천 데이터 조회와 추천 계산 책임을 분리하였다. 과거 해결 이력과 active 후보 조회는 repository가 담당하고, 후보 계산은 추천 로직이 담당한다. Service는 이를 조합하여 PL에게 추천 후보와 전체 후보 목록을 제공한다.

## E. 구현 결과

### 1. 실행 구조

기본 실행은 JavaFX UI이며, Swing UI는 별도 Gradle task로 실행한다.

```bash
./gradlew run --console=plain
./gradlew runSwing --console=plain
```

Oracle DB를 사용하는 데모 실행은 로컬 Oracle runbook에 따라 DB를 시작하고 fixed seed를 초기화한 뒤 수행한다.

### 2. 주요 화면과 기능

최종 보고서 편집 단계에서는 아래 기능별 화면 캡처를 배치한다.

| 기능 | JavaFX/Swing 구현 설명 |
| --- | --- |
| 로그인 | active 계정만 로그인 가능하며 실패 메시지를 표시 |
| 대시보드 | Admin은 전체 프로젝트/사용자 요약, 일반 사용자는 참여 프로젝트 목록 조회 |
| 프로젝트 관리 | Admin이 프로젝트 생성/수정/삭제와 참여자 관리를 수행 |
| 계정 관리 | Admin이 계정 생성, 이름 변경, 역할 변경, 활성/비활성 처리 |
| 이슈 목록/검색 | 프로젝트 일반 이슈를 조회하고 keyword/status/priority 등으로 검색 |
| 이슈 상세 | 기본 정보, 담당자, 댓글, 이력, 의존성, 가능한 action 표시 |
| 이슈 배정 | PL이 상태별로 assignee/verifier를 배정하고 추천 후보를 참고 |
| 상태 변경 | Dev/Tester/PL의 역할별 상태 전이와 사유 댓글 입력 |
| 댓글 | 일반 댓글 추가/수정/삭제와 상태 변경 사유 댓글 구분 |
| 의존성 | PL이 dependency를 추가/제거하고 resolved guard에 활용 |
| 삭제 이슈 관리 | PL 전용 deleted issue 조회, restore, purge |
| 통계 | 프로젝트 기준 상태/우선순위 분포와 일/월별 추이 조회 |

### 3. Assignment 추천 기능

추천 기능은 PL이 이슈를 배정할 때 의사결정을 돕기 위한 기능이다. 추천은 자동 배정이 아니라 후보 ranking을 제공하는 보조 기능으로 설계하였다.

추천 후보는 현재 이슈의 title, description과 과거 해결 이력의 유사도를 기반으로 계산한다. resolved/closed 이슈에서 fixer/resolver로 참여한 이력과 프로젝트 active 후보를 함께 고려한다. 데이터가 부족한 경우에도 전체 active 후보 목록을 제공하여 PL이 직접 배정할 수 있도록 한다.

## F. JUnit 테스트 수행 내역

테스트는 Domain, Service, Controller, Repository/JDBC, UI, Architecture boundary로 나누어 구성하였다.

| 테스트 범위 | 목적 |
| --- | --- |
| Domain 테스트 | Issue 상태 전이, 댓글, 이력, 의존성, 값 객체 규칙 검증 |
| Service 테스트 | 권한, 프로젝트 멤버십, 유스케이스 흐름, repository 조합 검증 |
| Controller 테스트 | 현재 사용자 경계와 service 호출 계약 검증 |
| Repository/JDBC 테스트 | Oracle schema, seed, row mapping, SQL 조회/저장 검증 |
| UI 테스트 | Swing/JavaFX 주요 panel, presenter, dialog의 렌더링과 interaction 계약 검증 |
| Architecture 테스트 | package dependency, repository convention, public attribution, 제출 메타데이터 검증 |

최종 제출 전 대표 검증 명령은 다음과 같다.

```bash
./gradlew check --console=plain
./gradlew verifySubmissionMetadata --console=plain
./gradlew oracleLocalTest --console=plain
```

Oracle local 실행이 어려운 환경에서는 GitHub Actions의 Oracle 통합 테스트 결과와 artifact를 검증 근거로 사용한다.

## G. GitHub 프로젝트 활용 요약

본 프로젝트는 GitHub Issue, Pull Request, Project board, Milestone을 사용하여 작업을 관리하였다. 일반 작업 흐름은 이슈 생성, 브랜치 생성, 작업/검증/커밋, PR 생성, 리뷰, dev 병합 순서로 진행하였다.

브랜치는 `feat/<issue>-<slug>`, `fix/<issue>-<slug>`, `docs/<issue>-<slug>`, `test/<issue>-<slug>`, `ci/<issue>-<slug>`, `chore/<issue>-<slug>`, `refactor/<issue>-<slug>` 형식을 사용하였다. PR은 기본적으로 dev를 대상으로 올리고, main은 최종 release 동기화 용도로 사용하였다.

GitHub Actions는 build/test, Oracle integration, SonarCloud, CodeQL/security, workflow guard, project metadata consistency를 검증한다. Project board는 이슈 상태 라벨과 연결되어 대기, 준비됨, 진행 중, 리뷰 중, 완료 상태를 추적한다.

최종 보고서에는 Project board, milestone, 대표 PR, CI 결과 화면을 캡처하여 협업 및 검증 근거로 포함한다.

## H. 한계 및 향후 보완

첫째, Assignment 추천 기능은 과거 이력과 유사도 기반 후보 추천으로 구현하였지만 외부 학습 모델이나 대규모 데이터 기반 평가까지는 포함하지 않았다. 추후에는 실제 프로젝트 데이터가 쌓였을 때 추천 정확도를 Precision@K 같은 기준으로 평가하고 개선할 수 있다.

둘째, 통계 화면은 프로젝트 기준 주요 집계를 제공하지만 시각화 표현은 더 개선할 수 있다. 현재는 과제 요구사항을 만족하는 데 필요한 상태/우선순위/일월별 추이를 중심으로 구현하였다.

셋째, JavaFX와 Swing은 같은 backend 계층을 재사용하도록 구현되었지만, 최종 제출 전에는 발표 장비에서 실제 마우스 포커스, 화면 크기, 주요 workflow의 시각적 품질을 다시 확인해야 한다.

넷째, 최종 보고서와 발표자료는 저장소 문서를 그대로 붙이는 것이 아니라 대표 산출물을 선별하여 60페이지 미만의 제출 문서로 편집해야 한다. 저장소의 `docs/` 문서는 원천 자료이고, 최종 PDF는 요구사항, 설계, 구현, 검증, 협업 근거를 압축하여 구성한다.

## 최종 편집 시 삽입할 증빙 목록

| 위치 | 필요한 증빙 |
| --- | --- |
| 프로젝트 개요 | 저장소 URL, Project URL, 실행 환경 |
| 요구사항 분석 | UCD, UC coverage 표, 대표 UC 명세 |
| 도메인 분석 | Domain Model 렌더 이미지 |
| 시스템 동작 분석 | 대표 SSD 3개, Operation Contract 요약 |
| 설계 구조 | Logical Architecture, DCD, 대표 SD, GRASP 설명 |
| 구현 결과 | JavaFX/Swing 같은 기능 비교 캡처 |
| 테스트와 검증 | `./gradlew check`, Oracle integration, SonarCloud, CodeQL 결과 |
| 협업 | Project board, milestone, 대표 PR/review evidence |
