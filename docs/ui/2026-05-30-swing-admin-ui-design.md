# Swing Admin 관리 UI 설계

## 목적

#24는 두 번째 UI toolkit 요구사항을 Swing 기반 Admin 관리 화면으로 검증한다. 목표는 JavaFX 이슈 업무 흐름을 다시 구현하는 것이 아니라, UC 다이어그램의 actor 분리와 같은 application/controller/domain/persistence 계층 재사용을 코드 구조와 화면 증거로 보이는 것이다.

UI toolkit 책임은 다음처럼 나눈다.

- JavaFX: `PL`, `Dev`, `Tester`가 사용하는 실사용자 이슈 업무 UI
- Swing: `Admin`이 사용하는 계정/프로젝트 관리 UI

## 범위

Swing UI는 Admin 전용 UC12/UC13 흐름을 최소 구현한다. 이슈 목록/상세, 상태 변경, 통계, 삭제 이슈 관리는 JavaFX 실사용자 UI 범위로 둔다.

포함 범위:

- `ApplicationContext.fromEnvironment()` bootstrap 재사용
- `AuthenticationController.login/logout`
- `DashboardController.viewProjects`
- `DashboardController.viewUsers`
- `AccountController.createAccount`
- `AccountController.renameAccount`
- `AccountController.changeAccountRole`
- `AccountController.activateAccount`
- `AccountController.deactivateAccount`
- `ProjectController.viewProjectAdminDetail`
- `ProjectController.viewProjectParticipants`
- `ProjectController.createProject`
- `ProjectController.renameProject`
- `ProjectController.changeProjectDescription`
- `ProjectController.deleteProject`
- `ProjectController.addProjectParticipant`
- `ProjectController.removeProjectParticipant`
- login, project management, participant management, account management, status message용 Swing panel
- JavaFX와 Swing을 독립 실행할 수 있는 별도 Swing entry point

제외 범위:

- issue 등록, search/detail, comment, assignment, status change, delete/restore, dependency 변경, statistics, recommendation action
- JavaFX 화면 코드 복사
- Swing UI class에서 service, repository, JDBC, domain mutation 직접 호출
- 별도 저장소, mock data, 두 번째 application bootstrap 경로

## 아키텍처

Swing은 presentation layer에만 위치한다. Swing class는 controller와 service result DTO에 의존할 수 있지만, service 구현체, repository, JDBC class, domain mutation method에는 의존하지 않는다. 권한 판단은 UI가 하지 않고 controller/service 실패를 status 영역에 표시한다.

예상 package:

```text
src/main/java/com/github/marcellokim/issuetracker/ui/swing/
```

예상 class:

- `SwingAdminUi`: application context를 만들고 Swing event dispatch thread에서 frame을 여는 entry point
- `SwingAdminFrame`: 최상위 frame과 tab layout 구성
- `AdminLoginPanel`: login form과 logout 상태 처리
- `ProjectManagementPanel`: project 목록, 생성, 이름/설명 변경, 삭제 action
- `ProjectParticipantPanel`: 선택한 project의 participant 조회, 추가, 제거 action
- `AccountManagementPanel`: 사용자 목록, 계정 생성, 이름 변경, role 변경, 활성화/비활성화 action
- `SwingAdminUiModel`: 현재 project, selected account, status message를 담는 작은 presentation state holder

UI는 기존 application boundary와 맞게 controller를 constructor injection으로 받는다.

```text
SwingAdminUi
  -> ApplicationContext.fromEnvironment()
  -> SwingAdminFrame(controllers)
  -> Swing panels
  -> controller methods
  -> application services
  -> repository ports
  -> JDBC adapters
```

## 데이터 흐름

1. Admin이 Swing entry point를 실행한다.
2. `SwingAdminUi`가 일반 실행 환경에서 `ApplicationContext`를 생성한다.
3. Admin이 `AuthenticationController.login`으로 로그인한다.
4. 로그인 성공 후 `DashboardController.viewProjects`와 `DashboardController.viewUsers`로 관리 대상 목록을 조회한다.
5. project 선택 시 `ProjectController.viewProjectAdminDetail`과 `viewProjectParticipants`를 호출한다.
6. project 생성/수정/삭제 또는 participant 추가/제거 action은 `ProjectController`로 보낸다.
7. 계정 생성/이름 변경/role 변경/활성화/비활성화 action은 `AccountController`로 보낸다.
8. 반환 결과를 Swing view model로 반영하고 목록을 다시 조회한다.

View는 business permission을 직접 판단하지 않는다. 사용자 event를 controller로 보내고, 반환 데이터나 예외 메시지를 표시한다. `Admin`이 아닌 사용자가 Swing Admin UI로 로그인하면 controller/service의 권한 실패가 그대로 표시되어야 한다.

## 오류 처리

Swing은 event handler 경계에서 `RuntimeException`을 잡아 공통 status 영역에 메시지를 표시한다. 이렇게 하면 UI 코드에 policy check를 중복 구현하지 않으면서도 controller/service 실패가 데모 중 보인다.

예상 오류:

- login 누락
- 잘못된 인증 정보
- 현재 사용자가 Admin이 아닌 경우
- 중복 project name
- active project participant에 대한 role 변경 또는 비활성화 거부
- ADMIN 계정을 project participant로 추가하려는 경우
- 이미 active issue 책임을 가진 participant 제거 거부
- database 환경 미설정

Database bootstrap 실패는 application context 없이 UI를 계속 진행할 수 없으므로 시작 오류 dialog로 표시한다.

## 테스트

테스트는 얇게 유지하고 pixel-level Swing assertion은 피한다.

최소 검증:

- `./gradlew check`에서 UI class compile 통과
- Swing class가 repository 또는 JDBC package를 import하지 않음
- constructor seam이 자연스러우면 fake/test controller로 frame 생성 smoke test 추가
- 기존 controller test가 behavior coverage의 중심으로 유지됨

수동 증거:

- Swing login/project/account/participant 관리 흐름 screenshot
- main app과 같은 environment 및 controller를 사용한다는 설명
- `Admin` 전용 UC12/UC13을 Swing에서 처리하고, 이슈 업무는 JavaFX에서 처리한다는 actor 분리 설명

## 완료 기준

이 이슈는 다음 조건을 만족하면 완료로 본다.

- Swing UI가 독립 실행됨
- `admin` 계정으로 로그인 가능
- project 목록과 admin project detail/participants가 controller를 통해 표시됨
- account 목록과 account 관리 action이 controller를 통해 실행됨
- project 생성/수정/삭제와 participant 추가/제거가 controller를 통해 실행됨
- non-Admin 로그인 시 Admin action이 거부되고 메시지가 표시됨
- 코드 구조상 같은 application/controller/domain/persistence stack 재사용이 확인됨
- 최종 문서에서 Swing을 Admin UI, JavaFX를 실사용자 UI로 설명 가능

## 설계 결정

Swing은 Admin 관리 UI로 둔다. JavaFX는 `PL`, `Dev`, `Tester`의 이슈 업무 UI를 담당한다. 이 분리는 UC 다이어그램의 actor 경계와 맞고, Swing에 복잡한 이슈 workflow를 중복 구현하지 않게 한다.
