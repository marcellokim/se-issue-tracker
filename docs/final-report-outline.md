# 최종 프로젝트 문서 목차와 페이지 예산

이 문서는 최종 PDF를 60페이지 미만으로 작성하기 위한 목차, 페이지 예산, 증빙 배치 기준을 정리한다. 실제 본문 문장은 최종 구현 상태, 테스트 결과, 발표 자료가 확정된 뒤 다듬는다.

## 작성 원칙

- 팀이 보유한 과제 요구사항 원문을 최상위 기준으로 삼는다. 원문 PDF는 현재 소스 트리와 제출용 source zip에 포함하지 않고 이후 커밋에서도 재추가하지 않는다.
- 보고서는 모든 개발 reference를 나열하지 않고 대표 산출물 중심으로 선별한다.
- JavaFX와 Swing은 기능을 나눈 UI가 아니라, 같은 controller/service/domain/persistence 계층을 재사용하는 두 개의 완전한 UI로 설명한다.
- CLI, 오래된 dashboard 이슈 목록, 삭제 이슈 count 노출처럼 현재 구현과 맞지 않는 과거 설명은 넣지 않는다.
- GitHub issue/PR/Project evidence는 부록식 나열보다 요구사항 추적표와 대표 캡처로 압축한다.

## 페이지 예산

| 구간 | 내용 | 권장 페이지 |
| --- | --- | ---: |
| 표지/팀 정보 | 과제명, 팀 번호, 팀원, 저장소/프로젝트 링크 | 1 |
| 1. 프로젝트 개요 | 문제 정의, 범위, 핵심 요구사항, 실행 환경 | 3 |
| 2. 요구사항 분석 | actor, UC 목록, include/extend 요약, 핵심 UC 선정 근거 | 6 |
| 3. 도메인 분석 | Domain Model, 주요 개념, 상태/역할/권한 가정 | 5 |
| 4. 시스템 동작 분석 | 대표 SSD 3개, Operation Contract 요약 | 7 |
| 5. 설계 구조 | Logical Architecture, MVC, 계층 책임, persistence/technical adapter 구조 | 7 |
| 6. 상세 설계 | DCD, 대표 SD와 GRASP 책임 설명, 주요 설계 선택 | 7 |
| 7. 구현 결과 | JavaFX 전체 UI, Swing 전체 UI, DB persistence, 핵심 workflow 화면 | 8 |
| 8. 테스트와 검증 | JUnit 범위, CI, Oracle 통합 테스트, Swing/JavaFX QA 증빙 | 6 |
| 9. 협업 및 프로젝트 관리 | GitHub Project, issue/PR/review 흐름, 마일스톤 진행 근거 | 2 |
| 10. 결론과 한계 | 완료 범위, 남은 리스크, 발표/데모 연결 | 2 |
| 부록 여유 | 표/스크린샷 조정, 캡션, 참고 링크 | 2 |
| 합계 |  | 56 |

페이지가 초과되면 다음 순서로 줄인다.

1. 개발 reference SSD/SD 전체 목록은 본문에서 제외하고 링크로 대체한다.
2. 협업 및 프로젝트 관리는 본문 2쪽 이내로 두고, 대표 Project 캡처, milestone, PR 흐름 1-2개만 남긴 뒤 나머지는 링크와 부록으로 넘긴다.
3. UI 스크린샷은 역할별 전체 나열보다 같은 기능을 JavaFX/Swing에서 비교하는 묶음으로 배치한다.
4. Operation Contract는 대표 command 중심으로 표만 남기고 상세 사후조건은 문서 링크로 넘긴다.

## 섹션별 원천 자료

| 섹션 | 원천 자료 | 삽입 기준 |
| --- | --- | --- |
| 보고서 본문 초안 | `docs/final-report-draft.md` | 최종 PDF 편집 전 prose 초안으로 사용하고, 캡처/표지/학번/페이지 편집은 제출본에서 처리 |
| 요구사항 분석 | `docs/use-cases/use_case_specifications.md`, `docs/requirements-traceability.md`, `docs/uml/ucd/ucd-issue-tracking-system.puml` | UC1~UC6은 핵심으로 설명하고, UC7~UC16은 coverage 표로 압축 |
| 도메인 분석 | `docs/uml/domain/domain-model-issue-tracking-system.puml`, `docs/assumptions.md` | 상태, 역할, deleted issue, dependency 정책을 현재 구현 기준으로 설명 |
| SSD | `docs/uml/ssd-candidate-catalog.md`, `docs/uml/ssd/ssd-01-register-issue.puml`, `docs/uml/ssd/ssd-05-assign-issue.puml`, `docs/uml/ssd/ssd-06-mark-fixed.puml` | 제출 대표 SSD 3개만 본문 삽입 |
| Operation Contract | `docs/uml/operation-contracts/required_operation_contracts.md` | 등록, 배정, 상태 변경, 삭제/복구, priority/dependency command를 표로 요약 |
| Architecture | `docs/uml/architecture/its_layer_architecture.md`, `docs/uml/architecture/logical-architecture-its.puml`, `docs/ooad-grasp-mvc.md` | Presentation/Application/Domain/Infrastructure 흐름과 MVC 재사용 근거를 함께 설명 |
| DCD/SD/GRASP | `docs/uml/dcd/its_dcd.puml`, `docs/uml/sd/SD_README.md`, `docs/uml/sd/sd_grasp/` | 대표 workflow만 본문에 넣고 나머지는 reference 처리 |
| API/구현 추적 | `docs/api/README.md`, `docs/api/*.md` | UC별 controller/service entry point와 구현 차이를 요약 |
| UI 결과 | `ui.javafx` package, `docs/ui/javaFX/API_명세/`, Swing package, QA 리포트 | 두 UI가 같은 backend contract를 호출한다는 증거 위주로 배치 |
| 테스트 | `src/test/java`, CI 결과, `docs/non-oracle-local-test-commands.md`, `docs/local-oracle-testing.md` | 테스트 계층별 목적과 대표 실행 결과를 표로 정리 |
| 협업 증빙 | GitHub Project, issue, PR, review, milestone | 최종 캡처와 대표 PR 링크만 사용 |

## 구현 증빙 배치

| 요구사항 | 본문에 넣을 증빙 |
| --- | --- |
| MVC 분리 | JavaFX/Swing UI -> Controller -> Service -> Domain/Repository 흐름 그림과 `ApplicationBootstrap` 설명 |
| 두 개 이상의 UI toolkit | JavaFX 전체 UI와 Swing 전체 UI의 같은 기능 화면 비교 |
| Persistence | Oracle 실행 방법, schema/seed, repository integration test 결과 |
| 상태 전이 | ASSIGNED -> FIXED -> RESOLVED -> CLOSED, reject, reopen 시나리오 |
| 삭제 이슈 관리 | PL 전용 deleted issue 화면, soft delete, restore, purge 설명 |
| Dependency | dependency 추가/제거와 resolved guard, JavaFX graph 또는 Swing action evidence |
| Statistics | 프로젝트 기준 통계 화면, DELETED 제외 정책 |
| Recommendation | UC5 배정 화면에 포함된 후보 추천과 KNN 테스트 결과 |
| JUnit | domain/service/controller/persistence/UI test 묶음과 `./gradlew check` 결과 |
| GitHub 협업 | Project board, issue/PR/review 흐름, milestone 진행 화면 |

## 설계-구현-테스트 연결표

최종 PDF 본문에는 아래 수준의 요약표를 넣고, UC별 상세 매핑은 `docs/requirements-traceability.md`의 traceability matrix를 원천 자료로 사용한다.

| 설계 산출물 | 구현 근거 | 테스트 근거 | 보고서 위치 |
| --- | --- | --- | --- |
| UCD / UC1~UC6 Fully Dressed | `controller` package의 UC별 entry point, `service` package의 use case 흐름 | `controller/*Test`, `service/*Test` | 2. 요구사항 분석, 8. 테스트와 검증 |
| Domain Model | `domain.User`, `Project`, `ProjectMember`, `Issue`, `Comment`, `IssueHistory`, `IssueDependency` | `domain/*Test` | 3. 도메인 분석 |
| SSD / Operation Contract | `IssueController`, `AssignmentController`, `IssueStateController`, `DeletedIssueController` public API | `IssueControllerTest`, `AssignmentControllerTest`, `IssueStateControllerTest`, `DeletedIssueControllerTest` | 4. 시스템 동작 분석 |
| Logical Architecture | `config.ApplicationBootstrap`, `controller`, `service`, `repository`, `persistence.jdbc`, `technical` package | `ArchitectureBoundaryTest`, `RepositoryConventionsSmokeTest` | 5. 설계 구조 |
| DCD / SD / GRASP | domain operation, service orchestration, repository port/adapter 구조 | domain/service 단위 테스트, controller flow test | 6. 상세 설계 |
| JavaFX 전체 UI | `ui.javafx` package와 shared controller 호출 | JavaFX smoke/UI 관련 테스트, 수동 화면 증빙 | 7. 구현 결과 |
| Swing 전체 UI | `ui.swing` package와 같은 controller/service 재사용 | `ui.swing/*Test`, Swing QA 리포트 | 7. 구현 결과, 8. 테스트와 검증 |
| Oracle persistence | JDBC repository, schema, seed, runbook | `OracleRepositoryIntegrationTest`, repository tests | 7. 구현 결과, 8. 테스트와 검증 |
| GitHub 협업 흐름 | issue, branch, PR, review, CI, Project board | CI checks, PR metadata, Project evidence | 9. 협업 및 프로젝트 관리 |

## 설계 방어 포인트

| 관점 | 보고서에 넣을 핵심 주장 | 근거 |
| --- | --- | --- |
| MVC | UI는 입력 수집과 결과 표시를 맡고, 업무 요청은 Controller를 통해 Service로 전달한다. | `ui.javafx`, `ui.swing`, `controller` package 경계 |
| UI toolkit 재사용성 | JavaFX와 Swing은 서로 다른 화면 구현이지만 같은 application/backend 계층을 호출한다. | `ApplicationBootstrap.ApplicationContext`, Swing/JavaFX controller 재사용 |
| GRASP Controller | 시스템 operation entry point를 Controller에 모아 UI와 use case 흐름을 분리했다. | `AuthenticationController`, `IssueController`, `AssignmentController`, `IssueStateController` 등 |
| Information Expert | 이슈 상태 전이, 배정, 댓글/이력 생성처럼 Issue가 자기 상태를 가장 잘 아는 규칙은 domain 객체에 둔다. | `domain.Issue`, `IssueWorkflowTest`, `IssueHistoryTest` |
| Low Coupling / High Cohesion | Service는 JDBC 구현체가 아니라 Repository interface와 service port에 의존한다. | `repository` interfaces, `persistence.jdbc` adapters |
| Protected Variations / DIP | password, session, clock, comment id 생성 같은 기술 관심사는 port와 adapter로 분리한다. | `PasswordHashing`, `CurrentUserSession`, `Clock`, `CommentIdProvider` |
| Pure Fabrication | 권한, 추천, 통계, dashboard, bootstrap처럼 domain entity가 아닌 책임은 별도 service/policy 객체로 분리한다. | `PermissionPolicy`, `AssignmentRecommendationService`, `StatisticsService`, `DashboardSummaryService` |
| SOLID | SRP는 package/layer responsibility로, DIP는 repository/service port로, OCP는 UI toolkit 추가 시 backend 변경 최소화로 설명한다. | architecture boundary tests, JavaFX/Swing 공존 구조 |

## 대표 데모 흐름 매핑

| 데모 흐름 | 보여줄 기능 | 보고서 섹션 | 필요한 증빙 |
| --- | --- | --- | --- |
| Admin 로그인 후 계정/프로젝트 관리 | UC11, UC12, UC13 | 7. 구현 결과, 9. 협업 및 프로젝트 관리 | JavaFX 또는 Swing 화면, seed 계정, 프로젝트 변경 결과 |
| PL이 이슈를 등록하고 배정 후보를 확인 | UC1, UC5, UC8 | 4. 시스템 동작 분석, 7. 구현 결과 | 등록 화면, 추천 후보, assignment 결과 |
| Dev가 배정 이슈를 fixed 처리 | UC6 mark fixed | 4. 시스템 동작 분석, 6. 상세 설계 | 상태 변경 dialog, 필수 comment, history |
| Tester가 fixed 이슈를 resolve 또는 reject | UC6 resolve/reject | 4. 시스템 동작 분석, 7. 구현 결과 | resolve 시 active assignment clear, reject 시 기존 assignment 유지 |
| PL이 close/reopen, priority, dependency를 처리 | UC6, UC7, UC16 | 6. 상세 설계, 7. 구현 결과 | 상태 전이, dependency guard, priority history |
| PL이 deleted issue를 관리 | UC9 | 4. 시스템 동작 분석, 7. 구현 결과 | soft delete, restore, purge 화면 |
| 프로젝트 통계 확인 | UC10 | 7. 구현 결과, 8. 테스트와 검증 | chart/table 화면, DELETED 제외 정책 |
| JavaFX와 Swing에서 같은 기능 비교 | 다중 UI toolkit, MVC 재사용 | 5. 설계 구조, 7. 구현 결과 | 같은 workflow의 두 UI 화면, 공통 controller/service 설명 |

## 팀원 작성 슬롯

| 담당 영역 | 넣어야 할 내용 | 원천 |
| --- | --- | --- |
| 구현 담당 | 핵심 기능 설명, 대표 화면, 실행 명령 | JavaFX/Swing 화면, `README.md`, QA 리포트 |
| 테스트 담당 | 테스트 분류, 실행 결과, 실패/보완 이력 | `./gradlew check`, CI, Oracle runbook |
| 문서 담당 | UC/SSD/OC/DCD/SD 선별, page budget 유지 | `docs/uml/`, `docs/requirements-traceability.md` |
| PM 담당 | GitHub Project evidence, 일정/역할/PR 흐름 | Project board, milestone, issue/PR 목록 |

## 최종 PDF freeze checklist

- [ ] `docs/requirements-traceability.md`의 UC1~UC16 표가 최신 `dev` 기준이다.
- [ ] `docs/final-report-draft.md`를 최종 제출 양식으로 옮기며 표지, 팀원 정보, 캡처, 페이지 번호를 편집한다.
- [ ] 대표 SSD 3개와 DCD/Architecture 그림이 렌더링 가능하다.
- [ ] JavaFX와 Swing 스크린샷이 같은 기능을 비교할 수 있게 준비되어 있다.
- [ ] `./gradlew check`와 제출 직전 CI 결과가 본문에 반영되어 있다.
- [ ] Oracle local 실행 또는 integration test 근거가 들어 있다.
- [ ] GitHub Project/issue/PR evidence 캡처가 최신이다.
- [ ] PDF가 56페이지 예산 안에 들어가며, 최종 제출 제한 60페이지 미만을 유지한다.
- [ ] README.txt, slides, demo video, 제출 zip과 문서 내용이 서로 충돌하지 않는다.
