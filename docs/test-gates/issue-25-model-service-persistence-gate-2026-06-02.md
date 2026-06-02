# Issue 25 Model, Service, Persistence Test Gate

## Purpose

이 문서는 #25의 모델, 서비스, 영속 저장소 테스트 gate를 제출 전 검증 증거로 연결하기 위한 2026-06-02 스냅샷이다. 최종 제출 직전에는 모든 대기 PR을 `dev`에 병합한 뒤 같은 명령을 다시 실행한다.

## Scope

- 기준 commit: `d2ec80b` (`fix: GitHub Project 항목 로딩 한계 보정 (#263)`)
- 기준 branch: `origin/dev`
- 대상: domain, service, persistence, controller 경계 테스트와 repository/setup guard
- 제외: Swing/JavaFX 화면 QA 증빙, 최종 PDF 본문, 제출 zip 생성

## Test Inventory

`src/test/java` 기준 테스트 클래스 수는 다음과 같다.

| 영역 | 테스트 클래스 수 | 주요 검증 |
|---|---:|---|
| application smoke | 1 | 기본 entry point와 실행 인자 분기 |
| controller | 10 | Controller가 service/use-case 경계로 요청을 위임하는지 |
| domain | 8 | Entity, value object, 이슈 상태/이력/의존성 규칙 |
| persistence | 7 | DB 환경, initializer, JDBC query/write support, Oracle repository integration |
| service | 13 | 계정, 프로젝트, 이슈, 배정, 상태 전이, 추천, 통계, 권한 정책 |
| setup | 6 | architecture boundary, workflow guard, 공개 이력 정책, repository convention |
| technical | 3 | password hashing, session, comment id generator |
| ui/javafx | 1 | JavaFX graph screen 단위 검증 |
| ui/swing | 34 | Swing presenter/panel/dialog/request 단위 검증 |

## Requirement Coverage

| 요구 범위 | 증거 |
|---|---|
| #16 User/Role/Project | `UserTest`, `ProjectTest`, `AccountServiceTest`, `ProjectServiceTest` |
| #17 Issue/Comment/History/Priority/Status | `IssueCreationTest`, `CommentTest`, `IssueHistoryTest`, `IssueWorkflowTest` |
| #18 JDBC repository/schema/seed | `DatabaseEnvironmentTest`, `DatabaseInitializerTest`, `JdbcIssueQueriesTest`, `JdbcIssueWriteSupportTest`, `OracleRepositoryIntegrationTest` |
| #19 register/search/detail/comment | `IssueServiceTest`, `IssueControllerTest` |
| #20 assignment/status workflow | `AssignmentServiceTest`, `IssueStateServiceTest`, `IssueWorkflowServiceTest` |
| #21 statistics | `StatisticsServiceTest`, `StatisticsControllerTest` |
| #22 recommendation | `AssignmentRecommendationServiceTest`, `KNNAssignmentRecommendationTest` |
| #43 reject fix | `IssueStateServiceTest`, `IssueWorkflowTest` |
| #44 delete/restore/FIFO | `DeletedIssueServiceTest`, `DeletedIssueControllerTest` |
| #45 dependency/cycle/resolve guard | `IssueDependencyTest`, `IssueServiceTest` |
| #46 edit issue policy | `IssueEditTest`, `IssueServiceTest` |
| #47 reopen policy | `IssueStateServiceTest`, `IssueWorkflowServiceTest` |

## Local Verification

### `./gradlew clean check --console=plain`

Result: passed.

Relevant output:

```text
> Task :auditAutomation
> Task :test FROM-CACHE
> Task :oracleIntegrationTest SKIPPED
> Task :verifyRepositorySetup
저장소 기본 구성을 확인했습니다 (41개 필수 경로 확인).
> Task :check

BUILD SUCCESSFUL in 4s
7 actionable tasks: 3 executed, 3 from cache, 1 up-to-date
```

JUnit XML summary from `build/test-results/test`:

```text
test_classes=83
tests=688
failures_or_errors=0
skipped=34
```

### `./gradlew verifySubmissionMetadata --console=plain`

Result: passed.

Relevant output:

```text
> Task :verifySubmissionMetadata
README 제출 메타데이터를 확인했습니다.

BUILD SUCCESSFUL
```

## Oracle Evidence

Local `check` does not require Oracle and skipped `oracleIntegrationTest` because no Oracle test DB environment was configured. Oracle 검증은 두 경로 중 하나로 최종 제출 전에 다시 확보한다.

| 경로 | 명령 또는 증거 |
|---|---|
| Local Docker | `./gradlew oracleLocalTest --console=plain` |
| CI | `빌드와 테스트` workflow의 `Oracle 통합 테스트` job과 `oracle-통합-테스트-리포트` artifact |

`.github/workflows/gradle.yml`은 일반 test report와 Oracle integration report를 각각 artifact로 업로드한다.

## Final Rerun Checklist

- [ ] #253, #254, #256, #257, #258, #261, #262, #265 병합 후 최신 `origin/dev`를 fetch한다.
- [ ] `./gradlew clean check --console=plain`을 다시 실행한다.
- [ ] `./gradlew verifySubmissionMetadata --console=plain`을 다시 실행한다.
- [ ] Oracle local 또는 CI `Oracle 통합 테스트` 통과 증거를 최종 보고서/제출 패키지에 연결한다.
- [ ] 실패가 있으면 #25를 닫지 않고 실패 test class와 원인을 별도 fix/test 이슈로 분리한다.
