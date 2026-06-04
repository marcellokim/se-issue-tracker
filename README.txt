SE 2026-1 소프트웨어공학 팀 프로젝트 제출물 README
프로젝트명: Issue Tracking System (ITS)


1. GitHub 주소

- GitHub 저장소
  https://github.com/marcellokim/se-issue-tracker

- GitHub Project
  https://github.com/users/marcellokim/projects/1


2. 제출물 구성 요약

본 제출물은 Java 기반 Issue Tracking System의 소스 코드, 실행 파일, JUnit 테스트 코드, Oracle DB schema/seed 데이터, 요구 분석 및 설계 문서, 발표 슬라이드, 프로젝트 보고서, 데모 영상을 포함한다.

최종 제출 zip에는 다음 항목을 포함한다.

1) README
- README.txt
  - 제출 산출물 목록, GitHub 주소, 실행 방법, 테스트 방법을 정리한 문서

2) 발표 슬라이드
- 최종 발표용 슬라이드 파일
  - 프로젝트 목표, 요구 분석, 설계, 구현 결과, 테스트 결과, 데모 흐름 요약

3) 프로젝트 보고서
- 최종 프로젝트 보고서 PDF
  - 프로젝트 개요, 가정사항 및 정책, 요구 정의 및 분석, 설계, 구현 결과, JUnit 테스트 수행 내역, 한계 및 향후 보완 정리

4) 데모 영상
- 30분 내외 데모 영상
  - 과제 예제 시나리오와 주요 구현 기능 동작 확인
  - JavaFX UI 중심 기능 시연
  - Swing UI 실행을 통해 UI를 제외한 Controller/Service/Domain/Repository/JDBC 계층 재사용 확인

5) 소스 코드, 실행 파일, 테스트 코드, 데이터
- src/main/java/
  - JavaFX/Swing UI, Controller, Service, Domain, Repository interface, JDBC 구현체, technical layer
- src/main/resources/
  - Oracle DB schema/seed와 JavaFX 리소스
- src/main/resources/db/oracle/schema-oracle.sql
  - Oracle DB 테이블, 제약조건, sequence 등 schema 정의
- src/main/resources/db/oracle/seed-oracle.sql
  - 데모용 계정, 프로젝트, 이슈, 댓글, 이력, 의존성, 통계 확인용 seed 데이터
- src/test/java/
  - Domain, Service, Controller, Repository/JDBC, Architecture Boundary, Oracle Integration Test
- build.gradle, settings.gradle
  - Gradle 빌드, 실행, 테스트, Oracle local task 정의
- gradlew, gradlew.bat, gradle/
  - Gradle Wrapper 실행 파일
- scripts/
  - Oracle local 실행 보조, 제출 패키징, GitHub 작업 보조 스크립트
- .github/workflows/
  - Gradle build/test, Oracle integration test, CodeQL, SonarCloud, PR metadata/project automation workflow

6) 요구 분석 및 설계 문서
- docs/assumptions.md
  - 팀 가정사항과 주요 정책 정리
- docs/requirements-traceability.md
  - 요구사항, UC, 구현, 테스트, 제출 증빙 연결표
- docs/execution-test-guide.md
  - 실행 방법, 테스트 방법, Oracle local test, 데모 seed 계정 안내
- docs/README.md
  - docs/artifact 폴더와 PlantUML 산출물 구조 안내
- docs/artifact/uc/
  - Use Case Diagram, Fully Dressed Use Case 명세
- docs/artifact/domain/
  - Domain Model
- docs/artifact/ssd/
  - System Sequence Diagram
- docs/artifact/oc/operation_contracts.md
  - Operation Contract
- docs/artifact/architecture/
  - Logical Architecture, Layer Architecture, 구현 계층 설명
- docs/artifact/sd/
  - Sequence Diagram과 GRASP 책임 설명
- docs/artifact/dcd/its_dcd.puml
  - Design Class Diagram
- docs/ooad-grasp-mvc.md
  - OOAD, GRASP, MVC 적용 설명
- docs/api/
  - Controller/API 명세
- docs/ui/javaFX/UI_navigation_map/
  - JavaFX 화면 이동 흐름
- docs/ui/javaFX/API_specification/
  - JavaFX 화면별 API 대응 문서
- docs/db-persistence-policy.md
  - DB persistence, seed, repository 정책
- docs/project-management-plan.md, docs/team-setup-manual.md, docs/automation-playbook.md
  - 팀 운영, GitHub 협업, 작업 흐름 보조 문서

참고: docs/artifact 아래의 .puml 파일은 PlantUML 원본이다. 보고서나 발표자료에 이미지로 넣을 때는 PNG 또는 SVG로 별도 렌더링해야 한다. 렌더링 방법은 docs/README.md의 PlantUML 렌더링 절차를 따른다.


3. 프로그램 기능 요약

본 프로그램은 팀 단위 소프트웨어 개발 과정에서 발생하는 이슈를 등록하고, 배정하고, 수정하고, 검증하고, 종료하는 흐름을 관리하는 Issue Tracking System이다.

주요 기능은 다음과 같다.

- 계정 관리: Admin이 사용자 계정을 생성하고 이름, 역할, 활성 상태를 관리한다.
- 프로젝트 관리: Admin이 프로젝트를 생성, 수정, 삭제하고 프로젝트 참여자를 관리한다.
- 이슈 등록: 프로젝트 참여자가 프로젝트 안에서 새 이슈를 등록한다.
- 이슈 검색 및 조회: PL, Dev, Tester가 참여 프로젝트의 일반 이슈를 검색하고 상세 내용을 확인한다.
- 이슈 배정 및 변경: PL이 이슈 상태에 따라 assignee Dev와 verifier Tester를 배정하거나 변경한다.
- 이슈 상태 변경: Dev, Tester, PL이 역할과 현재 상태에 맞는 상태 전이를 수행한다.
- 댓글 관리: 일반 댓글 추가/수정/삭제와 상태 변경 사유 댓글을 관리한다.
- 이슈 이력 관리: 이슈 생성, 상태 변경, 배정 변경, 댓글 변경, 의존성 변경 등을 IssueHistory로 기록한다.
- 이슈 의존성 관리: PL이 같은 프로젝트 안의 blocking/blocked 관계를 관리한다.
- 삭제 이슈 관리: PL이 NEW/CLOSED 이슈를 soft delete하고, 필요하면 복구 또는 영구 삭제한다.
- 통계 조회: 프로젝트 단위의 일/월별 이슈 발생, 상태 변경, 댓글 수, 상태/우선순위 분포를 조회한다.
- Assignment 추천: 과거 fixer/resolver 이력과 이슈 내용을 참고하여 assignee/verifier 후보를 추천한다.
- 다중 UI: JavaFX UI와 Swing UI를 제공하며, 두 UI는 Controller 이후의 Service, Domain, Repository, JDBC 계층을 공유한다.


4. 실행 환경

필수 환경:
- JDK 21 이상
- Gradle Wrapper 실행 가능 환경
- Oracle DB 23c Free 또는 Oracle XE
- Windows PowerShell, macOS/Linux shell 또는 WSL

권장 환경:
- Windows PowerShell
- Docker Desktop 또는 Docker CLI 사용 가능 환경
- Git

본 프로젝트는 Oracle DB를 사용한다. 로컬에서 앱을 실행하려면 Oracle DB schema가 준비되어 있어야 한다.


5. 데모 seed 계정

데모용 seed 데이터에는 다음 계정이 포함된다. DB에는 평문 비밀번호가 저장되지 않고 PBKDF2 기반 salt/hash만 저장된다.

- admin / DemoLocalAdmin!
- pl1 / DemoLocalPl1!
- pl2 / DemoLocalPl2!
- dev1 ~ dev10 / DemoLocalDev1! ~ DemoLocalDev10!
- tester1 ~ tester5 / DemoLocalTester1! ~ DemoLocalTester5!


6. 프로그램 실행 방법

6.1 기존 로컬 Oracle XE 또는 이미 준비된 Oracle DB를 사용하는 경우

Windows PowerShell 기준:

  $env:ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
  $env:ITS_DB_USER="ITS_USER"
  $env:ITS_DB_PASSWORD="ItsLocalDev2026!"

  .\gradlew.bat oracleConnectionCheck --console=plain

고정 데모 seed로 DB를 초기화하려면 다음 명령을 실행한다.
주의: 아래 명령은 앱 schema의 기존 core table 데이터를 지우고 seed를 다시 넣는다.

  .\gradlew.bat oracleResetFixedSeed --console=plain

JavaFX UI 실행:

  .\gradlew.bat run --console=plain

Swing UI 실행:

  .\gradlew.bat runSwing --console=plain


6.2 Gradle Docker Oracle task를 사용하는 경우

Windows PowerShell 기준:

  .\gradlew.bat oracleLocalStart --console=plain
  .\gradlew.bat oracleLocalResetFixedSeed --console=plain

  $env:ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/FREEPDB1"
  $env:ITS_DB_USER="ITS_USER"
  $env:ITS_DB_PASSWORD="ItsLocalDev2026!"

  .\gradlew.bat oracleConnectionCheck --console=plain
  .\gradlew.bat run --console=plain

Swing UI를 실행하려면 마지막 명령을 다음과 같이 바꾼다.

  .\gradlew.bat runSwing --console=plain


6.3 macOS/Linux shell 예시

  export ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/FREEPDB1"
  export ITS_DB_USER="ITS_USER"
  export ITS_DB_PASSWORD="ItsLocalDev2026!"

  ./gradlew oracleConnectionCheck --console=plain
  ./gradlew run --console=plain

Swing UI:

  ./gradlew runSwing --console=plain


7. 테스트 및 검증 방법

일반 JUnit 테스트와 기본 검증:

Windows PowerShell:

  .\gradlew.bat check --console=plain

macOS/Linux:

  ./gradlew check --console=plain

Oracle 통합 테스트까지 실행:

Windows PowerShell:

  .\gradlew.bat oracleLocalTest --console=plain

macOS/Linux:

  ./gradlew oracleLocalTest --console=plain

특정 테스트 파일 실행 예시:

  .\gradlew.bat test --tests "*IssueWorkflowTest" --console=plain
  .\gradlew.bat test --tests "*AssignmentServiceTest" --console=plain
  .\gradlew.bat test --tests "*IssueControllerTest" --console=plain

특정 테스트 메서드 실행 예시:

  .\gradlew.bat test --tests "*IssueWorkflowTest.resolveFixedIssue" --console=plain

제출 metadata 확인:

  .\gradlew.bat verifySubmissionMetadata --console=plain


8. 제출 전 확인 사항

제출 직전에는 다음 항목을 확인한다.

1) JavaFX UI 실행 확인
2) Swing UI 실행 확인
3) Oracle DB 연결 확인
4) 고정 seed 데이터 초기화 확인
5) ./gradlew check 통과 확인
6) Oracle integration test 통과 확인
7) README.txt, 발표 슬라이드, 프로젝트 보고서, 소스코드, 실행 파일, JUnit 테스트 코드, 데이터, 데모 영상 포함 여부 확인
8) .git, .gradle, build, .worktrees, 개인 임시 파일, 과제 원문 PDF, 중복 zip 파일 제외 여부 확인


9. 참고 문서

- 저장소 전체 개요: README.md
- 팀 가정사항 및 정책: docs/assumptions.md
- 실행/테스트/데모 계정 안내: docs/execution-test-guide.md
- 요구사항 추적: docs/requirements-traceability.md
- artifact 문서 안내: docs/README.md
- API 명세: docs/api/API_README.md
- JavaFX UI navigation map: docs/ui/javaFX/UI_navigation_map/javaFX_navigation_map.md
- UI API 명세: docs/ui/javaFX/API_specification/README.md
- 설계 원칙 정리: docs/ooad-grasp-mvc.md


10. 기타 참고

- JavaFX와 Swing은 서로 다른 UI Toolkit으로 구현되었지만, UI 이후의 Controller, Service, Domain, Repository, JDBC 계층은 공유한다.
- 일반 이슈 목록에서는 DELETED 상태 이슈를 제외하고, 삭제 이슈는 PL의 별도 삭제 이슈 관리 흐름에서 다룬다.
- 이슈 의존성은 별도 BLOCK/BLOCKED 상태가 아니라 FIXED -> RESOLVED 전이에서 확인하는 guard로 사용한다.
- Assignment 추천은 자동 배정이 아니라 PL의 배정 결정을 돕는 후보 추천 기능이다.
