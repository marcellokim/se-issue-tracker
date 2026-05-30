# Swing 보조 UI 설계

## 목적

#24는 두 번째 UI toolkit 요구사항을 작은 Swing 보조 화면으로 검증한다. 목표는 JavaFX 데모 흐름을 다시 만드는 것이 아니라, Swing도 JavaFX와 같은 application/controller/domain/persistence 계층을 재사용한다는 점을 코드 구조와 화면 증거로 보이는 것이다.

Swing UI는 read-only 증빙 slice로 둔다.

- 기존 demo 계정으로 로그인
- 접근 가능한 project 목록 조회
- 선택한 project의 issue 목록 조회
- 선택한 issue의 detail 조회
- controller 계층에서 전달되는 오류와 권한 실패 표시

## 범위

Swing UI는 최소 project/issue 조회 흐름만 제공한다.

포함 범위:

- `ApplicationContext.fromEnvironment()` bootstrap 재사용
- `AuthenticationController.login/logout`
- `DashboardController.viewProjects`
- `IssueController.searchIssues`
- `IssueController.viewIssueDetail`
- login, project list, issue list, detail, status message용 read-only Swing panel
- JavaFX와 Swing을 독립 실행할 수 있는 별도 Swing entry point

제외 범위:

- issue 등록, assignment, status change, delete/restore, dependency 변경, statistics, recommendation action
- JavaFX 화면 코드 복사
- Swing UI class에서 service, repository, JDBC, domain mutation 직접 호출
- 별도 저장소, mock data, 두 번째 application bootstrap 경로

## 아키텍처

Swing은 presentation layer에만 위치한다. Swing class는 controller와 service result DTO에 의존할 수 있지만, service 구현체, repository, JDBC class, domain mutation method에는 의존하지 않는다.

예상 package:

```text
src/main/java/com/github/marcellokim/issuetracker/ui/swing/
```

예상 class:

- `SwingSupportUi`: application context를 만들고 Swing event dispatch thread에서 frame을 여는 entry point
- `SwingSupportFrame`: 최상위 frame과 layout 구성
- `LoginPanel`: login form과 logout 상태 처리
- `ProjectListPanel`: project summary 목록
- `IssueListPanel`: 선택한 project의 issue summary table/list
- `IssueDetailPanel`: read-only issue detail, comment, history, available action name 표시
- `SwingUiModel`: 현재 project, issue, status message를 담는 작은 presentation state holder

UI는 기존 application boundary와 맞게 controller를 constructor injection으로 받는다.

```text
SwingSupportUi
  -> ApplicationContext.fromEnvironment()
  -> SwingSupportFrame(controllers)
  -> Swing panels
  -> controller methods
  -> application services
  -> repository ports
  -> JDBC adapters
```

## 데이터 흐름

1. 사용자가 Swing entry point를 실행한다.
2. `SwingSupportUi`가 일반 실행 환경에서 `ApplicationContext`를 생성한다.
3. 사용자가 `AuthenticationController.login`으로 로그인한다.
4. 로그인 성공 후 `DashboardController.viewProjects`로 접근 가능한 project를 조회한다.
5. project 선택 시 `IssueController.searchIssues(projectId, null, null, null)`을 호출한다.
6. issue 선택 시 `IssueController.viewIssueDetail(issueId)`을 호출한다.
7. 반환 결과를 read-only Swing view model로 표시한다.

View는 business permission을 직접 판단하지 않는다. 사용자 event를 controller로 보내고, 반환 데이터나 예외 메시지를 표시한다.

## 오류 처리

Swing은 event handler 경계에서 `RuntimeException`을 잡아 공통 status 영역에 메시지를 표시한다. 이렇게 하면 UI 코드에 policy check를 중복 구현하지 않으면서도 controller/service 실패가 데모 중 보인다.

예상 오류:

- login 누락
- 잘못된 인증 정보
- 현재 사용자에게 보이지 않는 project 또는 issue
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

- Swing login/project/issue/detail 흐름 screenshot
- main app과 같은 environment 및 controller를 사용한다는 설명

## 완료 기준

이 이슈는 다음 조건을 만족하면 완료로 본다.

- Swing UI가 독립 실행됨
- 기존 계정으로 로그인 가능
- project list와 issue list가 controller를 통해 표시됨
- issue detail이 write action 없이 표시됨
- 코드 구조상 같은 application/controller/domain/persistence stack 재사용이 확인됨
- 최종 문서에서 Swing을 두 번째 UI toolkit 증거로 인용 가능

## 설계 결정

Swing은 의도적으로 read-only로 둔다. JavaFX는 main write/action demo path를 담당한다. 이 분리는 workflow logic 중복을 막고 JavaFX action 작업과의 merge conflict를 줄인다.
