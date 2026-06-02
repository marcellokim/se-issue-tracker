# Issue 25 Model, Service, Persistence Test Gate

## Purpose

이 문서는 #25의 모델, 서비스, 영속 저장소 테스트 gate를 제출 전 검증 증거로 연결하기 위한 2026-06-02 스냅샷이다. 최종 제출 직전에는 모든 대기 PR을 `dev`에 병합한 뒤 같은 명령을 다시 실행한다.

## Scope

- 기준 commit: `8cda31b` (`fix: SonarCloud 자동 분석 범위 보정 (#270)`)
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

### `./gradlew check verifySubmissionMetadata --console=plain`

Result: passed.

Relevant output:

```text
> Task :auditAutomation
> Task :test
> Task :oracleIntegrationTest SKIPPED
> Task :verifyRepositorySetup
저장소 기본 구성을 확인했습니다 (41개 필수 경로 확인).
> Task :check
> Task :verifySubmissionMetadata
README 제출 메타데이터를 확인했습니다.

BUILD SUCCESSFUL in 23s
7 actionable tasks: 6 executed, 1 up-to-date
```

JUnit XML summary from `build/test-results/test`:

```text
test_classes=85
tests=711
failures_or_errors=0
skipped=34
```

## Oracle Evidence

Local `check` does not require Oracle and skipped `oracleIntegrationTest` because no Oracle test DB environment was configured. 최신 `dev`의 GitHub Actions에서는 `Oracle 통합 테스트` job이 성공했으므로, 최종 보고서/제출 패키지에는 해당 workflow run과 artifact를 연결한다.

| 경로 | 명령 또는 증거 |
|---|---|
| Local Docker | `./gradlew oracleLocalTest --console=plain` |
| CI | `빌드와 테스트` workflow의 `Oracle 통합 테스트` job과 `oracle-통합-테스트-리포트` artifact |

`.github/workflows/gradle.yml`은 일반 test report와 Oracle integration report를 각각 artifact로 업로드한다.

## CI Snapshot

`8cda31b` 기준 GitHub Actions/Checks 상태는 다음과 같이 확인했다.

| Check | 상태 | 판단 |
|---|---|---|
| 빌드와 테스트 | success | 필수 체크 통과 |
| 워크플로우 정책 검사 | success | 필수 체크 통과 |
| 보안 코드 분석 | success | Java/Kotlin, Python, GitHub Actions 분석 통과 |
| SonarCloud 분석 | success | workflow 실행 통과 |
| SonarCloud Code Analysis | failure | New Code coverage 70.4%, 기준 80% 미달. branch protection 필수 체크는 아님 |
| Oracle 통합 테스트 | success | CI Oracle 검증 통과 |

## Final Rerun Checklist

- [x] #253, #254, #256, #257, #258, #261, #262, #266 병합 후 최신 `origin/dev`를 fetch한다.
- [x] `./gradlew check verifySubmissionMetadata --console=plain`을 다시 실행한다.
- [x] Oracle local 또는 CI `Oracle 통합 테스트` 통과 증거를 최종 보고서/제출 패키지에 연결한다.
- [x] branch protection 필수 체크를 확인하고, 별도 SonarCloud App coverage check가 제출 차단 항목이 아님을 기록한다.
- [ ] 실패가 있으면 #25를 닫지 않고 실패 test class와 원인을 별도 fix/test 이슈로 분리한다.
