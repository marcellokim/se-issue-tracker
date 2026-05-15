# 과제 수행 개념 기준선

이 문서는 이슈 관리 시스템 과제를 진행하면서 유스케이스, 도메인 모델, SSD, Operation Contract, 개발 클래스 관점을 흔들리지 않게 맞추기 위한 기준선입니다.

강의 PDF 원문은 용량, 저작권, 공개 저장소 노출 범위 때문에 이 저장소에 올리지 않습니다. 대신 과제 수행 중 반복해서 판단해야 하는 핵심 개념만 요약해 팀원이 계속 참조할 수 있게 고정합니다.
프로젝트별 최신 결정은 `docs/assumptions.md`와 `docs/uml/README.md`를 우선하며, 이 문서는 강의 개념 경계를 확인하기 위한 기준선으로 사용합니다.

## 우선순위

판단이 충돌하면 아래 순서로 결정합니다.

1. `SE_Term_Project_2026-1.pdf`: 과제 요구사항, 제출물, 필수 기능의 최상위 기준
2. `docs/assumptions.md`: 팀이 확정한 상태 전이, 권한, DB/UI, 추천 방식
3. 이 문서: UML/OOAD 산출물의 개념 경계와 작성 규칙
4. 구현 편의: 위 기준과 충돌하지 않을 때만 반영

## 산출물 경계

| 산출물 | 반드시 표현할 것 | 표현하지 않을 것 |
| --- | --- | --- |
| 유스케이스 다이어그램 | 시스템 경계, 외부 액터, 액터 목표, include/extend/generalization 관계 | 상세 workflow, 화면 흐름, 내부 객체 협력, DB 구조 |
| 유스케이스 명세 | 액터 목표, 주 성공 시나리오, 확장/예외 시나리오, 사전/사후조건 | 구현 알고리즘, 위젯 단위 UI 동작 |
| 도메인 모델 | 업무에서 기억해야 하는 핵심 개념, 속성, 연관, 다중성 | 컨트롤러, 서비스, 저장소, DTO, DB 테이블, 화면 클래스 |
| SSD | 한 유스케이스 시나리오에서 액터가 시스템에 보내는 black-box 이벤트 | 내부 객체 호출, 컨트롤러/서비스/엔티티/DB lifeline |
| Operation Contract | 하나의 system operation이 도메인 객체에 남기는 효과 | 코드 절차, SQL, UI 절차, 서비스 호출 순서 |
| 개발 클래스 관점 | 구현에 필요한 서비스, 저장소, DTO, 정책 객체 | 제출용 도메인 모델의 대체물 |

## 유스케이스 기준

- 유스케이스는 액터가 목표를 달성하기 위해 시스템을 사용하는 이야기입니다. 다이어그램은 보조 산출물입니다.
- 액터는 시스템 외부의 역할입니다. `Admin`, `PL`, `Developer`, `Tester`는 같은 로그인 기능을 쓰더라도 서로 다른 외부 역할입니다.
- 주요 유스케이스는 actor-goal 수준으로 작성합니다. 반복되는 공통 절차는 보조 유스케이스로 분리할 수 있습니다.
- `<<include>>`는 기본 유스케이스가 항상 포함 유스케이스를 필요로 할 때만 씁니다.
- `<<extend>>`는 조건부 또는 선택 동작을 붙일 때만 씁니다. 기본 유스케이스는 확장 없이도 의미가 있어야 합니다.
- 액터 generalization은 공통 역할에서 특수 역할로 유스케이스를 상속시킬 때만 사용합니다.

## 도메인 모델 기준

- 도메인 모델은 개념 모델입니다. Java 클래스도, DB ERD도 아닙니다.
- 도메인 클래스는 이슈 관리 업무에서 의미가 있고 시스템이 기억해야 하는 개념이어야 합니다.
- 도메인 클래스에는 operation을 넣지 않습니다. 동작은 유스케이스, SSD, Operation Contract, 개발 클래스 관점에서 다룹니다.
- 속성은 시스템이 기억해야 하는 정보일 때만 추가합니다.
- 연관은 의미 있는 관계를 일정 시간 이상 기억해야 할 때만 추가합니다.
- 모든 typed field를 연관으로 바꾸지 않습니다. 업무적으로 중요한 관계만 association line으로 표시합니다.
- DB 테이블, repository, controller, UI 화면, session, DTO, application service는 제출용 도메인 모델에서 제외합니다.
- 조회 결과나 통계 결과는 보통 도메인 개념이 아닙니다. 구현에서는 read model이나 DTO로 둘 수 있지만 제출용 도메인 모델에는 신중하게 넣습니다.

## 도메인 용어 판단 기준

아래 표는 과제 산출물을 읽을 때 흔들리기 쉬운 도메인 용어의 판단 기준이다. 최신 제출용 도메인 모델의 정확한 클래스/속성/연관은 `docs/uml/domain/domain-model-issue-tracking-system.puml`을 따른다.

| 용어 | 고정 의미 |
| --- | --- |
| `User` | Admin, PL, Dev, Tester가 사용하는 사용자 계정 |
| `Role` | `ADMIN`, `PL`, `DEV`, `TESTER` 중 하나. `DEV`는 Developer 역할을 의미한다 |
| `Project` | Admin이 생성하고 팀원이 속하는 이슈 관리 프로젝트 |
| `Issue` | 하나의 등록 이슈를 대표하는 핵심 aggregate |
| `IssueStatus` | `NEW`, `ASSIGNED`, `FIXED`, `RESOLVED`, `CLOSED`, `REOPENED`, `DELETED` |
| `Priority` | `BLOCKER`, `CRITICAL`, `MAJOR`, `MINOR`, `TRIVIAL` |
| `Comment` | 이슈에 붙는 토론, 보완 설명, 반려 사유 |
| `IssueHistory` | 이슈 생명주기와 주요 변경의 감사 기록 |
| `IssueDependency` | 선행/차단 관계를 구조화해 저장하는 이슈 간 의존성. 별도 name/type보다 blocking issue와 blocked issue의 연결 방향으로 의미를 표현한다 |

경계가 애매한 용어는 아래처럼 취급합니다.

- `AssigneeRecommendation`: 추천 유스케이스의 조회 결과입니다. 개발 클래스 관점이나 DTO로는 가능하지만, 제출용 도메인 모델에서는 추천 결과 자체를 저장해야 할 때만 넣습니다.
- `DeletedIssueRetention`: 삭제된 이슈 30개 초과 시 오래된 항목을 정리하는 정책입니다. 도메인 모델에서는 `Issue.status = DELETED`와 `IssueHistory(STATUS_CHANGED, newValue=DELETED).changedDate`로 표현하고, 별도 `Issue.deletedAt` 속성은 두지 않습니다.
- `PermissionPolicy`: 권한 검사를 담당하는 정책/서비스입니다. 도메인 엔티티가 아니라 operation contract의 사전조건이나 개발 클래스 관점에서 다룹니다.

## 상태와 권한 기준

- 기본 상태 흐름은 `NEW -> ASSIGNED -> FIXED -> RESOLVED -> CLOSED`입니다.
- `FIXED`는 Developer가 수정 완료를 주장한 중간 상태입니다.
- Tester 검증 성공은 `FIXED -> RESOLVED`입니다.
- Tester 검증 실패는 `FIXED -> ASSIGNED`이며, 실패 사유를 comment/history에 남깁니다.
- PL은 이슈 close/reopen, 담당자 배정, priority 변경, delete, dependency 관리를 수행합니다.
- `REOPENED`는 다시 작업 대상으로 판단된 상태이며 PL이 재배정해 작업 흐름으로 돌립니다.
- `DELETED`는 soft-delete 상태입니다. 물리 삭제는 retention 정책입니다.
- Reporter는 assignment 전까지만 title/description을 직접 수정합니다. 이후 정정은 comment/history로 남깁니다.
- Admin은 계정과 프로젝트를 준비하고 관리합니다.

## SSD 기준

- SSD는 선택한 한 유스케이스 시나리오마다 작성합니다.
- 시스템 lifeline은 시스템 하나로 둡니다. controller, service, repository, entity를 SSD에 넣지 않습니다.
- 액터가 시스템에 보내는 메시지가 system event입니다.
- system event는 나중에 operation contract의 system operation 후보가 됩니다.
- 이 프로젝트의 우선 SSD 후보는 다음입니다.
  - `registerIssue(...)`: Tester가 이슈를 등록
  - `assignIssue(...)`: PL이 담당자와 검증자를 배정
  - `markFixed(...)`: Developer가 수정 완료를 표시
  - `resolveIssue(...)`: Tester가 수정 결과를 검증 성공 처리
  - `closeIssue(...)`: PL이 종료 처리

## Operation Contract 기준

- Operation Contract는 system operation 하나의 효과를 설명합니다.
- 최소 항목은 operation 이름/파라미터, 관련 유스케이스, 사전조건, 사후조건입니다.
- 사후조건이 가장 중요합니다. 성공 후 도메인 객체에 남은 변화를 과거형으로 씁니다.
- 사후조건에는 객체 생성, 속성 변경, association 형성/제거, history 기록을 포함합니다.
- 코드 단계, SQL, UI 절차, controller/service 호출 순서는 쓰지 않습니다.

우선 작성 후보는 다음입니다.

- `registerIssue(...)`: `Issue`가 생성되고 reporter, status, priority, reportedDate, history가 설정됨
- `assignIssue(...)`: assignee/verifier association이 설정되고 status가 `ASSIGNED`로 바뀌며 history가 기록됨
- `markFixed(...)`: fixer, status가 설정되고 `IssueHistory(STATUS_CHANGED).changedDate` 기준으로 변경 시각이 기록됨
- `resolveIssue(...)`: status가 설정되고 `IssueHistory(STATUS_CHANGED).changedDate` 기준으로 검증 성공 시각이 기록됨

## 판단 체크리스트

개념이나 클래스의 위치가 애매하면 아래 순서로 결정합니다.

1. 액터 목표인가? 그러면 유스케이스에 둡니다.
2. 이슈 관리 업무가 기억해야 하는 개념인가? 그러면 도메인 모델에 둡니다.
3. UI, DB, controller, service, session, DTO, repository인가? 그러면 제출용 도메인 모델에서 제외합니다.
4. 액터가 시스템에 보내는 이벤트인가? 그러면 SSD의 system operation 후보로 둡니다.
5. system operation이 도메인 객체를 의미 있게 바꾸는가? 그러면 Operation Contract를 씁니다.
6. 조회 결과나 통계 결과인가? 그러면 우선 개발 클래스 관점이나 DTO로 둡니다.
7. 그래도 애매하면 더 작은 도메인 모델을 선택하고, 구현 세부사항은 별도 문서나 코드에 둡니다.
