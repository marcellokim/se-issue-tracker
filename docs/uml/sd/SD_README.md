# Detailed Sequence Diagram 작성 기준

이 폴더의 SD는 Larman 스타일의 detailed sequence diagram을 기준으로 설계 단계의 interaction diagram으로 작성한다.

## 기본 방향

- SSD의 system operation을 첫 메시지로 삼는다.
- 첫 non-UI 수신 객체는 MVC Controller 클래스가 아니라 GRASP Controller 역할을 하는 설계 객체로 표현한다.
  - 예: `:IssueTrackingSystem`
  - Larman식 SD의 `role : class` 표기이며, 시스템 이벤트를 받는 대표 객체이다.
- SD의 중심은 레이어 계층 호출이 아니라, use case를 실현하기 위해 어떤 객체가 어떤 책임을 수행하는가이다.
- 현재 코드에는 Service와 Repository port가 존재하지만, SD에서는 핵심 책임이 아닌 경우 note나 guard로 축약한다.
- GRASP Controller는 system operation의 진입점으로 표현하고, 실제 사후조건은 `Issue`, `Comment`, `IssueHistory`, `IssueDependency`, `Project`, `User` 같은 domain object 협력으로 표현한다.
- Operation Contract의 postcondition은 객체 생성, association 형성/붕괴, 속성 변경 메시지로 드러낸다.
- 권한, 로그인, repository 조회, clock, persistence transaction은 필요한 경우 guard 또는 note로 남긴다.

## 산출물 우선순위

SD 작성 시 다음 artifact를 근거로 사용한다.

1. UC 명세: actor, main flow, alternative flow, include/extend 확인
2. SSD: system operation과 actor-system 응답 확인
3. Operation Contract: instance 생성/삭제, association 형성/붕괴, 속성 변경 확인
4. Domain Model: 책임을 맡을 도메인 객체와 association 확인
5. Logical Architecture: system operation을 받을 GRASP Controller 역할 확인
6. 현재 main 코드: 정책 방향, operation 이름, 주요 상태 전이 방향 확인

## GRASP 적용 기준

- Controller: SSD system operation을 받는 non-UI 객체. 여기서 Controller는 MVC Controller 클래스가 아니라 GRASP Controller 역할이다.
- Information Expert: 상태, association, 이력 기록에 필요한 정보를 가진 객체가 해당 책임을 가진다.
- Creator: 새 객체를 포함하거나 밀접하게 사용하는 도메인 객체가 생성 책임을 가진다.
- Low Coupling / High Cohesion: GRASP Controller가 도메인 속성을 직접 조작하지 않고 도메인 객체에 의도를 전달한다.
- Pure Fabrication / Indirection: Service, repository, policy, clock은 실제 코드에는 존재하지만 제출용 SD에서는 핵심 책임이 아닐 때 축약한다.

## 작성 순서

1. SSD에서 system operation과 actor 응답을 확인한다.
2. Operation Contract에서 사후조건을 확인한다.
3. Domain Model에서 책임을 맡을 객체와 관계를 확인한다.
4. Logical Architecture에서 어떤 GRASP Controller 역할이 operation을 받을지 확인한다.
5. SD에서는 GRASP Controller가 흐름을 시작하고, domain object가 사후조건을 실현하는 식으로 표현한다.
6. UC의 optional/alternative 흐름은 `alt`, `opt`, `loop` frame으로 표현한다.

## 구현 반영 수준

- 현재 코드의 Service/Repository/JDBC 호출 순서를 그대로 복사하지 않는다.
- 현재 코드와 정책 방향이 어긋나지 않도록 operation 이름, 상태 전이, 필수 comment, dependency guard, deleted issue 정책은 반영한다. dependency는 resolve 시점의 guard로 표현하며, resolve 성공만으로 자동 제거하지 않는다.
- 삭제/복구처럼 현재 구현에서 repository port가 많은 일을 처리하더라도, SD 작성 단계에서는 soft delete/restore의 개념적 사후조건을 `Issue`, `IssueHistory`, `IssueDependency` 협력으로 표현할 수 있다.
- 세부 SQL, transaction helper, repository factory, session store는 SD 대상이 아니다.

## 표기 규칙

- lifeline 이름은 가능하면 `objectName:ClassName` 형식을 사용한다.
- 동기 메시지는 `->`, 응답 메시지는 `-->`로 표현한다.
- 새 객체는 `create "roleName:ClassName" as Alias`로 표시한다.
- 다른 UC로 이어지는 선택 흐름도 필요한 메시지를 현재 SD 안에 직접 풀어 쓴다.
- 모든 helper 메서드를 메시지로 나열하지 않는다. 중요한 정책은 guard나 note로만 남긴다.

## 작성된 SD 목록

- PUML: `sd_puml/`
- GRASP 설명: `sd_grasp/`
- PNG/SVG 이미지는 필요할 때 PlantUML로 랜더링한다. 현재 저장소에서는 `.puml`과 GRASP 설명 파일을 원본으로 관리한다.

| SD | PUML 원본 | GRASP 설명 |
| --- | --- | --- |
| UC1 Register Issue | `sd_puml/sd-01-register-issue-detailed.puml` | `sd_grasp/sd-01-register-issue-grasp.txt` |
| UC5 Assign Issue | `sd_puml/sd-05-assign-issue-detailed.puml` | `sd_grasp/sd-05-assign-issue-grasp.txt` |
| UC6 Mark Fixed | `sd_puml/sd-06-mark-fixed-detailed.puml` | `sd_grasp/sd-06-mark-fixed-grasp.txt` |
| UC6 Resolve Fixed Issue | `sd_puml/sd-07-resolve-fixed-issue-detailed.puml` | `sd_grasp/sd-07-resolve-fixed-issue-grasp.txt` |
| UC6 Reject Fix | `sd_puml/sd-08-reject-fix-detailed.puml` | `sd_grasp/sd-08-reject-fix-grasp.txt` |
| UC6 Close Issue | `sd_puml/sd-09-close-issue-detailed.puml` | `sd_grasp/sd-09-close-issue-grasp.txt` |
| UC6 Reopen Issue | `sd_puml/sd-10-reopen-issue-detailed.puml` | `sd_grasp/sd-10-reopen-issue-grasp.txt` |
| UC9 Delete Issue | `sd_puml/sd-12-delete-issue-detailed.puml` | `sd_grasp/sd-12-delete-issue-grasp.txt` |
| UC7 Add Dependency | `sd_puml/sd-24-add-dependency-detailed.puml` | `sd_grasp/sd-24-add-dependency-grasp.txt` |
| UC9 Restore Deleted Issue | `sd_puml/sd-26-restore-deleted-issue-detailed.puml` | `sd_grasp/sd-26-restore-deleted-issue-grasp.txt` |
| UC16 Change Priority | `sd_puml/sd-27-change-priority-detailed.puml` | `sd_grasp/sd-27-change-priority-grasp.txt` |
