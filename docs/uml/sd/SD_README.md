# Detailed Sequence Diagram 작성 기준

이 폴더의 SD는 Larman 스타일의 Detailed Sequence Diagram을 기준으로 작성한다.

## 기본 방향

- UC1 Register Issue Detailed SD를 작성 기준선으로 삼는다.
- SSD의 system operation을 첫 메시지로 삼는다.
- 첫 non-UI 수신 객체는 logical architecture의 Use Case Controller를 사용한다.
  - 예: `:IssueController`, `:AssignmentController`, `:IssueStateController`, `:DeletedIssueController`
- SD의 중심은 UI/DB 호출 흐름이 아니라 도메인 객체 사이의 책임 협력이다.
- Controller는 system operation을 수신하고 흐름을 시작하는 객체로만 표현한다.
- Service, repository, persistence, policy, clock, session 같은 구현 조정 객체는 핵심 책임이 아닌 경우 lifeline으로 펼치지 않고 note 또는 guard로만 표현한다.
- 상태 변경, association 형성/제거, 이력 생성, 댓글 생성 같은 postcondition은 `Issue`, `Comment`, `IssueHistory`, `IssueDependency`, `Project`, `User` 같은 domain object 메시지로 표현한다.
- Operation Contract의 postcondition은 도메인 객체 메시지로 실현한다.
- Domain Model의 association과 enum/value type을 객체 책임 배정의 근거로 사용한다.
- GRASP 패턴은 객체 선택의 근거로 둔다.
- SD가 너무 구현 흐름 위주로 복잡해지거나, 반대로 OC 사후조건을 확인하기 어려울 만큼 단순해지지 않도록 조정한다.
- 제출용 가독성을 위해 핵심 객체 책임, 생성, association 형성, 상태/속성 변경만 선명하게 남기고 보조 서비스는 필요한 경우 note로 축약한다.

## 산출물 우선순위

SD 작성 시 다음 artifact를 최우선 근거로 사용한다.

1. UC 명세: actor, precondition, main flow, alternative flow, postcondition, include/extend 확인
2. SSD: system operation, actor-system 메시지, 응답 메시지 확인
3. Operation Contract: instance 생성/삭제, association 형성/붕괴, 속성값 변경 확인
4. 기본 가정사항: 상태 전이, 권한, 자동 필드, 삭제/복구/reopen 정책 확인
5. Domain Model: 책임을 맡을 도메인 객체, association, enum/value type 확인
6. Logical Architecture: system operation을 받을 controller와 MVC/Application Layer 경계 확인

## GRASP 적용 기준

- Controller: SSD system operation을 받는 non-UI 객체.
- Creator: 새 객체를 포함하거나 밀접하게 사용하는 도메인 객체가 생성 책임을 가진다.
- Information Expert: 상태, association, 이력 기록에 필요한 정보를 가진 객체가 해당 책임을 가진다.
- Low Coupling / High Cohesion: Controller가 도메인 속성을 직접 조작하지 않고 도메인 객체에 의도를 전달한다.
- Pure Fabrication / Indirection: repository, policy, clock 같은 기술/정책 객체는 설계 설명에는 남기되, 제출용 SD에서는 필요할 때만 note로 축약한다.

## 작성 순서

1. SSD에서 system operation과 actor 응답을 확인한다.
2. Operation Contract에서 instance 생성/삭제, association 형성/붕괴, 속성 변경을 확인한다.
3. Domain Model에서 책임을 맡을 객체와 association을 확인한다.
4. Logical Architecture에서 어떤 Controller가 system operation을 받을지 확인한다.
5. SD에서는 Controller가 흐름을 시작하고, 실제 postcondition은 도메인 객체 메시지로 표현한다.
6. UC의 optional/alternative 흐름은 `alt`, `opt`, `ref` frame으로 표현한다.

## 도메인 책임 표현 기준

- SD는 현재 코드의 repository/JDBC 호출 순서를 그대로 복사하지 않는다.
- 삭제/복구처럼 실제 구현에서는 repository 연산으로 처리되는 부분도, Operation Contract의 사후조건을 설명할 때는 도메인 객체 책임으로 표현할 수 있다.
- 예: soft delete는 `targetIssue:Issue`가 `softDelete(currentPL, now)` 책임을 수행하고, 그 결과 `IssueHistory(STATUS_CHANGED)`와 dependency association 제거가 발생하는 것으로 표현한다.
- 예: restore는 `targetIssue:Issue`가 삭제 이력에서 복구 대상 상태를 찾고 `restore(currentPL, now)` 책임을 수행하는 것으로 표현한다.
- 이런 표현은 구현 코드의 호출 순서를 보여주기 위한 것이 아니라, Larman식 객체 설계에서 어떤 domain object가 postcondition을 책임지는지 보여주기 위한 것이다.

## 표기 규칙

- lifeline 이름은 가능하면 `objectName:ClassName` 또는 `:ClassName` 형식을 사용한다.
- 상호작용 파트너는 사각형 머리의 `participant`로 표현한다. `control`, `entity` 아이콘 표기는 사용하지 않는다.
- lifeline head 안에는 roleName을 반드시 적는다. Class가 명확하면 `roleName:ClassName`으로 적고, 명확하지 않으면 roleName만 적는다.
- `Controller`, `Issue Controller`처럼 일반 역할명만 적는 표현은 피한다.
- 현재 logical architecture에 있는 컨트롤러를 사용할 때는 `issueController:IssueController`, `assignmentController:AssignmentController`, `issueStateController:IssueStateController`, `deletedIssueController:DeletedIssueController`처럼 repo의 클래스명과 연결되는 이름을 사용한다.
- PlantUML의 `control`, `entity` 키워드는 UML 역할을 직관적으로 보여주지만 아이콘 lifeline을 만들 수 있으므로, 강의자료 notation을 맞출 때는 `participant`로 작성하고 roleName/ClassName 및 note로 의미를 드러낸다.
- 동기 메시지는 `->`로 표현한다. 렌더링 결과가 속이 찬 검은 화살표로 보여야 한다.
- 비동기 메시지는 `->>`로 표현한다. 렌더링 결과가 꺾쇠 모양 화살표로 보여야 한다.
- 응답 메시지는 `-->`로 표현한다. 렌더링 결과가 점선 꺾쇠 모양 화살표로 보여야 한다.
- 새로 생성되는 객체는 `create "roleName:ClassName" as Alias` 다음에 `creator --> Alias : new`를 사용한다. 생성 메시지는 생성될 객체의 lifeline head를 향하는 점선 `new` 화살표로 보여야 한다.
- 다른 UC로 이어지는 선택 흐름은 `ref` frame으로 표현한다.
- 도메인 책임의 근거가 중요한 경우 note에 GRASP 근거 또는 OC postcondition을 짧게 적는다.

## 작성된 SD 목록

파일은 세부 폴더에 나누어 둔다.

- PUML: `sd_puml/`
- PNG: `sd_img/`
- GRASP 설명: `sd_grasp/`

| SD | PUML | PNG | GRASP 설명 |
| --- | --- | --- | --- |
| UC1 Register Issue | `sd_puml/sd-01-register-issue-detailed.puml` | `sd_img/sd-01-register-issue-detailed.png` | `sd_grasp/sd-01-register-issue-grasp.txt` |
| UC5 Assign Issue | `sd_puml/sd-05-assign-issue-detailed.puml` | `sd_img/sd-05-assign-issue-detailed.png` | `sd_grasp/sd-05-assign-issue-grasp.txt` |
| UC6 Mark Fixed | `sd_puml/sd-06-mark-fixed-detailed.puml` | `sd_img/sd-06-mark-fixed-detailed.png` | `sd_grasp/sd-06-mark-fixed-grasp.txt` |
| UC6 Resolve Fixed Issue | `sd_puml/sd-07-resolve-fixed-issue-detailed.puml` | `sd_img/sd-07-resolve-fixed-issue-detailed.png` | `sd_grasp/sd-07-resolve-fixed-issue-grasp.txt` |
| UC6 Reject Fix | `sd_puml/sd-08-reject-fix-detailed.puml` | `sd_img/sd-08-reject-fix-detailed.png` | `sd_grasp/sd-08-reject-fix-grasp.txt` |
| UC6 Close Issue | `sd_puml/sd-09-close-issue-detailed.puml` | `sd_img/sd-09-close-issue-detailed.png` | `sd_grasp/sd-09-close-issue-grasp.txt` |
| UC6 Reopen Issue | `sd_puml/sd-10-reopen-issue-detailed.puml` | `sd_img/sd-10-reopen-issue-detailed.png` | `sd_grasp/sd-10-reopen-issue-grasp.txt` |
| UC9 Delete Issue | `sd_puml/sd-12-delete-issue-detailed.puml` | `sd_img/sd-12-delete-issue-detailed.png` | `sd_grasp/sd-12-delete-issue-grasp.txt` |
| UC7 Add Dependency | `sd_puml/sd-24-add-dependency-detailed.puml` | `sd_img/sd-24-add-dependency-detailed.png` | `sd_grasp/sd-24-add-dependency-grasp.txt` |
| UC9 Restore Deleted Issue | `sd_puml/sd-26-restore-deleted-issue-detailed.puml` | `sd_img/sd-26-restore-deleted-issue-detailed.png` | `sd_grasp/sd-26-restore-deleted-issue-grasp.txt` |
| UC16 Change Priority | `sd_puml/sd-27-change-priority-detailed.puml` | `sd_img/sd-27-change-priority-detailed.png` | `sd_grasp/sd-27-change-priority-grasp.txt` |
